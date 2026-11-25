package com.ipvio.stream

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** T-072: translations of the "Continue watching" settings. */
class DictT072Test {
    private val ph = Regex("""\{#?(\d)\}""")

    private fun tr(s: String, code: String): String {
        I18nCore.load(DictT072)
        I18nCore.loadColumn(I18nCore.CODES.indexOf("nl"), DictT072Nl)
        I18nCore.loadColumn(I18nCore.CODES.indexOf("ar"), DictT072Ar)
        I18nCore.loadColumn(I18nCore.CODES.indexOf("pl"), DictT072Pl)
        I18nCore.loadColumn(I18nCore.CODES.indexOf("zh"), DictT072Zh)
        I18nCore.loadColumn(I18nCore.CODES.indexOf("pt"), DictT072Pt)
        return I18nCore.translate(s, I18nCore.CODES.indexOf(code))
    }

    @Test fun english_labels() {
        assertEquals("Never", tr("Nie", "en"))
        assertEquals("After 7 days", tr("Nach 7 Tagen", "en"))
        assertEquals("Remove movies from continue watching", tr("Filme aus Weiterschauen entfernen", "en"))
        assertEquals("Remove series from continue watching", tr("Serien aus Weiterschauen entfernen", "en"))
    }

    @Test fun other_columns() {
        assertEquals("Na 3 dagen", tr("Nach 3 Tagen", "nl"))
        assertEquals("Po 14 dniach", tr("Nach 14 Tagen", "pl"))
        assertEquals("Após 30 dias", tr("Nach 30 Tagen", "pt"))
    }

    @Test fun every_row_has_all_columns_and_placeholders() {
        for (l in DictT072.lines().filter { it.contains('¦') }) assertEquals(l, 9, l.split('¦').size)
        for (d in listOf(DictT072Nl, DictT072Ar, DictT072Pl, DictT072Zh, DictT072Pt)) for (l in d.lines().filter { it.contains('¦') }) assertEquals(l, 2, l.split('¦').size)
        for (d in listOf(DictT072, DictT072Nl, DictT072Ar, DictT072Pl, DictT072Zh, DictT072Pt)) for (l in d.lines().filter { it.contains('¦') }) {
            val cols = l.split('¦')
            val de = ph.findAll(cols[0]).map { it.value }.toSet()
            for (c in cols.drop(1)) assertTrue(l, ph.findAll(c).map { it.value }.toSet() == de)
        }
    }
}
