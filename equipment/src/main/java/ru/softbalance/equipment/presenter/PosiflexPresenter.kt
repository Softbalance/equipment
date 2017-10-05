package ru.softbalance.equipment.presenter

import android.content.Context
import ru.softbalance.equipment.R
import ru.softbalance.equipment.isActive
import ru.softbalance.equipment.model.DeviceConnectionType
import ru.softbalance.equipment.model.Task
import ru.softbalance.equipment.model.posiflex.Posiflex
import ru.softbalance.equipment.view.fragment.PosiflexFragment
import rx.android.schedulers.AndroidSchedulers
import rx.subscriptions.Subscriptions

class PosiflexPresenter(context: Context, settings: String) : Presenter<PosiflexFragment>(context) {

    private val settings = Posiflex.extractSettings(settings)

    override fun bindView(view: PosiflexFragment) {
        super.bindView(view)
        showSettings()
        updateTestButton()
        updateAcceptationButton()
        if (testPrintSubscription.isActive()) {
            view.showLoading(getString(R.string.test_print))
        }
    }

    private fun showSettings() {
        view?.let {
            it.showHost(settings.host)
            it.showPort(settings.port)
            it.showCodePage(settings.codePage)
        }
        showConnectionType()
        showUsbDeviceName()
    }

    private fun showUsbDeviceName() {
        view?.showUsbDeviceName(settings.deviceName)
    }

    fun onHostInput(host: String) {
        if (settings.host != host) {
            settings.host = host
            updateTestButton()
            resetConfirmation()
        }
    }

    private fun resetConfirmation() {
        areSettingsValid = false
        updateAcceptationButton()
    }

    fun onPortInput(port: Int) {
        if (settings.port != port) {
            settings.port = port
            resetConfirmation()
        }
    }

    private var testPrintSubscription = Subscriptions.unsubscribed()

    private var areSettingsValid: Boolean = false

    fun onTestPrint() {
        val driver = Posiflex.init(context, settings)
        if (driver == null) {
            view?.showError(context.getString(R.string.equipment_init_failure))
            return
        }
        val tasks = listOf(Task().apply { data = context.getString(R.string.text_print) })
        testPrintSubscription = driver.execute(tasks, true)
                .doOnSubscribe { view?.showLoading(getString(R.string.test_print)) }
                .doOnUnsubscribe { view?.hideLoading() }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ response ->
                    areSettingsValid = response.isSuccess()
                    updateAcceptationButton()
                    if (!areSettingsValid) {
                        view?.showError(response.resultInfo)
                    }
                }, {
                    view?.showError(it.toString())
                })
    }

    private fun updateAcceptationButton() {
        view?.showAcceptationButton(areSettingsValid, Posiflex.packSettings(settings))
    }

    fun onChangeConnectionType(type: DeviceConnectionType) {
        if (settings.connectionType != type) {
            settings.connectionType = type
            showConnectionType()
            updateTestButton()
            resetConfirmation()
        }
    }

    private fun showConnectionType() {
        view?.showConnectionType(settings.connectionType)
    }

    fun onSelectUsbDevice(productId: Int, name: String) {
        if (settings.productId != productId) {
            settings.productId = productId
            settings.deviceName = name
            updateTestButton()
            showUsbDeviceName()
            resetConfirmation()
        }
    }

    private fun updateTestButton() {
        val isTestPrintAvailable = (settings.connectionType == DeviceConnectionType.USB && settings.productId != 0)
                || (settings.connectionType == DeviceConnectionType.NETWORK && settings.host.isNotEmpty())
        view?.showTestButton(isTestPrintAvailable)
    }

    override fun onFinish() {
        testPrintSubscription.unsubscribe()
        super.onFinish()
    }

    private fun getString(resId: Int) = context.getString(resId)

    fun onCodePageInput(codePage: Int) {
        settings.codePage = codePage
    }
}