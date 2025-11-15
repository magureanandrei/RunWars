package tech.titans.runwars.models

import androidx.compose.ui.graphics.Path
import com.google.android.gms.maps.model.LatLng
import java.sql.Time
import kotlin.time.Duration

data class RunSession(
    val runId: String = "",
    val startTime: Long = 0L,
    val stopTime: Long = 0L,
    val distance: Int = 0,
    val duration: Long = 0L,
    val coordinatesList: MutableList<Coordinates> = mutableListOf()
)
