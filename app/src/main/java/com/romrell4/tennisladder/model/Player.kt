package com.romrell4.tennisladder.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Player(
	val user: User,
	val ladderId: Int,
	val earnedPoints: Int,
	val borrowedPoints: Int,
	val score: Int,
	val ranking: Int,
	val wins: Int,
	val losses: Int
): Parcelable {
	override fun hashCode(): Int {
		return user.hashCode()
	}

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (javaClass != other?.javaClass) return false

		other as Player

		if (user != other.user) return false

		return true
	}
}
