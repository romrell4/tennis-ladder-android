package com.romrell4.tennisladder.model

import android.os.Parcel
import android.os.Parcelable

data class Ladder(
    val id: Int,
    val name: String
): Parcelable {
    constructor(parcel: Parcel): this(
        parcel.readInt(),
        parcel.readString()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(id)
        parcel.writeString(name)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR: Parcelable.Creator<Ladder> {
        override fun createFromParcel(parcel: Parcel): Ladder {
            return Ladder(parcel)
        }

        override fun newArray(size: Int): Array<Ladder?> {
            return arrayOfNulls(size)
        }
    }
}