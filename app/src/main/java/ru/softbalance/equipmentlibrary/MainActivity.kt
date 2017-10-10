package ru.softbalance.equipmentlibrary

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast
import com.atol.drivers.fptr.Fptr
import com.atol.drivers.fptr.IFptr
import com.atol.drivers.paycard.IPaycard
import com.atol.drivers.paycard.Paycard
import ru.softbalance.equipment.view.DriverSetupActivity
import ru.softbalance.equipment.view.DriverSetupActivity.Companion.DRIVER_ARG
import ru.softbalance.equipment.view.DriverSetupActivity.Companion.DRIVER_TYPE_ATOL
import ru.softbalance.equipment.view.DriverSetupActivity.Companion.DRIVER_TYPE_POSIFLEX
import ru.softbalance.equipment.view.DriverSetupActivity.Companion.DRIVER_TYPE_SERVER
import ru.softbalance.equipment.view.DriverSetupActivity.Companion.PORT_ARG
import ru.softbalance.equipment.view.DriverSetupActivity.Companion.SERIAL_ARG
import ru.softbalance.equipment.view.DriverSetupActivity.Companion.SETTINGS_ARG
import ru.softbalance.equipment.view.DriverSetupActivity.Companion.URL_ARG
import rx.Single
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers

fun Button?.drawTick(context: Context, show: Boolean) {
    this?.setCompoundDrawablesWithIntrinsicBounds(
            if (show) ContextCompat.getDrawable(context, ru.softbalance.equipment.R.drawable.ic_confirm_selector) else null,
            null,
            null,
            null)
}

class MainActivity : AppCompatActivity() {

    companion object {
        const val DRIVER_REQUEST = 1
        const val REQUEST_SHOW_PAYCARD_SETTINGS = 2
        const val PREFERENCES = "PREFERENCES"
        const val PAYCARD_PREF = "PAYCARD_PREF"
        const val ATOL_PRINT_PREF = "ATOL_PRINT_PREF"
    }

    lateinit private var preferences: SharedPreferences

    lateinit var buttonAtolKkm: Button
    lateinit var buttonAtolPc: Button
    lateinit var buttonAtolPay: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        preferences = getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

        setContentView(R.layout.activity_main)

        buttonAtolKkm = findViewById<View>(R.id.buttonAtolKkm) as Button
        buttonAtolKkm.setOnClickListener {
            startActivityForResult(Intent(this, DriverSetupActivity::class.java)
                    .putExtra(DRIVER_ARG, DRIVER_TYPE_ATOL), DRIVER_REQUEST)
        }

        buttonAtolPc = findViewById<View>(R.id.buttonAtolPc) as Button
        buttonAtolPc.setOnClickListener {
            val intent = Intent(this, com.atol.drivers.paycard.settings.SettingsActivity::class.java)
            var settings: String? = getPaycardSettings()
            if (settings == null) {
                settings = getPaycardDefaultSettings()
            }
            intent.putExtra(com.atol.drivers.paycard.settings.SettingsActivity.DEVICE_SETTINGS, settings)
            startActivityForResult(intent, REQUEST_SHOW_PAYCARD_SETTINGS)
        }
        buttonAtolPay = findViewById<View>(R.id.buttonAtolPay) as Button

        buttonAtolPay.setOnClickListener {
            paymentTransaction()
                    .subscribeOn(Schedulers.computation())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe( {
                        Toast.makeText(this, "finished", Toast.LENGTH_LONG).show()
                    }, {
                        Toast.makeText(this, it.message, Toast.LENGTH_LONG).show()
                    })
        }

        findViewById<View>(R.id.buttonPrinter).setOnClickListener {
            startActivityForResult(Intent(this, DriverSetupActivity::class.java)
                    .putExtra(DRIVER_ARG, DRIVER_TYPE_SERVER)
                    .putExtra(URL_ARG, "178.170.230.140")
                    .putExtra(PORT_ARG, 9004),
                    DRIVER_REQUEST)
        }

        findViewById<View>(R.id.buttonPosiflex).setOnClickListener {
            startActivityForResult(Intent(this, DriverSetupActivity::class.java)
                    .putExtra(DRIVER_ARG, DRIVER_TYPE_POSIFLEX),
                    DRIVER_REQUEST)
        }

        drawButtonsStates()
    }

    private fun drawButtonsStates(){
        buttonAtolKkm.drawTick(this, getFptrSettings() != null)
        buttonAtolPc.drawTick(this, getPaycardSettings() != null)
        buttonAtolPay.isEnabled = getFptrSettings() != null && getPaycardSettings() != null
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            DRIVER_REQUEST -> {
                val settings = if (data?.getStringExtra(SETTINGS_ARG).isNullOrEmpty())
                    "no settings data found" else data?.getStringExtra(SETTINGS_ARG)
                val serial = if (data?.getStringExtra(SERIAL_ARG).isNullOrEmpty())
                    "no serial data found" else data?.getStringExtra(SERIAL_ARG)

                settings?.let { setPrintSettings(it) }
                drawButtonsStates()
                Toast.makeText(this, "fr settings: " + settings?.substring(0, 20) + " ...", Toast.LENGTH_LONG).show()
                Toast.makeText(this, "serial: " + serial, Toast.LENGTH_LONG).show()
            }
            REQUEST_SHOW_PAYCARD_SETTINGS -> if (data != null && data.extras != null) {
                val settings = data.extras.getString(com.atol.drivers.paycard.settings.SettingsActivity.DEVICE_SETTINGS)
                setPaycardSettings(settings)
                drawButtonsStates()
                Toast.makeText(this, "payment system settings: " + settings.substring(0, 20) + " ...", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun getPaycardSettings(): String? {
        return preferences.getString(PAYCARD_PREF, null)
    }

    private fun setPaycardSettings(settings: String) {
        val editor = preferences.edit()
        editor.putString(PAYCARD_PREF, settings)
        editor.apply()
    }

    private fun getFptrSettings(): String? {
        return preferences.getString(ATOL_PRINT_PREF, null)
    }

    private fun setPrintSettings(settings: String) {
        val editor = preferences.edit()
        editor.putString(ATOL_PRINT_PREF, settings)
        editor.apply()
    }

    private fun getPaycardDefaultSettings(): String {
        val paycard = Paycard()
        paycard.create(this)
        val settings = paycard._DeviceSettings
        paycard.destroy()
        return settings
    }

    fun paymentTransaction(): Single<Boolean> = Single.fromCallable {
        val fptr = Fptr()
        val paycard = Paycard()

        try {
            fptr.create(application)
            paycard.create(application)

            publishProgress("Загрузка настроек...")
            if (fptr.put_DeviceSettings(getFptrSettings()) < 0) {
                checkError(fptr)
            }
            if (paycard.put_DeviceSettings(getPaycardSettings()) < 0) {
                checkError(paycard)
            }
            paycard.put_PinPadDevice(fptr.get_PinPadDevice())
            paycard.put_ModemDevice(fptr.get_ModemDevice())

            publishProgress("Установка соединения...")
            if (fptr.put_DeviceEnabled(true) < 0) {
                checkError(fptr)
            }
            if (paycard.put_DeviceEnabled(true) < 0) {
                checkError(paycard)
            }
            publishProgress("OK")

            publishProgress("Проверка связи с ККМ...")
            if (fptr.GetStatus() < 0) {
                checkError(fptr)
            }
            publishProgress("OK")

            publishProgress("Продажа...")
            if (paycard.put_AuthorizationType(IPaycard.AUTHORIZATION_READER_PIN) < 0) {
                checkError(paycard)
            }
            if (paycard.put_OperationType(IPaycard.OPERATION_SUB) < 0) {
                checkError(paycard)
            }
            if (paycard.put_Sum(101.02) < 0) {
                checkError(paycard)
            }
            if (paycard.put_CharLineLength(fptr.get_CharLineLength()) < 0) {
                checkError(paycard)
            }
            if (paycard.PrepareAuthorization() < 0) {
                checkError(paycard)
            }
            if (paycard.Authorization() < 0) {
                checkError(paycard)
            }
            publishProgress("OK")

            publishProgress("Печать слипа...")
            if (fptr.put_Caption(paycard.get_Text()) < 0) {
                checkError(fptr)
            }
            if (fptr.put_TextWrap(IFptr.WRAP_LINE) < 0) {
                checkError(fptr)
            }
            if (fptr.put_Alignment(IFptr.ALIGNMENT_LEFT) < 0) {
                checkError(fptr)
            }
            if (fptr.PrintString() < 0) {
                checkError(fptr)
            }
            if (fptr.put_Mode(IFptr.MODE_REPORT_NO_CLEAR) < 0) {
                checkError(fptr)
            }
            if (fptr.SetMode() < 0) {
                checkError(fptr)
            }
            if (fptr.PrintFooter() < 0) {
                checkError(fptr)
            }
            publishProgress("OK")

        } catch (e: Exception) {
            publishProgress(e.toString())
        } finally {
            fptr.destroy()
            paycard.destroy()
        }
        true
    }

    private fun publishProgress(msg: String) {
        Log.d("MainActivity", msg)
    }

    @Throws(DriverException::class)
    private fun checkError(i: IFptr) {
        val rc = i._ResultCode
        if (rc < 0) {
            val rd = i._ResultDescription
            var bpd: String? = null
            if (rc == -6) {
                bpd = i._BadParamDescription
            }
            if (bpd != null) {
                throw DriverException(String.format("[%d] %s (%s)", rc, rd, bpd))
            } else {
                throw DriverException(String.format("[%d] %s", rc, rd))
            }
        }
    }

    @Throws(DriverException::class)
    private fun checkError(i: IPaycard) {
        val rc = i._ResultCode
        if (rc < 0) {
            val rd = i._ResultDescription
            var bpd: String? = null
            if (rc == -6) {
                bpd = i._BadParamDescription
            }
            if (bpd != null) {
                throw DriverException(String.format("[%d] %s (%s)", rc, rd, bpd))
            } else {
                throw DriverException(String.format("[%d] %s", rc, rd))
            }
        }
    }

    private class DriverException(msg: String) : Exception(msg)
}
