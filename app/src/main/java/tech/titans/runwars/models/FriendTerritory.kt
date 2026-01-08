package tech.titans.runwars.models

import com.google.android.gms.maps.model.LatLng

/**
 * Represents a friend's territory to be displayed on the map
 */
data class FriendTerritory(
    val friendId: String,
    val friendName: String,
    val colorIndex: Int, // Index into the color palette
    val territories: List<List<LatLng>> // List of territory polygons
)

/**
 * Color palette for friend territories
 */
object FriendTerritoryColors {
    // Semi-transparent fill colors (ARGB)
    val fillColors = listOf(
        0x400000FF, // Blue
        0x40FF00FF, // Magenta
        0x40FF6600, // Orange
        0x40FFFF00, // Yellow
        0x4000FFFF, // Cyan
        0x40FF0066, // Pink
        0x409900FF, // Purple
        0x4000FF66  // Teal
    )

    // Solid stroke colors (ARGB)
    val strokeColors = listOf(
        0xFF0000AA.toInt(), // Blue
        0xFFAA00AA.toInt(), // Magenta
        0xFFCC5500.toInt(), // Orange
        0xFFAAAA00.toInt(), // Yellow
        0xFF00AAAA.toInt(), // Cyan
        0xFFAA0044.toInt(), // Pink
        0xFF6600AA.toInt(), // Purple
        0xFF00AA44.toInt()  // Teal
    )

    fun getFillColor(index: Int): Int = fillColors[index % fillColors.size]
    fun getStrokeColor(index: Int): Int = strokeColors[index % strokeColors.size]
}

