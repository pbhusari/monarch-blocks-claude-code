/*
 * Copyright (c) 2026 American Printing House for the Blind
 * Use of this source code is governed by an MIT-style license that can be found in the LICENSE file.
 */

package org.aph.monarchblocks.monarch_utils

import android.content.Context
import android.content.DialogInterface
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.android.material.snackbar.Snackbar
import com.humanware.keysoftsdk.ui.dialog.ConfirmDialogListener
import com.humanware.keysoftsdk.ui.dialog.DialogCreator
import com.humanware.keysoftsdk.ui.dialog.EditTextDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.aph.monarchblocks.R
import org.aph.monarchblocks.monarch_utils.enums.ResultCode

class DialogHandler {
    companion object {
        private val job = SupervisorJob()
        private val scope = CoroutineScope(job + Dispatchers.IO)


        fun showDialog(code: Int, context: Context){
            when(code){
                ResultCode.SIMPLE_HW.value -> showSimpleDialog(true, context)
                ResultCode.LOADING_HW.value -> showLoadingDialog(context)
                ResultCode.PROGRESS_HW.value -> showProgressDialog(context)
                ResultCode.OK_HW.value -> showOkDialog(true, context)
                ResultCode.OK_CANCEL_HW.value -> showOkCancelDialog(true, context)
                ResultCode.SIMPLE_NATIVE.value -> showSimpleDialog(false, context)
                ResultCode.OK_NATIVE.value -> showOkDialog(false, context)
                ResultCode.OK_CANCEL_NATIVE.value -> showOkCancelDialog(false, context)
            }
        }


        /**
         * Displays a basic dialog which may be closed upon the user pressing the only
         * button available, called the neutral button.
         */
        private fun showSimpleDialog(isHumanWareVersion: Boolean, context: Context) {
            if (isHumanWareVersion) {
                val onNeutralClick = { dialog: DialogInterface, _: Int -> dialog.dismiss() }
                val dialog = DialogCreator.createDialog(
                    context,
                    context.resources.getString(R.string.simple_dialog),
                    context.resources.getString(R.string.ok),
                    context.resources.getString(R.string.simple_dialog_content),
                    onNeutralClick
                )

                dialog.show()
            } else {
                AlertDialog.Builder(context)
                    .setTitle(context.resources.getString(R.string.simple_dialog))
                    .setMessage(context.resources.getString(R.string.simple_dialog_content))
                    .setCancelable(true)
                    .show()
            }
        }


        /**
         * Displays a message prompting the user to wait for a task to be completed.
         *
         * The dialog automatically closes upon completion of the task.
         *
         * HumanWare Only Version
         */
        private fun showLoadingDialog(context: Context) {
            val dialog = DialogCreator.createLoading(
                context,
                context.resources.getString(com.humanware.keysoftsdk.R.string.please_wait)
            )

            dialog.show()

            // This only simulates a workload that takes time to perform.
            // The actual work performed by your application would go here.
            scope.launch {
                delay(5000) //Waits 5 seconds
            }.invokeOnCompletion { dialog.dismiss() }
        }


        /**
         * Displays a message prompting the user to wait for a task to be completed.
         *
         * The dialog automatically closes upon completion of the task and may be closed
         * earlier by the user upon selecting cancel.
         *
         * TODO: Progress value not displayed in Braille unless focus moved from "Please wait"
         *       to the percentage value.
         */
        private fun showProgressDialog(context: Context) {
            val progressRange = (0..100)

            val onCancelClick = { dialog: DialogInterface -> dialog.cancel() }
            val dialog = DialogCreator.createProgressDialog(
                context,
                context.resources.getString(com.humanware.keysoftsdk.R.string.please_wait),
                onCancelClick
            )

            dialog.show()

            // This only simulates a workload that takes time to perform.
            // Your actual application may want to perform intensive work as a separate coroutine
            // and update the ProgressDialog bar on the main thread using another coroutine
            // running concurrently.
            // TODO: potentially refactor the usage of simulateIntensiveWork to reflect this more closely,
            //       but it visually works for now.
            scope.launch {
                progressRange.forEach { progressLevel ->
                    delay(250)
                    context.mainExecutor.execute { dialog.setProgress(progressLevel) }
                }
            }.invokeOnCompletion { dialog.dismiss() }

        }


        /**
         * Displays a message to the user, which they may then close by pressing the OK item.
         */
        private fun showOkDialog(isHumanWareVersion: Boolean, context: Context) {
            if (isHumanWareVersion) {
                val onOkClick = { dialog: DialogInterface, _: Int -> dialog.dismiss() }
                val dialog = DialogCreator.createOkDialog(
                    context,
                    context.resources.getString(R.string.ok_title),
                    context.resources.getString(R.string.ok_only),
                    onOkClick
                )

                dialog.show()
            } else {
                AlertDialog.Builder(context)
                    .setTitle(context.resources.getString(R.string.ok_dialog))
                    .setMessage(context.resources.getString(R.string.ok_only))
                    .setPositiveButton(context.resources.getString(R.string.ok)) { dialog, _ ->
                        dialog.dismiss()
                    }
                    .setCancelable(true)
                    .show()
            }
        }

        /**
         * Displays a message to the user and prompts them to choose between an positive (OK)
         * and negative (Cancel) item.
         */
        private fun showOkCancelDialog(isHumanWareVersion: Boolean, context: Context) {
            if (isHumanWareVersion) {
                val onCancelClick = { dialog: DialogInterface, _: Int -> dialog.cancel() }
                val onOkClick = { dialog: DialogInterface, _: Int -> dialog.dismiss() }
                val dialog = DialogCreator.createOkCancelDialog(
                    context,
                    context.resources.getString(R.string.ok_or_cancel),
                    onCancelClick,
                    onOkClick
                )

                dialog.show()
            } else {
                AlertDialog.Builder(context)
                    .setTitle(context.resources.getString(R.string.ok_cancel_dialog))
                    .setMessage(context.resources.getString(R.string.ok_or_cancel))
                    .setCancelable(true)
                    .setPositiveButton(context.resources.getString(R.string.ok)) { dialog, _ ->
                        dialog.dismiss()
                    }
                    .setNegativeButton(context.resources.getString(R.string.cancel)) { dialog, _ ->
                        dialog.cancel()
                    }
            }
        }


        /**
         * Shows an EditTextDialog on any screen
         */
        private fun showEditTextDialog(onConfirm: () -> Unit, context: Context) {
            EditTextDialog(context).apply {
                setButtonText(R.string.translate)
                setConfirmListener(object : ConfirmDialogListener {
                    override fun onConfirm(textField: String?) {
                        dismiss()
                        onConfirm()
                    }

                    override fun onCancel() = dismiss()
                })
            }.show()
        }


        /**
         * Shows a simple Toast Message
         */
        fun showToastMsg(context: Context) {
            val duration = Toast.LENGTH_SHORT
            Toast.makeText(context, context.resources.getString(R.string.toast_msg), duration)
                .show()
        }


        /**
         * Shows a simple SnackBar message
         */
        fun showSnackBarMsg(rootView: View, context: Context) {
            Snackbar.make(
                rootView,
                context.resources.getString(R.string.snackbar_msg),
                Snackbar.LENGTH_LONG
            ).show()
        }
    }
}