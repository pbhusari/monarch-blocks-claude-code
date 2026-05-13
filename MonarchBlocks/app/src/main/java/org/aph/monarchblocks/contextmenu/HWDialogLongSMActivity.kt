/*
 * Copyright (c) 2026 American Printing House for the Blind
 * Use of this source code is governed by an MIT-style license that can be found in the LICENSE file.
 */

package org.aph.monarchblocks.contextmenu

import android.os.Bundle
import android.widget.TextView
import com.humanware.keysoftsdk.contextmenu.ContextMenuActivity
import org.aph.monarchblocks.R
import org.aph.monarchblocks.monarch_utils.enums.ResultCode

class HWDialogLongSMActivity: ContextMenuActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTitle(R.string.hw_dialog_long_context_menu)

        findViewById<TextView>(com.humanware.keysoftsdk.R.id.contextMenuTitle).apply {
            setText(R.string.hw_dialog_long_context_menu)
        }

        addContextMenuItem(getString(R.string.simple_dialog))
        addContextMenuItem(getString(R.string.loading_dialog))
        addContextMenuItem(getString(R.string.progress_dialog))
        addContextMenuItem(getString(R.string.ok_dialog))
        addContextMenuItem(getString(R.string.ok_cancel_dialog))
        addContextMenuItem(getString(R.string.edit_text_dialog))

        setOnItemClickListener { _, _, position, _ ->
            when (position) {
                0 -> setResult(ResultCode.SIMPLE_LONG_HW.value)
                1 -> setResult(ResultCode.LOADING_LONG_HW.value)
                2 -> setResult(ResultCode.PROGRESS_LONG_HW.value)
                3 -> setResult(ResultCode.OK_LONG_HW.value)
                4 -> setResult(ResultCode.OK_CANCEL_LONG_HW.value)
                5 -> setResult(ResultCode.EDIT_TEXT_LONG_HW.value)
            }

            finish()
        }
    }
}