package arizatus.zahman.sanitasi

import com.google.gson.annotations.SerializedName

data class PelaporanListResponse(
    @SerializedName("status")
    val status: String,

    @SerializedName("data")
    val data: List<PelaporanData>
)

data class PelaporanData(
    // 🚀 SINKRON: Mengikuti nama kolom primary key 'id' dari tabel laporans di database Navicat
    @SerializedName("id")
    val id_laporan: Int?,

    // 🚀 SINKRON: Mengikuti key 'id_komunal' untuk kebutuhan tracking lokasi sarana komunal
    @SerializedName("id_komunal")
    val id_komunal: Int?,

    @SerializedName("alamat_mck")
    val alamat_mck: String?,

    @SerializedName("deskrip_masalah")
    val deskrip_masalah: String?,

    @SerializedName("status_perbaikan")
    val status_perbaikan: String?
)