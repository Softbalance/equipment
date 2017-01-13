package ru.softbalance.equipment.view.fragment

import android.content.Intent
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Button
import com.atol.drivers.fptr.settings.SettingsActivity
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import ru.softbalance.equipment.R
import ru.softbalance.equipment.presenter.AtolPresenter
import ru.softbalance.equipment.presenter.PresentersCache
import java.util.concurrent.TimeUnit

class AtolFragment : BaseFragment() {

    interface Callback {
        fun onSettingsSelected(settings : String)
    }

    companion object {

        const val PRESENTER_NAME = "ATOL_PRESENTER"

        const val REQUEST_CONNECT_DEVICE = 1

        fun newInstance(): AtolFragment = AtolFragment().apply { arguments = Bundle()  }
    }

    private var connect : Button? = null
    private var print : Button? = null

    private lateinit var presenter: AtolPresenter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val pr = PresentersCache.get(PRESENTER_NAME)
        presenter = if (pr != null) pr as AtolPresenter else
            PresentersCache.add(PRESENTER_NAME, AtolPresenter()) as AtolPresenter

        updateResult()
    }

    fun updateResult(){
        if(presenter.printedSuccessful && presenter.settings.isNotEmpty() && hostParent is Callback) {
            (hostParent as Callback).onSettingsSelected(presenter.settings)
        }
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): android.view.View? {

        super.onCreateView(inflater, container, savedInstanceState)

        val rootView = inflater?.inflate(R.layout.fragment_atol, container, false)

        connect = rootView?.findViewById(R.id.connectPrinter) as Button
        print = rootView?.findViewById(R.id.testPrint) as Button

        connect?.setOnClickListener { presenter.startConnection() }
        print?.setOnClickListener { presenter.testPrint() }

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

    fun showSettingsState(ok: Boolean){
        updateResult()

        connect?.setCompoundDrawablesWithIntrinsicBounds(null, null,
                if (ok) ContextCompat.getDrawable(getActivity(), R.drawable.ic_confirm_selector) else null,
                null)
    }

    fun launchConnectionActivity(settings : String) {
        val intent = Intent(activity, SettingsActivity::class.java)
        intent.putExtra(SettingsActivity.DEVICE_SETTINGS, settings)
        startActivityForResult(intent, AtolFragment.REQUEST_CONNECT_DEVICE)
    }

    fun extractSettings(data: Bundle?): String?  =
        if (data != null && data.containsKey(SettingsActivity.DEVICE_SETTINGS))
            data.getString(SettingsActivity.DEVICE_SETTINGS)
         else null

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        if (requestCode == AtolFragment.REQUEST_CONNECT_DEVICE && data.extras != null) {
            presenter.settings = extractSettings(data.extras) ?: ""
            Observable.just(true)
                    .delay(500, TimeUnit.MILLISECONDS, AndroidSchedulers.mainThread())
                    .doOnNext { presenter.testPrint() }
                    .subscribe({}, Throwable::printStackTrace)
        }
    }
}