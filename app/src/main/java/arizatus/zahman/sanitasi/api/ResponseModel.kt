package arizatus.zahman.sanitasi.api

import com.google.gson.annotations.SerializedName

data class ResponseModel(
    @SerializedName("status")
    val status: String? = null,

    @SerializedName("message")
    val message: String? = null,

    @SerializedName("id_rt")
    val idRt: Int? = null,

    @SerializedName("id_komunal")
    val idKomunal: Int? = null,

    @SerializedName("id_anggaran")
    val idAnggaran: Int? = null
)