/*
 * Copyright (c) 2026 American Printing House for the Blind
 * Use of this source code is governed by an MIT-style license that can be found in the LICENSE file.
 */

package org.aph.monarchblocks.monarch_utils

import kotlin.math.*
import android.util.Size

import com.humanware.keysoftsdk.selfbrailling.aidl.DotsMatrix

/**
 * This class creates a braille scrollview. The scrollview does not need to be the entire screen height, but it will be the entire screen width.
 *
 * @param initialContent This is the entire string that you wish to show.
 * @param screenDimensions This is the height that you want the braille scrollview to be. You do not have to use full screen height available
 * @param translate a function which expects text as it's only parameter and returns a Unicode braille string
 * @param topMargin (Optional) Integer for how many pins at the top to give as a margin. Default is 0.
 * @param lineSpacing (Optional) Integer for how much space between lines. Default is 2.
 * @param noIndent (Optional) flag to suppress indenting of paragraphs. Default is false.
 */
class BrlScrollView(initialContent: String, val screenDimensions: Size, val translate: (input: String) -> (String),
    val topMargin: Int = 0, var lineSpacing: Int = 2, val noIndent: Boolean = false) {
    var isZoomEnabled = true
    var isHomeEndEnabled = true
    private var linesPerPage = 8
    private var topLine = 0
    private var lines: List<String> = listOf()
    var content: String = "" //untranslated content
        get() = field
        set(value) {
            field = value
            val brl = BrlFormatter.wrapBrl(translate(value), noIndent, linesPerPage)
            lines = brl.trimEnd('\n').lines()
            topLine = 0
        }
        
        init {
            calcLinesPerPage()
            content = initialContent
        }
    
    private fun calcLinesPerPage() {
        val maxLineHeight = 3 + lineSpacing //max size of a line
        linesPerPage = screenDimensions.height / maxLineHeight
        if ((linesPerPage * maxLineHeight + 4) <= screenDimensions.height)
            ++linesPerPage //last line does not require full spacing
    }
    
    private fun clearScreen(dm: DotsMatrix) {
        dm.include(DotsMatrix(Array(screenDimensions.height) {
            ByteArray(screenDimensions.width) {0}
        }), 0, topMargin)
    }

    fun zoomIn(): Boolean {
        var res = false
        if (isZoomEnabled && (lineSpacing < 4)) {
            ++lineSpacing
            calcLinesPerPage()
            reflowContent()
            res = true
        }
        return res
    }
    
    fun zoomOut(): Boolean {
        var res = false
        if (isZoomEnabled && (lineSpacing > 1)) {
            --lineSpacing
            calcLinesPerPage()
            reflowContent()
            res = true
        }
        return res
    }
    
    fun nextPage(): Boolean {
        var res = false
        if (topLine + linesPerPage < lines.count()) {
            topLine += linesPerPage
            res = true
        }
        return res
    }
    
    fun prevPage(): Boolean {
        var res = false
        if (topLine > 0) {
            topLine = max(topLine - linesPerPage, 0)
            res = true
        }
        return res
    }
    
    fun firstPage(): Boolean {
        var res = false
        if (isHomeEndEnabled) {
            topLine = 0
            res = true
        }
        return res
    }
    
    fun lastPage(): Boolean {
        var res = false
        if (isHomeEndEnabled) {
            val mod = lines.count() % linesPerPage
            if (mod > 0)
                topLine = lines.count() - mod
            else
                topLine = max(lines.count() - linesPerPage, 0)
            res = true
        }
        return res
    }
    
    fun getPage(dm: DotsMatrix) {
        val pos = Position(0, topMargin)
        val lastLine = min(topLine + linesPerPage - 1, lines.count() - 1)
        clearScreen(dm)
        for (x in topLine..lastLine) {
            val line = lines[x]
            Drawing.freeformBraille(line, dm, pos)
            pos.y += 3 + lineSpacing
        }
        if(!isLastPage()){
            dm.include(CustomGraphicsFunc.downArrowGraphic(), screenDimensions.width - 9, (screenDimensions.height * 2) - 4)
        }
    }

    private fun reflowContent() {
        //necessary when zooming, since space for "more" arrow must be recalculated
        val brl = BrlFormatter.wrapBrl(translate(content), noIndent, linesPerPage)
        lines = brl.trimEnd('\n').lines()
        topLine -= topLine % linesPerPage //must be a multiple of lines
    }




    fun getTextOnScreen():String{
        val lastLine = min(topLine + linesPerPage - 1, lines.count() - 1)
        var onScreen = ""

        for (x in topLine..lastLine) {
            val line = lines[x]

            if(onScreen.isEmpty()){
                onScreen = line
            }
            else{
                onScreen += '\n' + line
            }
        }

        return onScreen
    }



    fun isAllTextOnScreen():Boolean {
        return lines.count() <= linesPerPage
    }


    fun isFirstPage(): Boolean{
        return topLine == 0
    }

    fun isLastPage(): Boolean{
        val mod = lines.count() % linesPerPage
        var isOnLastPage = false

        isOnLastPage = if(mod > 0){
            topLine == (lines.count() - mod)
        } else{
            topLine == (max(lines.count() - linesPerPage, 0))
        }

        return isOnLastPage
    }


    fun getHeight(): Int {
        return screenDimensions.height
    }
}
