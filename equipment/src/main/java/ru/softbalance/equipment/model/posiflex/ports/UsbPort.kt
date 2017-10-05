package ru.softbalance.equipment.model.posiflex.ports

import android.content.Context
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import com.hoho.android.usbserial.driver.CdcAcmSerialDriver
import com.hoho.android.usbserial.driver.ProbeTable
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import java.io.IOException
import java.util.*

class UsbPort(private val ctx: Context,
              private val device: UsbDevice) : Port {

    private var port: UsbSerialPort? = null

    @Throws(IOException::class)
    override fun open() {
        val manager = ctx.getSystemService(Context.USB_SERVICE) as UsbManager
        if (!manager.hasPermission(device)) {
            throw IOException("No permissions")
        }
        val connection = manager.openDevice(device) ?: throw IOException("Can't open device")

        val customTable = ProbeTable()
                .addProduct(device.vendorId, device.productId, CdcAcmSerialDriver::class.java)
        val driver = UsbSerialProber(customTable).findAllDrivers(manager).first()
        port = driver.ports[0].also {
            it.open(connection)
            it.setParameters(115200, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE)
        }
    }

    override fun close() {
        try {
            port?.close()
            port = null
        } catch (e: IOException) {
            // ignore
        }
    }

    @Throws(IOException::class)
    override fun write(data: ByteArray) {
        port?.write(data, 1000)
    }

    @Throws(IOException::class)
    override fun read(size: Int, timeout: Int): ByteArray {
        val data = ByteArray(size)
        return Arrays.copyOf(data, port?.read(data, timeout) ?: 0)
    }
}