package com.ipvio.stream

import org.junit.Assert.assertEquals
import org.junit.Test

/** T-070: translations of "most watched channels". */
class DictTopChTest {
    private fun tr(s: String, code: String): String {
        I18nCore.load(DictTopCh)
        I18nCore.loadColumn(I18nCore.CODES.indexOf("nl"), DictTopChNl)
        I18nCore.loadColumn(I18nCore.CODES.indexOf("ar"), DictTopChAr)
        I18nCore.loadColumn(I18nCore.CODES.indexOf("pl"), DictTopChPl)
        I18nCore.loadColumn(I18nCore.CODES.indexOf("zh"), DictTopChZh)
        I18nCore.loadColumn(I18nCore.CODES.indexOf("pt"), DictTopChPt)
        return I18nCore.translate(s, I18nCore.CODES.indexOf(code))
    }

    @Test fun english_labels() {
        assertEquals("Most watched channels", tr("Meistgesehene Sender", "en"))
        assertEquals("The channels you watch the longest", tr("Die Sender, die du am längsten schaust", "en"))
    }

    @Test fun every_row_has_all_columns() {
        for (l in DictTopCh.lines().filter { it.contains('¦') }) assertEquals(l, 9, l.split('¦').size)
        for (d in listOf(DictTopChNl, DictTopChAr, DictTopChPl, DictTopChZh, DictTopChPt)) for (l in d.lines().filter { it.contains('¦') }) assertEquals(l, 2, l.split('¦').size)
    }

    @Test fun other_columns() {
        assertEquals("Meest bekeken zenders", tr("Meistgesehene Sender", "nl"))
        assertEquals("Najczęściej oglądane kanały", tr("Meistgesehene Sender", "pl"))
        assertEquals("Canais mais vistos", tr("Meistgesehene Sender", "pt"))
    }
}
