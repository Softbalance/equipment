package ru.softbalance.equipment.view

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.MenuItem
import ru.softbalance.equipment.R
import ru.softbalance.equipment.view.fragment.AtolFragment

class DriverSetupActivity : AppCompatActivity(), AtolFragment.Callback {

    companion object {
        const val DRIVER_TYPE = "DRIVER_TYPE";
        const val DRIVER_TYPE_ATOL = 1;
        const val DRIVER_TYPE_SERVER = 2;

        const val SETTINGS_ARG = "SETTINGS_ARG";
    }

    var settings:String = "";

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_lib)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeButtonEnabled(true)

        val fm = supportFragmentManager
        if (savedInstanceState == null) {

            val fr = when(intent.getIntExtra(DRIVER_TYPE, DRIVER_TYPE_ATOL)){
                DRIVER_TYPE_ATOL -> AtolFragment.newInstance()
                DRIVER_TYPE_SERVER -> AtolFragment.newInstance()
                else -> AtolFragment.newInstance()
            }

            fm.beginTransaction()
                    .add(R.id.fragmentContainer, fr, AtolFragment::class.java.simpleName)
                    .commit()
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

    override fun onBackPressed() {
        setResult(if (settings.isNotEmpty()) Activity.RESULT_OK else Activity.RESULT_CANCELED,
                Intent().putExtra(SETTINGS_ARG, settings))
        finish()
    }
}
