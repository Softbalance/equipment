package ru.softbalance.equipmentlibrary

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import ru.softbalance.equipment.view.SomeLibActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById(R.id.button).setOnClickListener {
            startActivity(Intent(this, SomeLibActivity::class.java))
        }
    }
}
