package com.johnson.sketchclock.common

import android.graphics.Bitmap
import android.graphics.Path
import android.graphics.Point
import androidx.core.graphics.alpha

object BoundaryParser {

    fun parseBoundary(bitmap: Bitmap): Path? {

        val nonTransparentMat = diffuse(bitmap)
        val edgePoints = squareTracking(nonTransparentMat)

        val path = Path().apply {
            moveTo(edgePoints.first().x.toFloat(), edgePoints.first().y.toFloat())
            edgePoints.forEach { point ->
                lineTo(point.x.toFloat(), point.y.toFloat())
            }
            close()
        }
        return path
    }

    private fun diffuse(bitmap: Bitmap, steps: Int = 5): Array<BooleanArray> {
        val pixels = IntArray(bitmap.width * bitmap.height)
        val distMat = Array(bitmap.height) { IntArray(bitmap.width) }
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        for (y in 0 until bitmap.height) {
            for (x in 0 until bitmap.width) {
                distMat[y][x] = when (pixels[y * bitmap.width + x].alpha) {
                    0 -> Int.MAX_VALUE
                    else -> 0
                }
            }
        }

        for (dist in 0 until steps - 1) {
            for (y in distMat.indices) {
                for (x in 0 until distMat[y].size) {
                    if (distMat[y][x] != Int.MAX_VALUE) continue

                    val left = if (x > 0) distMat[y][x - 1] else Int.MAX_VALUE
                    val top = if (y > 0) distMat[y - 1][x] else Int.MAX_VALUE
                    val right = if (x < distMat[y].size - 1) distMat[y][x + 1] else Int.MAX_VALUE
                    val bottom = if (y < distMat.size - 1) distMat[y + 1][x] else Int.MAX_VALUE

                    val minDist = minOf(left, top, right, bottom)
                    if (minDist == dist) {
                        distMat[y][x] = dist + 1
                    }
                }
            }
        }

        val mat = Array(bitmap.height) { BooleanArray(bitmap.width) }
        for (y in mat.indices) {
            for (x in 0 until mat[y].size) {
                mat[y][x] = distMat[y][x] != Int.MAX_VALUE
            }
        }

        return mat
    }

    private fun squareTracking(mat: Array<BooleanArray>): List<Point> {
        val width = mat[0].size
        val height = mat.size

        val pts = mutableListOf<Point>()
        val pt = Point(Int.MAX_VALUE, Int.MAX_VALUE)

        for (y in 0 until height) {
            for (x in 0 until width) {
                if (mat[y][x]) {
                    pt.set(x, y - 1)
                    break
                }
            }
            if (pt.x != Int.MAX_VALUE) break
        }

        var direction = Direction.DOWN

        val startPt = Point(pt)
        val startDirection = direction

        do {
            val hit = direction.of(mat, pt.x, pt.y) ?: false
            direction = if (hit) {
                pts.add(Point(pt))
                direction.turnRight()
            } else {
                pt.offset(direction)
                direction.turnLeft()
            }
            if (pts.size > 2000) {
                break
            }
        } while (pt != startPt || direction != startDirection)

        return pts
    }

    private fun Point.offset(direction: Direction) {
        when (direction) {
            Direction.UP -> offset(0, -1)
            Direction.LEFT -> offset(-1, 0)
            Direction.DOWN -> offset(0, 1)
            Direction.RIGHT -> offset(1, 0)
        }
    }

    private enum class Direction {
        UP, LEFT, DOWN, RIGHT;

        fun of(boundaryMat: Array<BooleanArray>, x: Int, y: Int): Boolean? {
            return when (this) {
                UP -> boundaryMat.getOrNull(y - 1)?.getOrNull(x)
                LEFT -> boundaryMat.getOrNull(y)?.getOrNull(x - 1)
                DOWN -> boundaryMat.getOrNull(y + 1)?.getOrNull(x)
                RIGHT -> boundaryMat.getOrNull(y)?.getOrNull(x + 1)
            }
        }

        fun turnLeft(): Direction {
            return when (this) {
                UP -> LEFT
                LEFT -> DOWN
                DOWN -> RIGHT
                RIGHT -> UP
            }
        }

        fun turnRight(): Direction {
            return when (this) {
                UP -> RIGHT
                LEFT -> UP
                DOWN -> LEFT
                RIGHT -> DOWN
            }
        }
    }
}