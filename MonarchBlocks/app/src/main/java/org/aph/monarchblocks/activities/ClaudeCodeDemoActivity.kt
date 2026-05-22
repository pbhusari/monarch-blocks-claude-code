/*
 * Copyright (c) 2026 American Printing House for the Blind
 * Use of this source code is governed by an MIT-style license that can be found in the LICENSE file.
 */

package org.aph.monarchblocks.activities

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Size
import android.view.Gravity
import android.view.KeyEvent
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import com.humanware.keysoftsdk.selfbrailling.SelfBraillingManager
import com.humanware.keysoftsdk.selfbrailling.aidl.DotsMatrix
import com.humanware.keysoftsdk.selfbrailling.widget.SelfBraillingWidget
import com.humanware.keysoftsdk.translator.BrailleGrade
import com.humanware.keysoftsdk.translator.Translator
import com.humanware.keysoftsdk.translator.aidl.TranslationQuery
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.aph.monarchblocks.SCREEN_HEIGHT
import org.aph.monarchblocks.SCREEN_WIDTH
import org.aph.monarchblocks.monarch_utils.BrlScrollView
import org.aph.monarchblocks.monarch_utils.Drawing
import org.aph.monarchblocks.monarch_utils.Position

/**
 * Presentation demo showing a Claude Code UI mockup on the Monarch 32×10 braille display.
 * Cycles through four frames showing a refactor task at 0% → 25% → 61% → 100%, then loops.
 * Pending subtasks show an animated braille spinner. Auto-advances every 3 min.
 * PAGE_DOWN / PAGE_UP navigate manually; MOVE_HOME resets to frame 1.
 */
class ClaudeCodeDemoActivity : AppCompatActivity() {

    private lateinit var manager: SelfBraillingManager
    private lateinit var widget: SelfBraillingWidget
    private lateinit var translator: Translator
    private lateinit var screenDimensions: Size
    private lateinit var brailleScreen: DotsMatrix

    private val mutableLiveDots    = MutableLiveData<Array<ByteArray>>()
    private val mutableViewedImage = MutableLiveData<Array<ByteArray>>()

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var textOverlay: TextView

    // ── Demo content ──────────────────────────────────────────────────────────

    private data class FrameSpec(val pct: Int, val subtasksDone: Int, val tasksDone: Int)

    private val taskNames = listOf(
        "read_existing_code",
        "analyse_patterns",
        "write_tests",
        "refactor_login",
        "refactor_session"
    )

    private val frameSpecs = listOf(
        FrameSpec(pct =   0, subtasksDone = 0, tasksDone = 0),
        FrameSpec(pct =  25, subtasksDone = 2, tasksDone = 2),
        FrameSpec(pct =  61, subtasksDone = 5, tasksDone = 4),
        FrameSpec(pct = 100, subtasksDone = 8, tasksDone = 5)
    )

    private var frameIndex = 0

    // ── Spinner ───────────────────────────────────────────────────────────────

    private val spinnerChars = "⠋⠙⠹⠸⠼⠴⠦⠧⠇⠏"
    private var spinnerIndex = 0

    private val spinnerTick = object : Runnable {
        override fun run() {
            spinnerIndex = (spinnerIndex + 1) % spinnerChars.length
            renderFrame()
            handler.postDelayed(this, 80L)
        }
    }

    private val frameTick = object : Runnable {
        override fun run() {
            frameIndex = (frameIndex + 1) % frameSpecs.size
            renderFrame()
            handler.postDelayed(this, 180_000L)
        }
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        manager    = SelfBraillingManager(this).apply { bindService() }
        widget     = SelfBraillingWidget(this)
        translator = Translator(this).apply { bindService() }
        supportActionBar?.hide()

        textOverlay = TextView(this).apply {
            setBackgroundColor(Color.argb(210, 0, 0, 0))
            setTextColor(Color.WHITE)
            textSize = 14f
            typeface = android.graphics.Typeface.MONOSPACE
            setPadding(24, 16, 24, 16)
            gravity = Gravity.BOTTOM or Gravity.START
        }
        val root = FrameLayout(this).apply {
            addView(widget, FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
            addView(textOverlay, FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM))
        }
        setContentView(root)

        val sizeX = manager.brailleDisplayDotsSizeX.takeIf { it > 0 } ?: SCREEN_WIDTH
        val sizeY = manager.brailleDisplayDotsSizeY.takeIf { it > 0 } ?: SCREEN_HEIGHT
        screenDimensions = Size(sizeX, sizeY)
        brailleScreen    = DotsMatrix(screenDimensions.height, screenDimensions.width)

        mutableViewedImage.observe(this) { widget.refresh(it) }
        mutableLiveDots.observe(this)    { manager.displayDots(it) }

        widget.onSelfBraillingWidgetListener = object :
            SelfBraillingWidget.OnSelfBraillingWidgetListener {
            override fun onFocused() = renderFrame()
            override fun onDoubleTapAtBraillePosition(x: Int, y: Int) {}
        }

        lifecycleScope.launch { delay(1000) }.invokeOnCompletion {
            renderFrame()
            startTimers()
        }
    }

    // ── Rendering ─────────────────────────────────────────────────────────────

    private fun buildFrameText(): String {
        val spec = frameSpecs[frameIndex]
        return buildString {
            appendLine("claude-code  agents:2 active")
            appendLine("bash  pytest tests/test_auth.py")
            appendLine("PRIMARY TASK")
            appendLine("refactor auth module     ${spec.pct.toString().padStart(4)}%")
            appendLine("SUBTASKS  ${spec.subtasksDone}/8")
            for ((i, name) in taskNames.withIndex()) {
                if (i < spec.tasksDone) {
                    appendLine(" ${name.padEnd(22)}done")
                } else {
                    appendLine(" $name")
                }
            }
        }.trimEnd()
    }

    private fun renderFrame() {
        val text = buildFrameText()
        val sv = BrlScrollView(text, screenDimensions, ::translate, lineSpacing = 1, noIndent = true)
        sv.getPage(brailleScreen)
        drawSpinners()
        mutableViewedImage.value = brailleScreen.matrix
        mutableLiveDots.value    = brailleScreen.matrix
        textOverlay.text = "── Frame ${frameIndex + 1}/${frameSpecs.size} ──\n$text"
    }

    // 5 header lines × 4 pins each; spinner at x=93 (braille column 31, the last column).
    private fun drawSpinners() {
        val spec = frameSpecs[frameIndex]
        if (spec.tasksDone >= taskNames.size) return
        val spinner = spinnerChars[spinnerIndex].toString()
        for (i in spec.tasksDone until taskNames.size) {
            Drawing.freeformBraille(spinner, brailleScreen, Position(93, (5 + i) * 4))
        }
    }

    // ── Timers ────────────────────────────────────────────────────────────────

    private fun startTimers() {
        handler.removeCallbacks(spinnerTick)
        handler.removeCallbacks(frameTick)
        handler.postDelayed(spinnerTick, 80L)
        handler.postDelayed(frameTick, 180_000L)
    }

    private fun resetFrameTimer() {
        handler.removeCallbacks(frameTick)
        handler.postDelayed(frameTick, 180_000L)
    }

    // ── Key handling ──────────────────────────────────────────────────────────

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_PAGE_DOWN -> {
                frameIndex = (frameIndex + 1) % frameSpecs.size
                renderFrame(); resetFrameTimer(); return true
            }
            KeyEvent.KEYCODE_PAGE_UP -> {
                frameIndex = (frameIndex - 1 + frameSpecs.size) % frameSpecs.size
                renderFrame(); resetFrameTimer(); return true
            }
            KeyEvent.KEYCODE_MOVE_HOME -> {
                frameIndex = 0; renderFrame(); resetFrameTimer(); return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

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
        handler.removeCallbacks(spinnerTick)
        handler.removeCallbacks(frameTick)
        manager.unbindService()
        translator.unbindService()
        super.onStop()
    }
}
