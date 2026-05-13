/*
 * Copyright (c) 2026 American Printing House for the Blind
 * Use of this source code is governed by an MIT-style license that can be found in the LICENSE file.
 */

package org.aph.monarchblocks.activities

import android.content.Intent
import android.os.Bundle
import com.humanware.keysoftsdk.R
import com.humanware.keysoftsdk.ui.menu.AbstractMenuActivity
import com.humanware.keysoftsdk.ui.menu.AccessibleListView
import com.humanware.keysoftsdk.ui.menu.accessibleitem.AccessibleItem
import com.humanware.keysoftsdk.ui.menu.accessibleitem.attributes.AccessibleItemAttributes

class SelfBraillingMenuActivity: AbstractMenuActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTitle(org.aph.monarchblocks.R.string.self_brailling_widget_menu_title)
    }

    /**
     * Gets the resource id for the menu's title.
     *
     * @return the resource id for the string corresponding to the menu's title
     */
    override fun getTitleId() = org.aph.monarchblocks.R.string.self_brailling_widget_menu_title


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
        return findViewById<AccessibleListView>(R.id.settings_listview).apply {
            //Add items to the list view
            setNext(makeAccessibleItem(org.aph.monarchblocks.R.string.show_image_example))
            setNext(makeAccessibleItem(org.aph.monarchblocks.R.string.braille_and_image_example))
            setNext(makeAccessibleItem(org.aph.monarchblocks.R.string.scrolling_braille_image_example))
            setNext(makeAccessibleItem(org.aph.monarchblocks.R.string.scrolling_braille_only_example))
            setNext(makeAccessibleItem(org.aph.monarchblocks.R.string.grid_movement_example))

            setOnItemClickListener { _, _, position, _ ->
                val targetActivity = when (position) {
                    0 -> SelfBraillingActivity::class.java
                    1 -> null
                    2 -> null
                    3 -> null
                    4 -> null
                    else -> null
                }

                if(targetActivity != null) {
                    startActivity(Intent(this@SelfBraillingMenuActivity, targetActivity))
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
                                   layoutId: Int = R.layout.one_textview,
                                   labelId: Int = R.id.textview): AccessibleItem {
        return AccessibleItem(
            AccessibleItemAttributes(
            resources.getString(labelNameId),
            layoutId,
            labelId
        )
        )
    }
}