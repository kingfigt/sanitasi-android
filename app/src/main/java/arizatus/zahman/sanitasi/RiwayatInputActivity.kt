package arizatus.zahman.sanitasi

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import arizatus.zahman.sanitasi.api.RetrofitClient
import com.google.gson.JsonObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import android.app.AlertDialog
import arizatus.zahman.sanitasi.api.ResponseModel

class RiwayatInputActivity : AppCompatActivity() {

    private lateinit var rvRiwayat: RecyclerView
    private var idUserAktif: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(resources.getIdentifier("activity_riwayat_input", "layout", packageName))

        val sharedPref = getSharedPreferences("SanitasiSession", Context.MODE_PRIVATE)
        idUserAktif = sharedPref.getInt("USER_ID", 0)

        val idRvData = resources.getIdentifier("rv_riwayat_data", "id", packageName)
        rvRiwayat = findViewById(idRvData)
        rvRiwayat.layoutManager = LinearLayoutManager(this)

        ambilDataRiwayatPelaporanMck()
        setupNavigation()

        findViewById<ImageView>(R.id.btn_logout)?.setOnClickListener {
            val sharedPreferences = getSharedPreferences("SanitasiSession", Context.MODE_PRIVATE)
            sharedPreferences.edit().clear().apply()
            val intentLogout = Intent(this, LoginActivity::class.java)
            intentLogout.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intentLogout)
            finish()
        }
    }

    private fun ambilDataRiwayatPelaporanMck() {
        RetrofitClient.instance.getRiwayatPelaporan(idUserAktif).enqueue(object : Callback<JsonObject> {
            override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                Log.d("URL_TERTEMBAK", "Request URL: ${call.request().url}")
                Log.d("URL_TERTEMBAK", "Response Code: ${response.code()}")

                if (response.isSuccessful && response.body() != null) {
                    try {
                        val jsonBody = response.body()!!
                        val dataArray = jsonBody.getAsJsonArray("data")

                        if (dataArray == null || dataArray.size() == 0) {
                            Toast.makeText(this@RiwayatInputActivity, "Belum ada riwayat laporan", Toast.LENGTH_SHORT).show()
                            return
                        }

                        val listData = ArrayList<JsonObject>()
                        for (i in 0 until dataArray.size()) {
                            listData.add(dataArray.get(i).asJsonObject)
                        }

                        val adapter = RiwayatAdapter(listData) { item ->
                            tampilkanOpsiEditHapus(item)
                        }
                        rvRiwayat.adapter = adapter

                    } catch (e: Exception) {
                        e.printStackTrace()
                        Toast.makeText(this@RiwayatInputActivity, "Gagal mengurai riwayat data", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@RiwayatInputActivity, "Server Error: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<JsonObject>, t: Throwable) {
                Toast.makeText(this@RiwayatInputActivity, "Kendala Jaringan: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun tampilkanOpsiEditHapus(item: JsonObject) {
        val options = arrayOf("Edit Data", "Hapus Data")
        AlertDialog.Builder(this)
            .setTitle("Pilih Aksi")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> prosesEditData(item)
                    1 -> konfirmasiHapusData(item)
                }
            }
            .show()
    }

    private fun prosesEditData(item: JsonObject) {
        val idData = item.get("id")?.asInt ?: return
        val typeData = if (item.has("type") && !item.get("type").isJsonNull) item.get("type").asString else return

        val intentTujuan = when (typeData) {
            "rumah_tangga" -> Intent(this, PendataanRumahTanggaActivity::class.java)
            "komunal" -> Intent(this, PendataanKomunalActivity::class.java)
            "spal" -> Intent(this, PendataanSpalActivity::class.java)
            "laporan" -> Intent(this, PelaporanActivity::class.java)
            else -> null
        }

        if (intentTujuan != null) {
            intentTujuan.putExtra("IS_EDIT_MODE", true)
            intentTujuan.putExtra("EDIT_ID", idData)
            startActivity(intentTujuan)
        } else {
            Toast.makeText(this, "Tipe data tidak dikenali untuk diedit", Toast.LENGTH_SHORT).show()
        }
    }

    private fun konfirmasiHapusData(item: JsonObject) {
        val idData = item.get("id")?.asInt ?: return
        val typeData = if (item.has("type") && !item.get("type").isJsonNull) item.get("type").asString else return

        AlertDialog.Builder(this)
            .setTitle("Konfirmasi Hapus")
            .setMessage("Apakah Anda yakin ingin menghapus riwayat ini secara permanen?")
            .setPositiveButton("Hapus") { _, _ ->
                RetrofitClient.instance.deleteData(typeData, idData).enqueue(object : Callback<ResponseModel> {
                    override fun onResponse(call: Call<ResponseModel>, response: Response<ResponseModel>) {
                        if (response.isSuccessful) {
                            Toast.makeText(this@RiwayatInputActivity, "Data berhasil dihapus!", Toast.LENGTH_SHORT).show()
                            ambilDataRiwayatPelaporanMck() // Refresh data
                        } else {
                            Toast.makeText(this@RiwayatInputActivity, "Gagal menghapus data dari server", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onFailure(call: Call<ResponseModel>, t: Throwable) {
                        Toast.makeText(this@RiwayatInputActivity, "Koneksi gagal: ${t.message}", Toast.LENGTH_SHORT).show()
                    }
                })
            }
            .setNegativeButton("Batal", null)
            .show()
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
        if (idNavPelaporan != 0) { findViewById<LinearLayout>(idNavPelaporan)?.setOnClickListener { startActivity(Intent(this, PelaporanActivity::class.java)); finish() } }
        if (idNavRiwayat != 0) { findViewById<LinearLayout>(idNavRiwayat)?.setOnClickListener { } }
    }
}