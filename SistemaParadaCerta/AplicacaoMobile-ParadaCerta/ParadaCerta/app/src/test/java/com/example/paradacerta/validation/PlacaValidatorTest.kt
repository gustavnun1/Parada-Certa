package com.example.paradacerta.validation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlacaValidatorTest {

    @Test
    fun aceitaPlacaAntigaComOuSemHifen() {
        assertTrue(PlacaValidator.isValida("ABC1234"))
        assertTrue(PlacaValidator.isValida("ABC-1234"))
        assertEquals("ABC1234", PlacaValidator.normalizar("abc-1234"))
    }

    @Test
    fun aceitaPlacaMercosul() {
        assertTrue(PlacaValidator.isValida("ABC1D23"))
        assertEquals("ABC1D23", PlacaValidator.normalizar("abc1d23"))
    }

    @Test
    fun rejeitaFormatosInvalidos() {
        assertFalse(PlacaValidator.isValida("ABCDE12"))
        assertFalse(PlacaValidator.isValida("ABC12D3"))
        assertFalse(PlacaValidator.isValida("1234ABC"))
        assertFalse(PlacaValidator.isValida("ABC123"))
    }
}
