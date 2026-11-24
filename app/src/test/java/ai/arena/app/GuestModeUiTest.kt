package ai.arena.app

import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf

/**
 * Testes do modo convidado (anônimo com limite) e do pedido de login.
 */
@RunWith(AndroidJUnit4::class)
class GuestModeUiTest {

    private fun launchWithMode(mode: String, block: (MainActivity) -> Unit) {
        val intent = android.content.Intent(
            ApplicationProvider.getApplicationContext(),
            MainActivity::class.java
        ).putExtra(MainActivity.EXTRA_MODE, mode)
        ActivityScenario.launch<MainActivity>(intent).use { scenario ->
            scenario.onActivity(block)
        }
    }

    @Test
    fun modoConvidadoMostraBannerECronometro() {
        launchWithMode(MainActivity.MODE_GUEST) { activity ->
            assertEquals(
                "banner do modo convidado deve aparecer",
                View.VISIBLE,
                activity.findViewById<View>(R.id.guest_banner).visibility
            )
            val timer = activity.findViewById<TextView>(R.id.timer_text).text.toString()
            assertTrue("cronômetro deve mostrar mm:ss ($timer)", timer.contains(":"))
        }
    }

    @Test
    fun sessaoAnonimaEhContadaAoAbrir() {
        launchWithMode(MainActivity.MODE_GUEST) { activity ->
            val m = GuestSessionManager(activity)
            assertEquals(1, m.sessionsUsed)
            assertEquals(GuestSessionManager.ANON_SESSION_LIMIT - 1, m.sessionsRemaining)
        }
    }

    @Test
    fun sessoesEsgotadasMostramPedidoDeLoginSemNovaSesao() {
        val m = GuestSessionManager(ApplicationProvider.getApplicationContext())
        repeat(GuestSessionManager.ANON_SESSION_LIMIT) { m.startAnonymousSession() }

        launchWithMode(MainActivity.MODE_GUEST) { activity ->
            assertEquals(
                "pedido de login deve aparecer",
                View.VISIBLE,
                activity.findViewById<View>(R.id.login_prompt).visibility
            )
            assertEquals(
                "sem sessões restantes, não mostra 'usar mais uma sessão'",
                View.GONE,
                activity.findViewById<View>(R.id.btn_another_session).visibility
            )
        }
    }

    @Test
    fun aoEsgotarTempoRestamSessoesEOfereceNovaSesao() {
        launchWithMode(MainActivity.MODE_GUEST) { activity ->
            activity.onSessionExpired()

            assertEquals(View.VISIBLE, activity.findViewById<View>(R.id.login_prompt).visibility)
            val btn = activity.findViewById<Button>(R.id.btn_another_session)
            assertEquals(View.VISIBLE, btn.visibility)
            assertTrue(
                "botão deve indicar sessões restantes (4)",
                btn.text.toString().contains("4")
            )
        }
    }

    @Test
    fun termineiLoginLiberaUsoIlimitado() {
        launchWithMode(MainActivity.MODE_LOGIN) { activity ->
            assertEquals(
                "barra de confirmação de login deve aparecer",
                View.VISIBLE,
                activity.findViewById<View>(R.id.login_pending_bar).visibility
            )

            activity.findViewById<Button>(R.id.btn_done_login).performClick()

            val m = GuestSessionManager(activity)
            assertTrue("login confirmado deve liberar acesso", m.isLoggedIn)
            assertEquals(View.GONE, activity.findViewById<View>(R.id.login_pending_bar).visibility)
        }
    }

    @Test
    fun entrarNoFluxoDeLoginAbrePaginaDeLogin() {
        launchWithMode(MainActivity.MODE_GUEST) { activity ->
            activity.onSessionExpired()
            activity.findViewById<Button>(R.id.btn_login_unlimited).performClick()

            val webView = activity.findViewById<android.webkit.WebView>(R.id.webview)
            val loaded = shadowOf(webView).lastLoadedUrl
            assertTrue("deve navegar para a página de login ($loaded)", loaded!!.contains("login"))
        }
    }
}
