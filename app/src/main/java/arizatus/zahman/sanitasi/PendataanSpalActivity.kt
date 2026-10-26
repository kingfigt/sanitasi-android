package arizatus.zahman.sanitasi

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import android.location.Geocoder
import arizatus.zahman.sanitasi.api.ResponseModel
import arizatus.zahman.sanitasi.api.RetrofitClient
import arizatus.zahman.sanitasi.utils.LocationHelper
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
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody

class PendataanSpalActivity : AppCompatActivity() {

    // Komponen Input
    private lateinit var etNoKK: EditText
    private lateinit var etNamaKepala: EditText
    private lateinit var rgStatusSpal: RadioGroup
    private lateinit var rgKondisiBak: RadioGroup
    private lateinit var btnSimpan: Button

    // Komponen Wilayah (Spinner)
    private lateinit var spinnerKelurahan: Spinner
    private lateinit var spinnerJalan: Spinner
    private lateinit var spinnerRt: Spinner
    private lateinit var spinnerRw: Spinner

    // Komponen Upload Foto & Peta
    private lateinit var btnUploadDokumen: Button
    private lateinit var tvNamaFile: TextView
    private lateinit var ivPreviewDokumen: ImageView
    private lateinit var mapView: MapView
    private var mainMarker: Marker? = null

    private var idUserAktif: Int = 0
    private var userRole: String = ""
    private var userKelurahan: String = ""
    private var fotoUri: Uri? = null
    private var currentPhotoPath: String? = null
    private val daftarBerkasFoto = ArrayList<File>()

    // Variabel Koordinat Geotagging (Default Kediri Mojoroto)
    private var selectedLatitude: Double = -7.8116
    private var selectedLongitude: Double = 112.0068
    private val LOCATION_PERMISSION_CODE = 105

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    // Variabel Kontrol Mode Edit Data
    private var isEditMode: Boolean = false
    private var idDataYangDiedit: Int = 0

    private val listKelurahan = arrayOf(
        "Pilih Kelurahan", "Bandar Kidul", "Bandar Lor", "Banjarmlati", "Bujel", "Campurejo",
        "Dermo", "Gayam", "Lirboyo", "Mojoroto", "Mrican", "Ngampel", "Pojok", "Sukorame", "Tamanan"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Configuration.getInstance().load(this, getSharedPreferences("osmdroid", Context.MODE_PRIVATE))

        val resLayoutId = resources.getIdentifier("activity_pendataan_spal", "layout", packageName)
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

        // Inisialisasi Komponen Form via Reflection
        etNoKK = findViewById(resources.getIdentifier("etNoKKSpal", "id", packageName))
        etNamaKepala = findViewById(resources.getIdentifier("etNamaKepalaSpal", "id", packageName))
        rgStatusSpal = findViewById(resources.getIdentifier("rgStatusSpal", "id", packageName))
        rgKondisiBak = findViewById(resources.getIdentifier("rgKondisiBak", "id", packageName))
        btnSimpan = findViewById(resources.getIdentifier("btnSimpanSpal", "id", packageName))

        etNoKK.setTextColor(Color.parseColor("#000000"))
        etNoKK.setHintTextColor(Color.parseColor("#808080"))
        etNoKK.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD))

        etNamaKepala.setTextColor(Color.parseColor("#000000"))
        etNamaKepala.setHintTextColor(Color.parseColor("#808080"))
        etNamaKepala.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD))

        spinnerKelurahan = findViewById(resources.getIdentifier("spinner_kelurahan_spal", "id", packageName))
        spinnerJalan = findViewById(resources.getIdentifier("spinner_alamat_jalan_spal", "id", packageName))
        spinnerRt = findViewById(resources.getIdentifier("spinner_rt_spal", "id", packageName))
        spinnerRw = findViewById(resources.getIdentifier("spinner_rw_spal", "id", packageName))
        setupSpinnerData()

        btnUploadDokumen = findViewById(resources.getIdentifier("btn_upload_dokumen_spal", "id", packageName))
        tvNamaFile = findViewById(resources.getIdentifier("tv_nama_file_spal", "id", packageName))
        ivPreviewDokumen = findViewById(resources.getIdentifier("iv_preview_dokumen_spal", "id", packageName))

        btnUploadDokumen.setOnClickListener { tampilkanPilihanMedia() }

        // Inisialisasi & Logika Peta
        mapView = findViewById(resources.getIdentifier("mapOSMSpal", "id", packageName))
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setBuiltInZoomControls(true)
        mapView.setMultiTouchControls(true)

        val mapController = mapView.controller
        mapController.setZoom(16.0)

        isEditMode = intent.getBooleanExtra("IS_EDIT_MODE", false)
        idDataYangDiedit = intent.getIntExtra("EDIT_ID", 0)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Setup Marker Utama
        mainMarker = Marker(mapView).apply {
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            title = "Titik Manhole SPAL"
            isDraggable = true
        }
        mapView.overlays.add(mainMarker)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_CODE)
            val startPoint = GeoPoint(selectedLatitude, selectedLongitude)
            mapController.setCenter(startPoint)
            updateMarkerLocation(selectedLatitude, selectedLongitude)
        } else {
            if (!isEditMode) {
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    if (location != null) {
                        selectedLatitude = location.latitude
                        selectedLongitude = location.longitude
                    }
                    val startPoint = GeoPoint(selectedLatitude, selectedLongitude)
                    mapController.setCenter(startPoint)
                    updateMarkerLocation(selectedLatitude, selectedLongitude)
                }.addOnFailureListener {
                    val startPoint = GeoPoint(selectedLatitude, selectedLongitude)
                    mapController.setCenter(startPoint)
                    updateMarkerLocation(selectedLatitude, selectedLongitude)
                }
            } else {
                val startPoint = GeoPoint(selectedLatitude, selectedLongitude)
                mapController.setCenter(startPoint)
                updateMarkerLocation(selectedLatitude, selectedLongitude)
            }
        }

        // Logika ketukan peta
        val mapEventsReceiver = object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                updateMarkerLocation(p.latitude, p.longitude)
                return true
            }
            override fun longPressHelper(p: GeoPoint): Boolean { return false }
        }
        val mapEventsOverlay = MapEventsOverlay(mapEventsReceiver)
        mapView.overlays.add(0, mapEventsOverlay)

        // Penanganan drag marker
        mainMarker?.setOnMarkerDragListener(object : Marker.OnMarkerDragListener {
            override fun onMarkerDrag(marker: Marker) {}
            override fun onMarkerDragEnd(marker: Marker) {
                updateMarkerLocation(marker.position.latitude, marker.position.longitude)
            }
            override fun onMarkerDragStart(marker: Marker) {}
        })

        if (isEditMode && idDataYangDiedit != 0) {
            btnSimpan.text = "Update Perubahan Data SPAL"
            muatDataLamaUntukDieditSPAL(idDataYangDiedit)
        }

        btnSimpan.setOnClickListener { simpanDataSpalKeServer() }
        setupNavigation()
    }

    private fun updateMarkerLocation(lat: Double, lon: Double) {
        selectedLatitude = lat
        selectedLongitude = lon
        val newPoint = GeoPoint(lat, lon)
        mainMarker?.position = newPoint
        mapView.invalidate()
        
        lacakNamaJalanDariKoordinat(lat, lon)
    }

    private fun lacakNamaJalanDariKoordinat(lat: Double, lon: Double) {
        Thread {
            try {
                val geocoder = Geocoder(this@PendataanSpalActivity, Locale.getDefault())
                val addresses = geocoder.getFromLocation(lat, lon, 1)
                
                runOnUiThread {
                    if (!addresses.isNullOrEmpty()) {
                        val address = addresses[0]
                        var streetName = address.thoroughfare
                        
                        if (streetName.isNullOrEmpty() && address.maxAddressLineIndex >= 0) {
                            streetName = address.getAddressLine(0).split(",")[0]
                        }
                        if (!streetName.isNullOrEmpty()) {
                            val adapter = spinnerJalan.adapter
                            if (adapter is ArrayAdapter<*>) {
                                var foundIndex = -1
                                for (i in 0 until adapter.count) {
                                    if (adapter.getItem(i).toString().contains(streetName, ignoreCase = true)) {
                                        foundIndex = i
                                        break
                                    }
                                }
                                if (foundIndex != -1) {
                                    spinnerJalan.setSelection(foundIndex)
                                } else {
                                    (adapter as ArrayAdapter<String>).add(streetName)
                                    adapter.notifyDataSetChanged()
                                    spinnerJalan.setSelection(adapter.count - 1)
                                }
                            }
                        } else {
                            Toast.makeText(this@PendataanSpalActivity, "Nama jalan tidak ditemukan di titik ini", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(this@PendataanSpalActivity, "Gagal melacak jalan: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
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
                            updateMarkerLocation(selectedLatitude, selectedLongitude)
                        }
                    }
                }
            }
        }
    }

    private fun muatDataLamaUntukDieditSPAL(idData: Int) {
        RetrofitClient.instance.getRiwayatPelaporan(idUserAktif).enqueue(object : Callback<JsonObject> {
            override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                if (response.isSuccessful && response.body() != null) {
                    try {
                        val jsonBody = response.body()!!
                        val dataArray = jsonBody.getAsJsonArray("data")
                        val dataLama = dataArray?.find { it.asJsonObject.get("id")?.asInt == idData }?.asJsonObject

                        if (dataLama != null) {
                            val kelurahanLama = dataLama.get("kelurahan")?.asString ?: ""
                            val indexKel = listKelurahan.indexOfFirst { it.equals(kelurahanLama, ignoreCase = true) }
                            if (indexKel != -1) {
                                spinnerKelurahan.setSelection(indexKel)
                            }

                            etNoKK.setText(dataLama.get("no_kk")?.asString ?: "")
                            etNamaKepala.setText(dataLama.get("kepala_keluarga")?.asString ?: dataLama.get("nama_kepala")?.asString ?: "")

                            val latRaw = dataLama.get("latitude")?.asString?.toDoubleOrNull() ?: -7.8116
                            val lonRaw = dataLama.get("longitude")?.asString?.toDoubleOrNull() ?: 112.0068

                            updateMarkerLocation(latRaw, lonRaw)
                            mapView.controller.setCenter(GeoPoint(latRaw, lonRaw))

                            Toast.makeText(this@PendataanSpalActivity, "Formulir data SPAL sukses dipulihkan!", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) { e.printStackTrace() }
                }
            }
            override fun onFailure(call: Call<JsonObject>, t: Throwable) {
                Toast.makeText(this@PendataanSpalActivity, "Gagal memuat data lama dari server", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun tampilkanPilihanMedia() {
        val opsi = arrayOf("Ambil Foto Lewat Kamera", "Pilih dari Galeri Foto")
        AlertDialog.Builder(this)
            .setTitle("Pilih Dokumen Pendukung")
            .setItems(opsi) { dialog, item ->
                when (item) {
                    0 -> bukaKameraSistem()
                    1 -> bukaGaleriSistem()
                }
                dialog.dismiss()
            }.show()
    }

    private fun bukaGaleriSistem() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        galleryLauncher.launch(intent)
    }

    private fun bukaKameraSistem() {
        val intentKamera = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        val fileFoto: File? = try { buatFileGambarSpesifik() } catch (ex: Exception) { null }
        fileFoto?.let {
            fotoUri = FileProvider.getUriForFile(this, "$packageName.fileprovider", it)
            intentKamera.putExtra(MediaStore.EXTRA_OUTPUT, fotoUri)
            cameraLauncher.launch(intentKamera)
        }
    }

    private fun buatFileGambarSpesifik(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile("SPAL_${timeStamp}_", ".jpg", storageDir).apply {
            currentPhotoPath = absolutePath
        }
    }

    private fun uriToFile(selectedImgUri: Uri, context: Context): File {
        val fileKustom = File.createTempFile("SPAL_GAL_", ".jpg", context.getExternalFilesDir(Environment.DIRECTORY_PICTURES))
        val inputStream = context.contentResolver.openInputStream(selectedImgUri) ?: return fileKustom
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

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val data = result.data
            if (data?.clipData != null) {
                val count = data.clipData!!.itemCount
                for (i in 0 until count) {
                    val uri = data.clipData!!.getItemAt(i).uri
                    daftarBerkasFoto.add(uriToFile(uri, this))
                }
                tvNamaFile.text = "Terpilih ${daftarBerkasFoto.size} Foto dari Galeri"
                ivPreviewDokumen.visibility = View.VISIBLE
                ivPreviewDokumen.setImageURI(data.clipData!!.getItemAt(0).uri)
            } else if (data?.data != null) {
                val uriTerpilih = data.data
                uriTerpilih?.let {
                    daftarBerkasFoto.add(uriToFile(it, this))
                    tvNamaFile.text = "Terpilih ${daftarBerkasFoto.size} Foto dari Galeri"
                    ivPreviewDokumen.visibility = View.VISIBLE
                    ivPreviewDokumen.setImageURI(it)
                }
            }
        }
    }

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            currentPhotoPath?.let { path ->
                val file = File(path)
                if (file.exists()) {
                    daftarBerkasFoto.add(file)
                }
            }
            tvNamaFile.text = "Terpilih ${daftarBerkasFoto.size} Foto dari Kamera"
            ivPreviewDokumen.visibility = View.VISIBLE
            ivPreviewDokumen.setImageURI(fotoUri)
        }
    }

    private fun setupSpinnerData() {
        var daftarTampil = listKelurahan
        if (userRole.contains("petugas", ignoreCase = true) && userKelurahan.isNotEmpty()) {
            daftarTampil = arrayOf(userKelurahan)
            spinnerKelurahan.isEnabled = false
            updateDropdownJalan(userKelurahan)
            updateDropdownRtRw(userKelurahan)
        }

        val adapterKelurahan = ArrayAdapter(this, R.layout.spinner_item_hitam, daftarTampil)
        adapterKelurahan.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerKelurahan.adapter = adapterKelurahan

        spinnerKelurahan.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val kelurahanTerpilih = parent?.getItemAtPosition(position).toString()
                updateDropdownJalan(kelurahanTerpilih)
                updateDropdownRtRw(kelurahanTerpilih)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun updateDropdownJalan(kelurahan: String) {
        val listJalan = LocationHelper.getJalanList(kelurahan)
        
        val adapterJalan = ArrayAdapter(this, R.layout.spinner_item_hitam, listJalan)
        adapterJalan.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerJalan.adapter = adapterJalan
    }

    private fun updateDropdownRtRw(kelurahan: String) {
        if (kelurahan == "Pilih Kelurahan") {
            val defaultArray = arrayOf("00")
            spinnerRt.adapter = ArrayAdapter(this, R.layout.spinner_item_hitam, defaultArray).apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
            spinnerRw.adapter = ArrayAdapter(this, R.layout.spinner_item_hitam, defaultArray).apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
            return
        }

        val maxRw = when (kelurahan) {
            "Pojok" -> 12; "Bandar Lor" -> 11; "Mojoroto" -> 10
            "Campurejo", "Lirboyo" -> 9; "Bandar Kidul" -> 8; "Sukorame" -> 7
            "Mrican" -> 6; else -> 5
        }
        val maxRt = 55

        val listRw = (1..maxRw).map { String.format("%02d", it) }.toTypedArray()
        val listRt = (1..maxRt).map { String.format("%02d", it) }.toTypedArray()

        val adapterRw = ArrayAdapter(this, R.layout.spinner_item_hitam, listRw)
        adapterRw.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerRw.adapter = adapterRw

        val adapterRt = ArrayAdapter(this, R.layout.spinner_item_hitam, listRt)
        adapterRt.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerRt.adapter = adapterRt
    }

    private fun simpanDataSpalKeServer() {
        if (idUserAktif == 0) {
            idUserAktif = 1
        }

        val noKk = etNoKK.text.toString().trim()
        val nama = etNamaKepala.text.toString().trim()

        val kelurahan = spinnerKelurahan.selectedItem.toString()
        val jalan = spinnerJalan.selectedItem.toString()
        val rt = spinnerRt.selectedItem.toString()
        val rw = spinnerRw.selectedItem.toString()

        if (noKk.isEmpty() || nama.isEmpty() ||
            kelurahan == "Pilih Kelurahan" || jalan == "Pilih Jalan" ||
            rgStatusSpal.checkedRadioButtonId == -1 || rgKondisiBak.checkedRadioButtonId == -1) {
            Toast.makeText(this, "Mohon lengkapi seluruh formulir data SPAL Wilayah!", Toast.LENGTH_SHORT).show()
            return
        }

        if (noKk.length != 16) {
            Toast.makeText(this, "No. KK harus berjumlah 16 digit angka!", Toast.LENGTH_SHORT).show()
            return
        }

        if (nama.length < 3) {
            Toast.makeText(this, "Nama Kepala Keluarga tidak valid (minimal 3 karakter)!", Toast.LENGTH_SHORT).show()
            return
        }

        val statusSpal = findViewById<RadioButton>(rgStatusSpal.checkedRadioButtonId).text.toString()
        val kondisiBak = findViewById<RadioButton>(rgKondisiBak.checkedRadioButtonId).text.toString()
        val alamatLengkapGabungan = "$jalan, RT $rt / RW $rw"

        val bagianFotoList = ArrayList<MultipartBody.Part>()
        for (file in daftarBerkasFoto) {
            if (file.exists()) {
                val requestFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
                val part = MultipartBody.Part.createFormData("foto_dokumen[]", file.name, requestFile)
                bagianFotoList.add(part)
            }
        }

        RetrofitClient.instance.simpanSpal(
            idUser = createPartFromString(idUserAktif.toString()),
            noKk = createPartFromString(noKk),
            namaKepala = createPartFromString(nama),
            kelurahan = createPartFromString(kelurahan),
            alamat = createPartFromString(alamatLengkapGabungan),
            statusSpal = createPartFromString(statusSpal),
            kondisiBak = createPartFromString(kondisiBak),
            latitude = createPartFromString(selectedLatitude.toString()),
            longitude = createPartFromString(selectedLongitude.toString()),
            foto_dokumen = if (bagianFotoList.isEmpty()) null else bagianFotoList
        ).enqueue(object : Callback<ResponseModel> {
            override fun onResponse(call: Call<ResponseModel>, response: Response<ResponseModel>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@PendataanSpalActivity, if (isEditMode) "Perubahan Jaringan Pipa SPAL Sukses Diperbarui!" else "Data Jaringan Pipa SPAL & Geotagging Berhasil!", Toast.LENGTH_SHORT).show()

                    if (isEditMode) {
                        startActivity(Intent(this@PendataanSpalActivity, RiwayatInputActivity::class.java))
                        finish()
                    } else {
                        etNoKK.text.clear()
                        etNamaKepala.text.clear()
                        spinnerKelurahan.setSelection(0)
                        rgStatusSpal.clearCheck()
                        rgKondisiBak.clearCheck()
                        daftarBerkasFoto.clear()
                        tvNamaFile.text = ""
                        ivPreviewDokumen.visibility = View.GONE
                    }
                } else {
                    val errorBodyText = response.errorBody()?.string() ?: "Detail eror kosong."
                    AlertDialog.Builder(this@PendataanSpalActivity)
                        .setTitle("Server Menolak SPAL (Code: ${response.code()})")
                        .setMessage(errorBodyText)
                        .setPositiveButton("TUTUP", null)
                        .show()
                }
            }

            override fun onFailure(call: Call<ResponseModel>, t: Throwable) {
                Toast.makeText(this@PendataanSpalActivity, "Gagal Hubungan Server: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun String.toDoubleOrNull(): Double? = try { this.toDouble() } catch (e: NumberFormatException) { null }

    private fun createPartFromString(stringData: String?): RequestBody {
        return (stringData ?: "").toRequestBody("text/plain".toMediaTypeOrNull())
    }

    override fun onResume() { super.onResume(); mapView.onResume() }
    override fun onPause() { super.onPause(); mapView.onPause() }

    private fun setupNavigation() {
        val idNavRt = resources.getIdentifier("nav_rt", "id", packageName)
        val idNavKomunal = resources.getIdentifier("nav_komunal", "id", packageName)
        val idNavSpal = resources.getIdentifier("nav_spal", "id", packageName)
        val idNavPelaporan = resources.getIdentifier("nav_pelaporan", "id", packageName)
        val idNavRiwayat = resources.getIdentifier("nav_riwayat", "id", packageName)

        if (idNavRt != 0) {
            findViewById<LinearLayout>(idNavRt)?.setOnClickListener {
                val intent = Intent(this, PendataanRumahTanggaActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                startActivity(intent)
            }
        }
        if (idNavKomunal != 0) {
            findViewById<LinearLayout>(idNavKomunal)?.setOnClickListener {
                val intent = Intent(this, PendataanKomunalActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                startActivity(intent)
            }
        }
        if (idNavSpal != 0) { findViewById<LinearLayout>(idNavSpal)?.setOnClickListener {} }
        if (idNavPelaporan != 0) {
            findViewById<LinearLayout>(idNavPelaporan)?.setOnClickListener {
                try {
                    val intentPelaporan = Intent(this, Class.forName("$packageName.PelaporanActivity"))
                    intentPelaporan.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    startActivity(intentPelaporan)
                } catch (e: Exception) {
                    Toast.makeText(this, "Gagal memuat halaman Pelaporan. Periksa pendaftaran Manifest!", Toast.LENGTH_LONG).show()
                }
            }
        }
        if (idNavRiwayat != 0) {
            findViewById<LinearLayout>(idNavRiwayat)?.setOnClickListener {
                val intent = Intent(this, RiwayatInputActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                startActivity(intent)
            }
        }
    }
}