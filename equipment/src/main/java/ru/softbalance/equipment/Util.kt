package ru.softbalance.equipment

import android.view.View
import android.view.ViewConfiguration
import rx.Subscription

fun String.toHttpUrl(port: Int): String {
    var url: String
    if (this.startsWith("http://")) {
        url = this
    } else {
        url = "http://" + this
    }

    if (!this.endsWith(":$port")) {
        url += ":$port"
    }

    return url
}

fun Subscription?.isActive(): Boolean {
    return this != null && !this.isUnsubscribed
}

fun Subscription?.isNonActive(): Boolean {
    return this == null || this.isUnsubscribed
}

class SingleClickListener(val click: (v: View) -> Unit) : View.OnClickListener {

    companion object {
        private val DOUBLE_CLICK_TIMEOUT = ViewConfiguration.getDoubleTapTimeout()
    }

    private var lastClick: Long = 0

    override fun onClick(v: View) {
        if (getLastClickTimeout() > DOUBLE_CLICK_TIMEOUT) {
            lastClick = System.currentTimeMillis()
            click(v)
        }
    }

    private fun getLastClickTimeout(): Long {
        return System.currentTimeMillis() - lastClick
    }
}

/**
 * Click listener setter that prevents double click on the view it´s set
 */
fun View.singleClick(l: (android.view.View?) -> Unit){
    setOnClickListener(SingleClickListener(l))
}

var View.visible: Boolean
    get() {
        return this.visibility == View.VISIBLE
    }
    set(value) {
        if (value) {
            this.visibility = View.VISIBLE
        } else {
            this.visibility = View.GONE
        }
    }
