package com.romrell4.tennisladder.support

import android.content.Intent
import android.os.Parcelable

fun <T: Parcelable> Intent.requireParcelableExtra(name: String): T = getParcelableExtra<T>(name) ?: throw IllegalStateException("Extra '$name' not found in extra")