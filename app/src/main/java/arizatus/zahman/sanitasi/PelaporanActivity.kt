package arizatus.zahman.sanitasi

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import arizatus.zahman.sanitasi.api.ResponseModel
import arizatus.zahman.sanitasi.api.RetrofitClient
import com.google.gson.JsonObject
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

class PelaporanActivity : AppCompatActivity() {

    private lateinit var acNamaLokasi: Spinner
    private lateinit var etDeskripsi: EditText

    private lateinit var rgKelayakan: RadioGroup
    private lateinit var rgAirBersih: RadioGroup
    private lateinit var rgPembuangan: RadioGroup
    private lateinit var rgKebersihan: RadioGroup
    private lateinit var rgPerbaikan: RadioGroup

    private lateinit var btnUpload: Button
    private lateinit var btnKirim: Button

    private var idUserAktif: Int = 0
    private var userRole: String = ""
    private var userKelurahan: String = ""
    private var idKomunalTerpilih: Int = -1
    private var alamatMckTerpilih: String = ""
    private var imageUri: Uri? = null
    private var currentFileFoto: File? = null
    private val daftarBerkasFoto = ArrayList<File>()

    private val CAMERA_PERMISSION_CODE = 201

    private var isEditMode: Boolean = false
    private var idDataYangDiedit: Int = 0

    private var listKomunalDatabase: ArrayList<KomunalData> = ArrayList()

    private val takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { sukses ->
        if (sukses) {
            currentFileFoto?.let {
                if (it.exists() && it.length() > 0) {
                    daftarBerkasFoto.add(it)
                    btnUpload.text = "TERPILIH ${daftarBerkasFoto.size} FOTO BUKTI"
                    btnUpload.setBackgroundColor(android.graphics.Color.parseColor("#27AE60"))
                }
            }
        } else {
            Toast.makeText(this, "Pengambilan foto dibatalkan", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val resLayoutId = resources.getIdentifier("activity_pelaporan", "layout", packageName)
        if (resLayoutId != 0) {
            setContentView(resLayoutId)
        } else {
            finish()
            return
        }

        val sharedPref = getSharedPreferences("SanitasiSession", Context.MODE_PRIVATE)
        idUserAktif = sharedPref.getInt("USER_ID", 0)
        userRole = sharedPref.getString("USER_ROLE", "") ?: ""
        userKelurahan = sharedPref.getString("USER_KELURAHAN", "") ?: ""

        val idSpinnerLokasi = resources.getIdentifier("et_nama_lokasi_lapor", "id", packageName)
        acNamaLokasi = findViewById<View>(idSpinnerLokasi) as Spinner

        etDeskripsi = findViewById(resources.getIdentifier("et_deskripsi_laporan", "id", packageName))
        rgKelayakan = findViewById(resources.getIdentifier("rg_kelayakan_jamban_lapor", "id", packageName))

        var idAir = resources.getIdentifier("rg_air_inter_lapor", "id", packageName)
        if (idAir == 0) {
            idAir = resources.getIdentifier("rg_air_bersih_lapor", "id", packageName)
        }
        rgAirBersih = findViewById(idAir)

        rgPembuangan = findViewById(resources.getIdentifier("rg_kondisi_pembuangan", "id", packageName))
        rgKebersihan = findViewById(resources.getIdentifier("rg_kebersihan_lapor", "id", packageName))
        rgPerbaikan = findViewById(resources.getIdentifier("rg_butuh_perbaikan", "id", packageName))
        btnUpload = findViewById(resources.getIdentifier("btn_upload", "id", packageName))
        btnKirim = findViewById(resources.getIdentifier("btn_simpan_laporan", "id", packageName))

        etDeskripsi.setTextColor(android.graphics.Color.parseColor("#000000"))
        etDeskripsi.setTypeface(null, android.graphics.Typeface.BOLD)

        loadDataKomunalDariDatabase()

        btnUpload.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_CODE)
            } else {
                bukaKameraMck()
            }
        }

        isEditMode = intent.getBooleanExtra("IS_EDIT_MODE", false)
        idDataYangDiedit = intent.getIntExtra("EDIT_ID", 0)

        if (isEditMode && idDataYangDiedit != 0) {
            btnKirim.text = "Update Perubahan Laporan"
            muatDataLamaUntukDieditLaporan(idDataYangDiedit)
        }

        btnKirim.setOnClickListener { kirimLaporanKeServer() }
        setupNavigation()
    }

    private fun loadDataKomunalDariDatabase() {
        RetrofitClient.instance.getListKomunal().enqueue(object : retrofit2.Callback<KomunalListResponse> {
            override fun onResponse(
                call: retrofit2.Call<KomunalListResponse>,
                response: retrofit2.Response<KomunalListResponse>
            ) {
                if (response.isSuccessful && response.body() != null) {
                    val dataDariServer = response.body()!!.data

                    val listTeksDropdown = ArrayList<String>()
                    listKomunalDatabase = ArrayList()

                    listTeksDropdown.add("Silakan memilih nama lokasi")
                    listKomunalDatabase.add(KomunalData(
                        id = -1,
                        nama_fasilitas = "",
                        nama_sistem = "",
                        alamat = "",
                        kelurahan = ""
                    ))

                    for (item in dataDariServer) {
                        if (userRole.contains("petugas", ignoreCase = true) && userKelurahan.isNotEmpty()) {
                            if (!item.kelurahan.equals(userKelurahan, ignoreCase = true)) {
                                continue
                            }
                        }
                        val namaFasilitasAsli = item.nama_fasilitas ?: "MCK Komunal Tanpa Nama"
                        listTeksDropdown.add(namaFasilitasAsli)
                        listKomunalDatabase.add(item)
                    }

                    val resSpinnerItem = resources.getIdentifier("spinner_item_hitam", "layout", packageName)
                    val adapter = ArrayAdapter(
                        this@PelaporanActivity,
                        if (resSpinnerItem != 0) resSpinnerItem else android.R.layout.simple_spinner_item,
                        listTeksDropdown
                    )
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    acNamaLokasi.adapter = adapter

                    acNamaLokasi.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                            if (position == 0) {
                                idKomunalTerpilih = -1
                                alamatMckTerpilih = ""
                            } else {
                                val objekKomunalAsli = listKomunalDatabase[position]
                                idKomunalTerpilih = objekKomunalAsli.id ?: -1
                                alamatMckTerpilih = objekKomunalAsli.nama_fasilitas ?: ""
                            }
                        }
                        override fun onNothingSelected(parent: AdapterView<*>?) {}
                    }
                } else {
                    Toast.makeText(this@PelaporanActivity, "Server Error: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: retrofit2.Call<KomunalListResponse>, t: Throwable) {
                Toast.makeText(this@PelaporanActivity, "Koneksi Gagal: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun muatDataLamaUntukDieditLaporan(idData: Int) {
        RetrofitClient.instance.getRiwayatPelaporan(idUserAktif).enqueue(object : retrofit2.Callback<JsonObject> {
            override fun onResponse(call: retrofit2.Call<JsonObject>, response: retrofit2.Response<JsonObject>) {
                if (response.isSuccessful && response.body() != null) {
                    try {
                        val jsonBody = response.body()!!
                        val dataArray = jsonBody.getAsJsonArray("data")

                        if (dataArray != null) {
                            for (i in 0 until dataArray.size()) {
                                val item = dataArray.get(i).asJsonObject
                                if (item.get("id")?.asInt == idData) {

                                    val deskripsiLama = item.get("deskrip_masalah")?.asString
                                        ?: item.get("deskripsi_laporan")?.asString
                                        ?: "Uraian Kerusakan Lapangan"

                                    etDeskripsi.setText(deskripsiLama)

                                    val idKomunalLama = item.get("id_komunal")?.asInt ?: -1
                                    if (idKomunalLama != -1) {
                                        val index = listKomunalDatabase.indexOfFirst { it.id == idKomunalLama }
                                        if (index != -1) acNamaLokasi.setSelection(index)
                                    }
                                    break
                                }
                            }
                        }
                        Toast.makeText(this@PelaporanActivity, "Formulir aduan berhasil dipulihkan!", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            override fun onFailure(call: retrofit2.Call<JsonObject>, t: Throwable) {
                Toast.makeText(this@PelaporanActivity, "Gagal memuat data lama", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun bukaKameraMck() {
        try {
            val cachePath = File(externalCacheDir, "camera_photos")
            cachePath.mkdirs()
            currentFileFoto = File(cachePath, "foto_bukti_mck_${System.currentTimeMillis()}.jpg")

            val authorities = "$packageName.fileprovider"
            imageUri = FileProvider.getUriForFile(this, authorities, currentFileFoto!!)

            takePictureLauncher.launch(imageUri!!)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Gagal membuka kamera: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            bukaKameraMck()
        } else {
            Toast.makeText(this, "Izin kamera ditolak oleh pengguna", Toast.LENGTH_SHORT).show()
        }
    }

    private fun kirimLaporanKeServer() {
        if (idUserAktif == 0) {
            idUserAktif = 1
        }

        if (idKomunalTerpilih == -1 || alamatMckTerpilih.isEmpty()) {
            Toast.makeText(this, "Peringatan: Silakan memilih nama lokasi MCK terlebih dahulu!", Toast.LENGTH_LONG).show()
            return
        }

        val deskripsi = etDeskripsi.text.toString().trim()
        if (deskripsi.isEmpty()) {
            Toast.makeText(this, "Mohon isi rincian deskripsi masalah bangunan!", Toast.LENGTH_SHORT).show()
            return
        }

        if (rgKelayakan.checkedRadioButtonId == -1 || rgAirBersih.checkedRadioButtonId == -1 ||
            rgPembuangan.checkedRadioButtonId == -1 || rgKebersihan.checkedRadioButtonId == -1 ||
            rgPerbaikan.checkedRadioButtonId == -1) {
            Toast.makeText(this, "Mohon lengkapi semua opsi penilaian kondisi kelayakan!", Toast.LENGTH_SHORT).show()
            return
        }

        val idKelayakan = rgKelayakan.checkedRadioButtonId
        val idAirBersih = rgAirBersih.checkedRadioButtonId
        val idPembuangan = rgPembuangan.checkedRadioButtonId
        val idKebersihan = rgKebersihan.checkedRadioButtonId
        val idPerbaikan = rgPerbaikan.checkedRadioButtonId

        val kelayakanTxt = findViewById<RadioButton>(idKelayakan).text.toString()
        val airBersihTxt = findViewById<RadioButton>(idAirBersih).text.toString()
        val pembuanganTxt = findViewById<RadioButton>(idPembuangan).text.toString()
        val kebersihanTxt = findViewById<RadioButton>(idKebersihan).text.toString()
        val perbaikanTxt = findViewById<RadioButton>(idPerbaikan).text.toString()

        val idUserBody = idUserAktif.toString().toRequestBody("text/plain".toMediaTypeOrNull())
        val idKomunalBody = idKomunalTerpilih.toString().toRequestBody("text/plain".toMediaTypeOrNull())
        val alamatMckBody = alamatMckTerpilih.toRequestBody("text/plain".toMediaTypeOrNull())
        val deskripsiBody = deskripsi.toRequestBody("text/plain".toMediaTypeOrNull())
        val kelayakanBody = kelayakanTxt.toRequestBody("text/plain".toMediaTypeOrNull())
        val airBersihBody = airBersihTxt.toRequestBody("text/plain".toMediaTypeOrNull())
        val pembuanganBody = pembuanganTxt.toRequestBody("text/plain".toMediaTypeOrNull())
        val kebersihanBody = kebersihanTxt.toRequestBody("text/plain".toMediaTypeOrNull())
        val perbaikanBody = perbaikanTxt.toRequestBody("text/plain".toMediaTypeOrNull())

        val bagianFotoList = ArrayList<MultipartBody.Part>()
        for (file in daftarBerkasFoto) {
            if (file.exists() && file.length() > 0) {
                val reqFile = RequestBody.create("image/jpeg".toMediaTypeOrNull(), file)
                val part = MultipartBody.Part.createFormData("foto_bukti[]", file.name, reqFile)
                bagianFotoList.add(part)
            }
        }

        RetrofitClient.instance.simpanLaporan(
            idUser = idUserBody,
            idKomunal = idKomunalBody,
            alamatMck = alamatMckBody,
            deskripMasalah = deskripsiBody,
            statusKelayakan = kelayakanBody,
            airBersih = airBersihBody,
            kondisiBuangan = pembuanganBody,
            kebersihan = kebersihanBody,
            statusPerbaikan = perbaikanBody,
            foto_bukti = if (bagianFotoList.isEmpty()) null else bagianFotoList
        ).enqueue(object : retrofit2.Callback<ResponseModel> {
            override fun onResponse(call: retrofit2.Call<ResponseModel>, response: retrofit2.Response<ResponseModel>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@PelaporanActivity, if (isEditMode) "Aduan Masalah Sukses Diperbarui!" else "Laporan Fisik Komunal Sukses Dikirim!", Toast.LENGTH_SHORT).show()

                    if (isEditMode) {
                        val intentTujuan = Intent(this@PelaporanActivity, RiwayatInputActivity::class.java)
                        intentTujuan.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                        startActivity(intentTujuan)
                        finish()
                    } else {
                        etDeskripsi.text.clear()
                        acNamaLokasi.setSelection(0)
                        rgKelayakan.clearCheck()
                        rgAirBersih.clearCheck()
                        rgPembuangan.clearCheck()
                        rgKebersihan.clearCheck()
                        rgPerbaikan.clearCheck()
                        btnUpload.text = "AMBIL FOTO MCK"
                        btnUpload.setBackgroundColor(android.graphics.Color.parseColor("#7F8C8D"))
                        daftarBerkasFoto.clear()
                        imageUri = null
                    }
                } else {

                    try {
                        val stringErorAsli = response.errorBody()?.string() ?: "Gagal memproses data di server."

                        AlertDialog.Builder(this@PelaporanActivity)
                            .setTitle("Detail Penolakan Server (400)")
                            .setMessage(stringErorAsli)
                            .setPositiveButton("TUTUP", null)
                            .show()
                    } catch (e: Exception) {
                        Toast.makeText(this@PelaporanActivity, "Gagal decode respon: ${response.code()}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            override fun onFailure(call: retrofit2.Call<ResponseModel>, t: Throwable) {
                Toast.makeText(this@PelaporanActivity, "Kendala Jaringan: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun setupNavigation() {
        val idNavRt = resources.getIdentifier("nav_rt", "id", packageName)
        val idNavKomunal = resources.getIdentifier("nav_komunal", "id", packageName)
        val idNavSpal = resources.getIdentifier("nav_spal", "id", packageName)
        val idNavPelaporan = resources.getIdentifier("nav_pelaporan", "id", packageName)
        val idNavRiwayat = resources.getIdentifier("nav_riwayat", "id", packageName)

        if (idNavRt != 0) { findViewById<LinearLayout>(idNavRt)?.setOnClickListener { startActivity(Intent(this, PendataanRumahTanggaActivity::class.java)); finish() } }
        if (idNavKomunal != 0) { findViewById<LinearLayout>(idNavKomunal)?.setOnClickListener { startActivity(Intent(this, PendataanKomunalActivity::class.java)); finish() } }
        if (idNavSpal != 0) { findViewById<LinearLayout>(idNavSpal)?.setOnClickListener { startActivity(Intent(this, PendataanSpalActivity::class.java)); finish() } }
        if (idNavPelaporan != 0) { findViewById<LinearLayout>(idNavPelaporan)?.setOnClickListener { } }
        if (idNavRiwayat != 0) { findViewById<LinearLayout>(idNavRiwayat)?.setOnClickListener { startActivity(Intent(this, RiwayatInputActivity::class.java)); finish() } }
    }
}