package com.iflytek.cyber.iot.show.core.launcher

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.graphics.drawable.Drawable
import com.iflytek.cyber.iot.show.core.impl.alarm.EvsAlarm

class LauncherDataHelper(private val context: Context) :
    SQLiteOpenHelper(context, DB_NAME, null, 3) {
    companion object {
        private const val TAG = "LauncherDataHelper"

        private const val TABLE_APPS = "apps"
        private const val TABLE_TEMPLATE_APPS = "template_apps"
        private const val DB_NAME = "launcher"

        private const val KEY_NAME = "name"
        private const val KEY_PACKAGE_NAME = "package_name"
        private const val KEY_ICON_URL = "icon_url"
        private const val KEY_APP_TYPE = "app_type"

        private const val KEY_SOURCE = "source"
        private const val KEY_IMG = "img"
        private const val KEY_TEMPLATE = "template"
        private const val KEY_BUSINESS = "business"
        private const val KEY_IS_DARK = "is_dark"
        private const val KEY_TYPE = "type"
        private const val KEY_URL = "url"
        private const val KEY_TEXT_IN = "textIn"
    }

    private fun createTable(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE $TABLE_APPS (id INTEGER PRIMARY KEY AUTOINCREMENT, $KEY_NAME TEXT, $KEY_PACKAGE_NAME TEXT, $KEY_APP_TYPE INTEGER, $KEY_ICON_URL TEXT)")
        db.execSQL("CREATE TABLE $TABLE_TEMPLATE_APPS (id INTEGER PRIMARY KEY AUTOINCREMENT, $KEY_SOURCE TEXT, $KEY_NAME TEXT, $KEY_ICON_URL TEXT, $KEY_IMG TEXT, $KEY_TEMPLATE INTEGER, $KEY_BUSINESS TEXT, $KEY_IS_DARK INTEGER, $KEY_TYPE TEXT, $KEY_URL TEXT, $KEY_TEXT_IN TEXT)")
    }

    private fun deleteTable(db: SQLiteDatabase) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_APPS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_TEMPLATE_APPS")
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
                if (db.isOpen) {
                    val contentValues = ContentValues()
                    contentValues.put(KEY_NAME, it.name)
                    contentValues.put(KEY_PACKAGE_NAME, it.packageName)
                    contentValues.put(KEY_ICON_URL, it.iconUrl)
                    contentValues.put(KEY_APP_TYPE, it.appType)
                    db.insert(TABLE_APPS, null, contentValues)
                }
            }

            db.close()
        }
    }

    fun queryTemplateApps(): List<TemplateAppData> {
        readableDatabase?.let { db ->
            val apps = mutableListOf<TemplateAppData>()

            val cursor = db.query(
                TABLE_TEMPLATE_APPS,    // The table to query
                null,                   // The array of columns to return (pass null to get all)
                null,                   // The columns for the WHERE clause
                null,                   // The values for the WHERE clause
                null,                   // don't group the rows
                null,                   // don't filter by row groups
                null                    // The sort order
            )

            while (cursor.moveToNext()) {
                val source = cursor.getString(cursor.getColumnIndex(KEY_SOURCE))
                val name = cursor.getString(cursor.getColumnIndex(KEY_NAME))
                val icon = cursor.getString(cursor.getColumnIndex(KEY_ICON_URL))
                val img = cursor.getString(cursor.getColumnIndex(KEY_IMG))
                val template = cursor.getInt(cursor.getColumnIndex(KEY_TEMPLATE))
                val business = cursor.getString(cursor.getColumnIndex(KEY_BUSINESS))
                val isDark = cursor.getInt(cursor.getColumnIndex(KEY_IS_DARK)) > 0
                val type = cursor.getString(cursor.getColumnIndex(KEY_TYPE))
                val url = cursor.getString(cursor.getColumnIndex(KEY_URL))
                val textIn = cursor.getString(cursor.getColumnIndex(KEY_TEXT_IN))

                val templateAppData = TemplateAppData(
                    source,
                    name,
                    icon,
                    img,
                    template,
                    business,
                    isDark,
                    type,
                    url,
                    textIn
                )

                apps.add(templateAppData)
            }

            cursor.close()
            db.close()

            return apps
        }
        return emptyList()
    }

    fun updateTemplateAppData(apps: List<TemplateAppData>) {
        writableDatabase?.let { db ->
            db.delete(TABLE_TEMPLATE_APPS, "$KEY_NAME != null", null)

            apps.map {
                if (db.isOpen) {
                    val contentValues = ContentValues()
                    contentValues.put(KEY_NAME, it.name)
                    contentValues.put(KEY_SOURCE, it.source)
                    contentValues.put(KEY_ICON_URL, it.iconUrl)
                    contentValues.put(KEY_IMG, it.img)
                    contentValues.put(KEY_TEMPLATE, it.template)
                    contentValues.put(KEY_BUSINESS, it.business)
                    contentValues.put(KEY_IS_DARK, if (it.isDark != false) 1 else 0)
                    contentValues.put(KEY_TYPE, it.type)
                    contentValues.put(KEY_URL, it.url)
                    contentValues.put(KEY_TEXT_IN, it.textIn)
                    db.insert(TABLE_TEMPLATE_APPS, null, contentValues)
                }
            }

            db.close()
        }
    }
}