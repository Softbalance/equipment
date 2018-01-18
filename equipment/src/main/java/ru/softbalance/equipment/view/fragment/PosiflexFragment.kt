package ru.softbalance.equipment.view.fragment

import android.content.Context
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.Bundle
import android.support.constraint.Group
import android.support.design.widget.BottomNavigationView
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import com.jakewharton.rxbinding.support.design.widget.RxBottomNavigationView
import com.jakewharton.rxbinding.widget.RxTextView
import kotlinx.android.synthetic.main.fragment_posiflex.*
import ru.softbalance.equipment.R
import ru.softbalance.equipment.model.DeviceConnectionType
import ru.softbalance.equipment.model.posiflex.Posiflex
import ru.softbalance.equipment.presenter.PosiflexPresenter
import ru.softbalance.equipment.presenter.PresentersCache
import ru.softbalance.equipment.singleClick
import ru.softbalance.equipment.view.DriverSetupActivity
import ru.softbalance.equipment.view.ViewUtils
import ru.softbalance.equipment.visible
import rx.android.schedulers.AndroidSchedulers
import rx.subscriptions.CompositeSubscription
import java.util.concurrent.TimeUnit

fun String.toIntOrNull(): Int? = try {
    this.toInt()
} catch (e: NumberFormatException) {
    null
}

inline fun <T> T.takeIf(predicate: (T) -> Boolean): T? {
    return if (predicate(this)) this else null
}

inline fun <T> T.also(block: (T) -> Unit): T {
    block(this)
    return this
}

class PosiflexFragment : BaseFragment() {

    interface Callback {
        fun onSettingsSelected(settings: String)
    }

    companion object {

        const val PRESENTER_NAME = "POSIFLEX_PRESENTER"

        fun newInstance(settings: String): PosiflexFragment {
            val args = Bundle().apply { putString(DriverSetupActivity.SETTINGS_ARG, settings) }
            return PosiflexFragment().apply { arguments = args }
        }
    }

    private lateinit var presenter: PosiflexPresenter

    private val autoDisposable = CompositeSubscription()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val pr = PresentersCache.get(PRESENTER_NAME)
        presenter = if (pr != null) pr as PosiflexPresenter else
            PresentersCache.add(PRESENTER_NAME, PosiflexPresenter(activity!!, arguments?.getString(DriverSetupActivity.SETTINGS_ARG) ?: ""))
    }

    override val title: String
        get() = getString(R.string.equipment_lib_title_posiflex)

    private lateinit var usbTypeMenuItem: MenuItem
    private lateinit var hostInput: EditText
    private lateinit var portInput: EditText
    private lateinit var codePageInput: EditText
    private lateinit var connectionTypeNavigationView: BottomNavigationView
    private lateinit var testPrintButton: Button
    private lateinit var usbDeviceName: TextView
    private lateinit var selectUsbDeviceButton: View
    private lateinit var codePageInfo: View
    private lateinit var networkGroup: Group
    private lateinit var usbGroup: Group

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_posiflex, container, false)

        hostInput = view.findViewById(R.id.host)
        portInput = view.findViewById(R.id.port)
        codePageInput = view.findViewById(R.id.codePage)
        connectionTypeNavigationView = view.findViewById(R.id.connectionType)
        usbTypeMenuItem = connectionTypeNavigationView.menu.findItem(R.id.type_usb)
        testPrintButton = view.findViewById(R.id.testPrint)
        usbDeviceName = view.findViewById(R.id.usbDeviceName)
        selectUsbDeviceButton = view.findViewById(R.id.selectUsbDevice)
        codePageInfo = view.findViewById(R.id.codePageInfo)
        networkGroup = view.findViewById(R.id.groupNetwork)
        usbGroup = view.findViewById(R.id.groupUsb)

        RxTextView.textChanges(hostInput).skip(1)
                .debounce(300, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { presenter.onHostInput(it.toString()) }

        RxTextView.textChanges(portInput).skip(1)
                .debounce(300, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { presenter.onPortInput(it.toString().toIntOrNull() ?: 0) }
                .also { autoDisposable.add(it) }


        RxTextView.textChanges(codePageInput).skip(1)
                .debounce(300, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { presenter.onCodePageInput(it.toString().toIntOrNull() ?: 0) }
                .also { autoDisposable.add(it) }

        RxBottomNavigationView.itemSelections(connectionTypeNavigationView).skip(1)
                .map {
                    if (it == usbTypeMenuItem) {
                        DeviceConnectionType.USB
                    } else {
                        DeviceConnectionType.NETWORK
                    }
                }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { presenter.onChangeConnectionType(it) }
                .also { autoDisposable.add(it) }

        testPrintButton.singleClick { presenter.onTestPrint() }
        selectUsbDeviceButton.singleClick { selectUsbDevice(it!!) }
        codePageInfo.singleClick { showCodePageInfoDialog() }

        return view
    }

    override fun onStart() {
        super.onStart()
        presenter.bindView(this)
    }

    private fun showCodePageInfoDialog() {
        AlertDialog.Builder(activity!!)
                .setTitle(R.string.title_printer_code_page)
                .setMessage(getString(R.string.info_printer_code_page, getString(R.string.app_name)))
                .setPositiveButton(android.R.string.ok, null)
                .show()
    }

    private fun selectUsbDevice(view: View) {
        val usbManager = context?.getSystemService(Context.USB_SERVICE) as UsbManager
        val usbDevices = usbManager.deviceList.values
                .filter { it.vendorId in Posiflex.VENDORS }

        val popup = ViewUtils.createPopupMenu(activity, view, 0, false)
        val menu = popup.menu

        usbDevices.forEach { device ->
            val name = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                device.productName
            } else {
                device.deviceName
            }
            menu.add(0, device.productId, device.productId, name)
        }

        popup.setOnMenuItemClickListener { item ->
            presenter.onSelectUsbDevice(item.itemId, item.title.toString())
            true
        }

        if (menu.size() != 0) {
            popup.show()
        } else {
            showInfo(getString(R.string.equipment_hint_no_usb_devices_connected))
        }
    }

    override fun onFinish() {
        PresentersCache.remove(PRESENTER_NAME)
        super.onFinish()
    }

    override fun onStop() {
        presenter.unbindView(this)
        super.onStop()
    }

    fun showConnectionType(type: DeviceConnectionType) {
        val isNetwork = type != DeviceConnectionType.USB

        groupNetwork.visible = isNetwork
        groupUsb.visible = !isNetwork
    }

    fun showTestButton(isEnabled: Boolean) {
        testPrintButton.isEnabled = isEnabled
    }

    fun showUsbDeviceName(deviceName: String) {
        usbDeviceName.text = deviceName.takeIf { it.isNotEmpty() } ?: getString(R.string.equipment_device_not_selected)
    }

    fun showAcceptationButton(isOK: Boolean, settings: String) {
        testPrintButton.setCompoundDrawablesWithIntrinsicBounds(
                if (isOK) ContextCompat.getDrawable(activity!!, R.drawable.ic_confirm_selector) else null,
                null,
                null,
                null)

        if (isOK) {
            (hostParent as Callback).onSettingsSelected(settings)
        }
    }

    fun showHost(host: String) {
        hostInput.setText(host)
    }

    fun showPort(port: Int) {
        portInput.setText(port.toString())
    }

    fun showCodePage(codePage: Int) {
        codePageInput.setText(codePage.toString())
    }
}