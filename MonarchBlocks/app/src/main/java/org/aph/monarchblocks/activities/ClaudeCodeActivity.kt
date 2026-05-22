/*
 * Copyright (c) 2026 American Printing House for the Blind
 * Use of this source code is governed by an MIT-style license that can be found in the LICENSE file.
 */

package org.aph.monarchblocks.activities

import android.content.Context
import android.os.Bundle
import android.text.InputType
import android.util.Size
import android.view.KeyEvent
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import com.humanware.keysoftsdk.selfbrailling.SelfBraillingManager
import com.humanware.keysoftsdk.selfbrailling.aidl.DotsMatrix
import com.humanware.keysoftsdk.selfbrailling.widget.SelfBraillingWidget
import com.humanware.keysoftsdk.translator.BrailleGrade
import com.humanware.keysoftsdk.translator.Translator
import com.humanware.keysoftsdk.translator.aidl.TranslationQuery
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.aph.monarchblocks.monarch_utils.BrlScrollView
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Claude Code interface for the Monarch 32x10 braille display (96x40 pins).
 *
 * Flow:
 *  1. INPUT  – EditText for typing a prompt (or API key on first launch).
 *  2. LOADING – blank screen + TTS "Thinking" while the API call is in flight.
 *  3. RESPONSE – full-screen SelfBraillingWidget with scrollable braille response.
 *
 * Keys in RESPONSE mode:
 *   PAGE_DOWN / PAGE_UP     – next / previous page
 *   MOVE_HOME / MOVE_END    – first / last page
 *   ZOOM_IN  / ZOOM_OUT     – increase / decrease line spacing
 *   MENU                    – return to INPUT mode
 *
 * API key is stored in SharedPreferences (key "api_key", prefs file "claude_prefs").
 * On first launch the activity prompts for the key before opening the chat.
 */
class ClaudeCodeActivity : AppCompatActivity() {

    private enum class Mode { API_KEY, INPUT, LOADING, RESPONSE }

    private var mode = Mode.INPUT

    // Shared input view (reused for both API-key entry and chat prompts).
    private lateinit var inputView: EditText

    // Braille display components – created once, reused across LOADING/RESPONSE cycles.
    private lateinit var manager: SelfBraillingManager
    private lateinit var widget: SelfBraillingWidget
    private lateinit var translator: Translator
    private lateinit var screenDimensions: Size
    private lateinit var brailleScreen: DotsMatrix
    private val mutableLiveDots   = MutableLiveData<Array<ByteArray>>()
    private val mutableViewedImage = MutableLiveData<Array<ByteArray>>()
    private var scrollView: BrlScrollView? = null
    private var brailleReady = false

    private val http = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (apiKey().isBlank()) openApiKeyEntry() else openChatInput()
    }

    // ── Input screens ─────────────────────────────────────────────────────────

    private fun openApiKeyEntry() {
        mode = Mode.API_KEY
        showEditText("Enter your Anthropic API key, then press Enter (Dots 8) to save")
    }

    private fun openChatInput() {
        mode = Mode.INPUT
        showEditText("Ask Claude anything, then press Enter (Dots 8) to send")
    }

    private fun showEditText(hint: String) {
        inputView = EditText(this).apply {
            this.hint = hint
            inputType = InputType.TYPE_CLASS_TEXT
            textSize = 28f
        }
        setContentView(inputView)
        inputView.requestFocus()
    }

    // ── Braille display ───────────────────────────────────────────────────────

    private fun ensureBrailleView() {
        if (!brailleReady) {
            manager = SelfBraillingManager(this).apply { bindService() }
            widget  = SelfBraillingWidget(this)
            translator = Translator(this).apply { bindService() }
            screenDimensions = Size(manager.brailleDisplayDotsSizeX, manager.brailleDisplayDotsSizeY)
            brailleScreen    = DotsMatrix(screenDimensions.height, screenDimensions.width)
            mutableViewedImage.observe(this) { widget.refresh(it) }
            mutableLiveDots.observe(this)    { manager.displayDots(it) }
            widget.onSelfBraillingWidgetListener = object :
                SelfBraillingWidget.OnSelfBraillingWidgetListener {
                override fun onFocused() = refreshScreen()
                override fun onDoubleTapAtBraillePosition(x: Int, y: Int) {}
            }
            brailleReady = true
        }
        setContentView(widget)
        supportActionBar?.hide()
    }

    // ── Key handling ──────────────────────────────────────────────────────────

    /** Intercept ENTER before the EditText can swallow it. */
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN && event.keyCode == KeyEvent.KEYCODE_ENTER) {
            when (mode) {
                Mode.API_KEY -> {
                    val key = inputView.text.toString().trim()
                    if (key.isNotEmpty()) { saveApiKey(key); openChatInput() }
                    return true
                }
                Mode.INPUT -> {
                    val prompt = inputView.text.toString().trim()
                    if (prompt.isNotEmpty()) { sendPrompt(prompt) }
                    return true
                }
                else -> {}
            }
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (mode == Mode.RESPONSE) {
            when (keyCode) {
                KeyEvent.KEYCODE_MENU      -> { openChatInput(); return true }
                KeyEvent.KEYCODE_PAGE_DOWN -> if (scroll { it.nextPage()  }) return true
                KeyEvent.KEYCODE_PAGE_UP   -> if (scroll { it.prevPage()  }) return true
                KeyEvent.KEYCODE_MOVE_HOME -> if (scroll { it.firstPage() }) return true
                KeyEvent.KEYCODE_MOVE_END  -> if (scroll { it.lastPage()  }) return true
                KeyEvent.KEYCODE_ZOOM_IN   -> if (scroll { it.zoomIn()    }) return true
                KeyEvent.KEYCODE_ZOOM_OUT  -> if (scroll { it.zoomOut()   }) return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun scroll(action: (BrlScrollView) -> Boolean): Boolean {
        val sv = scrollView ?: return false
        val moved = action(sv)
        if (moved) { sv.getPage(brailleScreen); refreshScreen(); speakVisible() }
        return moved
    }

    // ── API call ──────────────────────────────────────────────────────────────

    private fun sendPrompt(prompt: String) {
        mode = Mode.LOADING
        ensureBrailleView()
        clearScreen(); refreshScreen()
        manager.announceText("Thinking")

        lifecycleScope.launch {
            callClaude(prompt)
                .onSuccess { showResponse(it) }
                .onFailure { showResponse("Error: ${it.message}") }
        }
    }

    private fun showResponse(text: String) {
        mode = Mode.RESPONSE
        val sv = BrlScrollView(text, screenDimensions, ::translate)
        sv.getPage(brailleScreen)
        scrollView = sv
        refreshScreen()
        manager.announceText(text.take(200))
    }

    private suspend fun callClaude(prompt: String): Result<String> = withContext(Dispatchers.IO) {
        val key = apiKey()
        if (key.isBlank()) return@withContext Result.failure(Exception("No API key stored."))

        val body = JSONObject().apply {
            put("model", "claude-sonnet-4-6")
            put("max_tokens", 1024)
            put("messages", JSONArray().put(JSONObject().apply {
                put("role", "user")
                put("content", prompt)
            }))
        }.toString().toRequestBody("application/json".toMediaType())

        val req = Request.Builder()
            .url("https://api.anthropic.com/v1/messages")
            .addHeader("x-api-key", key)
            .addHeader("anthropic-version", "2023-06-01")
            .post(body)
            .build()

        try {
            val resp     = http.newCall(req).execute()
            val bodyText = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                val msg = runCatching {
                    JSONObject(bodyText).getJSONObject("error").getString("message")
                }.getOrDefault("API error ${resp.code}")
                return@withContext Result.failure(Exception(msg))
            }
            val text = JSONObject(bodyText).getJSONArray("content").getJSONObject(0).getString("text")
            Result.success(text)
        } catch (e: Exception) {
            Result.failure(e)
        }
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

    private fun refreshScreen() {
        mutableViewedImage.value = brailleScreen.matrix
        mutableLiveDots.value    = brailleScreen.matrix
    }

    private fun clearScreen() {
        brailleScreen.include(
            DotsMatrix(Array(screenDimensions.height) { ByteArray(screenDimensions.width) { 0 } }),
            0, 0
        )
    }

    private fun speakVisible() {
        val text = scrollView?.getTextOnScreen() ?: return
        if (text.isNotEmpty()) manager.announceText(text)
    }

    private fun apiKey(): String =
        getSharedPreferences("claude_prefs", Context.MODE_PRIVATE)
            .getString("api_key", "").orEmpty()

    private fun saveApiKey(key: String) =
        getSharedPreferences("claude_prefs", Context.MODE_PRIVATE)
            .edit().putString("api_key", key).apply()

    override fun onResume() {
        if (brailleReady) manager.bindService()
        super.onResume()
    }

    override fun onStop() {
        if (brailleReady) { manager.unbindService(); translator.unbindService() }
        super.onStop()
    }
}
