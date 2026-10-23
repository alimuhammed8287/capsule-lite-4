package com.example.capsule

import android.app.Activity
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

// Shows the last ten notifications the pill saw. Stored only on this phone.
class HistoryActivity : Activity() {

    private val hist by lazy { getSharedPreferences("history", MODE_PRIVATE) }
    private lateinit var box: LinearLayout

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(24), dp(20), dp(24))
        }
        col.addView(TextView(this).apply { text = "Recent notifications"; textSize = 20f })
        box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        col.addView(box)
        col.addView(Button(this).apply {
            text = "Clear history"
            setOnClickListener {
                hist.edit().remove("list").apply()
                refresh()
            }
        })
        setContentView(ScrollView(this).apply { addView(col) })
        refresh()
    }

    private fun refresh() {
        box.removeAllViews()
        val lines = (hist.getString("list", "") ?: "").split("\n").filter { it.isNotBlank() }
        if (lines.isEmpty()) box.addView(TextView(this).apply { text = "No notifications yet" })
        for (line in lines) {
            val p = line.split("|", limit = 4)
            if (p.size < 4) continue
            box.addView(TextView(this).apply {
                text = "${p[0]}  \u00B7  ${p[1]}\n${p[2]}\n${p[3]}"
                setPadding(0, dp(8), 0, dp(8))
            })
        }
    }
}
