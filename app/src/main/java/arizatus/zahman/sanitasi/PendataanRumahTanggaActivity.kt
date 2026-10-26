package arizatus.zahman.sanitasi

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import android.location.Geocoder
import java.util.Locale
import arizatus.zahman.sanitasi.api.ResponseModel
import arizatus.zahman.sanitasi.api.RetrofitClient
import arizatus.zahman.sanitasi.utils.LocationHelper
import com.google.gson.Gson
import com.google.gson.JsonObject
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import retrofit2.Call
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices

class PendataanRumahTanggaActivity : AppCompatActivity() {

    private lateinit var spinnerKelurahan: Spinner
    private lateinit var spinnerAlamatJalan: Spinner
    private lateinit var spinnerRt: Spinner
    private lateinit var spinnerRw: Spinner
    private lateinit var etKodePos: EditText

    private lateinit var etNoKk: EditText
    private lateinit var etNamaKepala: EditText

    private lateinit var spinnerJumlahPenghuni: Spinner
    private lateinit var spinnerJarakAir: Spinner

    private lateinit var rgKepemilikanJamban: RadioGroup
    private lateinit var rgJenisJamban: RadioGroup
    private lateinit var rgKelayakan: RadioGroup
    private lateinit var rgTempatBuangan: RadioGroup

    private lateinit var rgGreywater: RadioGroup

    private lateinit var rgPernahSedot: RadioGroup
    private lateinit var layoutDetailSedot: LinearLayout
    private lateinit var spinnerFrekuensiSedot: Spinner
    private lateinit var spinnerWaktuSedot: Spinner

    private lateinit var btnUploadDokumen: Button
    private lateinit var btnSimpan: Button

    private lateinit var mapView: MapView
    private var markerRumah: Marker? = null
    private val LOCATION_PERMISSION_CODE = 103
    private var selectedLatitude: Double = -7.816
    private var selectedLongitude: Double = 112.010

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private var idUserAktif: Int = 0
    private var userRole: String = ""
    private var userKelurahan: String = ""
    private var currentFileFoto: File? = null
    private val daftarBerkasFoto = ArrayList<File>()

    private var isEditMode: Boolean = false
    private var idDataYangDiedit: Int = 0

    private val daftarKelurahan = arrayOf(
        "Pilih Kelurahan", "Mojoroto", "Pojok", "Sukorame", "Bandar Lor",
        "Bandar Kidul", "Campurejo", "Lirboyo", "Mrican", "Dermo",
        "Ngampel", "Gayam", "Bujel", "Banjarmlati", "Tamanan"
    )

    private val opsiJumlahPenghuni = arrayOf(
        "Pilih Jumlah Penghuni", "1 - 2 Orang", "3 - 4 Orang", "5 Orang", "Lebih dari 5 Orang"
    )

    private val opsiJarakSumberAir = arrayOf(
        "Pilih Jarak Sumber Air", "< 10 Meter (Tidak Aman)", "≥ 10 Meter (Aman)"
    )

    private val ambilFotoKameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            currentFileFoto?.let {
                daftarBerkasFoto.add(it)
                Toast.makeText(this, "Foto KK/KTP berhasil terekam!", Toast.LENGTH_SHORT).show()
                btnUploadDokumen.text = "TERPILIH ${daftarBerkasFoto.size} FOTO"
            }
        }
    }

    private val ambilFotoGaleriLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val data = result.data
            if (data?.clipData != null) {
                val count = data.clipData!!.itemCount
                for (i in 0 until count) {
                    val uri = data.clipData!!.getItemAt(i).uri
                    daftarBerkasFoto.add(uriToFile(uri, this))
                }
                Toast.makeText(this, "$count foto dipilih dari galeri!", Toast.LENGTH_SHORT).show()
                btnUploadDokumen.text = "TERPILIH ${daftarBerkasFoto.size} FOTO"
            } else if (data?.data != null) {
                val uriTerpilih: Uri? = data.data
                uriTerpilih?.let { uri ->
                    daftarBerkasFoto.add(uriToFile(uri, this))
                    Toast.makeText(this, "Foto dokumen berhasil dipilih dari galeri!", Toast.LENGTH_SHORT).show()
                    btnUploadDokumen.text = "TERPILIH ${daftarBerkasFoto.size} FOTO"
                }
            }
        }
    }

    private val requestCameraPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) { eksekusiBukaKamera() } else { Toast.makeText(this, "Izin kamera ditolak!", Toast.LENGTH_LONG).show() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().load(this, getSharedPreferences("osmdroid", Context.MODE_PRIVATE))

        val resLayoutId = resources.getIdentifier("activity_pendataan_rt", "layout", packageName)
        if (resLayoutId != 0) {
            setContentView(resLayoutId)
        } else {
            finish()
            return
        }

        // Ambil Session Login Petugas
        val sharedPref = getSharedPreferences("SanitasiSession", Context.MODE_PRIVATE)
        idUserAktif = sharedPref.getInt("USER_ID", 0)
        userRole = sharedPref.getString("USER_ROLE", "") ?: ""
        userKelurahan = sharedPref.getString("USER_KELURAHAN", "") ?: ""

        // 🚀 ANTISIPASI: Jika Session 0, ambil cadangan dari id_user biasa, kalau tetap kosong paksa jadi 1
        if (idUserAktif == 0) {
            idUserAktif = sharedPref.getInt("id_user", 1)
        }

        spinnerKelurahan = findViewById(resources.getIdentifier("spinner_kelurahan", "id", packageName))
        spinnerAlamatJalan = findViewById(resources.getIdentifier("spinner_alamat_jalan", "id", packageName))
        spinnerRt = findViewById(resources.getIdentifier("spinner_rt", "id", packageName))
        spinnerRw = findViewById(resources.getIdentifier("spinner_rw", "id", packageName))
        etKodePos = findViewById(resources.getIdentifier("et_detail_no_rumah", "id", packageName))

        etNoKk = findViewById(resources.getIdentifier("et_no_kk", "id", packageName))
        etNamaKepala = findViewById(resources.getIdentifier("et_nama_kepala", "id", packageName))

        spinnerJumlahPenghuni = findViewById(resources.getIdentifier("spinner_jml_penghuni", "id", packageName))
        spinnerJarakAir = findViewById(resources.getIdentifier("spinner_jarak_air", "id", packageName))

        btnUploadDokumen = findViewById(resources.getIdentifier("btn_upload_dokumen", "id", packageName))
        rgKepemilikanJamban = findViewById(resources.getIdentifier("rg_kepemilikan_jamban", "id", packageName))
        rgJenisJamban = findViewById(resources.getIdentifier("rg_jenis_jamban", "id", packageName))
        rgTempatBuangan = findViewById(resources.getIdentifier("rg_tempat_buangan", "id", packageName))
        rgKelayakan = findViewById(resources.getIdentifier("rg_kelayakan", "id", packageName))

        rgGreywater = findViewById(resources.getIdentifier("rg_greywater", "id", packageName))
        rgPernahSedot = findViewById(resources.getIdentifier("rg_pernah_sedot", "id", packageName))
        layoutDetailSedot = findViewById(resources.getIdentifier("layout_detail_sedot", "id", packageName))
        spinnerFrekuensiSedot = findViewById(resources.getIdentifier("spinner_frekuensi_sedot", "id", packageName))
        spinnerWaktuSedot = findViewById(resources.getIdentifier("spinner_waktu_sedot", "id", packageName))

        btnSimpan = findViewById(resources.getIdentifier("btn_simpan", "id", packageName))
        mapView = findViewById(resources.getIdentifier("map_view", "id", packageName))

        isEditMode = intent.getBooleanExtra("IS_EDIT_MODE", false)
        idDataYangDiedit = intent.getIntExtra("EDIT_ID", 0)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        setupMapEngine()
        setupSpinnerKelurahan()
        setupLogikaWilayahBercabang()
        setupLogikaPenyedotanDinamis()
        setupDropdownKuesionerBaru()

        etNamaKepala.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { updateMarkerInfo() }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // Penanganan drag marker
        markerRumah?.setOnMarkerDragListener(object : Marker.OnMarkerDragListener {
            override fun onMarkerDrag(marker: Marker) {}
            override fun onMarkerDragEnd(marker: Marker) {
                placeMarker(marker.position)
            }
            override fun onMarkerDragStart(marker: Marker) {}
        })

        if (isEditMode && idDataYangDiedit != 0) {
            btnSimpan.text = "Update Perubahan RT"
            muatDataLamaUntukDieditRT(idDataYangDiedit)
        }

        btnUploadDokumen.setOnClickListener { tampilkanPilihanMedia() }
        btnSimpan.setOnClickListener { aksiSimpanRT() }
        setupNavigation()
    }

    private fun setupMapEngine() {
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setBuiltInZoomControls(false)
        mapView.setMultiTouchControls(true)
        mapView.isTilesScaledToDpi = true

        val mapController = mapView.controller
        mapController.setZoom(16.0)

        val eventsReceiver = object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                placeMarker(p)
                return true
            }
            override fun longPressHelper(p: GeoPoint): Boolean = false
        }
        mapView.overlays.add(0, MapEventsOverlay(eventsReceiver))

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_CODE)
            val startPoint = GeoPoint(selectedLatitude, selectedLongitude)
            mapController.setCenter(startPoint)
            placeMarker(startPoint)
        } else {
            if (!isEditMode) {
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    if (location != null) {
                        selectedLatitude = location.latitude
                        selectedLongitude = location.longitude
                    }
                    val startPoint = GeoPoint(selectedLatitude, selectedLongitude)
                    mapController.setCenter(startPoint)
                    placeMarker(startPoint)
                }.addOnFailureListener {
                    val startPoint = GeoPoint(selectedLatitude, selectedLongitude)
                    mapController.setCenter(startPoint)
                    placeMarker(startPoint)
                }
            } else {
                val startPoint = GeoPoint(selectedLatitude, selectedLongitude)
                mapController.setCenter(startPoint)
                placeMarker(startPoint)
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
                            placeMarker(newPoint)
                        }
                    }
                }
            }
        }
    }

    private fun placeMarker(p: GeoPoint) {
        if (markerRumah == null) {
            markerRumah = Marker(mapView)
            markerRumah?.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            mapView.overlays.add(markerRumah)
        }
        selectedLatitude = p.latitude
        selectedLongitude = p.longitude
        markerRumah?.position = p
        updateMarkerInfo()
        
        lacakNamaJalanDariKoordinat(p.latitude, p.longitude)
    }

    private fun lacakNamaJalanDariKoordinat(lat: Double, lon: Double) {
        Thread {
            try {
                val geocoder = Geocoder(this@PendataanRumahTanggaActivity, Locale.getDefault())
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
                            Toast.makeText(this@PendataanRumahTanggaActivity, "Nama jalan tidak ditemukan di titik ini", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(this@PendataanRumahTanggaActivity, "Gagal melacak jalan: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    private fun updateMarkerInfo() {
        val nama = etNamaKepala.text.toString().ifEmpty { "Rumah Tangga" }
        markerRumah?.title = "Lokasi: $nama"
        markerRumah?.showInfoWindow()
        mapView.invalidate()
    }

    private fun setupSpinnerKelurahan() {
        var daftarTampil = daftarKelurahan
        if (userRole.contains("petugas", ignoreCase = true) && userKelurahan.isNotEmpty()) {
            daftarTampil = arrayOf(userKelurahan)
            spinnerKelurahan.isEnabled = false
        }
        val adapter = ArrayAdapter(this, R.layout.spinner_item_hitam, daftarTampil)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerKelurahan.adapter = adapter
    }

    private fun setupDropdownKuesionerBaru() {
        val adapterJml = ArrayAdapter(this, R.layout.spinner_item_hitam, opsiJumlahPenghuni)
        adapterJml.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerJumlahPenghuni.adapter = adapterJml

        val adapterJarak = ArrayAdapter(this, R.layout.spinner_item_hitam, opsiJarakSumberAir)
        adapterJarak.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerJarakAir.adapter = adapterJarak
    }

    private fun setupLogikaWilayahBercabang() {
        spinnerKelurahan.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val kelurahan = parent?.getItemAtPosition(position).toString()
                if (kelurahan == "Pilih Kelurahan") {
                    etKodePos.setText("")
                    return
                }

                val daftarJalan = LocationHelper.getJalanList(kelurahan)

                val jalanAdapter = ArrayAdapter(this@PendataanRumahTanggaActivity, R.layout.spinner_item_hitam, daftarJalan)
                jalanAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinnerAlamatJalan.adapter = jalanAdapter

                val kodePosValue = LocationHelper.getKodePos(kelurahan)
                etKodePos.setText(kodePosValue)

                val maxRw = LocationHelper.getMaxRw(kelurahan)

                val listRw = ArrayList<String>()
                for (i in 1..maxRw) { listRw.add("RW %02d".format(i)) }
                val rwAdapter = ArrayAdapter(this@PendataanRumahTanggaActivity, R.layout.spinner_item_hitam, listRw)
                rwAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinnerRw.adapter = rwAdapter

                val listRt = ArrayList<String>()
                for (i in 1..55) { listRt.add("RT %02d".format(i)) }
                val rtAdapter = ArrayAdapter(this@PendataanRumahTanggaActivity, R.layout.spinner_item_hitam, listRt)
                rtAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinnerRt.adapter = rtAdapter
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupLogikaPenyedotanDinamis() {
        val opsiFrekuensi = arrayOf("1 Kali", "2 Kali", "3 Kali", "4 Kali", "Lebih dari 4 Kali")
        val frekuensiAdapter = ArrayAdapter(this, R.layout.spinner_item_hitam, opsiFrekuensi)
        frekuensiAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerFrekuensiSedot.adapter = frekuensiAdapter

        val opsiWaktu = arrayOf("Kurang dari 3 tahun lalu", "3 tahun yang lalu", "4 tahun yang lalu", "5 tahun yang lalu", "Lebih dari 5 tahun lalu")
        val waktuAdapter = ArrayAdapter(this, R.layout.spinner_item_hitam, opsiWaktu)
        waktuAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerWaktuSedot.adapter = waktuAdapter

        rgPernahSedot.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId == R.id.rb_sedot_pernah) {
                layoutDetailSedot.visibility = View.VISIBLE
            } else {
                layoutDetailSedot.visibility = View.GONE
            }
        }
    }

    private fun muatDataLamaUntukDieditRT(idData: Int) {
        RetrofitClient.instance.getRiwayatPelaporan(idUserAktif).enqueue(object : retrofit2.Callback<JsonObject> {
            override fun onResponse(call: retrofit2.Call<JsonObject>, response: retrofit2.Response<JsonObject>) {
                if (response.isSuccessful && response.body() != null) {
                    try {
                        val jsonBody = response.body()!!
                        val dataArray = jsonBody.getAsJsonArray("data")
                        val dataLama = dataArray?.find { it.asJsonObject.get("id")?.asInt == idData }?.asJsonObject

                        if (dataLama != null) {
                            val namaKelurahanLama = dataLama.get("kelurahan")?.asString ?: ""
                            val indexKel = daftarKelurahan.indexOf(namaKelurahanLama)
                            if (indexKel != -1) {
                                spinnerKelurahan.setSelection(indexKel)
                            }

                            etNoKk.setText(dataLama.get("no_kk")?.asString ?: "")
                            etNamaKepala.setText(dataLama.get("kepala_keluarga")?.asString ?: "")

                            val latRaw = dataLama.get("latitude")?.asString?.toDoubleOrNull() ?: -7.816
                            val lonRaw = dataLama.get("longitude")?.asString?.toDoubleOrNull() ?: 112.010

                            val titikLama = GeoPoint(latRaw, lonRaw)
                            mapView.controller.setCenter(titikLama)
                            placeMarker(titikLama)

                            Toast.makeText(this@PendataanRumahTanggaActivity, "Formulir data Rumah Tangga sukses di-load!", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) { e.printStackTrace() }
                }
            }
            override fun onFailure(call: retrofit2.Call<JsonObject>, t: Throwable) {
                Toast.makeText(this@PendataanRumahTanggaActivity, "Gagal memuat data lama dari server", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun tampilkanPilihanMedia() {
        val opsi = arrayOf("Ambil Foto Via Kamera HP", "Pilih Dari Galeri File")
        val builder = android.app.AlertDialog.Builder(this)
        builder.setTitle("Upload Bukti KK/KTP Warga")
        builder.setItems(opsi) { _, yangDipilih ->
            if (yangDipilih == 0) maxIzinDanBukaKamera() else bukaGaleri()
        }
        builder.show()
    }

    private fun maxIzinDanBukaKamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            eksekusiBukaKamera()
        } else {
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun eksekusiBukaKamera() {
        val intentKamera = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        val namaFile = "FOTO_RT_${System.currentTimeMillis()}.jpg"
        val direktoriPenyimpanan = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        currentFileFoto = File.createTempFile(namaFile, ".jpg", direktoriPenyimpanan)

        val uriFoto = FileProvider.getUriForFile(this, "${packageName}.fileprovider", currentFileFoto!!)
        intentKamera.putExtra(MediaStore.EXTRA_OUTPUT, uriFoto)
        ambilFotoKameraLauncher.launch(intentKamera)
    }

    private fun bukaGaleri() {
        val intentGaleri = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intentGaleri.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        ambilFotoGaleriLauncher.launch(intentGaleri)
    }

    private fun uriToFile(selectedImgUri: Uri, context: Context): File {
        val isiContentResolver = context.contentResolver
        val fileKustom = File.createTempFile("SRV_", ".jpg", context.getExternalFilesDir(Environment.DIRECTORY_PICTURES))
        val inputStream = isiContentResolver.openInputStream(selectedImgUri) ?: return fileKustom
        val outputStream = FileOutputStream(fileKustom)
        val buffer = ByteArray(1024)
        var panjangData: Int
        while (inputStream.read(buffer).also { panjangData = it } > 0) {
            outputStream.write(buffer, 0, panjangData)
        }
        outputStream.close()
        inputStream.close()
        return fileKustom
    }

    private fun createPartFromString(stringData: String?): RequestBody {
        val dataBersih = stringData ?: ""
        val mediaType = "text/plain".toMediaTypeOrNull()
        return dataBersih.toRequestBody(mediaType)
    }

    private fun aksiSimpanRT() {
        val idUserString = if (idUserAktif == 0) "1" else idUserAktif.toString()

        val noKkStr = etNoKk.text.toString().trim()
        val nama = etNamaKepala.text.toString().trim()

        val jmlStr = spinnerJumlahPenghuni.selectedItem?.toString() ?: ""
        val jarakStr = spinnerJarakAir.selectedItem?.toString() ?: ""

        val kelurahanTerpilih = spinnerKelurahan.selectedItem?.toString() ?: ""
        val jalanTerpilih = spinnerAlamatJalan.selectedItem?.toString() ?: ""
        val rtTerpilih = spinnerRt.selectedItem?.toString() ?: "RT 01"
        val rwTerpilih = spinnerRw.selectedItem?.toString() ?: "RW 01"
        val kodePosStr = etKodePos.text.toString().trim()

        if (kelurahanTerpilih.isEmpty() || kelurahanTerpilih == "Pilih Kelurahan") {
            Toast.makeText(this, "Silakan pilih kelurahan terlebih dahulu!", Toast.LENGTH_SHORT).show()
            return
        }

        if (jmlStr == "Pilih Jumlah Penghuni" || jarakStr == "Pilih Jarak Sumber Air") {
            Toast.makeText(this, "Harap lengkapi pilihan Jumlah Penghuni dan Jarak Air!", Toast.LENGTH_SHORT).show()
            return
        }

        if (noKkStr.isEmpty() || nama.isEmpty()) {
            Toast.makeText(this, "Semua data identitas wajib diisi!", Toast.LENGTH_SHORT).show()
            return
        }

        if (noKkStr.length != 16) {
            Toast.makeText(this, "No. KK harus berjumlah 16 digit angka!", Toast.LENGTH_SHORT).show()
            return
        }

        if (nama.length < 3) {
            Toast.makeText(this, "Nama Kepala Keluarga tidak valid (minimal 3 karakter)!", Toast.LENGTH_SHORT).show()
            return
        }

        val alamatFinal = "$jalanTerpilih, $rtTerpilih / $rwTerpilih, Kode Pos: $kodePosStr"

        val idGreywater = rgGreywater.checkedRadioButtonId
        val greywaterValue = if (idGreywater != -1) {
            findViewById<RadioButton>(idGreywater)?.text?.toString()?.trim() ?: "Saluran Tertutup"
        } else { "Saluran Tertutup" }

        val idPernahSedot = rgPernahSedot.checkedRadioButtonId
        val statusSedotText = if (idPernahSedot == R.id.rb_sedot_pernah) {
            val frekuensi = spinnerFrekuensiSedot.selectedItem?.toString() ?: "1 Kali"
            val waktu = spinnerWaktuSedot.selectedItem?.toString() ?: "Kurang dari 3 tahun lalu"
            "Pernah disedot ($frekuensi), Waktu Terakhir: $waktu"
        } else { "Belum Pernah Disedot" }

        val idKepemilikan = rgKepemilikanJamban.checkedRadioButtonId
        val kepemilikanJambanValue = if (idKepemilikan != -1) {
            findViewById<RadioButton>(idKepemilikan)?.text?.toString()?.trim()?.lowercase() ?: "sendiri"
        } else { "sendiri" }

        val idJenisJamban = rgJenisJamban.checkedRadioButtonId
        val jenisJambanValue = if (idJenisJamban != -1) {
            findViewById<RadioButton>(idJenisJamban)?.text?.toString()?.trim()?.lowercase() ?: "leher angsa"
        } else { "leher angsa" }

        val idKelayakan = rgKelayakan.checkedRadioButtonId
        val statusKelayakanValue = if (idKelayakan != -1) {
            findViewById<RadioButton>(idKelayakan)?.text?.toString()?.trim() ?: "Layak"
        } else { "Layak" }

        val idTempatBuangan = rgTempatBuangan.checkedRadioButtonId
        val tempatBuanganValue = if (idTempatBuangan != -1) {
            val teksTombol = findViewById<RadioButton>(idTempatBuangan)?.text?.toString()?.trim() ?: ""
            when {
                teksTombol.contains("Tangki Septic", ignoreCase = true) -> "tangki septic"
                teksTombol.contains("IPAL", ignoreCase = true) -> "ipal/spal"
                teksTombol.contains("Lubang tanah", ignoreCase = true) -> "lubang tanah"
                teksTombol.contains("Selokan", ignoreCase = true) -> "selokan"
                else -> "tangki septic"
            }
        } else { "tangki septic" }

        val bagianFotoList = ArrayList<MultipartBody.Part>()
        for (file in daftarBerkasFoto) {
            if (file.exists()) {
                val requestFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
                val part = MultipartBody.Part.createFormData("foto_dokumen[]", file.name, requestFile)
                bagianFotoList.add(part)
            }
        }

        val latString = selectedLatitude.toString()
        val lonString = selectedLongitude.toString()

        RetrofitClient.instance.simpanDataRT(
            idUser = createPartFromString(idUserString),
            kelurahan = createPartFromString(kelurahanTerpilih),
            alamat = createPartFromString(alamatFinal),
            noKk = createPartFromString(noKkStr),
            kepalaKeluarga = createPartFromString(nama),
            jmlPenghuni = createPartFromString(jmlStr),
            kepemilikanJamban = createPartFromString(kepemilikanJambanValue),
            jenisJamban = createPartFromString(jenisJambanValue),
            statusKelayakan = createPartFromString(statusKelayakanValue),
            pembuangan = createPartFromString(tempatBuanganValue),
            riwayatSedot = createPartFromString(statusSedotText),
            greywater = createPartFromString(greywaterValue),
            jarakAir = createPartFromString(jarakStr),
            longitude = createPartFromString(lonString),
            latitude = createPartFromString(latString),
            foto_dokumen = if (bagianFotoList.isEmpty()) null else bagianFotoList
        ).enqueue(object : retrofit2.Callback<ResponseModel> {
            override fun onResponse(call: Call<ResponseModel>, response: Response<ResponseModel>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@PendataanRumahTanggaActivity, if (isEditMode) "Data Rumah Tangga Sukses Diperbarui!" else "Data & Dokumen Rumah Tangga Berhasil Disimpan!", Toast.LENGTH_SHORT).show()

                    if (isEditMode) {
                        startActivity(Intent(this@PendataanRumahTanggaActivity, RiwayatInputActivity::class.java))
                        finish()
                    } else {
                        etNoKk.text.clear()
                        etNamaKepala.text.clear()
                        spinnerKelurahan.setSelection(0)
                        spinnerJumlahPenghuni.setSelection(0)
                        spinnerJarakAir.setSelection(0)
                        rgKepemilikanJamban.clearCheck()
                        rgJenisJamban.clearCheck()
                        rgKelayakan.clearCheck()
                        rgTempatBuangan.clearCheck()
                        rgGreywater.clearCheck()
                        rgPernahSedot.clearCheck()
                        layoutDetailSedot.visibility = View.GONE
                        daftarBerkasFoto.clear()
                        btnUploadDokumen.text = "UPLOAD BUKTI KK / KTP"
                    }
                } else {
                    val errorCode = response.code()
                    val errorBodyString = response.errorBody()?.string() ?: "Detail eror kosong."

                    android.app.AlertDialog.Builder(this@PendataanRumahTanggaActivity)
                        .setTitle("Validasi Gagal (Code: $errorCode)")
                        .setMessage(errorBodyString)
                        .setPositiveButton("TUTUP", null)
                        .show()
                }
            }
            override fun onFailure(call: Call<ResponseModel>, t: Throwable) {
                Toast.makeText(this@PendataanRumahTanggaActivity, "Kendala Jaringan API: ${t.message}", Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun String.toDoubleOrNull(): Double? = try { this.toDouble() } catch (e: NumberFormatException) { null }

    private fun setupNavigation() {
        findViewById<LinearLayout>(R.id.nav_rt)?.setOnClickListener { }
        findViewById<LinearLayout>(R.id.nav_komunal)?.setOnClickListener {
            startActivity(Intent(this, PendataanKomunalActivity::class.java))
            finish()
        }
        findViewById<LinearLayout>(R.id.nav_spal)?.setOnClickListener {
            startActivity(Intent(this, PendataanSpalActivity::class.java))
            finish()
        }
        findViewById<LinearLayout>(R.id.nav_pelaporan)?.setOnClickListener {
            startActivity(Intent(this, PelaporanActivity::class.java))
            finish()
        }
        findViewById<LinearLayout>(R.id.nav_riwayat)?.setOnClickListener {
            startActivity(Intent(this, RiwayatInputActivity::class.java))
            finish()
        }
    }

    override fun onResume() { super.onResume(); mapView.onResume() }
    override fun onPause() { super.onPause(); mapView.onPause() }
}