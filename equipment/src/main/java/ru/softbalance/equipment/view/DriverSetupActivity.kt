package ru.softbalance.equipment.view

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.MenuItem
import ru.softbalance.equipment.R
import ru.softbalance.equipment.view.fragment.AtolFragment
import ru.softbalance.equipment.view.fragment.BaseFragment
import ru.softbalance.equipment.view.fragment.PosiflexFragment
import ru.softbalance.equipment.view.fragment.PrintServerFragment

class DriverSetupActivity : AppCompatActivity(), AtolFragment.Callback, PrintServerFragment.Callback {

    companion object {
        const val DRIVER_ARG = "DRIVER_ARG"
        const val URL_ARG = "URL_ARG"
        const val PORT_ARG = "PORT_ARG"
        const val EQUIPMENT_TYPE_ARG = "EQUIPMENT_TYPE_ARG"

        const val DRIVER_TYPE_ATOL = 1
        const val DRIVER_TYPE_SERVER = 2
        const val DRIVER_TYPE_POSIFLEX = 3

        const val SETTINGS_ARG = "SETTINGS_ARG"
        const val SERIAL_ARG = "SERIAL_ARG"
    }

    var settings: String = ""
    var serial: String = ""
    var url: String = ""
    var port: Int = 0
    var type: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        restoreSettings(savedInstanceState)

        setContentView(R.layout.activity_lib)

        setSupportActionBar(findViewById(R.id.toolbar))

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeButtonEnabled(true)
        }

        if (savedInstanceState == null) {
            val fr = when (intent.getIntExtra(DRIVER_ARG, DRIVER_TYPE_ATOL)) {
                DRIVER_TYPE_ATOL -> AtolFragment.newInstance(settings)
                DRIVER_TYPE_SERVER -> PrintServerFragment.newInstance(url, port, type, settings)
                DRIVER_TYPE_POSIFLEX -> PosiflexFragment.newInstance()
                else -> AtolFragment.newInstance()
            }

            supportFragmentManager.beginTransaction()
                    .add(R.id.fragmentContainer, fr, fr.javaClass.simpleName)
                    .commit()
        }
    }

    private fun restoreSettings(savedInstanceState: Bundle?) {
        with(savedInstanceState ?: intent.extras) {
            url = getString(URL_ARG, "")
            port = getInt(PORT_ARG, 0)
            type = getInt(EQUIPMENT_TYPE_ARG, 0)
            settings = getString(SETTINGS_ARG, "")
            serial = getString(SERIAL_ARG, "")
        }
    }

    override fun onStart() {
        super.onStart()
        supportFragmentManager.findFragmentById(R.id.fragmentContainer)?.let {
            supportActionBar?.title = (it as BaseFragment).title
        }
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        val itemId = item?.itemId ?: 0
        when (itemId) {
            android.R.id.home -> onBackPressed()
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onSettingsSelected(settings: String, serial: String) {
        this.settings = settings
        this.serial = serial
    }

    override fun onSettingsSelected(settings: String,
                                    url: String,
                                    port: Int,
                                    type: Int) {
        this.settings = settings
        this.url = url
        this.port = port
        this.type = type
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        outState?.apply {
            putString(SETTINGS_ARG, settings)
            putString(URL_ARG, url)
            putInt(PORT_ARG, port)
            putInt(EQUIPMENT_TYPE_ARG, type)
        }
    }

    override fun finish() {
        val resultCode = if (settings.isNotEmpty()) Activity.RESULT_OK else Activity.RESULT_CANCELED
        val putExtra = Intent().apply {
            putExtra(SETTINGS_ARG, settings)
            putExtra(SERIAL_ARG, serial)
            putExtra(URL_ARG, url)
            putExtra(PORT_ARG, port)
            putExtra(EQUIPMENT_TYPE_ARG, type)
        }

        setResult(resultCode, putExtra)
        super.finish()
    }
}