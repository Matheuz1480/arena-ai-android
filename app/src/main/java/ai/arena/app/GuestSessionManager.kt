package ai.arena.app

import android.content.Context
import android.content.SharedPreferences

/**
 * Controla o modo anônimo (convidado) com limite de sessões.
 *
 * Política do app:
 *  - 1 "uso" = 1 sessão de [ANON_SESSION_MINUTES] minutos
 *  - limite de [ANON_SESSION_LIMIT] sessões anônimas no total
 *  - modo anônimo é privado (cookies/cache/histórico são apagados ao sair)
 *  - após confirmar o login: uso ilimitado
 */
class GuestSessionManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var isLoggedIn: Boolean
        get() = prefs.getBoolean(KEY_LOGGED_IN, false)
        set(value) {
            prefs.edit().putBoolean(KEY_LOGGED_IN, value).apply()
        }

    val sessionsUsed: Int
        get() = prefs.getInt(KEY_SESSIONS_USED, 0)

    val sessionsRemaining: Int
        get() = (ANON_SESSION_LIMIT - sessionsUsed).coerceAtLeast(0)

    val canStartAnonymousSession: Boolean
        get() = !isLoggedIn && sessionsRemaining > 0

    /** Registra o início de uma nova sessão anônima. Retorna false se não for permitido. */
    fun startAnonymousSession(): Boolean {
        if (isLoggedIn || sessionsRemaining <= 0) return false
        prefs.edit().putInt(KEY_SESSIONS_USED, sessionsUsed + 1).apply()
        return true
    }

    /** Confirma o login do usuário: libera uso ilimitado. */
    fun confirmLogin() {
        isLoggedIn = true
    }

    /** Volta ao estado inicial (usado em testes e para "recomeçar"). */
    fun reset() {
        prefs.edit().clear().apply()
    }

    companion object {
        const val PREFS_NAME = "arena_guest"
        const val KEY_LOGGED_IN = "logged_in"
        const val KEY_SESSIONS_USED = "sessions_used"

        /** Duração de cada sessão anônima, em minutos. */
        const val ANON_SESSION_MINUTES = 10L

        /** Quantidade total de sessões anônimas antes de pedir login. */
        const val ANON_SESSION_LIMIT = 5

        fun sessionDurationMs(): Long = ANON_SESSION_MINUTES * 60_000L
    }
}
