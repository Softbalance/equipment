package ru.softbalance.equipmentlibrary

import android.content.Intent
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import ru.softbalance.equipment.view.DriverSetupActivity
import ru.softbalance.equipmentlibrary.databinding.ActivityMainBinding


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding = DataBindingUtil.setContentView<ActivityMainBinding>(this, R.layout.activity_main)

        binding.button.setOnClickListener {
            startActivity(Intent(this, DriverSetupActivity::class.java))
        }
    }
}
