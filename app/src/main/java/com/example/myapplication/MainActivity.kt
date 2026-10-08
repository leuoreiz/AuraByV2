package com.example.myapplication

import android.content.Intent
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.InputType
import android.text.method.PasswordTransformationMethod
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

/** Login feito com Views nativas. Nenhum layout XML ou Compose é utilizado. */
class MainActivity : AppCompatActivity() {
    private val yellow = Color.rgb(248, 202, 65)
    private val white = Color.rgb(241, 241, 238)
    private lateinit var email: EditText
    private lateinit var password: EditText
    private var passwordVisible = false
    private fun dp(value: Int) = (value * resources.displayMetrics.density + .5f).toInt()
    private fun background(color: Int, radius: Int, border: Int? = null) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radius).toFloat()
        border?.let { setStroke(dp(1), it) }
    }
    private fun text(value: String, size: Float, color: Int = white, bold: Boolean = false) = TextView(this).apply {
        text = value
        textSize = size
        setTextColor(color)
        if (bold) typeface = Typeface.create("sans-serif", Typeface.BOLD)
        includeFontPadding = false
    }
    private fun LinearLayout.add(view: View, height: Int = -2, top: Int = 0) {
        addView(view, LinearLayout.LayoutParams(-1, if (height < 0) height else dp(height)).apply { topMargin = dp(top) })
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(19, 20, 16)
        window.navigationBarColor = Color.rgb(19, 20, 16)
        window.decorView.systemUiVisibility = 0
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        val scroll = ScrollView(this).apply {
            isFillViewport = true
            setBackgroundColor(Color.rgb(19, 20, 16))
        }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(20), dp(24), dp(20), dp(28))
        }
        scroll.addView(content)
        val logo = text("A", 30f, yellow, true).apply {
            gravity = Gravity.CENTER
            background = background(Color.rgb(31, 32, 25), 15, Color.rgb(104, 90, 36))
            contentDescription = "Aura Finanças"
        }
        content.addView(logo, LinearLayout.LayoutParams(dp(64), dp(64)))
        content.add(text("Aura Finanças", 24f, white, true).apply { gravity = Gravity.CENTER }, top = 20)
        content.add(text("Gestão patrimonial inteligente", 11f, yellow).apply { gravity = Gravity.CENTER }, top = 5)

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(24), dp(20), dp(24))
            background = background(Color.rgb(26, 29, 26), 20)
        }
        content.add(card, top = 24)
        card.add(text("Bem-vindo de volta", 20f, white, true))
        card.add(text("Acesse sua conta para gerenciar suas finanças", 11f, yellow), top = 6)
        card.add(text("E-mail", 11f, yellow), top = 24)
        email = input("seu.email@exemplo.com", false)
        card.add(field(email, "mail"), 44, 7)
        card.add(text("Senha", 11f, yellow), top = 16)
        password = input("••••••••", true)
        card.add(field(password, "lock"), 44, 7)

        val options = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        val remember = CheckBox(this).apply {
            text = "Lembrar de mim"
            textSize = 10f
            setTextColor(yellow)
            buttonTintList = android.content.res.ColorStateList.valueOf(yellow)
            minHeight = dp(48)
            setPadding(0, 0, 0, 0)
            isChecked = getPreferences(MODE_PRIVATE).getBoolean("remember", false)
            setOnCheckedChangeListener { _, checked ->
                getPreferences(MODE_PRIVATE).edit().putBoolean("remember", checked).apply()
            }
        }
        options.addView(remember, LinearLayout.LayoutParams(0, dp(48), 1f))
        options.addView(text("Esqueci minha senha", 10f, yellow, true).apply {
            gravity = Gravity.CENTER
            minHeight = dp(48)
            isClickable = true
            setOnClickListener {
                androidx.appcompat.app.AlertDialog.Builder(this@MainActivity)
                    .setTitle("Recuperar senha")
                    .setMessage("A recuperação de senha será disponibilizada após a conexão com o serviço de autenticação.")
                    .setPositiveButton("Entendi", null).show()
            }
        })
        card.add(options, top = 6)
        val enter = text("Entrar  →", 14f, Color.BLACK, true).apply {
            gravity = Gravity.CENTER
            background = background(yellow, 11)
            isClickable = true
            isFocusable = true
            setOnClickListener { validate() }
        }
        card.add(enter, 48, 2)
        password.setOnEditorActionListener { _, action, _ ->
            if (action == EditorInfo.IME_ACTION_DONE) { validate(); true } else false
        }
        setContentView(scroll)
    }
    private fun input(hintText: String, secret: Boolean) = EditText(this).apply {
        hint = hintText
        textSize = 13f
        setTextColor(white)
        setHintTextColor(Color.rgb(89, 82, 55))
        background = null
        setPadding(dp(10), 0, dp(4), 0)
        isSingleLine = true
        inputType = if (secret) InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            else InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        if (secret) transformationMethod = PasswordTransformationMethod.getInstance()
        imeOptions = if (secret) EditorInfo.IME_ACTION_DONE else EditorInfo.IME_ACTION_NEXT
        contentDescription = if (secret) "Senha" else "E-mail"
    }
    private fun field(input: EditText, icon: String): LinearLayout = LinearLayout(this).apply {
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(12), 0, dp(8), 0)
        background = background(Color.rgb(31, 34, 30), 12)
        addView(LineIcon(this@MainActivity, icon), LinearLayout.LayoutParams(dp(18), dp(18)))
        addView(input, LinearLayout.LayoutParams(0, -1, 1f))
        if (icon == "lock") {
            addView(LineIcon(this@MainActivity, "eye").apply {
                contentDescription = "Mostrar senha"
                isClickable = true
                isFocusable = true
                setPadding(dp(12), dp(12), dp(12), dp(12))
                setOnClickListener {
                    passwordVisible = !passwordVisible
                    input.transformationMethod = if (passwordVisible) null else PasswordTransformationMethod.getInstance()
                    input.setSelection(input.text.length)
                    contentDescription = if (passwordVisible) "Ocultar senha" else "Mostrar senha"
                    invalidate()
                }
            }, LinearLayout.LayoutParams(dp(44), dp(44)))
        }
    }
    private fun validate() {
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email.text.toString().trim()).matches()) {
            email.error = "Informe um e-mail válido"
            email.requestFocus()
        } else if (password.text.isNullOrEmpty()) {
            password.error = "Informe sua senha"
            password.requestFocus()
        } else {
            if (email.text.toString().trim().equals("teste@aura.com", ignoreCase = true) && password.text.toString() == "Aura1234") {
                password.text.clear()
                startActivity(Intent(this, ExpensesActivity::class.java))
            } else {
                password.error = "E-mail ou senha incorretos"
                Toast.makeText(this, "Conta de teste: teste@aura.com / Aura1234", Toast.LENGTH_LONG).show()
            }
        }
    }
    /** Ícones vetoriais desenhados em Kotlin, sem imagens externas. */
    private inner class LineIcon(context: Context, private val kind: String) : View(context) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = yellow
            style = Paint.Style.STROKE
            strokeWidth = 1.7f
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            canvas.save()
            canvas.translate(paddingLeft.toFloat(), paddingTop.toFloat())
            canvas.scale((width - paddingLeft - paddingRight) / 24f, (height - paddingTop - paddingBottom) / 24f)
            when (kind) {
                "mail" -> {
                    canvas.drawRoundRect(3f, 5f, 21f, 19f, 2f, 2f, paint)
                    canvas.drawPath(Path().apply { moveTo(3f, 6f); lineTo(12f, 13f); lineTo(21f, 6f) }, paint)
                }
                "lock" -> {
                    canvas.drawRoundRect(6f, 10f, 18f, 21f, 1f, 1f, paint)
                    canvas.drawArc(8f, 3f, 16f, 16f, 180f, 180f, false, paint)
                    canvas.drawLine(12f, 14f, 12f, 17f, paint)
                }
                "eye" -> {
                    canvas.drawPath(Path().apply {
                        moveTo(2f, 12f); cubicTo(7f, 4f, 17f, 4f, 22f, 12f)
                        cubicTo(17f, 20f, 7f, 20f, 2f, 12f); close()
                    }, paint)
                    canvas.drawCircle(12f, 12f, 3f, paint)
                    if (passwordVisible) canvas.drawLine(3f, 3f, 21f, 21f, paint)
                }
            }
            canvas.restore()
        }
    }
}
