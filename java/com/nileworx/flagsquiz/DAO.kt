package com.nileworx.flagsquiz

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.SQLException
import android.database.sqlite.SQLiteDatabase
import java.io.IOException
import kotlin.jvm.Throws

class DAO(context: Context?) {
    private var database: SQLiteDatabase? = null
    private val dbHandler: DataBaseHandler
    fun getOneFlag(FlagID: String): Cursor {
        val query = "SELECT * FROM $TABLE_FLAGS WHERE $FL_ID = '$FlagID'"
        val cursor = database!!.rawQuery(query, null)
        cursor.moveToFirst()
        return cursor
    }

    fun setFlagCompleted(flID: String, points: Int) {
        open()
        val values = ContentValues()
        values.put(FL_COMPLETED, "1")
        values.put(FL_POINTS, points)
        database!!.update(TABLE_FLAGS, values, "$FL_ID=?", arrayOf(flID))
    }

    fun setTries(flID: String, tries: Int) {
        open()
        val values = ContentValues()
        values.put(FL_TRIES, tries)
        database!!.update(TABLE_FLAGS, values, "$FL_ID=?", arrayOf(flID))
    }

    val nextFlag: Int
        get() {
            val query = "SELECT $FL_WEB_ID FROM $TABLE_FLAGS WHERE $FL_COMPLETED = 0 ORDER BY $FL_WEB_ID ASC LIMIT 1"
            val cursor = database!!.rawQuery(query, null)
            cursor.moveToFirst()
            return if (cursor.count > 0) {
                cursor.getInt(cursor.getColumnIndex(FL_WEB_ID))
            } else {
                0
            }
        }

    val flagNumber: Int
        get() {
            val query = "SELECT COUNT($FL_ID) AS completed_number FROM $TABLE_FLAGS WHERE $FL_COMPLETED = 1"
            val cursor = database!!.rawQuery(query, null)
            cursor.moveToFirst()
            return cursor.getInt(cursor.getColumnIndex("completed_number")) + 1
        }

    fun resetGame() {
        open()
        val flagsValues = ContentValues()
        flagsValues.put(FL_LETTER, "")
        flagsValues.put(FL_TRIES, 0)
        flagsValues.put(FL_POINTS, 0)
        flagsValues.put(FL_COMPLETED, "0")
        database!!.update(TABLE_FLAGS, flagsValues, null, null)
        val coinsValues = ContentValues()
        coinsValues.put(TOTAL_COINS, 25)
        coinsValues.put(USED_COINS, 0)
        database!!.update(TABLE_COINS, coinsValues, null, null)
        val emptyQuery = "DELETE FROM $TABLE_HELPS"
        database!!.execSQL(emptyQuery)
    }

    fun addTotalCoins(coins: Int) {
        open()
        val query = "UPDATE $TABLE_COINS SET $TOTAL_COINS = $TOTAL_COINS + $coins WHERE $COINS_ID = 1"
        database!!.execSQL(query)
    }

    fun addUsedCoins(coins: String) {
        open()
        val query = "UPDATE $TABLE_COINS SET $USED_COINS = $USED_COINS + $coins WHERE $COINS_ID = 1"
        database!!.execSQL(query)
    }

    fun addLetterHelpPos(flID: String, pos: String?) {
        open()
        val values = ContentValues()
        values.put(FL_LETTER, pos)
        database!!.update(TABLE_FLAGS, values, "$FL_ID=?", arrayOf(flID))
    }

    fun updateHelpState(flHelpID: String, flHelpField: String?) {
        open()
        val flagHintsValues = ContentValues()
        flagHintsValues.put(flHelpField, 1)
        val result = database!!.update(TABLE_HELPS, flagHintsValues, "$HE_FLAG=?", arrayOf(flHelpID))
        if (result == 0) {
            flagHintsValues.put(HE_FLAG, flHelpID)
            database!!.insert(TABLE_HELPS, null, flagHintsValues)
        }
    }

    fun setFlagPoints(FlagID: String, points: Int) {
        open()
        val values = ContentValues()
        values.put(FL_POINTS, points)
        database!!.update(TABLE_FLAGS, values, "$FL_ID=?", arrayOf(FlagID))
    }

    val totalScore: Cursor
        get() {
            open()
            val query = "SELECT SUM($FL_POINTS) AS total_score  FROM $TABLE_FLAGS"
            val cursor = database!!.rawQuery(query, null)
            cursor.moveToFirst()
            return cursor
        }

    val coinsCount: Cursor
        get() {
            open()
            val query = "SELECT *  FROM $TABLE_COINS WHERE $COINS_ID = 1"
            val cursor = database!!.rawQuery(query, null)
            cursor.moveToFirst()
            return cursor
        }

    fun getHelpState(flID: String): Cursor {
        open()
        val query = "SELECT * FROM $TABLE_HELPS WHERE $HE_FLAG = $flID"
        val cursor = database!!.rawQuery(query, null)
        cursor.moveToFirst()
        return cursor
    }

    fun getFlagWikipedia(flID: String): String {
        val query = "SELECT $FL_DESKRIPSI FROM $TABLE_FLAGS WHERE $FL_ID = $flID"
        val cursor = database!!.rawQuery(query, null)
        cursor.moveToFirst()
        return cursor.getString(cursor.getColumnIndex(FL_DESKRIPSI))
    }

    @Throws(SQLException::class)
    fun open() {
        database = dbHandler.writableDatabase
    }

    fun closeDatabase() {
        dbHandler.close()
    } /*public Integer getMaxOrder() {
		String query = "SELECT MAX(" + FL_ORDER + ") AS max_order FROM " + TABLE_FLAGS;
		Cursor cursor = database.rawQuery(query, null);
		cursor.moveToFirst();
		return cursor.getInt(cursor.getColumnIndex("max_order"));
	}

	public int getLastFlag() {
        String query = "SELECT " + FL_WEB_ID + " FROM " + TABLE_FLAGS + " ORDER BY " + FL_WEB_ID + " DESC LIMIT 1";
        Cursor cursor = database.rawQuery(query, null);
        cursor.moveToFirst();
        return cursor.getInt(cursor.getColumnIndex(FL_WEB_ID));
    }

	public void addFlag(String fl_name, String fl_country, String fl_city, int fl_is_country, String fl_image, String fl_wikipedia, int fl_web_id) {
		open();
		ContentValues v = new ContentValues();
		v.put("fl_name", fl_name);
		v.put("fl_country", fl_country);
		v.put("fl_city", fl_city);
		v.put("fl_is_country", fl_is_country);
		v.put("fl_image", fl_image);
		v.put("fl_wikipedia", fl_wikipedia);
		v.put("fl_tries", 0);
		v.put("fl_score", 0);
		v.put("fl_points", 0);
		v.put("fl_completed", "0");
		v.put("fl_image_sdcard", 1);
		v.put("fl_order", getMaxOrder() + 1);
		v.put("fl_web_id", fl_web_id);
		database.insert("flags", null, v);
	}

	public Cursor getFlags() {
		String query = "SELECT * FROM " + TABLE_FLAGS + " ORDER BY  " + FL_ORDER + " ASC";
		Cursor cursor = database.rawQuery(query, null);
		cursor.moveToFirst();
		return cursor;
	}

	public void addFlags2(String fl_name, String fl_country, String fl_city, String fl_wikipedia, int fl_order, int fl_web_id) {
		open();
		ContentValues v = new ContentValues();
		v.put("fl_name", fl_name);
		v.put("fl_country", fl_country);
		v.put("fl_city", fl_city);
		if (fl_country != null && !fl_country.equals("")) {
			v.put("fl_is_country", 1);
		} else {
			v.put("fl_is_country", 0);
		}
		v.put("fl_image", "0" + String.valueOf(fl_order) + ".jpg");
		v.put("fl_wikipedia", fl_wikipedia);
		v.put("fl_tries", 0);
		v.put("fl_score", 0);
		v.put("fl_points", 0);
		v.put("fl_completed", "0");
		v.put("fl_image_sdcard", 0);
		v.put("fl_order", fl_order);
		v.put("fl_status", 1);
		v.put("fl_web_id", fl_web_id);
		database.insert("flags2", null, v);
	}

    public Integer getNextFlag(String flID) {
        open();
        String query = "SELECT " + FL_ID + ", " + FL_WEB_ID + ", " + FL_IMAGE + ", " + FL_IMAGE_SDCARD + " FROM " + TABLE_FLAGS + " WHERE " + FL_COMPLETED + " = 0" + " ORDER BY " + FL_WEB_ID + " ASC LIMIT 1";
        Cursor cursor = database.rawQuery(query, null);
        cursor.moveToFirst();

        if (cursor.getCount() > 0) {
            return cursor.getInt(cursor.getColumnIndex(FL_WEB_ID));
        } else {
            return 0;
        }
    }

    public Cursor getStatsScore() {
        String query = "SELECT SUM(" + FL_POINTS + ") AS total_score FROM " + TABLE_FLAGS;
        Cursor cursor = database.rawQuery(query, null);
        cursor.moveToFirst();
        return cursor;
    }*/

    companion object {
        // All Static variables
        private const val TABLE_FLAGS = "flags"
        private const val TABLE_COINS = "coins"
        private const val TABLE_HELPS = "helps"

        // Flags Table Columns names
        private const val FL_ID = "_flid"
        private const val FL_DESKRIPSI = "fl_deskripsi"
        private const val FL_COMPLETED = "fl_completed"
        private const val FL_POINTS = "fl_points"
        private const val FL_TRIES = "fl_tries"
        private const val FL_LETTER = "fl_letter"
        private const val FL_WEB_ID = "fl_web_id"

        // Hints Table Columns names
        private const val COINS_ID = "_coid"
        private const val TOTAL_COINS = "total_coins"
        private const val USED_COINS = "used_coins"

        // Helps Table Columns names
        private const val HE_FLAG = "he_flag"
    }

    init {
        dbHandler = DataBaseHandler(context!!)
        try {
            dbHandler.createDataBase()
        } catch (ioe: IOException) {
            throw Error("Unable to create database")
        }
        try {
            dbHandler.openDataBase()
        } catch (sqle: SQLException) {
            throw sqle
        }
    }
}