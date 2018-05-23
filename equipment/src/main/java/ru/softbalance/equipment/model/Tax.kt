package ru.softbalance.equipment.model

import android.os.Parcel
import android.os.Parcelable

data class Tax(var id: Long = 0,
               var title: String = "") : Parcelable {
    constructor(source: Parcel) : this(
        source.readLong(),
        source.readString()
    )

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
        writeLong(id)
        writeString(title)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<Tax> = object : Parcelable.Creator<Tax> {
            override fun createFromParcel(source: Parcel): Tax = Tax(source)
            override fun newArray(size: Int): Array<Tax?> = arrayOfNulls(size)
        }
    }
}
