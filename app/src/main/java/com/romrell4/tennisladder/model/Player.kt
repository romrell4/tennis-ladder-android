package com.romrell4.tennisladder.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Player(
	val userId: String,
	val ladderId: Int,
	val name: String,
	val photoUrl: String?,
	val score: Int,
	val ranking: Int,
	val wins: Int,
	val losses: Int
): Parcelable