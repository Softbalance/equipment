package ru.softbalance.equipment.model.atol

import android.content.Context
import android.util.Log
import com.atol.drivers.fptr.Fptr
import com.atol.drivers.fptr.IFptr
import com.atol.drivers.paycard.IPaycard
import com.atol.drivers.paycard.Paycard
import rx.Single

object AtolPayment {

    fun paymentTransaction(context: Context, fptrSettings: String,
                           paycardSettings: String, sum: Double): Single<String> = Single.fromCallable {
        val fptr = Fptr()
        val paycard = Paycard()

        try {
            fptr.create(context)
            paycard.create(context)

            log("Загрузка настроек ККМ и ПТ...")
            if (fptr.put_DeviceSettings(fptrSettings) < 0) {
                checkError(fptr)
            }
            if (paycard.put_DeviceSettings(paycardSettings) < 0) {
                checkError(paycard)
            }
            paycard.put_PinPadDevice(fptr._PinPadDevice)
            paycard.put_ModemDevice(fptr._ModemDevice)

            log("Установка соединения...")
            if (fptr.put_DeviceEnabled(true) < 0) {
                checkError(fptr)
            }
            if (paycard.put_DeviceEnabled(true) < 0) {
                checkError(paycard)
            }
            log("OK")

            log("Проверка связи с ККМ...")
            if (fptr.GetStatus() < 0) {
                checkError(fptr)
            }
            log("OK")

            log("Продажа...")
            if (paycard.put_AuthorizationType(IPaycard.AUTHORIZATION_READER_PIN) < 0) {
                checkError(paycard)
            }
            if (paycard.put_OperationType(IPaycard.OPERATION_SUB) < 0) {
                checkError(paycard)
            }
            if (paycard.put_Sum(sum) < 0) {
                checkError(paycard)
            }
            if (paycard.put_CharLineLength(fptr._CharLineLength) < 0) {
                checkError(paycard)
            }
            if (paycard.PrepareAuthorization() < 0) {
                checkError(paycard)
            }
            if (paycard.Authorization() < 0) {
                checkError(paycard)
            }
            log("OK")

            paycard._Text

        } catch (e: Exception) {
            log(e.toString())
            throw e
        } finally {
            fptr.destroy()
            paycard.destroy()
        }
    }

    fun printSlip(context: Context, fptrSettings: String, slipText: String): Single<Boolean> = Single.fromCallable {

        val fptr = Fptr()

        try {
            fptr.create(context)
            log("Загрузка настроек ККМ ...")
            if (fptr.put_DeviceSettings(fptrSettings) < 0) {
                checkError(fptr)
            }

            if (fptr.put_DeviceEnabled(true) < 0) {
                checkError(fptr)
            }

            log("Проверка связи с ККМ...")
            if (fptr.GetStatus() < 0) {
                checkError(fptr)
            }
            log("OK")

            log("Печать слипа...")

            if (fptr.put_Caption(slipText) < 0) {
                checkError(fptr)
            }
            if (fptr.put_TextWrap(IFptr.WRAP_LINE) < 0) {
                checkError(fptr)
            }
            if (fptr.put_Alignment(IFptr.ALIGNMENT_LEFT) < 0) {
                checkError(fptr)
            }
            if (fptr.PrintString() < 0) {
                checkError(fptr)
            }
            if (fptr.put_Mode(IFptr.MODE_REPORT_NO_CLEAR) < 0) {
                checkError(fptr)
            }
            if (fptr.SetMode() < 0) {
                checkError(fptr)
            }
            if (fptr.PrintFooter() < 0) {
                checkError(fptr)
            }
            log("OK")
            true
        } catch (e: Exception) {
            log(e.toString())
            throw e
        } finally {
            fptr.destroy()
        }
    }

    @Throws(AtolPaymentException::class)
    private fun checkError(i: IFptr) {
        val rc = i._ResultCode
        if (rc < 0) {
            val rd = i._ResultDescription
            var bpd: String? = null
            if (rc == -6) {
                bpd = i._BadParamDescription
            }
            if (bpd != null) {
                throw AtolPaymentException(String.format("[%d] %s (%s)", rc, rd, bpd))
            } else {
                throw AtolPaymentException(String.format("[%d] %s", rc, rd))
            }
        }
    }

    @Throws(AtolPaymentException::class)
    private fun checkError(i: IPaycard) {
        val rc = i._ResultCode
        if (rc < 0) {
            val rd = i._ResultDescription
            var bpd: String? = null
            if (rc == -6) {
                bpd = i._BadParamDescription
            }
            if (bpd != null) {
                throw AtolPaymentException(String.format("[%d] %s (%s)", rc, rd, bpd))
            } else {
                throw AtolPaymentException(String.format("[%d] %s", rc, rd))
            }
        }
    }

    class AtolPaymentException(msg: String) : Exception(msg)

    private fun log(msg: String) {
        Log.d("AtolPayment", msg)
    }
}