package ru.softbalance.equipment.model.printserver

import io.reactivex.Observable
import ru.softbalance.equipment.model.EcrDriver
import ru.softbalance.equipment.model.EquipmentResponse
import ru.softbalance.equipment.model.Task
import ru.softbalance.equipment.model.printserver.api.PrintServerApi
import ru.softbalance.equipment.model.printserver.api.model.TasksRequest

class PrintServer(val api: PrintServerApi, val settings: String) : EcrDriver {
    override fun execute(tasks: List<Task>): Observable<EquipmentResponse> {
        return Observable.fromCallable { prepareRequest(tasks) }
                .flatMap { api.execute(it) }
    }

    private fun prepareRequest(tasks: List<Task>): TasksRequest {
        return TasksRequest().apply {
            this.tasks = tasks
            this.settings = settings
        }
    }
}