package arizatus.zahman.sanitasi.utils

import arizatus.zahman.sanitasi.JalanData
import arizatus.zahman.sanitasi.JalanListResponse
import arizatus.zahman.sanitasi.api.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

object LocationHelper {
    var dynamicJalanList: List<JalanData> = emptyList()

    fun fetchDynamicJalan(onComplete: (() -> Unit)? = null) {
        RetrofitClient.instance.getListJalan().enqueue(object : Callback<JalanListResponse> {
            override fun onResponse(call: Call<JalanListResponse>, response: Response<JalanListResponse>) {
                if (response.isSuccessful && response.body()?.status == "success") {
                    dynamicJalanList = response.body()?.data ?: emptyList()
                }
                onComplete?.invoke()
            }
            override fun onFailure(call: Call<JalanListResponse>, t: Throwable) {
                onComplete?.invoke() // Proceed even on failure
            }
        })
    }

    fun getJalanList(kelurahan: String): java.util.ArrayList<String> {
        val list = java.util.ArrayList<String>()
        list.add("Pilih Jalan")
        
        val array = when (kelurahan) {
            "Bandar Kidul" -> arrayOf("Pilih Jalan", "Jl. Kiai Haji Agus Salim", "Jl. Kiai Haji Wahid Hasyim", "Jl. Dokter Saharjo", "Jl. KH. Achmad Dahlan", "Jl. Penanggungan", "Jl. Bandar Kidul", "Kampung Tenun Bandar", "Jl. Kiai Haji Agus Salim Gg. I", "Jl. Kiai Haji Agus Salim Gg. II", "Jl. Kiai Haji Agus Salim Gg. III", "Jl. Kiai Haji Agus Salim Gg. IV", "Jl. Kiai Haji Agus Salim Gg. V", "Jl. Kiai Haji Agus Salim Gg. VI", "Jl. Kiai Haji Agus Salim Gg. VII", "Jl. Kiai Haji Wahid Hasyim Gg. VI", "Jl. Kiai Haji Wahid Hasyim Gg. VII", "Jl. Kiai Haji Wahid Hasyim Gg. VIII", "Jl. Kiai Haji Wahid Hasyim Gg. IX", "Gang Tenun", "Jl. Bandar Kidul Gg. 1", "Jl. Bandar Kidul Gg. 2", "Jl. Bandar Kidul Gg. 3", "Jl. Bandar Kidul Gg. 4", "Jl. Bandar Kidul Gg. Pondok")
            "Bandar Lor" -> arrayOf("Pilih Jalan", "Jl. Veteran", "Jl. Kiai Haji Wahid Hasyim", "Jl. Komisaris Daerah Polisi Slamet", "Jl. Penanggungan", "Jl. Bandar Lor", "Jl. Penanggungan Utara", "Kawasan Pondok Bandar Lor", "Jl. Penanggungan Gg. Gajah Mada", "Jl. Penanggungan Gg. Garuda", "Jl. Penanggungan Gg. Merpati", "Jl. Veteran Gg. I", "Jl. Veteran Gg. II", "Jl. Veteran Gg. III", "Jl. Veteran Gg. IV", "Jl. Kiai Haji Wahid Hasyim Gg. I", "Jl. Kiai Haji Wahid Hasyim Gg. II", "Jl. Kiai Haji Wahid Hasyim Gg. III", "Jl. Kiai Haji Wahid Hasyim Gg. IV", "Jl. Kiai Haji Wahid Hasyim Gg. V", "Jl. Komisaris Daerah Polisi Slamet Gg. I", "Jl. Komisaris Daerah Polisi Slamet Gg. II", "Jl. Komisaris Daerah Polisi Slamet Gg. II B", "Jl. Bandar Lor Gg. 1", "Jl. Bandar Lor Gg. 2", "Jl. Bandar Lor Gg. 3", "Jl. Bandar Lor Gg. 4", "Jl. Bandar Lor Gg. Makam")
            "Banjarmlati" -> arrayOf("Pilih Jalan", "Jl. KH. Agus Salim", "Jl. GOR Jayabaya", "Gg. Setono", "Gg. Pulaosari", "Gg. Gonda", "Jl. GOR Jayabaya Gg. I", "Jl. GOR Jayabaya Gg. II", "Jl. GOR Jayabaya Gg. III", "Jl. Banjarmlati", "Banjarmlati Krajan", "Jl. Banjarmlati Gg. 1", "Jl. Banjarmlati Gg. 2", "Jl. Banjarmlati Gg. Masjid", "Jl. Banjarmlati Gg. Makam")
            "Bujel" -> arrayOf("Pilih Jalan", "Jl. Suparjan Mangun Wijaya", "Jl. Suparjan Mangun Wijaya Gg. 1", "Jl. Suparjan Mangun Wijaya Gg. 2", "Jl. Suparjan Mangun Wijaya Gg. 3", "Jl. Suparjan Mangun Wijaya Gg. 4", "Jl. Suparjan Mangun Wijaya Gg. 5", "Jl. Suparjan Mangun Wijaya Gg. 6", "Gg. Melati", "Gg. Mawar", "Gg. Kenanga", "Jl. Jaksa Agung Suprapto", "Jl. Bujel", "Lingkungan Bujel Sukorame", "Jl. Bujel Gg. 1", "Jl. Bujel Gg. 2", "Jl. Bujel Gg. 3", "Jl. Bujel Gg. Masjid")
            "Campurejo" -> arrayOf("Pilih Jalan", "Jl. Dr. Saharjo", "Jl. Semeru", "Jl. Rinjani", "Jl. Wilis Utama", "Jl. Wilis Mukti I", "Jl. Wilis Mukti II", "Jl. Wilis Mukti III", "Jl. Wilis Mukti IV", "Jl. Wilis Mukti V", "Jl. Wilis Mukti VI", "Jl. Wilis Mukti VII", "Jl. Wilis Madya I", "Jl. Wilis Madya II", "Jl. Wilis Madya III", "Jl. Wilis Madya IV", "Jl. Wilis Madya V", "Jl. Wilis Mulya I", "Jl. Wilis Mulya II", "Jl. Wilis Mulya III", "Jl. Wilis Mulya IV", "Jl. Wilis Mulya V", "Jl. Wilis Asri 1", "Jl. Wilis Asri 2", "Jl. Wilis Asri 3", "Jl. Wilis Asri 4", "Jl. Wilis Asri 5", "Jl. Wilis Asri 6", "Jl. Wilis Asri 7", "Jl. Wilis Tama I", "Jl. Wilis Tama II", "Jl. Wilis Tama III", "Jl. Wilis Tama IV", "Jl. Wilis Tama V", "Jl. Wilis Tama VI", "Jl. Dr. Saharjo Gg. I", "Jl. Dr. Saharjo Gg. II", "Jl. Dr. Saharjo Gg. III", "Jl. Dr. Saharjo Gg. III A", "Jl. Dr. Saharjo Gg. IV", "Jl. Dr. Saharjo Gg. V", "Jl. Dr. Saharjo Gg. VI", "Jl. Dr. Saharjo Gg. VII", "Jl. Dr. Saharjo Gg. VIII", "Jl. Dr. Saharjo Gg. IX", "Jl. Dr. Saharjo Gg. X", "Jl. Semeru Gg. I", "Jl. Semeru Gg. I B", "Jl. Semeru Gg. II", "Jl. Semeru Gg. III", "Jl. Semeru Gg. IV", "Jl. Semeru Gg. V", "Jl. Semeru Gg. VI", "Jl. Semeru Gg. VII", "Jl. Semeru Gg. VIII", "Jl. Semeru Gg. IX", "Jl. Semeru Gg. X", "Jl. Semeru Gg. Baru")
            "Dermo" -> arrayOf("Pilih Jalan", "Jl. Mayjen Pandjaitan", "Jl. Mayjen Pandjaitan Gg. I", "Jl. Mayjen Pandjaitan Gg. II", "Jl. Mayjen Pandjaitan Gg. III", "Jl. Mayjen Pandjaitan Gg. IV", "Gg. Dermo Kidul", "Gg. Dermo Lor", "Jl. Veteran", "Jl. Sersan Bahrun", "Jl. Dermo", "Lingkungan Dermo Krajan", "Jl. Dermo Gg. 1", "Jl. Dermo Gg. 2", "Jl. Dermo Gg. 3")
            "Gayam" -> arrayOf("Pilih Jalan", "Jl. Raya Gayam", "Jl. Dr. Saharjo", "Jl. Raya Gayam Gg. I", "Jl. Raya Gayam Gg. II", "Jl. Raya Gayam Gg. III", "Jl. Raya Gayam Gg. IV", "Gg. Tani", "Gg. Gayam Asri", "Jl. Gayam Raya", "Lingkungan Gayam Selatan", "Jl. Gayam Raya Gg. 1", "Jl. Gayam Raya Gg. 2", "Jl. Gayam Raya Gg. 3")
            "Lirboyo" -> arrayOf("Pilih Jalan", "Jl. Kiai Haji Abdul Karim", "Jl. Penanggungan", "Jl. Dokter Saharjo", "Jl. Kiai Mojo", "Kawasan Pondok Lirboyo", "Jl. Kiai Haji Abdul Karim Gg. I", "Jl. Kiai Haji Abdul Karim Gg. II", "Jl. Kiai Haji Abdul Karim Gg. III", "Jl. Kiai Haji Abdul Karim Gg. IV", "Jl. Kiai Haji Abdul Karim Gg. V", "Jl. Kiai Haji Abdul Karim Gg. VI", "Jl. Kiai Haji Abdul Karim Gg. VII", "Jl. Kiai Haji Abdul Karim Gg. VIII", "Jl. Kiai Haji Abdul Karim Gg. IX", "Jl. Kiai Haji Abdul Karim Gg. X", "Jl. Penanggungan Gg. I", "Jl. Penanggungan Gg. II", "Jl. Penanggungan Gg. III")
            "Mojoroto" -> arrayOf("Pilih Jalan", "Jl. KH. Ahmad Dahlan", "Jl. Supriadi", "Jl. Veteran", "Jl. Mojoroto Raya", "Jl. Mojoroto Gg. 1", "Jl. Mojoroto Gg. 2", "Jl. Mojoroto Gg. 3", "Jl. Mojoroto Gg. IV", "Jl. Mojoroto Gg. 4 Barat", "Jl. Mojoroto Gg. 5", "Jl. Mojoroto Gg. 6 Barat", "Jl. Mojoroto Gg. 6 Timur", "Jl. Mojoroto Gg. 7", "Jl. Mojoroto Gg. 8", "Jl. Mojoroto Gg. Koramil")
            "Mrican" -> arrayOf("Pilih Jalan", "Jl. Gatot Subroto", "Jl. Dworowati", "Jl. Gatot Subroto Gg. I", "Jl. Gatot Subroto Gg. II", "Jl. Gatot Subroto Gg. III", "Jl. Gatot Subroto Gg. IV", "Jl. Gatot Subroto Gg. V", "Jl. Dworowati Gg. 1", "Jl. Dworowati Gg. 2", "Jl. Dworowati Gg. 3", "Gg. PG Mrican", "Jl. Sersan Suharmaji", "Jl. Mrican", "Kawasan PG Mrican", "Jl. Mrican Gg. 1", "Jl. Mrican Gg. 2", "Jl. Mrican Gg. 3")
            "Ngampel" -> arrayOf("Pilih Jalan", "Jl. KH. Munjat", "Jl. Ngampel Raya", "Jl. Ngampel Gg. 1", "Jl. Ngampel Gg. 2", "Jl. Ngampel Gg. 3", "Jl. Cendrawasih (Perum Ngampel Indah)", "Jl. Merak (Perum Ngampel Indah)", "Jl. Rajawali (Perum Ngampel Indah)", "Jl. Suparjan Mangun Wijaya", "Kampung Ngampel Rel", "Jl. Ngampel Raya Gg. 1", "Jl. Ngampel Raya Gg. 2", "Jl. Ngampel Raya Gg. 3")
            "Pojok" -> arrayOf("Pilih Jalan", "Jl. Mastrip", "Jl. Selomangleng", "Jl. Dr. Saharjo", "Jl.Lawu", "Jl. Lingkar Maskumambang", "Jl. Tembus Lebak Tumpang", "Jl. Gunung Klotok", "Jl. Mastrip Gg. Swadaya", "Jl. Mastrip Gg. Flamboyan", "Jl. Selomangleng Gg. Bima", "Jl. Dr. Saharjo Gg. III A", "Gg. Pojok I", "Gg. Pojok II", "Jalan Kompleks Perumahan Wilis Indah", "Perum Wilis Indah 2 Blok G Raya", "Perum Wilis Indah 2 Blok H Raya", "Perum Wilis Indah 2 Blok H1", "Perum Wilis Indah 2 Blok H 11", "Perum Wilis Indah 2 Blok I Raya", "Perum Wilis Indah 2 Blok I9", "Perum Wilis Indah 2 Blok J Raya", "Perum Wilis Indah 2 Blok J4", "Perum Wilis Indah 2 Blok J7")
            "Sukorame" -> arrayOf("Pilih Jalan", "Jl. Veteran", "Jl. Selomangleng", "Jl. Lingkar Maskumambang", "Jl. Veteran Gg. Merbabu", "Jl. Veteran Gg. Wilis", "Jl. Veteran Gg. Kawi", "Jl. Veteran Gg. Anjasmoro", "Jl. Sukorame Utama", "Gg. Masjid Sukorame", "Perumahan Sukorame Blok A", "Perumahan Sukorame Blok B", "Perumahan Sukorame Blok C", "Perumahan Sukorame Blok D", "Perumahan Sukorame Blok E", "Perumahan Sukorame Blok F", "Jl. Sukorame", "Jl. Kawi Utara", "Lingkungan Sukorame Timur", "Jl. Sukorame Gg. 1", "Jl. Sukorame Gg. 2", "Jl. Sukorame Gg. 3", "Jl. Sukorame Gg. 4", "Jl. Sukorame Gg. Makam")
            "Tamanan" -> arrayOf("Pilih Jalan", "Jl. Semeru", "Jl. Taman Sari", "Jl. Dr. Saharjo", "Jl. Tamanan Raya", "Lingkungan Sekitar Terminal", "Jl. Taman Sari Gg. I", "Jl. Taman Sari Gg. II", "Jl. Taman Sari Gg. III", "Jl. Taman Sari Gg. IV", "Gg. Terminal", "Gg. Makam Tamanan", "Jl. Tamanan Raya Gg. 1", "Jl. Tamanan Raya Gg. 2", "Jl. Tamanan Raya Gg. 3")
            else -> arrayOf("Pilih Jalan / Lingkungan")
        }
        
        // 1. Tambahkan data bawaan (default array)
        if (array.size > 1) {
            list.addAll(array.drop(1))
        }

        // 2. Tambahkan data historis dari database server
        val filtered = dynamicJalanList.filter { it.kelurahan == kelurahan }
        filtered.forEach { dbJalan ->
            if (!list.any { it.equals(dbJalan.namaJalan, ignoreCase = true) }) {
                list.add(dbJalan.namaJalan)
            }
        }

        return list
    }

    fun getKodePos(kelurahan: String): String {
        return when (kelurahan) {
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
    }

    fun getMaxRw(kelurahan: String): Int {
        return when (kelurahan) {
            "Mojoroto" -> 12; "Pojok" -> 11
            "Bandar Kidul", "Campurejo", "Lirboyo" -> 9
            "Sukorame", "Bandar Lor" -> 8; "Banjarmlati" -> 7
            "Mrican", "Bujel" -> 6; "Ngampel", "Tamanan" -> 5
            "Dermo", "Gayam" -> 4; else -> 5
        }
    }
}
