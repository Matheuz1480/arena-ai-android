package ai.arena.app

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Testes da política do modo anônimo:
 * 5 sessões de 10 minutos, depois login (uso ilimitado).
 */
@RunWith(AndroidJUnit4::class)
class GuestSessionManagerTest {

    private fun manager() =
        GuestSessionManager(ApplicationProvider.getApplicationContext())

    @Test
    fun limiteDeCincoSessoesAnonimas() {
        val m = manager()
        repeat(GuestSessionManager.ANON_SESSION_LIMIT) { i ->
            assertTrue("sessão ${i + 1} deve ser permitida", m.startAnonymousSession())
            assertEquals(GuestSessionManager.ANON_SESSION_LIMIT - i - 1, m.sessionsRemaining)
        }
        assertFalse("6ª sessão deve ser bloqueada", m.startAnonymousSession())
        assertEquals(0, m.sessionsRemaining)
    }

    @Test
    fun loginLiberaUsoIlimitado() {
        val m = manager()
        repeat(GuestSessionManager.ANON_SESSION_LIMIT) { m.startAnonymousSession() }
        assertFalse(m.canStartAnonymousSession)

        m.confirmLogin()

        assertTrue(m.isLoggedIn)
        assertFalse("logado não precisa de sessão anônima", m.canStartAnonymousSession)
        assertFalse("sessão anônima não deve ser contada quando logado", m.startAnonymousSession())
    }

    @Test
    fun duricaoDaSesaoEhDezMinutos() {
        assertEquals(10L, GuestSessionManager.ANON_SESSION_MINUTES)
        assertEquals(10L * 60_000L, GuestSessionManager.sessionDurationMs())
        assertEquals(5, GuestSessionManager.ANON_SESSION_LIMIT)
    }

    @Test
    fun resetVoltaAoEstadoInicial() {
        val m = manager()
        m.startAnonymousSession()
        m.reset()
        assertEquals(0, m.sessionsUsed)
        assertFalse(m.isLoggedIn)
    }
}
