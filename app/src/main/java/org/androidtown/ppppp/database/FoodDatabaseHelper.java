package org.androidtown.ppppp.database;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class FoodDatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "food_nutrition.db";
    private static final int DATABASE_VERSION = 13;  // ✅ 버전 증가 필요

    public FoodDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // 식단 테이블
        String CREATE_TABLE_FOOD = "CREATE TABLE IF NOT EXISTS food_data (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "date TEXT, " +
                "meal_type TEXT, " +
                "food_name TEXT, " +
                "calories REAL, " +
                "carbs REAL, " +
                "protein REAL, " +
                "fat REAL)";
        db.execSQL(CREATE_TABLE_FOOD);

        // 운동 일지 테이블
        String CREATE_TABLE_WORKOUT = "CREATE TABLE IF NOT EXISTS workout_log (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "date TEXT, " +
                "memo TEXT, " +
                "media_url TEXT)";
        db.execSQL(CREATE_TABLE_WORKOUT);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // DB 버전 변경 시 테이블 재생성
        db.execSQL("DROP TABLE IF EXISTS food_data");
        db.execSQL("DROP TABLE IF EXISTS workout_log");
        onCreate(db);
    }

    // 특정 컬럼 존재 여부 확인
    private boolean columnExists(SQLiteDatabase db, String tableName, String columnName) {
        Cursor cursor = db.rawQuery("PRAGMA table_info(" + tableName + ")", null);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                String currentColumn = cursor.getString(cursor.getColumnIndexOrThrow("name"));
                if (currentColumn.equals(columnName)) {
                    cursor.close();
                    return true;
                }
            }
            cursor.close();
        }
        return false;
    }
}
