package ru.softbalance.equipment.view.fragment

import android.content.Context
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.Bundle
import android.support.constraint.Group
import android.support.design.widget.BottomNavigationView
import android.support.v4.content.ContextCompat
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import com.jakewharton.rxbinding.support.design.widget.RxBottomNavigationView
import com.jakewharton.rxbinding.widget.RxTextView
import ru.softbalance.equipment.R
import ru.softbalance.equipment.model.DeviceConnectionType
import ru.softbalance.equipment.presenter.PresentersCache
import ru.softbalance.equipment.presenter.ShtrihPresenter
import ru.softbalance.equipment.singleClick
import ru.softbalance.equipment.view.DriverSetupActivity
import ru.softbalance.equipment.view.ViewUtils
import ru.softbalance.equipment.visible
import rx.android.schedulers.AndroidSchedulers
import rx.subscriptions.CompositeSubscription
import java.util.concurrent.TimeUnit

class ShtrihFragment : BaseFragment() {

    interface Callback {
        fun onSettingsSelected(settings: String)
    }

    companion object {

        const val PRESENTER_NAME = "SHTRIH_PRESENTER"

        fun newInstance(settings: String): ShtrihFragment {
            val args = Bundle().apply { putString(DriverSetupActivity.SETTINGS_ARG, settings) }
            return ShtrihFragment().apply { arguments = args }
        }
    }

    private lateinit var presenter: ShtrihPresenter

    private val autoDisposable = CompositeSubscription()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        presenter = (PresentersCache.get(PRESENTER_NAME) as? ShtrihPresenter)
                ?: PresentersCache.add(PRESENTER_NAME,
            ShtrihPresenter(activity!!,
                arguments?.getString(DriverSetupActivity.SETTINGS_ARG) ?: ""))
    }

    override val title: String
        get() = getString(R.string.equipment_lib_title_shtrih)

    private lateinit var usbTypeMenuItem: MenuItem
    private lateinit var hostInput: EditText
    private lateinit var portInput: EditText
    private lateinit var connectionTypeNavigationView: BottomNavigationView
    private lateinit var testPrintButton: Button
    private lateinit var usbDeviceName: TextView
    private lateinit var selectUsbDeviceButton: View
    private lateinit var networkGroup: Group
    private lateinit var usbGroup: Group

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_shtrih, container, false)

        hostInput = view.findViewById(R.id.host)
        portInput = view.findViewById(R.id.port)
        connectionTypeNavigationView = view.findViewById(R.id.connectionType)
        usbTypeMenuItem = connectionTypeNavigationView.menu.findItem(R.id.type_usb)
        testPrintButton = view.findViewById(R.id.testPrint)
        usbDeviceName = view.findViewById(R.id.usbDeviceName)
        selectUsbDeviceButton = view.findViewById(R.id.selectUsbDevice)
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

        return view
    }

    override fun onStart() {
        super.onStart()
        presenter.bindView(this)
    }

    private fun selectUsbDevice(view: View) {
        val usbManager = context?.getSystemService(Context.USB_SERVICE) as UsbManager
        val usbDevices = usbManager.deviceList.values

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

        networkGroup.visible = isNetwork
        usbGroup.visible = !isNetwork
    }

    fun showTestButton(isEnabled: Boolean) {
        testPrintButton.isEnabled = isEnabled
    }

    fun showUsbDeviceName(deviceName: String) {
        usbDeviceName.text = deviceName.takeIf { it.isNotEmpty() } ?:
                getString(R.string.equipment_device_not_selected)
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
}