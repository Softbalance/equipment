package ru.softbalance.equipment.presenter

import java.util.*

object PresentersCache {
    val presenters: MutableMap<String, Presenter<*>> = HashMap()

    fun get(presenterName : String) : Presenter<*>? = presenters[presenterName]
    fun add(presenterName : String, presenter : Presenter<*>) : Presenter<*>? {
        presenters.put(presenterName, presenter)
        return presenter
    }
    fun remove(presenterName : String) {
        presenters[presenterName]?.onFinish()
        presenters.remove(presenterName)
    }
}