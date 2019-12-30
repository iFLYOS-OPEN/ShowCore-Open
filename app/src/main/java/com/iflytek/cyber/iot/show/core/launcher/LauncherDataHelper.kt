package com.iflytek.cyber.iot.show.core.launcher

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.graphics.drawable.Drawable
import com.iflytek.cyber.iot.show.core.impl.alarm.EvsAlarm

class LauncherDataHelper(private val context: Context) :
    SQLiteOpenHelper(context, DB_NAME, null, 1) {
    companion object {
        private const val TAG = "LauncherDataHelper"

        private const val TABLE_APPS = "apps"
        private const val DB_NAME = "launcher"

        private const val KEY_NAME = "name"
        private const val KEY_PACKAGE_NAME = "package_name"
        private const val KEY_ICON_URL = "icon_url"
        private const val KEY_APP_TYPE = "app_type"
    }

    private fun createTable(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE $TABLE_APPS (id INTEGER PRIMARY KEY AUTOINCREMENT, $KEY_NAME TEXT, $KEY_PACKAGE_NAME TEXT, $KEY_APP_TYPE INTEGER, $KEY_ICON_URL TEXT)")
    }

    private fun deleteTable(db: SQLiteDatabase) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_APPS")
    }

    override fun onCreate(db: SQLiteDatabase?) {
        db ?: return
        createTable(db)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db ?: return
        deleteTable(db)
        createTable(db)
    }

    fun queryPartyApps(): List<AppData> {
        readableDatabase?.let { db ->
            val apps = mutableListOf<AppData>()

            val cursor = db.query(
                TABLE_APPS,                                 // The table to query
                null,                                       // The array of columns to return (pass null to get all)
                "$KEY_APP_TYPE = ?",                        // The columns for the WHERE clause
                arrayOf(AppData.TYPE_PARTY.toString()),     // The values for the WHERE clause
                null,                                       // don't group the rows
                null,                                       // don't filter by row groups
                null                                        // The sort order
            )
            with(cursor) {
                while (cursor.moveToNext()) {
                    val name = getString(getColumnIndexOrThrow(KEY_NAME))
                    val packageName = getString(getColumnIndexOrThrow(KEY_PACKAGE_NAME))
                    val icon: Drawable? = try {
                        context.packageManager.getApplicationIcon(packageName)
                    } catch (t: Throwable) {
                        t.printStackTrace()
                        null
                    }
                    val iconUrl = getString(getColumnIndexOrThrow(KEY_ICON_URL))
                    val appType = getInt(getColumnIndexOrThrow(KEY_APP_TYPE))
                    if (!(icon == null && iconUrl.isNullOrEmpty())) {
                        val appData = AppData(
                            name,
                            packageName,
                            icon,
                            iconUrl,
                            appType
                        )
                        apps.add(appData)
                    }
                }
            }

            cursor.close()

            db.close()

            return apps.toList()
        }
        return emptyList()
    }

    fun updatePartyApps(apps: List<AppData>) {
        writableDatabase?.let { db ->
            db.delete(TABLE_APPS, "$KEY_APP_TYPE = ?", arrayOf(AppData.TYPE_PARTY.toString()))

            apps.map {
                val contentValues = ContentValues()
                contentValues.put(KEY_NAME, it.name)
                contentValues.put(KEY_PACKAGE_NAME, it.packageName)
                contentValues.put(KEY_ICON_URL, it.iconUrl)
                contentValues.put(KEY_APP_TYPE, it.appType)
                db.insert(TABLE_APPS, null, contentValues)
            }

            db.close()
        }
    }
}