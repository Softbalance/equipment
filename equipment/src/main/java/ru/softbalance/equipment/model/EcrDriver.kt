package ru.softbalance.equipment.model

import rx.Observable

interface EcrDriver {

    fun execute(tasks: List<Task>): Observable<EquipmentResponse>

    fun finish()

}