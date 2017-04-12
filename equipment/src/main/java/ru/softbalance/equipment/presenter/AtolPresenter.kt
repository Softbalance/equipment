package ru.softbalance.equipment.presenter


import android.content.Context
import ru.softbalance.equipment.R
import ru.softbalance.equipment.isActive
import ru.softbalance.equipment.model.Task
import ru.softbalance.equipment.model.TaskType
import ru.softbalance.equipment.model.atol.Atol
import ru.softbalance.equipment.view.fragment.AtolFragment
import rx.Subscription
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers

class AtolPresenter(context: Context, settings: String) : Presenter<AtolFragment>(context) {

    var printedSuccessful: Boolean = false

    var settings: String = ""
        private set

    init {
        this.settings = settings
    }

    private var printTest: Subscription? = null

    override fun bindView(view: AtolFragment) {
        super.bindView(view)

        if (printTest.isActive()) {
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
        printTest?.unsubscribe()
    }

    fun testPrint() {
        if (printTest.isActive()) {
            return
        }

        view()?.showLoading(context.getString(R.string.test_print) ?: "")

        val tasks = listOf(
                Task().apply { data = context.getString(R.string.text_print) },
                Task().apply { type = TaskType.PRINT_HEADER })

        var driver: Atol = Atol(context, settings)

        printTest = driver.execute(tasks, true)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnUnsubscribe {
                    view()?.hideLoading()
                }
                .subscribe({ response ->
                    printedSuccessful = response.isSuccess()
                    view()?.let {
                        if (printedSuccessful && settings.isNotEmpty()) {
                            it.showSettingsState(true)
                            it.showConfirm("OK")
                        } else {
                            it.showError(response.resultInfo)
                        }
                    }
                }, {
                    printedSuccessful = false
                    view()?.showError(it.toString())
                })
    }

    fun startConnection() {
        if (settings.isNullOrEmpty()) {
            settings = Atol(context, settings).getDefaultSettings()
        }
        view()?.launchConnectionActivity(settings)
    }

    fun updateSettings(settings: String) {
        this.settings = settings
    }
}
