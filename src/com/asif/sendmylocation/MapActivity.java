package com.asif.sendmylocation;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.GoogleMap.OnMapLongClickListener;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class MapActivity extends FragmentActivity {

	private static GoogleMap mMap;
    private static TextView mTapTextView;
    private static FragmentManager fragmentManager;
    private static Marker marker = null;
    private static LatLng selectedPoint = null;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.map_activity);
        
        mTapTextView = (TextView) findViewById(R.id.tap_text);
        fragmentManager = getSupportFragmentManager();
        
        Toast.makeText(getApplicationContext(), "Tap to select your location!", Toast.LENGTH_LONG).show();
        
        setUpMapIfNeeded();
		overridePendingTransition(R.anim.trans_left_in, R.anim.trans_left_out);
	}
	
	@Override
	public void onBackPressed() {
	    super.onBackPressed();
	    overridePendingTransition(R.anim.trans_right_in, R.anim.trans_right_out);
	}
	
	public void goToNextActivity(View v)
	{
		if(selectedPoint == null)
		{
			Toast.makeText(getApplicationContext(), "Tap to select your location!", Toast.LENGTH_LONG).show();		
		}
		else
		{
			Intent i = new Intent(this, ShareActivity.class);
//			Intent i = new Intent(this, SendActivity.class);
			i.putExtra("LOCATION_RETRIEVAL_MODE", getResources().getString(R.string.select_from_map));
			i.putExtra("LONGITUDE", selectedPoint.longitude + "");
			i.putExtra("LATITUDE", selectedPoint.latitude + "");
			startActivity(i);
		}
	}

    /***** Sets up the map if it is possible to do so *****/
    public static void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            mMap = ((SupportMapFragment) fragmentManager.findFragmentById(R.id.map)).getMap();
            // Check if we were successful in obtaining the map.
            if (mMap != null)
                setUpMap();
        }
    }

    /**
     * This is where we can add markers or lines, add listeners or move the
     * camera.
     * <p>
     * This should only be called once and when we are sure that {@link #mMap}
     * is not null.
     */
    private static void setUpMap() {
    	mMap.setOnMapClickListener(new OnMapClickListener() {
			
    		 @Override
    		    public void onMapClick(LatLng point) {
    		        mTapTextView.setText("tapped, point=" + point);
    		        selectedPoint = point;
    		        
    		        if (marker != null) {
                        marker.remove();
    		        }
    		        
    		        // For dropping a marker at a point on the Map
    		        marker = mMap.addMarker(new MarkerOptions().position(point).draggable(true));
    		        
    		        // For zooming automatically to the Dropped PIN Location
    		        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(point, 15.0f));
    		    }
		});
        mMap.setOnMapLongClickListener(new OnMapLongClickListener() {
			
        	 @Override
        	    public void onMapLongClick(LatLng point) {
        	        mTapTextView.setText("long pressed, point=" + point);
        	        selectedPoint = point;
    		        
        	        if (marker != null) {
                        marker.remove();
    		        }
    		        
    		        // For dropping a marker at a point on the Map
    		        marker = mMap.addMarker(new MarkerOptions().position(point).draggable(true));
    		        
    		        // For zooming automatically to the Dropped PIN Location
    		        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(point, 15.0f));
        	    }
		});
        
        // For showing a move to my loction button
        mMap.setMyLocationEnabled(true);
    }

    /**** The mapfragment's id must be removed from the FragmentManager
     **** or else if the same it is passed on the next time then 
     **** app will crash ****/
    @Override
    public void onDestroy() {
        super.onDestroy();
        mMap = null;
    }

}
