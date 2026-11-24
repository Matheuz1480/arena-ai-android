# Arena AI — Aplicativo para Android 📱

Aplicativo Android nativo que abre o **[arena.ai](https://arena.ai/)** como um app dedicado, com **modo anônimo com limite**, ícone próprio, navegação por gestos, barra de progresso, atualizar puxando para baixo e tratamento de erros de conexão.

## ✨ Recursos

- 🕵️ **Modo convidado (anônimo)** — tipo aba anônima: cookies, cache e histórico são apagados; **5 sessões de 10 minutos** com cronômetro visível
- 🔐 **Login libera tudo** — ao atingir o limite, o app pede login; depois disso, uso ilimitado
- 🌐 Abre `https://arena.ai/` em tela cheia (WebView otimizada)
- ⬅️ Botão voltar navega no histórico do site
- 🔄 Atualizar a página puxando para baixo (*pull to refresh*)
- 📊 Barra de progresso de carregamento
- 📶 Tela de erro amigável com botão "Tentar novamente"
- 📥 Suporte a downloads de arquivos
- 📷 Permissão de câmera/microfone liberada para o domínio `arena.ai`
- 🔗 Links `arena.ai` abertos em outros apps abrem direto neste app
- 🌙 Tema escuro e visual *edge-to-edge*
- 🗣️ Interface em português e inglês (automático pelo idioma do celular)

## 📥 Download

Baixe o APK pronto na aba **Releases** do repositório:
https://github.com/Matheuz1480/arena-ai-android/releases

## 📲 Como instalar no seu celular

1. Baixe o arquivo **`ArenaAI-v1.1.apk`** para o celular (pelo Google Drive, e-mail ou cabo USB).
2. Toque no arquivo baixado.
3. Se aparecer "Instalar apps desconhecidos", permita a instalação nas configurações e continue.
4. Pronto! O ícone **Arena AI** aparece na tela inicial.

> **Requisitos:** Android 7.0 (Nougat) ou superior. Se já tiver a v1.0 instalada, a atualização instala por cima (mesmo keystore).

## 👤 Como funciona o modo anônimo

1. Ao abrir o app, escolha **"Continuar como convidado"**.
2. Você tem **5 sessões de 10 minutos** (cronômetro no topo da tela).
3. Nada é salvo: cookies, cache e histórico são apagados a cada sessão (como uma aba anônima).
4. Quando o tempo de uma sessão acaba, o app oferece **mais uma sessão** (se restarem) ou pede o **login**.
5. Depois do login (toque em "✓ Terminei o login" após entrar no site), o uso passa a ser **ilimitado**.

## ⚙️ Personalização

| O que | Onde |
|---|---|
| Minutos por sessão | `GuestSessionManager.ANON_SESSION_MINUTES` (padrão: 10) |
| Quantidade de sessões | `GuestSessionManager.ANON_SESSION_LIMIT` (padrão: 5) |
| Site aberto | `MainActivity.HOME_URL` |
| Versão | `app/build.gradle.kts` (`versionCode` / `versionName`) |
| Nome do app | `res/values/strings.xml` (`app_name`) |

## 🛠️ Como compilar (para desenvolvedores)

### Opção 1 — Android Studio
1. Abra a pasta `ArenaApp` no Android Studio.
2. Aguarde a sincronização do Gradle.
3. `Build > Build Bundle(s) / APK(s) > Build APK(s)`.

### Opção 2 — Linha de comando
```bash
# Requisitos: JDK 17 + Android SDK (platform 35, build-tools 35)
./gradlew assembleRelease
# APK em: app/build/outputs/apk/release/app-release.apk
```

### Opção 3 — GitHub Actions
Toda vez que você fizer push em `main`, a workflow compila o APK automaticamente (aba Actions).

## 🧪 Testes

```bash
./gradlew testDebugUnitTest
```

**21 testes funcionais** (Robolectric) cobrindo WebView, modo anônimo com limite, fluxo de login e navegação. Detalhes em [TEST-REPORT.md](TEST-REPORT.md).

## 🗂️ Estrutura do projeto

```
ArenaApp/
├── app/src/main/
│   ├── java/ai/arena/app/
│   │   ├── WelcomeActivity.kt       # Tela de escolha (convidado vs login)
│   │   ├── MainActivity.kt          # WebView + modo anônimo + login
│   │   └── GuestSessionManager.kt   # Política: 5 sessões de 10 min
│   └── res/...                      # Layouts, ícones, cores, textos
├── app/src/test/                    # 21 testes funcionais
├── releases/ArenaAI-v1.1.apk        # APK pronto ✅
├── arena-release.jks                # Keystore (guarde com segurança!)
└── publicar-no-github.sh            # Publica repo + Release no GitHub
```

## 📝 Observações

- **Login:** o app usa uma identificação de navegador Chrome para que o login
  (inclusive "Entrar com Google") funcione dentro do app.
- **Contador anônimo:** apenas o número de sessões usadas é salvo no app
  (para o limite funcionar) — nenhum dado de navegação é mantido.
- **Keystore:** o APK é assinado com `arena-release.jks` (senha: `arena123`).
  Mantenha-o em local seguro — sem ele, futuros APKs não poderão atualizar
  esta instalação.

---

Feito com Android (Kotlin) · minSdk 24 · targetSdk 35
