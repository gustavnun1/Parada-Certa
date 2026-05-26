package com.example.paradacerta.screens.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VehicleBrandsTest {

    @Test
    fun listaDeMarcasPermaneceOrdenadaParaOsSelectsDoApp() {
        assertEquals(VEHICLE_BRANDS.sorted(), VEHICLE_BRANDS)
    }

    @Test
    fun listaContemMarcasUsadasNoCadastroDeVeiculos() {
        assertTrue(VEHICLE_BRANDS.contains("Chevrolet"))
        assertTrue(VEHICLE_BRANDS.contains("Honda"))
        assertTrue(VEHICLE_BRANDS.contains("Toyota"))
        assertTrue(VEHICLE_BRANDS.contains("Volkswagen"))
    }
}
