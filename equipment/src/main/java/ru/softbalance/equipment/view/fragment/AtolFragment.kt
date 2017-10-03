package ru.softbalance.equipment.view.fragment

import android.content.Intent
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Button
import com.atol.drivers.fptr.settings.SettingsActivity
import ru.softbalance.equipment.R
import ru.softbalance.equipment.presenter.AtolPresenter
import ru.softbalance.equipment.presenter.PresentersCache
import ru.softbalance.equipment.view.DriverSetupActivity.Companion.SETTINGS_ARG
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import java.util.concurrent.TimeUnit

class AtolFragment : BaseFragment() {

    interface Callback {
        fun onSettingsSelected(settings: String, serial: String)
    }

    companion object {

        const val PRESENTER_NAME = "ATOL_PRESENTER"

        const val REQUEST_CONNECT_DEVICE = 1

        fun newInstance(settings: String = ""): AtolFragment {
            val args = Bundle().apply { putString(SETTINGS_ARG, settings) }
            return AtolFragment().apply { arguments = args }
        }
    }

    private var connect: Button? = null
    private var print: Button? = null
    private var getSerial: Button? = null
    private var openShift: Button? = null

    private lateinit var presenter: AtolPresenter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val pr = PresentersCache.get(PRESENTER_NAME)
        presenter = if (pr != null) pr as AtolPresenter else
            PresentersCache.add(PRESENTER_NAME, AtolPresenter(activity, arguments.getString(SETTINGS_ARG)))

        updateResult()
    }

    fun updateResult() {
        if (presenter.printedSuccessful && presenter.settings.isNotEmpty() && hostParent is Callback) {
            (hostParent as Callback).onSettingsSelected(presenter.settings, presenter.serial)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): android.view.View? {

        super.onCreateView(inflater, container, savedInstanceState)

        val rootView = inflater.inflate(R.layout.fragment_atol, container, false)

        connect = rootView.findViewById(R.id.connectPrinter)
        print = rootView.findViewById(R.id.testPrint)
        getSerial = rootView.findViewById(R.id.getSerial)
        openShift = rootView.findViewById(R.id.openShift)

        connect?.setOnClickListener { presenter.startConnection() }
        print?.setOnClickListener { presenter.testPrint() }
        getSerial?.setOnClickListener { presenter.getInfo() }
        openShift?.setOnClickListener { presenter.openShift() }

        presenter.bindView(this)

        return rootView
    }

    override fun onFinish() {
        PresentersCache.remove(PRESENTER_NAME)

        super.onFinish()
    }

    override fun onDestroyView() {
        presenter.unbindView(this)
        super.onDestroyView()
    }

    fun showSettingsState(ok: Boolean) {
        updateResult()

        connect?.setCompoundDrawablesWithIntrinsicBounds(null, null,
                if (ok) ContextCompat.getDrawable(activity, R.drawable.ic_confirm_selector) else null,
                null)
    }

    fun launchConnectionActivity(settings: String) {
        val intent = Intent(activity, SettingsActivity::class.java)
        intent.putExtra(SettingsActivity.DEVICE_SETTINGS, settings)
        startActivityForResult(intent, AtolFragment.REQUEST_CONNECT_DEVICE)
    }

    fun extractSettings(data: Bundle?): String? =
            if (data != null && data.containsKey(SettingsActivity.DEVICE_SETTINGS))
                data.getString(SettingsActivity.DEVICE_SETTINGS)
            else null

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        if (requestCode == AtolFragment.REQUEST_CONNECT_DEVICE && data.extras != null) {
            presenter.updateSettings(extractSettings(data.extras) ?: "")
            Observable.just(true)
                    .delay(500, TimeUnit.MILLISECONDS)
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnNext { presenter.testPrint() }
                    .subscribe({}, Throwable::printStackTrace)
        }
    }

    override val title: String
        get() = getString(R.string.equipment_lib_title_atol)
}