package ai.arena.app

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat

/**
 * Tela inicial: escolha entre "Continuar como convidado" (anônimo, com limite)
 * ou "Entrar" (uso ilimitado após login).
 */
class WelcomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_welcome)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.welcome_root)) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        val manager = GuestSessionManager(this)

        // Já logado: nem mostra esta tela
        if (manager.isLoggedIn) {
            openMain(MainActivity.MODE_NORMAL)
            return
        }

        findViewById<TextView>(R.id.welcome_guest_desc).text = getString(
            R.string.welcome_guest_desc,
            GuestSessionManager.ANON_SESSION_LIMIT,
            GuestSessionManager.ANON_SESSION_MINUTES
        )

        findViewById<Button>(R.id.btn_continue_guest).setOnClickListener {
            openMain(MainActivity.MODE_GUEST)
        }

        findViewById<Button>(R.id.btn_login).setOnClickListener {
            openMain(MainActivity.MODE_LOGIN)
        }
    }

    private fun openMain(mode: String) {
        startActivity(Intent(this, MainActivity::class.java).putExtra(MainActivity.EXTRA_MODE, mode))
        finish()
    }
}
