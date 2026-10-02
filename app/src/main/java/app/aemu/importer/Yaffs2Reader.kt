package app.aemu.importer

import java.io.IOException
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.TreeMap

/** Sunset modification, 2026-10-02. Clean YAFFS2 recovery snapshots, not NAND journal replay.
 * Original Kotlin implementation of the documented object-header/packed-tag layout.
 * Keeps page offsets, not file contents, in memory. No ECC correction or YAFFS1 support.
 */
class Yaffs2Reader(private val src: RandomSource, private val checkCancelled: () -> Unit = {}) {
    data class Geometry(val pageSize: Int, val spareSize: Int, val order: ByteOrder) {
        val stride get() = pageSize + spareSize
    }
    data class Node(val id: Long, val parent: Long, val name: String, val type: Int,
                    val mode: Int, val size: Long, val alias: String, val equivalent: Long)
    private data class Chunk(val offset: Long, val bytes: Int)
    val geometry = detect(src) ?: throw IOException("not a supported YAFFS2 recovery snapshot")
    private val nodes = LinkedHashMap<Long, Node>()
    private val chunks = HashMap<Long, TreeMap<Long, Chunk>>()
    private val paths = HashMap<Long, String>()

    init {
        if (src.size % geometry.stride != 0L) fail("truncated page")
        if (src.size / geometry.stride > 1_000_000) fail("too many pages")
        val page = ByteBuffer.allocate(geometry.stride).order(geometry.order)
        var offset = 0L
        while (offset < src.size) {
            checkCancelled()
            page.clear(); src.read(offset, page)
            val tag = geometry.pageSize
            val sequence = unsigned(page, tag)
            if (sequence == 0xffffffffL) { offset += geometry.stride; continue }
            // mkyaffs2image snapshots use a single initial sequence. Raw NAND
            // dumps with deletions/shrinks/shadows require a real backward scan.
            if (sequence != 0x1000L) fail("journal/raw NAND images are not supported")
            val id = unsigned(page, tag + 4)
            val chunk = unsigned(page, tag + 8)
            val count = unsigned(page, tag + 12)
            if (id == 0L || id > 0x3ffffL || chunk and 0x80000000L != 0L) fail("invalid/extended tags")
            if (chunk == 0L) {
                val type = page.getInt(0)
                if (type !in 1..5 || nodes.containsKey(id)) fail("invalid or repeated object header")
                val name = string(page.array(), 10, 256)
                if (id != 1L && (name.isEmpty() || name == "." || name == ".." ||
                            name.any { it == '/' || it == '\\' || it.code < 32 })) fail("unsafe object name")
                val size = if (type == 1) unsigned(page, 292) else 0L
                if (size > src.size) fail("file size exceeds snapshot")
                // Older snapshot writers leave the high-size field erased.
                val high = unsigned(page, 496)
                if (type == 1 && high != 0L && high != 0xffffffffL) fail("64-bit file sizes are unsupported")
                if (page.getInt(504) !in listOf(0, -1) || page.getInt(508) !in listOf(0, -1)) fail("shadow/shrink header")
                nodes[id] = Node(id, unsigned(page, 4), name, type, page.getInt(268), size,
                    if (type == 2) string(page.array(), 300, 160) else "", unsigned(page, 296))
                if (nodes.size > 100_000) fail("too many objects")
            } else {
                if (count !in 1..geometry.pageSize.toLong() || chunk > src.size / geometry.stride) fail("invalid data chunk")
                if (chunks.getOrPut(id) { TreeMap() }.put(chunk, Chunk(offset, count.toInt())) != null) fail("repeated data chunk")
            }
            offset += geometry.stride
        }
        nodes[1]?.let { if (it.type != 3) fail("root is not a directory") }
        for ((id, data) in chunks) {
            val node = nodes[id] ?: fail("data without object header")
            if (node.type != 1) fail("data on non-file object")
            val expected = (node.size + geometry.pageSize - 1) / geometry.pageSize
            if (data.size.toLong() != expected) fail("missing file chunks")
            var index = 1L
            for ((number, chunk) in data) {
                val length = minOf(geometry.pageSize.toLong(), node.size - (index - 1) * geometry.pageSize).toInt()
                if (number != index || chunk.bytes != length) fail("missing or short file chunk")
                index++
            }
        }
        val usedPaths = HashSet<String>()
        for (node in nodes.values) {
            checkCancelled()
            if (node.type == 1 && node.size > 0 && !chunks.containsKey(node.id)) fail("file has no data")
            val path = path(node.id)
            if (node.id != 1L && !usedPaths.add(path)) fail("duplicate path")
            if (node.type == 4) fileNode(node)
        }
    }

    fun walk(visit: (String, Node) -> Unit) {
        for (node in nodes.values.sortedBy { paths[it.id] }) {
            checkCancelled()
            if (node.id != 1L) visit(paths.getValue(node.id), node)
        }
    }

    /** Hard links are resolved to file bytes; the Android importer may copy them. */
    fun copy(node: Node, out: OutputStream) {
        val file = fileNode(node)
        val buffer = ByteBuffer.allocate(geometry.pageSize)
        for (chunk in chunks[file.id]?.values.orEmpty()) {
            checkCancelled()
            buffer.clear(); buffer.limit(chunk.bytes); src.read(chunk.offset, buffer)
            out.write(buffer.array(), 0, chunk.bytes)
        }
    }

    fun fileMode(node: Node): Int = fileNode(node).mode

    private fun fileNode(start: Node): Node {
        var node = start
        val seen = HashSet<Long>()
        while (node.type == 4) {
            if (!seen.add(node.id) || seen.size > 256) fail("hard-link cycle")
            node = nodes[node.equivalent] ?: fail("missing hard-link target")
        }
        if (node.type != 1) fail("hard link does not target a file")
        return node
    }

    private fun path(id: Long): String {
        paths[id]?.let { return it }
        var current = id
        val names = ArrayList<String>()
        val seen = HashSet<Long>()
        while (current != 1L) {
            if (!seen.add(current) || seen.size > 256) fail("directory cycle/depth limit")
            val node = nodes[current] ?: fail("missing parent directory")
            names.add(node.name)
            current = node.parent
            if (current != 1L && nodes[current]?.type != 3) fail("parent is not a directory")
        }
        val path = names.asReversed().joinToString("/")
        paths[id] = path
        return path
    }

    companion object {
        private fun fail(message: String): Nothing = throw IOException("YAFFS2: $message")
        private fun unsigned(b: ByteBuffer, at: Int) = b.getInt(at).toLong() and 0xffffffffL
        private fun string(b: ByteArray, start: Int, length: Int): String {
            val end = (start until start + length).firstOrNull { b[it] == 0.toByte() }
                ?: fail("unterminated name/alias")
            return String(b, start, end - start, Charsets.UTF_8)
        }
        fun probe(src: RandomSource) = detect(src) != null
        private fun detect(src: RandomSource): Geometry? {
            for ((page, spare) in listOf(2048 to 64, 4096 to 128)) {
                if (src.size < page + spare) continue
                val b = ByteBuffer.allocate(page + spare); src.read(0, b)
                for (order in listOf(ByteOrder.LITTLE_ENDIAN, ByteOrder.BIG_ENDIAN)) {
                    b.order(order)
                    if (unsigned(b, page) == 0x1000L && unsigned(b, page + 4) in 1..0x3ffffL &&
                        unsigned(b, page + 8) == 0L && b.getInt(0) in 1..5 &&
                        unsigned(b, 4) in 1..0x3ffffL) return Geometry(page, spare, order)
                }
            }
            return null
        }
    }
}
