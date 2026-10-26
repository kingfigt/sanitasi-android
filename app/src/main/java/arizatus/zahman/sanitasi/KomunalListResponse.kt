package arizatus.zahman.sanitasi

import com.google.gson.annotations.SerializedName

data class KomunalListResponse(
    @SerializedName("status")
    val status: String,

    @SerializedName("data")
    val data: List<KomunalData>
)

// 🚀 SINKRON MURNI: Hanya mengambil 4 parameter utama sesuai database awalmu!
data class KomunalData(
    @SerializedName("id")
    val id: Int?,

    @SerializedName("nama_fasilitas")
    val nama_fasilitas: String?,

    @SerializedName("nama_sistem")
    val nama_sistem: String?,

    @SerializedName("alamat")
    val alamat: String?,

    @SerializedName("kelurahan")
    val kelurahan: String?
)