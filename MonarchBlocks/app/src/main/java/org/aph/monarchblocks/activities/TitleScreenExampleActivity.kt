/*
 * Copyright (c) 2026 American Printing House for the Blind
 * Use of this source code is governed by an MIT-style license that can be found in the LICENSE file.
 */

package org.aph.monarchblocks.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.view.KeyEvent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import com.humanware.keysoftsdk.selfbrailling.SelfBraillingManager
import com.humanware.keysoftsdk.selfbrailling.aidl.DotsMatrix
import com.humanware.keysoftsdk.selfbrailling.widget.SelfBraillingWidget
import com.humanware.keysoftsdk.translator.Translator
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.aph.monarchblocks.R
import org.aph.monarchblocks.SCREEN_WIDTH
import org.aph.monarchblocks.contextmenu.SimpleContextMenuActivity
import org.aph.monarchblocks.monarch_utils.BrailleUtil
import org.aph.monarchblocks.monarch_utils.CustomGraphicsFunc
import org.aph.monarchblocks.monarch_utils.DialogHandler
import org.aph.monarchblocks.monarch_utils.enums.ResultCode

class TitleScreenExampleActivity: AppCompatActivity() {
    companion object {
        const val pinDown: Byte = 0
        const val pinUp: Byte = 1
    }

    private var areServicesBound = false

    //Variables for SelfBraillingWidget
    private lateinit var manager: SelfBraillingManager
    private lateinit var widget: SelfBraillingWidget
    private var mutableLiveDots = MutableLiveData<Array<ByteArray>>()
    private var mutableViewedImage = MutableLiveData<Array<ByteArray>>()
    private lateinit var screenDimensions: Size
    private lateinit var brailleScreen: DotsMatrix
    //============================================

    private lateinit var translator: Translator


    /*
    NOTE: I've found that the SelfBraillingWidget doesn't work well with any onCreate method other than the
    one that only has the savedInstanceState. I'm not sure why or if this has been fixed since writing this.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTitle(getString(R.string.app_name))
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

        //Setup for Braille Translator
        translator = Translator(this).apply { bindService() }

        widget.onSelfBraillingWidgetListener = object :
            SelfBraillingWidget.OnSelfBraillingWidgetListener {
            override fun onFocused() = refreshScreen()

            override fun onDoubleTapAtBraillePosition(pointX: Int, pointY: Int) {
                Log.e("POINT", "$pointX, $pointY")

                //The offset for the finger touch point is off by about 3 pins for the X value.
                val adjustedX = if(pointX >= 3) pointX - 3 else pointX

                //This makes one pin up where you put your finger.
                brailleScreen.include(DotsMatrix(arrayOf(byteArrayOf(1))), adjustedX, pointY)
                refreshScreen()
            }
        }


        /*
        This is only needed if the setTile option has not been set to an empty string. KeySoft will show the title of the app
        or the app name on the Monarch screen which will wipe out any screen changes. If setTitle is an empty string this is not an issue.
         */
        lifecycleScope.launch{
            delay(1000)
        }.invokeOnCompletion {
            onDrawTitleExample()
        }
    }


    override fun onResume() {
        super.onResume()

        if(!areServicesBound){
         //We need to rebind the services when coming back to the app.
            manager.bindService()
            translator.bindService()

            lifecycleScope.launch{
                delay(1000)
            }.invokeOnCompletion {
                onDrawTitleExample()
            }
        }
    }


    override fun onStop() {
        super.onStop()

        if(areServicesBound){
            //Need to unbind services
            areServicesBound = false
            manager.unbindService()
            translator.unbindService()
        }
    }


    override fun onDestroy() {
        super.onDestroy()

        if(areServicesBound){
            //Need to unbind services
            areServicesBound = false
            manager.unbindService()
            translator.unbindService()
        }
    }


    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when(keyCode){
            KeyEvent.KEYCODE_MENU -> {
                val intent = Intent(this, SimpleContextMenuActivity::class.java)
                startContextMenu.launch(intent)
            }
            KeyEvent.KEYCODE_ENTER -> {
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
            }
        }


        return super.onKeyDown(keyCode, event)
    }



    private val startContextMenu = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        when (it.resultCode) {
            ResultCode.TOAST_MSG.value -> DialogHandler.showToastMsg(this)
            ResultCode.SNACKBAR_MSG.value -> DialogHandler.showSnackBarMsg(window.decorView.rootView, this)
            else -> DialogHandler.showDialog(it.resultCode, this)
        }
    }


    /**
     * Draws the Title Example and refresh the screen
     */
    private fun onDrawTitleExample(){
        val title1Graphic = createFancyFontGraphic(getString(R.string.title_part_1))
        val title2Graphic = createFancyFontGraphic(getString(R.string.title_part_2))

        if(title1Graphic != null && title2Graphic != null){
            brailleScreen.include(title1Graphic, CustomGraphicsFunc.calculateImageCenterOffset(title1Graphic.numberOfColumns, SCREEN_WIDTH), 0)
            var yOffset = title1Graphic.numberOfRows + 1

            brailleScreen.include(title2Graphic, CustomGraphicsFunc.calculateImageCenterOffset(title2Graphic.numberOfColumns, SCREEN_WIDTH), yOffset)
            yOffset += title2Graphic.numberOfRows + 4

            val titleLabel = createTitleBrailleLabel()
            brailleScreen.include(titleLabel, CustomGraphicsFunc.calculateImageCenterOffset(titleLabel.numberOfColumns, SCREEN_WIDTH), yOffset)
            yOffset += titleLabel.numberOfRows + 5

            val pressEnterLabel = createEnterBrailleLabel()
            brailleScreen.include(pressEnterLabel, CustomGraphicsFunc.calculateImageCenterOffset(pressEnterLabel.numberOfColumns, SCREEN_WIDTH), yOffset)

            refreshScreen()
        }
    }


    /**
     * Creates the DotsMatrix object for the fancy Font characters
     *
     * @return A DotsMatrix object of the app title in fancy font
     */
    private fun createFancyFontGraphic(title: String):DotsMatrix? {
        return CustomGraphicsFunc.sentenceToFancyFontGraphic(title.uppercase())
    }


    /**
     * Converts the app name to braille and then creates a DotsMatrix label that will go below the fancy font title.
     *
     * @return A DotsMatrix object for the braille title label
     */
    private fun createTitleBrailleLabel():DotsMatrix {
        val brailleLabel = BrailleUtil.translate(getString(R.string.app_name), translator)
        return CustomGraphicsFunc.createBrailleSentenceGraphic(brailleLabel)
    }


    /**
     * Creates the braille DotsMatrix label for the "Press Enter" text
     *
     * @return A DotsMatrix object for the "Press Enter" text.
     */
    private fun createEnterBrailleLabel(): DotsMatrix {
        val brailleLabel = BrailleUtil.translate(getString(R.string.enter_statement), translator)
        return CustomGraphicsFunc.createBrailleSentenceGraphic(brailleLabel)
    }



    /**
    * Sends updated values from the backing Livedata to the screen.
     */
    private fun refreshScreen() {
        mutableViewedImage.value = brailleScreen.matrix
        mutableLiveDots.value = brailleScreen.matrix
    }

    /**
     * Lowers all pins on the screen.
     */
    private fun clearScreen() {
        brailleScreen.include(DotsMatrix(Array(screenDimensions.height) {
            ByteArray(screenDimensions.width) { pinDown }
        }), 0, 0)
    }

}