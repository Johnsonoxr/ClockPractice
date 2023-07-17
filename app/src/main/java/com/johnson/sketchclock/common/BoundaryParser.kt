package com.johnson.sketchclock.common

import android.graphics.Bitmap
import android.graphics.Path
import android.graphics.PointF
import android.util.Log
import androidx.core.graphics.alpha
import kotlin.math.hypot

object BoundaryParser {

    fun parseBoundary(bitmap: Bitmap): Path? {

        val distMat = Array(bitmap.height) { IntArray(bitmap.width) }
        val pixels = IntArray(bitmap.height * bitmap.width).apply { bitmap.getPixels(this, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height) }

        for (y in 0 until bitmap.height) {
            for (x in 0 until bitmap.width) {
                val pixel = pixels[y * bitmap.width + x]
                distMat[y][x] = if (pixel.alpha != 0) 0 else Int.MAX_VALUE
            }
        }

        fillDistanceMapSteps(distMat, 4)
        val points = getPoints(distMat, 4)
        return linkPointsToPath(points)
    }

    private fun fillDistanceMapSteps(intArray: Array<IntArray>, distance: Int) {
        for (dist in 0 until distance) {
            for (y in intArray.indices) {
                for (x in 0 until intArray[y].size) {
                    if (intArray[y][x] != Int.MAX_VALUE) continue

                    if (x > 0 && intArray[y][x - 1] == dist
                        || y > 0 && intArray[y - 1][x] == dist
                        || x < intArray[y].size - 1 && intArray[y][x + 1] == dist
                        || y < intArray.size - 1 && intArray[y + 1][x] == dist
                    ) {
                        intArray[y][x] = dist + 1
                    }
                }
            }
        }
    }

    private fun getPoints(distMat: Array<IntArray>, targetDist: Int): List<PointF> {
        val points = mutableListOf<PointF>()
        for (y in distMat.indices) {
            for (x in 0 until distMat[y].size) {
                if (distMat[y][x] == targetDist) {
                    points.add(PointF(x.toFloat(), y.toFloat()))
                }
            }
        }
        return points
    }

    private fun linkPointsToPath(points: List<PointF>): Path {
        val path = Path()
        if (points.isEmpty()) return path
        val pts = points.toMutableList()

        while (pts.isNotEmpty()) {
            val p = Path()

            val firstPoint = pts.removeAt(0)
            var pt = firstPoint
            p.moveTo(firstPoint.x, firstPoint.y)

            while (pts.isNotEmpty()) {
                val nextPoint = pts.minBy { hypot(pt.x - it.x, pt.y - it.y) }
                pts.remove(nextPoint)
                p.lineTo(nextPoint.x, nextPoint.y)
                pt = nextPoint
            }

            p.close()

//            if (pts.isEmpty()) {
//                path.addPath(p)
//                break
//            }

            path.addPath(p)
        }

        return path
    }
}