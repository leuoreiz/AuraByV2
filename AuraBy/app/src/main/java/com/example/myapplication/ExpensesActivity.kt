package com.example.myapplication

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.text.NumberFormat
import java.util.Locale

/** Painel local com dados demonstrativos; não acessa serviços financeiros. */
class ExpensesActivity : AppCompatActivity() {
    private val gold = Color.rgb(248, 202, 65)
    private val ink = Color.rgb(13, 17, 16)
    private val muted = Color.rgb(148, 151, 141)
    private lateinit var body: LinearLayout
    private val money = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
    private var month = "Abril"
    private fun dp(n: Int) = (n * resources.displayMetrics.density + .5f).toInt()
    private fun bg(color: Int, radius: Int = 16) = GradientDrawable().apply { setColor(color); cornerRadius = dp(radius).toFloat() }
    private fun label(s: String, size: Float = 13f, color: Int = Color.WHITE, bold: Boolean = false) = TextView(this).apply {
        text = s; textSize = size; setTextColor(color); includeFontPadding = false
        if (bold) typeface = Typeface.create("sans-serif", Typeface.BOLD)
    }
    private fun LinearLayout.put(v: View, top: Int = 0, height: Int = -2) {
        addView(v, LinearLayout.LayoutParams(-1, if (height < 0) height else dp(height)).apply { topMargin = dp(top) })
    }
    private fun row() = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
    private fun panel() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(20), dp(20), dp(20)); background = bg(Color.rgb(29, 33, 29))
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        month = savedInstanceState?.getString("month") ?: "Abril"
        window.statusBarColor = ink; window.navigationBarColor = ink
        render()
    }
    override fun onSaveInstanceState(outState: Bundle) { super.onSaveInstanceState(outState); outState.putString("month", month) }
    private fun render() {
        val scroll = ScrollView(this).apply { setBackgroundColor(ink); isFillViewport = true }
        body = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(20), dp(20), dp(28)) }
        scroll.addView(body)
        val header = row()
        header.addView(label("A", 22f, gold, true).apply { gravity = Gravity.CENTER; background = bg(Color.rgb(42, 43, 30), 10) }, LinearLayout.LayoutParams(dp(42), dp(42)))
        header.addView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(10), 0, 0, 0)
            put(label("Aura", 17f, Color.WHITE, true)); put(label("Despesas", 10f, muted), 3)
        }, LinearLayout.LayoutParams(0, -2, 1f))
        header.addView(label("Sair", 12f, gold).apply {
            gravity = Gravity.CENTER; minHeight = dp(48); minWidth = dp(48)
            setOnClickListener { finish() }
        })
        body.put(header)
        val title = row()
        title.addView(label("Despesas", 24f, Color.WHITE, true), LinearLayout.LayoutParams(0, -2, 1f))
        title.addView(label(month, 12f, gold).apply { setPadding(dp(10), dp(7), dp(10), dp(7)); background = bg(Color.rgb(37, 41, 35), 10) })
        body.put(title, 24)
        val months = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        listOf("Abril", "Março", "Fevereiro").forEach { name ->
            val active = name == month
            months.addView(label(if (name == "Abril") "✓ Este mês (Abril)" else name, 11f, if (active) ink else Color.WHITE, active).apply {
                gravity = Gravity.CENTER; background = bg(if (active) gold else Color.rgb(35, 39, 35), 24)
                minHeight = dp(44); setPadding(dp(10), 0, dp(10), 0)
                setOnClickListener { month = name; render() }
            }, LinearLayout.LayoutParams(0, dp(44), if (name == "Abril") 1.5f else 1f).apply { rightMargin = dp(6) })
        }
        body.put(months, 16)
        val values = when (month) {
            "Março" -> listOf(1250.0, 1050.0, 650.0, 480.0, 390.0, 230.0, 130.0)
            "Fevereiro" -> listOf(1080.0, 980.0, 510.0, 400.0, 350.0, 220.5, 150.0)
            else -> listOf(1150.0, 980.0, 540.0, 420.0, 380.0, 220.5, 150.0)
        }
        val total = values.sum()
        val previous = when (month) { "Abril" -> 4180.0; "Março" -> 3690.5; else -> 4020.0 }
        val difference = previous - total
        val summary = panel()
        val consolidated = row()
        consolidated.addView(label("TOTAL CONSOLIDADO", 10f, gold), LinearLayout.LayoutParams(0, -2, 1f))
        consolidated.addView(label(String.format(Locale("pt", "BR"), "%s %.1f%%", if (difference >= 0) "↘" else "↗", kotlin.math.abs(difference) / previous * 100), 11f, Color.rgb(94, 222, 134), true))
        summary.put(consolidated)
        summary.put(label(money.format(total), 31f, Color.WHITE, true), 10)
        summary.put(label("${if (difference >= 0) "Economia" else "Aumento"} de ${money.format(kotlin.math.abs(difference))} em relação ao mês anterior.", 11f, gold), 8)
        val stats = row()
        fun stat(titleText: String, value: String, foot: String) = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; background = bg(Color.rgb(35, 39, 33), 12); setPadding(dp(12), dp(12), dp(12), dp(12))
            put(label(titleText, 10f, muted)); put(label(value, 16f, gold, true), 8); put(label(foot, 10f, muted), 6)
        }
        stats.addView(stat("▣ Mês anterior", money.format(previous), "Referência mensal"), LinearLayout.LayoutParams(0, -2, 1f).apply { rightMargin = dp(8) })
        stats.addView(stat("◷ Média diária", money.format(total / if (month == "Fevereiro") 28 else if (month == "Março") 31 else 30), "por dia no mês"), LinearLayout.LayoutParams(0, -2, 1f))
        summary.put(stats, 18)
        val budget = row()
        budget.addView(label("Consumo do teto mensal", 10f, gold), LinearLayout.LayoutParams(0, -2, 1f))
        budget.addView(label(String.format(Locale("pt", "BR"), "%.1f%% (R$ 5.000,00)", total / 50), 10f, gold, true))
        summary.put(budget, 18)
        summary.put(ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 5000; progress = total.toInt(); progressTintList = android.content.res.ColorStateList.valueOf(gold)
            progressBackgroundTintList = android.content.res.ColorStateList.valueOf(Color.rgb(55, 59, 53))
        }, 5, 8)
        body.put(summary, 20)
        body.put(label("◉  Despesas por Categoria", 16f, Color.WHITE, true), 24)
        val categories = panel()
        val names = listOf("Alimentação", "Moradia", "Transporte", "Compras & Lazer", "Saúde & Farmácia", "Assinaturas & Serviços", "Outros")
        val colors = listOf(gold, Color.rgb(255, 222, 142), Color.rgb(155, 164, 157), Color.rgb(127, 145, 135), Color.rgb(59, 216, 121), Color.rgb(156, 111, 234), Color.rgb(103, 117, 109))
        val bar = row()
        values.forEachIndexed { i, amount -> bar.addView(View(this).apply { setBackgroundColor(colors[i]) }, LinearLayout.LayoutParams(0, dp(7), amount.toFloat())) }
        categories.put(bar)
        names.forEachIndexed { i, name ->
            val line = row().apply { minimumHeight = dp(46) }
            line.addView(label("●", 14f, colors[i]), LinearLayout.LayoutParams(dp(22), -2))
            line.addView(label(name, 12f).apply { setPadding(0, 0, dp(6), 0) }, LinearLayout.LayoutParams(0, -2, 1f))
            line.addView(label(money.format(values[i]), 12f, Color.WHITE, true))
            line.addView(label("${(values[i] / total * 100).toInt()}%", 10f, muted).apply { gravity = Gravity.END }, LinearLayout.LayoutParams(dp(34), -2))
            line.setOnClickListener {
                androidx.appcompat.app.AlertDialog.Builder(this).setTitle(name)
                    .setMessage("$month: ${money.format(values[i])}\nValores de demonstração.")
                    .setPositiveButton("Fechar", null).show()
            }
            categories.put(line, if (i == 0) 14 else 0)
        }
        body.put(categories, 12)
        body.put(label("Conta de demonstração • dados fictícios", 11f, muted).apply { gravity = Gravity.CENTER }, 20)
        setContentView(scroll)
    }
}
