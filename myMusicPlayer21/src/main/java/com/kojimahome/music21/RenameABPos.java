/*
 * Copyright (C) 2008 The Android Open Source Project
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

import com.kojimahome.music21.R;
import com.kojimahome.music21.R.id;
import com.kojimahome.music21.R.layout;
import com.kojimahome.music21.R.string;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.media.AudioManager;
import android.content.Intent;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class RenameABPos extends Activity
{
    private EditText mPlaylist;
    private TextView mPrompt;
    private Button mSaveButton;
    private long mRenameId;
    private String mOriginalName;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.create_playlist);
        getWindow().setLayout(WindowManager.LayoutParams.FILL_PARENT,
                                    WindowManager.LayoutParams.WRAP_CONTENT);

        mPrompt = (TextView)findViewById(R.id.prompt);
        mPlaylist = (EditText)findViewById(R.id.playlist);
        mSaveButton = (Button) findViewById(R.id.create);
        mSaveButton.setOnClickListener(mOpenClicked);

        ((Button)findViewById(R.id.cancel)).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                finish();
            }
        });


        mRenameId = getIntent().getLongExtra("rowid", -1);
        mOriginalName = getIntent().getStringExtra("rename");
        String defaultname = mOriginalName;
        
        if (mOriginalName == null || defaultname == null) {
            Log.i("@@@@", "Rename failed: " + mRenameId + "/" + defaultname);
            finish();
            return;
        }
        
        String promptformat;
        if (mOriginalName.equals(defaultname)) {
            promptformat = getString(R.string.rename_playlist_same_prompt);
        } else {
            promptformat = getString(R.string.rename_playlist_diff_prompt);
        }
                
        String prompt = String.format(promptformat, mOriginalName, defaultname);
        mPrompt.setText(prompt);
        mPlaylist.setText(defaultname);
        mPlaylist.setSelection(defaultname.length());
        mPlaylist.addTextChangedListener(mTextWatcher);
        setSaveButton();
    }
    
    TextWatcher mTextWatcher = new TextWatcher() {
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            // don't care about this one
        }
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            // check if playlist with current name exists already, and warn the user if so.
            setSaveButton();
        };
        public void afterTextChanged(Editable s) {
            // don't care about this one
        }
    };
    
    private void setSaveButton() {
        String typedname = mPlaylist.getText().toString();
        if (typedname.trim().length() == 0) {
            mSaveButton.setEnabled(false);
        } else {
            mSaveButton.setEnabled(true);
            if (idForplaylist(typedname) >= 0
                    && ! mOriginalName.equals(typedname)) {
                mSaveButton.setText(R.string.create_playlist_overwrite_text);
            } else {
                mSaveButton.setText(R.string.create_playlist_create_text);
            }
        }

    }
    
    private int idForplaylist(String name) {
        Cursor c = MusicUtils.query(this, MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,
                new String[] { MediaStore.Audio.Playlists._ID },
                MediaStore.Audio.Playlists.NAME + "=?",
                new String[] { name },
                MediaStore.Audio.Playlists.NAME);
        int id = -1;
        if (c != null) {
            c.moveToFirst();
            if (!c.isAfterLast()) {
                id = c.getInt(0);
            }
        }
        c.close();
        return id;
    }
    
    
    
    @Override
    public void onSaveInstanceState(Bundle outcicle) {
        outcicle.putString("defaultname", mPlaylist.getText().toString());
        outcicle.putLong("rename", mRenameId);
    }
    
    @Override
    public void onResume() {
        super.onResume();
    }

    private View.OnClickListener mOpenClicked = new View.OnClickListener() {
        public void onClick(View v) {
            String name = mPlaylist.getText().toString();
            if (name != null && name.length() > 0) {
                Intent result_intent = new Intent();
                result_intent.putExtra("rename", mPlaylist.getText().toString());
                result_intent.putExtra("rowid", mRenameId);
                setResult(RESULT_OK, result_intent);
                Toast.makeText(RenameABPos.this, R.string.abpos_renamed_message, Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    };
}
