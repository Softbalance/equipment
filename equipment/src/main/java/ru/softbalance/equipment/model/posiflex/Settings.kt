package ru.softbalance.equipment.model.posiflex

import android.os.Parcel
import android.os.Parcelable
import ru.softbalance.equipment.model.DeviceConnectionType

data class Settings(
    var connectionType: DeviceConnectionType = DeviceConnectionType.NETWORK,
    var productId: Int = 0,
    var deviceName: String = "",
    var host: String = "192.168.",
    var port: Int = 9100,
    var codePage: Int = 28,
    var offsetHeaderBottom: Int = 0) : Parcelable {

    constructor(source: Parcel) : this(
        DeviceConnectionType.values()[source.readInt()],
        source.readInt(),
        source.readString(),
        source.readString(),
        source.readInt(),
        source.readInt(),
        source.readInt()
    )

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
        writeInt(connectionType.ordinal)
        writeInt(productId)
        writeString(deviceName)
        writeString(host)
        writeInt(port)
        writeInt(codePage)
        writeInt(offsetHeaderBottom)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<Settings> = object : Parcelable.Creator<Settings> {
            override fun createFromParcel(source: Parcel): Settings = Settings(source)
            override fun newArray(size: Int): Array<Settings?> = arrayOfNulls(size)
        }
    }
}