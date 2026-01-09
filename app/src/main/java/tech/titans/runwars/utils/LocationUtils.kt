package tech.titans.runwars.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.SphericalUtil
import kotlin.math.abs

object LocationUtils {
    fun isClosedLoop(points: List<LatLng>): Boolean {
        if (points.size < 3) return false

        val startPoint = points.first()
        val endPoint = points.last()

        // Calculate distance between start and end (in meters)
        val distance = SphericalUtil.computeDistanceBetween(startPoint, endPoint)

        // Consider it a loop if start and end are within 50 meters
        println("🔄 Loop check: distance between start/end = $distance meters")
        return distance <= 50.0
    }

    // Calculate territory area (closing the polygon)
    fun calculateCapturedArea(points: List<LatLng>): Double {
        if (points.size < 3) return 0.0

        if (!isClosedLoop(points)) return 0.0

        // Close the polygon by adding first point at the end
        val closedPath = if (points.first() != points.last()) {
            points + points.first()
        } else {
            points
        }

        return abs(SphericalUtil.computeArea(closedPath))
    }

    // Create custom marker icon for user position - running figure
    fun createUserMarkerBitmap(running: Boolean): Bitmap {
        val size = 140
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val paint = android.graphics.Paint().apply {
            isAntiAlias = true
            style = android.graphics.Paint.Style.FILL
        }

        val centerX = size / 2f
        val centerY = size / 2f

        // Shadow
        paint.color = android.graphics.Color.parseColor("#40000000")
        canvas.drawCircle(centerX, size * 0.85f, 35f, paint)

        // Body (main torso) - blue
        paint.color = android.graphics.Color.parseColor("#1E88E5")
        val bodyPath = android.graphics.Path().apply {
            moveTo(centerX, centerY - 10)
            lineTo(centerX + 20, centerY + 15)
            lineTo(centerX + 15, centerY + 35)
            lineTo(centerX - 15, centerY + 35)
            lineTo(centerX - 20, centerY + 15)
            close()
        }
        canvas.drawPath(bodyPath, paint)

        // Head
        paint.color = android.graphics.Color.parseColor("#FFD8B4")
        canvas.drawCircle(centerX, centerY - 25, 18f, paint)

        // Head outline
        paint.style = android.graphics.Paint.Style.STROKE
        paint.strokeWidth = 2f
        paint.color = android.graphics.Color.parseColor("#D4A574")
        canvas.drawCircle(centerX, centerY - 25, 18f, paint)
        paint.style = android.graphics.Paint.Style.FILL

        // Arms (running pose)
        paint.color = android.graphics.Color.parseColor("#1565C0")
        paint.strokeWidth = 10f
        paint.strokeCap = android.graphics.Paint.Cap.ROUND
        paint.style = android.graphics.Paint.Style.STROKE

        // Left arm (back)
        canvas.drawLine(centerX - 15, centerY, centerX - 35, centerY + 25, paint)

        // Right arm (forward)
        canvas.drawLine(centerX + 15, centerY, centerX + 30, centerY - 15, paint)

        // Legs (running pose)
        paint.color = android.graphics.Color.parseColor("#424242")
        paint.strokeWidth = 12f

        // Left leg (forward)
        canvas.drawLine(centerX - 5, centerY + 35, centerX - 15, centerY + 60, paint)
        canvas.drawLine(centerX - 15, centerY + 60, centerX - 10, centerY + 75, paint)

        // Right leg (back)
        canvas.drawLine(centerX + 5, centerY + 35, centerX + 20, centerY + 50, paint)
        canvas.drawLine(centerX + 20, centerY + 50, centerX + 30, centerY + 60, paint)

        // Shoes
        paint.style = android.graphics.Paint.Style.FILL
        paint.color = android.graphics.Color.parseColor("#E53935")
        canvas.drawCircle(centerX - 10, centerY + 75, 8f, paint)
        canvas.drawCircle(centerX + 30, centerY + 60, 8f, paint)

        // Running indicator (green pulse if running)
        if (running) {
            paint.color = android.graphics.Color.parseColor("#4CAF50")
            paint.alpha = 180
            canvas.drawCircle(centerX, centerY - 25, 25f, paint)

            paint.alpha = 255
            canvas.drawCircle(centerX, centerY - 25, 20f, paint)
        }

        return bitmap
    }

    /**
     * Scans the path for the largest closed loop.
     * Returns the list of points forming that loop, or null if no loop is found.
     */
    fun findLargestLoop(points: List<LatLng>, thresholdMeters: Double = 30.0): List<LatLng>? {
        if (points.size < 10) return null // Need enough points to form a loop

        var maxArea = 0.0
        var bestLoop: List<LatLng>? = null

        // Scan for loops
        // We look for a point 'j' that is close to an earlier point 'i'
        for (i in 0 until points.size - 5) {
            // Optimization: Don't check every single point against every other if list is huge.
            // But for typical runs (<10k points), this O(N^2) is fine on modern phones.

            for (j in (i + 5) until points.size) {
                val start = points[i]
                val end = points[j]

                val distance = SphericalUtil.computeDistanceBetween(start, end)

                if (distance < thresholdMeters) {
                    // Found a closure! Extract the loop.
                    val loopCandidate = points.subList(i, j + 1)

                    // Check if this loop is valid (e.g. not just pacing back and forth)
                    // We can check area
                    val area = SphericalUtil.computeArea(loopCandidate)

                    // Filter out tiny "glitch" loops (e.g. < 100 sqm)
                    if (area > 100.0 && area > maxArea) {
                        maxArea = area
                        bestLoop = loopCandidate
                    }
                }
            }
        }

        return bestLoop
    }
}