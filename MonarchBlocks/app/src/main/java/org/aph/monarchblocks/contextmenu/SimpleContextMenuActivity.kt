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

class SimpleContextMenuActivity: ContextMenuActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTitle(R.string.context_menu_title)

        findViewById<TextView>(com.humanware.keysoftsdk.R.id.contextMenuTitle).apply {
            setText(R.string.context_menu_title)
        }

        addContextMenuItem("⣿" + getString(R.string.hw_dialog_submenu))
        addContextMenuItem("⣿" + getString(R.string.hw_dialog_long_submenu))
        addContextMenuItem("⣿" + getString(R.string.native_dialogs_submenu))
        addContextMenuItem("⣿" + getString(R.string.native_dialogs_long_submenu))
        addContextMenuItem("show_toast")
        addContextMenuItem("show_snackbar")

        setOnItemClickListener { _, _, position, _ ->
            when (position) {
                0 -> {
                    val intent = Intent(this@SimpleContextMenuActivity, HWDialogSubMenuActivity::class.java)
                    intent.setFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT)//Required for Submenu to work properly
                    startActivity(intent)
                }
                1 -> {
                    val intent = Intent(this@SimpleContextMenuActivity, HWDialogLongSMActivity::class.java)
                    intent.setFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT)//Required for Submenu to work properly
                    startActivity(intent)
                }
                2 -> {
                    val intent = Intent(this@SimpleContextMenuActivity, NativeDialogSubMenuActivity::class.java)
                    intent.setFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT)//Required for Submenu to work properly
                    startActivity(intent)
                }
                3 -> {
                    val intent = Intent(this@SimpleContextMenuActivity, NativeDialogLongSMActivity::class.java)
                    intent.setFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT)//Required for Submenu to work properly
                    startActivity(intent)
                }
                4 -> setResult(ResultCode.TOAST_MSG.value)
                5 -> setResult(ResultCode.SNACKBAR_MSG.value)
            }

            finish()
        }
    }
}