package ru.softbalance.equipment.model

import io.reactivex.Observable

interface EcrDriver {

    fun execute(tasks: List<Task>): Observable<EquipmentResponse>

}