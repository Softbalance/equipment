package ru.softbalance.equipment.view

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
import com.atol.drivers.fptr.settings.SettingsActivity
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import ru.softbalance.equipment.R
import ru.softbalance.equipment.model.Task
import ru.softbalance.equipment.model.TaskType
import ru.softbalance.equipment.model.atol.Atol

class DriverSetupActivity : AppCompatActivity() {

    companion object {
        const val REQUEST_CONNECT_DEVICE = 1
    }

    private lateinit var driver: Atol
    private var settings = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        driver = Atol(this, settings)

        setContentView(R.layout.activity_lib)
        findViewById(R.id.connect).setOnClickListener { startConnection() }
        findViewById(R.id.testPrint).setOnClickListener { testPrint() }
    }

    private fun testPrint() {
        driver.finish()
        driver = Atol(this, settings)

        val tasks = listOf(Task().apply { data = "Тестовая печать" },
                Task().apply { type = TaskType.PRINT_FOOTER })

        driver.execute(tasks)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ toast(it.resultInfo) }, { toast(it.toString()) })
    }

    private fun toast(text: String) {
        Toast.makeText(this, text, Toast.LENGTH_LONG).show()
    }

    private fun startConnection() {
        val intent = Intent(this, SettingsActivity::class.java)
        if (settings.isEmpty()) {
            settings = driver.getDefaultSettings()
        }
        intent.putExtra(SettingsActivity.DEVICE_SETTINGS, settings)
        startActivityForResult(intent, REQUEST_CONNECT_DEVICE)
    }

    fun extractSettings(data: Bundle?): String? {
        if (data != null && data.containsKey(SettingsActivity.DEVICE_SETTINGS)) {
            return data.getString(SettingsActivity.DEVICE_SETTINGS)
        } else {
            return null
        }
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        if (requestCode == REQUEST_CONNECT_DEVICE && data.extras != null) {
            settings = extractSettings(data.extras) ?: ""
        }
    }
}
