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
        fun onSettingsSelected(settings: String, url: String, port: Int)
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

    private lateinit var connect: Button
    private lateinit var saveSettings: Button
    private lateinit var print: Button
    private lateinit var deviceTypes: TextView
    private lateinit var deviceModels: TextView
    private lateinit var deviceDrivers: TextView

    private lateinit var url: EditText
    private lateinit var port: EditText

    private lateinit var settings: LinearLayout

    private lateinit var presenter: PrintServerPresenter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val pr = PresentersCache.get(PRESENTER_NAME)
        presenter = if (pr != null) pr as PrintServerPresenter else
            PresentersCache.add(PRESENTER_NAME,
                    PrintServerPresenter(activity, arguments.getString(URL_ARG), arguments.getInt(PORT_ARG)))
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): android.view.View? {
        super.onCreateView(inflater, container, savedInstanceState)
        val rootView = inflater.inflate(R.layout.fragment_print_server, container, false)

        url = rootView.findViewById(R.id.ip_address) as EditText
        port = rootView.findViewById(R.id.port) as EditText
        deviceTypes = rootView.findViewById(R.id.device_type) as TextView
        deviceModels = rootView.findViewById(R.id.model) as TextView
        deviceDrivers = rootView.findViewById(R.id.driver) as TextView
        saveSettings = rootView.findViewById(R.id.save_settings) as Button
        settings = rootView.findViewById(R.id.settings_layout) as LinearLayout

        if (savedInstanceState == null) {
            port.setText(arguments.getInt(PORT_ARG).toString())
            url.setText(arguments.getString(URL_ARG))
        }

        connect = rootView.findViewById(R.id.connect) as Button
        print = rootView.findViewById(R.id.testPrint) as Button

        connect.setOnClickListener { presenter.getDevices(url.text.toString(), port.text.toString().toInt()) }
        deviceTypes.setOnClickListener { selectDevice() }
        deviceModels.setOnClickListener { selectModel() }
        deviceDrivers.setOnClickListener { selectDriver() }
        saveSettings.setOnClickListener { presenter.saveSettings() }
        print.setOnClickListener { presenter.testPrint() }

        Observable.combineLatest(// TODO move to javarx2
                RxTextView.textChanges(url),
                RxTextView.textChanges(port)
        ) { urlValue, portValue ->
            urlValue.isNotEmpty() && portValue.isNotEmpty() &&
                    HttpUrl.parse(presenter.getPrintServerUrl(urlValue.toString(), portValue.toString().toInt())) != null
        }
                .subscribe { enabled -> connect.isEnabled = enabled }

        presenter.bindView(this)

        return rootView
    }

    fun updateResult(ok: Boolean) {
        if (ok && hostParent is PrintServerFragment.Callback) {
            val callback = hostParent ?: return
            val settings = presenter.zipSettings ?: return
            if (callback is PrintServerFragment.Callback) {
                callback.onSettingsSelected(settings, presenter.url, presenter.port)
            }
        }
    }

    private fun selectDevice() {
        val types = presenter.deviceTypes

        if (types == null) {
            showError(getString(R.string.no_data))
        } else {
            val popupMenu = ViewUtils.createPopupMenu(activity, deviceTypes, 0, false)
            for (i in types.indices) {
                popupMenu.menu.add(0, i, i, types[i].name)
            }

            popupMenu.setOnMenuItemClickListener({ item ->
                presenter.selectDeviceType(types[item.itemId])
                true
            })

            popupMenu.show()
        }
    }

    private fun selectModel() {
        val models = presenter.models

        if (models == null) {
            showError(getString(R.string.no_data))
        } else {
            val popupMenu = ViewUtils.createPopupMenu(activity, deviceModels, 0, false)
            for (i in models.indices) {
                popupMenu.menu.add(0, i, i, models[i].name)
            }

            popupMenu.setOnMenuItemClickListener({ item ->
                presenter.selectModel(models[item.itemId])
                true
            })

            popupMenu.show()
        }
    }

    private fun selectDriver() {
        val drivers = presenter.drivers

        if (drivers == null) {
            showError(getString(R.string.no_data))
        } else {
            val popupMenu = ViewUtils.createPopupMenu(activity, deviceDrivers, 0, false)
            for (i in drivers.indices) {
                popupMenu.menu.add(0, i, i, drivers[i].name)
            }

            popupMenu.setOnMenuItemClickListener({ item ->
                presenter.selectDriver(drivers[item.itemId])
                true
            })

            popupMenu.show()
        }
    }

    fun buildSettingsUI(settingsData: MutableList<SettingsPresenter<*, *>>) {
        settings.removeAllViews()

        val inflater = LayoutInflater.from(activity)

        if (settingsData.isEmpty()) {
            saveSettings.isEnabled = false
        } else {
            saveSettings.isEnabled = true

            settingsData.filterNotNull()
                    .sortedBy { it.sort }
                    .forEach { inflateSettings(it, inflater) }

            generateSequence(0) { it + 1 }.take(settings.childCount - 1)
                    .map { i -> settings.getChildAt(i) }
                    .filter { v -> v.tag != null && v.getTag(TAG_SETTINGS_MODEL) != null }
                    .map { v -> v.getTag(TAG_SETTINGS_MODEL) }
                    .forEach { setupDependencies(it) }
        }
    }

    private fun inflateSettings(sp: Any, inflater: LayoutInflater) {
        when (sp) {
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
                compoundButton, isChecked ->
                onBooleanSettingsChecked(compoundButton, isChecked)
            }
        }
        container.addView(checkBox)
    }

    private fun inflateStringSettings(inflater: LayoutInflater,
                                      vp: StringSettingsPresenter,
                                      container: ViewGroup) {
        val til = inflater.inflate(R.layout.view_settings_edittext, container, false) as TextInputLayout
        with(til) {
            tag = vp.id
            setTag(TAG_SETTINGS_MODEL, vp)
            hint = vp.title
        }
        val editText = til.findViewById(R.id.settings_view) as EditText
        editText.setText(vp.value)

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
                    presenter.saveSettingValue(vp)
                    setupDependencies(vp)
                }

        container.addView(til)
    }

    private fun inflateListSettings(inflater: LayoutInflater,
                                    vp: ListSettingsPresenter,
                                    container: ViewGroup) {
        val settingsGroup = inflater.inflate(R.layout.view_settings_list, container, false) as ViewGroup
        settingsGroup.tag = vp.id

        (settingsGroup.findViewById(R.id.title) as TextView).text = vp.title

        val textView = settingsGroup.findViewById(R.id.settings_view) as TextView
        textView.setTag(TAG_SETTINGS_MODEL, vp)

        vp.values
                .filterNotNull()
                .filter { listValue -> listValue.valueId == vp.value }
                .first()
                .let { listValue -> textView.text = listValue.title }

        textView.setOnClickListener { onClickListSettings(textView) }

        container.addView(settingsGroup)
    }

    private fun onClickListSettings(view: View) {
        val vsp = view.getTag(TAG_SETTINGS_MODEL) as ListSettingsPresenter

        val popup = ViewUtils.createPopupMenu(activity, view, 0, false)
        vsp.values
                .filterNotNull()
                .forEach { listValue ->
                    popup.menu.add(0,
                            listValue.valueId,
                            listValue.valueId,
                            listValue.title)
                }

        popup.setOnMenuItemClickListener { item ->
            (view as TextView).text = item.title
            vsp.value = item.itemId
            presenter.saveSettingValue(vsp)
            setupDependencies(vsp)
            true
        }

        popup.show()
    }

    private fun onBooleanSettingsChecked(compoundButton: CompoundButton, isChecked: Boolean) {
        val bsp = compoundButton.getTag(TAG_SETTINGS_MODEL) as BooleanSettingsPresenter
        bsp.value = isChecked
        presenter.saveSettingValue(bsp)
        setupDependencies(bsp)
    }

    private fun setupDependencies(sp: Any) {
        val deps = when (sp) {
            is BooleanSettingsPresenter -> sp.dependencies.filter { dep -> dep.values.contains(sp.value) }
            is StringSettingsPresenter -> sp.dependencies.filter { dep -> dep.values.contains(sp.value) }
            is ListSettingsPresenter -> sp.dependencies.filter { dep -> dep.values.contains(sp.value) }
            else -> emptyList<Dependency<Any>>()
        }
        deps.forEach { setupDependency(it) }
    }

    private fun setupDependency(dep: Dependency<*>) {
        dep.settingsIds
                .map({ settingsId -> settings.findViewWithTag(settingsId) })
                .filterNotNull()
                .forEach { view -> view.visibility = if (dep.isVisible) View.VISIBLE else View.GONE }
    }

    fun showConnectionState(ok: Boolean) {
        connect.setCompoundDrawablesWithIntrinsicBounds(null, null,
                if (ok) ContextCompat.getDrawable(activity, R.drawable.ic_confirm_selector) else null,
                null)
    }

    fun showPrintState(ok: Boolean) {
        print.setCompoundDrawablesWithIntrinsicBounds(null, null,
                if (ok) ContextCompat.getDrawable(activity, R.drawable.ic_confirm_selector) else null,
                null)
    }

    fun showPrintAvailable(ok: Boolean) {
        updateResult(ok)
        print.isEnabled = ok
    }

    fun showType(type: PrintDeviceType) {
        deviceTypes.text = type.name
    }

    fun showModel(model: PrintDeviceModel) {
        deviceModels.text = model.name
    }

    fun showDriver(driver: PrintDeviceDriver) {
        deviceDrivers.text = driver.name
    }

    override fun onFinish() {
        PresentersCache.remove(PRESENTER_NAME)

        super.onFinish()
    }

    override fun onDestroyView() {
        presenter.unbindView(this)
        super.onDestroyView()
    }
}