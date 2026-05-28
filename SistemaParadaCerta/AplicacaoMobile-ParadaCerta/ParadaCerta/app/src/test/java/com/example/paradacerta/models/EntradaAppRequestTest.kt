package com.example.paradacerta.models

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Test

class EntradaAppRequestTest {

    @Test
    fun serializaCamposEsperadosPelaApi() {
        val request = EntradaAppRequest(
            cpfUsuario = "12345678909",
            estacionamentoId = 10,
            placa = "ABC1D23",
            horaEntradaDispositivoMs = 1_771_957_700_000L
        )

        val json = Gson().toJson(request)

        assertEquals(
            """{"cpfUsuario":"12345678909","estacionamentoId":10,"placa":"ABC1D23","horaEntradaDispositivoMs":1771957700000}""",
            json
        )
    }
}
