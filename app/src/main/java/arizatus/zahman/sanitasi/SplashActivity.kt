package arizatus.zahman.sanitasi

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.enableEdgeToEdge
import arizatus.zahman.sanitasi.utils.LocationHelper

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_splash)

        LocationHelper.fetchDynamicJalan {
            Handler(Looper.getMainLooper()).postDelayed({
                val sharedPref = getSharedPreferences("SanitasiSession", android.content.Context.MODE_PRIVATE)
                val userId = sharedPref.getInt("USER_ID", 0)

                if (userId != 0) {
                    // Jika sudah login, langsung ke halaman utama
                    startActivity(Intent(this, PendataanRumahTanggaActivity::class.java))
                } else {
                    // Jika belum login, ke halaman login
                    startActivity(Intent(this, LoginActivity::class.java))
                }
                finish()
            }, 1000)
        }
    }
}
