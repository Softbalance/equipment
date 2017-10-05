package ru.softbalance.equipment.model.posiflex.ports

import java.io.IOException

interface Port {
    @Throws(IOException::class)
    fun open()

    fun close()

    @Throws(IOException::class)
    fun write(data: ByteArray)

    @Throws(IOException::class)
    fun read(size: Int, timeout: Int): ByteArray
}