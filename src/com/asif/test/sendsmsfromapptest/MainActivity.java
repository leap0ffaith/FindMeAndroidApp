package com.asif.test.sendsmsfromapptest;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Contacts;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.support.v7.app.ActionBarActivity;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends ActionBarActivity {

	private Location networkLocation = null;
    private Location gpsLocation = null;
    LocationManager mLocationManager ;
    private static final int TWO_MINUTES = 1000 * 60 * 2;
    private boolean sendButtonShowed = false;
    private final int PICK_CONTACT_FOR_TEXT = 100;
    private String selectedContactName, selectedContactNumber;
    private final String primaryURIToGoogleMap = "Hi! Find me following this direction - http://maps.google.com/maps?&daddr=" ;
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
//		RelativeLayout rl = (RelativeLayout) findViewById(R.id.relativeLayout1);
//		RelativeLayout.LayoutParams rlParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
//		rlParams.addRule(RelativeLayout.BELOW , findViewById(R.id.textView1).getId());
		
		
//		Button btn = new Button(getApplicationContext());
//		btn.setText("asdkjfh");
//		rl.addView(btn, rlParams);
		
//		ProgressWheel wheel = new ProgressWheel(getApplicationContext());
//		wheel.setBarColor(Color.BLUE);
//		rl.addView(wheel, rlParams);
//		wheel.spin();
		
		mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
		if(!mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER))
		{
			displayLocationAccessDialog();
		}
		
		
		
		startLocationListeners();
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) 
	{
		super.onActivityResult(requestCode, resultCode, data);
		
		switch (requestCode) 
		{
		case PICK_CONTACT_FOR_TEXT:
			if (resultCode == RESULT_CANCELED || data==null) 
			{
				Toast.makeText(this, "No contacts selected.", Toast.LENGTH_SHORT).show();
			}
			else if (resultCode == RESULT_OK) 
			{
				List<String> phoneList = new LinkedList<String>();
				phoneList = getNameAndPhoneList( this, data.getData());
				int size = phoneList.size();
				if (size < 2)
				{
					Toast.makeText(this, "No Number Found!", Toast.LENGTH_LONG).show();
				}
				else if (size == 2) 
				{
					selectedContactName = phoneList.get(0);
					selectedContactNumber = phoneList.get(1).replaceAll("\\D", "");
//					Toast.makeText(getApplicationContext(), selectedContactName + " - " + selectedContactNumber, Toast.LENGTH_SHORT).show();
					changeNameNumberTextViewText(selectedContactName,selectedContactNumber);
				} 
				else 
				{
					selectedContactName = phoneList.get(0);
					selectedContactNumber = phoneList.get(1).replaceAll("\\D", "");
//					Toast.makeText(getApplicationContext(),"mulitple available, selected: "+  selectedContactName + " - " + selectedContactNumber, Toast.LENGTH_SHORT).show();
					// TODO multiple numbers saved under this contact - maybe show another dialog to choose 1
				}
			} 
			break;
		default:
			break;
		}

		if (data == null)
			return;
		else {

			if (data.hasExtra("Return Action")) {
				if (data.getStringExtra("Return Action").equals(
						"PickFromMultipleContactsForText")) {
					String numberPicked = data.getStringExtra("NumberPicked");
					Toast.makeText(getApplicationContext(), "else", Toast.LENGTH_SHORT).show();
//					Intent intent = new Intent(this, SendMessageActivity.class);
//					intent.setFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
//					intent.putExtra("number",
//							numberPicked.replaceAll("\\D", ""));
//					intent.putExtra("compose", message_to_copy);
//					startActivity(intent);
				}
			}
		}
	}
	
	private void changeNameNumberTextViewText(String contactName, String contactNumber) 
	{
		TextView tv = (TextView) findViewById(R.id.contactNameTextView);
		if(contactNumber.length() == 0)
		{
			tv.setVisibility(ViewGroup.GONE);
		}
		else
		{
			if(contactName.length() > 0)
			{
				tv.setText(contactName + " - " + contactNumber);
			}
			else
			{
				tv.setText(contactNumber);
			}
			tv.setVisibility(ViewGroup.VISIBLE);
		}
		
	}

	public List<String> getNameAndPhoneList(Context context, Uri data) 
	{
		ArrayList<String> list = new ArrayList<String>();

		Cursor cursor = context.getContentResolver().query(data, null, null, null, null);
		if (cursor.moveToFirst()) 
		{
			if (cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)).equals("1")) 
			{
				String contactId = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID));
				String name = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
				list.add(name);
				Cursor phones = context.getContentResolver()
						.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = " + contactId, null, null);
				int numberIndex = phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
				while (phones.moveToNext())
					list.add(phones.getString(numberIndex));

				phones.close();
			}
		}
		cursor.close();

		return list;
	}	
	
	private void startLocationListeners() 
	{
		mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, new LocationListener() {
			
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
		});
		
		mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, new LocationListener() {
			
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
		});
	}

	private void checkIfAcceptableLocationAchieved()
	{
		boolean isGpsLocationAcceptable = false;
		boolean isNetworkLocationAcceptable = false;
	
		isGpsLocationAcceptable = (gpsLocation != null && gpsLocation.getAccuracy() <= 30 && (System.currentTimeMillis()-gpsLocation.getTime())<5000 );
		isNetworkLocationAcceptable = (networkLocation != null && networkLocation.getAccuracy() <= 30 && (System.currentTimeMillis()-networkLocation.getTime())<5000 );
		
		if(isGpsLocationAcceptable || isNetworkLocationAcceptable)
		{
			changeLocationWaitingProgressBarStatus();
		}
	}

	private void changeLocationWaitingProgressBarStatus() {
		Button sendButton = (Button) findViewById(R.id.sendButton);
		LinearLayout waitingLinearLayout = (LinearLayout) findViewById(R.id.waitingLinearLayout);
		waitingLinearLayout.setVisibility(ViewGroup.GONE);
		sendButton.setVisibility(ViewGroup.VISIBLE);
		sendButtonShowed = true;
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
	
	private boolean isProviderAllowed(String s){
        boolean flag = false;
        for(String provider : mLocationManager.getAllProviders()){
            if(provider.contains(s)){
                flag = true;
                break;
            }
        }

        return flag;
    }
	
	/*private void startLocationListeners(){
        // Here we check for "network", "gps" in providers and start them if they are available
        // Note that "network" is not available in the emulator
        startLocationListener(networkLocationListener, LocationManager.NETWORK_PROVIDER);
        startLocationListener(gpsLocationListener, LocationManager.GPS_PROVIDER);
        mLocationManager.addGpsStatusListener(gpsStatusListener);

        mRunning = true;

    }*/
	
	// gps and location code
	private void displayLocationAccessDialog(){
        AlertDialog.Builder dialog = new AlertDialog.Builder(MainActivity.this);
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
	
	// onClick of the Send Button
	public void sendDirectionViaSMS(View v)
	{
//		Toast.makeText(getApplicationContext(), "onClick", Toast.LENGTH_LONG).show();
//		Button sendButton = (Button) findViewById(R.id.sendButton);
//		sendButton.setVisibility(ViewGroup.GONE);
//		LinearLayout waitingLinearLayout = (LinearLayout) findViewById(R.id.waitingLinearLayout);
//		waitingLinearLayout.setVisibility(ViewGroup.VISIBLE);
		if(selectedContactNumber.length()==0)
		{
			Toast.makeText(getApplicationContext(), "Please select a friend!", Toast.LENGTH_LONG).show();
			return ;
		}
		Location bestLocation = selectBestLocationForSending();
		String smsContent = primaryURIToGoogleMap + bestLocation.getLatitude() + "," + bestLocation.getLongitude();
		
		sendSMS(selectedContactNumber,smsContent);
		changeSendButtonText("Sending...");
		
//		sendSMS("01847003239","hi asif");
	}
	
	private Location selectBestLocationForSending() {
		if(gpsLocation != null && System.currentTimeMillis() - gpsLocation.getTime() <= 5000 && gpsLocation.getAccuracy() <= 30)
			return gpsLocation;
		if(networkLocation != null && System.currentTimeMillis() - networkLocation.getTime() <= 5000 && networkLocation.getAccuracy() <= 30)
			return networkLocation;
		if(isBetterLocation(gpsLocation, networkLocation))
			return gpsLocation;
		else
			return networkLocation;
	}

	// onClick of the choose friend button
	public void chooseFriend(View v)
	{
		Intent intentContact = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
		startActivityForResult(intentContact, PICK_CONTACT_FOR_TEXT);
//		ContactListDialogFragment dialog = new ContactListDialogFragment();
//		dialog.show(getSupportFragmentManager(),"ContactListDialogFragment");
	}
	
	private void changeSendButtonText(String text) {
		Button sendButton = (Button) findViewById(R.id.sendButton);
		sendButton.setText(text);
	}
	
	 //---sends an SMS message to another device---
    private void sendSMS(String phoneNumber, String message)
    {        
        String SENT = "SMS_SENT";
        String DELIVERED = "SMS_DELIVERED";
 
        PendingIntent sentPI = PendingIntent.getBroadcast(this, 0,
            new Intent(SENT), 0);
 
        PendingIntent deliveredPI = PendingIntent.getBroadcast(this, 0,
            new Intent(DELIVERED), 0);
 
        //---when the SMS has been sent---
        registerReceiver(new BroadcastReceiver(){
            @Override
            public void onReceive(Context arg0, Intent arg1) {
                switch (getResultCode())
                {
                    case Activity.RESULT_OK:
                        Toast.makeText(getBaseContext(), "Direction sent!",  Toast.LENGTH_SHORT).show();
                        changeSendButtonText(getString(R.string.send_button_text));
                        break;
                    case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                        Toast.makeText(getBaseContext(), "Generic failure", 
                                Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_NO_SERVICE:
                        Toast.makeText(getBaseContext(), "No service", 
                                Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_NULL_PDU:
                        Toast.makeText(getBaseContext(), "Null PDU", 
                                Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_RADIO_OFF:
                        Toast.makeText(getBaseContext(), "Radio off", 
                                Toast.LENGTH_SHORT).show();
                        break;
                }
            }

			
        }, new IntentFilter(SENT));
 
        //---when the SMS has been delivered---
        registerReceiver(new BroadcastReceiver(){
            @Override
            public void onReceive(Context arg0, Intent arg1) {
                switch (getResultCode())
                {
                    case Activity.RESULT_OK:
                        Toast.makeText(getBaseContext(), "Direction delivered", 
                                Toast.LENGTH_SHORT).show();
                        break;
                    case Activity.RESULT_CANCELED:
                        Toast.makeText(getBaseContext(), "Direction not delivered", 
                                Toast.LENGTH_SHORT).show();
                        break;                        
                }
            }
        }, new IntentFilter(DELIVERED));        
 
        SmsManager sms = SmsManager.getDefault();
        sms.sendTextMessage(phoneNumber, null, message, sentPI, deliveredPI);        
    }
    
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
