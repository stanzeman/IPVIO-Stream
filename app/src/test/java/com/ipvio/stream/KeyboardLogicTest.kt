package com.ipvio.stream

import org.junit.Assert.assertEquals
import org.junit.Test

/** Russian layer of the search keyboard (T-056). */
class KeyboardLogicTest {
    @Test
    fun russianLayerHasAll33LettersOnce() {
        val keys = KeyboardLogic.CYRILLIC.joinToString("")
        val alphabet = "абвгдеёжзийклмнопрстуфхцчшщъыьэюя"
        assertEquals(33, alphabet.length)
        assertEquals(alphabet.toSet(), keys.toSet())
        assertEquals(keys.length, keys.toSet().size)
    }

    @Test
    fun symbolsAndSpecialLettersWinOverTheAlphabet() {
        assertEquals(KeyboardLogic.SYMBOLS, KeyboardLogic.rows(symbols = true, special = false, cyrillic = true))
        assertEquals(KeyboardLogic.SPECIAL, KeyboardLogic.rows(symbols = false, special = true, cyrillic = true))
        assertEquals(KeyboardLogic.CYRILLIC, KeyboardLogic.rows(symbols = false, special = false, cyrillic = true))
        assertEquals(KeyboardLogic.LATIN, KeyboardLogic.rows(symbols = false, special = false, cyrillic = false))
    }

    @Test
    fun switchKeyShowsTheOtherAlphabet() {
        assertEquals("АБВ", KeyboardLogic.alphabetLabel(cyrillic = false))
        assertEquals("ABC", KeyboardLogic.alphabetLabel(cyrillic = true))
    }

    @Test
    fun everyLayerHasThreeRows() {
        listOf(KeyboardLogic.LATIN, KeyboardLogic.CYRILLIC, KeyboardLogic.SYMBOLS, KeyboardLogic.SPECIAL).forEach { assertEquals(3, it.size) }
    }
}
