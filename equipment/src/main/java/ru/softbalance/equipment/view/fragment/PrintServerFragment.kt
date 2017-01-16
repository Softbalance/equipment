package ru.softbalance.equipment.view.fragment

import android.os.Bundle
import android.support.design.widget.TextInputLayout
import android.support.v4.content.ContextCompat
import android.text.InputFilter
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.jakewharton.rxbinding.widget.RxTextView
import okhttp3.HttpUrl
import ru.softbalance.equipment.R
import ru.softbalance.equipment.model.printserver.api.model.PrintDeviceDriver
import ru.softbalance.equipment.model.printserver.api.model.PrintDeviceModel
import ru.softbalance.equipment.model.printserver.api.model.PrintDeviceType
import ru.softbalance.equipment.model.printserver.api.model.SettingsValues
import ru.softbalance.equipment.model.printserver.api.response.settings.*
import ru.softbalance.equipment.presenter.PresentersCache
import ru.softbalance.equipment.presenter.PrintServerPresenter
import ru.softbalance.equipment.view.DriverSetupActivity.Companion.PORT_ARG
import ru.softbalance.equipment.view.DriverSetupActivity.Companion.URL_ARG
import ru.softbalance.equipment.view.ViewUtils
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import java.util.concurrent.TimeUnit

class PrintServerFragment : BaseFragment() {

    interface Callback {
        fun onSettingsSelected(settings: String)
    }

    companion object {

        private val TAG_SETTINGS_MODEL = R.id.settings_id

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
    private var saveSettings: Button? = null
    private var print: Button? = null
    private var deviceTypes: TextView? = null
    private var deviceModels: TextView? = null
    private var deviceDrivers: TextView? = null

    private lateinit var url: EditText
    private lateinit var port: EditText

    private lateinit var settings: LinearLayout

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
        saveSettings = rootView?.findViewById(R.id.save_settings) as Button
        settings = rootView?.findViewById(R.id.settings_layout) as LinearLayout

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
        saveSettings?.setOnClickListener { presenter.saveSettings(SettingsValues()) } // TODO get from UI
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

    fun buildSettingsUI(settingsData: SettingsResponse?) {
        settings.removeAllViews()

        val inflater = LayoutInflater.from(activity)

        if (settingsData == null) {
            saveSettings?.isEnabled = false
        } else {
            saveSettings?.isEnabled = true

            val settingsPresenters = mutableListOf<Any>()
            settingsPresenters.addAll(settingsData.boolSettings)
            settingsPresenters.addAll(settingsData.stringSettings)
            settingsPresenters.addAll(settingsData.listSettings)

            settingsPresenters.filterNotNull().forEach { inflateSettings(it, inflater) }

            generateSequence(0) {it + 1}.take(settings.childCount-1)
                    .map { i -> settings.getChildAt(i) }
                    .filter { v -> v.getTag() != null && v.getTag(TAG_SETTINGS_MODEL) != null }
                    .map { v ->  v.getTag(TAG_SETTINGS_MODEL)}
                    .forEach { setupDependencies(it) }
        }
    }

    private fun inflateSettings(sp: Any, inflater: LayoutInflater) {
        when(sp){
            is BooleanSettingsPresenter -> inflateBooleanSettings(inflater, sp, settings)
            is StringSettingsPresenter -> inflateStringSettings(inflater, sp, settings)
            is ListSettingsPresenter -> inflateListSettings(inflater, sp, settings)
        }
    }

    private fun inflateBooleanSettings(inflater: LayoutInflater, vp: BooleanSettingsPresenter, container: ViewGroup) {
        val checkBox = inflater.inflate(R.layout.view_settings_checkbox, container, false) as CheckBox
        checkBox.apply {
            text = vp.title
            isChecked = vp.value ?: false
            tag = vp.id
            setTag(TAG_SETTINGS_MODEL, vp)
            setOnCheckedChangeListener {
                compoundButton, isChecked -> onBooleanSettingsChecked(compoundButton, isChecked)}
        }
        container.addView(checkBox)
    }

    private fun inflateStringSettings(inflater: LayoutInflater,
                                      vp: StringSettingsPresenter,
                                      container: ViewGroup) {
        val til = inflater.inflate(R.layout.view_settings_edittext, container, false) as TextInputLayout
        til.tag = vp.id
        til.setTag(TAG_SETTINGS_MODEL, vp)
        til.hint = vp.title
        val editText = til.findViewById(R.id.settings_view) as EditText
        editText.setText(vp.title)

        if (vp.maxLength > 0) {
            editText.filters = arrayOf<InputFilter>(android.text.InputFilter.LengthFilter(vp.maxLength))
        }

        if (vp.isNumber) {
            editText.inputType = InputType.TYPE_CLASS_NUMBER
        }

        RxTextView.textChanges(editText)
                .throttleLast(500, TimeUnit.MILLISECONDS, AndroidSchedulers.mainThread())
                .subscribe { text ->
                    vp.value = text.toString()
                    setupDependencies(vp)
                }

        container.addView(til)
    }

    private fun inflateListSettings(inflater: LayoutInflater,
                                    vp: ListSettingsPresenter,
                                    container: ViewGroup) {
        val settingsGroup = inflater.inflate(R.layout.view_settings_list, container, false) as ViewGroup
        settingsGroup.tag = vp.id

        (settingsGroup.findViewById(R.id.title) as TextView).setText(vp.title)

        val textView = settingsGroup.findViewById(R.id.settings_view) as TextView
        textView.setTag(TAG_SETTINGS_MODEL, vp)

        vp.values
                .filterNotNull()
                .filter({ listValue -> listValue.valueId == vp.value })
                .first()
                .let { listValue -> textView.setText(listValue.title) }

        textView.setOnClickListener { it }

        container.addView(settingsGroup)
    }

    private fun onBooleanSettingsChecked(compoundButton: CompoundButton, isChecked: Boolean) {
        val bsp = compoundButton.getTag(TAG_SETTINGS_MODEL) as BooleanSettingsPresenter
        bsp.value = isChecked
        setupDependencies(bsp)
    }

    private fun setupDependencies(sp: Any) {
        when(sp){
            is BooleanSettingsPresenter -> sp.dependencies
                    .filter { dep -> dep.values.contains(sp.value) }
                    .forEach { setupDependency(it) }
            is StringSettingsPresenter -> sp.dependencies
                    .filter { dep -> dep.values.contains(sp.value) }
                    .forEach { setupDependency(it) }
            is ListSettingsPresenter -> sp.dependencies
                    .filter { dep -> dep.values.contains(sp.value) }
                    .forEach { setupDependency(it) }
        }
    }

    private fun setupDependency(dep: Dependency<*>) {
        dep.settingsIds
                .map({ settingsId -> settings.findViewWithTag(settingsId) })
                .filterNotNull()
                .forEach { view -> view.visibility = if (dep.isVisible) View.VISIBLE else View.GONE }
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