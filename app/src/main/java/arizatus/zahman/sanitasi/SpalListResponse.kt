package arizatus.zahman.sanitasi

import com.google.gson.annotations.SerializedName

data class SpalListResponse(
    @SerializedName("status")
    val status: String,

    @SerializedName("data")
    val data: List<SpalData>
)

data class SpalData(
    // 🚀 SINKRON: Mengunci target primary key 'id' dari tabel database Laragon
    @SerializedName("id")
    val id_spal: Int?,

    @SerializedName("no_kk")
    val no_kk: String?,

    // 🚀 SINKRON: Fallback penamaan field kepala keluarga atau nama kepala agar klop di RiwayatAdapter
    @SerializedName("kepala_keluarga")
    val nama_kepala: String?,

    @SerializedName("alamat")
    val alamat: String?,

    @SerializedName("kelurahan")
    val kelurahan: String?,

    @SerializedName("status_spal")
    val status_spal: String?,

    @SerializedName("kondisi_bak")
    val kondisi_bak: String?,

    // 🚀 SINKRON: Ditambahkan koordinat agar Geotagging peta manhole SPAL di Mojoroto bisa ter-load kembali saat diedit
    @SerializedName("latitude")
    val latitude: String?,

    @SerializedName("longitude")
    val longitude: String?
)