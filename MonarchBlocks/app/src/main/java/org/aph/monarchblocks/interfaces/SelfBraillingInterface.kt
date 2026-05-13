/*
 * Copyright (c) 2026 American Printing House for the Blind
 * Use of this source code is governed by an MIT-style license that can be found in the LICENSE file.
 */

package org.aph.monarchblocks.interfaces

import android.util.Size
import androidx.lifecycle.MutableLiveData
import com.humanware.keysoftsdk.selfbrailling.SelfBraillingManager
import com.humanware.keysoftsdk.selfbrailling.aidl.DotsMatrix
import com.humanware.keysoftsdk.selfbrailling.widget.SelfBraillingWidget

interface SelfBraillingInterface {
    var areServicesBound: Boolean
    var manager: SelfBraillingManager //This needs to be lateinit
    var widget: SelfBraillingWidget //This needs to be lateinit
    var mutableLiveDots: MutableLiveData<Array<ByteArray>>
    var mutableViewedImage: MutableLiveData<Array<ByteArray>>
    var screenDimensions: Size //This needs to be lateinit
    var brailleScreen: DotsMatrix //This needs to be lateinit


    /**
     * Binds the services for the SelfBraillingWidget. This is used in the onResume method
     */
    fun bindSBServices()

    /**
     * Unbinds the services for the SelfBraillingWidget. This is used in onStop and onDestroy.
     */
    fun unbindSBServices()


    /**
     * Sends Updated values from the backing Livedata to the screen.
     */
    fun refreshScreen(){
        mutableViewedImage.value = brailleScreen.matrix
        mutableLiveDots.value = brailleScreen.matrix
    }

    /**
     * Lowers all pins on the screen.
     */
    fun clearScreen(){
        brailleScreen.include(DotsMatrix(Array(screenDimensions.height) {
            ByteArray(screenDimensions.width)
        }), 0, 0)
    }
}