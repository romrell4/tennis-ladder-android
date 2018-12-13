package com.romrell4.tennisladder.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import java.util.*

@Parcelize
data class Ladder(
	val ladderId: Int,
	val name: String,
	val startDate: Date,
	val endDate: Date
): Parcelable