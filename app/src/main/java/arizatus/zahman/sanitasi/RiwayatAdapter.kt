package arizatus.zahman.sanitasi

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.JsonObject

class RiwayatAdapter(
    private val listData: List<JsonObject>,
    private val onItemClick: (JsonObject) -> Unit
) : RecyclerView.Adapter<RiwayatAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvJudul: TextView = view.findViewById(view.context.resources.getIdentifier("tv_judul_riwayat", "id", view.context.packageName))
        val tvAlamat: TextView = view.findViewById(view.context.resources.getIdentifier("tv_alamat_riwayat", "id", view.context.packageName))
        val tvTanggal: TextView = view.findViewById(view.context.resources.getIdentifier("tv_tanggal_riwayat", "id", view.context.packageName))
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutId = parent.context.resources.getIdentifier("item_riwayat", "layout", parent.context.packageName)
        val view = LayoutInflater.from(parent.context).inflate(layoutId, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = listData[position]

        val jenisData = if (item.has("deskrip_masalah") && !item.get("deskrip_masalah").isJsonNull) {
            item.get("deskrip_masalah").asString
        } else {
            "Pendataan Sanitasi"
        }

        val alamat = if (item.has("alamat") && !item.get("alamat").isJsonNull) {
            item.get("alamat").asString
        } else if (item.has("alamat_mck") && !item.get("alamat_mck").isJsonNull) {
            item.get("alamat_mck").asString
        } else {
            "-"
        }
        
        val namaUtama = if (item.has("kepala_keluarga") && !item.get("kepala_keluarga").isJsonNull) {
            item.get("kepala_keluarga").asString
        } else if (item.has("nama_fasilitas") && !item.get("nama_fasilitas").isJsonNull) {
            item.get("nama_fasilitas").asString
        } else {
            jenisData
        }

        val tanggal = if (item.has("created_at") && !item.get("created_at").isJsonNull) {
            item.get("created_at").asString
        } else {
            "-"
        }

        // Set text langsung ke interface widget list item tanpa ada listener klik
        holder.tvJudul.text = namaUtama
        holder.tvAlamat.text = "$jenisData\n$alamat"
        holder.tvTanggal.text = tanggal

        holder.itemView.setOnClickListener {
            onItemClick(item)
        }
    }

    override fun getItemCount(): Int = listData.size
}