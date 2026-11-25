package com.ipvio.stream

import org.junit.Assert.assertEquals
import org.junit.Test

/** T-068: translations of "most watched channels". */
class DictT068Test {
    private fun tr(s: String, code: String): String {
        I18nCore.load(DictT068)
        I18nCore.loadColumn(I18nCore.CODES.indexOf("nl"), DictT068Nl)
        I18nCore.loadColumn(I18nCore.CODES.indexOf("ar"), DictT068Ar)
        I18nCore.loadColumn(I18nCore.CODES.indexOf("pl"), DictT068Pl)
        I18nCore.loadColumn(I18nCore.CODES.indexOf("zh"), DictT068Zh)
        I18nCore.loadColumn(I18nCore.CODES.indexOf("pt"), DictT068Pt)
        return I18nCore.translate(s, I18nCore.CODES.indexOf(code))
    }

    @Test fun english_labels() {
        assertEquals("Most watched channels", tr("Meistgesehene Sender", "en"))
        assertEquals("The channels you watch the longest", tr("Die Sender, die du am längsten schaust", "en"))
    }

    @Test fun every_row_has_all_columns() {
        for (l in DictT068.lines().filter { it.contains('¦') }) assertEquals(l, 9, l.split('¦').size)
        for (d in listOf(DictT068Nl, DictT068Ar, DictT068Pl, DictT068Zh, DictT068Pt)) for (l in d.lines().filter { it.contains('¦') }) assertEquals(l, 2, l.split('¦').size)
    }

    @Test fun other_columns() {
        assertEquals("Meest bekeken zenders", tr("Meistgesehene Sender", "nl"))
        assertEquals("Najczęściej oglądane kanały", tr("Meistgesehene Sender", "pl"))
        assertEquals("Canais mais vistos", tr("Meistgesehene Sender", "pt"))
    }
}
