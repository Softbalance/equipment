package ru.softbalance.equipment.presenter

import android.content.Context
import ru.softbalance.equipment.view.fragment.BaseFragment
import java.lang.ref.WeakReference

abstract class Presenter <F : BaseFragment>{

    var viewRef : WeakReference<F> = WeakReference<F>(null)

    fun view() : F? = viewRef.get()

    var context : Context? = null

    open fun bindView(view: F) {
        this.viewRef.clear()
        this.viewRef = WeakReference(view)

        context = view.activity
    }

    open fun unbindView(view: F) {
        if (this.viewRef.get() != null && this.viewRef.get() == view) {
            this.viewRef.clear()
        }

        context = null
    }

    abstract fun onFinish()
}