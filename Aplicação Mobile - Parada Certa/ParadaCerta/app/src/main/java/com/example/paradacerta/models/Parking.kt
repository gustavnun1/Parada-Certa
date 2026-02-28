package com.example.paradacerta.models

/**
 * Modelo de dados para representar um estacionamento
 */
data class Parking(
    val id: Int,
    val name: String,
    val address: String,
    val pricePerHour: Double,
    val availableSpots: Int,
    val totalSpots: Int,
    val rating: Double,
    val distanceKm: Double,
    val hasPromo: Boolean = false,
    val promoText: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    var isFavorite: Boolean = false
)

/**
 * Dados mockados de estacionamentos
 */
object MockParkingData {
    val parkingList = listOf(
        Parking(
            id = 1,
            name = "Estacionamento Center Park",
            address = "Av. Paulista, 1500 - Bela Vista, São Paulo",
            pricePerHour = 12.50,
            availableSpots = 45,
            totalSpots = 100,
            rating = 4.5,
            distanceKm = 0.8,
            hasPromo = true,
            promoText = "50% OFF na primeira hora",
            latitude = -23.561414,
            longitude = -46.656139
        ),
        Parking(
            id = 2,
            name = "Park & Go Shopping",
            address = "Rua Augusta, 2690 - Jardins, São Paulo",
            pricePerHour = 15.00,
            availableSpots = 23,
            totalSpots = 80,
            rating = 4.8,
            distanceKm = 1.2,
            hasPromo = false,
            latitude = -23.558890,
            longitude = -46.659878
        ),
        Parking(
            id = 3,
            name = "Estacionamento Rápido 24h",
            address = "Rua da Consolação, 3000 - Consolação, São Paulo",
            pricePerHour = 10.00,
            availableSpots = 67,
            totalSpots = 120,
            rating = 4.2,
            distanceKm = 0.5,
            hasPromo = true,
            promoText = "Diária por R$ 50,00",
            latitude = -23.553890,
            longitude = -46.661234
        ),
        Parking(
            id = 4,
            name = "Super Vagas Centro",
            address = "Rua Barão de Itapetininga, 255 - República, São Paulo",
            pricePerHour = 8.50,
            availableSpots = 12,
            totalSpots = 60,
            rating = 4.0,
            distanceKm = 1.5,
            hasPromo = false,
            latitude = -23.543567,
            longitude = -46.642345
        ),
        Parking(
            id = 5,
            name = "Garagem Premium Jardins",
            address = "Alameda Santos, 1000 - Jardim Paulista, São Paulo",
            pricePerHour = 18.00,
            availableSpots = 8,
            totalSpots = 40,
            rating = 4.9,
            distanceKm = 2.1,
            hasPromo = false,
            latitude = -23.567123,
            longitude = -46.654321
        ),
        Parking(
            id = 6,
            name = "Park Express Vila Mariana",
            address = "Rua Domingos de Morais, 2564 - Vila Mariana, São Paulo",
            pricePerHour = 9.00,
            availableSpots = 34,
            totalSpots = 70,
            rating = 4.3,
            distanceKm = 3.2,
            hasPromo = true,
            promoText = "R$ 5,00 até às 10h",
            latitude = -23.588456,
            longitude = -46.638567
        )
    )

    fun getParkingById(id: Int): Parking? {
        return parkingList.find { it.id == id }
    }
}