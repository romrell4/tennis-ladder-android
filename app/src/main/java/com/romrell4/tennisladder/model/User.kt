package com.romrell4.tennisladder.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class User(
	val userId: String,
	var name: String,
	var email: String,
	var phoneNumber: String?,
	var photoUrl: String?,
	var availabilityText: String?,
): Parcelable {
	override fun hashCode(): Int {
		return userId.hashCode()
	}

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (javaClass != other?.javaClass) return false

		other as User

		if (userId != other.userId) return false

		return true
	}
}
