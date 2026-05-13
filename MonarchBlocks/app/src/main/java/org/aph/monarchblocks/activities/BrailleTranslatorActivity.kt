/*
 * Copyright (c) 2026 American Printing House for the Blind
 * Use of this source code is governed by an MIT-style license that can be found in the LICENSE file.
 */

package org.aph.monarchblocks.activities

import android.os.Bundle
import com.humanware.keysoftsdk.translator.Translator
import com.humanware.keysoftsdk.ui.dialog.ConfirmDialogListener
import com.humanware.keysoftsdk.ui.dialog.EditTextDialog
import com.humanware.keysoftsdk.ui.menu.AbstractMenuActivity
import com.humanware.keysoftsdk.ui.menu.AccessibleListView
import com.humanware.keysoftsdk.ui.menu.accessibleitem.TwoTextViewItem
import com.humanware.keysoftsdk.R as sdkR
import org.aph.monarchblocks.R as demoR

/**
 * Implements the menu showcasing translation of text to and from Braille offered by the SDK.
 *
 * @property translator provides access to the translation utilities included in the SDK
 *
 * @see sdkR.layout.sdk_settings_layout
 */
class BrailleTranslatorActivity : AbstractMenuActivity() {

    private lateinit var translator: Translator


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        translator = Translator(this).apply { bindService() }
        title = getString(demoR.string.translator_activity_title)
    }

    /**
     * Gets the resource id for the menu's title.
     *
     * @return the resource id for the string corresponding to the menu's title
     */
    override fun getTitleId() = demoR.string.translator_activity_title

    /**
     * Any initialization required to setup the menu items is found here.
     *
     * This is where you should define the sort strategy to use to sort
     * the items displayed in the menu, which should be one of the
     * enum values in [AccessibleListView.Sort].
     *
     * Will be called in the [AbstractMenuActivity.onCreate] method.
     *
     * @return the sort strategy for the menu elements
     */
    override fun initialize(): AccessibleListView.Sort {
        listview = inflateTextInputLayout()

        return AccessibleListView.Sort.BY_POSITION
    }



    override fun onDestroy() {
        super.onDestroy()
        translator.unbindService()
    }

    /**
     * Inflates the [AccessibleListView] item in [sdkR.layout.sdk_settings_layout]
     * to create a list of Braille translation items for the menu.
     *
     * Because items are sorted by position as defined in [initialize], setNext is
     * used to place items in order one after the other.
     *
     * @see DialogSampleActivity for an example of layout sorted by label.
     *
     * @return an [AccessibleListView] item constructed from the specifications
     * in [sdkR.layout.accessible_listview_layout]
     */
    private fun inflateTextInputLayout(): AccessibleListView {
        val onForwardTranslate = { textField: String? -> translator.forwardTranslate(textField) }
        val onBackTranslate = { textField: String? -> translator.backTranslate(textField) }

        val forwardTranslationItem = TranslationItem(demoR.string.text_to_braille, onForwardTranslate, false)
        val backTranslationItem = TranslationItem(demoR.string.braille_to_text, onBackTranslate, true)

        return findViewById<AccessibleListView>(sdkR.id.settings_listview).apply {
            setNext(forwardTranslationItem)
            setNext(backTranslationItem)

            setOnItemClickListener { _, _, position, _ ->
                if (position == 0) {
                    forwardTranslationItem.onAction()
                } else {
                    backTranslationItem.onAction()
                }

            }
        }
    }

    /**
     * An accessible item extending [TwoTextViewItem] to provide two text views:
     * - a description of the translation activity related to the item
     * - the result of the translation, shown as: initial text → translated text
     *
     * @property action the translation action to perform
     * @property defaultBrailleText whether or not to include some sample Braille text
     *                              to allow for easier testing of the Braille to text
     *                              translation without having to input Braille.
     */
    private inner class TranslationItem(
        labelId: Int,
        val action: (String?) -> String,
        val defaultBrailleText: Boolean
    ) :
        TwoTextViewItem(this, labelId) {
        override fun onAction() {
            EditTextDialog(this@BrailleTranslatorActivity).apply {
                if (defaultBrailleText) {
                    setEditText(getString(demoR.string.braille_sample))
                }

                setButtonText(demoR.string.translate)
                setConfirmListener(object : ConfirmDialogListener {
                    override fun onConfirm(textField: String?) {
                        val translationResult = action(textField)
                        setSubLabel("$textField → $translationResult")
                        dismiss()
                    }

                    override fun onCancel() = dismiss()
                })
            }.show()
        }
    }
}