package com.romrell4.tennisladder.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Player(
    val userId: Int,
    val ladderId: Int,
    val name: String,
    val score: Int,
    val ranking: Int,
    val wins: Int,
    val losses: Int
): Parcelable