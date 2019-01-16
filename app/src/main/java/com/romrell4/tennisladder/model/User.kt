package com.romrell4.tennisladder.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class User(
	val userId: String,
	var name: String,
	var email: String,
	var phoneNumber: String?,
	var photoUrl: String?
): Parcelable