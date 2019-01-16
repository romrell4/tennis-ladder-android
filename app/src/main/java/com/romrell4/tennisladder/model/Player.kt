package com.romrell4.tennisladder.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Player(
	val user: User,
	val ladderId: Int,
	val score: Int,
	val ranking: Int,
	val wins: Int,
	val losses: Int
): Parcelable