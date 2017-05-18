package ru.softbalance.equipmentlibrary

import android.content.Intent
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
import ru.softbalance.equipment.view.DriverSetupActivity
import ru.softbalance.equipment.view.DriverSetupActivity.Companion.DRIVER_ARG
import ru.softbalance.equipment.view.DriverSetupActivity.Companion.DRIVER_TYPE_ATOL
import ru.softbalance.equipment.view.DriverSetupActivity.Companion.DRIVER_TYPE_SERVER
import ru.softbalance.equipment.view.DriverSetupActivity.Companion.PORT_ARG
import ru.softbalance.equipment.view.DriverSetupActivity.Companion.SERIAL_ARG
import ru.softbalance.equipment.view.DriverSetupActivity.Companion.SETTINGS_ARG
import ru.softbalance.equipment.view.DriverSetupActivity.Companion.URL_ARG
import ru.softbalance.equipmentlibrary.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    companion object {
        const val DRIVER_REQUEST = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        findViewById(R.id.buttonAtol).setOnClickListener {
            startActivityForResult(Intent(this, DriverSetupActivity::class.java)
                    .putExtra(DRIVER_ARG, DRIVER_TYPE_ATOL), DRIVER_REQUEST)
        }

        findViewById(R.id.buttonPrinter).setOnClickListener {
            startActivityForResult(Intent(this, DriverSetupActivity::class.java)
                    .putExtra(DRIVER_ARG, DRIVER_TYPE_SERVER)
                    .putExtra(URL_ARG, "178.170.230.140")
                    .putExtra(PORT_ARG, 9004),
                    DRIVER_REQUEST)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == DRIVER_REQUEST){
            val settings = if (data?.getStringExtra(SETTINGS_ARG).isNullOrEmpty())
                "no settings data found" else data?.getStringExtra(SETTINGS_ARG)
            val serial = if (data?.getStringExtra(SERIAL_ARG).isNullOrEmpty())
                "no serial data found" else data?.getStringExtra(SERIAL_ARG)

            Toast.makeText(this, "settings: " + settings, Toast.LENGTH_LONG).show()
            Toast.makeText(this, "serial: " + serial, Toast.LENGTH_LONG).show()
        }
    }
}
