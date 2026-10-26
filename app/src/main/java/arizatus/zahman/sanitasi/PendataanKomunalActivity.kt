package arizatus.zahman.sanitasi

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import arizatus.zahman.sanitasi.api.ResponseModel
import arizatus.zahman.sanitasi.api.RetrofitClient
import arizatus.zahman.sanitasi.utils.LocationHelper
import com.google.gson.Gson
import com.google.gson.JsonObject
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import java.util.Locale

class PendataanKomunalActivity : AppCompatActivity() {

    // Binding Komponen Data Wilayah Bercabang
    private lateinit var spinnerKelurahan: Spinner
    private lateinit var spinnerAlamatJalan: Spinner
    private lateinit var spinnerRt: Spinner
    private lateinit var spinnerRw: Spinner
    private lateinit var etKodePos: EditText
    private lateinit var etNamaFasilitas: EditText

    // Komponen input spesifikasi komunal
    private lateinit var spinnerJenisKomunal: Spinner
    private lateinit var spinnerBilikMandi: Spinner
    private lateinit var spinnerJumlahKakus: Spinner
    private lateinit var spinnerJarakAirKomunal: Spinner

    // Binding Komponen RadioGroup Penilaian Fisik & Kelayakan
    private lateinit var rgKondisiFisik: RadioGroup
    private lateinit var rgAirKomunal: RadioGroup
    private lateinit var rgKelayakan: RadioGroup

    private lateinit var btnSimpan: Button
    private lateinit var mapView: MapView

    private var markerKomunal: Marker? = null
    private var selectedLatitude: Double = -7.816
    private var selectedLongitude: Double = 112.010
    private val LOCATION_PERMISSION_CODE = 104

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private var idUserAktif: Int = 0
    private var userRole: String = ""
    private var userKelurahan: String = ""

    // Variabel kontrol mode edit dari halaman riwayat
    private var isEditMode: Boolean = false
    private var idDataYangDiedit: Int = 0

    // Master Data Kelurahan Kecamatan Mojoroto Kota Kediri
    private val daftarKelurahan = arrayOf(
        "Pilih Kelurahan", "Mojoroto", "Pojok", "Sukorame", "Bandar Lor",
        "Bandar Kidul", "Campurejo", "Lirboyo", "Mrican", "Dermo",
        "Ngampel", "Gayam", "Bujel", "Banjarmlati", "Tamanan"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().load(this, getSharedPreferences("osmdroid", Context.MODE_PRIVATE))

        val resLayoutId = resources.getIdentifier("activity_pendataan_komunal", "layout", packageName)
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

        // 1. Inisialisasi ID Sektor Data Wilayah
        spinnerKelurahan = findViewById(resources.getIdentifier("spinner_kelurahan", "id", packageName))
        spinnerAlamatJalan = findViewById(resources.getIdentifier("spinner_alamat_jalan", "id", packageName))
        spinnerRt = findViewById(resources.getIdentifier("spinner_rt", "id", packageName))
        spinnerRw = findViewById(resources.getIdentifier("spinner_rw", "id", packageName))
        etKodePos = findViewById(resources.getIdentifier("et_detail_no_rumah", "id", packageName))
        etNamaFasilitas = findViewById(resources.getIdentifier("et_nama_fasilitas", "id", packageName))

        // 2. Inisialisasi ID Sektor Spesifikasi Komunal Baru
        spinnerJenisKomunal = findViewById(resources.getIdentifier("spinner_jenis_komunal", "id", packageName))
        spinnerBilikMandi = findViewById(resources.getIdentifier("spinner_bilik_mandi", "id", packageName))
        spinnerJumlahKakus = findViewById(resources.getIdentifier("spinner_jumlah_kakus", "id", packageName))
        spinnerJarakAirKomunal = findViewById(resources.getIdentifier("spinner_jarak_air_komunal", "id", packageName))

        // 3. Inisialisasi ID Sektor Penilaian RadioGroup dengan fallback aman
        var targetFisikId = resources.getIdentifier("rg_kondisi_fik_komunal", "id", packageName)
        if (targetFisikId == 0) {
            targetFisikId = resources.getIdentifier("rg_kondisi_fifik_komunal", "id", packageName)
        }
        if (targetFisikId == 0) {
            targetFisikId = resources.getIdentifier("rg_kondisi_fisik_komunal", "id", packageName)
        }
        rgKondisiFisik = findViewById(targetFisikId)

        rgAirKomunal = findViewById(resources.getIdentifier("rg_air_komunal", "id", packageName))
        rgKelayakan = findViewById(resources.getIdentifier("rg_kelayakan", "id", packageName))

        btnSimpan = findViewById(resources.getIdentifier("btn_simpan", "id", packageName))
        mapView = findViewById(resources.getIdentifier("map_view", "id", packageName))

        isEditMode = intent.getBooleanExtra("IS_EDIT_MODE", false)
        idDataYangDiedit = intent.getIntExtra("EDIT_ID", 0)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        setupMapEngine()
        setupSpinnerDasar()
        setupLogikaWilayahKomunalBercabang()

        if (isEditMode && idDataYangDiedit != 0) {
            btnSimpan.text = "Update Perubahan Data"
            muatDataLamaUntukDiedit(idDataYangDiedit)
        }

        btnSimpan.setOnClickListener { saveData() }
        setupNavigation()
    }

    private fun setupMapEngine() {
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setBuiltInZoomControls(true)
        mapView.setMultiTouchControls(true)
        mapView.isTilesScaledToDpi = true

        val mapController = mapView.controller
        mapController.setZoom(16.0)

        val eventsReceiver = object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                updateMarkerPosition(p)
                return true
            }
            override fun longPressHelper(p: GeoPoint): Boolean = false
        }
        mapView.overlays.add(0, MapEventsOverlay(eventsReceiver))

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_CODE)
            val startPoint = GeoPoint(selectedLatitude, selectedLongitude)
            mapController.setCenter(startPoint)
            updateMarkerPosition(startPoint)
        } else {
            if (!isEditMode) {
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    if (location != null) {
                        selectedLatitude = location.latitude
                        selectedLongitude = location.longitude
                    }
                    val startPoint = GeoPoint(selectedLatitude, selectedLongitude)
                    mapController.setCenter(startPoint)
                    updateMarkerPosition(startPoint)
                }.addOnFailureListener {
                    val startPoint = GeoPoint(selectedLatitude, selectedLongitude)
                    mapController.setCenter(startPoint)
                    updateMarkerPosition(startPoint)
                }
            } else {
                val startPoint = GeoPoint(selectedLatitude, selectedLongitude)
                mapController.setCenter(startPoint)
                updateMarkerPosition(startPoint)
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (!isEditMode) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                        if (location != null) {
                            selectedLatitude = location.latitude
                            selectedLongitude = location.longitude
                            val newPoint = GeoPoint(selectedLatitude, selectedLongitude)
                            mapView.controller.setCenter(newPoint)
                            updateMarkerPosition(newPoint)
                        }
                    }
                }
            }
        }
    }

    private fun updateMarkerPosition(p: GeoPoint) {
        if (markerKomunal == null) {
            markerKomunal = Marker(mapView)
            markerKomunal?.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            mapView.overlays.add(markerKomunal)
        }
        selectedLatitude = p.latitude
        selectedLongitude = p.longitude
        markerKomunal?.position = p
        updateMarkerTitle()
        
        lacakNamaJalanDariKoordinat(p.latitude, p.longitude)
    }

    private fun lacakNamaJalanDariKoordinat(lat: Double, lon: Double) {
        Thread {
            try {
                val geocoder = Geocoder(this@PendataanKomunalActivity, Locale.getDefault())
                val addresses = geocoder.getFromLocation(lat, lon, 1)
                
                runOnUiThread {
                    if (!addresses.isNullOrEmpty()) {
                        val address = addresses[0]
                        var streetName = address.thoroughfare
                        
                        if (streetName.isNullOrEmpty() && address.maxAddressLineIndex >= 0) {
                            streetName = address.getAddressLine(0).split(",")[0]
                        }
                        if (!streetName.isNullOrEmpty()) {
                            val adapter = spinnerAlamatJalan.adapter
                            if (adapter is ArrayAdapter<*>) {
                                var foundIndex = -1
                                for (i in 0 until adapter.count) {
                                    if (adapter.getItem(i).toString().contains(streetName, ignoreCase = true)) {
                                        foundIndex = i
                                        break
                                    }
                                }
                                if (foundIndex != -1) {
                                    spinnerAlamatJalan.setSelection(foundIndex)
                                } else {
                                    (adapter as ArrayAdapter<String>).add(streetName)
                                    adapter.notifyDataSetChanged()
                                    spinnerAlamatJalan.setSelection(adapter.count - 1)
                                }
                            }
                        } else {
                            Toast.makeText(this@PendataanKomunalActivity, "Nama jalan tidak ditemukan di titik ini", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(this@PendataanKomunalActivity, "Gagal melacak jalan: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    private fun updateMarkerTitle() {
        val jalanTerpilih = spinnerAlamatJalan.selectedItem?.toString() ?: ""
        val namaLokasi = if (jalanTerpilih.startsWith("Pilih") || jalanTerpilih.isEmpty()) "Lokasi Komunal" else jalanTerpilih
        val jenisSarana = spinnerJenisKomunal.selectedItem?.toString() ?: "MCK"
        markerKomunal?.title = "$jenisSarana: $namaLokasi"
        markerKomunal?.showInfoWindow()
        mapView.invalidate()
    }

    private fun setupSpinnerDasar() {
        var daftarTampil = daftarKelurahan
        if (userRole.contains("petugas", ignoreCase = true) && userKelurahan.isNotEmpty()) {
            daftarTampil = arrayOf(userKelurahan)
            spinnerKelurahan.isEnabled = false
        }
        val adapter = ArrayAdapter(this, R.layout.spinner_item_hitam, daftarTampil)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerKelurahan.adapter = adapter

        val opsiJenis = arrayOf("MCK Umum Bersama", "Sistem IPAL Komunal", "Tangki Septik Kelompok")
        val jenisAdapter = ArrayAdapter(this, R.layout.spinner_item_hitam, opsiJenis)
        jenisAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerJenisKomunal.adapter = jenisAdapter

        val opsiAngka = Array(10) { i -> "${i + 1} Bilik" }
        val angkaAdapter = ArrayAdapter(this, R.layout.spinner_item_hitam, opsiAngka)
        angkaAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerBilikMandi.adapter = angkaAdapter
        spinnerJumlahKakus.adapter = angkaAdapter

        val opsiJarak = arrayOf(
            "Kurang dari 10 Meter (Risiko Tinggi)",
            "10 - 15 Meter (Kategori Aman)",
            "Lebih dari 15 Meter (Sangat Layak)"
        )
        val jarakAdapter = ArrayAdapter(this, R.layout.spinner_item_hitam, opsiJarak)
        jarakAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerJarakAirKomunal.adapter = jarakAdapter
    }

    private fun setupLogikaWilayahKomunalBercabang() {
        spinnerKelurahan.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val kelurahan = parent?.getItemAtPosition(position).toString()
                if (kelurahan == "Pilih Kelurahan") {
                    etKodePos.setText("")
                    return
                }

                val daftarJalan = LocationHelper.getJalanList(kelurahan)

                val finalJalanAdapter = ArrayAdapter(this@PendataanKomunalActivity, R.layout.spinner_item_hitam, daftarJalan)
                finalJalanAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinnerAlamatJalan.adapter = finalJalanAdapter

                spinnerAlamatJalan.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) { updateMarkerTitle() }
                    override fun onNothingSelected(p0: AdapterView<*>?) {}
                }

                val kodePosValue = when (kelurahan) {
                    "Mrican", "Dermo" -> "64111"
                    "Mojoroto", "Ngampel" -> "64112"
                    "Gayam" -> "64113"
                    "Sukorame", "Bandar Lor" -> "64114"
                    "Pojok" -> "64115"
                    "Campurejo" -> "64116"
                    "Lirboyo" -> "64117"
                    "Bandar Kidul" -> "64118"
                    "Bujel", "Banjarmlati", "Tamanan" -> "64119"
                    else -> "64100"
                }
                etKodePos.setText(kodePosValue)

                val maxRw = when (kelurahan) {
                    "Mojoroto" -> 12; "Pojok" -> 11
                    "Bandar Kidul", "Campurejo", "Lirboyo" -> 9
                    "Sukorame", "Bandar Lor" -> 8; "Banjarmlati" -> 7
                    "Mrican", "Bujel" -> 6; "Ngampel", "Tamanan" -> 5
                    "Dermo", "Gayam" -> 4; else -> 5
                }
                val listRw = ArrayList<String>()
                for (i in 1..maxRw) { listRw.add("RW %02d".format(i)) }
                val rwAdapter = ArrayAdapter(this@PendataanKomunalActivity, R.layout.spinner_item_hitam, listRw)
                rwAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinnerRw.adapter = rwAdapter

                val listRt = ArrayList<String>()
                for (i in 1..55) { listRt.add("RT %02d".format(i)) }
                val rtAdapter = ArrayAdapter(this@PendataanKomunalActivity, R.layout.spinner_item_hitam, listRt)
                rtAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinnerRt.adapter = rtAdapter
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun muatDataLamaUntukDiedit(idData: Int) {
        RetrofitClient.instance.getListKomunal().enqueue(object : Callback<KomunalListResponse> {
            override fun onResponse(call: Call<KomunalListResponse>, response: Response<KomunalListResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    val dataLama = response.body()!!.data.find { it.id == idData }
                    if (dataLama != null) {
                        
                        try {
                            val jsonStr = Gson().toJson(dataLama)
                            val jsonObj = Gson().fromJson(jsonStr, JsonObject::class.java)
                            val namaFas = jsonObj.get("nama_fasilitas")?.asString ?: ""
                            if (namaFas.isNotEmpty()) {
                                etNamaFasilitas.setText(namaFas)
                            }
                        } catch (e: Exception) {}

                        // 🚀 FIXED SINKRONISASI LOGIKA REFLEKSI DATA:
                        // Mengecek isi kelurahan langsung dari wrapper model minimalis buatan kita tanpa refleksi mentah
                        val namaKelurahanLama = try {
                            val jsonStr = Gson().toJson(dataLama)
                            val jsonObj = Gson().fromJson(jsonStr, JsonObject::class.java)
                            jsonObj.get("kelurahan")?.asString ?: ""
                        } catch (e: Exception) {
                            ""
                        }

                        val indexKel = daftarKelurahan.indexOf(namaKelurahanLama)
                        if (indexKel != -1) {
                            spinnerKelurahan.setSelection(indexKel)
                        }

                        Toast.makeText(this@PendataanKomunalActivity, "Data lama berhasil dimuat ke formulir!", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            override fun onFailure(call: Call<KomunalListResponse>, t: Throwable) {
                Toast.makeText(this@PendataanKomunalActivity, "Gagal memuat data lama dari server", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun saveData() {
        if (idUserAktif == 0) {
            idUserAktif = 1
        }

        val kelurahanTerpilih = spinnerKelurahan.selectedItem.toString()
        if (kelurahanTerpilih == "Pilih Kelurahan") {
            Toast.makeText(this, "Silakan pilih kelurahan terlebih dahulu!", Toast.LENGTH_SHORT).show()
            return
        }

        val jalanTerpilih = spinnerAlamatJalan.selectedItem?.toString() ?: ""
        val rtTerpilih = spinnerRt.selectedItem?.toString() ?: "RT 01"
        val rwTerpilih = spinnerRw.selectedItem?.toString() ?: "RW 01"
        val kodePosStr = etKodePos.text.toString()

        val bilikMandiStr = spinnerBilikMandi.selectedItem?.toString() ?: "1 Bilik"
        val jmlWcStr = spinnerJumlahKakus.selectedItem?.toString() ?: "1 Bilik"
        val jarakAirStr = spinnerJarakAirKomunal.selectedItem?.toString() ?: ""

        if (rgKelayakan.checkedRadioButtonId == -1 || rgKondisiFisik.checkedRadioButtonId == -1 || rgAirKomunal.checkedRadioButtonId == -1) {
            Toast.makeText(this, "Mohon lengkapi semua penilaian pilihan kondisi & kelayakan!", Toast.LENGTH_SHORT).show()
            return
        }

        val alamatFinalKomunal = "$jalanTerpilih, $rtTerpilih / $rwTerpilih, Kode Pos: $kodePosStr"

        val idFisik = rgKondisiFisik.checkedRadioButtonId
        val kondisiFisikTxt = findViewById<RadioButton>(idFisik).text.toString()

        val idAir = rgAirKomunal.checkedRadioButtonId
        val kondisiAirTxt = findViewById<RadioButton>(idAir).text.toString()

        val statusKelayakan = findViewById<RadioButton>(rgKelayakan.checkedRadioButtonId).text.toString()
        val jenisSaranaSistem = spinnerJenisKomunal.selectedItem.toString()
        val namaFasilitasInput = etNamaFasilitas.text.toString().trim()
        val namaFasilitasFinal = if (namaFasilitasInput.isNotEmpty()) namaFasilitasInput else "$jenisSaranaSistem ($jalanTerpilih)"

        RetrofitClient.instance.simpanKomunal(
            idUser = idUserAktif.toString(),
            kelurahan = kelurahanTerpilih,
            alamat = alamatFinalKomunal,
            rw = rwTerpilih,
            rt = rtTerpilih,
            kodePos = kodePosStr,
            namaFasilitas = namaFasilitasFinal,
            namaSistem = jenisSaranaSistem,
            bilikMandi = bilikMandiStr,
            jmlWc = jmlWcStr,
            jarakAir = jarakAirStr,
            kondisiFisik = kondisiFisikTxt,
            kondisiAir = kondisiAirTxt,
            statusKelayakan = statusKelayakan,
            latitude = selectedLatitude.toString(),
            longitude = selectedLongitude.toString()
        ).enqueue(object : Callback<ResponseModel> {
            override fun onResponse(call: Call<ResponseModel>, response: Response<ResponseModel>) {
                if (response.isSuccessful) {
                    val builder = android.app.AlertDialog.Builder(this@PendataanKomunalActivity)
                    builder.setTitle(if (isEditMode) "Pembaruan Sukses!" else "Penyimpanan Berhasil!")
                    builder.setMessage(response.body()?.message ?: "Data berhasil disinkronisasi ke Laragon.")
                    builder.setPositiveButton("OK") { _, _ ->
                        if (isEditMode) {
                            startActivity(Intent(this@PendataanKomunalActivity, RiwayatInputActivity::class.java))
                            finish()
                        } else {
                            etNamaFasilitas.setText("")
                            spinnerKelurahan.setSelection(0)
                            rgKelayakan.clearCheck()
                            rgKondisiFisik.clearCheck()
                            rgAirKomunal.clearCheck()
                        }
                    }
                    builder.show()
                } else {
                    val errorBodyText = response.errorBody()?.string() ?: "Eror Data"
                    val builder = android.app.AlertDialog.Builder(this@PendataanKomunalActivity)
                    builder.setTitle("Server Menolak (Code: ${response.code()})")
                    builder.setMessage(errorBodyText)
                    builder.setPositiveButton("OK", null)
                    builder.show()
                }
            }

            override fun onFailure(call: Call<ResponseModel>, t: Throwable) {
                val builder = android.app.AlertDialog.Builder(this@PendataanKomunalActivity)
                builder.setTitle("Koneksi Laragon Gagal")
                builder.setMessage(t.message)
                builder.setPositiveButton("OK", null)
                builder.show()
            }
        })
    }

    private fun setupNavigation() {
        val idNavRt = resources.getIdentifier("nav_rt", "id", packageName)
        val idNavSpal = resources.getIdentifier("nav_spal", "id", packageName)
        val idNavPelaporan = resources.getIdentifier("nav_pelaporan", "id", packageName)
        val idNavRiwayat = resources.getIdentifier("nav_riwayat", "id", packageName)

        if (idNavRt != 0) { findViewById<View>(idNavRt)?.setOnClickListener { startActivity(Intent(this, PendataanRumahTanggaActivity::class.java)); finish() } }
        if (idNavSpal != 0) { findViewById<View>(idNavSpal)?.setOnClickListener { startActivity(Intent(this, PendataanSpalActivity::class.java)); finish() } }
        if (idNavPelaporan != 0) { findViewById<View>(idNavPelaporan)?.setOnClickListener { startActivity(Intent(this, PelaporanActivity::class.java)); finish() } }
        if (idNavRiwayat != 0) { findViewById<View>(idNavRiwayat)?.setOnClickListener { startActivity(Intent(this, RiwayatInputActivity::class.java)); finish() } }
    }

    override fun onResume() { super.onResume(); mapView.onResume() }
    override fun onPause() { super.onPause(); mapView.onPause() }
}