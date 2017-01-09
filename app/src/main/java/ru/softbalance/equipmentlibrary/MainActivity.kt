package ru.softbalance.equipmentlibrary

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById(R.id.button).setOnClickListener {
            startActivity(Intent(this, ru.softbalance.equipment.SomeLibActivity::class.java)) }
    }
}
