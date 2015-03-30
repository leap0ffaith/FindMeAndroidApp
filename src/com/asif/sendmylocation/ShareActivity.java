package com.asif.sendmylocation;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.asif.sendmylocation.R;
import com.google.android.gms.maps.model.LatLng;

public class ShareActivity extends Activity {
	
	TextView headerTextView;
	boolean fetchLocationWithGPS = true;
    LatLng manuallySelectedLocation = null;
    private Location networkLocation = null;
    private Location gpsLocation = null;
    LocationManager mLocationManager ;
    LocationListener gpsLocationListener, networkLocationListener;
    private boolean sendButtonShowed = false;
    final float ACCEPTABLE_ACCURACY = 60;
    private static final int TWO_MINUTES = 1000 * 60 * 2;
    LinearLayout buttonsContainerLinearLayout;
    private final String primaryURIToGoogleMap = "Hi! Find me following this direction - http://maps.google.com/maps?&daddr=" ;
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		getWindow().requestFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.share_activity);
		overridePendingTransition(R.anim.trans_left_in, R.anim.trans_left_out);
		manageActivityData(savedInstanceState);
	}
	
	private void manageActivityData(Bundle savedInstanceState)
	{
		String newString;
		if (savedInstanceState == null) 
		{
			Log.d("MainActivity","savedInstanceState null");
		    Bundle extras = getIntent().getExtras();
		    if(extras == null) 
		    {
		    	Toast.makeText(getBaseContext(), "Extras null",  Toast.LENGTH_SHORT).show();
		        newString= null;
		        fetchLocationWithGPS = true;
		        manuallySelectedLocation = null;
		    } 
		    else 
		    {
		        newString= extras.getString("LOCATION_RETRIEVAL_MODE");
		        if(newString == null || newString.length() == 0)
		        {
		        	Log.d("MainActivity","LOCATION_RETRIEVAL_MODE: null");
//		        	Toast.makeText(getBaseContext(), "LOCATION_RETRIEVAL_MODE: null",  Toast.LENGTH_SHORT).show();
		        	newString= null;
			        fetchLocationWithGPS = true;
			        manuallySelectedLocation = null;
		        }
		        else if(newString.length() > 0)
		        {
		        	Log.d("MainActivity","LOCATION_RETRIEVAL_MODE: " + newString);
//		        	Toast.makeText(getBaseContext(), "LOCATION_RETRIEVAL_MODE: " + newString,  Toast.LENGTH_SHORT).show();
		        	if(newString.equals(getResources().getString(R.string.select_from_map)))
		        	{
		        		String longString = extras.getString("LONGITUDE");
		        		String latString = extras.getString("LATITUDE");
		        		if(longString.length() == 0 || latString.length() == 0)
		        		{
		        			Log.d("MainActivity","data did not come correctly");
//		        			Toast.makeText(getBaseContext(), "data did not come correctly",  Toast.LENGTH_SHORT).show();
		        			fetchLocationWithGPS = true;
		        		}
		        		else
		        		{
		        			double longitude,latitude;
		        			
		        			fetchLocationWithGPS = false;
		        			latitude = Double.parseDouble(latString);
		        			longitude = Double.parseDouble(longString);
		        			manuallySelectedLocation = new LatLng(latitude,longitude);
		        			Log.d("MainActivity","latitude: "+latitude + " longitude: "+longitude);
//		        			Toast.makeText(getBaseContext(), "latitude: "+latitude + " longitude: "+longitude,  Toast.LENGTH_SHORT).show();
		        		}
		        	}
		        	else
		        	{
		        		fetchLocationWithGPS = true;
	        			manuallySelectedLocation = null;
		        	}
		        }
		    }
		} 
		/*else 
		{
		    newString= (String) savedInstanceState.getSerializable("LOCATION_RETRIEVAL_MODE");
		    if(newString.length() > 0)
	        {
	        	if(newString.equals(getResources().getString(R.string.select_from_map)))
	        	{
	        		String longString = (String) savedInstanceState.getSerializable("LONGITUDE");
	        		String latString = (String) savedInstanceState.getSerializable("LATITUDE");
	        		if(longString.length() == 0 || latString.length() == 0)
	        		{
	        			// TODO data did not come correctly
	        			fetchLocationWithGPS = true;
	        		}
	        		else
	        		{
	        			double longitude,latitude;
		        		
	        			fetchLocationWithGPS = false;
	        			latitude = Double.parseDouble(latString);
	        			longitude = Double.parseDouble(longString);
	        			manuallySelectedLocation = new LatLng(latitude,longitude);
	        		}
	        	}
	        	else
	        	{
	        		fetchLocationWithGPS = true;
        			manuallySelectedLocation = null;
	        	}
	        }
	        else
	        {
	        	newString= null;
		        fetchLocationWithGPS = true;
		        manuallySelectedLocation = null;
	        }
		}*/
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
		
		buttonsContainerLinearLayout = (LinearLayout) findViewById(R.id.buttons_container);
		if(fetchLocationWithGPS)
		{
			mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
			if(!mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER))
			{
				displayLocationAccessDialog();
			}
			startLocationListeners();
		}
		else
		{
			changeLocationWaitingProgressBarStatus();
		}
	}
	
	// gps and location code
		private void displayLocationAccessDialog(){
	        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
	        dialog.setMessage(R.string.gps_network_not_enabled);
	        dialog.setPositiveButton(R.string.enable, new DialogInterface.OnClickListener() {
	            @Override
	            public void onClick(DialogInterface paramDialogInterface, int paramInt) {
	                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
	                startActivity(intent);
	            }
	        });
	        dialog.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {

	            @Override
	            public void onClick(DialogInterface paramDialogInterface, int paramInt) {
	            }
	        });
	        dialog.show();
	    }
		
		private void startLocationListeners() 
		{
			gpsLocationListener = new LocationListener() {
				
				@Override
				public void onStatusChanged(String provider, int status, Bundle extras) {
					
				}
				
				@Override
				public void onProviderEnabled(String provider) {
					
				}
				
				@Override
				public void onProviderDisabled(String provider) {
					
				}
				
				@Override
				public void onLocationChanged(Location location) {
					Log.d("GPS_DATA", "Lat/Lon: "+ location.getLatitude()+" , " + location.getLongitude() + ". Accuracy: "+ location.getAccuracy() + ". Taken " + (System.currentTimeMillis() - location.getTime()) + " milliSec ago.");
					if(isBetterLocation(location, gpsLocation))
					{
						gpsLocation = location;
						if(!sendButtonShowed)
						{
							checkIfAcceptableLocationAchieved();
						}
					}
				}
			};
			
			networkLocationListener = new LocationListener() {
				
				@Override
				public void onStatusChanged(String provider, int status, Bundle extras) {
					
				}
				
				@Override
				public void onProviderEnabled(String provider) {
					
				}
				
				@Override
				public void onProviderDisabled(String provider) {
					
				}
				
				@Override
				public void onLocationChanged(Location location) {
					Log.d("NETWORK_DATA", "Lat/Lon: "+ location.getLatitude()+" , " + location.getLongitude() + ". Accuracy: "+ location.getAccuracy() + ". Taken " + (System.currentTimeMillis() - location.getTime()) + " milliSec ago.");
					if(isBetterLocation(location, gpsLocation))
					{
						networkLocation = location;
						if(!sendButtonShowed)
						{
							checkIfAcceptableLocationAchieved();
						}
					}
				}
			};
			
			mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, gpsLocationListener);
			mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, networkLocationListener);
		}
		
		private void checkIfAcceptableLocationAchieved()
		{
			boolean isGpsLocationAcceptable = false;
			boolean isNetworkLocationAcceptable = false;
		
			isGpsLocationAcceptable = (gpsLocation != null && gpsLocation.getAccuracy() <= ACCEPTABLE_ACCURACY && (System.currentTimeMillis()-gpsLocation.getTime())<5000 );
			isNetworkLocationAcceptable = (networkLocation != null && networkLocation.getAccuracy() <= ACCEPTABLE_ACCURACY && (System.currentTimeMillis()-networkLocation.getTime())<5000 );
			
			if(isGpsLocationAcceptable || isNetworkLocationAcceptable)
			{
				changeLocationWaitingProgressBarStatus();
			}
		}
		
		/** Determines whether one Location reading is better than the current Location fix
		  * @param location  The new Location that you want to evaluate
		  * @param currentBestLocation  The current Location fix, to which you want to compare the new one
		  */
		protected boolean isBetterLocation(Location location, Location currentBestLocation) {
		    if (currentBestLocation == null) {
		        // A new location is always better than no location
		        return true;
		    }
		    if(location == null)
		    {
		    	return false;
		    }
		    
		    // Check whether the new location fix is newer or older
		    long timeDelta = location.getTime() - currentBestLocation.getTime();
		    boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;
		    boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;
		    boolean isNewer = timeDelta > 0;

		    // If it's been more than two minutes since the current location, use the new location
		    // because the user has likely moved
		    if (isSignificantlyNewer) {
		        return true;
		    // If the new location is more than two minutes older, it must be worse
		    } else if (isSignificantlyOlder) {
		        return false;
		    }

		    // Check whether the new location fix is more or less accurate
		    int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
		    boolean isLessAccurate = accuracyDelta > 0;
		    boolean isMoreAccurate = accuracyDelta < 0;
		    boolean isSignificantlyLessAccurate = accuracyDelta > 200;

		    // Check if the old and new location are from the same provider
		    boolean isFromSameProvider = isSameProvider(location.getProvider(),
		            currentBestLocation.getProvider());

		    // Determine location quality using a combination of timeliness and accuracy
		    if (isMoreAccurate) {
		        return true;
		    } else if (isNewer && !isLessAccurate) {
		        return true;
		    } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
		        return true;
		    }
		    return false;
		}
		
		/** Checks whether two providers are the same */
		private boolean isSameProvider(String provider1, String provider2) {
		    if (provider1 == null) {
		      return provider2 == null;
		    }
		    return provider1.equals(provider2);
		}
	
		private Location selectBestLocationForSending() {
			if(gpsLocation != null && System.currentTimeMillis() - gpsLocation.getTime() <= 5000 && gpsLocation.getAccuracy() <= ACCEPTABLE_ACCURACY)
				return gpsLocation;
			if(networkLocation != null && System.currentTimeMillis() - networkLocation.getTime() <= 5000 && networkLocation.getAccuracy() <= ACCEPTABLE_ACCURACY)
				return networkLocation;
			if(isBetterLocation(gpsLocation, networkLocation))
				return gpsLocation;
			else
				return networkLocation;
		}
		
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.send_button:
			String smsContent = primaryURIToGoogleMap;
			if(fetchLocationWithGPS)
			{
				Location bestLocation = selectBestLocationForSending();
				smsContent = smsContent + bestLocation.getLatitude() + "," + bestLocation.getLongitude();
			}
			else
			{
				smsContent = smsContent + manuallySelectedLocation.latitude + "," + manuallySelectedLocation.longitude;
			}
			Intent i = new Intent(this, SendSMSActivity.class);
			i.putExtra("SMS_BODY", "smsContent");
			startActivity(i);
			break;
		case R.id.sms_button:
			String smsContent1 = primaryURIToGoogleMap; 
			if(fetchLocationWithGPS)
			{
				Location bestLocation = selectBestLocationForSending();
				smsContent1 = smsContent1 + bestLocation.getLatitude() + "," + bestLocation.getLongitude();
			}
			else
			{
				smsContent1 = smsContent1 + manuallySelectedLocation.latitude + "," + manuallySelectedLocation.longitude;
			}
			Intent smsIntent = new Intent(Intent.ACTION_VIEW);
			smsIntent.setType("vnd.android-dir/mms-sms");
			smsIntent.putExtra("sms_body", smsContent1); 
			startActivity(smsIntent);
			break;
		case R.id.share_button:
			String latLong = "";
			if(fetchLocationWithGPS)
			{
				Location bestLocation = selectBestLocationForSending();
				latLong = bestLocation.getLatitude() + "," + bestLocation.getLongitude();
			}
			else
			{
				latLong = manuallySelectedLocation.latitude + "," + manuallySelectedLocation.longitude;
			}
			Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND); 
		    sharingIntent.setType("text/plain");
		    String shareBody = "http://maps.google.com/maps/place/" + latLong;
		    sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "I'm here!");
		    sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareBody);
		    startActivity(Intent.createChooser(sharingIntent, "Share via"));
			break;
		default:
			break;
		}
	}
	
	private void changeLocationWaitingProgressBarStatus() {
		LinearLayout waitingLinearLayout = (LinearLayout) findViewById(R.id.waitingLinearLayout);
		waitingLinearLayout.setVisibility(ViewGroup.GONE);
		buttonsContainerLinearLayout.setVisibility(ViewGroup.VISIBLE);
		sendButtonShowed = true;
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
