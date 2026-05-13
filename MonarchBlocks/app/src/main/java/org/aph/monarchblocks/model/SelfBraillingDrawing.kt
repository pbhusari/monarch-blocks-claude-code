/*
 * Copyright (c) 2026 American Printing House for the Blind
 * Use of this source code is governed by an MIT-style license that can be found in the LICENSE file.
 */

package org.aph.monarchblocks.model

import android.graphics.Bitmap
import android.util.Size
import com.humanware.keysoftsdk.selfbrailling.aidl.DotsMatrix
import org.aph.monarchblocks.activities.SelfBraillingActivity.Companion.pinUp
import org.aph.monarchblocks.activities.SelfBraillingActivity.Companion.pinDown
import kotlin.random.Random

/**
 * The source code in this file would be substituted by your own model and
 * view model implementations. The following is only included in order to make the
 * demonstration work and display something meaningful for you to test out.
 */

/**
 * Simple example of a model describing drawing or graphics made of Braille dots
 * to be displayed on the Monarch.
 *
 * @property normalizedTwoDimensionalPixels the raw data corresponding to the graphics,
 *                                          after transformation from 1D to 2D and normalization.
 */
sealed class SelfBraillingDrawing {
    protected lateinit var normalizedTwoDimensionalPixels: Array<ByteArray>

    /**
     * Allows taking the 2D-array backing a [SelfBraillingDrawing] object and obtaining
     * a [DotsMatrix] out of it.
     *
     * @return [normalizedTwoDimensionalPixels] as [DotsMatrix]
     */
    fun asDotsMatrix() = DotsMatrix(normalizedTwoDimensionalPixels)

    /**
     * Allows transforming the 1D raw data into a 2D matrix with width equal to the
     * number of pixels desired or the number of screen columns.
     *
     * @param oneDimensionalArray the raw data as a 1D byte array
     * @param columnCount the desired width of the 2D matrix
     *
     * @return the raw data reshaped in two dimensions
     */
    protected fun reshape(oneDimensionalArray: ByteArray, columnCount: Int): Array<ByteArray> {
        return oneDimensionalArray
            .toList()
            .chunked(columnCount)
            .map { it.toByteArray() }
            .toTypedArray()
    }

    /**
     * Takes the procedural data or bitmap data and converts it into
     * the form expected by the Monarch. The exact conversion is to
     * be determined according to the data you are working with.
     *
     * @param rawData the raw data as a 1D-array
     *
     * @return the normalized data, consisting of 0s and 1s only
     */
    abstract fun normalize(rawData: ByteArray): ByteArray


    /**
     * Allows taking one of the monochrome (1-bit) bitmap images found in the resource files
     * and converting it to a normalized matrix of Braille dots for display on the Monarch.
     */
    class BitmapBraille(bitmap: Bitmap) : SelfBraillingDrawing() {
        init {
            val normalizedPixels = bitmapToPixelArray(bitmap).run { normalize(this) }
            normalizedTwoDimensionalPixels = reshape(normalizedPixels, bitmap.width)
        }

        override fun normalize(rawData: ByteArray): ByteArray {
            return rawData.map { if (it < 0) pinDown else pinUp }
                .toByteArray()
        }

        /**
         * Converts a bitmap image to a 1D-[ByteArray].
         *
         * @param bitmap the raw bitmap image
         *
         * @return the [ByteArray] representing the bitmap, not normalized
         */
        private fun bitmapToPixelArray(bitmap: Bitmap): ByteArray {
            val rawPixels = IntArray(bitmap.width * bitmap.height) { 0 }
            bitmap.getPixels(
                rawPixels,
                0,
                bitmap.width,
                0,
                0,
                bitmap.width,
                bitmap.height
            )

            return rawPixels
                .map { it.toByte() }
                .toByteArray()
        }
    }

    /**
     * Allows displaying an arbitrary set of Braille dots generated procedurally on
     * the Monarch.
     */
    class ProceduralBraille(width: Int, rawData: ByteArray) : SelfBraillingDrawing() {
        init {
            val normalizedPixels = normalize(rawData)
            normalizedTwoDimensionalPixels = reshape(normalizedPixels, width)
        }

        override fun normalize(rawData: ByteArray): ByteArray {
            return rawData.map { if (it < 1) pinDown else pinUp }
                .toByteArray()
        }
    }
}


/**
 * Generates a [ByteArray] representing dots randomly lowered or raised covering an area of given size.
 *
 * @param areaSize the size of the area to cover
 *
 * @return a random array of dots, not normalized
 */
fun randomDots(areaSize: Size): ByteArray {
    return Random.nextBytes(areaSize.width * areaSize.height)
}

/**
 * Generates a [ByteArray] representing vertical lines covering an area of given size.
 *
 * @param areaSize the size of the area to cover
 *
 * @return an array representing vertical lines
 */
fun verticalLines(areaSize: Size): ByteArray {
    return ByteArray(areaSize.width * areaSize.height) {
        if (it % 4 == 0) pinUp else pinDown
    }
}
