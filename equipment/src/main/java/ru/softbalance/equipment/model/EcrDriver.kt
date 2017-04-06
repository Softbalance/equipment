package ru.softbalance.equipment.model

import rx.Observable

interface EcrDriver {

    fun execute(tasks: List<Task>, finishAfterExecute: Boolean): Observable<EquipmentResponse>

    fun finish()

}