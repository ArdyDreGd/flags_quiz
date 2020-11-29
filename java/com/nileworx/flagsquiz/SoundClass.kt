package com.nileworx.flagsquiz

import android.content.Context
import android.content.SharedPreferences
import android.media.MediaPlayer
import java.io.IOException

class SoundClass(var context: Context) {
    var sound: MediaPlayer
    var mSharedPreferences: SharedPreferences
    fun playSound(effect: Int) {
        if (mSharedPreferences.getInt("sound", 1) == 1) {
            sound = MediaPlayer()
            val fd = context.resources.openRawResourceFd(effect)
            try {
                sound.setDataSource(fd.fileDescriptor, fd.startOffset, fd.length)
                sound.prepare()
                sound.start()
                sound.setOnCompletionListener { mp -> mp.release() }
            } catch (e: IllegalArgumentException) {
                e.printStackTrace()
            } catch (e: IllegalStateException) {
                e.printStackTrace()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    init {
        sound = MediaPlayer()
        mSharedPreferences = context.getSharedPreferences("MyPref", 0)
    }
}