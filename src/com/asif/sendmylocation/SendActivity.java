package com.asif.sendmylocation;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CallLog;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;

public class SendActivity extends Activity {

	private Location networkLocation = null;
    private Location gpsLocation = null;
    LocationManager mLocationManager ;
    private static final int TWO_MINUTES = 1000 * 60 * 2;
    private boolean sendButtonShowed = false;
    private final int PICK_CONTACT_FOR_TEXT = 100;
    private String selectedContactName, selectedContactNumber;
    private final String primaryURIToGoogleMap = "Hi! Find me following this direction - http://maps.google.com/maps?&daddr=" ;
    LocationListener gpsLocationListener, networkLocationListener;
    Button sendButton;
    TextView statusTextView;
    TextView contactTextView;
    EditText mobileNumberEditText;
    boolean fetchLocationWithGPS = true;
    LatLng manuallySelectedLocation = null;
    TextView headerTextView;
    final float ACCEPTABLE_ACCURACY = 60;
    
    BroadcastReceiver sendingStatusBroadcastReceiver, deliveryStatusBroadcastReceiver;
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().requestFeature(Window.FEATURE_NO_TITLE);
//		setContentView(R.layout.activity_main);
		setContentView(R.layout.send_activity);
		overridePendingTransition(R.anim.trans_left_in, R.anim.trans_left_out);
		manageActivityData(savedInstanceState);
	}
	
	@Override
	public void onBackPressed() {
	    super.onBackPressed();
	    overridePendingTransition(R.anim.trans_right_in, R.anim.trans_right_out);
	}
	
	@Override
	protected void onResume() 
	{
		headerTextView = (TextView) findViewById(R.id.textView1);
		Typeface type = Typeface.createFromAsset(getAssets(),"fonts/aaargh_normal_font.ttf"); 
		headerTextView.setTypeface(type);
		
		super.onResume();
		
		sendButton = (Button) findViewById(R.id.sendButton);
		statusTextView = (TextView) findViewById(R.id.smsStatusTextView);
		contactTextView = (TextView) findViewById(R.id.contactNameTextView);
		mobileNumberEditText = (EditText) findViewById(R.id.mobile_number);
		mobileNumberEditText.getBackground().setColorFilter(getResources().getColor(R.color.black_description), PorterDuff.Mode.SRC_ATOP);
		mobileNumberEditText.setOnEditorActionListener(new OnEditorActionListener() {
	        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
	            if ((event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) || (actionId == EditorInfo.IME_ACTION_DONE)) {
	            	selectedContactNumber = mobileNumberEditText.getText().toString();
	            	hideNameNumberTextView();
	            }    
	            return false;
	        }
	    });
		
		mobileNumberEditText.addTextChangedListener(new TextValidator(mobileNumberEditText) {
			
			@Override
			public void validate(TextView textView, String text) 
			{
				selectedContactNumber = mobileNumberEditText.getText().toString();
				
				if(textView.getText().toString().length() == 0)
				{
					if(!mobileNumberEditText.isFocused())
					{
						mobileNumberEditText.setText(getResources().getString(R.string.edit_text_enter_number));
						mobileNumberEditText.setGravity(android.view.Gravity.CENTER);
					}
					return ;
				}
				String number = textView.getText().toString();
				if(!number.matches("[0-9]+") && number.length() <= 5)
				{
					textView.setTextColor(getResources().getColor(R.color.red));
					return ;
				}
				textView.setTextColor(getResources().getColor(R.color.black_description));
			}
		});
		
		initBroadcastReceivers();
		
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
	
	private void initBroadcastReceivers() 
	{
		sendingStatusBroadcastReceiver = new BroadcastReceiver()
		{
            @Override
            public void onReceive(Context arg0, Intent arg1) 
            {
                switch (getResultCode())
                {
                    case Activity.RESULT_OK:
//                        Toast.makeText(getBaseContext(), "Direction sent!",  Toast.LENGTH_SHORT).show();
                    	Log.d("MainActivity", "Direction sent!");
                    	statusTextView.setTextColor(getResources().getColor(R.color.black_description));
                        statusTextView.setText("Direction sent!");
                        statusTextView.setVisibility(View.VISIBLE);
                        clearContactSelection();
                        sendButton.setText(getString(R.string.send_button_text));
                        enableSendButton();
                        try{
                        	unregisterReceiver(sendingStatusBroadcastReceiver);
                        } catch(IllegalArgumentException e)
                        {
                        	Log.e("BroadcastReceiver" , "Not registered yet!");
                        }
                        break;
                    case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
//                        Toast.makeText(getBaseContext(), "Generic failure", Toast.LENGTH_SHORT).show();
                    	Log.e("MainActivity", "Generic failure");
                    	statusTextView.setTextColor(getResources().getColor(R.color.red));
                        statusTextView.setText("Sending failed!");
                        statusTextView.setVisibility(View.VISIBLE);
                        sendButton.setText("Try Again!");
                        enableSendButton();
                        try{
                        	unregisterBroadcastReceivers();
                        } catch(IllegalArgumentException e)
                        {
                        	Log.e("BroadcastReceiver" , "Not registered yet!");
                        }
                        break;
                    case SmsManager.RESULT_ERROR_NO_SERVICE:
//                        Toast.makeText(getBaseContext(), "No service", Toast.LENGTH_SHORT).show();
                    	Log.e("MainActivity", "No service");
                        statusTextView.setTextColor(getResources().getColor(R.color.red));
                        statusTextView.setText("Sending failed! - No Service!");
                        statusTextView.setVisibility(View.VISIBLE);
                        sendButton.setText("Try Again!");
                        enableSendButton();
                        try{
                        	unregisterBroadcastReceivers();
                        } catch(IllegalArgumentException e)
                        {
                        	Log.e("BroadcastReceiver" , "Not registered yet!");
                        }
                        break;
                    case SmsManager.RESULT_ERROR_NULL_PDU:
//                        Toast.makeText(getBaseContext(), "Null PDU", Toast.LENGTH_SHORT).show();
                    	Log.e("MainActivity", "Null PDU");
                        statusTextView.setTextColor(getResources().getColor(R.color.red));
                        statusTextView.setText("Sending failed!");
                        statusTextView.setVisibility(View.VISIBLE);
                        sendButton.setText("Try Again! - No PDU provided!");
                        enableSendButton();
                        try{
                        	unregisterBroadcastReceivers();
                        } catch(IllegalArgumentException e)
                        {
                        	Log.e("BroadcastReceiver" , "Not registered yet!");
                        }
                        break;
                    case SmsManager.RESULT_ERROR_RADIO_OFF:
//                        Toast.makeText(getBaseContext(), "Radio off", Toast.LENGTH_SHORT).show();
                    	Log.e("MainActivity", "Radio off");
                        statusTextView.setTextColor(getResources().getColor(R.color.red));
                        statusTextView.setText("Sending failed! - Radio is off!");
                        statusTextView.setVisibility(View.VISIBLE);
                        sendButton.setText("Try Again!");
                        enableSendButton();
                        try{
                        	unregisterBroadcastReceivers();
                        } catch(IllegalArgumentException e)
                        {
                        	Log.e("BroadcastReceiver" , "Not registered yet!");
                        }
                        break;
                }
            }

			
        };
		deliveryStatusBroadcastReceiver = new BroadcastReceiver(){
            @Override
            public void onReceive(Context arg0, Intent arg1) {
                switch (getResultCode())
                {
                    case Activity.RESULT_OK:
//                        Toast.makeText(getBaseContext(), "Direction delivered", Toast.LENGTH_SHORT).show();
                    	Log.d("MainActivity", "Direction delivered!");
                    	statusTextView.setTextColor(getResources().getColor(R.color.black_description));
                        statusTextView.setText("Direction delivered!");
                        statusTextView.setVisibility(View.VISIBLE);
                        try{
                        	unregisterReceiver(deliveryStatusBroadcastReceiver);
                        } catch(IllegalArgumentException e)
                        {
                        	Log.e("BroadcastReceiver" , "Not registered yet!");
                        }
                        break;
                    case Activity.RESULT_CANCELED:
                        Toast.makeText(getBaseContext(), "Direction not delivered", Toast.LENGTH_SHORT).show();
                        Log.e("MainActivity", "Direction not delivered!");
                    	statusTextView.setTextColor(getResources().getColor(R.color.red));
                        statusTextView.setText("Direction not delivered!");
                        statusTextView.setVisibility(View.VISIBLE);
                        try{
                        	unregisterReceiver(deliveryStatusBroadcastReceiver);
                        } catch(IllegalArgumentException e)
                        {
                        	Log.e("BroadcastReceiver" , "Not registered yet!");
                        }
                        break;                        
                }
            }
        };
	}

	@Override
	protected void onPause() {
		super.onPause();
		
		sendButton.setText("Send");
		enableSendButton();
		
		removeLocationUpdates();
		try{
        	unregisterBroadcastReceivers();
        } catch(IllegalArgumentException e)
        {
        	Log.e("BroadcastReceiver" , "Not registered yet!");
        }
	}
	
	private void unregisterBroadcastReceivers() {
		
		if(sendingStatusBroadcastReceiver!=null)
		{
			unregisterReceiver(sendingStatusBroadcastReceiver);
		}
		if(deliveryStatusBroadcastReceiver!=null)
		{
			unregisterReceiver(deliveryStatusBroadcastReceiver);
		}
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
					selectedContactName = "";
					selectedContactNumber = "";
					hideNameNumberTextView();
					mobileNumberEditText.setText(getResources().getString(R.string.edit_text_enter_number));
					mobileNumberEditText.setGravity(android.view.Gravity.CENTER);
				}
				else if (size == 2) 
				{
					selectedContactName = phoneList.get(0);
					selectedContactNumber = phoneList.get(1).replaceAll("\\D", "");
//					Toast.makeText(getApplicationContext(), selectedContactName + " - " + selectedContactNumber, Toast.LENGTH_SHORT).show();
					Log.d("MainActivity", selectedContactName + " - " + selectedContactNumber);
					changeNameNumberTextViewText(selectedContactName,selectedContactNumber);
					mobileNumberEditText.setText(selectedContactNumber);
					mobileNumberEditText.setGravity(android.view.Gravity.CENTER);
				} 
				else 
				{
					selectedContactName = phoneList.get(0);
					
					phoneList.remove(0);
					
					AlertDialog.Builder alertDialog = new AlertDialog.Builder(SendActivity.this);
					alertDialog.setTitle("Select A Number");
					final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(
		                    SendActivity.this,
		                    android.R.layout.select_dialog_singlechoice);
					for(String number : phoneList)
					{
						arrayAdapter.add(number);
					}
					alertDialog.setAdapter(arrayAdapter,
		                    new DialogInterface.OnClickListener() {

		                        @Override
		                        public void onClick(DialogInterface dialog, int which) {
		                        	selectedContactNumber = arrayAdapter.getItem(which).replaceAll("\\D", "");
		                        	changeNameNumberTextViewText(selectedContactName,selectedContactNumber);
		        					mobileNumberEditText.setText(selectedContactNumber);
		    						mobileNumberEditText.setGravity(android.view.Gravity.CENTER);
		                        }
	                    });
					alertDialog.setOnKeyListener(new Dialog.OnKeyListener() {

			            @Override
			            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) 
			            {
			                if (keyCode == KeyEvent.KEYCODE_BACK) 
			                {
			                	Toast.makeText(getApplicationContext(), "No Number Selected!", Toast.LENGTH_LONG).show();
			                	selectedContactName = "";
			                	selectedContactNumber = "";
			                	hideNameNumberTextView();
								mobileNumberEditText.setText(getResources().getString(R.string.edit_text_enter_number));
								mobileNumberEditText.setGravity(android.view.Gravity.CENTER);
			                    dialog.dismiss();
			                }
			                return true;
			            }
			        });
					alertDialog.show();
				}
			} 
			break;
		default:
			break;
		}
	}
	
	private void changeNameNumberTextViewText(String contactName, String contactNumber) 
	{
		
		if(contactNumber.length() == 0)
		{
			contactTextView.setVisibility(ViewGroup.GONE);
		}
		else
		{
			if(contactName.length() > 0)
			{
				contactTextView.setText(contactName + " - " + contactNumber);
			}
			else
			{
				contactTextView.setText(contactNumber);
			}
			contactTextView.setVisibility(ViewGroup.VISIBLE);
		}
		
	}
	
	private void hideNameNumberTextView()
	{
		contactTextView.setVisibility(ViewGroup.GONE);
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
	
	private void removeLocationUpdates() 
	{
		if(mLocationManager != null)
		{
			if(gpsLocationListener != null)
			{
				mLocationManager.removeUpdates(gpsLocationListener);
			}
			if(networkLocationListener != null)
			{
				mLocationManager.removeUpdates(networkLocationListener);
			}
		}
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

	private void changeLocationWaitingProgressBarStatus() {
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
	
	// gps and location code
	private void displayLocationAccessDialog(){
        AlertDialog.Builder dialog = new AlertDialog.Builder(SendActivity.this);
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
		statusTextView.setText("");
		statusTextView.setVisibility(View.GONE);
		
		if(selectedContactNumber == null || selectedContactNumber.length()==0)
		{
			Toast.makeText(getApplicationContext(), "Please select a friend!", Toast.LENGTH_LONG).show();
			return ;
		}
		
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
		
		if(selectedContactNumber.length() <= 5)
		{
			Toast.makeText(getApplicationContext(), "Please enter a valid number!", Toast.LENGTH_LONG).show();
			return ;
		}
		
		sendSMS(selectedContactNumber,smsContent);
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

	// onClick of the choose friend button
	public void chooseFriend(View v)
	{
		openContactsURI();
	}
	
	public void numberEditTextOnClick(View v)
	{
		if( ((EditText)v).getText().toString().equals(getResources().getString(R.string.edit_text_enter_number))){
			((EditText)v).setText("");
			((EditText)v).setGravity(android.view.Gravity.LEFT);
		}
	}
	
	void openContactsURI()
	{
		Intent intentContact = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
		startActivityForResult(intentContact, PICK_CONTACT_FOR_TEXT);
	}
	
	void openCallLogURI()
	{
		Intent showCallLog = new Intent();
		showCallLog.setAction(Intent.ACTION_VIEW);
		showCallLog.setType(CallLog.Calls.CONTENT_TYPE);
		startActivityForResult(showCallLog, PICK_CONTACT_FOR_TEXT); 
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
 
        // unregister previous broadcast receivers
        try{
        	unregisterBroadcastReceivers();
        } catch(IllegalArgumentException e)
        {
        	Log.e("BroadcastReceiver" , "Not registered yet!");
        }
        
        //---when the SMS has been sent---
        registerReceiver(sendingStatusBroadcastReceiver, new IntentFilter(SENT));
        
        sendButton.setText("Sending...");
		disableSendButton();
 
        //---when the SMS has been delivered---
        registerReceiver(deliveryStatusBroadcastReceiver, new IntentFilter(DELIVERED));        
 
        SmsManager sms = SmsManager.getDefault();
        sms.sendTextMessage(phoneNumber, null, message, sentPI, deliveredPI);        
    }
    
	protected void clearContactSelection() {
		contactTextView.setText("");
		contactTextView.setVisibility(View.GONE);
	}

	protected void enableSendButton() {
		sendButton.setEnabled(true);
	}

	protected void disableSendButton() {
		sendButton.setEnabled(false);
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
            Toast.makeText(SendActivity.this, "Help Selected", Toast.LENGTH_SHORT).show();
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
            Toast.makeText(SendActivity.this, "Request feature selected", Toast.LENGTH_SHORT).show();
            
            Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts("mailto","asif.bohemian@gmail.com", null));
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Requesting New Feature in Send My Location App");
            startActivity(Intent.createChooser(emailIntent, "Send email..."));
            
            return true;
        
        case R.id.menuContactUsBug:
            Toast.makeText(SendActivity.this, "Report Bug Selected", Toast.LENGTH_SHORT).show();
            
            Intent emailBugIntent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts("mailto","asif.bohemian@gmail.com", null));
            emailBugIntent.putExtra(Intent.EXTRA_SUBJECT, "Reporting bugs in Send My Location App");
            startActivity(Intent.createChooser(emailBugIntent, "Send email..."));
            
            return true;
            
        case R.id.menuRateUs:
            Toast.makeText(SendActivity.this, "Rate this app Selected", Toast.LENGTH_SHORT).show();
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
