/*
 * Copyright (c) 2026 American Printing House for the Blind
 * Use of this source code is governed by an MIT-style license that can be found in the LICENSE file.
 */

package org.aph.monarchblocks.activities

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Size
import android.view.KeyEvent
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import com.humanware.keysoftsdk.selfbrailling.SelfBraillingManager
import com.humanware.keysoftsdk.selfbrailling.aidl.DotsMatrix
import com.humanware.keysoftsdk.selfbrailling.widget.SelfBraillingWidget
import com.humanware.keysoftsdk.translator.BrailleGrade
import com.humanware.keysoftsdk.translator.Translator
import com.humanware.keysoftsdk.translator.aidl.TranslationQuery
import org.aph.monarchblocks.monarch_utils.BrlScrollView

/**
 * Presentation demo showing the Claude Code UI mockup on the Monarch 32×10 braille display.
 *
 * Cycles through four frames that show a refactor task progressing from 0% → 25% → 61% → 100%.
 * Auto-advances every 3 s; PAGE_DOWN / PAGE_UP navigate manually; MOVE_HOME resets to frame 1.
 */
class ClaudeCodeDemoActivity : AppCompatActivity() {

    private lateinit var manager: SelfBraillingManager
    private lateinit var widget: SelfBraillingWidget
    private lateinit var translator: Translator
    private lateinit var screenDimensions: Size
    private lateinit var brailleScreen: DotsMatrix

    private val mutableLiveDots    = MutableLiveData<Array<ByteArray>>()
    private val mutableViewedImage = MutableLiveData<Array<ByteArray>>()

    private var frameIndex = 0
    private val handler = Handler(Looper.getMainLooper())
    private val autoAdvanceMs = 3_000L

    private val frames = listOf(
        "claude-code  agents:2 active\n" +
        "bash  pytest tests/test_auth.py\n" +
        "PRIMARY TASK\n" +
        "refactor auth module        0%\n" +
        "SUBTASKS  0/8\n" +
        " read_existing_code\n" +
        " analyse_patterns\n" +
        " write_tests\n" +
        " refactor_login\n" +
        " refactor_session",

        "claude-code  agents:2 active\n" +
        "bash  pytest tests/test_auth.py\n" +
        "PRIMARY TASK\n" +
        "refactor auth module       25%\n" +
        "SUBTASKS  2/8\n" +
        " read_existing_code    done\n" +
        " analyse_patterns      done\n" +
        " write_tests\n" +
        " refactor_login\n" +
        " refactor_session",

        "claude-code  agents:2 active\n" +
        "bash  pytest tests/test_auth.py\n" +
        "PRIMARY TASK\n" +
        "refactor auth module       61%\n" +
        "SUBTASKS  5/8\n" +
        " read_existing_code    done\n" +
        " analyse_patterns      done\n" +
        " write_tests           done\n" +
        " refactor_login        done\n" +
        " refactor_session",

        "claude-code  agents:2 active\n" +
        "bash  pytest tests/test_auth.py\n" +
        "PRIMARY TASK\n" +
        "refactor auth module      100%\n" +
        "SUBTASKS  8/8\n" +
        " read_existing_code    done\n" +
        " analyse_patterns      done\n" +
        " write_tests           done\n" +
        " refactor_login        done\n" +
        " refactor_session      done"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        manager    = SelfBraillingManager(this).apply { bindService() }
        widget     = SelfBraillingWidget(this).also { setContentView(it) }
        translator = Translator(this).apply { bindService() }
        supportActionBar?.hide()

        screenDimensions = Size(manager.brailleDisplayDotsSizeX, manager.brailleDisplayDotsSizeY)
        brailleScreen    = DotsMatrix(screenDimensions.height, screenDimensions.width)

        mutableViewedImage.observe(this) { widget.refresh(it) }
        mutableLiveDots.observe(this)    { manager.displayDots(it) }

        widget.onSelfBraillingWidgetListener = object :
            SelfBraillingWidget.OnSelfBraillingWidgetListener {
            override fun onFocused() = renderFrame()
            override fun onDoubleTapAtBraillePosition(x: Int, y: Int) {}
        }

        renderFrame()
        scheduleNext()
    }

    private fun renderFrame() {
        // lineSpacing=1 → 40/(3+1)=10 lines per page, matching the 10-row display.
        val sv = BrlScrollView(
            frames[frameIndex], screenDimensions, ::translate,
            lineSpacing = 1, noIndent = true
        )
        sv.getPage(brailleScreen)
        mutableViewedImage.value = brailleScreen.matrix
        mutableLiveDots.value    = brailleScreen.matrix
    }

    private fun scheduleNext() {
        handler.removeCallbacksAndMessages(null)
        if (frameIndex < frames.lastIndex) {
            handler.postDelayed({
                frameIndex++
                renderFrame()
                scheduleNext()
            }, autoAdvanceMs)
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_PAGE_DOWN -> {
                if (frameIndex < frames.lastIndex) { frameIndex++; renderFrame(); scheduleNext() }
                return true
            }
            KeyEvent.KEYCODE_PAGE_UP -> {
                if (frameIndex > 0) { frameIndex--; renderFrame(); scheduleNext() }
                return true
            }
            KeyEvent.KEYCODE_MOVE_HOME -> {
                frameIndex = 0; renderFrame(); scheduleNext()
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun translate(input: String): String {
        val query = TranslationQuery().apply {
            setGrade(BrailleGrade.GRADE_UNSPECIFIED)
            setBack(false)
        }
        return input.lines().joinToString("\n") { line ->
            query.setOriginal(line)
            translator.translate(query)
            query.translated
        }
    }

    override fun onResume() {
        manager.bindService()
        super.onResume()
    }

    override fun onStop() {
        handler.removeCallbacksAndMessages(null)
        manager.unbindService()
        translator.unbindService()
        super.onStop()
    }
}
