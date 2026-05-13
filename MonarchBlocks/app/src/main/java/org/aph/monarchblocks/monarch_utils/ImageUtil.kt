/*
 * Copyright (c) 2026 American Printing House for the Blind
 * Use of this source code is governed by an MIT-style license that can be found in the LICENSE file.
 */

package org.aph.monarchblocks.monarch_utils

import android.content.Context
import android.graphics.BitmapFactory
import com.humanware.keysoftsdk.selfbrailling.aidl.DotsMatrix
import org.aph.monarchblocks.model.SelfBraillingDrawing

class ImageUtil {
    companion object{
        /**
         * Gets raw bitmap image and converts it to a DotsMatrix object if possible.
         *
         * @param imageID The image resource ID.
         * @param context The context of the calling activity
         *
         * @return The DotsMatrix image for the Frog.
         */
        fun onGetImageAsDotsMatrix(imageID: Int, context: Context): DotsMatrix?{
            val imageBitmap = BitmapFactory.decodeStream(context.resources.openRawResource(imageID))
            val imageDotsMatrix = if(imageBitmap != null) SelfBraillingDrawing.BitmapBraille(imageBitmap).asDotsMatrix() else null

            return imageDotsMatrix
        }
    }
}