/* //device/samples/SampleCode/src/com/android/samples/app/RemoteServiceInterface.java
**
** Copyright 2007, The Android Open Source Project
**
** Licensed under the Apache License, Version 2.0 (the "License"); 
** you may not use this file except in compliance with the License. 
** You may obtain a copy of the License at 
**
**     http://www.apache.org/licenses/LICENSE-2.0 
**
** Unless required by applicable law or agreed to in writing, software 
** distributed under the License is distributed on an "AS IS" BASIS, 
** WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
** See the License for the specific language governing permissions and 
** limitations under the License.
*/

package com.kojimahome.music21;

import android.graphics.Bitmap;

interface IMediaPlaybackService
{
    void openFile(String path, boolean oneShot);
    void openFileAsync(String path);
    void open(in long [] list, int position);
    int getQueuePosition();
    boolean isPlaying();
	int abRepeatingState();
	boolean abListTraverseMode();
	void setAbListTraverseMode(boolean mode);
	long getAbRecRowId ();
	void setAbRecRowId (long id);
	boolean getAbEdited ();
	void setAbEdited (boolean edited);
	void setABPause(long abpause);
	boolean getInABPause();
	void setAPos();
	boolean setBPos();
	void shiftAPos(long delta);
	void shiftBPos(long delta);
	long getAPos();
	long getBPos();
	void clearABPos();
    void stop();
    void pause();
    void play();
    void prev();
    void next();
    boolean currentAbPoints();
    boolean prevAbPoints();
    boolean nextAbPoints();
    void setPosInAbList(int pos);
    long duration();
    long position();
    long seek(long pos);
    long jump(long jumpdist);
    String getTrackName();
    String getAlbumName();
    String getData();
    long getAlbumId();
    String getArtistName();
    long getArtistId();
    void enqueue(in long [] list, int action);
    long [] getQueue();
    void moveQueueItem(int from, int to);
    void setQueuePosition(int index);
    String getPath();
    long getAudioId();
    void setShuffleMode(int shufflemode);
    int getShuffleMode();
    int removeTracks(int first, int last);
    int removeTrack(long id);
    void setRepeatMode(int repeatmode);
    int getRepeatMode();
    int getMediaMountedCount();
}

