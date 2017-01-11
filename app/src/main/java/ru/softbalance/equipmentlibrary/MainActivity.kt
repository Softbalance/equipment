package ru.softbalance.equipmentlibrary

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
import com.fasterxml.jackson.databind.ObjectMapper
import com.jakewharton.retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory
import ru.softbalance.equipment.model.Task
import ru.softbalance.equipment.model.atol.Atol
import ru.softbalance.equipment.model.mapping.jackson.JacksonConfigurator
import ru.softbalance.equipment.model.printserver.api.PrintServerApi
import ru.softbalance.equipment.view.DriverSetupActivity
import java.util.concurrent.TimeUnit


class MainActivity : AppCompatActivity() {

    private lateinit var api: PrintServerApi

    private lateinit var mapper: ObjectMapper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById(R.id.button).setOnClickListener {
            startActivity(Intent(this, DriverSetupActivity::class.java))
//            testPrintServer()
        }

        mapper = JacksonConfigurator.build()
        api = Retrofit.Builder()
                .baseUrl("http://178.170.230.140:9004")
                .client(getClient())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(JacksonConverterFactory.create(mapper))
                .build()
                .create(PrintServerApi::class.java)
    }

    private fun testPrintServer() {
//        val task = Task()
//        task.type = TaskType.CANCEL_CHECK
//        val strType = mapper.writeValueAsString(task)
//        toast(strType)
//        val taskDes = mapper.readValue(strType, Task::class.java)

        val tasks = listOf(Task(), Task())
        Atol(this, "")
                .execute(tasks)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ toast(it.resultInfo) }, { toast(it.toString()) })
    }

    private fun toast(text: String) {
        Toast.makeText(this, text, Toast.LENGTH_LONG).show()
    }

    private fun getClient(): OkHttpClient {


        val builder = OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)

        if (BuildConfig.DEBUG) {
            val interceptor = HttpLoggingInterceptor()
            interceptor.level = HttpLoggingInterceptor.Level.BODY
            builder.addInterceptor(interceptor)
        }

        return builder.build()
    }
}
