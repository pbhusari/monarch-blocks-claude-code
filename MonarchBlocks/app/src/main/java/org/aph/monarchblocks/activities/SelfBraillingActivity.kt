/*
 * Copyright (c) 2026 American Printing House for the Blind
 * Use of this source code is governed by an MIT-style license that can be found in the LICENSE file.
 */

package org.aph.monarchblocks.activities

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.view.KeyEvent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import com.humanware.keysoftsdk.selfbrailling.SelfBraillingManager
import com.humanware.keysoftsdk.selfbrailling.aidl.DotsMatrix
import com.humanware.keysoftsdk.selfbrailling.widget.SelfBraillingWidget
import com.humanware.keysoftsdk.translator.BrailleGrade
import com.humanware.keysoftsdk.translator.Translator
import com.humanware.keysoftsdk.translator.aidl.TranslationQuery
import org.aph.monarchblocks.R
import org.aph.monarchblocks.contextmenu.SelfBraillingContextMenuActivity
import org.aph.monarchblocks.model.SelfBraillingDrawing
import org.aph.monarchblocks.model.randomDots
import org.aph.monarchblocks.model.verticalLines
import org.aph.monarchblocks.monarch_utils.BrlScrollView
import org.aph.monarchblocks.monarch_utils.CustomGraphicsFunc
import org.aph.monarchblocks.monarch_utils.DialogHandler
import org.aph.monarchblocks.monarch_utils.enums.ResultCode
import java.util.Locale


/**
 * Implements a sample usage of the [SelfBraillingWidget] allowing visualization of simple graphics.
 *
 * See the [README](./README.md) for a description of the main fields found in this class.
 *
 * Bitmap images created from [Material Symbols and Icons - Google Fonts](https://fonts.google.com/icons)
 * icons: [Wb Sunny](https://fonts.google.com/icons?selected=Material%20Symbols%20Outlined%3Awb_sunny%3AFILL%400%3Bwght%40400%3BGRAD%400%3Bopsz%4048)
 * and [Lunch Dining](https://fonts.google.com/icons?selected=Material%20Symbols%20Outlined%3Alunch_dining%3AFILL%400%3Bwght%40400%3BGRAD%400%3Bopsz%4048)
 * converted to 1-bit bitmap images.
 */
class SelfBraillingActivity : AppCompatActivity() {
    companion object {
        const val TAG = "SelfBraillingActivity"
        const val pinDown: Byte = 0
        const val pinUp: Byte = 1
    }

    private lateinit var manager: SelfBraillingManager
    private lateinit var widget: SelfBraillingWidget

    private var mutableLiveDots = MutableLiveData<Array<ByteArray>>()
    private var mutableViewedImage = MutableLiveData<Array<ByteArray>>()

    private lateinit var screenDimensions: Size
    private lateinit var brailleScreen: DotsMatrix

    private lateinit var translator: Translator
    private var scrollView: BrlScrollView? = null

    private lateinit var brailleBurger: SelfBraillingDrawing
    private lateinit var brailleSun: SelfBraillingDrawing
    private lateinit var brailleRandom: SelfBraillingDrawing
    private lateinit var brailleVerticalLines: SelfBraillingDrawing

    private val startContextMenu = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        clearScreen()

        when (it.resultCode) {
            ResultCode.BITMAPS.value -> {
                // More than one DotsMatrix may be included on the screen simultaneously.
                brailleScreen.include(brailleBurger.asDotsMatrix(), 10, 10)
                brailleScreen.include(brailleSun.asDotsMatrix(), 60, 10)
                refreshScreen()
            }
            ResultCode.RANDOM.value -> {
                // A DotsMatrix may cover the whole screen.
                brailleScreen.include(brailleRandom.asDotsMatrix(), 0, 0)
                refreshScreen()
            }
            ResultCode.VERTICAL_LINES.value -> {
                // A DotsMatrix can also entirely substitute the Braille screen object.
                brailleScreen = brailleVerticalLines.asDotsMatrix()
                refreshScreen()
            }
            else -> {
                DialogHandler.showDialog(it.resultCode, this)
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        manager = SelfBraillingManager(this).apply { bindService() }
        widget = SelfBraillingWidget(this).also { setContentView(it) }
        title = getString(R.string.self_brailling_widget_activity)

        // This ensures we have access to the full screen without any overlapping bars.
        supportActionBar?.hide()

        // The widget and manager are now responsible for observing any changes
        // to the underlying LiveData instances.
        mutableViewedImage.observe(this) { widget.refresh(it) }
        mutableLiveDots.observe(this) { manager.displayDots(it) }

        setTitle("")

        screenDimensions = Size(manager.brailleDisplayDotsSizeX, manager.brailleDisplayDotsSizeY)
        brailleScreen = DotsMatrix(screenDimensions.height, screenDimensions.width)

        brailleRandom = SelfBraillingDrawing.ProceduralBraille(screenDimensions.width, randomDots(screenDimensions))
        brailleVerticalLines = SelfBraillingDrawing.ProceduralBraille(screenDimensions.width, verticalLines(screenDimensions))

        //This is needed when making screens with both braille and graphics on them.
        translator = Translator(this).apply { bindService() }

        drawBitmapImagesToScreen()

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
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        Log.d(TAG, "onKeyDown Triggered")
        var scrollHandled = false
        when (keyCode) {
            KeyEvent.KEYCODE_MENU -> {
                val contextMenuIntent = Intent(this, SelfBraillingContextMenuActivity::class.java)
                startContextMenu.launch(contextMenuIntent)
            }

            KeyEvent.KEYCODE_B -> {
                if (event?.hasModifiers(KeyEvent.META_CTRL_ON) == true) {
                    clearScreen()
                    brailleScreen.include(brailleBurger.asDotsMatrix(), 10, 10)
                    brailleScreen.include(brailleSun.asDotsMatrix(), 60, 10)
                    refreshScreen()
                }
            }

            KeyEvent.KEYCODE_R -> {
                if (event?.hasModifiers(KeyEvent.META_CTRL_ON or KeyEvent.META_SHIFT_ON) == true) {
                    clearScreen()
                    brailleScreen.include(brailleRandom.asDotsMatrix(), 0, 0)
                    refreshScreen()
                }
            }

            KeyEvent.KEYCODE_V -> {
                if (event?.hasModifiers(KeyEvent.META_CTRL_ON) == true) {
                    clearScreen()
                    brailleScreen.include(brailleVerticalLines.asDotsMatrix(), 0, 0)
                    refreshScreen()
                }
            }

            KeyEvent.KEYCODE_S -> {//Scroll Example
                clearScreen()
                drawScrollingBrailleExample()
                refreshScreen()
            }

            KeyEvent.KEYCODE_T -> {//Title example
                scrollView = null
                clearScreen()
                drawTitleScreenExample()
                refreshScreen()
            }

            KeyEvent.KEYCODE_ZOOM_IN -> {
                scrollHandled = handleScroll(keyCode)
            }

            KeyEvent.KEYCODE_ZOOM_OUT -> {
                scrollHandled = handleScroll(keyCode)
            }

            KeyEvent.KEYCODE_PAGE_UP -> {
                scrollHandled = handleScroll(keyCode)
            }

            KeyEvent.KEYCODE_PAGE_DOWN -> {
                scrollHandled = handleScroll(keyCode)
            }
            
            KeyEvent.KEYCODE_MOVE_HOME -> {
                scrollHandled = handleScroll(keyCode)
            }
            KeyEvent.KEYCODE_MOVE_END -> {
                scrollHandled = handleScroll(keyCode)
            }
        }

        if (scrollHandled) {
            refreshScreen()
            speakVisibleScrollText()
        }

        return super.onKeyDown(keyCode, event)
    }

    override fun onResume() {
        manager.bindService()
        super.onResume()
    }

    override fun onStop() {
        manager.unbindService()
        translator.unbindService()
        super.onStop()
    }


    /**
     * Draws a bitmap image of a burger and a sun to the screen.
     */
    private fun drawBitmapImagesToScreen(){
        val bitmapBurger = BitmapFactory.decodeStream(resources.openRawResource(R.raw.burger))
        val bitmapSun = BitmapFactory.decodeStream(resources.openRawResource(R.raw.sun))

        brailleBurger = SelfBraillingDrawing.BitmapBraille(bitmapBurger)
        brailleSun = SelfBraillingDrawing.BitmapBraille(bitmapSun)

        // The demo starts with a drawing of two bitmap images, a burger and a sun.
        brailleScreen.include(brailleBurger.asDotsMatrix(), 10, 10)
        brailleScreen.include(brailleSun.asDotsMatrix(), 60, 10)
    }


    /**
     *
     */
    private fun drawTitleScreenExample(){
        clearScreen()
        val monarchTitleDotsMatrix = CustomGraphicsFunc.sentenceToFancyFontGraphic("Monarch")
        val blocksTitleDotsMatrix = CustomGraphicsFunc.sentenceToFancyFontGraphic("Blocks")

        val centeredXOffsetMonarch = (96 - (monarchTitleDotsMatrix?.numberOfColumns ?: 2)) / 2
        val centeredXOffsetBlocks = (96 - (blocksTitleDotsMatrix?.numberOfColumns ?: 2)) / 2

        if(monarchTitleDotsMatrix != null && blocksTitleDotsMatrix != null) {
            brailleScreen.include(monarchTitleDotsMatrix, centeredXOffsetMonarch, 0)
            brailleScreen.include(blocksTitleDotsMatrix, centeredXOffsetBlocks, monarchTitleDotsMatrix.numberOfRows + 1)
            val titleBrailleLabelText = translate("Monarch Blocks")
            val titleBrailleGraphic = CustomGraphicsFunc.createBrailleSentenceGraphic(titleBrailleLabelText)
            val titleBrailleXOffset = (96 - (titleBrailleGraphic.numberOfColumns)) / 2

            val titleTotalHeight = monarchTitleDotsMatrix.numberOfRows + blocksTitleDotsMatrix.numberOfRows + 1

            brailleScreen.include(titleBrailleGraphic, titleBrailleXOffset, (titleTotalHeight + 1))

            val pressEnterBrailleText = translate("Press Enter")
            val pressEnterBrailleGraphic = CustomGraphicsFunc.createBrailleSentenceGraphic(pressEnterBrailleText)
            val pressEnterXOffset = (96 - pressEnterBrailleGraphic.numberOfColumns) / 2

            brailleScreen.include(pressEnterBrailleGraphic, pressEnterXOffset, 25)
        }
    }


    /**
     * Draws an example of having a half graphic screen with a half braille screen. The braille is scrollable.
     */
    private fun drawScrollingBrailleExample(){
        val scrollViewHeight = screenDimensions.height/2
        val scrollViewDimensions = Size(screenDimensions.width, scrollViewHeight)

        //If we were using the entire screen we would be able to have 8 lines of braille. Since the scrollview is only half the screen we only have 4 lines shown at a time.
        val scrollViewContent = getString(R.string.test_paragraph)

        val sv = BrlScrollView(scrollViewContent, scrollViewDimensions, ::translateForScrollContent, 20)
        sv.getPage(brailleScreen) //Sets the brailleScreen with the content for the scrollView.
        scrollView = sv
        speakVisibleScrollText()

        val squareGraphic = CustomGraphicsFunc.createSquareGraphic()

        //Include the square image above the scrollable braille text.
        val xOffset = (screenDimensions.width - squareGraphic.numberOfColumns) / 2 //This will ensure the square is centered on the x axis
        brailleScreen.include(squareGraphic, xOffset, 0)
    }


    /**
     * Handles moving the braille scrollview up and down depending on what D-Pad direction is pressed.
     *
     * @param keyCode The Android KeyEvent key code for what key has been pressed.
     *
     * @return Boolean determining if the scroll was handled or not.
     */
    private fun handleScroll(keyCode: Int): Boolean{
        var res = false

        if(scrollView != null){
            when(keyCode){
                KeyEvent.KEYCODE_PAGE_UP -> res = scrollView!!.prevPage()
                KeyEvent.KEYCODE_PAGE_DOWN -> res = scrollView!!.nextPage()
                KeyEvent.KEYCODE_MOVE_HOME -> res = scrollView!!.firstPage()
                KeyEvent.KEYCODE_MOVE_END -> res = scrollView!!.lastPage()
                KeyEvent.KEYCODE_ZOOM_OUT -> res = scrollView!!.zoomOut()
                KeyEvent.KEYCODE_ZOOM_IN -> res = scrollView!!.zoomIn()
            }

            if(res){
                scrollView!!.getPage(brailleScreen)
            }
        }

        return res
    }


    /**
     * Speaks the text that is visible in the scrollview currently. Because of issues with the back translation
     * it only speaks English text.
     */
    private fun speakVisibleScrollText(){
        if(Locale.getDefault().language == "en"){
            val visibleText = scrollView!!.getTextOnScreen()
            if(visibleText.isNotEmpty()){
                val plainText = translate(visibleText, true)
                manager.announceText(plainText)
            }
        }
    }


    /**
    * Translates plain text into unicode braille. This will use the device language and selected braille tables to create the braille.
     * It has the option to back translate (braille to plain text), but it may not work properly depending on the language selected.
     *
     * @param input The string to be translated
     * @param setBack (Optional) Boolean for whether or not you want to back translate or not. Default is false meaning it will translate plain text to braille by default.
     *
     * @return A translated string either in unicode braille or plain text depending on the setBack value. The braille will always be uncontracted. The reason for this is to minimize errors when back translating.
     */
    private fun translate(input: String, setBack:Boolean = false): String {
        var res = ""
        val query = TranslationQuery()
        query.setGrade(BrailleGrade.GRADE_UNCONTRACTED)
        query.setBack(setBack)
        val lines = input.lines()
        for (line in lines) {
            query.setOriginal(line)
            translator.translate(query)
            res += query.translated + "\n"
        }
        res= res.trimEnd('\n') //no trailing \n

        return res
    }


    /**
    * Special version for translating to Unicode braille for the BrlScrollView class
    * Translates plain text into unicode braille. This will use the device language and selected braille tables to create the braille.
     *
     * @param input The string to be translated
     *
     * @return A translated string in unicode Braille.
     */
    fun translateForScrollContent(input: String): String {
        var res = ""
        val query = TranslationQuery()
        query.setGrade(BrailleGrade.GRADE_UNSPECIFIED)
        query.setBack(false)
        val lines = input.lines()
        for (line in lines) {
            query.setOriginal(line)
            translator.translate(query)
            res += query.translated + "\n"
        }
        res= res.trimEnd('\n'); //no trailing \n
        return res
    }


    /**
     * Sends updated values from the backing Livedata to the screen.
     */
    private fun refreshScreen() {
        Log.d("ScreenLog", "This is the braillScreen: \n" + brailleScreenToString())
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


    private fun brailleScreenToString():String {
        val res = StringBuilder()

        for(array in brailleScreen.a){
            for(bit in array){
                res.append(bit.toString())
                res.append(" ")
            }
            res.appendLine("")
        }

        return res.toString()
    }

}
