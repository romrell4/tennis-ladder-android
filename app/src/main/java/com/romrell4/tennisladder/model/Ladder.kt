package com.romrell4.tennisladder.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Ladder(
    val id: Int,
    val name: String
): Parcelable