package ru.ktoddler.view

interface View {

    fun showError(error: String)

    fun showInfo(info: String)

    fun showConfirm(confirm: String)

    fun showLoading(info: String)

    fun hideLoading()
}
