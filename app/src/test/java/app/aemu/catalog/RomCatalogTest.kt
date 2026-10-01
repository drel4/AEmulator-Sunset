package app.aemu.catalog

import org.junit.Assert.*
import org.junit.Test

class RomCatalogTest {
    @Test fun parsesMotdSectionsAndAllStatuses() {
        val catalog = RomCatalogParser.parse("""
            #Hello
            #Привет
            [Official];Notes;https://example.org/forum
            Sony;4.4.2;Xperia UI;-1;;https://example.org/unknown
            Samsung;4.4.2;TouchWiz;0;No boot;https://example.org/broken
            HTC;4.3;Sense;1;Issues;https://example.org/issues
            Nexus;4.4;AOSP;2;Works;https://example.org/good
        """.trimIndent())
        assertEquals("Hello\nПривет", catalog.motd)
        assertEquals("Official", catalog.sections.single().name)
        assertEquals("Notes", catalog.sections.single().comment)
        assertEquals("https://example.org/forum", catalog.sections.single().source)
        assertEquals(listOf(-1, 0, 1, 2), catalog.sections.single().roms.map { it.status })
        assertTrue(catalog.warningLines.isEmpty())
    }
    @Test fun keepsEmptySectionsAndMissingDownloadUrls() {
        val catalog = RomCatalogParser.parse("[Empty];;\n[Sunset];;https://example.org/\nSamsung;4.4.2;TouchWiz;0;;\nDigma;6.0;AOSP;-1;;")
        assertEquals(2, catalog.sections.size)
        assertTrue(catalog.sections.first().roms.isEmpty())
        assertEquals(2, catalog.sections.last().roms.size)
        assertTrue(catalog.sections.last().roms.all { it.url == null })
        assertTrue(catalog.warningLines.isEmpty())
    }
    @Test fun acceptsBomCrLfWhitespaceAndUrlSemicolons() {
        val catalog = RomCatalogParser.parse("\uFEFF# MOTD\r\n [Section] ; ;https://example.org/a;b\r\n Phone ; 4.4 ; Skin ; 2 ; ; https://example.org/file?a=1;b=2 \r\n")
        assertEquals("MOTD", catalog.motd)
        assertEquals("https://example.org/a;b", catalog.sections.single().source)
        assertEquals("Phone", catalog.sections.single().roms.single().device)
        assertEquals("https://example.org/file?a=1;b=2", catalog.sections.single().roms.single().url)
    }
    @Test fun malformedRecordsDoNotHideGoodRoms() {
        val catalog = RomCatalogParser.parse("[Good];;\nmalformed\nPhone;4.4;UI;900;;javascript:alert(1)\n[broken\nFine;4.3;UI;2;;https://example.org/file")
        assertEquals(listOf(2, 3, 4), catalog.warningLines)
        assertEquals(-1, catalog.sections.single().roms.first().status)
        assertNull(catalog.sections.single().roms.first().url)
        assertEquals(2, catalog.sections.single().roms.size)
    }
    @Test fun rejectsNonWebLinksAndCredentials() {
        for (url in listOf("javascript:alert(1)", "file:///tmp/file", "intent://x", "https://user:pass@example.org/", "https://", ""))
            assertNull(url, CatalogUrls.valid(url))
        assertEquals("https://example.org/a", CatalogUrls.valid(" https://example.org/a "))
        assertEquals("http://example.org/a", CatalogUrls.valid("http://example.org/a"))
    }
    @Test fun emptyCatalogAndUngroupedRomAreSupported() {
        assertTrue(RomCatalogParser.parse("#No entries yet").sections.isEmpty())
        assertEquals("", RomCatalogParser.parse("Phone;4.4;UI;2;;").sections.single().name)
    }
    @Test fun commentsAfterListAreNotMotd() {
        val catalog = RomCatalogParser.parse("#MOTD\n[One];;\n#Comment\n[Two];;")
        assertEquals("MOTD", catalog.motd)
        assertEquals(2, catalog.sections.size)
    }
    @Test(expected = IllegalArgumentException::class) fun boundsLineCount() {
        RomCatalogParser.parse("\n".repeat(10_001))
    }
}
