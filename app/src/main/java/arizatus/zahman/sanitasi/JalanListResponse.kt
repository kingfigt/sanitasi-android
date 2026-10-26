package arizatus.zahman.sanitasi

import com.google.gson.annotations.SerializedName

data class JalanData(
    @SerializedName("id") val id: Int,
    @SerializedName("kelurahan") val kelurahan: String,
    @SerializedName("nama_jalan") val namaJalan: String
)

data class JalanListResponse(
    @SerializedName("status") val status: String,
    @SerializedName("data") val data: List<JalanData>
)
