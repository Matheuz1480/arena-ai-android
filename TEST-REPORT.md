# 🧪 Relatório de Testes — Arena AI v1.1

**Data:** 24/11/2026 · **Pacote:** `ai.arena.app` · **Build:** release assinado

## 1. Testes funcionais (Robolectric + JUnit4) — 21/21 ✅ APROVADO

Os testes executam as Activities reais (`WelcomeActivity`, `MainActivity`) em ambiente
Android simulado, exercitando WebView, modo anônimo com limite e fluxo de login.

### Núcleo do WebView (8 testes)
| Teste | O que verifica | Resultado |
|---|---|---|
| `homeDoArenaAiEhCarregadaAoAbrir` | O app abre carregando `https://arena.ai/` | ✅ |
| `webViewComJavaScriptEArmazenamentoHabilitados` | JavaScript e DOM storage ativos (o site é um SPA) | ✅ |
| `userAgentEhChromeParaLoginFuncionar` | UA de Chrome sem `; wv` — login OAuth não é bloqueado | ✅ |
| `linksHttpPermanecemDentroDoApp` | Links http(s) continuam dentro do app | ✅ |
| `linksExternosAbremNoNavegador` | `mailto:`/`tel:`/`intent:` abrem apps externos | ✅ |
| `telaDeErroApareceQuandoFalhaERetryRecarrega` | Sem internet → tela de erro + retry funciona | ✅ |
| `barraDeProgressoSomeAoTerminarCarregamento` | Barra de progresso aparece e some | ✅ |
| `voltarSemHistoricoEncerraAAtividade` | Voltar encerra o app sem histórico | ✅ |

### Política do modo anônimo (4 testes)
| Teste | O que verifica | Resultado |
|---|---|---|
| `limiteDeCincoSessoesAnonimas` | 5 sessões permitidas, 6ª bloqueada | ✅ |
| `loginLiberaUsoIlimitado` | Após login: acesso liberado, sem contar sessões | ✅ |
| `duricaoDaSesaoEhDezMinutos` | 1 sessão = 10 min; 5 sessões no total | ✅ |
| `resetVoltaAoEstadoInicial` | Reset limpa contador e login | ✅ |

### Modo convidado na interface (6 testes)
| Teste | O que verifica | Resultado |
|---|---|---|
| `modoConvidadoMostraBannerECronometro` | Banner + cronômetro visíveis no modo convidado | ✅ |
| `sessaoAnonimaEhContadaAoAbrir` | Abrir em modo convidado consome 1 sessão | ✅ |
| `sessoesEsgotadasMostramPedidoDeLoginSemNovaSesao` | Limite → pedido de login (sem "mais uma sessão") | ✅ |
| `aoEsgotarTempoRestamSessoesEOfereceNovaSesao` | Tempo acabou → oferece nova sessão (restantes) | ✅ |
| `termineiLoginLiberaUsoIlimitado` | "Terminei o login" libera acesso ilimitado | ✅ |
| `entrarNoFluxoDeLoginAbrePaginaDeLogin` | "Entrar" navega para a página de login | ✅ |

### Tela inicial (3 testes)
| Teste | O que verifica | Resultado |
|---|---|---|
| `botaoConvidadoAbreMainEmModoConvidado` | "Continuar como convidado" → modo anônimo | ✅ |
| `botaoEntrarAbreMainEmModoLogin` | "Entrar" → fluxo de login | ✅ |
| `jaLogadoVaiDiretoParaMainNormal` | Já logado pula a tela de escolha | ✅ |

**Resultado final: 21 testes · 0 falhas · 0 erros · 0 ignorados**

## 2. Verificação estática do APK — ✅

| Verificação | v1.1 |
|---|---|
| Assinatura (`apksigner verify`) | ✅ válida — mesmo keystore da v1.0 (atualização instala por cima) |
| Pacote / versão | ✅ `ai.arena.app` · versionCode 2 · versionName 1.1 |
| SDKs | ✅ minSdk 24 (Android 7.0+) · targetSdk 35 |
| Atividades | ✅ `WelcomeActivity` (launcher) + `MainActivity` (deep links) |
| Permissões | ✅ apenas `INTERNET` e `ACCESS_NETWORK_STATE` |
| Ícones | ✅ 10 PNGs + ícone adaptativo |

## 3. Como reproduzir os testes

```bash
./gradlew testDebugUnitTest
```

## 4. Limitações do ambiente de build

- Sem `/dev/kvm` → sem emulador Android; o teste visual final é instalar no celular.
