package ai.arena.app

import android.widget.Button
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf

/**
 * Testes da tela inicial (escolha entre convidado e login).
 */
@RunWith(AndroidJUnit4::class)
class WelcomeActivityTest {

    @Test
    fun botaoConvidadoAbreMainEmModoConvidado() {
        ActivityScenario.launch(WelcomeActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.findViewById<Button>(R.id.btn_continue_guest).performClick()

                val intent = shadowOf(activity).nextStartedActivity
                assertEquals(MainActivity.MODE_GUEST, intent.getStringExtra(MainActivity.EXTRA_MODE))
                assertTrue("tela inicial deve fechar", activity.isFinishing)
            }
        }
    }

    @Test
    fun botaoEntrarAbreMainEmModoLogin() {
        ActivityScenario.launch(WelcomeActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.findViewById<Button>(R.id.btn_login).performClick()

                val intent = shadowOf(activity).nextStartedActivity
                assertEquals(MainActivity.MODE_LOGIN, intent.getStringExtra(MainActivity.EXTRA_MODE))
            }
        }
    }

    @Test
    fun jaLogadoVaiDiretoParaMainNormal() {
        GuestSessionManager(ApplicationProvider.getApplicationContext()).confirmLogin()

        // A Activity termina dentro do onCreate — usa o controller direto do Robolectric
        val controller = Robolectric.buildActivity(WelcomeActivity::class.java).create()
        val activity = controller.get()

        assertTrue("deve fechar a tela inicial", activity.isFinishing)
        val intent = shadowOf(activity).nextStartedActivity
        assertNotNull("deve abrir a MainActivity sem mostrar a escolha", intent)
        assertEquals(MainActivity.MODE_NORMAL, intent!!.getStringExtra(MainActivity.EXTRA_MODE))
        controller.destroy()
    }
}
