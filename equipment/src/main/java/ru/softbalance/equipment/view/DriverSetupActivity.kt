package ru.softbalance.equipment.view

import android.app.Activity
import android.content.Intent
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.MenuItem
import ru.softbalance.equipment.R
import ru.softbalance.equipment.databinding.ActivityLibBinding
import ru.softbalance.equipment.view.fragment.AtolFragment
import ru.softbalance.equipment.view.fragment.BaseFragment
import ru.softbalance.equipment.view.fragment.PrintServerFragment

class DriverSetupActivity : AppCompatActivity(), AtolFragment.Callback, PrintServerFragment.Callback {

    companion object {
        const val DRIVER_ARG = "DRIVER_ARG"
        const val URL_ARG = "URL_ARG"
        const val PORT_ARG = "PORT_ARG"
        const val EQUIPMENT_TYPE_ARG = "EQUIPMENT_TYPE_ARG"

        const val DRIVER_TYPE_ATOL = 1
        const val DRIVER_TYPE_SERVER = 2

        const val SETTINGS_ARG = "SETTINGS_ARG"
    }

    var settings: String = ""
    var url: String = ""
    var port: Int = 0
    var type: Int = 0

    private lateinit var binding: ActivityLibBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        restoreSettings(savedInstanceState)

        binding = DataBindingUtil.setContentView<ActivityLibBinding>(this, R.layout.activity_lib)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeButtonEnabled(true)
        }

        if (savedInstanceState == null) {
            val fr = when (intent.getIntExtra(DRIVER_ARG, DRIVER_TYPE_ATOL)) {
                DRIVER_TYPE_ATOL -> AtolFragment.newInstance(settings)
                DRIVER_TYPE_SERVER -> PrintServerFragment.newInstance(url, port, type, settings)
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
        }
    }

    override fun onStart() {
        super.onStart()
        supportFragmentManager.findFragmentById(binding.fragmentContainer.id)?.let {
            supportActionBar?.title = (it as BaseFragment).getTitle()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        val itemId = item?.itemId ?: 0
        when (itemId) {
            android.R.id.home -> onBackPressed()
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onSettingsSelected(settings: String) {
        this.settings = settings
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
            putExtra(URL_ARG, url)
            putExtra(PORT_ARG, port)
            putExtra(EQUIPMENT_TYPE_ARG, type)
        }

        setResult(resultCode, putExtra)
        super.finish()
    }
}