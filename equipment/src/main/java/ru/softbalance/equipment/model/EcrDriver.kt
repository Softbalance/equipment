package ru.softbalance.equipment.model

import rx.Single

interface EcrDriver {

    fun execute(tasks: List<Task>, finishAfterExecute: Boolean): Single<EquipmentResponse>

    fun getSerial(finishAfterExecute: Boolean): Single<String>

    fun getSessionState(finishAfterExecute: Boolean): Single<SessionStateResponse>

    fun openShift(finishAfterExecute: Boolean): Single<OpenShiftResponse>

    fun finish()
}