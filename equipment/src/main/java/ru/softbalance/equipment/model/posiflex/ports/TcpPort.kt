package ru.softbalance.equipment.model.posiflex.ports

import android.os.SystemClock

import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket

class TcpPort(private val host: String,
              private val port: Int) : Port {

    private var socket: Socket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null

    @Throws(IOException::class)
    override fun open() {
        socket = Socket(host, port).also {
            inputStream = it.getInputStream()
            outputStream = it.getOutputStream()
        }
    }

    override fun close() {
        try {
            socket?.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        socket = null
    }

    @Throws(IOException::class)
    override fun write(data: ByteArray) {
        outputStream?.let {
            it.write(data)
            it.flush()
        }
    }

    @Throws(IOException::class)
    override fun read(size: Int, timeout: Int): ByteArray {
        val input = inputStream ?: return ByteArray(0)

        val data = ByteArray(size)
        val end = SystemClock.elapsedRealtime() + timeout
        var counter = 0
        while (SystemClock.elapsedRealtime() <= end && counter < size) {
            val readed = input.read(data, counter, size - counter)
            if (readed > 0)
                counter += readed
        }
        return data
    }
}