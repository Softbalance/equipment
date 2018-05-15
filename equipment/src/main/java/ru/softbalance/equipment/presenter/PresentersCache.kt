package ru.softbalance.equipment.presenter

import java.util.*

object PresentersCache {
    val presenters: MutableMap<String, Presenter<*>> = HashMap()

    fun get(presenterName: String): Presenter<*>? = presenters[presenterName]

    fun <T : Presenter<*>> add(presenterName: String, presenter: T): T {
        presenters[presenterName] = presenter
        return presenter
    }

    fun remove(presenterName: String) {
        presenters[presenterName]?.let {
            it.onFinish()
            presenters.remove(presenterName)
        }
    }
}