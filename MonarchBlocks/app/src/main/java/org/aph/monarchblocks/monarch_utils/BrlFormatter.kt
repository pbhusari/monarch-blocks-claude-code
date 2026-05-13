/*
 * Copyright (c) 2026 American Printing House for the Blind
 * Use of this source code is governed by an MIT-style license that can be found in the LICENSE file.
 */

package org.aph.monarchblocks.monarch_utils

var MARGIN = 32
var LAST_LINE_MARGIN = 28
const val BRAILLE_SPACE = '\u2800'

class BrlFormatter {
    companion object {

        /**
         * This method wraps braille that is drawn to the screen using the SelfBraillingWidget. This is mainly used by the BrlScrollView.
         *
         * @param input The Braille string that is going to be drawn to the screen.
         * @param noIndent (Optional) A Boolean that indicates whether to inhibit braille indentation. By default the braille will be indented two braille spaces.
         * @param endLineIndex (Optional) An Integer determining how many lines of braille are showing. Default is 8 lines of braille.
         * @param width (Optional) An Integer for the width the braille will need to wrap at. By default it is set to the maximum width of the device.
         */
        fun wrapBrl(input: String, noIndent: Boolean = false, endLineIndex: Int = 8, width: Int = 96): String {
            if(width != 96){
                MARGIN = width / 3
                if(MARGIN > 4){
                    LAST_LINE_MARGIN = MARGIN - 4
                }
            }
            else{
                MARGIN = 32
                LAST_LINE_MARGIN = MARGIN - 4
            }

            var output = ""
            val paragraphs = input.trimEnd('\n').lines()
            var lineNumber = 1
            for (paragraph in paragraphs) {
                if (paragraph.isEmpty()) {
                    output += "\n"
                    continue
                }
                if (!noIndent)
                    output += "" + BRAILLE_SPACE + BRAILLE_SPACE //indent
                var paragraphIndex = 0
                var lineIndex = if (!noIndent) { 2 } else { 0 } //compansate for indentation
                while (paragraphIndex < paragraph.length) {
                    val lineCharsLeft = if(lineNumber % endLineIndex != 0) MARGIN - lineIndex else LAST_LINE_MARGIN - lineIndex
                    var next = paragraphIndex + lineCharsLeft
                    if (next >= paragraph.length) {
                        output += paragraph.substring(paragraphIndex, paragraph.length) + "\n"
                        lineNumber++
                        break
                    }
                    while ((paragraph[next] != BRAILLE_SPACE) && (next > paragraphIndex))
                        --next
                    if (next == paragraphIndex) { //no spaces
                        output += paragraph.substring(paragraphIndex, paragraphIndex + lineCharsLeft) + "\n"
                        lineNumber++
                        paragraphIndex += lineCharsLeft
                        lineIndex = 0
                    }
                    else {
                        output += paragraph.substring(paragraphIndex, next) + "\n" //no trailing space
                        lineNumber++
                        paragraphIndex = next + 1 //skip the space in input
                        lineIndex = 0
                    }
                } //while
            } //for paragraph
            return output.trimEnd(BRAILLE_SPACE, '\n')
        }
    }
}