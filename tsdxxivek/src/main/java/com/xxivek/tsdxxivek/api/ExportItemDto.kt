package com.xxivek.tsdxxivek.api

import com.google.gson.annotations.SerializedName

/**
 * DTO для экспорта одной позиции товара в JSON.
 * Соответствует структуре: {Shtrih, ShtrihTip, TovNaim, TovCena, TovKol}
 */
data class ExportItemDto(
    @SerializedName("Shtrih")
    val shtrih: String,

    @SerializedName("ShtrihTip")
    val shtrihTip: Int,

    @SerializedName("TovNaim")
    val tovNaim: String,

    @SerializedName("TovCena")
    val tovCena: Double,

    @SerializedName("TovKol")
    val tovKol: Int
)
