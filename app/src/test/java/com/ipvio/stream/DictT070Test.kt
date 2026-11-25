package com.ipvio.stream

import org.junit.Assert.assertEquals
import org.junit.Test

/** T-070: translations of "most watched channels". */
class DictT070Test {
    private fun tr(s: String, code: String): String {
        I18nCore.load(DictT070)
        I18nCore.loadColumn(I18nCore.CODES.indexOf("nl"), DictT070Nl)
        I18nCore.loadColumn(I18nCore.CODES.indexOf("ar"), DictT070Ar)
        I18nCore.loadColumn(I18nCore.CODES.indexOf("pl"), DictT070Pl)
        I18nCore.loadColumn(I18nCore.CODES.indexOf("zh"), DictT070Zh)
        I18nCore.loadColumn(I18nCore.CODES.indexOf("pt"), DictT070Pt)
        return I18nCore.translate(s, I18nCore.CODES.indexOf(code))
    }

    @Test fun english_labels() {
        assertEquals("Most watched channels", tr("Meistgesehene Sender", "en"))
        assertEquals("The channels you watch the longest", tr("Die Sender, die du am längsten schaust", "en"))
    }

    @Test fun every_row_has_all_columns() {
        for (l in DictT070.lines().filter { it.contains('¦') }) assertEquals(l, 9, l.split('¦').size)
        for (d in listOf(DictT070Nl, DictT070Ar, DictT070Pl, DictT070Zh, DictT070Pt)) for (l in d.lines().filter { it.contains('¦') }) assertEquals(l, 2, l.split('¦').size)
    }

    @Test fun other_columns() {
        assertEquals("Meest bekeken zenders", tr("Meistgesehene Sender", "nl"))
        assertEquals("Najczęściej oglądane kanały", tr("Meistgesehene Sender", "pl"))
        assertEquals("Canais mais vistos", tr("Meistgesehene Sender", "pt"))
    }
}
