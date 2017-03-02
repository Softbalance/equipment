package ru.softbalance.equipment.view.fragment

import android.app.ProgressDialog
import android.support.v4.app.Fragment
import android.widget.Toast
import ru.softbalance.equipment.view.View

abstract class BaseFragment : Fragment(), View {

    private var progressDialog: ProgressDialog? = null

    protected val hostParent: Any?
        get() {
            var hostParent: Any? = null
            if (parentFragment != null) {
                hostParent = parentFragment
            } else if (activity != null) {
                hostParent = activity
            }
            return hostParent
        }

    override fun onDestroy() {
        if (isRemoving || activity.isFinishing) {
            onFinish()
        }
        super.onDestroy()
    }

    protected open fun onFinish() {}

    private fun toast(msg: String) = Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()

    override fun showError(error: String) = toast(error)

    override fun showInfo(info: String) = toast(info)

    override fun showConfirm(confirm: String) = toast(confirm)

    override fun showLoading(info: String) {
        hideLoading()

        progressDialog = ProgressDialog(activity).apply {
            setMessage(info)
            isIndeterminate = true
            setProgressStyle(ProgressDialog.STYLE_SPINNER)
            setCancelable(false)
            show()
        }

    }

    override fun hideLoading() {
        progressDialog?.dismiss()
    }

    abstract fun getTitle(): String
}
