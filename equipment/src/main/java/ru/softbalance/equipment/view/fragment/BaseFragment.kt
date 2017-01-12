package ru.ktoddler.view.fragment

import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import ru.ktoddler.view.View

abstract class BaseFragment : Fragment(), View {

    companion object {
        private var currentContext: Context? = null
    }

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

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): android.view.View? {
        currentContext = context;
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onDestroyView() {
        currentContext = null;
        super.onDestroyView()
    }

    protected open fun onFinish() {}

    private fun isFragmentActive(tag: String): Boolean {
        val fragment = childFragmentManager.findFragmentByTag(tag)
        return fragment != null && !fragment.isDetached && !fragment.isRemoving
    }

    private fun toast(msg: String) = Toast.makeText(currentContext, msg, Toast.LENGTH_SHORT).show()

    override fun showError(err: String) = toast(err)

    override fun showInfo(info: String) = toast(info)

    override fun showConfirm(confirm: String) = toast(confirm)

    override fun showLoading(info: String) {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun hideLoading() {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}
