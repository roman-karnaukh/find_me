package org.roman.findme.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import static org.roman.findme.location_service.Constants.dbTableName;

import static org.roman.findme.location_service.Constants.TAG;

/**
 * Created by Karnaukh Roman on 22.08.2016.
 */
public class DBHelper extends SQLiteOpenHelper {

    public DBHelper(Context context) {
        super(context, "myDB", null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.d(TAG, "--- onCreate database ---");
        // создаем таблицу с полями
        db.execSQL("create table notSendetCoordinates ("
                + "id integer primary key autoincrement,"
                + "latitude text,"
                + "longitude text" + ");");

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + dbTableName);
        onCreate(db);
    }
}
