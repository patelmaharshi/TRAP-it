package com.vrtrappers.trapit.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;

public class BookmarksHelper extends SQLiteOpenHelper {
    // If you change the database schema, you must increment the database version.
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "Bookmarks.db";
    private static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + Bookmarks.BookmarksEntry.TABLE_NAME + " (" +
                    Bookmarks.BookmarksEntry._ID + " INTEGER PRIMARY KEY," +
                    Bookmarks.BookmarksEntry.COLUMN_NAME_TITLE + " TEXT," +
                    Bookmarks.BookmarksEntry.COLUMN_NAME_SNIPPET + " TEXT)";

    private static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + Bookmarks.BookmarksEntry.TABLE_NAME;
    public BookmarksHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ENTRIES);
    }
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // This database is only a cache for online data, so its upgrade policy is
        // to simply to discard the data and start over
        db.execSQL(SQL_DELETE_ENTRIES);
        onCreate(db);
    }
    public boolean isExistTitle(String title){
        SQLiteDatabase db=getReadableDatabase();
        String query="Select * from "+Bookmarks.BookmarksEntry.TABLE_NAME+" where "+Bookmarks.BookmarksEntry.COLUMN_NAME_TITLE+" = '"+title+"'";
        Cursor cursor = db.rawQuery(query, null);
        if(cursor.getCount()<=0){
            cursor.close();
            return false;
        }
        cursor.close();
        return true;
    }
    public boolean removeAll(){
        SQLiteDatabase db=getWritableDatabase();
        int count= db.delete(Bookmarks.BookmarksEntry.TABLE_NAME, "1", null);
        db.close();
        if(count==0){
            return false;
        }
        return true;
    }
    public boolean addEntry(String title,String snippet){
        if(isExistTitle(title))
            return true;
        // Gets the data repository in write mode
        SQLiteDatabase db = getWritableDatabase();

        // Create a new map of values, where column names are the keys
        ContentValues values = new ContentValues();
        values.put(Bookmarks.BookmarksEntry.COLUMN_NAME_TITLE, title);
        values.put(Bookmarks.BookmarksEntry.COLUMN_NAME_SNIPPET,snippet);

        // Insert the new row, returning the primary key value of the new row
        long newRowId = db.insert(Bookmarks.BookmarksEntry.TABLE_NAME, null, values);
        db.close();
        if(newRowId==-1){
            return false;//not bookmarked
        }
        return true;//bookmarked
    }
    public boolean removeEntry(String title){
        if(!isExistTitle(title))
            return false;
        SQLiteDatabase db = getWritableDatabase();
        // Define 'where' part of query.
        String selection = Bookmarks.BookmarksEntry.COLUMN_NAME_TITLE + " LIKE ?";
        // Specify arguments in placeholder order.
        String[] selectionArgs = { title };
        // Issue SQL statement.
        int rowcount=db.delete(Bookmarks.BookmarksEntry.TABLE_NAME, selection, selectionArgs);
        db.close();
        if(rowcount==0){
            return true;//bookmarked
        }
        return false;//not bookmarked
    }
    public ArrayList<String[]> getAllRecords(){
        SQLiteDatabase db=getReadableDatabase();
        ArrayList<String[]> result=null;
        Cursor cursor=db.rawQuery("select * from "+Bookmarks.BookmarksEntry.TABLE_NAME,null);
        if (cursor.moveToLast()) {
            int i=0;
            result=new ArrayList<>(cursor.getCount());
            while (!cursor.isBeforeFirst()) {
                String[] item=new String[2];
                item[0]=cursor.getString(cursor.getColumnIndex(Bookmarks.BookmarksEntry.COLUMN_NAME_TITLE));
                item[1]=cursor.getString(cursor.getColumnIndex(Bookmarks.BookmarksEntry.COLUMN_NAME_SNIPPET));
                result.add(i++,item);
                cursor.moveToPrevious();
            }
        }
        cursor.close();
        return result;
    }
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }

}
