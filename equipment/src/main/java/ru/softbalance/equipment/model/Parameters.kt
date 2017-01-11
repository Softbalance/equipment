package ru.softbalance.equipment.model

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

import java.math.BigDecimal

@JsonInclude(JsonInclude.Include.NON_NULL)
class Parameters {

    var font: Int? = null

    var bold: Boolean? = null

    var italic: Boolean? = null

    @JsonProperty("dblheight")
    var doubleHeight: Boolean? = null

    var underline: Boolean? = null

    var overline: Boolean? = null

    var negative: Boolean? = null

    var upsideDown: Boolean? = null

    var zeroSlashed: Boolean? = null

    var charRotation: Int? = null

    var standardColor: Boolean? = null

    var wrap: Boolean? = null

    @Alignment
    var alignment: String? = null

    var newLine: Boolean? = null

    var lineSpacingMax: Boolean? = null

    var barCodeHeight: Int? = null

    var barCodeType: String? = null

    @JsonProperty("BarCodeHasCC")
    var barCodeHasControlSymbol: Boolean? = null

    var barCodePrintText: Boolean? = null

    var price: BigDecimal? = null

    var quantity: BigDecimal? = null

    var department: String? = null

    @JsonProperty("Summ")
    var sum: BigDecimal? = null

    var typeClose: Int? = null

    @JsonProperty("EnableCheckSumm")
    var enableCheckSum: Boolean? = null

    var tax: Int? = null

    var printDoc: Boolean? = null

    @ReportType
    var reportType: Int? = null
}
