package com.example.capsule

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix

// Generates QR codes and Code 128 barcodes on the phone. Saved codes stay in local storage only.
class CodeActivity : Activity() {

    private val codes by lazy { getSharedPreferences("codes", MODE_PRIVATE) }
    private var format = BarcodeFormat.QR_CODE
    private lateinit var input: EditText
    private lateinit var formatBtn: Button
    private lateinit var preview: ImageView
    private lateinit var message: TextView
    private lateinit var savedBox: LinearLayout

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(24), dp(20), dp(24))
            gravity = Gravity.CENTER_HORIZONTAL
        }
        col.addView(TextView(this).apply { text = "Code generator"; textSize = 20f })

        input = EditText(this).apply { hint = "Text, link, Wi-Fi or number" }
        col.addView(input)

        formatBtn = Button(this).apply { text = formatLabel() }
        formatBtn.setOnClickListener {
            format = if (format == BarcodeFormat.QR_CODE) BarcodeFormat.CODE_128 else BarcodeFormat.QR_CODE
            formatBtn.text = formatLabel()
        }
        col.addView(formatBtn)

        col.addView(Button(this).apply { text = "Generate"; setOnClickListener { generate() } })

        preview = ImageView(this).apply {
            setBackgroundColor(Color.WHITE)
            setPadding(dp(8), dp(8), dp(8), dp(8))
            adjustViewBounds = true
            layoutParams = LinearLayout.LayoutParams(dp(260), LinearLayout.LayoutParams.WRAP_CONTENT)
                .apply { topMargin = dp(12) }
        }
        col.addView(preview)

        message = TextView(this).apply { textSize = 13f }
        col.addView(message)

        col.addView(Button(this).apply { text = "Save this code"; setOnClickListener { save() } })

        col.addView(TextView(this).apply {
            text = "Saved codes (stored only on this phone)"
            textSize = 14f
            setPadding(0, dp(20), 0, dp(4))
        })
        savedBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        col.addView(savedBox)
        refreshSaved()

        setContentView(ScrollView(this).apply { addView(col) })
    }

    private fun formatLabel() =
        if (format == BarcodeFormat.QR_CODE) "Format: QR code" else "Format: Code 128 barcode"

    private fun generate() {
        val text = input.text.toString().trim()
        if (text.isEmpty()) {
            message.text = "Type something first."
            return
        }
        val bmp = encode(text, format)
        if (bmp == null) {
            preview.setImageDrawable(null)
            message.text = "This text can't be encoded in this format. Try the QR code."
        } else {
            preview.setImageBitmap(bmp)
            message.text = ""
        }
    }

    private fun encode(text: String, f: BarcodeFormat): Bitmap? = runCatching {
        val w = if (f == BarcodeFormat.QR_CODE) 600 else 800
        val h = if (f == BarcodeFormat.QR_CODE) 600 else 300
        val matrix: BitMatrix = MultiFormatWriter().encode(text, f, w, h)
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        for (x in 0 until w) {
            for (y in 0 until h) {
                bmp.setPixel(x, y, if (matrix.get(x, y)) Color.BLACK else Color.WHITE)
            }
        }
        bmp
    }.getOrNull()

    private fun loadSaved(): List<Pair<String, String>> =
        (codes.getString("list", "") ?: "").split("\n").filter { it.contains("|") }
            .map { val parts = it.split("|", limit = 2); parts[0] to parts[1] }

    private fun writeSaved(list: List<Pair<String, String>>) {
        codes.edit().putString("list", list.joinToString("\n") { "${it.first}|${it.second}" }).apply()
    }

    private fun save() {
        val text = input.text.toString().trim().replace("\n", " ")
        if (text.isEmpty()) {
            message.text = "Nothing to save."
            return
        }
        val tag = if (format == BarcodeFormat.QR_CODE) "QR" else "C128"
        writeSaved(loadSaved() + (tag to text))
        message.text = "Saved."
        refreshSaved()
    }

    private fun refreshSaved() {
        savedBox.removeAllViews()
        val items = loadSaved()
        if (items.isEmpty()) savedBox.addView(TextView(this).apply { this.text = "None yet" })
        items.forEachIndexed { index, (tag, value) ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            row.addView(TextView(this).apply {
                this.text = "$tag  $value"
                isSingleLine = true
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
            row.addView(Button(this).apply {
                this.text = "Show"
                setOnClickListener {
                    input.setText(value)
                    format = if (tag == "QR") BarcodeFormat.QR_CODE else BarcodeFormat.CODE_128
                    formatBtn.text = formatLabel()
                    generate()
                }
            })
            row.addView(Button(this).apply {
                this.text = "Delete"
                setOnClickListener {
                    writeSaved(loadSaved().filterIndexed { i, _ -> i != index })
                    refreshSaved()
                }
            })
            savedBox.addView(row)
        }
    }
}
