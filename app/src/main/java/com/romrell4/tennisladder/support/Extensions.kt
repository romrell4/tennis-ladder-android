package com.romrell4.tennisladder.support

import android.content.Intent

inline fun <reified T> Intent.getExtra(name: String) = extras?.get(name) as? T
inline fun <reified T> Intent.requireExtra(name: String) = this.getExtra<T>(name) ?: throw IllegalStateException("Extra '$name' not found in extra")
