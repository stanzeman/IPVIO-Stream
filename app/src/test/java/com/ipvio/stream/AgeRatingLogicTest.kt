package com.ipvio.stream

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AgeRatingLogicTest {
    @Test fun countryFromTmdbLanguage() {
        assertEquals("DE", AgeRatingLogic.country("de-DE"))
        assertEquals("US", AgeRatingLogic.country("en-US"))
        assertEquals("US", AgeRatingLogic.country("de"))
    }

    @Test fun germanNumberBecomesFsk() {
        assertEquals("FSK 12", AgeRatingLogic.pick(mapOf("DE" to "12", "US" to "PG-13"), "DE"))
        assertEquals("FSK 0", AgeRatingLogic.pick(mapOf("DE" to "0"), "DE"))
    }

    @Test fun ownCountryWithoutMarker() {
        assertEquals("PG-13", AgeRatingLogic.pick(mapOf("DE" to "12", "US" to "PG-13"), "US"))
        assertEquals("12", AgeRatingLogic.pick(mapOf("FR" to "12"), "FR"))
    }

    @Test fun fallsBackToUsWithMarker() {
        assertEquals("R (US)", AgeRatingLogic.pick(mapOf("US" to "R"), "DE"))
        assertEquals("TV-MA (US)", AgeRatingLogic.pick(mapOf("DE" to " ", "US" to "TV-MA"), "DE"))
    }

    @Test fun noRatingGivesNull() {
        assertNull(AgeRatingLogic.pick(emptyMap(), "DE"))
        assertNull(AgeRatingLogic.pick(mapOf("GB" to "15"), "DE"))
        assertNull(AgeRatingLogic.pick(mapOf("US" to "NR"), "DE"))
    }

    @Test fun movieCertTakesTheHighestRelease() {
        assertEquals("18", AgeRatingLogic.movieCert(listOf(4 to "18", 3 to "16")))
        assertEquals("12", AgeRatingLogic.movieCert(listOf(3 to "12", 4 to "6")))
        assertEquals("18", AgeRatingLogic.movieCert(listOf(1 to "", 4 to "18")))
        assertNull(AgeRatingLogic.movieCert(listOf(3 to "", 4 to " ")))
    }

    @Test fun omdbRatedIsUsRating() {
        assertEquals("PG-13 (US)", AgeRatingLogic.omdb("PG-13", "DE"))
        assertEquals("PG-13", AgeRatingLogic.omdb("PG-13", "US"))
        assertNull(AgeRatingLogic.omdb("N/A", "DE"))
        assertNull(AgeRatingLogic.omdb("Not Rated", "DE"))
        assertNull(AgeRatingLogic.omdb(null, "DE"))
    }

    @Test fun minAgeReadsNumbersLabelsAndUsRatings() {
        assertEquals(16, AgeRatingLogic.minAge("FSK 16"))
        assertEquals(12, AgeRatingLogic.minAge("12"))
        assertEquals(16, AgeRatingLogic.minAge("16+"))
        assertEquals(12, AgeRatingLogic.minAge("12A"))
        assertEquals(18, AgeRatingLogic.minAge("TV-MA (US)"))
        assertEquals(14, AgeRatingLogic.minAge("TV-14"))
        assertEquals(6, AgeRatingLogic.minAge("tv-y7"))
        assertEquals(0, AgeRatingLogic.minAge("G"))
        assertNull(AgeRatingLogic.minAge("NR"))
        assertNull(AgeRatingLogic.minAge("Tous publics"))
        assertNull(AgeRatingLogic.minAge(null))
    }

    @Test fun highestOfSeveralRatingsOfOneCountry() {
        assertEquals("16", AgeRatingLogic.highest(listOf("12", "16", "6")))
        assertEquals("TV-MA", AgeRatingLogic.highest(listOf("TV-PG", "TV-MA")))
        assertEquals("12", AgeRatingLogic.highest(listOf("", "NR", "12")))
        assertNull(AgeRatingLogic.highest(listOf(" ", "N/A")))
    }

    @Test fun strictestTakesTheUsRatingWhenItIsHigher() {
        // South Park: FSK 12 in Germany, TV-MA in the US
        assertEquals(18, AgeRatingLogic.strictest(mapOf("DE" to "12", "US" to "TV-MA"), "DE"))
        assertEquals(6, AgeRatingLogic.strictest(mapOf("DE" to "6", "US" to "TV-Y"), "DE"))
        assertEquals(16, AgeRatingLogic.strictest(mapOf("RU" to "16+"), "RU"))
        assertEquals(AgeRatingLogic.UNKNOWN, AgeRatingLogic.strictest(mapOf("FR" to "10"), "DE"))
    }
}
