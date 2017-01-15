package ru.softbalance.equipment.view.fragment

import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import com.jakewharton.rxbinding.widget.RxTextView
import okhttp3.HttpUrl
import ru.softbalance.equipment.R
import ru.softbalance.equipment.model.printserver.api.model.PrintDeviceDriver
import ru.softbalance.equipment.model.printserver.api.model.PrintDeviceModel
import ru.softbalance.equipment.model.printserver.api.model.PrintDeviceType
import ru.softbalance.equipment.presenter.PresentersCache
import ru.softbalance.equipment.presenter.PrintServerPresenter
import ru.softbalance.equipment.view.DriverSetupActivity.Companion.PORT_ARG
import ru.softbalance.equipment.view.DriverSetupActivity.Companion.URL_ARG
import ru.softbalance.equipment.view.ViewUtils
import rx.Observable

class PrintServerFragment : BaseFragment() {

    interface Callback {
        fun onSettingsSelected(settings: String)
    }

    companion object {

        const val PRESENTER_NAME = "PRINT_SERVER_PRESENTER"

        fun newInstance(url: String, port: Int): PrintServerFragment {
            val args = Bundle().apply {
                putString(URL_ARG, url)
                putInt(PORT_ARG, port)
            }
            return PrintServerFragment().apply { arguments = args }
        }
    }

    private var connect: Button? = null
    private var print: Button? = null
    private var deviceTypes: TextView? = null
    private var deviceModels: TextView? = null
    private var deviceDrivers: TextView? = null

    private lateinit var url: EditText
    private lateinit var port: EditText

    private lateinit var presenter: PrintServerPresenter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val pr = PresentersCache.get(PRESENTER_NAME)
        presenter = if (pr != null) pr as PrintServerPresenter else
            PresentersCache.add(PRESENTER_NAME,
                    PrintServerPresenter(
                            arguments.getString(URL_ARG), arguments.getInt(PORT_ARG))) as PrintServerPresenter
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): android.view.View? {

        super.onCreateView(inflater, container, savedInstanceState)

        val rootView = inflater?.inflate(R.layout.fragment_print_server, container, false)

        url = rootView?.findViewById(R.id.ip_address) as EditText
        port = rootView?.findViewById(R.id.port) as EditText
        deviceTypes = rootView?.findViewById(R.id.device_type) as TextView
        deviceModels = rootView?.findViewById(R.id.model) as TextView
        deviceDrivers = rootView?.findViewById(R.id.driver) as TextView

        if (savedInstanceState == null) {
            port.setText(arguments.getInt(PORT_ARG).toString())
            url.setText(arguments.getString(URL_ARG))
        }

        connect = rootView?.findViewById(R.id.connect) as Button
        print = rootView?.findViewById(R.id.testPrint) as Button

        connect?.setOnClickListener { presenter.getDevices(url.text.toString(), port.text.toString().toInt()) }
        deviceTypes?.setOnClickListener { selectDevice() }
        deviceModels?.setOnClickListener { selectModel() }
        deviceDrivers?.setOnClickListener { selectDriver() }
        print?.setOnClickListener { presenter.testPrint() }

        Observable.combineLatest( // TODO move to javarx2
                RxTextView.textChanges(url),
                RxTextView.textChanges(port)
        ) { urlValue, portValue -> urlValue.isNotEmpty() && portValue.isNotEmpty() &&
                HttpUrl.parse(presenter.getPrintServerUrl(urlValue.toString(), portValue.toString().toInt())) != null }
                .subscribe { enabled ->
                    connect?.setEnabled(enabled)
                }

        presenter.bindView(this)

        return rootView
    }

    private fun selectDevice() {
        val types = presenter.deviceTypes;

        if (types == null) {
            showError(getString(R.string.no_data))
        } else {
            val popupMenu = ViewUtils.createPopupMenu(activity, deviceTypes, 0, false)
            val menu = popupMenu.getMenu()
            for (i in types.indices) {
                menu.add(0, i, i, types.get(i).name)
            }

            popupMenu.setOnMenuItemClickListener({ item ->
                val deviceType = types.get(item.itemId)
                presenter.selectDeviceType(deviceType)
                true
            })

            popupMenu.show()
        }
    }

    private fun selectModel() {
        val models = presenter.models;

        if (models == null) {
            showError(getString(R.string.no_data))
        } else {
            val popupMenu = ViewUtils.createPopupMenu(activity, deviceModels, 0, false)
            val menu = popupMenu.getMenu()
            for (i in models.indices) {
                menu.add(0, i, i, models.get(i).name)
            }

            popupMenu.setOnMenuItemClickListener({ item ->
                val model = models.get(item.itemId)
                presenter.selectModel(model)
                true
            })

            popupMenu.show()
        }
    }

    private fun selectDriver() {
        val drivers = presenter.drivers;

        if (drivers == null) {
            showError(getString(R.string.no_data))
        } else {
            val popupMenu = ViewUtils.createPopupMenu(activity, deviceDrivers, 0, false)
            val menu = popupMenu.getMenu()
            for (i in drivers.indices) {
                menu.add(0, i, i, drivers.get(i).name)
            }

            popupMenu.setOnMenuItemClickListener({ item ->
                val driver = drivers.get(item.itemId)
                presenter.selectDriver(driver)
                true
            })

            popupMenu.show()
        }
    }

    fun showConnectionState(ok: Boolean){
        connect?.setCompoundDrawablesWithIntrinsicBounds(null, null,
                if (ok) ContextCompat.getDrawable(getActivity(), R.drawable.ic_confirm_selector) else null,
                null)
    }

    fun showPrintState(ok: Boolean){
        print?.setCompoundDrawablesWithIntrinsicBounds(null, null,
                if (ok) ContextCompat.getDrawable(getActivity(), R.drawable.ic_confirm_selector) else null,
                null)
    }

    fun showPrintAvailable(ok: Boolean) = print?.setEnabled(ok)

    fun showType(type : PrintDeviceType) = deviceTypes?.setText(type.name)

    fun showModel(model : PrintDeviceModel) = deviceModels?.setText(model.name)

    fun showDriver(driver : PrintDeviceDriver) = deviceDrivers?.setText(driver.name)

    override fun onFinish() {
        PresentersCache.remove(PRESENTER_NAME)

        super.onFinish()
    }

    override fun onDestroyView() {
        presenter.unbindView(this)
        super.onDestroyView()
    }
}