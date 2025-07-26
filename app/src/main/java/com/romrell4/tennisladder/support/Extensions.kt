package com.romrell4.tennisladder.support

import android.content.Intent
import com.romrell4.tennisladder.repository.ServiceException

inline fun <reified T> Intent.getExtra(name: String) = extras?.get(name) as? T
inline fun <reified T> Intent.requireExtra(name: String) = this.getExtra<T>(name) ?: throw IllegalStateException("Extra '$name' not found in extra")

/**
 * Converts a Throwable to a user-friendly message.
 * If the Throwable is a ServiceException, it extracts the error message.
 * Otherwise, it returns a generic error message with the Throwable's message.
 */
fun Throwable.readableMessage(): String {
    return (this as? ServiceException)?.error?.error
        ?: "An error occurred. Details: $message"
}
