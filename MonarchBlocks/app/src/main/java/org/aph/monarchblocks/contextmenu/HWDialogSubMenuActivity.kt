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

class HWDialogSubMenuActivity: ContextMenuActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTitle(R.string.humanware_dialog_context_menu)//This is to set the name of the activity

        findViewById<TextView>(com.humanware.keysoftsdk.R.id.contextMenuTitle).apply {
            setText(R.string.humanware_dialog_context_menu)//This is to set the title in the viewable screen.
        }

        addContextMenuItem(getString(R.string.simple_dialog))
        addContextMenuItem(getString(R.string.loading_dialog))
        addContextMenuItem(getString(R.string.progress_dialog))
        addContextMenuItem(getString(R.string.ok_dialog))
        addContextMenuItem(getString(R.string.ok_cancel_dialog))
        addContextMenuItem(getString(R.string.edit_text_dialog))

        setOnItemClickListener { _, _, position, _ ->
            when (position) {
                0 -> setResult(ResultCode.SIMPLE_HW.value)
                1 -> setResult(ResultCode.LOADING_HW.value)
                2 -> setResult(ResultCode.PROGRESS_HW.value)
                3 -> setResult(ResultCode.OK_HW.value)
                4 -> setResult(ResultCode.OK_CANCEL_HW.value)
                5 -> setResult(ResultCode.EDIT_TEXT_HW.value)
            }

            finish()
        }
    }
}