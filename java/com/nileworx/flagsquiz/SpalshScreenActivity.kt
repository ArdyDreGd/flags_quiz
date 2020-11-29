package com.nileworx.flagsquiz

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import com.google.example.games.basegameutils.BaseGameActivity

class SpalshScreenActivity : BaseGameActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)

        val handler = Handler()
        handler.postDelayed({
            val intent = Intent (this@SpalshScreenActivity, MainActivity::class.java)
            startActivity(intent)
            finish()
        }, 3500)
    }

    override fun onSignInSucceeded() {
    }

    override fun onSignInFailed() {
    }

}
