package com.kojimahome.music21;

import android.net.Uri;
import android.os.Bundle;
import android.app.Activity;
import android.app.ListActivity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

public class HelpListActivity extends ListActivity implements MusicUtils.Defs, OnItemClickListener {
	private ListView lv = null;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.help_list_main);
        lv = getListView();
        
        HelpListItem helplist[] = new HelpListItem[] {
        		new HelpListItem(R.string.ab_help_ab_sub, R.layout.ab_help_activity),
        		new HelpListItem(R.string.ab_help_adjust_ab_sub, R.layout.adjust_ab_help_activity),
        		new HelpListItem(R.string.ab_help_interval_ab_sub, R.layout.interval_ab_help_activity),
        		new HelpListItem(R.string.ab_help_jump_sub, R.layout.jump_help_activity),
        		new HelpListItem(R.string.ab_help_bookmark_sub, R.layout.bookmark_help_activity),
        		new HelpListItem(R.string.ab_help_list_traverse_sub, R.layout.list_traverse_help_activity),
        		new HelpListItem(R.string.ab_help_manage_sub, R.layout.manage_list_help_activity),
        		new HelpListItem(R.string.ab_help_lyric_sub, R.layout.lyric_help_activity),
        		new HelpListItem(R.string.ab_help_migration_sub, R.layout.migration_help_activity),
        		new HelpListItem(R.string.ab_help_bluetooth_sub, R.layout.bluetooth_help_activity),
        		new HelpListItem(R.string.ab_help_float_pad_sub, R.layout.float_pad_help_activity),
        		new HelpListItem(R.string.sleep_timer, R.layout.sleep_timer_help_activity),
        		new HelpListItem(R.string.ab_help_ringdroid_sub, R.layout.ringdroid_help_activity),
        		new HelpListItem(R.string.release_note, R.layout.release_note_help_activity),
        };

        HelpAdapter adapter = new HelpAdapter(this, R.layout.help_list_item, helplist);
        lv.setAdapter(adapter);
        lv.setOnItemClickListener(this);
    }
    
    @Override
    public void onItemClick(AdapterView<?> adView, View target, int position, long id) {

    	Intent intent = new Intent();
    	intent.setClass(this, HelpPageActivity.class);
    	HelpItemHolder holder = null;
    	holder = (HelpItemHolder)target.getTag();
    	if (holder != null) {
    		int layoutid = holder.layoutId;
	    	intent.putExtra(HelpPageActivity.LAYOUT, layoutid);
	    	startActivity(intent);
    	}
    }
     
    class HelpListItem {
    	int itemTextRes;
    	int layoutId;
    	public HelpListItem(int textid, int layoutid) {
    		this.itemTextRes = textid;
    		this.layoutId = layoutid;
    	}  	
    }
    
    public class HelpAdapter extends ArrayAdapter<HelpListItem> {
    	Context context;
    	int layoutResId;
    	HelpListItem data[] = null;
    	public HelpAdapter(Context context, int layoutResId, HelpListItem[] data) {
    		super(context, layoutResId, data);
    		this.context = context;
    		this.layoutResId = layoutResId;
    		this.data = data;
    	}
    	
    	@Override
    	public View getView(int position, View convertView, ViewGroup parent) {
    		View row = convertView;
    		HelpItemHolder holder = null;
    		
    		if (row == null) {
    			LayoutInflater inflater = ((Activity) context).getLayoutInflater();
    			row = inflater.inflate(layoutResId, parent, false);
    			holder = new HelpItemHolder();
    			holder.textTitle = (TextView) row.findViewById(R.id.text1);
    			row.setTag(holder);
    		} else {
    			holder = (HelpItemHolder) row.getTag();
    		}
    		HelpListItem listitem = data[position];
    		holder.textTitle.setText(listitem.itemTextRes);
    		holder.layoutId = listitem.layoutId;
    		
    		return row;
    		
    	}
    }
    
    public static class HelpItemHolder {
		TextView textTitle;
		int layoutId;
	}
    
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		menu.add(0, RELEASE_NOTE, 0, R.string.release_note).setIcon(
				R.drawable.ic_menu_abpos_help);
		menu.add(0, DONATION, 0, R.string.ab_donation).setIcon(
				R.drawable.ic_menu_donation);
		menu.add(0, REVIEW_APP, 0, R.string.comment_title).setIcon(
				R.drawable.ic_menu_donation);
		return true;
	}
	
	@Override
    public boolean onPrepareOptionsMenu(Menu menu) {		
		MenuItem item = menu.findItem(DONATION);
    	if (MusicUtils.donated(getApplicationContext())) {
    		item.setVisible(false);
    	} else {
    		item.setVisible(true);
    	}
		return true;
	}
    
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Intent intent;
		switch (item.getItemId()) {
		case RELEASE_NOTE:
			intent = new Intent();
			intent.setClass(this, NewFeaturesActivity.class);
	        startActivity(intent);
			break;
		case DONATION:
			try {
				intent = new Intent(Intent.ACTION_VIEW);
				intent.setData(Uri
						.parse("market://details?id=com.learnerstechlab.abdonation5"));
				startActivity(intent);
			} catch (ActivityNotFoundException e) {
				try {
					Intent intent2 = new Intent(Intent.ACTION_VIEW);
					intent2.setData(Uri
							.parse("http://play.google.com/store/apps/details?id=com.learnerstechlab.abdonation5"));
					startActivity(intent2);
				} catch (ActivityNotFoundException e2) {
					Toast.makeText(this,
							"Apps access Google Play Not installed.",
							Toast.LENGTH_LONG).show();
				}
			}
			break;
		case REVIEW_APP:
			try {
				intent = new Intent(Intent.ACTION_VIEW);
				intent.setData(Uri
						.parse("market://details?id=com.kojimahome.music21"));
				startActivity(intent);
			} catch (ActivityNotFoundException e) {
				try {
					Intent intent2 = new Intent(Intent.ACTION_VIEW);
					intent2.setData(Uri
							.parse("http://play.google.com/store/apps/details?id=com.kojimahome.music21"));
					startActivity(intent2);
				} catch (ActivityNotFoundException e2) {
					Toast.makeText(this,
							"Apps access Google Play Not installed.",
							Toast.LENGTH_LONG).show();
				}
			}
			break;
		}
		return true;
	}
    
}
