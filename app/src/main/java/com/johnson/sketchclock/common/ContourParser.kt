package com.johnson.sketchclock.common

import android.graphics.Bitmap
import android.graphics.Path
import android.graphics.Point
import androidx.core.graphics.alpha
import com.google.gson.Gson
import java.io.File

private const val TAG = "ContourParser"

private val gson = Gson()

fun File.contourFile(): File {
    if (this.endsWith("_contour.txt")) {
        throw IllegalArgumentException("File name should not end with _contour.txt")
    }
    return File(parentFile, "${nameWithoutExtension}_contour.txt")
}

fun File.saveContour(bitmap: Bitmap) {
    val contour = ContourParser.parseContour(bitmap)
    this.contourFile().writeText(gson.toJson(contour))
}

fun File.loadContour(): List<IntArray>? {
    return this.contourFile().takeIf { it.exists() }
        ?.readText()
        ?.let { gson.fromJson(it, Array<IntArray>::class.java).toList() }
}

fun File.loadContourPath(): Path? {
    return loadContour()?.let { contourList -> contourToPath(contourList) }
}

private fun contourToPath(contourList: List<IntArray>): Path {
    val allPath = Path()
    contourList.forEach { contour ->
        val path = Path()
        for (i in contour.indices step 2) {
            val x = contour[i]
            val y = contour[i + 1]
            if (path.isEmpty) {
                path.moveTo(x.toFloat(), y.toFloat())
            } else {
                path.lineTo(x.toFloat(), y.toFloat())
            }
        }
        path.close()
        allPath.addPath(path)
    }
    return allPath
}

private const val DISTANCE_TOLERANCE = .5f

object ContourParser {

    fun parseContourPath(bitmap: Bitmap): Path {
        val contourList = parseContour(bitmap)
        return contourToPath(contourList)
    }

    fun parseContour(bitmap: Bitmap): List<IntArray> {
        val edgeMask = Array(bitmap.height) { BooleanArray(bitmap.width) }
        val nonTransparentMat: Array<BooleanArray> = diffuse(bitmap)

        val contourList = mutableListOf<IntArray>()

        do {
            val contour = squareTracking(nonTransparentMat, edgeMask) ?: break
            if (contour.isNotEmpty()) {
                contourList.add(contour)
            }
        } while (contour.isNotEmpty())

        return contourList
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
     * @param contourMask The boolean 2d array that marks the existing contour pixels
     */
    private fun squareTracking(mat: Array<BooleanArray>, contourMask: Array<BooleanArray>): IntArray? {
        val width = mat[0].size
        val height = mat.size

        val pts = mutableListOf<Point>()
        val pt = Point(Int.MAX_VALUE, Int.MAX_VALUE)

        for (y in 0 until height) {
            val rowAbove = mat.getOrNull(y - 1)
            val row = mat[y]
            val contourMaskRow = contourMask[y]
            for (x in 0 until width) {
                if (row[x] && rowAbove?.get(x) != true && !contourMaskRow[x]) {
                    pt.set(x, y)
                    break
                }
            }
            if (pt.x != Int.MAX_VALUE) break
        }
        if (pt.x == Int.MAX_VALUE) return null

        var direction = Direction.UP

        val startPt = Point(pt)
        val startDirection = direction

        do {
            val outOfBoundary = direction.of(mat, pt.x, pt.y) != true
            direction = if (outOfBoundary) {
                contourMask[pt.y][pt.x] = true
                pts.add(Point(pt))
                direction.turnLeft()
            } else {
                pt.offset(direction)
                direction.turnRight()
            }
        } while (pt != startPt || direction != startDirection)

        val arr = IntArray(pts.size * 2)
        pts.forEachIndexed { index, point ->
            arr[index * 2] = point.x
            arr[index * 2 + 1] = point.y
        }
        return arr
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

        fun of(contourMat: Array<BooleanArray>, x: Int, y: Int): Boolean? {
            return when (this) {
                UP -> contourMat.getOrNull(y - 1)?.getOrNull(x)
                LEFT -> contourMat.getOrNull(y)?.getOrNull(x - 1)
                DOWN -> contourMat.getOrNull(y + 1)?.getOrNull(x)
                RIGHT -> contourMat.getOrNull(y)?.getOrNull(x + 1)
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