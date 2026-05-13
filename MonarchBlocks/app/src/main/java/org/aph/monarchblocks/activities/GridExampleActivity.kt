/*
 * Copyright (c) 2026 American Printing House for the Blind
 * Use of this source code is governed by an MIT-style license that can be found in the LICENSE file.
 */

package org.aph.monarchblocks.activities

import android.os.Bundle
import android.util.Size
import android.view.KeyEvent
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import com.humanware.keysoftsdk.selfbrailling.SelfBraillingManager
import com.humanware.keysoftsdk.selfbrailling.aidl.DotsMatrix
import com.humanware.keysoftsdk.selfbrailling.widget.SelfBraillingWidget
import org.aph.monarchblocks.interfaces.SelfBraillingInterface

class GridExampleActivity: AppCompatActivity(), SelfBraillingInterface {
    override var areServicesBound: Boolean = false
    override lateinit var manager: SelfBraillingManager
    override lateinit var widget: SelfBraillingWidget
    override lateinit var screenDimensions: Size
    override lateinit var brailleScreen: DotsMatrix
    override var mutableLiveDots: MutableLiveData<Array<ByteArray>> = MutableLiveData<Array<ByteArray>>()
    override var mutableViewedImage: MutableLiveData<Array<ByteArray>> = MutableLiveData<Array<ByteArray>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        areServicesBound = true

        //Setup for SelfBraillingWidget
        manager = SelfBraillingManager(this).apply { bindService() }
        widget = SelfBraillingWidget(this).also { setContentView(it) }
        // This ensures we have access to the full screen without any overlapping bars.
        supportActionBar?.hide()
        // The widget and manager are now responsible for observing any changes
        // to the underlying LiveData instances.
        mutableViewedImage.observe(this) { widget.refresh(it) }
        mutableLiveDots.observe(this) { manager.displayDots(it) }
        screenDimensions = Size(manager.brailleDisplayDotsSizeX, manager.brailleDisplayDotsSizeY)
        brailleScreen = DotsMatrix(screenDimensions.height, screenDimensions.width)

        widget.onSelfBraillingWidgetListener = object :
            SelfBraillingWidget.OnSelfBraillingWidgetListener {
            override fun onFocused() = refreshScreen()

            override fun onDoubleTapAtBraillePosition(p0: Int, p1: Int) {
                //The offset for the finger touch point is off by about 3 pins for the X value.
                val adjustedX = if(p0 >= 3) p0 - 3 else p0

                //TODO: Include code for what happens when the user touches the grid.
            }

            }

        //TODO: Add code to draw the grid and refresh the screen.
    }


    override fun onResume() {
        super.onResume()
        if(!areServicesBound){
            bindSBServices()
            refreshScreen()
        }
    }


    override fun onStop() {
        super.onStop()
        if(areServicesBound){
            unbindSBServices()
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        if(areServicesBound)
            unbindSBServices()

    }


    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return super.onKeyDown(keyCode, event)
    }


    override fun bindSBServices() {
        areServicesBound = true
        manager.bindService()
    }

    override fun unbindSBServices() {
        areServicesBound = false
        manager.unbindService()
    }

}