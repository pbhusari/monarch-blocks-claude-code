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

class NativeDialogLongSMActivity: ContextMenuActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTitle(R.string.native_dialogs_long_title)

        findViewById<TextView>(com.humanware.keysoftsdk.R.id.contextMenuTitle).apply {
            setText(R.string.native_dialogs_long_title)
        }

        addContextMenuItem(getString(R.string.simple_dialog))
        addContextMenuItem(getString(R.string.ok_dialog))
        addContextMenuItem(getString(R.string.ok_cancel_dialog))

        setOnItemClickListener { _, _, position, _ ->
            when (position) {
                0 -> setResult(ResultCode.SIMPLE_LONG_NATIVE.value)
                1 -> setResult(ResultCode.OK_LONG_NATIVE.value)
                2 -> setResult(ResultCode.OK_CANCEL_LONG_NATIVE.value)
            }

            finish()
        }
    }
}