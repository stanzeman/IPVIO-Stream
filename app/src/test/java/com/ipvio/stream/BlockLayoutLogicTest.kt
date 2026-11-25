package com.ipvio.stream

import org.junit.Assert.assertEquals
import org.junit.Test

/** T-069: blocks added in an update start hidden when they are hidden by default. */
class BlockLayoutLogicTest {
    private val defaults = setOf("favch", "hist", "rum", "rus")

    @Test fun nothing_saved_uses_the_defaults() {
        assertEquals(defaults, BlockLayoutLogic.hiddenOnLoad(emptyList(), null, defaults))
    }

    @Test fun new_default_hidden_blocks_start_hidden_on_a_saved_layout() {
        val savedOrder = listOf("hero", "tiles", "favch", "hist", "trend", "mg")
        val savedHidden = setOf("tiles")   // the user showed "favch" and "hist" and hid "tiles"
        assertEquals(setOf("tiles", "rum", "rus"), BlockLayoutLogic.hiddenOnLoad(savedOrder, savedHidden, defaults))
    }

    @Test fun a_block_the_user_switched_on_stays_on() {
        val savedOrder = listOf("hero", "favch", "hist", "rum", "rus")
        assertEquals(setOf("rus"), BlockLayoutLogic.hiddenOnLoad(savedOrder, setOf("rus"), defaults))
    }
}
