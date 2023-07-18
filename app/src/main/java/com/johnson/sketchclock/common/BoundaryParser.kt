package com.johnson.sketchclock.common

import android.graphics.Bitmap
import android.graphics.Path
import android.graphics.Point
import androidx.core.graphics.alpha

private const val TAG = "BoundaryParser"

object BoundaryParser {

    fun parseBoundary(bitmap: Bitmap): Path {
        val pathCollection = Path()
        val edgeMask = Array(bitmap.height) { BooleanArray(bitmap.width) }

        val nonTransparentMat: Array<BooleanArray> = diffuse(bitmap)

        while (true) {
            val edgePoints: List<Point> = squareTracking(nonTransparentMat, edgeMask)

            if (edgePoints.isEmpty()) {
                break
            }

            edgePoints.forEach { point ->
                edgeMask[point.y][point.x] = true
            }

            val path = Path().apply {
                moveTo(edgePoints.first().x.toFloat(), edgePoints.first().y.toFloat())
                edgePoints.forEach { point ->
                    lineTo(point.x.toFloat(), point.y.toFloat())
                }
                close()
            }
            pathCollection.addPath(path)
        }

        return pathCollection
    }

    private fun diffuse(bitmap: Bitmap, steps: Int = 5): Array<BooleanArray> {
        val width = bitmap.width
        val height = bitmap.height

        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        for (i in pixels.indices) {
            pixels[i] = when (pixels[i].alpha) {
                0 -> Int.MAX_VALUE
                else -> 0
            }
        }

        for (step in 0 until steps) {
            for (y in 0 until height) {
                for (x in 0 until width) {
                    val index = y * width + x
                    if (pixels[index] != Int.MAX_VALUE) continue

                    if (x > 0 && pixels[index - 1] == step
                        || y > 0 && pixels[index - width] == step
                        || x < width - 1 && pixels[index + 1] == step
                        || y < height - 1 && pixels[index + width] == step
                    ) {
                        pixels[index] = step + 1
                    }
                }
            }
        }

        val mat = Array(bitmap.height) { BooleanArray(bitmap.width) }
        for (y in mat.indices) {
            for (x in 0 until mat[y].size) {
                mat[y][x] = pixels[y * width + x] != Int.MAX_VALUE
            }
        }

        return mat
    }

    /**
     * @param mat The boolean 2d array to perform square tracking on
     * @param invalidStartPointMask The boolean 2d array to use as a mask for the starting point
     */
    private fun squareTracking(mat: Array<BooleanArray>, invalidStartPointMask: Array<BooleanArray>): List<Point> {
        val width = mat[0].size
        val height = mat.size

        val pts = mutableListOf<Point>()
        val pt = Point(Int.MAX_VALUE, Int.MAX_VALUE)

        for (y in -1 until height - 1) {
            val row = mat.getOrNull(y)
            val row1 = mat[y + 1]
            for (x in 0 until width) {
                if (row1[x] && row?.get(x) != true && !invalidStartPointMask[y][x]) {
                    pt.set(x, y)
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