package com.ipvio.stream

import org.junit.Assert.assertEquals
import org.junit.Test

/** T-063 to T-066: translations of the new home and tab lists. */
class DictT064Test {
    private fun tr(s: String, code: String): String {
        I18nCore.load(DictT064)
        I18nCore.loadColumn(I18nCore.CODES.indexOf("nl"), DictT064Nl)
        I18nCore.loadColumn(I18nCore.CODES.indexOf("ar"), DictT064Ar)
        I18nCore.loadColumn(I18nCore.CODES.indexOf("pl"), DictT064Pl)
        return I18nCore.translate(s, I18nCore.CODES.indexOf(code))
    }

    @Test fun english_rows() {
        assertEquals("Top 10 movies today", tr("Top 10 Filme heute", "en"))
        assertEquals("Popular & new · Series", tr("Beliebt & neu · Serien", "en"))
        assertEquals("Russian & Kazakh", tr("Russisch & Kasachisch", "en"))
        assertEquals("Completing series data", tr("Serien-Daten werden ergänzt", "en"))
        assertEquals("Movies from Russia or Kazakhstan or in Russian or Kazakh, newest first",
            tr("Filme aus Russland oder Kasachstan oder auf Russisch oder Kasachisch, neueste zuerst", "en"))
    }

    @Test fun every_row_has_all_columns() {
        for (l in DictT064.lines().filter { it.contains('¦') }) assertEquals(l, 9, l.split('¦').size)
        for (d in listOf(DictT064Nl, DictT064Ar, DictT064Pl)) for (l in d.lines().filter { it.contains('¦') }) assertEquals(l, 2, l.split('¦').size)
    }

    @Test fun dutch_arabic_polish_rows() {
        assertEquals("Nieuwe titels", tr("Neue Titel", "nl"))
        assertEquals("عناوين جديدة", tr("Neue Titel", "ar"))
        assertEquals("Nowe tytuły", tr("Neue Titel", "pl"))
    }
}
