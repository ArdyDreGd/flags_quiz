package com.nileworx.flagsquiz

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.os.Vibrator
import android.view.View
import android.widget.*
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.example.games.basegameutils.BaseGameActivity

class SettingsActivity : BaseGameActivity() {
    var mSharedPreferences: SharedPreferences? = null
    var e: SharedPreferences.Editor? = null
    var sou: SoundClass? = null
    var dialog: CustomDialog? = null
    var marketLink: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        val titleBar = findViewById(R.id.titleBar) as RelativeLayout
        val title = titleBar.findViewById<View>(R.id.title) as TextView
        val scoreAndCoins = titleBar.findViewById<View>(R.id.scoreAndCoins) as LinearLayout
        val sound = findViewById(R.id.sound) as RelativeLayout
        val soundText = findViewById(R.id.soundText) as TextView
        val vibrate = findViewById(R.id.vibrate) as RelativeLayout
        val vibrateText = findViewById(R.id.vibrateText) as TextView
        val rate = findViewById(R.id.rate) as RelativeLayout
        val rateText = findViewById(R.id.rateText) as TextView
        val share = findViewById(R.id.share) as RelativeLayout
        val shareText = findViewById(R.id.shareText) as TextView
        val reset = findViewById(R.id.reset) as RelativeLayout
        val resetText = findViewById(R.id.resetText) as TextView
        val back = titleBar.findViewById<View>(R.id.back) as ImageButton
        title.text = resources.getString(R.string.settingsTitle).toUpperCase()
        scoreAndCoins.visibility = View.GONE
        rateText.text = resources.getString(R.string.rateBtn)
        shareText.text = resources.getString(R.string.shareBtn)
        resetText.text = resources.getString(R.string.resetBtn)
        dialog = CustomDialog(this@SettingsActivity)
        sou = SoundClass(this@SettingsActivity)
        mSharedPreferences = applicationContext.getSharedPreferences("MyPref", 0)
        e = mSharedPreferences?.edit()
        if (mSharedPreferences?.getInt("sound", 1) == 1) {
            soundText.text = resources.getString(R.string.soundBtnOn)
        } else {
            soundText.text = resources.getString(R.string.soundBtnOff)
        }
        if (mSharedPreferences?.getInt("vibrate", 1) == 1) {
            vibrateText.text = resources.getString(R.string.vibrateBtnOn)
        } else {
            vibrateText.text = resources.getString(R.string.vibrateBtnOff)
        }
        val ad = findViewById(R.id.adView) as AdView
        ad?.loadAd(AdRequest.Builder().build())
        marketLink = "https://play.google.com/store/apps/details?id=$packageName"
        sound.setOnClickListener {
            if (mSharedPreferences?.getInt("sound", 1) == 1) {
                e?.putInt("sound", 0)
                e?.commit()
                soundText.text = resources.getString(R.string.soundBtnOff)
            } else {
                e?.putInt("sound", 1)
                e?.commit()
                soundText.text = resources.getString(R.string.soundBtnOn)
                sou!!.playSound(R.raw.play)
            }
        }
        vibrate.setOnClickListener {
            sou!!.playSound(R.raw.buttons)
            if (mSharedPreferences?.getInt("vibrate", 1) == 1) {
                e?.putInt("vibrate", 0)
                vibrateText.text = resources.getString(R.string.vibrateBtnOff)
            } else {
                val vib = this@SettingsActivity.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                vib.vibrate(500)
                e?.putInt("vibrate", 1)
                vibrateText.text = resources.getString(R.string.vibrateBtnOn)
            }
            e?.commit()
        }
        rate.setOnClickListener {
            sou!!.playSound(R.raw.buttons)
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(marketLink)
            if (!MyStartActivity(intent)) {
                intent.data = Uri.parse(marketLink)
                if (!MyStartActivity(intent)) {
                    Toast.makeText(this@SettingsActivity, resources.getString(R.string.noGooglePlayMessage), Toast.LENGTH_LONG).show()
                }
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
        reset.setOnClickListener {
            sou!!.playSound(R.raw.buttons)
            val msg = resources.getString(R.string.resetDlg)
            dialog!!.showDialog(R.layout.blue_dialog, "resetDlg", msg, null)
        }
        back.setOnClickListener { finish() }
    }

    private fun MyStartActivity(aIntent: Intent): Boolean {
        return try {
            startActivity(aIntent)
            true
        } catch (e: ActivityNotFoundException) {
            false
        }
    }

    override fun onBackPressed() {
        val intent = Intent(this@SettingsActivity, MainActivity::class.java)
        startActivity(intent)
    }

    override fun onSignInFailed() {}
    override fun onSignInSucceeded() {}
}