package arizatus.zahman.sanitasi.api

import arizatus.zahman.sanitasi.KomunalListResponse
import arizatus.zahman.sanitasi.PelaporanListResponse
import arizatus.zahman.sanitasi.SpalListResponse
import arizatus.zahman.sanitasi.JalanListResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.GET
import retrofit2.http.Query
import com.google.gson.JsonObject

interface ApiService {

    // =======================================================================
    // 1. MODUL DATA RUMAH TANGGA (MULTIPART FOR FOTO)
    // =======================================================================
    @Multipart
    @POST("sanitasi/store-rt")
    fun simpanDataRT(
        @Part("id_user") idUser: RequestBody?,
        @Part("kelurahan") kelurahan: RequestBody,
        @Part("alamat") alamat: RequestBody,
        @Part("no_kk") noKk: RequestBody,
        @Part("kepala_keluarga") kepalaKeluarga: RequestBody,
        @Part("jml_penghuni") jmlPenghuni: RequestBody,
        @Part("kepemilikan_jamban") kepemilikanJamban: RequestBody,
        @Part("jenis_jamban") jenisJamban: RequestBody,
        @Part("status_kelayakan") statusKelayakan: RequestBody,
        @Part("pembuangan") pembuangan: RequestBody,
        @Part("riwayat_sedot") riwayatSedot: RequestBody,
        @Part("greywater") greywater: RequestBody,
        @Part("jarak_air") jarakAir: RequestBody,
        @Part("longitude") longitude: RequestBody,
        @Part("latitude") latitude: RequestBody,
        @Part foto_dokumen: List<MultipartBody.Part>?
    ): Call<ResponseModel>

    @GET("sanitasi/riwayat-rt")
    fun getRiwayatRT(@Query("id_user") idUser: Int): Call<JsonObject>

    // =======================================================================
    // 2. MODUL SANITASI KOMUNAL (FORM URL ENCODED FOR TEXT ONLY)
    // =======================================================================
    @FormUrlEncoded
    @POST("sanitasi/simpan-komunal")
    fun simpanKomunal(
        @Field("id_user") idUser: String,
        @Field("kelurahan") kelurahan: String,
        @Field("alamat") alamat: String,
        @Field("rw") rw: String,
        @Field("rt") rt: String,
        @Field("kode_pos") kodePos: String,
        @Field("nama_fasilitas") namaFasilitas: String,
        @Field("nama_sistem") namaSistem: String,
        @Field("bilik_mandi") bilikMandi: String,
        @Field("jml_wc") jmlWc: String,
        @Field("jarak_air") jarakAir: String,
        @Field("kondisi_fisik") kondisiFisik: String,
        @Field("kondisi_air") kondisiAir: String,
        @Field("status_kelayakan") statusKelayakan: String,
        @Field("latitude") latitude: String,
        @Field("longitude") longitude: String
    ): Call<ResponseModel>

    @GET("sanitasi/list-komunal")
    fun getListKomunal(): Call<KomunalListResponse>

    @GET("sanitasi/riwayat-komunal")
    fun getRiwayatKomunal(@Query("id_user") idUser: Int): Call<JsonObject>

    // =======================================================================
    // 3. MODUL JARINGAN SPAL (FORM URL ENCODED)
    // =======================================================================
    @Multipart
    @POST("sanitasi/store-spal")
    fun simpanSpal(
        @Part("id_user") idUser: RequestBody,
        @Part("no_kk") noKk: RequestBody,
        @Part("nama_kepala") namaKepala: RequestBody,
        @Part("kelurahan") kelurahan: RequestBody,
        @Part("alamat") alamat: RequestBody,
        @Part("status_spal") statusSpal: RequestBody,
        @Part("kondisi_bak") kondisiBak: RequestBody,
        @Part("latitude") latitude: RequestBody,
        @Part("longitude") longitude: RequestBody,
        @Part foto_dokumen: List<MultipartBody.Part>?
    ): Call<ResponseModel>

    @GET("sanitasi/riwayat-spal")
    fun getRiwayatSpal(@Query("id_user") idUser: Int): Call<JsonObject>

    // =======================================================================
    // 4. MODUL PELAPORAN ADUAN (MULTIPART FOR FOTO BUKTI)
    // =======================================================================
    @Multipart
    @POST("sanitasi/simpan-laporan")
    fun simpanLaporan(
        @Part("id_user") idUser: RequestBody,
        @Part("id_komunal") idKomunal: RequestBody,
        @Part("alamat_mck") alamatMck: RequestBody,
        @Part("deskrip_masalah") deskripMasalah: RequestBody,
        @Part("status_kelayakan") statusKelayakan: RequestBody,
        @Part("air_bersih") airBersih: RequestBody,
        @Part("kondisi_buangan") kondisiBuangan: RequestBody,
        @Part("kebersihan") kebersihan: RequestBody,
        @Part("status_perbaikan") statusPerbaikan: RequestBody,
        @Part foto_bukti: List<MultipartBody.Part>?
    ): Call<ResponseModel>

    @GET("sanitasi/riwayat-pelaporan")
    fun getRiwayatPelaporan(@Query("id_user") idUser: Int): Call<JsonObject>

    // =======================================================================
    // 5. SEKTOR SECURITY LOGINS
    // =======================================================================
    @FormUrlEncoded
    @POST("sanitasi/login")
    fun loginUser(
        @Field("username") username: String,
        @Field("password") password: String
    ): Call<LoginResponse>

    @POST("sanitasi/logout")
    fun logoutUser(): Call<ResponseModel>

    @GET("sanitasi/list-jalan")
    fun getListJalan(@Query("kelurahan") kelurahan: String? = null): Call<JalanListResponse>

    @FormUrlEncoded
    @POST("sanitasi/delete-data")
    fun deleteData(
        @Field("type") type: String,
        @Field("id") id: Int
    ): Call<ResponseModel>
}

// =======================================================================
// DATA MODEL WRAPPER (Pastikan ditaruh di bawah atau satu package)
// =======================================================================
data class LoginResponse(val status: String, val message: String, val user: UserData?)
data class UserData(val id: Int, val name: String, val username: String, val role: String, val kelurahan: String?)
data class KomunalListResponse(val status: String, val data: List<KomunalData>)
data class KomunalData(val id: Int?, val nama_fasilitas: String?, val nama_sistem: String?, val alamat: String?)
data class RiwayatItem(val id: Int, val id_user: Int, val id_komunal: Int?, val nama_fasilitas: String?, val kepala_keluarga: String?, val alamat: String?, val alamat_mck: String?, val deskrip_masalah: String?, val created_at: String)
data class ResponseRiwayat(val status: String, val data: ArrayList<RiwayatItem>)