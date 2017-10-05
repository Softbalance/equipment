package ru.softbalance.equipment.presenter

import android.content.Context
import ru.softbalance.equipment.view.fragment.BaseFragment
import java.lang.ref.WeakReference

abstract class Presenter<F : BaseFragment>(context: Context) {

    protected var view: F? = null

    val context: Context = context.applicationContext

    open fun bindView(view: F) {
        this.view = view
    }

    open fun unbindView(view: F) {
        if (this.view == view) {
            this.view = null
        }
    }

    open fun onFinish() {}
}