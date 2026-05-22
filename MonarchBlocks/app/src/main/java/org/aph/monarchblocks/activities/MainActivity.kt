/*
 * Copyright (c) 2026 American Printing House for the Blind
 * Use of this source code is governed by an MIT-style license that can be found in the LICENSE file.
 */

package org.aph.monarchblocks.activities

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import com.humanware.keysoftsdk.ui.menu.AbstractMenuActivity
import com.humanware.keysoftsdk.ui.menu.AccessibleListView
import com.humanware.keysoftsdk.ui.menu.accessibleitem.AccessibleItem
import com.humanware.keysoftsdk.ui.menu.accessibleitem.attributes.AccessibleItemAttributes
import dagger.hilt.android.AndroidEntryPoint
import org.aph.monarchblocks.contextmenu.SimpleContextMenuActivity
import org.aph.monarchblocks.monarch_utils.DialogHandler
import org.aph.monarchblocks.monarch_utils.enums.ResultCode
import com.humanware.keysoftsdk.R as sdkR
import org.aph.monarchblocks.R as BlocksAppR


/**
 * Implements the main menu view of this demo app with a custom menu
 * defined using [AbstractMenuActivity].
 *
 * An [AbstractMenuActivity] contains two components:
 * - a [TextView], the title bar of the activity
 * - an [AccessibleListView], the view containing each clickable item
 *
 * @see sdkR.layout.sdk_settings_layout
 */
@AndroidEntryPoint
class MainActivity : AbstractMenuActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTitle(BlocksAppR.string.main_menu)
    }

    /**
     * Gets the resource id for the menu's title.
     *
     * @return the resource id for the string corresponding to the menu's title
     */
    override fun getTitleId() = BlocksAppR.string.main_menu


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
        listview = inflateSettingItemLayout()

        return AccessibleListView.Sort.BY_POSITION
    }

    /**
     * Inflates the [AccessibleListView] item in [sdkR.layout.sdk_settings_layout]
     * to create a list of settings for the main menu.
     *
     * Because items are sorted by position as defined in [initialize], setNext is
     * used to place items in order one after the other.
     *
     * @see DialogSampleActivity for an example of layout sorted by label.
     *
     * @return an [AccessibleListView] item constructed from the specifications in [sdkR.layout.accessible_listview_layout]
     */
    private fun inflateSettingItemLayout(): AccessibleListView {
        return findViewById<AccessibleListView>(sdkR.id.settings_listview).apply {
            //Add items to the list view
            setNext(makeAccessibleItem(BlocksAppR.string.self_brailling_samples))
            setNext(makeAccessibleItem(BlocksAppR.string.layout_samples))
            setNext(makeAccessibleItem(BlocksAppR.string.braille_translation_sample))
            setNext(makeAccessibleItem(BlocksAppR.string.claude_code_demo))


            setOnItemClickListener { _, _, position, _ ->
                val targetActivity = when (position) {
                    0 -> SelfBraillingMenuActivity::class.java
                    1 -> null
                    2 -> null
                    3 -> ClaudeCodeDemoActivity::class.java
                    else -> null
                }

                if(targetActivity != null) {
                    startActivity(Intent(this@MainActivity, targetActivity))
                }
            }
        }
    }

    /**
     * Creates an [AccessibleItem] to be displayed in the menu's [AccessibleListView].
     *
     * @param labelName the name of the item to be displayed
     * @param layoutId the id of the layout for the menu item
     * @param labelId the id of the label item
     *
     * @return an [AccessibleItem] to be inserted in the main menu
     */
    private fun makeAccessibleItem(labelNameId: Int,
                                   layoutId: Int = sdkR.layout.one_textview,
                                   labelId: Int = sdkR.id.textview): AccessibleItem {
        return AccessibleItem(AccessibleItemAttributes(
            resources.getString(labelNameId),
            layoutId,
            labelId
        ))
    }


    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when(keyCode){
            KeyEvent.KEYCODE_MENU -> {
                val intent = Intent(this, SimpleContextMenuActivity::class.java)
                startContextMenu.launch(intent)
            }
        }

        return super.onKeyDown(keyCode, event)
    }


    private val startContextMenu = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        when(it.resultCode){
            ResultCode.TOAST_MSG.value -> DialogHandler.showToastMsg(this)
            ResultCode.SNACKBAR_MSG.value -> DialogHandler.showSnackBarMsg(window.decorView.rootView, this)
            else -> DialogHandler.showDialog(it.resultCode, this)
        }
    }
}
