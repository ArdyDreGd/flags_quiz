package com.nileworx.flagsquiz

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.widget.ImageButton
import android.widget.Toast
import com.google.android.gms.games.Games
import com.google.android.gms.games.GamesActivityResultCodes
import com.google.example.games.basegameutils.BaseGameActivity

class MainActivity : BaseGameActivity() {
    var dialog: CustomDialog? = null
    var sou: SoundClass? = null
    var db: DAO? = null
    var mSharedPreferences: SharedPreferences? = null
    var e: SharedPreferences.Editor? = null
    var marketLink: String? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val play: ImageButton = findViewById(R.id.play) as ImageButton
        val settings: ImageButton = findViewById(R.id.settings) as ImageButton
        val exit: ImageButton = findViewById(R.id.exit) as ImageButton
        val rate: ImageButton = findViewById(R.id.rate) as ImageButton
        val share: ImageButton = findViewById(R.id.share) as ImageButton
        val sound: ImageButton = findViewById(R.id.sound) as ImageButton
        val leaderboard: ImageButton = findViewById(R.id.leaderboard) as ImageButton
        dialog = CustomDialog(this@MainActivity)
        sou = SoundClass(this@MainActivity)
        db = DAO(this)
        db!!.open()
        mSharedPreferences = applicationContext.getSharedPreferences("MyPref", 0)
        e = mSharedPreferences?.edit()
        if (mSharedPreferences?.getInt("sound", 1) == 1) {
            sound.setBackgroundResource(R.drawable.button_sound_on_main)
        } else {
            sound.setBackgroundResource(R.drawable.button_sound_off_main)
        }
        marketLink = "https://play.google.com/store/apps/details?id=$packageName"
        play.setOnClickListener {
            sou!!.playSound(R.raw.play)
            if (db!!.nextFlag != 0) {
                val intent = Intent(this@MainActivity, GameActivity::class.java)
                intent.putExtra("FlagId", db!!.nextFlag.toString())
                startActivity(intent)
            } else {
                dialog!!.showDialog(R.layout.red_dialog, "finishDlg", resources.getString(R.string.finishDlg), null)
            }
        }
        sound.setOnClickListener {
            if (mSharedPreferences?.getInt("sound", 1) == 1) {
                e?.putInt("sound", 0)
                e?.commit()
                sound.setBackgroundResource(R.drawable.button_sound_off_main)
            } else {
                e?.putInt("sound", 1)
                e?.commit()
                sound.setBackgroundResource(R.drawable.button_sound_on_main)
                sou!!.playSound(R.raw.buttons)
            }
        }
        share.setOnClickListener {
            sou!!.playSound(R.raw.buttons)
            val sharingIntent = Intent(Intent.ACTION_SEND)
            sharingIntent.type = "text/plain"
            val shareMessage = packageName + " " + resources.getString(R.string.shareDlgMessage) + marketLink
            sharingIntent.putExtra(Intent.EXTRA_SUBJECT, packageName)
            sharingIntent.putExtra(Intent.EXTRA_TEXT, shareMessage)
            startActivity(Intent.createChooser(sharingIntent, resources.getString(R.string.shareDlgTitle)))
        }
        rate.setOnClickListener {
            sou!!.playSound(R.raw.buttons)
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(marketLink)
            startActivity(intent)
            if (!MyStartActivity(intent)) {
                intent.data = Uri.parse(marketLink)
                if (!MyStartActivity(intent)) {
                    Toast.makeText(this@MainActivity, resources.getString(R.string.noGooglePlayMessage), Toast.LENGTH_LONG).show()
                }
            }
        }
        settings.setOnClickListener {
            sou!!.playSound(R.raw.buttons)
            val intent = Intent(this@MainActivity, SettingsActivity::class.java)
            startActivity(intent)
        }
        exit.setOnClickListener {
            sou!!.playSound(R.raw.buttons)
            dialog!!.showDialog(R.layout.blue_dialog, "exitDlg", resources.getString(R.string.exitDlg), null)
        }
        leaderboard.setOnClickListener {
            sou!!.playSound(R.raw.buttons)
            if (!isSignedIn) {
                beginUserInitiatedSignIn()
            } else {
                startActivityForResult(Games.Leaderboards.getLeaderboardIntent(apiClient, getString(R.string.flags_score_leaderboard)), 2)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == 2 && resultCode == GamesActivityResultCodes.RESULT_RECONNECT_REQUIRED) {
            mHelper.disconnect()
        } else {
            mHelper.onActivityResult(requestCode, resultCode, data)
        }
    }

    fun MyStartActivity(aIntent: Intent?): Boolean {
        return try {
            startActivity(aIntent)
            true
        } catch (e: ActivityNotFoundException) {
            false
        }
    }

    override fun onSignInSucceeded() {}
    override fun onSignInFailed() {}
}