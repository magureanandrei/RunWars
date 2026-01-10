package tech.titans.runwars.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.SphericalUtil
import kotlin.math.abs
import org.locationtech.jts.geom.Coordinate as JtsCoordinate
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.Polygon
import org.locationtech.jts.operation.union.CascadedPolygonUnion
import tech.titans.runwars.models.Coordinates

object LocationUtils {
    private val geometryFactory = GeometryFactory()

    fun isClosedLoop(points: List<LatLng>): Boolean {
        if (points.size < 3) return false
        val distance = SphericalUtil.computeDistanceBetween(points.first(), points.last())
        return distance <= 50.0
    }

    fun calculateCapturedArea(points: List<LatLng>): Double {
        if (points.size < 3) return 0.0
        val closedPath = if (points.first() != points.last()) points + points.first() else points
        return abs(SphericalUtil.computeArea(closedPath))
    }

    // --- MATH ENGINE (Merge & Steal) ---

    fun mergeIfOverlapping(
        existingPath: List<Coordinates>,
        newPath: List<Coordinates>
    ): List<Coordinates>? {
        val poly1 = toJtsPolygon(existingPath) ?: return null
        val poly2 = toJtsPolygon(newPath) ?: return null

        try {
            if (!poly1.intersects(poly2)) return null

            val unionGeometry = poly1.union(poly2)
            return cleanAndExtractPolygon(unionGeometry)

        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    fun subtractPath(victimPath: List<Coordinates>, attackerPath: List<Coordinates>): List<Coordinates>? {
        val victimPoly = toJtsPolygon(victimPath) ?: return null
        val attackerPoly = toJtsPolygon(attackerPath) ?: return null

        try {
            val difference = victimPoly.difference(attackerPoly)

            // Handle Total Destruction
            if (difference.isEmpty) return emptyList()

            return cleanAndExtractPolygon(difference)

        } catch (e: Exception) {
            e.printStackTrace()
            return victimPath // Fail safe
        }
    }

    // --- HELPER FUNCTIONS ---

    private fun cleanAndExtractPolygon(geometry: Geometry): List<Coordinates>? {
        // If the result is multiple islands, keep the largest one
        if (geometry is org.locationtech.jts.geom.MultiPolygon || geometry is org.locationtech.jts.geom.GeometryCollection) {
            var maxArea = 0.0
            var largestPoly: Geometry? = null

            for (i in 0 until geometry.numGeometries) {
                val geom = geometry.getGeometryN(i)
                if (geom.area > maxArea && geom.area > 0.0000001) {
                    maxArea = geom.area
                    largestPoly = geom
                }
            }
            return largestPoly?.let { geometryToCoordinates(it) }
        }
        return geometryToCoordinates(geometry)
    }

    private fun toJtsPolygon(points: List<Coordinates>): Geometry? {
        // 1. CLEANING: Remove duplicate consecutive points (JTS hates them)
        val cleanedPoints = mutableListOf<Coordinates>()
        points.forEach { p ->
            if (cleanedPoints.isEmpty() ||
                (cleanedPoints.last().latitude != p.latitude || cleanedPoints.last().longitude != p.longitude)) {
                cleanedPoints.add(p)
            }
        }

        if (cleanedPoints.size < 3) return null

        // 2. CLOSURE: Ensure loop is closed
        val closedPoints = if (cleanedPoints.first().latitude != cleanedPoints.last().latitude ||
            cleanedPoints.first().longitude != cleanedPoints.last().longitude) {
            cleanedPoints + cleanedPoints.first()
        } else {
            cleanedPoints
        }

        val jtsCoords = closedPoints.map {
            JtsCoordinate(it.longitude, it.latitude)
        }.toTypedArray()

        return try {
            val rawPolygon = geometryFactory.createPolygon(jtsCoords)

            // 3. VALIDATION: Fix self-intersections (bow-ties) with buffer(0)
            if (!rawPolygon.isValid) {
                return rawPolygon.buffer(0.0)
            }
            rawPolygon
        } catch (e: Exception) {
            null
        }
    }

    private fun geometryToCoordinates(geometry: Geometry): List<Coordinates> {
        return geometry.coordinates.map {
            Coordinates(latitude = it.y, longitude = it.x)
        }
    }

    fun unifyTerritories(rawTerritories: List<List<LatLng>>): List<List<LatLng>> {
        if (rawTerritories.isEmpty()) return emptyList()

        val polygons = rawTerritories.mapNotNull { path ->
            val coords = path.map { Coordinates(it.latitude, it.longitude) }
            toJtsPolygon(coords)
        }

        if (polygons.isEmpty()) return emptyList()

        try {
            val unionGeometry = CascadedPolygonUnion.union(polygons)
            val resultList = mutableListOf<List<LatLng>>()

            fun extract(geom: Geometry) {
                if (geom is Polygon) {
                    resultList.add(geom.coordinates.map { LatLng(it.y, it.x) })
                } else if (geom is org.locationtech.jts.geom.GeometryCollection) {
                    for (i in 0 until geom.numGeometries) extract(geom.getGeometryN(i))
                }
            }
            extract(unionGeometry)
            return resultList
        } catch (e: Exception) {
            return rawTerritories
        }
    }

    // --- UI HELPERS ---

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

        paint.color = android.graphics.Color.parseColor("#40000000")
        canvas.drawCircle(centerX, size * 0.85f, 35f, paint)

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

        paint.color = android.graphics.Color.parseColor("#FFD8B4")
        canvas.drawCircle(centerX, centerY - 25, 18f, paint)

        paint.style = android.graphics.Paint.Style.STROKE
        paint.strokeWidth = 2f
        paint.color = android.graphics.Color.parseColor("#D4A574")
        canvas.drawCircle(centerX, centerY - 25, 18f, paint)
        paint.style = android.graphics.Paint.Style.FILL

        paint.color = android.graphics.Color.parseColor("#1565C0")
        paint.strokeWidth = 10f
        paint.strokeCap = android.graphics.Paint.Cap.ROUND
        paint.style = android.graphics.Paint.Style.STROKE

        canvas.drawLine(centerX - 15, centerY, centerX - 35, centerY + 25, paint)
        canvas.drawLine(centerX + 15, centerY, centerX + 30, centerY - 15, paint)

        paint.color = android.graphics.Color.parseColor("#424242")
        paint.strokeWidth = 12f
        canvas.drawLine(centerX - 5, centerY + 35, centerX - 15, centerY + 60, paint)
        canvas.drawLine(centerX - 15, centerY + 60, centerX - 10, centerY + 75, paint)
        canvas.drawLine(centerX + 5, centerY + 35, centerX + 20, centerY + 50, paint)
        canvas.drawLine(centerX + 20, centerY + 50, centerX + 30, centerY + 60, paint)

        paint.style = android.graphics.Paint.Style.FILL
        paint.color = android.graphics.Color.parseColor("#E53935")
        canvas.drawCircle(centerX - 10, centerY + 75, 8f, paint)
        canvas.drawCircle(centerX + 30, centerY + 60, 8f, paint)

        if (running) {
            paint.color = android.graphics.Color.parseColor("#4CAF50")
            paint.alpha = 180
            canvas.drawCircle(centerX, centerY - 25, 25f, paint)
            paint.alpha = 255
            canvas.drawCircle(centerX, centerY - 25, 20f, paint)
        }
        return bitmap
    }

    fun findLargestLoop(points: List<LatLng>, thresholdMeters: Double = 30.0): List<LatLng>? {
        if (points.size < 10) return null
        var maxArea = 0.0
        var bestLoop: List<LatLng>? = null
        for (i in 0 until points.size - 5) {
            for (j in (i + 5) until points.size) {
                val start = points[i]
                val end = points[j]
                val distance = SphericalUtil.computeDistanceBetween(start, end)
                if (distance < thresholdMeters) {
                    val loopCandidate = points.subList(i, j + 1)
                    val area = SphericalUtil.computeArea(loopCandidate)
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