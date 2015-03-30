package com.asif.sendmylocation;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;

import com.asif.sendmylocation.R;

public class LocationActivity extends Activity {
	
	TextView headerTextView;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		getWindow().requestFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.location_activity);
		overridePendingTransition(R.anim.trans_left_in, R.anim.trans_left_out);
	}
	
	@Override
	public void onBackPressed() {
	    super.onBackPressed();
	    overridePendingTransition(R.anim.trans_right_in, R.anim.trans_right_out);
	}
	
	@Override
	protected void onResume() {
		headerTextView = (TextView) findViewById(R.id.location_text_view);
		Typeface type = Typeface.createFromAsset(getAssets(),"fonts/aaargh_normal_font.ttf"); 
		headerTextView.setTypeface(type);
		super.onResume();
	}
	
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.gps_button:
			Intent i = new Intent(this, ShareActivity.class);     
			i.putExtra("LOCATION_RETRIEVAL_MODE", getResources().getString(R.string.use_gps));
			startActivity(i);
			break;
		case R.id.select_from_map_button:
			Intent intent = new Intent(this, MapActivity.class);      
			startActivity(intent);
			break;
		default:
			break;
		}
	}
	
	private void launchMarket() {
//		Uri uri = Uri.parse("market://details?id=com.revesoft.itelmobiledialer.dialer");
	    Uri uri = Uri.parse("market://details?id=" + getPackageName());
	    Intent myAppLinkToMarket = new Intent(Intent.ACTION_VIEW, uri);
	    try {
	        startActivity(myAppLinkToMarket);
	    } catch (ActivityNotFoundException e) {
	        Toast.makeText(this, "Oops! Unable to find market app!", Toast.LENGTH_LONG).show();
	    }
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		switch(item.getItemId()){
        case R.id.menuHelp:
            new AlertDialog.Builder(this)
            .setTitle("What is this app!")
            .setMessage(getResources().getString(R.string.help_text))
            .setPositiveButton("Got it!", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) { 
                    // continue with delete
                }
             })
           /* .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) { 
                    // do nothing
                }
             })*/
            .setIcon(android.R.drawable.ic_dialog_info)
             .show();
            return true;

        case R.id.menuContactUs:
            Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts("mailto","asif.bohemian@gmail.com", null));
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Requesting New Feature in Send My Location App");
            startActivity(Intent.createChooser(emailIntent, "Send email..."));
            
            return true;
        
        case R.id.menuContactUsBug:
            Intent emailBugIntent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts("mailto","asif.bohemian@gmail.com", null));
            emailBugIntent.putExtra(Intent.EXTRA_SUBJECT, "Reporting bugs in Send My Location App");
            startActivity(Intent.createChooser(emailBugIntent, "Send email..."));
            
            return true;
            
        case R.id.menuRateUs:
            launchMarket();
            return true;
            
        case R.id.menuShare:
        	Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND); 
		    sharingIntent.setType("text/plain");
		    String shareBody = "https://play.google.com/store/apps/details?id=" + getPackageName();
		    sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareBody);
		    startActivity(Intent.createChooser(sharingIntent, "Share via"));
            return true;
            
        default:
            return super.onOptionsItemSelected(item);
        }
	}
}
