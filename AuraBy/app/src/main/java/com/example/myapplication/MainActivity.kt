package com.example.myapplication

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.InputType
import android.text.method.PasswordTransformationMethod
import android.util.Patterns
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject

/** Login e Cadastro completo feitos com Views nativas. Nenhum layout XML ou Compose é utilizado. */
class MainActivity : AppCompatActivity() {
    private val yellow = Color.rgb(248, 202, 65)
    private val white = Color.rgb(241, 241, 238)
    private val darkBg = Color.rgb(19, 20, 16)
    private val cardBg = Color.rgb(26, 29, 26)
    private val fieldBg = Color.rgb(31, 34, 30)
    private val muted = Color.rgb(148, 151, 141)

    private lateinit var email: EditText
    private lateinit var password: EditText
    private lateinit var confirmPassword: EditText
    private lateinit var confirmPasswordLabel: TextView
    private lateinit var confirmPasswordField: LinearLayout
    private lateinit var optionsRow: LinearLayout
    private lateinit var rememberCheck: CheckBox
    private lateinit var titleText: TextView
    private lateinit var subtitleText: TextView
    private lateinit var submitButton: TextView
    private lateinit var loginTabBtn: TextView
    private lateinit var registerTabBtn: TextView
    private lateinit var footerPromptText: TextView
    private lateinit var footerActionText: TextView

    private var isRegisterMode = false
    private var passwordVisible = false
    private var confirmPasswordVisible = false

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
        window.statusBarColor = darkBg
        window.navigationBarColor = darkBg
        window.decorView.systemUiVisibility = 0
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        isRegisterMode = savedInstanceState?.getBoolean("isRegisterMode", false) ?: false

        val scroll = ScrollView(this).apply {
            isFillViewport = true
            setBackgroundColor(darkBg)
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
            setPadding(dp(20), dp(22), dp(20), dp(24))
            background = background(cardBg, 20)
        }
        content.add(card, top = 24)

        // Abas superiores: Entrar | Cadastrar
        val modeTabs = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            background = background(Color.rgb(19, 22, 19), 12)
            setPadding(dp(4), dp(4), dp(4), dp(4))
        }
        loginTabBtn = text("Entrar", 12f, Color.BLACK, true).apply {
            gravity = Gravity.CENTER
            minHeight = dp(40)
            isClickable = true
            isFocusable = true
            setOnClickListener { setMode(false) }
        }
        registerTabBtn = text("Cadastrar", 12f, white, true).apply {
            gravity = Gravity.CENTER
            minHeight = dp(40)
            isClickable = true
            isFocusable = true
            setOnClickListener { setMode(true) }
        }
        modeTabs.addView(loginTabBtn, LinearLayout.LayoutParams(0, dp(40), 1f).apply { rightMargin = dp(4) })
        modeTabs.addView(registerTabBtn, LinearLayout.LayoutParams(0, dp(40), 1f))
        card.add(modeTabs, top = 0)

        titleText = text("Bem-vindo de volta", 20f, white, true)
        subtitleText = text("Acesse sua conta para gerenciar suas finanças", 11f, yellow)
        card.add(titleText, top = 18)
        card.add(subtitleText, top = 6)

        card.add(text("E-mail", 11f, yellow), top = 20)
        email = input("seu.email@exemplo.com", false)
        card.add(field(email, "mail", isConfirmField = false), 44, 7)

        card.add(text("Senha", 11f, yellow), top = 16)
        password = input("••••••••", true)
        card.add(field(password, "lock", isConfirmField = false), 44, 7)

        confirmPasswordLabel = text("Confirmar senha", 11f, yellow)
        confirmPassword = input("Repita sua senha", true).apply {
            contentDescription = "Confirmar senha"
        }
        confirmPasswordField = field(confirmPassword, "lock", isConfirmField = true)
        card.add(confirmPasswordLabel, top = 16)
        card.add(confirmPasswordField, 44, 7)

        val prefs = getSharedPreferences("aura_finance_prefs", Context.MODE_PRIVATE)
        val rememberSaved = prefs.getBoolean("remember", false)
        val savedEmail = prefs.getString("remembered_email", "") ?: ""
        if (rememberSaved && savedEmail.isNotEmpty()) {
            email.setText(savedEmail)
        }

        optionsRow = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        rememberCheck = CheckBox(this).apply {
            text = "Lembrar de mim"
            textSize = 10f
            setTextColor(yellow)
            buttonTintList = ColorStateList.valueOf(yellow)
            minHeight = dp(48)
            setPadding(0, 0, 0, 0)
            isChecked = rememberSaved
            setOnCheckedChangeListener { _, checked ->
                prefs.edit().putBoolean("remember", checked).apply()
                if (!checked) {
                    prefs.edit().remove("remembered_email").apply()
                }
            }
        }
        optionsRow.addView(rememberCheck, LinearLayout.LayoutParams(0, dp(48), 1f))
        optionsRow.addView(text("Esqueci minha senha", 10f, yellow, true).apply {
            gravity = Gravity.CENTER
            minHeight = dp(48)
            isClickable = true
            setOnClickListener { showForgotPasswordDialog() }
        })
        card.add(optionsRow, top = 6)

        submitButton = text("Entrar  →", 14f, Color.BLACK, true).apply {
            gravity = Gravity.CENTER
            background = background(yellow, 11)
            isClickable = true
            isFocusable = true
            setOnClickListener { handleSubmit() }
        }
        card.add(submitButton, 48, 8)

        val footerRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, dp(14), 0, dp(2))
        }
        footerPromptText = text("Não tem uma conta? ", 11f, muted, false)
        footerActionText = text("Cadastre-se", 11f, yellow, true).apply {
            isClickable = true
            isFocusable = true
            setPadding(dp(4), dp(6), dp(4), dp(6))
            setOnClickListener { setMode(!isRegisterMode) }
        }
        footerRow.addView(footerPromptText)
        footerRow.addView(footerActionText)
        card.add(footerRow, top = 6)

        password.setOnEditorActionListener { _, action, _ ->
            if (!isRegisterMode && action == EditorInfo.IME_ACTION_DONE) {
                handleSubmit()
                true
            } else false
        }
        confirmPassword.setOnEditorActionListener { _, action, _ ->
            if (isRegisterMode && action == EditorInfo.IME_ACTION_DONE) {
                handleSubmit()
                true
            } else false
        }

        setMode(isRegisterMode, clearErrors = false)
        setContentView(scroll)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("isRegisterMode", isRegisterMode)
    }

    private fun setMode(register: Boolean, clearErrors: Boolean = true) {
        isRegisterMode = register
        if (clearErrors) {
            email.error = null
            password.error = null
            confirmPassword.error = null
        }

        loginTabBtn.background = if (!register) background(yellow, 9) else null
        loginTabBtn.setTextColor(if (!register) Color.BLACK else white)

        registerTabBtn.background = if (register) background(yellow, 9) else null
        registerTabBtn.setTextColor(if (register) Color.BLACK else white)

        if (register) {
            titleText.text = "Criar nova conta"
            subtitleText.text = "Cadastre seu e-mail e senha para começar"
            password.hint = "Mínimo de 6 caracteres"
            password.imeOptions = EditorInfo.IME_ACTION_NEXT
            confirmPasswordLabel.visibility = View.VISIBLE
            confirmPasswordField.visibility = View.VISIBLE
            confirmPassword.imeOptions = EditorInfo.IME_ACTION_DONE
            optionsRow.visibility = View.GONE
            submitButton.text = "Criar conta  →"
            footerPromptText.text = "Já possui uma conta? "
            footerActionText.text = "Entrar"
        } else {
            titleText.text = "Bem-vindo de volta"
            subtitleText.text = "Acesse sua conta para gerenciar suas finanças"
            password.hint = "••••••••"
            password.imeOptions = EditorInfo.IME_ACTION_DONE
            confirmPasswordLabel.visibility = View.GONE
            confirmPasswordField.visibility = View.GONE
            confirmPassword.text.clear()
            optionsRow.visibility = View.VISIBLE
            submitButton.text = "Entrar  →"
            footerPromptText.text = "Não tem uma conta? "
            footerActionText.text = "Cadastre-se"
        }
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

    private fun field(input: EditText, icon: String, isConfirmField: Boolean): LinearLayout = LinearLayout(this).apply {
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(12), 0, dp(8), 0)
        background = background(fieldBg, 12)
        addView(LineIcon(this@MainActivity, icon, isConfirmField), LinearLayout.LayoutParams(dp(18), dp(18)))
        addView(input, LinearLayout.LayoutParams(0, -1, 1f))
        if (icon == "lock") {
            addView(LineIcon(this@MainActivity, "eye", isConfirmField).apply {
                contentDescription = "Mostrar senha"
                isClickable = true
                isFocusable = true
                setPadding(dp(12), dp(12), dp(12), dp(12))
                setOnClickListener {
                    if (isConfirmField) {
                        confirmPasswordVisible = !confirmPasswordVisible
                        input.transformationMethod = if (confirmPasswordVisible) null else PasswordTransformationMethod.getInstance()
                        input.setSelection(input.text.length)
                        contentDescription = if (confirmPasswordVisible) "Ocultar senha" else "Mostrar senha"
                    } else {
                        passwordVisible = !passwordVisible
                        input.transformationMethod = if (passwordVisible) null else PasswordTransformationMethod.getInstance()
                        input.setSelection(input.text.length)
                        contentDescription = if (passwordVisible) "Ocultar senha" else "Mostrar senha"
                    }
                    invalidate()
                }
            }, LinearLayout.LayoutParams(dp(44), dp(44)))
        }
    }

    private fun getRegisteredAccounts(): MutableMap<String, String> {
        val accounts = mutableMapOf<String, String>()
        // Conta padrão de demonstração
        accounts["teste@aura.com"] = "Aura1234"

        val prefs = getSharedPreferences("aura_finance_prefs", Context.MODE_PRIVATE)
        val raw = prefs.getString("registered_users", "{}") ?: "{}"
        try {
            val json = JSONObject(raw)
            val keys = json.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                accounts[key.lowercase().trim()] = json.getString(key)
            }
        } catch (_: Exception) {
        }
        return accounts
    }

    private fun saveAccount(userEmail: String, userPassword: String) {
        val prefs = getSharedPreferences("aura_finance_prefs", Context.MODE_PRIVATE)
        val raw = prefs.getString("registered_users", "{}") ?: "{}"
        val json = try {
            JSONObject(raw)
        } catch (_: Exception) {
            JSONObject()
        }
        json.put(userEmail.lowercase().trim(), userPassword)
        prefs.edit().putString("registered_users", json.toString()).apply()
    }

    private fun handleSubmit() {
        if (isRegisterMode) {
            registerUser()
        } else {
            loginUser()
        }
    }

    private fun registerUser() {
        val emailStr = email.text.toString().trim()
        val passStr = password.text.toString()
        val confirmStr = confirmPassword.text.toString()

        if (!Patterns.EMAIL_ADDRESS.matcher(emailStr).matches()) {
            email.error = "Informe um e-mail válido"
            email.requestFocus()
            return
        }
        if (passStr.isEmpty()) {
            password.error = "Crie uma senha"
            password.requestFocus()
            return
        }
        if (passStr.length < 6) {
            password.error = "A senha deve ter pelo menos 6 caracteres"
            password.requestFocus()
            return
        }
        if (confirmStr.isEmpty()) {
            confirmPassword.error = "Confirme sua senha"
            confirmPassword.requestFocus()
            return
        }
        if (passStr != confirmStr) {
            confirmPassword.error = "As senhas não coincidem"
            confirmPassword.requestFocus()
            return
        }

        val accounts = getRegisteredAccounts()
        val normalizedEmail = emailStr.lowercase()
        if (accounts.containsKey(normalizedEmail)) {
            email.error = "Este e-mail já está cadastrado"
            email.requestFocus()
            Toast.makeText(this, "Este e-mail já possui conta. Faça login!", Toast.LENGTH_SHORT).show()
            return
        }

        saveAccount(normalizedEmail, passStr)

        val prefs = getSharedPreferences("aura_finance_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putString("current_user_email", normalizedEmail)
            .apply {
                if (rememberCheck.isChecked) {
                    putString("remembered_email", emailStr)
                }
            }
            .apply()

        Toast.makeText(this, "Cadastro realizado com sucesso!", Toast.LENGTH_SHORT).show()
        password.text.clear()
        confirmPassword.text.clear()
        setMode(false)
        email.setText(emailStr)
        startActivity(Intent(this, ExpensesActivity::class.java))
    }

    private fun loginUser() {
        val emailStr = email.text.toString().trim()
        val passStr = password.text.toString()

        if (!Patterns.EMAIL_ADDRESS.matcher(emailStr).matches()) {
            email.error = "Informe um e-mail válido"
            email.requestFocus()
            return
        }
        if (passStr.isEmpty()) {
            password.error = "Informe sua senha"
            password.requestFocus()
            return
        }

        val accounts = getRegisteredAccounts()
        val normalizedEmail = emailStr.lowercase()
        val savedPassword = accounts[normalizedEmail]

        if (savedPassword != null && savedPassword == passStr) {
            val prefs = getSharedPreferences("aura_finance_prefs", Context.MODE_PRIVATE)
            prefs.edit().apply {
                putString("current_user_email", normalizedEmail)
                if (rememberCheck.isChecked) {
                    putString("remembered_email", emailStr)
                } else {
                    remove("remembered_email")
                }
            }.apply()

            password.text.clear()
            startActivity(Intent(this, ExpensesActivity::class.java))
        } else if (savedPassword == null) {
            email.error = "Conta não encontrada"
            Toast.makeText(
                this,
                "E-mail não cadastrado. Toque em 'Cadastrar' para criar sua conta!",
                Toast.LENGTH_LONG
            ).show()
        } else {
            password.error = "Senha incorreta"
            password.requestFocus()
        }
    }

    private fun showForgotPasswordDialog() {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(20), dp(22), dp(14))
            background = background(cardBg, 16)
        }
        container.add(text("Redefinir senha", 18f, white, true))
        container.add(text("Informe seu e-mail cadastrado e defina uma nova senha", 11f, yellow), top = 4)

        container.add(text("E-mail da conta", 11f, yellow), top = 16)
        val resetEmailInput = input("seu.email@exemplo.com", false).apply {
            background = background(fieldBg, 10)
            setPadding(dp(12), dp(10), dp(12), dp(10))
            setText(email.text.toString().trim())
        }
        container.add(resetEmailInput, 44, 6)

        container.add(text("Nova senha", 11f, yellow), top = 14)
        val newPassInput = input("Mínimo de 6 caracteres", true).apply {
            background = background(fieldBg, 10)
            setPadding(dp(12), dp(10), dp(12), dp(10))
        }
        container.add(newPassInput, 44, 6)

        val dialog = AlertDialog.Builder(this)
            .setView(container)
            .create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val actions = LinearLayout(this).apply { gravity = Gravity.END }
        val cancelBtn = text("Cancelar", 12f, muted, true).apply {
            gravity = Gravity.CENTER
            setPadding(dp(14), dp(10), dp(14), dp(10))
            setOnClickListener { dialog.dismiss() }
        }
        val saveBtn = text("Atualizar senha", 12f, Color.BLACK, true).apply {
            gravity = Gravity.CENTER
            background = background(yellow, 10)
            setPadding(dp(16), dp(10), dp(16), dp(10))
            setOnClickListener {
                val targetEmail = resetEmailInput.text.toString().trim().lowercase()
                val newPass = newPassInput.text.toString()
                if (!Patterns.EMAIL_ADDRESS.matcher(targetEmail).matches()) {
                    resetEmailInput.error = "Informe um e-mail válido"
                    return@setOnClickListener
                }
                val accounts = getRegisteredAccounts()
                if (!accounts.containsKey(targetEmail)) {
                    resetEmailInput.error = "E-mail não cadastrado"
                    return@setOnClickListener
                }
                if (newPass.length < 6) {
                    newPassInput.error = "Mínimo de 6 caracteres"
                    return@setOnClickListener
                }
                saveAccount(targetEmail, newPass)
                email.setText(targetEmail)
                dialog.dismiss()
                Toast.makeText(this@MainActivity, "Senha atualizada com sucesso!", Toast.LENGTH_SHORT).show()
            }
        }
        actions.addView(cancelBtn)
        actions.addView(saveBtn, LinearLayout.LayoutParams(-2, -2).apply { leftMargin = dp(8) })
        container.add(actions, top = 20)

        dialog.show()
    }

    /** Ícones vetoriais desenhados em Kotlin, sem imagens externas. */
    private inner class LineIcon(
        context: Context,
        private val kind: String,
        private val isConfirmField: Boolean = false
    ) : View(context) {
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
                    val visible = if (isConfirmField) confirmPasswordVisible else passwordVisible
                    if (visible) canvas.drawLine(3f, 3f, 21f, 21f, paint)
                }
            }
            canvas.restore()
        }
    }
}
