/*
 * Copyright (c) 2026 American Printing House for the Blind
 * Use of this source code is governed by an MIT-style license that can be found in the LICENSE file.
 */

package org.aph.monarchblocks.contextmenu


import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import com.humanware.keysoftsdk.contextmenu.ContextMenuActivity
import org.aph.monarchblocks.R
import org.aph.monarchblocks.monarch_utils.enums.ResultCode

/**
 * Implementation of a context menu for the self-brailling widget activity.
 * Allows retaining the state of the activity and opening a context menu offering
 * some options to change the state of the activity.
 */
class SelfBraillingContextMenuActivity : ContextMenuActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTitle(R.string.context_menu_self_brailling)

        findViewById<TextView>(com.humanware.keysoftsdk.R.id.contextMenuTitle).apply {
            setText(R.string.context_menu_self_brailling)
        }

        addContextMenuItem("bitmaps")
        addContextMenuItem("random")
        addContextMenuItem("vertical_lines")
        addContextMenuItem("⣿" + getString(R.string.hw_dialog_submenu))
        addContextMenuItem("⣿" + getString(R.string.hw_dialog_long_submenu))
        addContextMenuItem("⣿" + getString(R.string.native_dialogs_submenu))
        addContextMenuItem("⣿" + getString(R.string.native_dialogs_long_submenu))
        addContextMenuItem("show_toast")
        addContextMenuItem("show_snackbar")

        setOnItemClickListener { _, _, position, _ ->
            when (position) {
                0 -> setResult(ResultCode.BITMAPS.value)
                1 -> setResult(ResultCode.RANDOM.value)
                2 -> setResult(ResultCode.VERTICAL_LINES.value)
                3 -> {
                    val intent = Intent(this@SelfBraillingContextMenuActivity, HWDialogSubMenuActivity::class.java)
                    intent.setFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT)//Required for Submenu to work properly
                    startActivity(intent)
                }
                4 -> {
                    val intent = Intent(this@SelfBraillingContextMenuActivity, HWDialogLongSMActivity::class.java)
                    intent.setFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT)//Required for Submenu to work properly
                    startActivity(intent)
                }
                5 -> {
                    val intent = Intent(this@SelfBraillingContextMenuActivity, NativeDialogSubMenuActivity::class.java)
                    intent.setFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT)//Required for Submenu to work properly
                    startActivity(intent)
                }
                6 -> {
                    val intent = Intent(this@SelfBraillingContextMenuActivity, NativeDialogLongSMActivity::class.java)
                    intent.setFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT)//Required for Submenu to work properly
                    startActivity(intent)
                }
                7 -> setResult(ResultCode.TOAST_MSG.value)
                8 -> setResult(ResultCode.SNACKBAR_MSG.value)
            }

            finish()
        }
    }
}