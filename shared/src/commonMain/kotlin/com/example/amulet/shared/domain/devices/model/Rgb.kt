package com.example.amulet.shared.domain.devices.model

data class Rgb(
    val red: Int,
    val green: Int,
    val blue: Int
) {
    init {
        require(red in 0..255) { "Red must be in 0..255" }
        require(green in 0..255) { "Green must be in 0..255" }
        require(blue in 0..255) { "Blue must be in 0..255" }
    }
    
    fun toHex(): String = "#%02X%02X%02X".format(red, green, blue)
    
    companion object {
        fun fromHex(hex: String): Rgb {
            val cleanHex = hex.removePrefix("#")
            require(cleanHex.length == 6) { "Hex color must be 6 characters" }
            
            return Rgb(
                red = cleanHex.substring(0, 2).toInt(16),
                green = cleanHex.substring(2, 4).toInt(16),
                blue = cleanHex.substring(4, 6).toInt(16)
            )
        }
    }
}
