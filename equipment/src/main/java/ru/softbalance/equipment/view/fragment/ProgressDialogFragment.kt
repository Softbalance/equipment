package ru.softbalance.equipment.view.fragment

import android.app.Dialog
import android.app.ProgressDialog
import android.os.Bundle
import android.support.v7.app.AppCompatDialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

class ProgressDialogFragment : AppCompatDialogFragment() {

    companion object {

        fun spinner(message: String): ProgressDialogFragment {
            return newInstance(ProgressDialog.STYLE_SPINNER, null, message, false)
        }

        fun newInstance(progressStyle: Int, message: String, isCancelable: Boolean): ProgressDialogFragment {
            return newInstance(progressStyle, null, message, false)
        }

        fun newInstance(progressStyle: Int, title: String?, message: String, isCancelable: Boolean): ProgressDialogFragment {
            val dialog = ProgressDialogFragment()
            val args = Bundle()
            args.putString("title", title)
            args.putString("message", message)
            args.putInt("progressStyle", progressStyle)
            args.putBoolean("isCancelable", isCancelable)
            dialog.setArguments(args)
            return dialog
        }
    }

    private var title: String? = null

    private var message: String? = null

    private var progressStyle: Int = 0

    private var dialogCancelable: Boolean = false

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        with(getArguments()) {
           dialogCancelable = getBoolean("isCancelable")
           progressStyle = getInt("progressStyle")
           message = getString("message")
           title = getString("title")
        }
        setCancelable(dialogCancelable)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = ProgressDialog(getActivity(), getTheme())
        dialog.setTitle(title)
        dialog.setMessage(message)
        dialog.isIndeterminate = true
        dialog.setProgressStyle(progressStyle)
        dialog.setCancelable(isCancelable)
        return dialog
    }
}
