# Aura Finanças

Abra AuraByV2 no Android Studio, sincronize o Gradle e execute app.

Conta de demonstração local (ou crie sua própria conta na aba **Cadastrar**):
- E-mail: teste@aura.com
- Senha: Aura1234

Na tela inicial (`MainActivity.kt`), é possível alternar entre **Entrar** e **Cadastrar** (ou tocar em **Cadastre-se** no rodapé) para criar novas contas com e-mail, senha e confirmação de senha, que ficam salvas localmente no dispositivo. Também é possível redefinir a senha em **Esqueci minha senha**.

O login abre a tela de despesas, com abas superiores para alternar entre **Despesas** e **Receitas**.
Os meses Abril, Março e Fevereiro alteram resumo e categorias em ambas as telas.
- Na tela de **Despesas** (`ExpensesActivity.kt`), há o botão **+ Adicionar Despesa** funcional para cadastrar novas saídas por descrição, valor e categoria.
- Na tela de **Receitas** (`RevenuesActivity.kt`), há o botão **+ Adicionar Receita** funcional para cadastrar novas entradas na conta por descrição, valor e categoria.
Toque nas categorias para consultar os valores detalhados ou nos lançamentos adicionados para gerenciá-los.
Sair retorna ao login. Todos os valores base são fictícios; nenhuma conta real
ou serviço remoto é criado. A senha de teste no código serve apenas à demonstração.

Todas as telas são feitas com Views nativas em Kotlin, sem Compose ou layout XML.
MainActivity.kt: login. ExpensesActivity.kt: painel de despesas. RevenuesActivity.kt: painel de receitas.
O projeto aninhado AuraBy, presente no ZIP original, recebeu as mesmas mudanças.
