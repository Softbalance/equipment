package ru.softbalance.equipment.view.fragment

import android.support.v4.app.Fragment
import android.widget.Toast
import ru.ktoddler.view.View

abstract class BaseFragment : Fragment(), View {

    private var progressDialog : ProgressDialogFragment? = null

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

    private fun getShowingDialog() : Fragment? =
            childFragmentManager.findFragmentByTag(ProgressDialogFragment::class.java.simpleName)

    override fun showLoading(info: String) {
        if (getShowingDialog() == null) {
            childFragmentManager.beginTransaction().let {
                it.add(ProgressDialogFragment.spinner(info), ProgressDialogFragment::class.java.simpleName)
                it.commitAllowingStateLoss()
            }
        }
    }

    override fun hideLoading() {
         if(getShowingDialog() != null){
            (getShowingDialog() as ProgressDialogFragment).dismissAllowingStateLoss()
        }
    }
}
