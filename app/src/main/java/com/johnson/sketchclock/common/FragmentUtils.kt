package com.johnson.sketchclock.common

import android.content.Context
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.johnson.sketchclock.databinding.DialogEdittextBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

fun Fragment.launchWhenStarted(block: suspend () -> Unit) {
    viewLifecycleOwner.lifecycleScope.launch {
        viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            block()
        }
    }
}

fun <T> Flow<T>.collectLatestWhenStarted(fragment: Fragment, block: suspend (T) -> Unit) {
    fragment.viewLifecycleOwner.lifecycleScope.launch {
        fragment.viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            collectLatest {
                block(it)
            }
        }
    }
}

fun <T> Flow<T>.collectLatestWhenStarted(activity: ComponentActivity, block: suspend (T) -> Unit) {
    activity.lifecycleScope.launch {
        activity.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            collectLatest {
                block(it)
            }
        }
    }
}

fun Fragment.showEditTextDialog(title: String, defaultText: String? = null, onDone: (String) -> Unit) {
    val ctx = context ?: return
    val dialogVb = DialogEdittextBinding.inflate(layoutInflater)

    MaterialAlertDialogBuilder(ctx)
        .setTitle(title)
        .setView(dialogVb.root)
        .setPositiveButton(android.R.string.ok) { _, _ ->
            val newName = dialogVb.etText.text?.toString()?.trim()
            if (newName.isNullOrEmpty()) {
                Toast.makeText(ctx, "Input cannot be empty", Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }
            onDone(newName)
        }
        .setNegativeButton(android.R.string.cancel, null)
        .show()

    dialogVb.etText.setText(defaultText)
    dialogVb.etText.selectAll()
    dialogVb.etText.requestFocus()

    lifecycleScope.launch {
        delay(300)
        val imm = ctx.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(dialogVb.etText, InputMethodManager.SHOW_IMPLICIT)
    }
}

fun Fragment.showDialog(title: String, message: String? = null, onConfirm: () -> Unit) {
    val ctx = context ?: return
    MaterialAlertDialogBuilder(ctx)
        .setTitle(title)
        .setMessage(message)
        .setPositiveButton(android.R.string.ok) { _, _ ->
            onConfirm()
        }
        .setNegativeButton(android.R.string.cancel, null)
        .show()
}