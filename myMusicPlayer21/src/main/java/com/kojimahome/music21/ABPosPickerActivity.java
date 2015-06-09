/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.kojimahome.music21;

import com.google.android.gms.analytics.HitBuilders;
import com.kojimahome.music21.CreatePlaylist;
import com.kojimahome.music21.DeleteItems;
import com.kojimahome.music21.MediaPlaybackService;
import com.kojimahome.music21.MusicBrowserActivity;
import com.kojimahome.music21.MusicUtils;
import com.kojimahome.music21.R;
import com.kojimahome.music21.R.drawable;
import com.kojimahome.music21.R.id;
import com.kojimahome.music21.R.layout;
import com.kojimahome.music21.R.string;
import com.kojimahome.music21.MusicUtils.ServiceToken;

import android.app.AlertDialog;
import android.app.KeyguardManager;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ContentUris;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.RemoteException;
import android.provider.MediaStore;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.Locale;

public class ABPosPickerActivity extends ListActivity 
	implements View.OnCreateContextMenuListener, MusicUtils.Defs
{
	private static final String LOGTAG = "ABPosPickerActivity";
	private static final int DELETE_ABPOS = CHILD_MENU_BASE + 1;
	private static final int RENAME_ABPOS = CHILD_MENU_BASE + 2;
	private static final int AB_DISPLAY_ORDER = CHILD_MENU_BASE + 3;
	private static final int DELETE_LISTED = CHILD_MENU_BASE + 4;
	private static final int DELETE_DATABASE = CHILD_MENU_BASE + 5;
	private static final int DB_BACKUP = CHILD_MENU_BASE + 6;
	private static final int DB_RESTORE = CHILD_MENU_BASE + 7;
	private static final int ADJUSTABLE_BM = CHILD_MENU_BASE + 8;
	private static final int EXPORT_AB_SOUND = CHILD_MENU_BASE + 9;
	public static final String CALLEDFROMFLOATPAD = "calledfromfloatpad";
	public static final String ACTION_ABPOSPICKER_CLOSED
							="leanerstechlab.music21.abpospickerclosed";
	private static boolean mCalledFromFloatPad = false;
	private static String mOrderBy;
    private ServiceToken mToken;

    public ABPosPickerActivity()
    {
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle icicle)
    {
        super.onCreate(icicle);
        
        mMusicTitle = getIntent().getStringExtra(ABDbAdapter.KEY_MUSICTITLE);
        mMusicPath = getIntent().getStringExtra(ABDbAdapter.KEY_MUSICPATH);
        mMusicDuration = getIntent().getLongExtra(ABDbAdapter.KEY_MUSICDURATION, 0);
        mMusicData = getIntent().getStringExtra(ABDbAdapter.KEY_MUSIC_DATA);
        mCalledFromFloatPad = getIntent().getBooleanExtra(CALLEDFROMFLOATPAD, false);
        mABDbHelper = new ABDbAdapter(this);
        mABDbHelper.open();
        setTitle(mMusicTitle);
        mToken = MusicUtils.bindToService(this);
        init();
    }

    @Override
    public void onDestroy() {
        MusicUtils.unbindFromService(mToken);
        super.onDestroy();
        Intent i2 = new Intent(ACTION_ABPOSPICKER_CLOSED);
//    	i2.setClass(this, WidgetsWindow.class);
    	sendBroadcast(i2);
        if (mABDbHelper != null) {
        	mABDbHelper.close();
        }
        if (mCursor != null) {
            mCursor.close();
        }
    }

    public void init() {

        setContentView(R.layout.abpos_picker_activity);
        ListView lv = getListView();
        lv.setOnCreateContextMenuListener(this);

        MakeCursor();
//        if (null == mCursor || 0 == mCursor.getCount()) {
        if (null == mCursor) {
            return;
        }
        
        mSdMessage = (TextView) findViewById(R.id.sd_message);
        
        if (mCursor.getCount() == 0){ 
        	mSdMessage.setText("No Entry");
        	mSdMessage.setVisibility(View.VISIBLE);
        } else {
        	mSdMessage.setVisibility(View.GONE);
        }

        PickListAdapter adapter = new PickListAdapter(
                this,
                R.layout.track_list_item,
                mCursor,
                new String[] {},
                new int[] {});

        setListAdapter(adapter);
//        sendDbModifiedtoService();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        // Don't show the menu items if we got launched by path/filedescriptor, or
        // if we're in one shot mode. In most cases, these menu items are not
        // useful in those modes, so for consistency we never show them in these
        // modes, instead of tailoring them to the specific file being played.

	        menu.add(1, AB_DISPLAY_ORDER, 0, R.string.abpos_order)
	        		.setIcon(R.drawable.abpos_order);
            menu.add(1, DELETE_LISTED, 0, R.string.ab_delete_listed)
                    .setIcon(R.drawable.delete_listed);
            menu.add(1, DELETE_DATABASE, 0, R.string.ab_delete_database)
                    .setIcon(R.drawable.delete_database);
            menu.add(1, DB_BACKUP, 0, R.string.ab_db_backup)
            .setIcon(R.drawable.backup_database);
            menu.add(1, DB_RESTORE, 0, R.string.ab_db_restore)
            .setIcon(R.drawable.restore_database);
            menu.add(1, ADJUSTABLE_BM, 0, R.string.ab_adjustable_bm_on);
            return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
    	MenuItem item = menu.findItem(ADJUSTABLE_BM);
    	if (MusicUtils.getBooleanPref(this, MusicUtils.Defs.NEW_BM, false)) {
    		item.setTitle(R.string.ab_adjustable_bm_off);
    	} else {
    		item.setTitle(R.string.ab_adjustable_bm_on);
    	}
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	AlertDialog.Builder mybuilder = new AlertDialog.Builder(this);
    	DeleteListedListener listedlistener = new DeleteListedListener();
    	CancelDeleteListener cancellistener = new CancelDeleteListener();
    	DeleteDatabaseListener databaselistener = new DeleteDatabaseListener();
    	BackupDatabaseListener backuplistener = new BackupDatabaseListener();
    	RestoreDatabaseListener restorelistener = new RestoreDatabaseListener();
    	
    	
        switch (item.getItemId()) {
        case AB_DISPLAY_ORDER:
        	Intent intent = new Intent();
            intent.setClass(this, ABPosDispOrderActivity.class);
            startActivityForResult(intent, AB_DISPLAY_ORDER);
            return super.onOptionsItemSelected(item);
        case DELETE_LISTED:
        	mybuilder.setTitle(R.string.ab_delete_listed);
        	mybuilder.setMessage(R.string.are_you_sure);
        	mybuilder.setPositiveButton(android.R.string.yes, listedlistener);
//        	mybuilder.setIcon(R.drawable.ic_dialog_time);
        	mybuilder.setNegativeButton(android.R.string.cancel, cancellistener);
        	mybuilder.setCancelable(true);
        	mybuilder.show();
            return super.onOptionsItemSelected(item);
//            break;
        case DELETE_DATABASE: 
        	mybuilder.setTitle(R.string.ab_delete_database);
        	mybuilder.setMessage(R.string.are_you_sure);
        	mybuilder.setPositiveButton(android.R.string.yes, databaselistener);
        	mybuilder.setNegativeButton(android.R.string.cancel, cancellistener);
        	mybuilder.setCancelable(true);
        	mybuilder.show();
            return super.onOptionsItemSelected(item);
//        	break;
        case DB_BACKUP:
        	mybuilder.setTitle(R.string.ab_db_backup);
        	mybuilder.setMessage(R.string.are_you_sure);
        	mybuilder.setPositiveButton(android.R.string.yes, backuplistener);
        	mybuilder.setNegativeButton(android.R.string.cancel, cancellistener);
        	mybuilder.setCancelable(true);
        	mybuilder.show();
        	break;
        case DB_RESTORE:
        	mybuilder.setTitle(R.string.ab_db_restore);
        	mybuilder.setMessage(R.string.are_you_sure);
        	mybuilder.setPositiveButton(android.R.string.yes, restorelistener);
        	mybuilder.setNegativeButton(android.R.string.cancel, cancellistener);
        	mybuilder.setCancelable(true);
        	mybuilder.show();
        	break;
        case ADJUSTABLE_BM:
        	if (MusicUtils.getBooleanPref(this, MusicUtils.Defs.NEW_BM, false)) {
        		MusicUtils.setBooleanPref(this, MusicUtils.Defs.NEW_BM, false);
        	} else {
        		MusicUtils.setBooleanPref(this, MusicUtils.Defs.NEW_BM, true);
        	}
        	break;
        }
//        mybuilder.setNegativeButton(android.R.string.cancel, cancellistener);
//    	mybuilder.setCancelable(true);
//    	mybuilder.show();
        return super.onOptionsItemSelected(item);
    }
    
    public class DeleteListedListener 
    	implements android.content.DialogInterface.OnClickListener {
    	public void onClick (DialogInterface v, int i) {
//    		mWhereClause = ABDbAdapter.KEY_MUSICPATH + "=\"" + mMusicPath + "\""
//    		+ " AND " + ABDbAdapter.KEY_MUSICDURATION + "=" + Long.toString(mMusicDuration);
    		mWhereClause = ABDbAdapter.KEY_MUSICTITLE + "=\"" + mMusicTitle + "\""
    		+ " AND " + ABDbAdapter.KEY_MUSICDURATION + ">=" + Long.toString(mMusicDuration - 1) 
    		+ " AND " + ABDbAdapter.KEY_MUSICDURATION + "<=" + Long.toString(mMusicDuration + 1);
        	if (mABDbHelper !=null) {
        		if (mABDbHelper.deleteWhereABPos(mWhereClause))
        		showToast(R.string.abpos_deleted_message);
        		sendDbModifiedtoService();
        		init();
        		
        	}
    		
    	}
    }
    
    public class DeleteDatabaseListener 
	implements android.content.DialogInterface.OnClickListener {
	public void onClick (DialogInterface v, int i) {
		if (mABDbHelper !=null) {
    		if (mABDbHelper.deleteAllABPos()) {
    			showToast(R.string.abpos_deleted_message);
    			sendDbModifiedtoService();
    			init();
    		}
    	}
		
	}
}
    
    public class CancelDeleteListener 
	implements android.content.DialogInterface.OnClickListener {
	public void onClick (DialogInterface v, int i) {
    	}
		
	}
    
    public class BackupDatabaseListener 
	implements android.content.DialogInterface.OnClickListener {
	public void onClick (DialogInterface v, int i) {
		try {
    		mABDbHelper.close();
            File sd = Environment.getExternalStorageDirectory();
            File data = Environment.getDataDirectory();
//            File dir = getFilesDir();

            if (sd.canWrite()) {
            	String currentDBPath;
            	File currentDB;
            	if (!new File(ABDbAdapter.DATABASE_NAME).exists()) {
	                currentDBPath = "/data/com.kojimahome.music21/databases/abrepeat";
	                currentDB = new File(data, currentDBPath);
            	} else {
            		currentDBPath = "/com.learnerstechlab/abrepeat/abrepeat";
                    currentDB = new File(sd, currentDBPath);
            	}
                String backupDBPath = "abrepeat";
                File backupDB = new File(sd, backupDBPath);

//                if (currentDB.exists()) {
                    FileChannel src = new FileInputStream(currentDB).getChannel();
                    FileChannel dst = new FileOutputStream(backupDB).getChannel();
                    dst.transferFrom(src, 0, src.size());
                    src.close();
                    dst.close();
//                }
            }
            mABDbHelper.open();
        } catch (Exception e) {
        	Log.e("ABPosPickerActivity","Backup Error: " + e);
        }
        showToast(R.string.ab_db_backupped_message);
        init();
	} 
}
    
    public class RestoreDatabaseListener 
	implements android.content.DialogInterface.OnClickListener {
	public void onClick (DialogInterface v, int i) {
		try {
    		mABDbHelper.close();
            File sd = Environment.getExternalStorageDirectory();
//            File data = Environment.getDataDirectory();

            if (sd.canWrite()) {
            	String currentDBPath;
            	File currentDB;
//            	if (!new File(ABDbAdapter.DATABASE_NAME).exists()) {
//	                currentDBPath = "/data/com.kojimahome.music21/databases/abrepeat";
//	                currentDB = new File(data, currentDBPath);
//            	} else {
            		currentDBPath = "/com.learnerstechlab/abrepeat/abrepeat";
                    currentDB = new File(sd, currentDBPath);
//            	}
                String backupDBPath = "abrepeat";
                File backupDB = new File(sd, backupDBPath);

//                if (currentDB.exists()&&backupDB.exists()) {
                    FileChannel src = new FileInputStream(backupDB).getChannel();
                    FileChannel dst = new FileOutputStream(currentDB).getChannel();
                    dst.transferFrom(src, 0, src.size());
                    src.close();
                    dst.close();
//                }
            }
            mABDbHelper.open();
        } catch (Exception e) {
        	Log.e("ABPosPickerActivity","Restore Error: " + e);
        }
        showToast(R.string.ab_db_restored_message);
        sendDbModifiedtoService();
        init();
	}
}
    
    private void showToast(int resid) {
        if (mToast == null) {
            mToast = Toast.makeText(this, "", Toast.LENGTH_SHORT);
        }
        mToast.setText(resid);
        mToast.show();
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (resultCode != RESULT_OK) {
            return;
        }
        switch (requestCode) {
            
            case RENAME_ABPOS:
            	String abtitle = intent.getStringExtra("rename");
            	Long rowId = intent.getLongExtra("rowid", -1);
            	if (mABDbHelper !=null && rowId != -1) mABDbHelper.updateABPos(rowId, abtitle);
            	sendDbModifiedtoService();
            	init();
//      
            	break;
            case AB_DISPLAY_ORDER:
            	sendDbModifiedtoService();
            	init();
            	break;
        }
        return;
    }
    
    
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id)
    {
        mCursor.moveToPosition(position);

        Intent result_intent = new Intent();
        result_intent.putExtra(ABDbAdapter.KEY_ROWID, mCursor.getLong(mCursor.getColumnIndex(ABDbAdapter.KEY_ROWID)));
        result_intent.putExtra(ABDbAdapter.KEY_MUSICTITLE, mCursor.getLong(mCursor.getColumnIndex(ABDbAdapter.KEY_MUSICTITLE)));
        result_intent.putExtra(ABDbAdapter.KEY_MUSICPATH, mCursor.getLong(mCursor.getColumnIndex(ABDbAdapter.KEY_MUSICPATH)));
        result_intent.putExtra(ABDbAdapter.KEY_MUSICDURATION, mCursor.getLong(mCursor.getColumnIndex(ABDbAdapter.KEY_MUSICDURATION)));
        result_intent.putExtra(ABDbAdapter.KEY_APOS, mCursor.getLong(mCursor.getColumnIndex(ABDbAdapter.KEY_APOS)));
        result_intent.putExtra(ABDbAdapter.KEY_BPOS, mCursor.getLong(mCursor.getColumnIndex(ABDbAdapter.KEY_BPOS)));
        result_intent.putExtra(ABDbAdapter.KEY_POSITION, mCursor.getPosition());
        
        if (mCalledFromFloatPad) {
        	result_intent.setClass(this, MediaPlaybackService.class);
        	result_intent.setAction(MediaPlaybackService.SERVICECMD);
        	result_intent.putExtra(MediaPlaybackService.CMDNAME, MediaPlaybackService.CMDABPOSSELECTED);
        	startService(result_intent);
//        	Intent i2 = new Intent(ACTION_ABPOSPICKER_CLOSED);
////        	i2.setClass(this, WidgetsWindow.class);
//        	sendBroadcast(i2);
        	finish();
        } else {
        	setResult(RESULT_OK, result_intent);
            finish();
        }
            return;
    }

    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfoIn) {

        AdapterContextMenuInfo mi = (AdapterContextMenuInfo) menuInfoIn;

        menu.add(0, DELETE_ABPOS, 0, R.string.delete_playlist_menu);
        menu.add(0, RENAME_ABPOS, 0, R.string.rename_playlist_menu);
        menu.add(0, EXPORT_AB_SOUND, 0, R.string.export_ab_sound);
      
        if ((mCursor != null) && (mi != null)) {
	        mCursor.moveToPosition(mi.position);
	        menu.setHeaderTitle(mCursor.getString(mCursor.getColumnIndexOrThrow(
	                ABDbAdapter.KEY_ABTITLE)));
//	        long bpos = mCursor.getLong(mCursor.getColumnIndexOrThrow(
//	                ABDbAdapter.KEY_BPOS));
//	        if (bpos > 0) menu.add(0, EXPORT_AB_SOUND, 0, R.string.export_ab_sound);
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterContextMenuInfo mi = (AdapterContextMenuInfo) item.getMenuInfo();
        Long rowId = mCursor.getLong(mCursor.getColumnIndexOrThrow(
                ABDbAdapter.KEY_ROWID));
        String abtitle = mCursor.getString(mCursor.getColumnIndexOrThrow(
                ABDbAdapter.KEY_ABTITLE));
        switch (item.getItemId()) {
            case DELETE_ABPOS:
            	mABDbHelper.deleteABPos(rowId);
                Toast.makeText(this, R.string.abpos_deleted_message, Toast.LENGTH_SHORT).show();
                
                sendDbModifiedtoService();
                init();
//                if (mCursor.getCount() == 0) {
//                    setTitle(R.string.no_playlists_title);
//                }
                break;

            case RENAME_ABPOS:
                Intent intent = new Intent();
                intent.setClass(this, RenameABPos.class);
                intent.putExtra("rename", abtitle);
                intent.putExtra("rowid", rowId);
                startActivityForResult(intent, RENAME_ABPOS);
                break;
            case EXPORT_AB_SOUND:
                MyApplication.tracker().send(new HitBuilders.EventBuilder("Action", "Button")
                        .setLabel("ExportABinList")
                        .build());
                try {
                    Intent intent2 = new Intent(Intent.ACTION_EDIT,
                            Uri.parse(mMusicData));
                    intent2.putExtra("was_get_content_intent",
//                            mWasGetContentIntent);
                    		false);
                    intent2.putExtra(com.ringdroid.RingdroidEditActivity.APOS,
                    		mCursor.getLong(mCursor.getColumnIndex(ABDbAdapter.KEY_APOS)));
                    intent2.putExtra(com.ringdroid.RingdroidEditActivity.BPOS,
                    		mCursor.getLong(mCursor.getColumnIndex(ABDbAdapter.KEY_BPOS)));
                    intent2.putExtra(com.ringdroid.RingdroidEditActivity.ABTITLE,
                    		mCursor.getString(mCursor.getColumnIndex(ABDbAdapter.KEY_ABTITLE)));
                    intent2.setClassName(
//                            "com.ringdroid",
                    		"com.kojimahome.music21",
                    "com.ringdroid.RingdroidEditActivity");
                    startActivityForResult(intent2, EXPORT_AB_SOUND);
                } catch (Exception e) {
                    Log.e(LOGTAG, "Couldn't start ringdroid editor:"+e);
                }
            	break;
        }
        return true;
    }
    
    private void sendDbModifiedtoService() {
    	Log.i(LOGTAG, "DB Modified Msg Sent");
    	Intent intent = new Intent();
        intent.setAction(MediaPlaybackService.SERVICECMD);
        intent.putExtra("command", MediaPlaybackService.AB_DB_MODIFIED);
        sendBroadcast(intent);
    }

    
    
    
    private void MakeCursor() {


        Cursor[] cs;
        // Use ArrayList for the moment, since we don't know the size of
        // Cursor[]. If the length of Corsor[] larger than really used,
        // a NPE will come up when access the content of Corsor[].
        ArrayList<Cursor> cList = new ArrayList<Cursor>();

        Cursor c;

//        	mWhereClause = ABDbAdapter.KEY_MUSICPATH + "=\"" + mMusicPath + "\""
//        		+ " AND " + ABDbAdapter.KEY_MUSICDURATION + "=" + Long.toString(mMusicDuration);
//        	mWhereClause = ABDbAdapter.KEY_MUSICTITLE + "=\"" + mMusicTitle + "\""
//    		+ " AND " + ABDbAdapter.KEY_MUSICDURATION + "=" + Long.toString(mMusicDuration);
        	mWhereClause = ABDbAdapter.KEY_MUSICTITLE + "=\"" + mMusicTitle + "\""
    		+ " AND " + ABDbAdapter.KEY_MUSICDURATION + ">=" + Long.toString(mMusicDuration - 1) 
    		+ " AND " + ABDbAdapter.KEY_MUSICDURATION + "<=" + Long.toString(mMusicDuration + 1);
        	
        	switch (MusicUtils.getIntPref(this, "abposdisporder", ABPosDispOrderActivity.CREATION_NORMAL)) {
        	case ABPosDispOrderActivity.CREATION_NORMAL:
        		mOrderBy = ABDbAdapter.KEY_ROWID + " asc";
    			break;
    		case ABPosDispOrderActivity.CREATION_REVERSE:
    			mOrderBy = ABDbAdapter.KEY_ROWID + " desc";
    			break;
    		case ABPosDispOrderActivity.APOS_NORMAL:
    			mOrderBy = ABDbAdapter.KEY_APOS + " asc";
    			break;
    		case ABPosDispOrderActivity.APOS_REVERSE:
    			mOrderBy = ABDbAdapter.KEY_APOS + " desc";
    			break;
    		case ABPosDispOrderActivity.NAME_NORMAL:
    			mOrderBy = ABDbAdapter.KEY_ABTITLE + " asc";
    			break;
    		case ABPosDispOrderActivity.NAME_REVERSE:
    			mOrderBy = ABDbAdapter.KEY_ABTITLE + " desc";
    			break;
    		default:
    			mOrderBy = ABDbAdapter.KEY_ABTITLE + " asc";
        	}
        	
        	c = mABDbHelper.fetchABPos(mWhereClause, mOrderBy);
        	if (c != null) 
        		cList.add(c);
        	

        // Get the ArrayList size.
        int size = cList.size();
        if (0 == size) {
            // If no video/audio/SDCard exist, return.
            mCursor = null;
            return;
        }

        // The size is known now, we're sure each item of Cursor[] is not null.
        cs = new Cursor[size];
        cs = cList.toArray(cs);
        mCursor = new SortCursor(cs, ABDbAdapter.KEY_APOS);
    }

    private Cursor mCursor;
    private String mMusicTitle;
    private String mMusicPath;
    private long mMusicDuration;
    private String mMusicData;
    private String mWhereClause;
    private ABDbAdapter mABDbHelper;
    private Toast mToast;
    private TextView mSdMessage;

    static class PickListAdapter extends SimpleCursorAdapter {
        int mABTitleIdx;
        int mAPosIdx;
        int mBPosIdx;
        int mDurationIdx;
        int mMusicTitleIdx;

        PickListAdapter(Context context, int layout, Cursor cursor, String[] from, int[] to) {
            super(context, layout, cursor, from, to);

            mABTitleIdx = cursor.getColumnIndexOrThrow(ABDbAdapter.KEY_ABTITLE);
            mAPosIdx = cursor.getColumnIndexOrThrow(ABDbAdapter.KEY_APOS);
            mBPosIdx = cursor.getColumnIndexOrThrow(ABDbAdapter.KEY_BPOS);
            mDurationIdx = cursor.getColumnIndexOrThrow(ABDbAdapter.KEY_MUSICDURATION);
            mMusicTitleIdx = cursor.getColumnIndexOrThrow(ABDbAdapter.KEY_MUSICTITLE);
        }
        
        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
           View v = super.newView(context, cursor, parent);
           ImageView iv = (ImageView) v.findViewById(R.id.icon);
           iv.setVisibility(View.VISIBLE);
           ViewGroup.LayoutParams p = iv.getLayoutParams();
           p.width = ViewGroup.LayoutParams.WRAP_CONTENT;
           p.height = ViewGroup.LayoutParams.WRAP_CONTENT;

           TextView tv = (TextView) v.findViewById(R.id.duration);
           tv.setVisibility(View.GONE);
           iv = (ImageView) v.findViewById(R.id.play_indicator);
           iv.setVisibility(View.GONE);
           
           return v;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {

            TextView tv = (TextView) view.findViewById(R.id.line1);
            String name = cursor.getString(mABTitleIdx);
            String durationformat;
        	StringBuilder sFormatBuilder = new StringBuilder();
        	Formatter sFormatter = new Formatter(sFormatBuilder, Locale.getDefault());
        	Object[] sTimeArgs = new Object[5];
        	final Object[] timeArgs = sTimeArgs;
        	Long apos;
        	Long bpos;
        	Long abduration;
        	String apostimestring;
        	String bpostimestring;
        	String abpostimestring;
        	
            tv.setText(name);
            
            
            apos = cursor.getLong(mAPosIdx);
            bpos = cursor.getLong(mBPosIdx);
            abduration = (bpos - apos);
            apos = apos/1000;
            bpos = bpos/1000;
            abduration = abduration/1000;
            
            TextView tv3 = (TextView) view.findViewById(R.id.line2);
            
    		durationformat = 
                apos < 3600 ? "%2$d:%5$02d" : "%1$d:%3$02d:%5$02d";
     		sFormatBuilder.setLength(0);
             timeArgs[0] = apos / 3600;
             timeArgs[1] = apos / 60;
             timeArgs[2] = (apos / 60) % 60;
             timeArgs[3] = apos;
             timeArgs[4] = apos % 60;
     		apostimestring = sFormatter.format(durationformat, timeArgs).toString();
     		
     		durationformat = 
                bpos < 3600 ? "%2$d:%5$02d" : "%1$d:%3$02d:%5$02d";
     		sFormatBuilder.setLength(0);
             timeArgs[0] = bpos / 3600;
             timeArgs[1] = bpos / 60;
             timeArgs[2] = (bpos / 60) % 60;
             timeArgs[3] = bpos;
             timeArgs[4] = bpos % 60;
     		bpostimestring = sFormatter.format(durationformat, timeArgs).toString();
     		
     		if (bpos != 0) {
     			abpostimestring = apostimestring + "  --  " +bpostimestring;
     		} else {
     			abpostimestring = apostimestring;  //bookmark
     		}
     		
     		tv3.setText(abpostimestring);
     		tv3.setVisibility(View.VISIBLE);
     		
     		if (bpos != 0) {
            TextView tv2 = (TextView) view.findViewById(R.id.duration);
    		durationformat = 
                abduration < 3600 ? "%2$d:%5$02d" : "%1$d:%3$02d:%5$02d";
     		sFormatBuilder.setLength(0);
             timeArgs[0] = abduration / 3600;
             timeArgs[1] = abduration / 60;
             timeArgs[2] = (abduration / 60) % 60;
             timeArgs[3] = abduration;
             timeArgs[4] = abduration % 60;
     		tv2.setText(sFormatter.format(durationformat, timeArgs).toString());
            tv2.setVisibility(View.VISIBLE);
     		}

            StringBuilder builder = new StringBuilder();

            if (name == null) {
                builder.append(context.getString(R.string.unknown_album_name));
            } else {
                builder.append(name);
            }
            builder.append("\n");

            tv.setText(builder.toString());

            ImageView iv = (ImageView) view.findViewById(R.id.icon);
            if (bpos != 0) {
            	iv.setImageResource(R.drawable.ic_mp_repeat_off_btn); //ab repeat
            } else {
            	iv.setImageResource(R.drawable.ic_bookmark); //bookmark
            }
            iv.setBackgroundResource(android.R.color.white);

        }
    }
}
