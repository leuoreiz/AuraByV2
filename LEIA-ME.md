# Aura Finanças

Abra MyApplication no Android Studio, sincronize o Gradle e execute app.

Conta de demonstração local:
- E-mail: teste@aura.com
- Senha: Aura1234

O login abre a segunda tela de despesas. Os meses Abril, Março e Fevereiro
alteram resumo e categorias. Toque nas categorias para consultar os valores.
Sair retorna ao login. Todos os valores são fictícios; nenhuma conta real
ou serviço remoto é criado. A senha de teste no código serve apenas à demonstração.

Ambas as telas são feitas com Views nativas em Kotlin, sem Compose ou layout XML.
MainActivity.kt: login. ExpensesActivity.kt: painel de despesas.
O projeto aninhado AuraBy, presente no ZIP original, recebeu as mesmas mudanças.

A compilação não foi verificada aqui porque o ambiente não consegue baixar o
Gradle. Execute o build no Android Studio antes de distribuir o aplicativo.
