package ru.softbalance.equipment.presenter


import android.content.Context
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import ru.softbalance.equipment.R
import ru.softbalance.equipment.model.Task
import ru.softbalance.equipment.model.TaskType
import ru.softbalance.equipment.model.atol.Atol
import ru.softbalance.equipment.view.fragment.AtolFragment

class AtolPresenter(context: Context) : Presenter<AtolFragment>(context) {

    private var driver: Atol
    private var isPrinting: Boolean = false
    var printedSuccessful: Boolean = false

    var settings: String = ""
        private set

    init {
        driver = Atol(context, settings)
    }

    private fun initDriver() {
        driver.finish()
        driver = Atol(context, settings)
    }

    private var printTest: Disposable? = null

    override fun bindView(view: AtolFragment) {
        super.bindView(view)

        if (isPrinting) {
            view.showLoading(context.getString(R.string.test_print))
        } else {
            view.hideLoading()
        }

        view.showSettingsState(printedSuccessful && settings.isNotEmpty())
    }

    override fun unbindView(view: AtolFragment) {
        view()?.hideLoading()

        super.unbindView(view)
    }

    override fun onFinish() {
        printTest?.dispose()
    }

    fun testPrint() {
        if (!isPrinting) {

            view()?.showLoading(context.getString(R.string.test_print) ?: "")

            isPrinting = true

            val tasks = listOf(
                    Task(data = context.getString(R.string.text_print)),
                    Task(type = TaskType.PRINT_HEADER))

            printTest = driver.execute(tasks)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnDispose {
                        isPrinting = false
                        view()?.hideLoading()
                    }
                    .subscribe({ response ->
                        printedSuccessful = response.isSuccess()
                        view()?.let {
                            it.showSettingsState(printedSuccessful && settings.isNotEmpty())
                            it.showConfirm(response.resultInfo)
                        }
                    }, {
                        printedSuccessful = false
                        view()?.showError(it.toString())
                    })
        }
    }

    fun startConnection() {
        if (settings.isNullOrEmpty()) {
            settings = driver.getDefaultSettings()
        }
        view()?.launchConnectionActivity(settings)
    }

    fun updateSettings(settings: String) {
        this.settings = settings
        initDriver()
    }
}
