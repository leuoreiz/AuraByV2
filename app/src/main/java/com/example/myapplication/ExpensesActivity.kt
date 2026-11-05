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
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import org.json.JSONArray
import org.json.JSONObject
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

/**
 * Painel de Despesas e Receitas com suporte a arrastar para o lado (ViewPager),
 * seletor completo de todos os 12 meses do ano e cadastro de compras no cartão de crédito
 * (à vista ou parceladas automaticamente entre os meses).
 */
class ExpensesActivity : AppCompatActivity() {

    private lateinit var dashboard: AuraDashboardView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val initialMonth = savedInstanceState?.getString("month")
            ?: intent.getStringExtra("month")
            ?: getCurrentCalendarMonth()
        val initialPage = savedInstanceState?.getInt("page")
            ?: intent.getIntExtra("page", 0)

        window.statusBarColor = Color.rgb(13, 17, 16)
        window.navigationBarColor = Color.rgb(13, 17, 16)

        dashboard = AuraDashboardView(
            activity = this,
            initialPage = initialPage,
            initialMonth = initialMonth
        )
        setContentView(dashboard.rootView)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (::dashboard.isInitialized) {
            outState.putString("month", dashboard.currentMonth)
            outState.putInt("page", dashboard.currentPage)
        }
    }

    companion object {
        val gold = Color.rgb(248, 202, 65)
        val ink = Color.rgb(13, 17, 16)
        val muted = Color.rgb(148, 151, 141)
        val green = Color.rgb(94, 222, 134)
        val red = Color.rgb(239, 108, 108)
        val purpleCard = Color.rgb(179, 136, 255)

        val allMonths = listOf(
            "Janeiro",
            "Fevereiro",
            "Março",
            "Abril",
            "Maio",
            "Junho",
            "Julho",
            "Agosto",
            "Setembro",
            "Outubro",
            "Novembro",
            "Dezembro"
        )

        val expenseCategories = listOf(
            "Alimentação",
            "Moradia",
            "Transporte",
            "Compras & Lazer",
            "Saúde & Farmácia",
            "Assinaturas & Serviços",
            "Outros"
        )

        val expenseColors = listOf(
            gold,
            Color.rgb(255, 222, 142),
            Color.rgb(155, 164, 157),
            Color.rgb(127, 145, 135),
            Color.rgb(59, 216, 121),
            Color.rgb(156, 111, 234),
            Color.rgb(103, 117, 109)
        )

        data class CustomEntry(
            val description: String,
            val amount: Double,
            val categoryIndex: Int,
            val isCreditCard: Boolean = false,
            val cardName: String = "",
            val installmentInfo: String = ""
        )

        private fun Context.dp(n: Int) = (n * resources.displayMetrics.density + .5f).toInt()

        private fun Context.bg(color: Int, radius: Int = 16, strokeColor: Int? = null) = GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
            strokeColor?.let { setStroke(dp(1), it) }
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

        fun getCurrentCalendarMonth(): String {
            val idx = java.util.Calendar.getInstance()
                .get(java.util.Calendar.MONTH)
                .coerceIn(0, allMonths.lastIndex)
            return allMonths[idx]
        }

        fun daysInMonth(month: String): Int = when (month) {
            "Fevereiro" -> 28
            "Abril", "Junho", "Setembro", "Novembro" -> 30
            else -> 31
        }

        fun previousMonthName(month: String): String {
            val idx = allMonths.indexOf(month).coerceAtLeast(0)
            val prevIdx = if (idx > 0) idx - 1 else allMonths.lastIndex
            return allMonths[prevIdx]
        }

        fun getCurrentUserEmail(context: Context): String {
            val prefs = context.getSharedPreferences("aura_finance_prefs", Context.MODE_PRIVATE)
            return (prefs.getString("current_user_email", "teste@aura.com") ?: "teste@aura.com").lowercase().trim()
        }

        fun isDemoAccount(context: Context): Boolean {
            return getCurrentUserEmail(context) == "teste@aura.com"
        }

        private fun expensesPrefKey(context: Context, targetMonth: String): String {
            val userEmail = getCurrentUserEmail(context)
            return if (userEmail == "teste@aura.com") "expenses_$targetMonth" else "expenses_${userEmail}_$targetMonth"
        }

        fun getCustomExpenses(context: Context, targetMonth: String): MutableList<CustomEntry> {
            val prefs = context.getSharedPreferences("aura_finance_prefs", Context.MODE_PRIVATE)
            val raw = prefs.getString(expensesPrefKey(context, targetMonth), "[]") ?: "[]"
            val list = mutableListOf<CustomEntry>()
            try {
                val arr = JSONArray(raw)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    list.add(
                        CustomEntry(
                            description = obj.optString("description", "Despesa"),
                            amount = obj.optDouble("amount", 0.0),
                            categoryIndex = obj.optInt("categoryIndex", 0).coerceIn(0, expenseCategories.lastIndex),
                            isCreditCard = obj.optBoolean("isCreditCard", false),
                            cardName = obj.optString("cardName", ""),
                            installmentInfo = obj.optString("installmentInfo", "")
                        )
                    )
                }
            } catch (_: Exception) {
            }
            return list
        }

        fun saveCustomExpenses(context: Context, targetMonth: String, entries: List<CustomEntry>) {
            val arr = JSONArray()
            entries.forEach { item ->
                val obj = JSONObject().apply {
                    put("description", item.description)
                    put("amount", item.amount)
                    put("categoryIndex", item.categoryIndex)
                    put("isCreditCard", item.isCreditCard)
                    put("cardName", item.cardName)
                    put("installmentInfo", item.installmentInfo)
                }
                arr.put(obj)
            }
            context.getSharedPreferences("aura_finance_prefs", Context.MODE_PRIVATE)
                .edit()
                .putString(expensesPrefKey(context, targetMonth), arr.toString())
                .apply()
        }

        /**
         * Modal estilizado de Resumo Mensal comparando o valor que entrou (Receitas) e o valor que saiu (Despesas).
         */
        fun showMonthlySummaryDialog(
            context: Context,
            month: String,
            totalRevenues: Double,
            totalExpenses: Double,
            futureCardTotal: Double
        ) {
            val money = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
            val balance = totalRevenues - totalExpenses
            val isPositive = balance >= 0.0

            val container = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(context.dp(22), context.dp(20), context.dp(22), context.dp(16))
                background = context.bg(Color.rgb(26, 29, 26), 18, Color.rgb(90, 74, 32))
            }

            container.put(context, context.label("📊 Resumo Mensal ($month)", 18f, Color.WHITE, true))
            container.put(
                context,
                context.label("Balanço consolidado entre Receitas e Despesas do mês", 11f, gold),
                4
            )

            // Card de Entradas (Receitas)
            val revBox = context.row().apply {
                background = context.bg(Color.rgb(33, 39, 34), 12)
                setPadding(context.dp(14), context.dp(12), context.dp(14), context.dp(12))
            }
            val revLeft = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
            revLeft.put(context, context.label("↗ Valor que entrou (Receitas)", 11f, muted))
            revLeft.put(context, context.label("Total de entradas em $month", 10f, green), 2)
            revBox.addView(revLeft, LinearLayout.LayoutParams(0, -2, 1f))
            revBox.addView(context.label("+ ${money.format(totalRevenues)}", 15f, green, true))
            container.put(context, revBox, 16)

            // Card de Saídas (Despesas)
            val expBox = context.row().apply {
                background = context.bg(Color.rgb(39, 34, 34), 12)
                setPadding(context.dp(14), context.dp(12), context.dp(14), context.dp(12))
            }
            val expLeft = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
            expLeft.put(context, context.label("↘ Valor que saiu (Despesas)", 11f, muted))
            expLeft.put(context, context.label("Total de gastos em $month", 10f, red), 2)
            expBox.addView(expLeft, LinearLayout.LayoutParams(0, -2, 1f))
            expBox.addView(context.label("- ${money.format(totalExpenses)}", 15f, red, true))
            container.put(context, expBox, 10)

            // Card de Saldo Final do Mês
            val balanceBox = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                background = context.bg(Color.rgb(35, 39, 33), 14, if (isPositive) green else red)
                setPadding(context.dp(16), context.dp(14), context.dp(16), context.dp(14))
            }
            val balHeader = context.row()
            balHeader.addView(
                context.label("SALDO DO MÊS", 10f, gold, true),
                LinearLayout.LayoutParams(0, -2, 1f)
            )
            balHeader.addView(
                context.label(
                    if (balance > 0.0) "SUPERÁVIT" else if (balance < 0.0) "DÉFICIT" else "EQUILIBRADO",
                    10f,
                    if (isPositive) green else red,
                    true
                )
            )
            balanceBox.put(context, balHeader)
            balanceBox.put(
                context,
                context.label(
                    "${if (balance >= 0.0) "" else "- "}${money.format(abs(balance))}",
                    24f,
                    if (isPositive) green else red,
                    true
                ),
                6
            )
            val statusMessage = when {
                totalRevenues == 0.0 && totalExpenses == 0.0 -> "Nenhuma receita ou despesa registrada em $month."
                balance > 0.0 -> "Você economizou ${money.format(balance)} neste mês!"
                balance < 0.0 -> "Atenção: seus gastos superaram as receitas em ${money.format(abs(balance))}."
                else -> "Suas despesas igualaram exatamente suas receitas."
            }
            balanceBox.put(context, context.label(statusMessage, 11f, Color.WHITE), 6)

            if (totalRevenues > 0.0) {
                val committedPct = (totalExpenses / totalRevenues * 100.0)
                balanceBox.put(
                    context,
                    context.label(
                        String.format(Locale("pt", "BR"), "Comprometimento da receita: %.1f%%", committedPct),
                        10.5f,
                        gold
                    ),
                    6
                )
            }
            if (futureCardTotal > 0.0) {
                balanceBox.put(
                    context,
                    context.label("💳 Lançamentos futuros no cartão: ${money.format(futureCardTotal)}", 10.5f, muted),
                    4
                )
            }
            container.put(context, balanceBox, 12)

            val dialog = AlertDialog.Builder(context)
                .setView(container)
                .create()
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

            val actions = context.row().apply { gravity = Gravity.END }
            val closeBtn = context.label("Fechar", 12f, ink, true).apply {
                gravity = Gravity.CENTER
                background = context.bg(gold, 10)
                setPadding(context.dp(22), context.dp(10), context.dp(22), context.dp(10))
                setOnClickListener { dialog.dismiss() }
            }
            actions.addView(closeBtn)
            container.put(context, actions, 18)

            dialog.show()
        }

        /**
         * Modal estilizado no padrão escuro/dourado do Aura Finanças para exibir detalhes de categorias e lançamentos.
         */
        fun showStyledDetailsDialog(
            context: Context,
            title: String,
            subtitle: String,
            details: String,
            accentColor: Int = gold,
            removeLabel: String? = null,
            onRemove: (() -> Unit)? = null
        ) {
            val container = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(context.dp(22), context.dp(20), context.dp(22), context.dp(16))
                background = context.bg(Color.rgb(26, 29, 26), 18, Color.rgb(75, 68, 38))
            }

            val headerRow = context.row()
            headerRow.addView(
                context.label("●", 15f, accentColor, true),
                LinearLayout.LayoutParams(context.dp(20), -2)
            )
            headerRow.addView(
                context.label(title, 18f, Color.WHITE, true),
                LinearLayout.LayoutParams(0, -2, 1f)
            )
            container.put(context, headerRow)
            container.put(context, context.label(subtitle, 13f, gold, true), 6)

            val infoBox = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                background = context.bg(Color.rgb(35, 39, 33), 12)
                setPadding(context.dp(14), context.dp(12), context.dp(14), context.dp(12))
            }
            infoBox.put(context, context.label(details, 12f, Color.rgb(220, 223, 216)))
            container.put(context, infoBox, 14)

            val dialog = AlertDialog.Builder(context)
                .setView(container)
                .create()
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

            val actions = context.row().apply { gravity = Gravity.END }
            if (removeLabel != null && onRemove != null) {
                val closeBtn = context.label("Fechar", 12f, muted, true).apply {
                    gravity = Gravity.CENTER
                    setPadding(context.dp(16), context.dp(10), context.dp(16), context.dp(10))
                    setOnClickListener { dialog.dismiss() }
                }
                val removeBtn = context.label(removeLabel, 12f, Color.WHITE, true).apply {
                    gravity = Gravity.CENTER
                    background = context.bg(Color.rgb(195, 68, 68), 10)
                    setPadding(context.dp(16), context.dp(10), context.dp(16), context.dp(10))
                    setOnClickListener {
                        dialog.dismiss()
                        onRemove()
                    }
                }
                actions.addView(closeBtn)
                actions.addView(removeBtn, LinearLayout.LayoutParams(-2, -2).apply { leftMargin = context.dp(8) })
            } else {
                val closeBtn = context.label("Fechar", 12f, ink, true).apply {
                    gravity = Gravity.CENTER
                    background = context.bg(gold, 10)
                    setPadding(context.dp(20), context.dp(10), context.dp(20), context.dp(10))
                    setOnClickListener { dialog.dismiss() }
                }
                actions.addView(closeBtn)
            }
            container.put(context, actions, 18)

            dialog.show()
        }

        /**
         * Modal para escolher qualquer um dos 12 meses do ano.
         */
        fun showMonthPickerDialog(
            context: Context,
            currentMonth: String,
            onMonthSelected: (String) -> Unit
        ) {
            val container = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(context.dp(20), context.dp(20), context.dp(20), context.dp(16))
                background = context.bg(Color.rgb(26, 29, 26), 18, Color.rgb(65, 62, 42))
            }

            container.put(context, context.label("Escolher Mês", 18f, Color.WHITE, true))
            container.put(context, context.label("Selecione qualquer mês do ano para consultar ou lançar valores", 11f, gold), 4)

            val dialog = AlertDialog.Builder(context)
                .setView(container)
                .create()
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

            val grid = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
            }
            container.put(context, grid, 16)

            for (rowIdx in 0 until 4) {
                val rowLayout = context.row()
                for (colIdx in 0 until 3) {
                    val monthIndex = rowIdx * 3 + colIdx
                    val monthName = allMonths[monthIndex]
                    val isSelected = monthName == currentMonth
                    val hasEntries = getCustomExpenses(context, monthName).isNotEmpty() ||
                        RevenuesActivity.getCustomRevenues(context, monthName).isNotEmpty()

                    val displayLabel = if (hasEntries && !isSelected) "$monthName •" else monthName
                    val btn = context.label(
                        displayLabel,
                        12f,
                        if (isSelected) ink else Color.WHITE,
                        isSelected || hasEntries
                    ).apply {
                        gravity = Gravity.CENTER
                        minHeight = context.dp(44)
                        background = context.bg(
                            if (isSelected) gold else Color.rgb(36, 40, 35),
                            11,
                            if (hasEntries && !isSelected) Color.rgb(110, 96, 45) else null
                        )
                        isClickable = true
                        isFocusable = true
                        setOnClickListener {
                            dialog.dismiss()
                            onMonthSelected(monthName)
                        }
                    }
                    rowLayout.addView(
                        btn,
                        LinearLayout.LayoutParams(0, context.dp(44), 1f).apply {
                            if (colIdx < 2) rightMargin = context.dp(8)
                        }
                    )
                }
                grid.addView(
                    rowLayout,
                    LinearLayout.LayoutParams(-1, -2).apply {
                        if (rowIdx > 0) topMargin = context.dp(8)
                    }
                )
            }

            val closeRow = context.row().apply { gravity = Gravity.END }
            closeRow.addView(
                context.label("Fechar", 12f, muted, true).apply {
                    setPadding(context.dp(14), context.dp(10), context.dp(8), context.dp(6))
                    setOnClickListener { dialog.dismiss() }
                }
            )
            container.put(context, closeRow, 12)

            dialog.show()
        }

        fun showAddExpenseDialog(context: Context, month: String, onAdded: () -> Unit) {
            val money = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
            val container = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(context.dp(22), context.dp(18), context.dp(22), context.dp(10))
                background = context.bg(Color.rgb(26, 29, 26), 16)
            }

            container.put(context, context.label("Adicionar Despesa", 18f, Color.WHITE, true))
            container.put(context, context.label("Registre uma saída para o mês de $month", 11f, gold), 4)

            container.put(context, context.label("Descrição", 11f, gold), 16)
            val descInput = EditText(context).apply {
                hint = "Ex: Supermercado, Aluguel, Uber..."
                textSize = 13f
                setTextColor(Color.WHITE)
                setHintTextColor(muted)
                background = context.bg(Color.rgb(37, 41, 35), 10)
                setPadding(context.dp(12), context.dp(10), context.dp(12), context.dp(10))
                isSingleLine = true
            }
            container.put(context, descInput, 6)

            container.put(context, context.label("Valor (R$)", 11f, gold), 14)
            val amountInput = EditText(context).apply {
                hint = "Ex: 150,00"
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
            val spinnerAdapter = createStyledSpinnerAdapter(context, expenseCategories)
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
            val saveBtn = context.label("Salvar Despesa", 12f, ink, true).apply {
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
                    val catIndex = categorySpinner.selectedItemPosition.coerceIn(0, expenseCategories.lastIndex)
                    val desc = descInput.text.toString().trim().ifEmpty { expenseCategories[catIndex] }
                    val entries = getCustomExpenses(context, month)
                    entries.add(0, CustomEntry(desc, value, catIndex, isCreditCard = false))
                    saveCustomExpenses(context, month, entries)
                    dialog.dismiss()
                    onAdded()
                    Toast.makeText(context, "Despesa de ${money.format(value)} adicionada em $month!", Toast.LENGTH_SHORT).show()
                }
            }
            actions.addView(cancelBtn)
            actions.addView(saveBtn, LinearLayout.LayoutParams(-2, -2).apply { leftMargin = context.dp(8) })
            container.put(context, actions, 20)

            dialog.show()
        }

        /**
         * Modal completo para cadastrar Compras no Cartão de Crédito (à vista ou parceladas entre os meses).
         */
        fun showAddCreditCardPurchaseDialog(
            context: Context,
            initialMonth: String,
            onAdded: (String) -> Unit
        ) {
            val money = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
            val scrollWrapper = ScrollView(context).apply {
                isFillViewport = true
            }
            val container = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(context.dp(22), context.dp(18), context.dp(22), context.dp(12))
                background = context.bg(Color.rgb(26, 29, 26), 16, Color.rgb(90, 74, 32))
            }
            scrollWrapper.addView(container)

            val currentMonthIdx = allMonths.indexOf(initialMonth).coerceAtLeast(0)
            val nextMonthIdx = (currentMonthIdx + 1) % allMonths.size
            val nextMonthName = allMonths[nextMonthIdx]

            container.put(context, context.label("💳 Compra no Cartão", 18f, Color.WHITE, true))
            container.put(
                context,
                context.label("Compras em $initialMonth entram na fatura do próximo mês ($nextMonthName)", 11f, gold),
                4
            )

            container.put(context, context.label("Descrição da compra", 11f, gold), 14)
            val descInput = EditText(context).apply {
                hint = "Ex: Notebook, Mercado, Passagem aérea..."
                textSize = 13f
                setTextColor(Color.WHITE)
                setHintTextColor(muted)
                background = context.bg(Color.rgb(37, 41, 35), 10)
                setPadding(context.dp(12), context.dp(10), context.dp(12), context.dp(10))
                isSingleLine = true
            }
            container.put(context, descInput, 6)

            container.put(context, context.label("Cartão utilizado", 11f, gold), 12)
            val cardNameInput = EditText(context).apply {
                hint = "Ex: Nubank, Itaú, Inter, C6..."
                textSize = 13f
                setTextColor(Color.WHITE)
                setHintTextColor(muted)
                background = context.bg(Color.rgb(37, 41, 35), 10)
                setPadding(context.dp(12), context.dp(10), context.dp(12), context.dp(10))
                isSingleLine = true
            }
            container.put(context, cardNameInput, 6)

            container.put(context, context.label("Valor Total da Compra (R$)", 11f, gold), 12)
            val amountInput = EditText(context).apply {
                hint = "Ex: 600,00"
                textSize = 13f
                setTextColor(Color.WHITE)
                setHintTextColor(muted)
                background = context.bg(Color.rgb(37, 41, 35), 10)
                setPadding(context.dp(12), context.dp(10), context.dp(12), context.dp(10))
                isSingleLine = true
                inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            }
            container.put(context, amountInput, 6)

            val rowInstallmentsAndMonth = context.row()
            val installmentsCol = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
            installmentsCol.put(context, context.label("Parcelas", 11f, gold))
            val installmentOptions = (1..12).map { n ->
                if (n == 1) "1x (À vista)" else "${n}x sem juros"
            }
            val installmentsSpinner = Spinner(context).apply {
                adapter = createStyledSpinnerAdapter(context, installmentOptions)
                background = context.bg(Color.rgb(37, 41, 35), 10)
                setPadding(context.dp(6), context.dp(4), context.dp(6), context.dp(4))
            }
            installmentsCol.put(context, installmentsSpinner, 6, 44)

            val startMonthCol = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
            startMonthCol.put(context, context.label("1ª Fatura (Próx. mês)", 11f, gold))
            val nextMonthBox = context.label("📅 $nextMonthName", 13f, gold, true).apply {
                gravity = Gravity.CENTER_VERTICAL
                background = context.bg(Color.rgb(37, 41, 35), 10, Color.rgb(90, 74, 32))
                setPadding(context.dp(12), 0, context.dp(12), 0)
            }
            startMonthCol.put(context, nextMonthBox, 6, 44)

            rowInstallmentsAndMonth.addView(
                installmentsCol,
                LinearLayout.LayoutParams(0, -2, 1f).apply { rightMargin = context.dp(8) }
            )
            rowInstallmentsAndMonth.addView(
                startMonthCol,
                LinearLayout.LayoutParams(0, -2, 1f)
            )
            container.put(context, rowInstallmentsAndMonth, 12)

            container.put(context, context.label("Categoria", 11f, gold), 12)
            val categorySpinner = Spinner(context).apply {
                adapter = createStyledSpinnerAdapter(context, expenseCategories)
                background = context.bg(Color.rgb(37, 41, 35), 10)
                setPadding(context.dp(6), context.dp(4), context.dp(6), context.dp(4))
                setSelection(3) // Padrão: Compras & Lazer
            }
            container.put(context, categorySpinner, 6, 44)

            val actions = context.row().apply { gravity = Gravity.END }
            val dialog = AlertDialog.Builder(context)
                .setView(scrollWrapper)
                .create()
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

            val cancelBtn = context.label("Cancelar", 12f, muted, true).apply {
                gravity = Gravity.CENTER
                setPadding(context.dp(14), context.dp(10), context.dp(14), context.dp(10))
                setOnClickListener { dialog.dismiss() }
            }
            val saveBtn = context.label("Lançar no Cartão", 12f, ink, true).apply {
                gravity = Gravity.CENTER
                background = context.bg(gold, 10)
                setPadding(context.dp(16), context.dp(10), context.dp(16), context.dp(10))
                setOnClickListener {
                    val rawAmount = amountInput.text.toString().trim()
                        .replace("R$", "")
                        .replace(" ", "")
                        .let { if (it.contains(",")) it.replace(".", "").replace(",", ".") else it }
                    val totalValue = rawAmount.toDoubleOrNull()
                    if (totalValue == null || totalValue <= 0.0) {
                        amountInput.error = "Informe um valor válido"
                        amountInput.requestFocus()
                        return@setOnClickListener
                    }

                    val installments = (installmentsSpinner.selectedItemPosition + 1).coerceIn(1, 12)
                    val startMonthIdx = nextMonthIdx
                    val catIndex = categorySpinner.selectedItemPosition.coerceIn(0, expenseCategories.lastIndex)
                    val baseDesc = descInput.text.toString().trim().ifEmpty { expenseCategories[catIndex] }
                    val cardName = cardNameInput.text.toString().trim().ifEmpty { "Cartão de Crédito" }
                    val perInstallment = totalValue / installments

                    for (p in 0 until installments) {
                        val targetMonthIdx = (startMonthIdx + p) % allMonths.size
                        val targetMonth = allMonths[targetMonthIdx]
                        val installmentBadge = if (installments == 1) "À vista" else "${p + 1}/${installments}"
                        val monthEntries = getCustomExpenses(context, targetMonth)
                        monthEntries.add(
                            0,
                            CustomEntry(
                                description = baseDesc,
                                amount = perInstallment,
                                categoryIndex = catIndex,
                                isCreditCard = true,
                                cardName = cardName,
                                installmentInfo = installmentBadge
                            )
                        )
                        saveCustomExpenses(context, targetMonth, monthEntries)
                    }

                    val firstMonthName = allMonths[startMonthIdx]
                    dialog.dismiss()
                    onAdded(firstMonthName)

                    val msg = if (installments == 1) {
                        "Compra de ${money.format(totalValue)} lançada para a fatura de $firstMonthName (próximo mês)!"
                    } else {
                        "Compra parcelada em ${installments}x de ${money.format(perInstallment)} a partir de $firstMonthName (próximo mês)!"
                    }
                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                }
            }
            actions.addView(cancelBtn)
            actions.addView(saveBtn, LinearLayout.LayoutParams(-2, -2).apply { leftMargin = context.dp(8) })
            container.put(context, actions, 18)

            dialog.show()
        }

        private fun createStyledSpinnerAdapter(context: Context, items: List<String>): ArrayAdapter<String> {
            return object : ArrayAdapter<String>(context, android.R.layout.simple_spinner_item, items) {
                override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                    return (super.getView(position, convertView, parent) as TextView).apply {
                        setTextColor(Color.WHITE)
                        textSize = 12.5f
                        setPadding(context.dp(8), context.dp(6), context.dp(8), context.dp(6))
                    }
                }

                override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                    return (super.getDropDownView(position, convertView, parent) as TextView).apply {
                        setTextColor(Color.WHITE)
                        setBackgroundColor(Color.rgb(35, 39, 35))
                        textSize = 13f
                        setPadding(context.dp(14), context.dp(11), context.dp(14), context.dp(11))
                    }
                }
            }
        }

        fun buildExpensesPage(
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
            title.addView(context.label("Despesas", 24f, Color.WHITE, true), LinearLayout.LayoutParams(0, -2, 1f))
            body.put(context, title, 4)

            // Barra de seleção rápida + botão para escolher qualquer mês do ano
            val currentIdx = allMonths.indexOf(month).coerceAtLeast(0)
            val prevIdx = if (currentIdx > 0) currentIdx - 1 else allMonths.lastIndex
            val quickMonths = listOf(month, allMonths[prevIdx])

            val monthsBar = context.row()
            quickMonths.forEach { name ->
                val active = name == month
                monthsBar.addView(
                    context.label(
                        if (active) "✓ $name" else name,
                        11f,
                        if (active) ink else Color.WHITE,
                        active
                    ).apply {
                        gravity = Gravity.CENTER
                        background = context.bg(if (active) gold else Color.rgb(35, 39, 35), 24)
                        minHeight = context.dp(42)
                        setPadding(context.dp(10), 0, context.dp(10), 0)
                        setOnClickListener { onMonthSelected(name) }
                    },
                    LinearLayout.LayoutParams(0, context.dp(42), 1f).apply {
                        rightMargin = context.dp(6)
                    }
                )
            }
            monthsBar.addView(
                context.label("📅 Escolher Mês", 11f, gold, true).apply {
                    gravity = Gravity.CENTER
                    background = context.bg(Color.rgb(35, 39, 35), 24, gold)
                    minHeight = context.dp(42)
                    setPadding(context.dp(12), 0, context.dp(12), 0)
                    isClickable = true
                    isFocusable = true
                    setOnClickListener {
                        showMonthPickerDialog(context, month, onMonthSelected)
                    }
                },
                LinearLayout.LayoutParams(0, context.dp(42), 1.35f)
            )
            body.put(context, monthsBar, 14)

            // Botões de ação: Adicionar Despesa e Compra no Cartão de Crédito
            val actionsRow = context.row()
            val addExpenseButton = context.label("+ Nova Despesa", 12f, ink, true).apply {
                gravity = Gravity.CENTER
                background = context.bg(gold, 12)
                minHeight = context.dp(46)
                isClickable = true
                isFocusable = true
                setOnClickListener { showAddExpenseDialog(context, month, onRefresh) }
            }
            val addCardPurchaseButton = context.label("💳 Compra no Cartão", 12f, gold, true).apply {
                gravity = Gravity.CENTER
                background = context.bg(Color.rgb(32, 36, 31), 12, gold)
                minHeight = context.dp(46)
                isClickable = true
                isFocusable = true
                setOnClickListener {
                    showAddCreditCardPurchaseDialog(context, month) { _ ->
                        onRefresh()
                    }
                }
            }
            actionsRow.addView(
                addExpenseButton,
                LinearLayout.LayoutParams(0, context.dp(46), 1f).apply { rightMargin = context.dp(8) }
            )
            actionsRow.addView(
                addCardPurchaseButton,
                LinearLayout.LayoutParams(0, context.dp(46), 1.15f)
            )
            body.put(context, actionsRow, 12)

            val isDemo = isDemoAccount(context)
            val userEmail = getCurrentUserEmail(context)

            fun demoBaseForMonth(m: String): MutableList<Double> = when (m) {
                "Novembro", "Abril" -> mutableListOf(1150.0, 980.0, 540.0, 420.0, 380.0, 220.5, 150.0)
                "Outubro", "Março" -> mutableListOf(1250.0, 1050.0, 650.0, 480.0, 390.0, 230.0, 130.0)
                "Setembro", "Fevereiro" -> mutableListOf(1080.0, 980.0, 510.0, 400.0, 350.0, 220.5, 150.0)
                else -> MutableList(expenseCategories.size) { 0.0 }
            }

            fun monthTotalForUser(m: String): Double {
                val base = if (isDemo) demoBaseForMonth(m) else MutableList(expenseCategories.size) { 0.0 }
                val custom = getCustomExpenses(context, m)
                return base.sum() + custom.sumOf { it.amount }
            }

            val baseValues = if (isDemo) demoBaseForMonth(month) else MutableList(expenseCategories.size) { 0.0 }
            val customEntries = getCustomExpenses(context, month)
            customEntries.forEach { entry ->
                baseValues[entry.categoryIndex] = baseValues[entry.categoryIndex] + entry.amount
            }
            val values = baseValues.toList()
            val total = values.sum()

            fun demoRevenuesBaseForMonth(m: String): MutableList<Double> = when (m) {
                "Novembro", "Abril" -> mutableListOf(4200.0, 1150.0, 520.0, 430.0, 180.0, 250.0, 120.0)
                "Outubro", "Março" -> mutableListOf(4200.0, 890.0, 480.0, 380.0, 150.0, 250.0, 70.0)
                "Setembro", "Fevereiro" -> mutableListOf(4200.0, 750.0, 440.0, 320.0, 110.0, 250.0, 80.0)
                else -> MutableList(RevenuesActivity.revenueCategories.size) { 0.0 }
            }
            val revBase = if (isDemo) demoRevenuesBaseForMonth(month) else MutableList(RevenuesActivity.revenueCategories.size) { 0.0 }
            val customRevenues = RevenuesActivity.getCustomRevenues(context, month)
            val totalRevenuesMonth = revBase.sum() + customRevenues.sumOf { it.amount }

            val prevMonth = previousMonthName(month)
            val previous = if (isDemo && (month == "Novembro" || month == "Outubro" || month == "Setembro" || month == "Abril" || month == "Março" || month == "Fevereiro")) {
                when (month) {
                    "Novembro", "Abril" -> 4180.0
                    "Outubro", "Março" -> 3690.5
                    else -> 4020.0
                }
            } else {
                monthTotalForUser(prevMonth)
            }
            val difference = previous - total
            val diffPercent = if (previous > 0.0) abs(difference) / previous * 100.0 else 0.0

            val summary = context.panel()
            val consolidated = context.row()
            consolidated.addView(context.label("TOTAL CONSOLIDADO ($month)", 10f, gold), LinearLayout.LayoutParams(0, -2, 1f))
            consolidated.addView(
                context.label(
                    String.format(Locale("pt", "BR"), "%s %.1f%%", if (difference >= 0) "↘" else "↗", diffPercent),
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
                    "${if (difference >= 0) "Economia" else "Aumento"} de ${money.format(abs(difference))} em relação ao mês anterior.",
                    11f,
                    gold
                ),
                8
            )

            val cardEntries = customEntries.filter { it.isCreditCard }
            val nextMonthName = allMonths[(currentIdx + 1) % allMonths.size]

            data class FutureCardItem(val dueMonth: String, val entry: CustomEntry)
            val futureCardItems = mutableListOf<FutureCardItem>()
            cardEntries.forEach { item ->
                futureCardItems.add(FutureCardItem(month, item))
            }
            for (offset in 1 until allMonths.size) {
                val mName = allMonths[(currentIdx + offset) % allMonths.size]
                getCustomExpenses(context, mName)
                    .filter { it.isCreditCard }
                    .forEach { item -> futureCardItems.add(FutureCardItem(mName, item)) }
            }
            val futureCardTotal = futureCardItems.sumOf { it.entry.amount }

            // Botão de Resumo Mensal (Receitas x Despesas) que abre a janela popup
            val monthlySummaryButton = context.label("📊  Resumo Mensal ($month)", 12.5f, Color.WHITE, true).apply {
                gravity = Gravity.CENTER
                background = context.bg(Color.rgb(35, 40, 34), 12, gold)
                minHeight = context.dp(44)
                isClickable = true
                isFocusable = true
                setOnClickListener {
                    showMonthlySummaryDialog(
                        context = context,
                        month = month,
                        totalRevenues = totalRevenuesMonth,
                        totalExpenses = total,
                        futureCardTotal = futureCardTotal
                    )
                }
            }
            body.put(context, monthlySummaryButton, 10, 44)

            val stats = context.row()
            fun stat(titleText: String, value: String, foot: String) = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                background = context.bg(Color.rgb(35, 39, 33), 12)
                setPadding(context.dp(12), context.dp(12), context.dp(12), context.dp(12))
                put(context, context.label(titleText, 10f, muted))
                put(context, context.label(value, 15f, gold, true), 8)
                put(context, context.label(foot, 10f, muted), 6)
            }
            stats.addView(
                stat("▣ Mês anterior", money.format(previous), prevMonth),
                LinearLayout.LayoutParams(0, -2, 1f).apply { rightMargin = context.dp(8) }
            )
            stats.addView(
                stat(
                    "💳 Lançamentos Futuros",
                    money.format(futureCardTotal),
                    if (futureCardItems.isEmpty()) "Venc. em $nextMonthName" else "${futureCardItems.size} parcela(s) futura(s)"
                ),
                LinearLayout.LayoutParams(0, -2, 1f)
            )
            summary.put(context, stats, 18)

            val budget = context.row()
            budget.addView(context.label("Consumo do teto mensal", 10f, gold), LinearLayout.LayoutParams(0, -2, 1f))
            budget.addView(context.label(String.format(Locale("pt", "BR"), "%.1f%% (R$ 5.000,00)", total / 50), 10f, gold, true))
            summary.put(context, budget, 18)
            summary.put(
                context,
                ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal).apply {
                    max = 5000
                    progress = total.toInt().coerceAtMost(5000)
                    progressTintList = android.content.res.ColorStateList.valueOf(gold)
                    progressBackgroundTintList = android.content.res.ColorStateList.valueOf(Color.rgb(55, 59, 53))
                },
                5,
                8
            )
            body.put(context, summary, 18)

            body.put(context, context.label("◉  Despesas por Categoria", 16f, Color.WHITE, true), 24)
            val categories = context.panel()
            val bar = context.row()
            if (total > 0.0) {
                values.forEachIndexed { i, amount ->
                    if (amount > 0.0) {
                        bar.addView(
                            View(context).apply { setBackgroundColor(expenseColors[i]) },
                            LinearLayout.LayoutParams(0, context.dp(7), amount.toFloat())
                        )
                    }
                }
            } else {
                bar.addView(
                    View(context).apply { setBackgroundColor(Color.rgb(55, 59, 53)) },
                    LinearLayout.LayoutParams(0, context.dp(7), 1f)
                )
            }
            categories.put(context, bar)

            expenseCategories.forEachIndexed { i, name ->
                val line = context.row().apply { minimumHeight = context.dp(46) }
                line.addView(context.label("●", 14f, expenseColors[i]), LinearLayout.LayoutParams(context.dp(22), -2))
                line.addView(context.label(name, 12f).apply { setPadding(0, 0, context.dp(6), 0) }, LinearLayout.LayoutParams(0, -2, 1f))
                line.addView(context.label(money.format(values[i]), 12f, Color.WHITE, true))
                val pct = if (total > 0.0) (values[i] / total * 100).toInt() else 0
                line.addView(
                    context.label("$pct%", 10f, muted).apply { gravity = Gravity.END },
                    LinearLayout.LayoutParams(context.dp(34), -2)
                )
                line.setOnClickListener {
                    val addedInCat = customEntries.filter { it.categoryIndex == i }
                    val details = if (addedInCat.isEmpty()) {
                        "Valores consolidados da categoria em $month."
                    } else {
                        val itemsText = addedInCat.joinToString("\n") { item ->
                            val tag = if (item.isCreditCard) " [💳 ${item.cardName} ${item.installmentInfo}]" else ""
                            "• ${item.description}$tag: ${money.format(item.amount)}"
                        }
                        "Lançamentos na categoria:\n$itemsText"
                    }
                    showStyledDetailsDialog(
                        context = context,
                        title = name,
                        subtitle = "$month: ${money.format(values[i])}",
                        details = details,
                        accentColor = expenseColors[i]
                    )
                }
                categories.put(context, line, if (i == 0) 14 else 0)
            }
            body.put(context, categories, 12)

            // Seção específica de Compras no Cartão de Crédito (Lançamentos Futuros e Mês de Vencimento) ABAIXO de Despesas por Categoria
            body.put(context, context.label("💳  Compras no Cartão de Crédito", 16f, Color.WHITE, true), 24)
            val cardPanel = context.panel().apply {
                background = context.bg(Color.rgb(29, 33, 29), 16, Color.rgb(90, 74, 32))
            }
            val invoiceHeader = context.row()
            invoiceHeader.addView(
                context.label("Lançamentos futuros • Mês de vencimento", 11f, gold, true),
                LinearLayout.LayoutParams(0, -2, 1f)
            )
            invoiceHeader.addView(context.label(money.format(futureCardTotal), 14f, gold, true))
            cardPanel.put(context, invoiceHeader)

            if (futureCardItems.isEmpty()) {
                cardPanel.put(
                    context,
                    context.label(
                        "Nenhum lançamento futuro cadastrado no cartão.\nCompras feitas em $month terão vencimento em $nextMonthName.",
                        11f,
                        muted
                    ),
                    10
                )
            } else {
                futureCardItems.forEachIndexed { idx, itemWrapper ->
                    val entry = itemWrapper.entry
                    val dueMonth = itemWrapper.dueMonth
                    val itemRow = context.row().apply { minimumHeight = context.dp(52) }
                    itemRow.addView(
                        context.label("💳", 13f, gold),
                        LinearLayout.LayoutParams(context.dp(26), -2)
                    )
                    val infoCol = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
                    val badgeText = if (entry.installmentInfo.isNotEmpty()) " (${entry.installmentInfo})" else ""
                    infoCol.put(context, context.label("${entry.description}$badgeText", 12f, Color.WHITE, true))
                    infoCol.put(
                        context,
                        context.label("📅 Vencimento: $dueMonth • ${entry.cardName.ifEmpty { "Cartão" }}", 10.5f, gold, true),
                        2
                    )
                    infoCol.put(
                        context,
                        context.label("Categoria: ${expenseCategories[entry.categoryIndex]}", 10f, muted),
                        1
                    )
                    itemRow.addView(infoCol, LinearLayout.LayoutParams(0, -2, 1f))
                    itemRow.addView(context.label("- ${money.format(entry.amount)}", 12f, red, true))

                    itemRow.setOnClickListener {
                        showStyledDetailsDialog(
                            context = context,
                            title = "${entry.description}$badgeText",
                            subtitle = "📅 Vencimento: $dueMonth • ${money.format(entry.amount)}",
                            details = "Cartão: ${entry.cardName.ifEmpty { "Cartão de Crédito" }}\n" +
                                "Categoria: ${expenseCategories[entry.categoryIndex]}\n" +
                                "Parcela: ${entry.installmentInfo.ifEmpty { "À vista" }}\n\n" +
                                "Deseja remover este lançamento da fatura de $dueMonth?",
                            accentColor = gold,
                            removeLabel = "Remover",
                            onRemove = {
                                val allInDueMonth = getCustomExpenses(context, dueMonth)
                                allInDueMonth.remove(entry)
                                saveCustomExpenses(context, dueMonth, allInDueMonth)
                                onRefresh()
                            }
                        )
                    }
                    cardPanel.put(context, itemRow, if (idx == 0) 12 else 8)
                }
            }
            body.put(context, cardPanel, 12)

            val nonCardEntries = customEntries.filter { !it.isCreditCard }
            if (nonCardEntries.isNotEmpty()) {
                body.put(context, context.label("◉  Outras Despesas Adicionadas ($month)", 16f, Color.WHITE, true), 22)
                val customPanel = context.panel()
                nonCardEntries.forEachIndexed { index, entry ->
                    val itemRow = context.row().apply { minimumHeight = context.dp(44) }
                    itemRow.addView(
                        context.label("●", 12f, expenseColors[entry.categoryIndex]),
                        LinearLayout.LayoutParams(context.dp(20), -2)
                    )
                    val infoCol = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
                    infoCol.put(context, context.label(entry.description, 12f, Color.WHITE, true))
                    infoCol.put(context, context.label(expenseCategories[entry.categoryIndex], 10f, muted), 2)
                    itemRow.addView(infoCol, LinearLayout.LayoutParams(0, -2, 1f))
                    itemRow.addView(context.label("- ${money.format(entry.amount)}", 12f, red, true))
                    itemRow.setOnClickListener {
                        showStyledDetailsDialog(
                            context = context,
                            title = entry.description,
                            subtitle = "$month • ${money.format(entry.amount)}",
                            details = "Categoria: ${expenseCategories[entry.categoryIndex]}\n\nDeseja remover este lançamento?",
                            accentColor = expenseColors[entry.categoryIndex],
                            removeLabel = "Remover",
                            onRemove = {
                                val updated = getCustomExpenses(context, month)
                                updated.remove(entry)
                                saveCustomExpenses(context, month, updated)
                                onRefresh()
                            }
                        )
                    }
                    customPanel.put(context, itemRow, if (index == 0) 0 else 6)
                }
                body.put(context, customPanel, 12)
            }

            val footerText = if (isDemo) {
                "Conta de demonstração • Arraste para o lado ↔"
            } else {
                "Conta: $userEmail • Arraste para o lado ↔"
            }
            body.put(
                context,
                context.label(footerText, 11f, muted).apply { gravity = Gravity.CENTER },
                20
            )
            return scroll
        }
    }
}

/**
 * Container que une o cabeçalho, o ViewPager (para trocar de tela arrastando para o lado)
 * e a barra inferior fixa (Despesas / Receitas na parte de baixo do aplicativo).
 */
class AuraDashboardView(
    private val activity: AppCompatActivity,
    initialPage: Int = 0,
    initialMonth: String = "Abril"
) {
    private val gold = Color.rgb(248, 202, 65)
    private val ink = Color.rgb(13, 17, 16)
    private val muted = Color.rgb(148, 151, 141)

    var currentMonth: String = initialMonth
        private set
    var currentPage: Int = initialPage
        private set

    val rootView: LinearLayout
    private val subtitleView: TextView
    private val viewPager: ViewPager
    private val expensesBottomBtn: TextView
    private val revenuesBottomBtn: TextView
    private val pagerAdapter: DashboardPagerAdapter

    private fun dp(n: Int) = (n * activity.resources.displayMetrics.density + .5f).toInt()

    private fun bg(color: Int, radius: Int = 16) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radius).toFloat()
    }

    private fun label(s: String, size: Float = 13f, color: Int = Color.WHITE, bold: Boolean = false) = TextView(activity).apply {
        text = s
        textSize = size
        setTextColor(color)
        includeFontPadding = false
        if (bold) typeface = Typeface.create("sans-serif", Typeface.BOLD)
    }

    init {
        rootView = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(ink)
        }

        // 1. Cabeçalho Superior
        val header = LinearLayout(activity).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(20), dp(16), dp(20), dp(10))
        }
        header.addView(
            label("A", 22f, gold, true).apply {
                gravity = Gravity.CENTER
                background = bg(Color.rgb(42, 43, 30), 10)
            },
            LinearLayout.LayoutParams(dp(42), dp(42))
        )

        val userEmail = ExpensesActivity.getCurrentUserEmail(activity)
        subtitleView = label(
            "${if (initialPage == 0) "Despesas" else "Receitas"} • $userEmail",
            10f,
            muted
        )
        header.addView(
            LinearLayout(activity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(10), 0, 0, 0)
                addView(label("Aura", 17f, Color.WHITE, true))
                addView(subtitleView, LinearLayout.LayoutParams(-2, -2).apply { topMargin = dp(3) })
            },
            LinearLayout.LayoutParams(0, -2, 1f)
        )
        header.addView(
            label("Sair", 12f, gold).apply {
                gravity = Gravity.CENTER
                minHeight = dp(44)
                minWidth = dp(48)
                setOnClickListener { activity.finish() }
            }
        )
        rootView.addView(header, LinearLayout.LayoutParams(-1, -2))

        // 2. ViewPager Central (permite arrastar para o lado entre Despesas e Receitas)
        pagerAdapter = DashboardPagerAdapter()
        viewPager = ViewPager(activity).apply {
            adapter = pagerAdapter
            addOnPageChangeListener(object : ViewPager.SimpleOnPageChangeListener() {
                override fun onPageSelected(position: Int) {
                    currentPage = position
                    updateBottomBarState(position)
                }
            })
        }
        rootView.addView(viewPager, LinearLayout.LayoutParams(-1, 0, 1f))

        // 3. Barra Inferior Fixa (Despesas e Receitas na parte de baixo do aplicativo)
        val bottomWrapper = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(ink)
            setPadding(dp(20), dp(8), dp(20), dp(14))
        }
        val bottomTabs = LinearLayout(activity).apply {
            gravity = Gravity.CENTER_VERTICAL
            background = bg(Color.rgb(26, 30, 26), 14)
            setPadding(dp(4), dp(4), dp(4), dp(4))
        }

        expensesBottomBtn = label("↘  Despesas", 12f, ink, true).apply {
            gravity = Gravity.CENTER
            minHeight = dp(44)
            setOnClickListener { viewPager.setCurrentItem(0, true) }
        }
        revenuesBottomBtn = label("↗  Receitas", 12f, Color.WHITE, true).apply {
            gravity = Gravity.CENTER
            minHeight = dp(44)
            setOnClickListener { viewPager.setCurrentItem(1, true) }
        }

        bottomTabs.addView(
            expensesBottomBtn,
            LinearLayout.LayoutParams(0, dp(44), 1f).apply { rightMargin = dp(6) }
        )
        bottomTabs.addView(
            revenuesBottomBtn,
            LinearLayout.LayoutParams(0, dp(44), 1f)
        )
        bottomWrapper.addView(bottomTabs, LinearLayout.LayoutParams(-1, -2))
        rootView.addView(bottomWrapper, LinearLayout.LayoutParams(-1, -2))

        viewPager.setCurrentItem(initialPage, false)
        updateBottomBarState(initialPage)
    }

    private fun updateBottomBarState(activePage: Int) {
        val isExpenses = activePage == 0
        val userEmail = ExpensesActivity.getCurrentUserEmail(activity)
        subtitleView.text = "${if (isExpenses) "Despesas" else "Receitas"} • $userEmail"

        expensesBottomBtn.background = bg(if (isExpenses) gold else Color.rgb(35, 39, 35), 11)
        expensesBottomBtn.setTextColor(if (isExpenses) ink else Color.WHITE)

        revenuesBottomBtn.background = bg(if (!isExpenses) gold else Color.rgb(35, 39, 35), 11)
        revenuesBottomBtn.setTextColor(if (!isExpenses) ink else Color.WHITE)
    }

    private inner class DashboardPagerAdapter : PagerAdapter() {
        override fun getCount(): Int = 2

        override fun isViewFromObject(view: View, obj: Any): Boolean = view === obj

        override fun getItemPosition(obj: Any): Int = POSITION_NONE

        override fun instantiateItem(container: ViewGroup, position: Int): Any {
            val pageView = if (position == 0) {
                ExpensesActivity.buildExpensesPage(
                    context = activity,
                    month = currentMonth,
                    onMonthSelected = { selected ->
                        currentMonth = selected
                        notifyDataSetChanged()
                    },
                    onRefresh = { notifyDataSetChanged() }
                )
            } else {
                RevenuesActivity.buildRevenuesPage(
                    context = activity,
                    month = currentMonth,
                    onMonthSelected = { selected ->
                        currentMonth = selected
                        notifyDataSetChanged()
                    },
                    onRefresh = { notifyDataSetChanged() }
                )
            }
            container.addView(pageView, ViewGroup.LayoutParams(-1, -1))
            return pageView
        }

        override fun destroyItem(container: ViewGroup, position: Int, obj: Any) {
            container.removeView(obj as View)
        }
    }
}
