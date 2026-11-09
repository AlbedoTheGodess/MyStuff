package com.example.mystuff

import android.app.AlertDialog
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class Schein(val titel: String, val ort: String, val serie: String, val nummer: String)

class MainActivity : AppCompatActivity() {

    private val scheine = mutableListOf<Schein>()
    private var modus = "Ort"            // "Ort" oder "Serie"
    private var gruppe: String? = null   // gewählte Gruppe (z. B. ein Ort oder eine Serie)
    private var suche = ""
    private var anzeige = listOf<Pair<String, String?>>() // Text, Gruppe (falls Gruppe)

    private lateinit var liste: ListView
    private lateinit var titelZeile: TextView

    private val foto = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bmp ->
        if (bmp != null) erkenne(bmp)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        laden()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(24, 48, 24, 24); setBackgroundColor(BG)
        }

        val kopf = TextView(this).apply {
            text = "MyStuff"; textSize = 34f; setTextColor(LILA); typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER; setPadding(0, 0, 0, 16)
        }
        val scanBtn = Button(this).apply {
            text = "📷 0-Euro-Schein scannen"; setBackgroundColor(LILA); setTextColor(Color.WHITE)
            setOnClickListener { foto.launch(null) }
        }
        val sucheFeld = EditText(this).apply {
            hint = "Suchen (Ort, Titel, Serie)"; setTextColor(Color.WHITE); setHintTextColor(Color.GRAY)
        }
        sucheFeld.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { suche = s.toString().trim(); gruppe = null; aktualisieren() }
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
        })

        val umschalter = LinearLayout(this).apply { gravity = Gravity.CENTER }
        val ortBtn = Button(this).apply {
            text = "Nach Ort"; setBackgroundColor(LILA_DUNKEL); setTextColor(Color.WHITE)
            setOnClickListener { modus = "Ort"; gruppe = null; aktualisieren() }
        }
        val serieBtn = Button(this).apply {
            text = "Nach Serie"; setBackgroundColor(LILA_DUNKEL); setTextColor(Color.WHITE)
            setOnClickListener { modus = "Serie"; gruppe = null; aktualisieren() }
        }
        umschalter.addView(ortBtn); umschalter.addView(serieBtn)

        titelZeile = TextView(this).apply { textSize = 18f; setPadding(0, 16, 0, 16); setTextColor(LILA_HELL) }
        liste = ListView(this)

        root.addView(kopf); root.addView(scanBtn); root.addView(sucheFeld); root.addView(umschalter)
        root.addView(titelZeile); root.addView(liste)
        setContentView(root)

        liste.setOnItemClickListener { _, _, pos, _ ->
            val g = anzeige[pos].second
            if (g != null) { gruppe = g; aktualisieren() }
        }
        liste.setOnItemLongClickListener { _, _, pos, _ ->
            // Lange drücken = Schein löschen (nur in der Scheinliste)
            if (anzeige[pos].second == null) {
                val treffer = gefiltert().getOrNull(pos)
                if (treffer != null) {
                    AlertDialog.Builder(this).setTitle("Löschen?").setMessage(treffer.titel)
                        .setPositiveButton("Ja") { _, _ -> scheine.remove(treffer); speichern(); aktualisieren() }
                        .setNegativeButton("Nein", null).show()
                }
            }
            true
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (gruppe != null) { gruppe = null; aktualisieren() } else finish()
            }
        })
        aktualisieren()
    }

    // ---------- Anzeige ----------
    private fun passt(s: Schein) = suche.isEmpty() ||
        listOf(s.titel, s.ort, s.serie, s.nummer).any { it.contains(suche, ignoreCase = true) }

    private fun key(s: Schein) = if (modus == "Ort") s.ort else s.serie

    private fun gefiltert(): List<Schein> = when {
        suche.isNotEmpty() -> scheine.filter { passt(it) }
        gruppe != null -> scheine.filter { key(it) == gruppe }
        else -> emptyList()
    }.sortedBy { it.titel }

    private fun aktualisieren() {
        if (suche.isEmpty() && gruppe == null) {
            titelZeile.text = "Sortiert nach $modus (${scheine.size} Scheine)"
            anzeige = scheine.groupBy { key(it) }.toSortedMap().map { (k, v) -> "$k  (${v.size})" to k }
        } else {
            val l = gefiltert()
            titelZeile.text = if (gruppe != null && suche.isEmpty()) "$modus: $gruppe  –  zurück mit Zurück-Taste" else "Suchergebnis (${l.size})"
            anzeige = l.map { "${it.titel}\n${it.ort}  •  Serie ${it.serie}  •  Nr. ${it.nummer}" to null }
        }
        liste.adapter = object : ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, anzeige.map { it.first }) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val v = super.getView(position, convertView, parent) as TextView
                v.setTextColor(Color.WHITE)
                return v
            }
        }
    }

    companion object {
        val BG = Color.parseColor("#0D0716")
        val LILA = Color.parseColor("#8E2DE2")
        val LILA_DUNKEL = Color.parseColor("#4A1A7A")
        val LILA_HELL = Color.parseColor("#C9A0FF")
    }

    // ---------- Scannen ----------
    private fun erkenne(bmp: Bitmap) {
        val erkenner = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        erkenner.process(InputImage.fromBitmap(bmp, 0))
            .addOnSuccessListener { ergebnis ->
                val text = ergebnis.text
                val m = Regex("([A-Z]{4})\\s?(\\d{5,6})").find(text.replace("O", "0").let { text })
                val serie = m?.groupValues?.get(1) ?: ""
                val nummer = m?.groupValues?.get(2) ?: ""
                val zeilen = ergebnis.textBlocks.flatMap { it.lines }.map { it.text.trim() }
                val titel = zeilen.firstOrNull { z ->
                    val buchst = z.filter { it.isLetter() }
                    buchst.length >= 6 && buchst.all { it.isUpperCase() } &&
                        !z.contains("EURO") && !z.contains("SOUV")
                } ?: ""
                bestaetigen(titel, serie, nummer)
            }
            .addOnFailureListener { Toast.makeText(this, "Konnte nichts erkennen", Toast.LENGTH_LONG).show() }
    }

    private fun bestaetigen(titel: String, serie: String, nummer: String) {
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(48, 16, 48, 0) }
        val t = EditText(this).apply { hint = "Titel"; setText(titel) }
        val o = EditText(this).apply { hint = "Ort (z. B. Gütersloh)" }
        val s = EditText(this).apply { hint = "Serie (4 Buchstaben, z. B. XEBS)"; setText(serie) }
        val n = EditText(this).apply { hint = "Nummer"; setText(nummer) }
        listOf(t, o, s, n).forEach { box.addView(it) }
        AlertDialog.Builder(this).setTitle("Schein speichern").setView(box)
            .setPositiveButton("Speichern") { _, _ ->
                val neu = Schein(t.text.toString().trim(), o.text.toString().trim().ifEmpty { "Unbekannt" },
                    s.text.toString().trim().uppercase().ifEmpty { "?" }, n.text.toString().trim())
                if (neu.titel.isNotEmpty()) { scheine.add(neu); speichern(); aktualisieren() }
            }
            .setNegativeButton("Abbrechen", null).show()
    }

    // ---------- Speichern ----------
    private fun datei() = File(filesDir, "scheine.json")

    private fun speichern() {
        val arr = JSONArray()
        scheine.forEach { arr.put(JSONObject().put("t", it.titel).put("o", it.ort).put("s", it.serie).put("n", it.nummer)) }
        datei().writeText(arr.toString())
    }

    private fun laden() {
        if (!datei().exists()) return
        val arr = JSONArray(datei().readText())
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            scheine.add(Schein(o.getString("t"), o.getString("o"), o.getString("s"), o.getString("n")))
        }
    }
}
