package ai.arena.app

import android.content.Intent
import android.net.Uri
import android.view.View
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.robolectric.Shadows.shadowOf

/**
 * Testes funcionais do wrapper Arena AI.
 *
 * Executam a Activity real sobre o Robolectric (ambiente Android simulado),
 * exercitando a lógica de WebView, navegação, erros e progresso.
 */
@RunWith(AndroidJUnit4::class)
class MainActivityTest {

    private fun request(url: String, mainFrame: Boolean = true): WebResourceRequest {
        val req = mock(WebResourceRequest::class.java)
        `when`(req.url).thenReturn(Uri.parse(url))
        `when`(req.isForMainFrame).thenReturn(mainFrame)
        `when`(req.method).thenReturn("GET")
        return req
    }

    @Test
    fun homeDoArenaAiEhCarregadaAoAbrir() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val webView = activity.findViewById<WebView>(R.id.webview)
                assertEquals("https://arena.ai/", shadowOf(webView).lastLoadedUrl)
            }
        }
    }

    @Test
    fun webViewComJavaScriptEArmazenamentoHabilitados() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val settings = activity.findViewById<WebView>(R.id.webview).settings
                assertTrue("JavaScript deve estar habilitado", settings.javaScriptEnabled)
                assertTrue("DOM storage deve estar habilitado", settings.domStorageEnabled)
            }
        }
    }

    @Test
    fun userAgentEhChromeParaLoginFuncionar() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val ua = activity.findViewById<WebView>(R.id.webview).settings.userAgentString!!
                assertTrue("UA deve conter Chrome: $ua", ua.contains("Chrome"))
                assertFalse("UA não deve marcar WebView ('; wv'): $ua", ua.contains("; wv"))
            }
        }
    }

    @Test
    fun linksHttpPermanecemDentroDoApp() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val webView = activity.findViewById<WebView>(R.id.webview)
                val client = shadowOf(webView).webViewClient
                val handled = client.shouldOverrideUrlLoading(
                    webView, request("https://arena.ai/leaderboard")
                )
                assertFalse("http(s) deve continuar no WebView", handled)
                assertNull("nenhuma Activity externa deve ser iniciada", shadowOf(activity).nextStartedActivity)
            }
        }
    }

    @Test
    fun linksExternosAbremNoNavegador() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val webView = activity.findViewById<WebView>(R.id.webview)
                val client = shadowOf(webView).webViewClient
                val handled = client.shouldOverrideUrlLoading(
                    webView, request("mailto:oi@arena.ai")
                )
                assertTrue("scheme não-http deve abrir externamente", handled)
                val intent = shadowOf(activity).nextStartedActivity
                assertNotNull("deve iniciar Intent ACTION_VIEW", intent)
                assertEquals(Intent.ACTION_VIEW, intent!!.action)
                assertEquals(Uri.parse("mailto:oi@arena.ai"), intent.data)
            }
        }
    }

    @Test
    fun telaDeErroApareceQuandoFalhaERetryRecarrega() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val webView = activity.findViewById<WebView>(R.id.webview)
                val client = shadowOf(webView).webViewClient
                val error = mock(WebResourceError::class.java)
                `when`(error.errorCode).thenReturn(WebViewClient.ERROR_CONNECT)
                `when`(error.description).thenReturn("net::ERR_CONNECTION_REFUSED")

                client.onReceivedError(webView, request("https://arena.ai/"), error)

                val errorView = activity.findViewById<View>(R.id.error_view)
                assertEquals("tela de erro deve aparecer", View.VISIBLE, errorView.visibility)

                activity.findViewById<Button>(R.id.btn_retry).performClick()
                assertEquals("retry deve esconder a tela de erro", View.GONE, errorView.visibility)
                assertNotNull("deve recarregar a página", shadowOf(webView).lastLoadedUrl)
            }
        }
    }

    @Test
    fun barraDeProgressoSomeAoTerminarCarregamento() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val webView = activity.findViewById<WebView>(R.id.webview)
                val chrome = shadowOf(webView).webChromeClient
                val progress = activity.findViewById<View>(R.id.progress)

                chrome.onProgressChanged(webView, 42)
                assertEquals("progresso deve aparecer durante o load", View.VISIBLE, progress.visibility)

                chrome.onProgressChanged(webView, 100)
                assertEquals("progresso deve sumir ao concluir", View.GONE, progress.visibility)
            }
        }
    }

    @Test
    fun voltarSemHistoricoEncerraAAtividade() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.onBackPressedDispatcher.onBackPressed()
                assertTrue("sem histórico, voltar deve encerrar a atividade", activity.isFinishing)
            }
        }
    }
}
