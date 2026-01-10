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

    /**
     * COOKIE CUTTER LOGIC:
     * Subtracts the attacker's path from the victim's path.
     * Returns the largest remaining chunk of the victim's territory.
     * Returns NULL if the victim was completely wiped out.
     */
    fun subtractPath(victimPath: List<Coordinates>, attackerPath: List<Coordinates>): List<Coordinates>? {
        // Convert to JTS Geometry
        val victimPoly = toJtsPolygon(victimPath) ?: return null
        val attackerPoly = toJtsPolygon(attackerPath) ?: return null

        try {
            // Perform the math: Victim - Attacker
            val difference = victimPoly.difference(attackerPoly)

            if (difference.isEmpty) return emptyList() // Victim was completely destroyed!

            // If the result is split into islands (MultiPolygon), we keep the largest one
            // to fit your current data model (which supports 1 list of coords).
            if (difference is org.locationtech.jts.geom.MultiPolygon || difference is org.locationtech.jts.geom.GeometryCollection) {
                var maxArea = 0.0
                var largestPoly: Geometry? = null

                for (i in 0 until difference.numGeometries) {
                    val geom = difference.getGeometryN(i)
                    if (geom.area > maxArea) {
                        maxArea = geom.area
                        largestPoly = geom
                    }
                }
                return largestPoly?.let { geometryToCoordinates(it) }
            }

            // If it's a simple Polygon, just return it
            return geometryToCoordinates(difference)

        } catch (e: Exception) {
            e.printStackTrace()
            return victimPath // If math fails, fail safe (don't delete their land)
        }
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

    /**
     * Checks if two paths overlap and returns the merged path if they do.
     * Returns NULL if they do not intersect.
     */
    fun mergeIfOverlapping(
        existingPath: List<Coordinates>,
        newPath: List<Coordinates>
    ): List<Coordinates>? {
        val poly1 = toJtsPolygon(existingPath) ?: return null
        val poly2 = toJtsPolygon(newPath) ?: return null

        try {
            // Check intersection (Safety check)
            if (!poly1.intersects(poly2)) {
                return null
            }

            // Calculate Union
            // This is where it usually crashes on "Figure 8" runs without a try-catch
            val unionGeometry = poly1.union(poly2)

            // HANDLE MULTI-POLYGONS (Islands)
            // If the merge creates two islands (or error artifacts), keep the largest one.
            if (unionGeometry is org.locationtech.jts.geom.MultiPolygon ||
                unionGeometry is org.locationtech.jts.geom.GeometryCollection) {

                var maxArea = 0.0
                var largestPoly: Geometry? = null

                for (i in 0 until unionGeometry.numGeometries) {
                    val geom = unionGeometry.getGeometryN(i)
                    // Filter out tiny artifacts (noise)
                    if (geom.area > maxArea && geom.area > 0.0000001) {
                        maxArea = geom.area
                        largestPoly = geom
                    }
                }
                return largestPoly?.let { geometryToCoordinates(it) }
            }

            // Convert back to our Coordinate list
            return geometryToCoordinates(unionGeometry)

        } catch (e: Exception) {
            // If JTS crashes (TopologyException), we Log it and return NULL
            // This prevents the App Crash and just saves the run separately instead of merging.
            e.printStackTrace()
            return null
        }
    }

    private fun toJtsPolygon(points: List<Coordinates>): Geometry? {
        if (points.size < 3) return null

        // JTS requires the loop to be closed (first point == last point)
        val closedPoints = if (points.first() != points.last()) {
            points + points.first()
        } else {
            points
        }

        val jtsCoords = closedPoints.map {
            JtsCoordinate(it.longitude, it.latitude) // JTS uses (x, y) -> (lng, lat)
        }.toTypedArray()

        return try {
            // 1. Create the raw polygon (might be invalid/self-intersecting)
            val rawPolygon = geometryFactory.createPolygon(jtsCoords)

            // 2. THE FIX: If it's invalid (like a figure-8), buffer(0) fixes it.
            if (!rawPolygon.isValid) {
                return rawPolygon.buffer(0.0)
            }

            rawPolygon
        } catch (e: Exception) {
            // If it's so broken even JTS can't read it, return null to prevent crash
            return null
        }
    }

    private fun geometryToCoordinates(geometry: Geometry): List<Coordinates> {
        // Handle MultiPolygons (if merge creates islands) by taking the largest/convex hull
        // or just simplifying. For this implementation, we take the boundary coordinates.
        return geometry.coordinates.map {
            Coordinates(latitude = it.y, longitude = it.x)
        }
    }

    /**
     * Takes a list of raw territory paths (which might overlap)
     * and returns a clean list of non-overlapping polygons for display.
     */
    fun unifyTerritories(rawTerritories: List<List<LatLng>>): List<List<LatLng>> {
        if (rawTerritories.isEmpty()) return emptyList()

        val polygons = rawTerritories.mapNotNull { path ->
            // Reuse your existing conversion logic (LatLong -> JTS Coordinate)
            // Ensure you map LatLng(lat, lng) to JTS(x=lng, y=lat)
            val coords = path.map {
                JtsCoordinate(it.longitude, it.latitude)
            }.toMutableList()

            // Close the loop if needed
            if (coords.isNotEmpty() && coords.first() != coords.last()) {
                coords.add(coords.first())
            }

            try {
                if (coords.size >= 4) // JTS needs at least 4 coords for a valid LinearRing (A-B-C-A)
                    geometryFactory.createPolygon(coords.toTypedArray())
                else null
            } catch (e: Exception) { null }
        }

        if (polygons.isEmpty()) return emptyList()

        // Efficiently union all polygons
        val unionGeometry = CascadedPolygonUnion.union(polygons)

        // Convert back to List<List<LatLng>> for Compose
        val resultList = mutableListOf<List<LatLng>>()

        fun extractPolygons(geom: Geometry) {
            if (geom is Polygon) {
                val path = geom.coordinates.map { LatLng(it.y, it.x) } // Map back: y=lat, x=lng
                resultList.add(path)
            } else if (geom is org.locationtech.jts.geom.MultiPolygon) {
                for (i in 0 until geom.numGeometries) {
                    extractPolygons(geom.getGeometryN(i))
                }
            } else if (geom is org.locationtech.jts.geom.GeometryCollection) {
                for (i in 0 until geom.numGeometries) {
                    extractPolygons(geom.getGeometryN(i))
                }
            }
        }

        extractPolygons(unionGeometry)
        return resultList
    }
}