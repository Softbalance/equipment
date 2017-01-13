package ru.softbalance.equipment.presenter


import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import ru.softbalance.equipment.R
import ru.softbalance.equipment.model.Task
import ru.softbalance.equipment.model.TaskType
import ru.softbalance.equipment.model.atol.Atol
import ru.softbalance.equipment.view.fragment.AtolFragment

class AtolPresenter : Presenter<AtolFragment> () {

    private var driver: Atol? = null;
    private var isPrinting : Boolean = false
    var printedSuccessful : Boolean = false

    var settings: String = ""

    private var printTest : Disposable? = null

    override fun bindView(view: AtolFragment){
        super.bindView(view)

        driver = Atol(view()!!.activity, settings)

        if (isPrinting) {
            view()?.showLoading(context?.getString(R.string.test_print) ?: "")
        } else {
            view()?.hideLoading()
        }

        view()?.showSettingsState(printedSuccessful && settings.isNotEmpty())
    }

    override fun unbindView(view: AtolFragment) {
        view()?.hideLoading()

        super.unbindView(view)
    }

    override fun onFinish() {
        printTest?.dispose()
    }

    fun testPrint() {
        if (!isPrinting && context != null && driver != null) {

            view()?.showLoading(context?.getString(R.string.test_print) ?: "")

            isPrinting = true

            driver?.finish()
            driver = Atol(context!!, settings)

            val tasks = listOf(
                    Task(data = context!!.getString(R.string.text_print)),
                    Task(type = TaskType.PRINT_HEADER))

            printTest = driver!!.execute(tasks)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnDispose {
                        isPrinting = false
                        view()?.hideLoading()
                    }
                    .subscribe({
                        printedSuccessful = it.isSuccess()
                        view()?.showSettingsState(printedSuccessful && settings.isNotEmpty())
                        view()?.showConfirm(it.resultInfo)
                    }, {
                        printedSuccessful = false
                        view()?.showError(it.toString())
                    })
        }
    }

    fun startConnection() {
        if(settings.isNullOrEmpty()) {
            settings = driver?.getDefaultSettings() ?: ""
        }
        view()?.launchConnectionActivity(settings)
    }

}
