package com.romrell4.tennisladder.support

import android.content.Intent
import android.view.View
import androidx.core.view.isVisible
import com.romrell4.tennisladder.R
import com.romrell4.tennisladder.repository.ServiceException

inline fun <reified T> Intent.getExtra(name: String) = extras?.get(name) as? T
inline fun <reified T> Intent.requireExtra(name: String) = this.getExtra<T>(name) ?: throw IllegalStateException("Extra '$name' not found in extra")

/**
 * Converts a Throwable to a user-friendly message.
 * If the Throwable is a ServiceException, it extracts the error message.
 * Otherwise, it returns a generic error message with the Throwable's message.
 * Matches the original Callback<T> onError behavior.
 */
fun Throwable.readableMessage(): String {
    return (this as? ServiceException)?.error?.error
        ?: App.context.getString(R.string.error_message, message)
}

fun <T, V : View> V.bindVisibilityTo(data: T?, block: V.(T) -> Unit) {
    isVisible = data != null
    data?.let { this.block(it) }
}
