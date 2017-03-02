package ru.softbalance.equipment

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