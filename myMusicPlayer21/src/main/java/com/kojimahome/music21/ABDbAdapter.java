package com.kojimahome.music21;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;


public class ABDbAdapter {
    public static final String KEY_ROWID = "_id";
    public static final String KEY_ABTITLE = "abtitle";
    public static final String KEY_APOS = "apos";
    public static final String KEY_BPOS = "bpos";
    public static final String KEY_MUSICPATH = "musicpath";
    public static final String KEY_MUSICTITLE = "musictitle";
    public static final String KEY_MUSICDURATION = "musicduration";
    public static final String KEY_POSITION = "position";
    
//    for lyric activity
    public static final String KEY_MUSIC_DATA = "musicdata";

    private static final String TAG = "ABDbAdapter";
    private DatabaseHelper mDbHelper;
    private SQLiteDatabase mDb;

    /**
     * Database creation sql statement
     */
    private static final String DATABASE_CREATE =
        "create table abpos (_id integer primary key autoincrement, "
        + "abtitle text not null, "
        + "musicpath text, "
        + "musictitle text, "
        + "musicduration integer, "
        + "apos integer, "
        + "bpos integer, "
        + "extra text);";

//    private static final String DATABASE_NAME = "abrepeat";
    
    public static final String DB_DIRECTORY =
        Environment.getExternalStorageDirectory() +
        "/com.learnerstechlab/abrepeat/";
    public static final String DATABASE_NAME = DB_DIRECTORY + "abrepeat";
    private static final String OLD_DATABASE_NAME = 
    	Environment.getDataDirectory() +
    	"/data/com.kojimahome.music21/databases/abrepeat";
    
    private static final String DATABASE_TABLE = "abpos";
    private static final int DATABASE_VERSION = 1;

    private final Context mCtx;

    private static class DatabaseHelper extends SQLiteOpenHelper {

        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }
        
        DatabaseHelper(Context context, String db_name) {
            super(context, db_name, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {

            db.execSQL(DATABASE_CREATE);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
                    + newVersion + ", which will destroy all old data");
            db.execSQL("DROP TABLE IF EXISTS notes");
            onCreate(db);
        }
    }

    /**
     * Constructor - takes the context to allow the database to be
     * opened/created
     * 
     * @param ctx the Context within which to work
     */
    public ABDbAdapter(Context ctx) {
        this.mCtx = ctx;
    }

    /**
     * Open the notes database. If it cannot be opened, try to create a new
     * instance of the database. If it cannot be created, throw an exception to
     * signal the failure
     * 
     * @return this (self reference, allowing this to be chained in an
     *         initialization call)
     * @throws SQLException if the database could be neither opened or created
     */
//    public ABDbAdapter open() throws SQLException {
//        mDbHelper = new DatabaseHelper(mCtx);
//        mDb = mDbHelper.getWritableDatabase();
//        return this;
//    }

    public ABDbAdapter open() throws SQLException {
//    	mDbHelper = new DatabaseHelper (mCtx);
    	if(mDb != null && mDb.isOpen()) {
            return this;
        } else {
        	File databaseFile = new File(DATABASE_NAME);
        	if (!databaseFile.exists()) {
        		File oldDatabaseFile = new File(OLD_DATABASE_NAME);
//        		// fallback test
//        	mDb = mDbHelper.getWritableDatabase();
        		if (oldDatabaseFile.exists()) {
        			if (copyFile(oldDatabaseFile, databaseFile)) {
        				oldDatabaseFile.delete();
            			mDb = SQLiteDatabase.openOrCreateDatabase(DATABASE_NAME, null);
        			} else {
        				Toast.makeText(mCtx, R.string.db_on_sd_failed, Toast.LENGTH_LONG).show();
//  fix      				mDb = SQLiteDatabase.openOrCreateDatabase(OLD_DATABASE_NAME, null);
        				mDbHelper = new DatabaseHelper (mCtx, OLD_DATABASE_NAME);
        				mDb = mDbHelper.getWritableDatabase();
        			}
        		} else {
        			if(!new File(DB_DIRECTORY).exists()) {
                        new File(DB_DIRECTORY).mkdirs();
                    }
        			try {
        				mDb = SQLiteDatabase.openOrCreateDatabase(DATABASE_NAME, null);
        				mDbHelper = new DatabaseHelper (mCtx, DATABASE_NAME);
        				mDbHelper.onCreate(mDb);
        			} catch (Exception e) {
        				Toast.makeText(mCtx, R.string.db_on_sd_failed, Toast.LENGTH_LONG).show();
//  fix      				mDb = SQLiteDatabase.openOrCreateDatabase(OLD_DATABASE_NAME, null);
        				mDbHelper = new DatabaseHelper (mCtx, OLD_DATABASE_NAME);
        				mDb = mDbHelper.getWritableDatabase();
        			}
//                    mDbHelper.onCreate(mDb);
        		}
        	} else {
        		mDbHelper = new DatabaseHelper (mCtx, DATABASE_NAME);
        		mDb = SQLiteDatabase.openOrCreateDatabase(DATABASE_NAME, null);
        	}
        }
    	return this;
    }
    
    public void close() {
        mDbHelper.close();
    }


    /**
     * Create a new note using the title and body provided. If the note is
     * successfully created return the new rowId for that note, otherwise return
     * a -1 to indicate failure.
     * 
     * @param title the title of the note
     * @param body the body of the note
     * @return rowId or -1 if failed
     */
    public long createABPos(String abtitle, long apos, long bpos, String musicpath, String musictitle,
    		long musicduration) {
        ContentValues initialValues = new ContentValues();
        initialValues.put(KEY_ABTITLE, abtitle);
        initialValues.put(KEY_APOS, apos);
        initialValues.put(KEY_BPOS, bpos);
        initialValues.put(KEY_MUSICPATH, musicpath);
        initialValues.put(KEY_MUSICTITLE, musictitle);
        initialValues.put(KEY_MUSICDURATION, musicduration);

        return mDb.insert(DATABASE_TABLE, null, initialValues);
    }
    
    public boolean checkIfABPosExists(String whereclause) {

        Cursor c = mDb.query(DATABASE_TABLE, new String[] {KEY_ROWID},
                whereclause, null, null, null, null);
        
        if (c == null) {
        	return false;
        } else {
        	if (c.getCount()==0) {
        		return false;
        	} else {
        		return true;
        	}
        }
    }

    /**
     * Delete the note with the given rowId
     * 
     * @param rowId id of note to delete
     * @return true if deleted, false otherwise
     */
    public boolean deleteABPos(long rowId) {

        return mDb.delete(DATABASE_TABLE, KEY_ROWID + "=" + rowId, null) > 0;
    }
    
    public boolean deleteAllABPos() {

        return mDb.delete(DATABASE_TABLE, null, null) > 0;
    }
    
    public boolean deleteWhereABPos(String whereclause) {

        return mDb.delete(DATABASE_TABLE, whereclause, null) > 0;
    }

    /**
     * Return a Cursor over the list of all notes in the database
     * 
     * @return Cursor over all notes
     */
    public Cursor fetchABPos(String whereclause, String orderby) throws SQLException {
    	
    	Cursor c = mDb.query(DATABASE_TABLE, new String[] {KEY_ROWID, 
        		KEY_ABTITLE, KEY_APOS, KEY_BPOS, KEY_MUSICPATH, KEY_MUSICTITLE, KEY_MUSICDURATION},
                whereclause, null, null, null, orderby);

        return c;
    }

    /**
     * Return a Cursor positioned at the note that matches the given rowId
     * 
     * @param rowId id of note to retrieve
     * @return Cursor positioned to matching note, if found
     * @throws SQLException if note could not be found/retrieved
     */
    public Cursor fetchAllABPos() throws SQLException {

    	String musicpath = "test";
    	Long musicduration = (long) 0;
    	String whereclause = ABDbAdapter.KEY_MUSICPATH + "=" + musicpath;
    	
        Cursor mCursor =

            mDb.query(true, DATABASE_TABLE, new String[] {KEY_ROWID,
                    KEY_ABTITLE, KEY_APOS, KEY_BPOS, KEY_MUSICPATH, KEY_MUSICTITLE, KEY_MUSICDURATION}, 
                    whereclause, null,
                    null, null, null, null);
        	
        
//        if (mCursor != null) {
//            mCursor.moveToFirst();
//        }
        return mCursor;

    }

    /**
     * Update the note using the details provided. The note to be updated is
     * specified using the rowId, and it is altered to use the title and body
     * values passed in
     * 
     * @param rowId id of note to update
     * @param title value to set note title to
     * @param body value to set note body to
     * @return true if the note was successfully updated, false otherwise
     */
    public boolean updateABPos(long rowId, String abtitle) {
        ContentValues args = new ContentValues();
        args.put(KEY_ABTITLE, abtitle);
//        args.put(KEY_APOS, apos);
//        args.put(KEY_BPOS, bpos);
//        args.put(KEY_MUSICPATH, musicpath);
//        args.put(KEY_MUSICTITLE, musictitle);
//        args.put(KEY_MUSICDURATION, musicduration);

        return mDb.update(DATABASE_TABLE, args, KEY_ROWID + "=" + rowId, null) > 0;
    }
    
    public boolean updateABPos(long rowId, long apos, long bpos) {
        ContentValues args = new ContentValues();
        args.put(KEY_APOS, apos);
        args.put(KEY_BPOS, bpos);

        return mDb.update(DATABASE_TABLE, args, KEY_ROWID + "=" + rowId, null) > 0;
    }
    
    
    private boolean copyFile (File oldfile, File newfile) {
    	try {
            FileChannel src = new FileInputStream(oldfile).getChannel();
            File newdir = new File(newfile.getParent());
            if (!newdir.exists()) newdir.mkdirs();            	
            FileChannel dst = new FileOutputStream(newfile).getChannel();
            dst.transferFrom(src, 0, src.size());
            src.close();
            dst.close();
            return true;
    	} catch (Exception e) {
    		return false;
    	}
    }
    
}
