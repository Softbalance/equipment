package ru.softbalance.equipment.view.fragment

import android.content.Intent
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Button
import com.atol.drivers.fptr.settings.SettingsActivity
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import ru.ktoddler.view.fragment.BaseFragment
import ru.softbalance.equipment.R
import ru.softbalance.equipment.model.Task
import ru.softbalance.equipment.model.TaskType
import ru.softbalance.equipment.model.atol.Atol


class AtolFragment : BaseFragment() {

    interface Callback {
        fun onSettingsSelected(settings : String)
    }

    companion object {
        const val REQUEST_CONNECT_DEVICE = 1

        const val SAVE_SETTINGS_PARAM = "SAVE_SETTINGS_PARAM"

        fun newInstance(): AtolFragment = AtolFragment().apply { arguments = Bundle()  }

        private var printTest : Disposable? = null
        private var isPrinting : Boolean = false

        private var connect : Button? = null
        private var print : Button? = null
        private var progressDialog : ProgressDialogFragment? = null
    }

    private lateinit var driver: Atol
    private var settings = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        settings = savedInstanceState?.getString(SAVE_SETTINGS_PARAM) ?: ""
        if(settings.isNotEmpty() && hostParent is Callback) {
            (hostParent as Callback).onSettingsSelected(settings)
        }

        driver = Atol(activity, settings)
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): android.view.View? {

        super.onCreateView(inflater, container, savedInstanceState)

        val rootView = inflater?.inflate(R.layout.fragment_atol, container, false)

        connect = rootView?.findViewById(R.id.connectPrinter) as Button
        print = rootView?.findViewById(R.id.testPrint) as Button

        connect?.setOnClickListener { startConnection() }
        print?.setOnClickListener { testPrint() }

        showSettingsGot()

        progressDialog = childFragmentManager.findFragmentByTag(ProgressDialogFragment::class.java.simpleName)
                as ProgressDialogFragment?

        if(!isPrinting){
            progressDialog?.dismissAllowingStateLoss()
        }

        return rootView
    }

    override fun onDestroyView() {
        connect = null
        print = null
        progressDialog = null
        super.onDestroyView()
    }

    private fun showSettingsGot(){
        if (settings.isNotEmpty()){
            connect?.setCompoundDrawablesWithIntrinsicBounds(null, null,
                    ContextCompat.getDrawable(getActivity(), R.drawable.ic_confirm_selector), null)
        }
    }

    private fun testPrint() {
        if (!isPrinting) {

            progressDialog = ProgressDialogFragment.spinner(getString(R.string.test_print))
            progressDialog?.show(childFragmentManager, ProgressDialogFragment::class.java.simpleName)
            isPrinting = true

            driver.finish()
            driver = Atol(activity, settings)

            val tasks = listOf(
                    Task(data = activity.getString(R.string.text_print)),
                    Task(type = TaskType.PRINT_HEADER))

            printTest = driver.execute(tasks)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnDispose {
                        isPrinting = false
                        progressDialog?.dismissAllowingStateLoss()
                    }
                    .subscribe({
                        showConfirm(it.resultInfo)
                    }, {
                        showError(it.toString())
                    })
        }
    }

    private fun startConnection() {
        val intent = Intent(activity, SettingsActivity::class.java)
        if (settings.isEmpty()) {
            settings = driver.getDefaultSettings()
        }
        intent.putExtra(SettingsActivity.DEVICE_SETTINGS, settings)
        startActivityForResult(intent, AtolFragment.REQUEST_CONNECT_DEVICE)
    }

    fun extractSettings(data: Bundle?): String?  =
        if (data != null && data.containsKey(SettingsActivity.DEVICE_SETTINGS))
            data.getString(SettingsActivity.DEVICE_SETTINGS)
         else null

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        if (requestCode == AtolFragment.REQUEST_CONNECT_DEVICE && data.extras != null) {
            settings = extractSettings(data.extras) ?: ""
            showInfo(getString(R.string.setting_received))
            showSettingsGot()
            if(hostParent is Callback) {
                (hostParent as Callback).onSettingsSelected(settings)
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        outState?.putCharSequence(SAVE_SETTINGS_PARAM, settings)
        super.onSaveInstanceState(outState)
    }
}