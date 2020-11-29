package com.nileworx.flagsquiz

import android.content.Context
import android.database.SQLException
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteOpenHelper
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import kotlin.jvm.Throws

class DataBaseHandler(private val myContext: Context) : SQLiteOpenHelper(myContext, DB_NAME, null, 1) {
    private var myDataBase: SQLiteDatabase? = null

    @Throws(IOException::class)
    fun createDataBase() {
        val dbExist = checkDataBase()
        if (dbExist) {
        } else {
            this.readableDatabase
            try {
                copyDataBase()
            } catch (e: IOException) {
                throw Error("Error copying database")
            }
        }
    }

    private fun checkDataBase(): Boolean {
        var checkDB: SQLiteDatabase? = null
        try {
            val myPath = DB_PATH
            checkDB = SQLiteDatabase.openDatabase(myPath, null, SQLiteDatabase.OPEN_READONLY)
        } catch (e: SQLiteException) {
        }
        checkDB?.close()
        return if (checkDB != null) true else false
    }

    @Throws(IOException::class)
    private fun copyDataBase() {
        val myInput = myContext.assets.open("database/$DB_NAME")
        val outFileName = DB_PATH
        val myOutput: OutputStream = FileOutputStream(outFileName)
        val buffer = ByteArray(1024)
        var length: Int
        while (myInput.read(buffer).also { length = it } > 0) {
            myOutput.write(buffer, 0, length)
        }
        myOutput.flush()
        myOutput.close()
        myInput.close()
    }

    @Throws(SQLException::class)
    fun openDataBase() {
        val myPath = DB_PATH
        myDataBase = SQLiteDatabase.openDatabase(myPath, null, SQLiteDatabase.OPEN_READONLY)
    }

    @Synchronized
    override fun close() {
        if (myDataBase != null) myDataBase!!.close()
        super.close()
    }

    override fun onCreate(db: SQLiteDatabase) {}
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}

    companion object {
        private lateinit var DB_PATH: String
        private const val DB_NAME = "FlagsQuiz"
    }

    init {
        DB_PATH = myContext.getDatabasePath(DB_NAME).toString()
    }
}