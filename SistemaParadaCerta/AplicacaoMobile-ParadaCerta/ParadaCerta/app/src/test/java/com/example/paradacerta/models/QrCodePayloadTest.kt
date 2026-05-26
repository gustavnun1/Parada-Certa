package com.example.paradacerta.models

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class QrCodePayloadTest {

    private val gson = Gson()

    @Test
    fun parseKioskEntryPayload() {
        val json = """
            {"v":1,"app":"paradacerta","type":"entrada","estacionamentoId":42,"token":"abc-123"}
        """.trimIndent()

        val payload = gson.fromJson(json, QrCodePayload::class.java)

        assertEquals("entrada", payload.type)
        assertEquals(42, payload.estacionamentoId)
        assertEquals("abc-123", payload.token)
        assertNull(payload.tipo)
    }

    @Test
    fun parseLegacyEntryPayload() {
        val json = """
            {"tipo":"ENTRADA","id":7,"nome":"Centro Park","precoHora":12.5}
        """.trimIndent()

        val payload = gson.fromJson(json, QrCodePayload::class.java)

        assertEquals("ENTRADA", payload.tipo)
        assertEquals(7, payload.id)
        assertEquals("Centro Park", payload.nome)
        assertEquals(12.5, payload.precoHora ?: 0.0, 0.0)
    }

    @Test
    fun parsePaymentPayload() {
        val json = """
            {"tipo":"PAGAMENTO","sessaoId":"sessao-1","valor":25.5,"nome":"Centro Park","pixKey":"pix@paradacerta.com"}
        """.trimIndent()

        val payload = gson.fromJson(json, QrCodePayload::class.java)

        assertEquals("PAGAMENTO", payload.tipo)
        assertEquals("sessao-1", payload.sessaoId)
        assertEquals(25.5, payload.valor ?: 0.0, 0.0)
        assertEquals("pix@paradacerta.com", payload.pixKey)
    }
}
