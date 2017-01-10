package ru.softbalance.equipment.model

interface EcrDriver {

    fun execute(tasks: List<Task>): Boolean

}