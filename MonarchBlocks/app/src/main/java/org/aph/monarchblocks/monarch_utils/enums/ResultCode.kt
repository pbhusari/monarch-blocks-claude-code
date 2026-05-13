/*
 * Copyright (c) 2026 American Printing House for the Blind
 * Use of this source code is governed by an MIT-style license that can be found in the LICENSE file.
 */

package org.aph.monarchblocks.monarch_utils.enums

/**
 * Used for the Result returned by clicking Context Menu items.
 */
enum class ResultCode(val value: Int) {
    //SelfBraillingWiget Codes
    BITMAPS(1),
    RANDOM(2),
    VERTICAL_LINES(3),
    //Dialog Codes
    SIMPLE_HW(4),
    LOADING_HW(5),
    PROGRESS_HW(6),
    OK_HW(7),
    OK_CANCEL_HW(20),
    EDIT_TEXT_HW(8),
    SIMPLE_NATIVE(9),
    OK_NATIVE(10),
    OK_CANCEL_NATIVE(11),
    //Dialog Codes for Longer Text
    SIMPLE_LONG_HW(12),
    LOADING_LONG_HW(13),
    PROGRESS_LONG_HW(14),
    OK_LONG_HW(15),
    OK_CANCEL_LONG_HW(23),
    EDIT_TEXT_LONG_HW(16),
    SIMPLE_LONG_NATIVE(17),
    OK_LONG_NATIVE(18),
    OK_CANCEL_LONG_NATIVE(19),
    TOAST_MSG(21),
    SNACKBAR_MSG(22)
}