package com.nileworx.flagsquiz

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.database.Cursor
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Vibrator
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.*
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.InterstitialAd
import com.google.android.gms.games.Games
import com.google.example.games.basegameutils.BaseGameActivity
import com.nileworx.flagsquiz.GameActivity
import java.util.*

class GameActivity : BaseGameActivity(), View.OnTouchListener {
    var sWidth = 0
    var sHeight = 0
    var countSpaces = 0
    var flTries = 0
    var flPoints = 0
    var resultss = 0
    var coins = 0
    @JvmField
    var globalViewId = 0
    var isLetterHelpOn = 0
    var screenInches = 0.0
    var dialog: CustomDialog? = null
    var sou: SoundClass? = null
    var cd: ConnectionDetector? = null
    var db: DAO? = null
    var c: Cursor? = null
    var mSharedPreferences: SharedPreferences? = null
    var e: SharedPreferences.Editor? = null
    var flagId: String? = null
    var marketLink: String? = null
    var quizText: String? = null
    var flSolution: String? = null
    var flJwb: String? = null
    var flSoal: String? = null
    var isFlCompleted: String? = null
    var fldesc: String? = null
    var flLetter: String? = null
    var titleBar: RelativeLayout? = null
    var spacesGrid1: LinearLayout? = null
    var spacesGrid2: LinearLayout? = null
    var rightHelps: LinearLayout? = null
    var tvSoal: TextView? = null
    var scoreTitle: TextView? = null
    var scoreValue: TextView? = null
    var coinsX: TextView? = null
    @JvmField
    var coinsValue: TextView? = null
    var hide: ImageButton? = null
    var letter: ImageButton? = null
    var solution: ImageButton? = null
    var back: ImageButton? = null
    var lettersGrid: GridView? = null
    private var interstitial: InterstitialAd? = null
    lateinit var alphabetLettersArray: CharArray
    lateinit var alphabetSpacesArray: CharArray
    lateinit var spaceViews: Array<TextView?>
    var lettersArray: ArrayList<HashMap<String, String>?>? = null
    var leAdapter: LettersAdapter? = null
    var positionsArray: ArrayList<HashMap<String, String>>? = null
    var positionsMap: HashMap<String, String>? = null
    var animBlink: Animation? = null
    var animShake: Animation? = null
    var animShakeLetter: Animation? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)
        titleBar = findViewById(R.id.titleBar) as RelativeLayout
        scoreTitle = titleBar!!.findViewById<View>(R.id.scoreTitle) as TextView
        scoreValue = titleBar!!.findViewById<View>(R.id.scoreValue) as TextView
        coinsX = titleBar!!.findViewById<View>(R.id.coinsX) as TextView
        coinsValue = titleBar!!.findViewById<View>(R.id.coinsValue) as TextView
        hide = findViewById(R.id.hide) as ImageButton
        letter = findViewById(R.id.letter) as ImageButton
        solution = findViewById(R.id.solution) as ImageButton
        spacesGrid1 = findViewById(R.id.spacesGrid1) as LinearLayout
        spacesGrid2 = findViewById(R.id.spacesGrid2) as LinearLayout
        lettersGrid = findViewById(R.id.lettersGrid) as GridView
        tvSoal = findViewById(R.id.tvSoal) as TextView
        rightHelps = findViewById(R.id.rightHelps) as LinearLayout
        back = findViewById(R.id.back) as ImageButton
        val displaymetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displaymetrics)
        sWidth = displaymetrics.widthPixels
        sHeight = displaymetrics.heightPixels
        val dens = displaymetrics.densityDpi
        val wi = sWidth.toDouble() / dens.toDouble()
        val hi = sHeight.toDouble() / dens.toDouble()
        val x = Math.pow(wi, 2.0)
        val y = Math.pow(hi, 2.0)
        screenInches = Math.sqrt(x + y)
        dialog = CustomDialog(this@GameActivity)
        sou = SoundClass(this@GameActivity)
        cd = ConnectionDetector(this@GameActivity)
        setAds()
        mSharedPreferences = applicationContext.getSharedPreferences("MyPref", 0)
        e = mSharedPreferences?.edit()
        if (mSharedPreferences?.getInt("usingNum", 0) != 100) {
            countUsingNumForRating()
        }
        db = DAO(this)
        db!!.open()
        val tf = Typeface.createFromAsset(assets, "fonts/" + resources.getString(R.string.main_font))
        scoreTitle!!.text = resources.getString(R.string.score)
        scoreTitle!!.typeface = tf
        scoreValue!!.typeface = tf
        scoreValue!!.text = totalScoreNumber.toString()
        coinsX!!.typeface = tf
        coinsValue!!.typeface = tf
        coinsValue!!.text = coinsNumber.toString()
        flagId = intent.getStringExtra("FlagId")
        marketLink = "https://play.google.com/store/apps/details?id=$packageName"
        loadData()
        setButtonsStateForUsedHelps()
        back!!.setOnClickListener {
            sou!!.playSound(R.raw.buttons)
            finish()
        }
    }

    private fun setAds() {
        val ad = findViewById(R.id.adView) as AdView
        ad?.loadAd(AdRequest.Builder().build())
        interstitial = InterstitialAd(this)
        interstitial!!.adUnitId = resources.getString(R.string.adInterstitialUnitId)
        val adRequest = AdRequest.Builder().build()
        interstitial!!.loadAd(adRequest)
    }

    fun showInterstitialAd() {
        Handler().postDelayed({
            if (interstitial!!.isLoaded) {
                interstitial!!.show()
            }
        }, 3000)
    }

    fun countUsingNumForRating() {
        e!!.putInt("usingNum", mSharedPreferences!!.getInt("usingNum", 0) + 1)
        e!!.commit()
        if (mSharedPreferences!!.getInt("usingNum", 0) >= 8) {
            Handler().postDelayed({
                cd = ConnectionDetector(this@GameActivity)
                if (cd!!.isConnectingToInternet) {
                    val msg = resources.getString(R.string.rateDlg)
                    dialog!!.showDialog(R.layout.blue_dialog, "rateDlg", msg, marketLink)
                }
            }, 3000)
        }
    }

    private fun loadData() {
        c = db!!.getOneFlag(flagId!!)
        if (c!!.count != 0) {
            flTries = c!!.getInt(c!!.getColumnIndex(KEY_TRIES))
            flJwb = c!!.getString(c!!.getColumnIndex(KEY_JAWABAN)).trim { it <= ' ' }
            flSoal = c!!.getString(c!!.getColumnIndex(KEY_SOAL)).trim { it <= ' ' }
            fldesc = c!!.getString(c!!.getColumnIndex(KEY_DESKRIPSI)).trim { it <= ' ' }
            flLetter = c!!.getString(c!!.getColumnIndex(KEY_LETTER))
            isFlCompleted = c!!.getString(c!!.getColumnIndex(KEY_COMPLETED))
            val title = titleBar!!.findViewById<View>(R.id.title) as TextView
            title.text = db!!.flagNumber.toString()
            quizText = flJwb
            flSolution = quizText
            if (flLetter == null || flLetter == "") {
                flLetter = "1000"
            }
            tvSoal!!.text = flSoal
            if (isFlCompleted != "1") {
                generateSpaces(quizText)
                generateLetters(quizText)
                Handler().postDelayed({
                    executeSpaceHelpIfAlreadyUsed()
                    executeLettersHelpIfAlreadyUsed()
                }, 0)
                rightHelps!!.visibility = View.VISIBLE
                hide!!.setOnClickListener(helpClickHandler)
                letter!!.setOnClickListener(helpClickHandler)
                solution!!.setOnClickListener(helpClickHandler)
            }
        }
    }

    fun generateSpaces(quizText: String?) {
        alphabetSpacesArray = quizText!!.toCharArray()
        val spacesArray = ArrayList<HashMap<String, String>>()
        spaceViews = arrayOfNulls(quizText.length)
        for (i in 0 until quizText.length) {
            val spacesMap = HashMap<String, String>()
            spacesMap[KEY_SPACE_GAME] = Character.toString(quizText[i])
            spacesArray.add(spacesMap)
            spaceViews[i] = TextView(this)
            val config = resources.configuration
            var width = 0
            var height = 0
            var textSize = 0
            if (config.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK == Configuration.SCREENLAYOUT_SIZE_SMALL) {
                width = 30
                height = 30
                textSize = 24
            } else if (config.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK == Configuration.SCREENLAYOUT_SIZE_NORMAL) {
                width = 60
                height = 60
                textSize = 20
                if (sWidth <= 320) {
                    width = 30
                    height = 30
                    textSize = 20
                }
                if (sWidth > 480 && screenInches >= 4 && screenInches <= 5) {
                    if (sWidth >= 1080) {
                        width = 120
                        height = 120
                        textSize = 22
                    } else {
                        width = 80
                        height = 80
                        textSize = 22
                    }
                }
            } else if (config.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK == Configuration.SCREENLAYOUT_SIZE_LARGE) {
                width = 50
                height = 50
                textSize = 28
                if (screenInches > 6.5 && screenInches < 9) {
                    width = 70
                    height = 70
                    textSize = 36
                }
            } else if (config.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK == Configuration.SCREENLAYOUT_SIZE_XLARGE) {
                width = 100
                height = 96
                textSize = 56
            } else {
                width = 60
                height = 60
                textSize = 40
            }
            spaceViews[i]!!.layoutParams = ViewGroup.LayoutParams(width, height)
            spaceViews[i]!!.gravity = Gravity.CENTER
            spaceViews[i]!!.setTextColor(Color.WHITE)
            if (Character.toString(quizText[i]) != " ") {
                spaceViews[i]!!.setBackgroundResource(R.drawable.letter_space)
                spaceViews[i]!!.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize.toFloat())
                spaceViews[i]!!.typeface = Typeface.DEFAULT_BOLD
            } else {
                spaceViews[i]!!.layoutParams = ViewGroup.LayoutParams(width / 3, height)
                spaceViews[i]!!.setBackgroundColor(Color.TRANSPARENT)
                spaceViews[i]!!.setPadding(0, 0, 0, 0)
                spaceViews[i]!!.visibility = View.INVISIBLE
                spaceViews[i]!!.text = " "
            }
            if (i < 8) {
                spacesGrid1!!.addView(spaceViews[i])
            } else if (i < 16) {
                spacesGrid2!!.addView(spaceViews[i])
            }
            spaceViews[i]!!.setOnClickListener(spacesItemClickHandler(i))
        }
    }

    inner class spacesItemClickHandler(private val position: Int) : View.OnClickListener {
        override fun onClick(v: View) {
            val leSpace = spaceViews[position]
            if (leSpace!!.text != "" && position != flLetter!!.toInt()) {
                for (i in positionsArray!!.indices) {
                    if (positionsArray!![i][KEY_SPACE_POSITION] == position.toString()) {
                        val letterPos = positionsArray!![i][KEY_LETTER_POSITION]!!.toInt()
                        lettersGrid!!.getChildAt(letterPos).visibility = View.VISIBLE
                        leSpace.text = ""
                        sou!!.playSound(R.raw.space)
                        positionsArray!!.removeAt(i)
                        break
                    }
                }
            }
        }

    }

    fun generateLetters(quizText: String?) {
        var quizText = quizText
        var y = resources.getString(R.string.jmlkotak).toInt()
        val alphabet = resources.getString(R.string.alphabet)
        countSpaces = quizText!!.length - quizText.replace(" ", "").length
        alphabetLettersArray = quizText.toCharArray()
        lettersArray = ArrayList()
        lettersGrid!!.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_MOVE) {
                true
            } else false
        }
        quizText = quizText.replace(" ", "")
        for (i in 0 until quizText.length) {
            var lettersMap = HashMap<String, String>()
            lettersMap[KEY_LETTER_GAME] = Character.toString(quizText[i])
            lettersMap["is_real"] = "1"
            lettersArray!!.add(lettersMap)
            if (i == quizText.length - 1) {
                if (quizText.length >= y) {
                    y = quizText.length + 4
                }
                for (x in quizText.length until y) {
                    val random = Random()
                    val chNum = random.nextInt(alphabet.length)
                    lettersMap = HashMap()
                    lettersMap[KEY_LETTER_GAME] = Character.toString(alphabet[chNum])
                    lettersMap["is_real"] = "0"
                    lettersArray!!.add(lettersMap)
                }
            }
        }
        Collections.shuffle(lettersArray)
        leAdapter = LettersAdapter(this, lettersArray)
        lettersGrid!!.adapter = leAdapter
        positionsArray = ArrayList()
        lettersGrid!!.onItemClickListener = lettersItemClickHandler
    }

    private val lettersItemClickHandler = AdapterView.OnItemClickListener { parent, view, position, id ->
        if (spaceViews.size > positionsArray!!.size + countSpaces) {
            lettersGrid!!.getChildAt(position).visibility = View.INVISIBLE
            sou!!.playSound(R.raw.buttons)
            addLetters(position)
        }
    }

    private fun addLetters(position: Int) {
        for (i in spaceViews.indices) {
            val leSpace = spaceViews[i]
            if (leSpace!!.visibility == View.INVISIBLE) {
                continue
            }
            if (leSpace.text == "" || leSpace.text == "?") {
                leSpace.text = lettersArray!![position]!![KEY_LETTER_GAME]!!.toUpperCase()
                positionsMap = HashMap()
                positionsMap!![KEY_LETTER_POSITION] = position.toString()
                positionsMap!![KEY_SPACE_POSITION] = i.toString()
                positionsArray!!.add(positionsMap!!)
                checkIfFinal()
                break
            }
        }
    }

    private fun checkIfFinal() {
        if (spaceViews.size == positionsArray!!.size + countSpaces) {
            for (x in spaceViews.indices) {
                if (spaceViews[x]!!.text.toString() == alphabetLettersArray[x].toString().toUpperCase() == false) {
                    if (flTries < 4) {
                        flTries++
                        db!!.setTries(flagId!!, flTries)
                        resultss = 0
                    }
                    break
                } else {
                    if (x == spaceViews.size - 1) {
                        flPoints = 0
                        when (flTries) {
                            0 -> flPoints = 100
                            1 -> flPoints = 80
                            2 -> flPoints = 60
                            3 -> flPoints = 40
                            4 -> flPoints = 20
                        }
                        resultss = 1
                    }
                }
            }
            isRight(resultss)
        }
    }

    private fun isRight(result: Int) {
        e!!.putInt("playingNum", mSharedPreferences!!.getInt("playingNum", 0) + 1)
        e!!.commit()
        if (result == 0) {
            if (mSharedPreferences!!.getInt("vibrate", 1) == 1) {
                val v = this.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                v.vibrate(500)
                val flagLayout = findViewById(R.id.flagLayout) as LinearLayout
                animShake = AnimationUtils.loadAnimation(applicationContext, R.anim.shake)
                flagLayout.startAnimation(animShake)
            }
            sou!!.playSound(R.raw.wrong_crowd)
            dialog!!.showDialog(R.layout.red_dialog, "wrongDlg", resources.getString(R.string.wrongDlg), null)
        } else {
            sou!!.playSound(R.raw.right_crowd)
            db!!.setFlagCompleted(flagId!!, flPoints)
            addPoints()
            addCoins()
            dialog!!.showDialog(R.layout.correct_dialog, "correctDlg", fldesc, flagId.toString())
        }
        if (mSharedPreferences!!.getInt("playingNum", 0) >= 5) {
            showInterstitialAd()
            e!!.putInt("playingNum", 0)
            e!!.commit()
        }
    }

    private fun addPoints() {
        db!!.setFlagPoints(flagId!!, flPoints)
        scoreValue!!.text = totalScoreNumber.toString()
        if (apiClient.isConnected) {
            Games.Leaderboards.submitScore(apiClient, getString(R.string.flags_score_leaderboard), totalScoreNumber.toLong())
        }
    }

    val totalScoreNumber: Int
        get() {
            val cScore = db!!.totalScore
            return cScore.getInt(cScore.getColumnIndex("total_score"))
        }

    private fun addCoins() {
        coins = 0
        if (flPoints == 100) {
            coins = 2
        } else if (flPoints > 0 && flPoints < 100) {
            coins = 1
        }
        db!!.addTotalCoins(coins)
        coinsValue!!.text = coinsNumber.toString()
    }

    val coinsNumber: Int
        get() {
            val cCoins = db!!.coinsCount
            return cCoins.getInt(cCoins.getColumnIndex("total_coins")) - cCoins.getInt(cCoins.getColumnIndex("used_coins"))
        }

    fun executeSpaceHelpIfAlreadyUsed() {
        if (isHelpUsed(R.id.letter) == 1 && flLetter != "1000") {
            val pos = flLetter!!.toInt()
            val leSpaceLetter = spaceViews[pos]
            leSpaceLetter!!.text = alphabetSpacesArray[pos].toString().toUpperCase()
            leSpaceLetter.setTextColor(Color.YELLOW)
            lettersGrid!!.post {
                for (i in 0 until lettersGrid!!.childCount) {
                    val vLetterHelp = lettersGrid!!.getChildAt(i) as View
                    val flLetterView = vLetterHelp.findViewById<View>(R.id.letterButton) as TextView
                    val spaceChar = alphabetSpacesArray[pos].toString().toUpperCase()
                    if (flLetterView.text == spaceChar && lettersArray!![i]!!["is_real"] == "1") {
                        positionsMap = HashMap()
                        positionsMap!![KEY_LETTER_POSITION] = i.toString()
                        positionsMap!![KEY_SPACE_POSITION] = pos.toString()
                        positionsArray!!.add(positionsMap!!)
                        vLetterHelp.visibility = View.GONE
                        break
                    }
                }
            }
        }
    }

    fun executeLettersHelpIfAlreadyUsed() {
        if (isHelpUsed(R.id.hide) == 1) {
            for (i in lettersArray!!.indices) {
                if (lettersArray!![i]!!["is_real"] == "0") {
                    lettersGrid!!.post { lettersGrid!!.getChildAt(i).visibility = View.INVISIBLE }
                }
            }
        }
    }

    private val helpClickHandler = View.OnClickListener { v ->
        sou!!.playSound(R.raw.buttons)
        getHelp(v.id)
    }

    fun getHelp(viewId: Int) {
        val remainCoins = coinsValue!!.text.toString().toInt()
        if (isHelpUsed(viewId) != 1) {
            val noHideCoins = viewId == R.id.hide && remainCoins < 5
            val noLetterCoins = viewId == R.id.letter && remainCoins < 5
            val noSolutionCoins = viewId == R.id.solution && remainCoins < 10
            if (noHideCoins || noLetterCoins || noSolutionCoins) {
                dialog!!.showDialog(R.layout.blue_dialog, "noCoinsDlg", resources.getString(R.string.noCoinsDlg), null)
            } else {
                var msg: String? = ""
                when (viewId) {
                    R.id.hide -> msg = resources.getString(R.string.hideHelpDlg)
                    R.id.letter -> msg = resources.getString(R.string.letterHelpDlg)
                    R.id.solution -> msg = resources.getString(R.string.solutionHelpDlg)
                }
                globalViewId = viewId
                dialog!!.showDialog(R.layout.blue_dialog, "helpDlg", msg, null)
            }
        }
    }

    fun executeHelp(viewId: Int) {
        when (viewId) {
            R.id.hide -> {
                db!!.updateHelpState(flagId!!, "he_hide")
                sou!!.playSound(R.raw.explosion)
                animBlink = AnimationUtils.loadAnimation(applicationContext, R.anim.blink)
                hide!!.isSelected = true
                hide!!.isEnabled = false
                var i = 0
                while (i < lettersArray!!.size) {
                    if (lettersArray!![i]!!["is_real"] == "0") {
                        var x = 0
                        while (x < positionsArray!!.size) {
                            if (positionsArray!![x][KEY_LETTER_POSITION] == i.toString()) {
                                val spacePos = positionsArray!![x][KEY_SPACE_POSITION]
                                spaceViews[spacePos!!.toInt()]!!.text = ""
                                positionsArray!!.removeAt(x)
                            }
                            x++
                        }
                        lettersGrid!!.getChildAt(i).animation = animBlink
                        lettersGrid!!.getChildAt(i).visibility = View.INVISIBLE
                    }
                    i++
                }
            }
            R.id.letter -> {
                db!!.updateHelpState(flagId!!, "he_letter")
                animBlink = AnimationUtils.loadAnimation(applicationContext, R.anim.blink)
                isLetterHelpOn = 1
                var i = 0
                while (i < spaceViews.size) {
                    val position: Int
                    position = i
                    val leSpaceLetter = spaceViews[position]
                    if (leSpaceLetter!!.visibility == View.INVISIBLE) {
                        i++
                        continue
                    }
                    if (leSpaceLetter.text == "") {
                        leSpaceLetter.text = "?"
                        leSpaceLetter.setOnClickListener {
                            val newPos: Int
                            newPos = position
                            if (spaceViews[newPos]!!.text == "?") {
                                letter!!.isSelected = true
                                letter!!.isEnabled = false
                                hideLetter(newPos)
                                spaceViews[newPos]!!.text = alphabetSpacesArray[newPos].toString().toUpperCase()
                                spaceViews[newPos]!!.setTextColor(Color.YELLOW)
                                coinsValue!!.text = coinsNumber.toString()
                                db!!.addLetterHelpPos(flagId!!, newPos.toString())
                                flLetter = newPos.toString()
                                stopLetterHelp()
                                checkIfFinal()
                            }
                        }
                        animShakeLetter = AnimationUtils.loadAnimation(applicationContext, R.anim.shake_letter)
                        lettersGrid!!.onItemClickListener = AdapterView.OnItemClickListener { parent, view, position, id ->
                            Toast.makeText(this@GameActivity, resources.getString(R.string.stopLetterHelp), Toast.LENGTH_LONG).show()
                            if (mSharedPreferences!!.getInt("vibrate", 1) == 1) {
                                val v = this@GameActivity.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                                v.vibrate(500)
                                animShake = AnimationUtils.loadAnimation(applicationContext, R.anim.shake)
                                lettersGrid!!.startAnimation(animShake)
                            }
                        }
                    }
                    i++
                }
            }
            R.id.solution -> {
                db!!.updateHelpState(flagId!!, "he_solution")
                dialog!!.showDialog(R.layout.red_dialog, "solutionDlg", flSolution, null)
            }
        }
    }

    private fun hideLetter(pos: Int) {
        var foundIt = false
        var invisibleChar = 0
        val spaceChar = alphabetSpacesArray[pos].toString().toUpperCase()
        var vLetterHelp: View
        for (i in 0 until lettersGrid!!.childCount) {
            vLetterHelp = lettersGrid!!.getChildAt(i) as View
            val leLetterLetter = vLetterHelp.findViewById<View>(R.id.letterButton) as TextView
            if (leLetterLetter.text == spaceChar && lettersArray!![i]!!["is_real"] == "1") {
                if (vLetterHelp.visibility == View.INVISIBLE) {
                    invisibleChar = i
                    continue
                }
                positionsMap = HashMap()
                positionsMap!![KEY_LETTER_POSITION] = i.toString()
                positionsMap!![KEY_SPACE_POSITION] = pos.toString()
                positionsArray!!.add(positionsMap!!)
                vLetterHelp.animation = animBlink
                vLetterHelp.visibility = View.INVISIBLE
                foundIt = true
                break
            }
        }
        if (foundIt == false) {
            for (x in positionsArray!!.indices) {
                val letter = positionsArray!![x][KEY_LETTER_POSITION]!!.toInt()
                val space = positionsArray!![x][KEY_SPACE_POSITION]!!.toInt()
                if (letter == invisibleChar) {
                    val thisChar = alphabetSpacesArray[space].toString().toUpperCase()
                    val leSpaceLetter = spaceViews[space]
                    if (leSpaceLetter!!.text != thisChar) {
                        leSpaceLetter.text = ""
                        positionsArray!!.removeAt(x)
                    }
                }
            }
            vLetterHelp = lettersGrid!!.getChildAt(invisibleChar) as View
            positionsMap = HashMap()
            positionsMap!![KEY_LETTER_POSITION] = invisibleChar.toString()
            positionsMap!![KEY_SPACE_POSITION] = pos.toString()
            positionsArray!!.add(positionsMap!!)
            vLetterHelp.animation = animBlink
            vLetterHelp.visibility = View.INVISIBLE
            foundIt = true
        }
    }

    fun stopLetterHelp() {
        isLetterHelpOn = 0
        for (i in spaceViews.indices) {
            val leSpaceLetter2 = spaceViews[i]
            if (leSpaceLetter2!!.visibility == View.INVISIBLE) {
                continue
            }
            if (leSpaceLetter2.text == "?") {
                leSpaceLetter2.text = ""
            }
            spaceViews[i]!!.setOnClickListener(spacesItemClickHandler(i))
        }
        lettersGrid!!.onItemClickListener = lettersItemClickHandler
    }

    private fun isHelpUsed(viewId: Int): Int {
        var state = 0
        c = db!!.getHelpState(flagId!!)
        if (c!!.count != 0) {
            when (viewId) {
                R.id.hide -> state = c!!.getInt(c!!.getColumnIndex("he_hide"))
                R.id.letter -> state = c!!.getInt(c!!.getColumnIndex("he_letter"))
                R.id.solution -> state = c!!.getInt(c!!.getColumnIndex("he_solution"))
            }
        }
        return state
    }

    private fun setButtonsStateForUsedHelps() {
        c = db!!.getHelpState(flagId!!)
        if (c!!.count != 0) {
            if (c!!.getInt(c!!.getColumnIndex("he_hide")) == 1) {
                hide!!.isSelected = true
                hide!!.isEnabled = false
            }
            if (c!!.getInt(c!!.getColumnIndex("he_letter")) == 1 && flLetter != "1000") {
                letter!!.isSelected = true
                letter!!.isEnabled = false
            }
        }
    }

    override fun onTouch(arg0: View, arg1: MotionEvent): Boolean {
        return false
    }

    override fun onSignInFailed() {}
    override fun onSignInSucceeded() {}
    override fun onBackPressed() {
        super.onBackPressed()
        val intent = Intent(this@GameActivity, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
        finish()
        startActivity(intent)
    }

    companion object {
        const val KEY_JAWABAN = "fl_jawaban"
        const val KEY_DESKRIPSI = "fl_deskripsi"
        const val KEY_LETTER = "fl_letter"
        const val KEY_SOAL = "fl_soal"
        const val KEY_TRIES = "fl_tries"
        const val KEY_COMPLETED = "fl_completed"
        const val KEY_LETTER_GAME = "letter"
        const val KEY_SPACE_GAME = "space"
        const val KEY_LETTER_POSITION = "letter_position"
        const val KEY_SPACE_POSITION = "space_position"
    }
}