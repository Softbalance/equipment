package ru.softbalance.equipment.model

import rx.Single

interface EcrDriver {

    fun execute(tasks: List<Task>, finishAfterExecute: Boolean): Single<EquipmentResponse>

    fun getSerial(finishAfterExecute: Boolean): Single<SerialResponse>

    fun getSessionState(finishAfterExecute: Boolean): Single<SessionStateResponse>

    fun openShift(finishAfterExecute: Boolean): Single<OpenShiftResponse>

    fun getOfdStatus(finishAfterExecute: Boolean): Single<OfdStatusResponse>

    fun finish()
}