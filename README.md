# Arena AI — Aplicativo para Android 📱

Aplicativo Android nativo que abre o **[arena.ai](https://arena.ai/)** como um app dedicado, com ícone próprio na tela inicial, navegação por gestos, barra de progresso, atualizar puxando para baixo e tratamento de erros de conexão.

## ✨ Recursos

- 🌐 Abre `https://arena.ai/` em tela cheia (WebView otimizada)
- 🔐 Login funciona normalmente (incluindo "Entrar com Google")
- ⬅️ Botão voltar navega no histórico do site
- 🔄 Atualizar a página puxando para baixo (*pull to refresh*)
- 📊 Barra de progresso de carregamento
- 📶 Tela de erro amigável com botão "Tentar novamente" quando estiver sem internet
- 📥 Suporte a downloads de arquivos
- 📷 Permissão de câmera/microfone liberada para o domínio `arena.ai`
- 🔗 Links `arena.ai` abertos em outros apps abrem direto neste app
- 🌙 Tema escuro e visual *edge-to-edge*
- 🗣️ Interface em português e inglês (automático pelo idioma do celular)

## 📥 Download

Baixe o APK pronto na aba **Releases** do repositório:
https://github.com/Matheuz1480/arena-ai-android/releases

## 📲 Como instalar no seu celular

1. Baixe o arquivo **`ArenaAI-v1.0.apk`** para o celular (por exemplo, pelo Google Drive, e-mail ou cabo USB).
2. Toque no arquivo baixado.
3. Se aparecer "Instalar apps desconhecidos", permita a instalação nas configurações e continue.
4. Pronto! O ícone **Arena AI** aparece na tela inicial.

> **Requisitos:** Android 7.0 (Nougat) ou superior.

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

### Opção 3 — GitHub Actions (sem instalar nada)
1. Envie esta pasta para um repositório no GitHub.
2. A workflow `.github/workflows/build-apk.yml` compila automaticamente.
3. Baixe o APK em *Actions > Build APK > Artifacts*.

## 🗂️ Estrutura do projeto

```
ArenaApp/
├── app/src/main/
│   ├── java/ai/arena/app/MainActivity.kt   # Toda a lógica do app
│   ├── res/layout/activity_main.xml        # Interface
│   └── res/...                             # Ícones, cores, textos
├── releases/ArenaAI-v1.0.apk               # APK pronto ✅
├── arena-release.jks                       # Keystore (guarde com segurança!)
└── keystore.properties                     # Senha do keystore
```

## ⚙️ Personalização

- **Trocar o site:** em `MainActivity.kt`, altere a constante `HOME_URL`.
- **Versão:** em `app/build.gradle.kts`, altere `versionCode` e `versionName`.
- **Nome do app:** em `res/values/strings.xml`, altere `app_name`.

## 📝 Observações

- **Login:** o app usa uma identificação de navegador Chrome para que o login
  (inclusive "Entrar com Google") funcione dentro do app. Se aparecer algum
  bloqueio do Google, use o login por e-mail/senha do próprio arena.ai.
- **Atualizações:** para atualizar o app no futuro, basta gerar um novo APK com
  `versionCode` maior e instalá-lo por cima (mesmo keystore).
- **Keystore:** o arquivo `arena-release.jks` (senha: `arena123`) foi usado para
  assinar o APK. Mantenha-o em local seguro — sem ele, futuros APKs não poderão
  atualizar esta instalação.

---

Feito com Android (Kotlin) · minSdk 24 · targetSdk 35
