package com.example.myapplication

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray
import org.json.JSONObject
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

/**
 * Tela de Receitas (Valor que entra na conta).
 * Suporta navegação arrastando para o lado (ViewPager) e barra inferior fixa (Despesas / Receitas).
 */
class RevenuesActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val initialMonth = savedInstanceState?.getString("month")
            ?: intent.getStringExtra("month")
            ?: "Abril"
        window.statusBarColor = Color.rgb(13, 17, 16)
        window.navigationBarColor = Color.rgb(13, 17, 16)

        val dashboard = AuraDashboardView(
            activity = this,
            initialPage = 1,
            initialMonth = initialMonth
        )
        setContentView(dashboard.rootView)
    }

    companion object {
        val gold = Color.rgb(248, 202, 65)
        val ink = Color.rgb(13, 17, 16)
        val muted = Color.rgb(148, 151, 141)
        val green = Color.rgb(94, 222, 134)
        val red = Color.rgb(239, 108, 108)

        val revenueCategories = listOf(
            "Salário",
            "Freelance & Projetos",
            "Rendimentos & Investimentos",
            "Vendas & Comissões",
            "Reembolsos",
            "Aluguéis & Dividendos",
            "Outras Entradas"
        )

        val revenueColors = listOf(
            Color.rgb(94, 222, 134),
            gold,
            Color.rgb(255, 222, 142),
            Color.rgb(112, 198, 255),
            Color.rgb(156, 111, 234),
            Color.rgb(155, 164, 157),
            Color.rgb(103, 117, 109)
        )

        data class CustomEntry(val description: String, val amount: Double, val categoryIndex: Int)

        private fun Context.dp(n: Int) = (n * resources.displayMetrics.density + .5f).toInt()

        private fun Context.bg(color: Int, radius: Int = 16) = GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
        }

        private fun Context.label(s: String, size: Float = 13f, color: Int = Color.WHITE, bold: Boolean = false) = TextView(this).apply {
            text = s
            textSize = size
            setTextColor(color)
            includeFontPadding = false
            if (bold) typeface = Typeface.create("sans-serif", Typeface.BOLD)
        }

        private fun LinearLayout.put(context: Context, v: View, top: Int = 0, height: Int = -2) {
            addView(
                v,
                LinearLayout.LayoutParams(-1, if (height < 0) height else context.dp(height)).apply {
                    topMargin = context.dp(top)
                }
            )
        }

        private fun Context.row() = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }

        private fun Context.panel() = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(20))
            background = bg(Color.rgb(29, 33, 29))
        }

        fun getCustomRevenues(context: Context, targetMonth: String): MutableList<CustomEntry> {
            val prefs = context.getSharedPreferences("aura_finance_prefs", Context.MODE_PRIVATE)
            val raw = prefs.getString("revenues_$targetMonth", "[]") ?: "[]"
            val list = mutableListOf<CustomEntry>()
            try {
                val arr = JSONArray(raw)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    list.add(
                        CustomEntry(
                            description = obj.optString("description", "Receita"),
                            amount = obj.optDouble("amount", 0.0),
                            categoryIndex = obj.optInt("categoryIndex", 0).coerceIn(0, revenueCategories.lastIndex)
                        )
                    )
                }
            } catch (_: Exception) {
            }
            return list
        }

        fun saveCustomRevenues(context: Context, targetMonth: String, entries: List<CustomEntry>) {
            val arr = JSONArray()
            entries.forEach { item ->
                val obj = JSONObject().apply {
                    put("description", item.description)
                    put("amount", item.amount)
                    put("categoryIndex", item.categoryIndex)
                }
                arr.put(obj)
            }
            context.getSharedPreferences("aura_finance_prefs", Context.MODE_PRIVATE)
                .edit()
                .putString("revenues_$targetMonth", arr.toString())
                .apply()
        }

        fun showAddRevenueDialog(context: Context, month: String, onAdded: () -> Unit) {
            val money = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
            val container = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(context.dp(22), context.dp(18), context.dp(22), context.dp(10))
                background = context.bg(Color.rgb(26, 29, 26), 16)
            }

            container.put(context, context.label("Adicionar Receita", 18f, Color.WHITE, true))
            container.put(context, context.label("Registre uma entrada na conta para o mês de $month", 11f, gold), 4)

            container.put(context, context.label("Descrição", 11f, gold), 16)
            val descInput = EditText(context).apply {
                hint = "Ex: Salário, Projeto Freelance, Dividendos..."
                textSize = 13f
                setTextColor(Color.WHITE)
                setHintTextColor(muted)
                background = context.bg(Color.rgb(37, 41, 35), 10)
                setPadding(context.dp(12), context.dp(10), context.dp(12), context.dp(10))
                isSingleLine = true
            }
            container.put(context, descInput, 6)

            container.put(context, context.label("Valor de Entrada (R$)", 11f, gold), 14)
            val amountInput = EditText(context).apply {
                hint = "Ex: 1500,00"
                textSize = 13f
                setTextColor(Color.WHITE)
                setHintTextColor(muted)
                background = context.bg(Color.rgb(37, 41, 35), 10)
                setPadding(context.dp(12), context.dp(10), context.dp(12), context.dp(10))
                isSingleLine = true
                inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            }
            container.put(context, amountInput, 6)

            container.put(context, context.label("Categoria", 11f, gold), 14)
            val spinnerAdapter = object : ArrayAdapter<String>(context, android.R.layout.simple_spinner_item, revenueCategories) {
                override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                    return (super.getView(position, convertView, parent) as TextView).apply {
                        setTextColor(Color.WHITE)
                        textSize = 13f
                        setPadding(context.dp(8), context.dp(8), context.dp(8), context.dp(8))
                    }
                }

                override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                    return (super.getDropDownView(position, convertView, parent) as TextView).apply {
                        setTextColor(Color.WHITE)
                        setBackgroundColor(Color.rgb(35, 39, 35))
                        textSize = 13f
                        setPadding(context.dp(14), context.dp(12), context.dp(14), context.dp(12))
                    }
                }
            }
            val categorySpinner = Spinner(context).apply {
                adapter = spinnerAdapter
                background = context.bg(Color.rgb(37, 41, 35), 10)
                setPadding(context.dp(6), context.dp(4), context.dp(6), context.dp(4))
            }
            container.put(context, categorySpinner, 6, 46)

            val actions = context.row().apply { gravity = Gravity.END }
            val dialog = AlertDialog.Builder(context)
                .setView(container)
                .create()
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

            val cancelBtn = context.label("Cancelar", 12f, muted, true).apply {
                gravity = Gravity.CENTER
                setPadding(context.dp(16), context.dp(10), context.dp(16), context.dp(10))
                setOnClickListener { dialog.dismiss() }
            }
            val saveBtn = context.label("Salvar Receita", 12f, ink, true).apply {
                gravity = Gravity.CENTER
                background = context.bg(gold, 10)
                setPadding(context.dp(16), context.dp(10), context.dp(16), context.dp(10))
                setOnClickListener {
                    val rawAmount = amountInput.text.toString().trim()
                        .replace("R$", "")
                        .replace(" ", "")
                        .let { if (it.contains(",")) it.replace(".", "").replace(",", ".") else it }
                    val value = rawAmount.toDoubleOrNull()
                    if (value == null || value <= 0.0) {
                        amountInput.error = "Informe um valor válido"
                        amountInput.requestFocus()
                        return@setOnClickListener
                    }
                    val catIndex = categorySpinner.selectedItemPosition.coerceIn(0, revenueCategories.lastIndex)
                    val desc = descInput.text.toString().trim().ifEmpty { revenueCategories[catIndex] }
                    val entries = getCustomRevenues(context, month)
                    entries.add(0, CustomEntry(desc, value, catIndex))
                    saveCustomRevenues(context, month, entries)
                    dialog.dismiss()
                    onAdded()
                    Toast.makeText(context, "Receita de ${money.format(value)} adicionada!", Toast.LENGTH_SHORT).show()
                }
            }
            actions.addView(cancelBtn)
            actions.addView(saveBtn, LinearLayout.LayoutParams(-2, -2).apply { leftMargin = context.dp(8) })
            container.put(context, actions, 20)

            dialog.show()
        }

        fun buildRevenuesPage(
            context: Context,
            month: String,
            onMonthSelected: (String) -> Unit,
            onRefresh: () -> Unit
        ): View {
            val money = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
            val scroll = ScrollView(context).apply {
                setBackgroundColor(ink)
                isFillViewport = true
            }
            val body = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(context.dp(20), context.dp(12), context.dp(20), context.dp(24))
            }
            scroll.addView(body)

            val title = context.row()
            title.addView(context.label("Receitas", 24f, Color.WHITE, true), LinearLayout.LayoutParams(0, -2, 1f))
            title.addView(
                context.label(month, 12f, gold).apply {
                    setPadding(context.dp(10), context.dp(7), context.dp(10), context.dp(7))
                    background = context.bg(Color.rgb(37, 41, 35), 10)
                }
            )
            body.put(context, title, 4)

            val months = LinearLayout(context).apply { gravity = Gravity.CENTER_VERTICAL }
            listOf("Abril", "Março", "Fevereiro").forEach { name ->
                val active = name == month
                months.addView(
                    context.label(
                        if (name == "Abril") "✓ Este mês (Abril)" else name,
                        11f,
                        if (active) ink else Color.WHITE,
                        active
                    ).apply {
                        gravity = Gravity.CENTER
                        background = context.bg(if (active) gold else Color.rgb(35, 39, 35), 24)
                        minHeight = context.dp(44)
                        setPadding(context.dp(10), 0, context.dp(10), 0)
                        setOnClickListener { onMonthSelected(name) }
                    },
                    LinearLayout.LayoutParams(0, context.dp(44), if (name == "Abril") 1.5f else 1f).apply {
                        rightMargin = context.dp(6)
                    }
                )
            }
            body.put(context, months, 14)

            // Botão funcional de Adicionar Receita
            val addRevenueButton = context.label("+  Adicionar Receita", 13f, ink, true).apply {
                gravity = Gravity.CENTER
                background = context.bg(gold, 12)
                minHeight = context.dp(46)
                isClickable = true
                isFocusable = true
                setOnClickListener { showAddRevenueDialog(context, month, onRefresh) }
            }
            body.put(context, addRevenueButton, 14, 46)

            val baseValues = when (month) {
                "Março" -> mutableListOf(4200.0, 890.0, 480.0, 380.0, 150.0, 250.0, 70.0)
                "Fevereiro" -> mutableListOf(4200.0, 750.0, 440.0, 320.0, 110.0, 250.0, 80.0)
                else -> mutableListOf(4200.0, 1150.0, 520.0, 430.0, 180.0, 250.0, 120.0)
            }
            val customEntries = getCustomRevenues(context, month)
            customEntries.forEach { entry ->
                baseValues[entry.categoryIndex] = baseValues[entry.categoryIndex] + entry.amount
            }
            val values = baseValues.toList()
            val total = values.sum()
            val previous = when (month) {
                "Abril" -> 6420.0
                "Março" -> 6150.0
                else -> 5980.0
            }
            val difference = total - previous

            val summary = context.panel()
            val consolidated = context.row()
            consolidated.addView(context.label("TOTAL CONSOLIDADO (ENTRADAS)", 10f, gold), LinearLayout.LayoutParams(0, -2, 1f))
            consolidated.addView(
                context.label(
                    String.format(Locale("pt", "BR"), "%s %.1f%%", if (difference >= 0) "↗" else "↘", abs(difference) / previous * 100),
                    11f,
                    if (difference >= 0) green else red,
                    true
                )
            )
            summary.put(context, consolidated)
            summary.put(context, context.label(money.format(total), 31f, Color.WHITE, true), 10)
            summary.put(
                context,
                context.label(
                    "${if (difference >= 0) "Crescimento" else "Redução"} de ${money.format(abs(difference))} em relação ao mês anterior.",
                    11f,
                    gold
                ),
                8
            )

            val stats = context.row()
            fun stat(titleText: String, value: String, foot: String) = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                background = context.bg(Color.rgb(35, 39, 33), 12)
                setPadding(context.dp(12), context.dp(12), context.dp(12), context.dp(12))
                put(context, context.label(titleText, 10f, muted))
                put(context, context.label(value, 16f, gold, true), 8)
                put(context, context.label(foot, 10f, muted), 6)
            }
            stats.addView(
                stat("▣ Mês anterior", money.format(previous), "Referência mensal"),
                LinearLayout.LayoutParams(0, -2, 1f).apply { rightMargin = context.dp(8) }
            )
            stats.addView(
                stat("◷ Média diária", money.format(total / if (month == "Fevereiro") 28 else if (month == "Março") 31 else 30), "entrada por dia"),
                LinearLayout.LayoutParams(0, -2, 1f)
            )
            summary.put(context, stats, 18)

            val goal = context.row()
            goal.addView(context.label("Progresso da meta mensal", 10f, gold), LinearLayout.LayoutParams(0, -2, 1f))
            goal.addView(context.label(String.format(Locale("pt", "BR"), "%.1f%% (R$ 8.000,00)", total / 80), 10f, gold, true))
            summary.put(context, goal, 18)
            summary.put(
                context,
                ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal).apply {
                    max = 8000
                    progress = total.toInt().coerceAtMost(8000)
                    progressTintList = android.content.res.ColorStateList.valueOf(green)
                    progressBackgroundTintList = android.content.res.ColorStateList.valueOf(Color.rgb(55, 59, 53))
                },
                5,
                8
            )
            body.put(context, summary, 18)

            body.put(context, context.label("◉  Receitas por Categoria", 16f, Color.WHITE, true), 24)
            val categories = context.panel()
            val bar = context.row()
            values.forEachIndexed { i, amount ->
                bar.addView(
                    View(context).apply { setBackgroundColor(revenueColors[i]) },
                    LinearLayout.LayoutParams(0, context.dp(7), amount.toFloat())
                )
            }
            categories.put(context, bar)

            revenueCategories.forEachIndexed { i, name ->
                val line = context.row().apply { minimumHeight = context.dp(46) }
                line.addView(context.label("●", 14f, revenueColors[i]), LinearLayout.LayoutParams(context.dp(22), -2))
                line.addView(context.label(name, 12f).apply { setPadding(0, 0, context.dp(6), 0) }, LinearLayout.LayoutParams(0, -2, 1f))
                line.addView(context.label(money.format(values[i]), 12f, Color.WHITE, true))
                line.addView(
                    context.label("${(values[i] / total * 100).toInt()}%", 10f, muted).apply { gravity = Gravity.END },
                    LinearLayout.LayoutParams(context.dp(34), -2)
                )
                line.setOnClickListener {
                    val addedInCat = customEntries.filter { it.categoryIndex == i }
                    val details = if (addedInCat.isEmpty()) {
                        "$month: ${money.format(values[i])}\nValores consolidados de entrada na categoria."
                    } else {
                        val itemsText = addedInCat.joinToString("\n") { "• ${it.description}: ${money.format(it.amount)}" }
                        "$month: ${money.format(values[i])}\n\nReceitas adicionadas:\n$itemsText"
                    }
                    AlertDialog.Builder(context)
                        .setTitle(name)
                        .setMessage(details)
                        .setPositiveButton("Fechar", null)
                        .show()
                }
                categories.put(context, line, if (i == 0) 14 else 0)
            }
            body.put(context, categories, 12)

            if (customEntries.isNotEmpty()) {
                body.put(context, context.label("◉  Receitas Adicionadas ($month)", 16f, Color.WHITE, true), 22)
                val customPanel = context.panel()
                customEntries.forEachIndexed { index, entry ->
                    val itemRow = context.row().apply { minimumHeight = context.dp(44) }
                    itemRow.addView(
                        context.label("●", 12f, revenueColors[entry.categoryIndex]),
                        LinearLayout.LayoutParams(context.dp(20), -2)
                    )
                    val infoCol = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
                    infoCol.put(context, context.label(entry.description, 12f, Color.WHITE, true))
                    infoCol.put(context, context.label(revenueCategories[entry.categoryIndex], 10f, muted), 2)
                    itemRow.addView(infoCol, LinearLayout.LayoutParams(0, -2, 1f))
                    itemRow.addView(context.label("+ ${money.format(entry.amount)}", 12f, green, true))
                    itemRow.setOnClickListener {
                        AlertDialog.Builder(context)
                            .setTitle(entry.description)
                            .setMessage("Categoria: ${revenueCategories[entry.categoryIndex]}\nValor: ${money.format(entry.amount)}\n\nDeseja remover este lançamento?")
                            .setPositiveButton("Remover") { _, _ ->
                                val updated = getCustomRevenues(context, month)
                                if (index in updated.indices) {
                                    updated.removeAt(index)
                                    saveCustomRevenues(context, month, updated)
                                    onRefresh()
                                }
                            }
                            .setNegativeButton("Fechar", null)
                            .show()
                    }
                    customPanel.put(context, itemRow, if (index == 0) 0 else 6)
                }
                body.put(context, customPanel, 12)
            }

            body.put(
                context,
                context.label("Conta de demonstração • Arraste para o lado ↔", 11f, muted).apply { gravity = Gravity.CENTER },
                20
            )
            return scroll
        }
    }
}
