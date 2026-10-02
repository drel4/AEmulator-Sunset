package app.aemu.importer

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.nio.file.StandardOpenOption
import java.security.MessageDigest
import org.junit.Assert.*
import org.junit.Test

class Yaffs2ReaderTest {
    private fun source(bytes: ByteArray) = object : RandomSource {
        override val size = bytes.size.toLong()
        override fun read(pos: Long, dst: ByteBuffer) {
            val count = minOf(dst.remaining(), bytes.size - pos.toInt())
            if (count > 0) dst.put(bytes, pos.toInt(), count)
            while (dst.hasRemaining()) dst.put(0)
        }
    }
    private fun header(id: Int, parent: Int, name: String, type: Int = 3, size: Int = 0,
                       alias: String = "", target: Int = 0, page: Int = 2048,
                       order: ByteOrder = ByteOrder.LITTLE_ENDIAN): ByteArray {
        val b = ByteBuffer.wrap(ByteArray(page + page / 32) { 0xff.toByte() }).order(order)
        b.putInt(0, type); b.putInt(4, parent)
        for (i in 10 until 266) b.put(i, 0)
        name.toByteArray().copyInto(b.array(), 10)
        b.putInt(268, if (type == 3) 0x41ed else 0x81ed)
        b.putInt(292, size); b.putInt(296, target)
        for (i in 300 until 460) b.put(i, 0)
        alias.toByteArray().copyInto(b.array(), 300)
        b.putInt(page, 0x1000); b.putInt(page + 4, id); b.putInt(page + 8, 0)
        return b.array()
    }
    private fun data(id: Int, chunk: Int, bytes: ByteArray, page: Int = 2048,
                     order: ByteOrder = ByteOrder.LITTLE_ENDIAN): ByteArray {
        val b = ByteBuffer.wrap(ByteArray(page + page / 32) { 0xff.toByte() }).order(order)
        bytes.copyInto(b.array())
        b.putInt(page, 0x1000); b.putInt(page + 4, id); b.putInt(page + 8, chunk); b.putInt(page + 12, bytes.size)
        return b.array()
    }
    private fun image(vararg pages: ByteArray) = source(pages.reduce { a, b -> a + b })
    private fun rejects(src: RandomSource) {
        try { Yaffs2Reader(src); fail("accepted malformed snapshot") } catch (_: IOException) { }
    }

    @Test fun readsFilesDirectoriesLinksAndHardLinksWithoutLoadingFileContents() {
        val bytes = "test file".toByteArray()
        val fs = Yaffs2Reader(image(header(1, 1, ""), header(258, 257, "file", 1, bytes.size),
            data(258, 1, bytes), header(257, 1, "bin"), header(259, 257, "link", 2, alias = "/system/bin/file"),
            header(260, 257, "hard", 4, target = 258)))
        val found = LinkedHashMap<String, Yaffs2Reader.Node>()
        fs.walk { path, node -> found[path] = node }
        assertEquals(setOf("bin", "bin/file", "bin/link", "bin/hard"), found.keys)
        assertEquals("/system/bin/file", found.getValue("bin/link").alias)
        for (path in listOf("bin/file", "bin/hard")) {
            val out = ByteArrayOutputStream(); fs.copy(found.getValue(path), out)
            assertArrayEquals(bytes, out.toByteArray())
            assertEquals(0x81ed, fs.fileMode(found.getValue(path)))
        }
    }
    @Test fun detectsBothGeometriesAndByteOrdersAndOrdersChunks() {
        for (page in listOf(2048, 4096)) for (order in listOf(ByteOrder.LITTLE_ENDIAN, ByteOrder.BIG_ENDIAN)) {
            val first = ByteArray(page) { (it % 251).toByte() }; val last = byteArrayOf(5, 6, 7)
            val fs = Yaffs2Reader(image(header(1, 1, "", page = page, order = order),
                header(257, 1, "file", 1, page + 3, page = page, order = order),
                data(257, 2, last, page, order), data(257, 1, first, page, order)))
            assertEquals(page, fs.geometry.pageSize); assertEquals(order, fs.geometry.order)
            fs.walk { _, node -> val out = ByteArrayOutputStream(); fs.copy(node, out); assertArrayEquals(first + last, out.toByteArray()) }
        }
    }
    @Test fun rejectsUnsafeNamesMissingParentsAndDirectoryCycles() {
        for (name in listOf("..", "a/b", "a\\b", ".", "")) rejects(image(header(1, 1, ""), header(257, 1, name)))
        rejects(image(header(1, 1, ""), header(257, 999, "orphan")))
        rejects(image(header(1, 1, ""), header(257, 258, "a"), header(258, 257, "b")))
        rejects(image(header(1, 1, ""), header(257, 1, "same"), header(258, 1, "same")))
    }
    @Test fun rejectsMissingDuplicateAndShortChunks() {
        rejects(image(header(1, 1, ""), header(257, 1, "file", 1, 3)))
        rejects(image(header(1, 1, ""), header(257, 1, "file", 1, 3), data(257, 1, byteArrayOf(1))))
        rejects(image(header(1, 1, ""), header(257, 1, "file", 1, 1), data(257, 1, byteArrayOf(1)), data(257, 1, byteArrayOf(1))))
        rejects(image(header(1, 1, ""), data(257, 1, byteArrayOf(1))))
    }
    @Test fun rejectsTruncationJournalTagsAndRepeatedHeaders() {
        rejects(source((header(1, 1, "") + header(257, 1, "file")).dropLast(1).toByteArray()))
        rejects(image(header(1, 1, ""), header(257, 1, "a"), header(257, 1, "b")))
        rejects(image(header(1, 1, ""), header(257, 1, "a").also { ByteBuffer.wrap(it).order(ByteOrder.LITTLE_ENDIAN).putInt(2048, 0x1001) }))
        assertFalse(Yaffs2Reader.probe(source(byteArrayOf(1, 2, 3))))
    }
    @Test fun rejectsHardLinkCyclesAndMissingTargets() {
        rejects(image(header(1, 1, ""), header(257, 1, "hard", 4, target = 999)))
        rejects(image(header(1, 1, ""), header(257, 1, "a", 4, target = 258), header(258, 1, "b", 4, target = 257)))
    }
    @Test fun cancellationStopsIndexingAndCopying() {
        try { Yaffs2Reader(image(header(1, 1, ""))) { throw IOException("cancelled") }; fail() } catch (e: IOException) { assertEquals("cancelled", e.message) }
        var cancelled = false
        val fs = Yaffs2Reader(image(header(1, 1, ""), header(257, 1, "file", 1, 1), data(257, 1, byteArrayOf(1)))) {
            if (cancelled) throw IOException("cancelled")
        }
        var file: Yaffs2Reader.Node? = null; fs.walk { _, n -> file = n }; cancelled = true
        try { fs.copy(file!!, ByteArrayOutputStream()); fail() } catch (e: IOException) { assertEquals("cancelled", e.message) }
    }
    @Test fun optionalRealCwmSnapshot() {
        val fixture = System.getenv("AEMU_YAFFS2_FIXTURE")?.let(::File) ?: return
        assertEquals("51824642b12922d01c83cf40ddd795fa", fixture.inputStream().use { input ->
            val md = MessageDigest.getInstance("MD5"); val buffer = ByteArray(65536)
            while (true) { val n = input.read(buffer); if (n < 0) break; md.update(buffer, 0, n) }
            md.digest().joinToString("") { "%02x".format(it) }
        })
        FileChannel.open(fixture.toPath(), StandardOpenOption.READ).use { channel ->
            val fs = Yaffs2Reader(ChannelSource(channel)); var objects = 0; var files = 0; var bytes = 0L
            var buildProp = false; var framework = false
            fs.walk { path, node ->
                objects++
                if (node.type == 1 || node.type == 4) {
                    files++
                    fs.copy(node, object : OutputStream() {
                        override fun write(b: Int) { bytes++ }
                        override fun write(b: ByteArray, off: Int, len: Int) { bytes += len }
                    })
                }
                if (path == "build.prop" && node.type == 1) buildProp = true
                if (path == "framework" && node.type == 3) framework = true
            }
            assertTrue(buildProp && framework && files > 100 && bytes > 10_000_000)
            println("CWM YAFFS2 fixture: $objects objects, $files files, $bytes file bytes; all file chunks copied")
        }
    }
}
