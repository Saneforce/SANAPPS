/*
       Licensed to the Apache Software Foundation (ASF) under one
       or more contributor license agreements.  See the NOTICE file
       distributed with this work for additional information
       regarding copyright ownership.  The ASF licenses this file
       to you under the Apache License, Version 2.0 (the
       "License"); you may not use this file except in compliance
       with the License.  You may obtain a copy of the License at

         http://www.apache.org/licenses/LICENSE-2.0

       Unless required by applicable law or agreed to in writing,
       software distributed under the License is distributed on an
       "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
       KIND, either express or implied.  See the License for the
       specific language governing permissions and limitations
       under the License.
*/
package com.saneforce.sangps;


import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Calendar;
import java.text.SimpleDateFormat;

import android.app.Activity;
import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import com.google.android.gms.location.LocationSettingsRequest; 
import com.google.android.gms.location.LocationSettingsResult; 
import com.google.android.gms.location.LocationSettingsStatusCodes; 

import android.location.Geocoder;
import android.location.Address;

import android.os.Bundle;
import android.os.StrictMode;
import android.widget.Toast;
import android.content.Intent;
import android.content.IntentSender;
import android.app.IntentService;
import android.content.IntentFilter; 
import android.content.BroadcastReceiver;
import android.provider.Settings;
import android.provider.Settings.Secure;

public class SANGPS extends CordovaPlugin implements LocationListener,GoogleApiClient.ConnectionCallbacks,GoogleApiClient.OnConnectionFailedListener
{
	private static final long INTERVAL = 1000 * 3;
	private static final long FASTEST_INTERVAL = 1000 * 2;
	
	LocationRequest mLocationRequest;
	GoogleApiClient mGoogleApiClient;
	Location mCurrentLocation;
	String mLastUpdateTime;
	int mShowSettings;
    private Boolean useLocationTracker = false;

	private static final String ACTION_GPS = "android.location.PROVIDERS_CHANGED";
	private BroadcastReceiver yourReceiver;

	@Deprecated
	public static final String LOCATION_PROVIDERS_ALLOWED = "location_providers_allowed";

	protected void createLocationRequest() {
		mLocationRequest = new LocationRequest();
		mLocationRequest.setInterval(INTERVAL);
		mLocationRequest.setFastestInterval(FASTEST_INTERVAL);
		mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
	}

	@Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
		if (action.equals("getdate")) {
			//Toast.makeText(this.webView.getContext(), GetDateTime(), Toast.LENGTH_LONG).show();
			callbackContext.success(GetDateTime());
		}
		else if (action.equals("check_gps")) {
			JSONObject arg_object = args.getJSONObject(0);
			mShowSettings=arg_object.getInt("gps");
			callbackContext.success(String.valueOf(chkEnaGPS()));
		}
		else
		{ 
			JSONObject arg_object = args.getJSONObject(0);
			mShowSettings=arg_object.getInt("gps");
			if(useLocationTracker==false)
			{
				if (!isGooglePlayServicesAvailable()) {
					Toast.makeText(this.webView.getContext(), "Location Service Not Available", Toast.LENGTH_LONG).show();
					return false;
				}
				int SDK_INT = android.os.Build.VERSION.SDK_INT;
				if (SDK_INT > 8) 
				{
					StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
					StrictMode.setThreadPolicy(policy);
					//your codes here
				}
    
				createLocationRequest();
				mGoogleApiClient = new GoogleApiClient.Builder(this.webView.getContext())
					.addApi(LocationServices.API)
					.addConnectionCallbacks(this)
					.addOnConnectionFailedListener(this)
					.build();	
				mGoogleApiClient.connect();
				useLocationTracker=true;
			}		
			registerReceiverGPS();	
			Context context=this.cordova.getActivity().getApplicationContext();
			LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
			lm.addGpsStatusListener(new android.location.GpsStatus.Listener()
			{
				public void onGpsStatusChanged(int event)
				{
					if (event!=1) chkEnaGPS();
				}
			});
			chkEnaGPS();
		}
		return true;
	}
	public String GetDateTime(){
		Calendar c = Calendar.getInstance();
        System.out.println("Current time => "+c.getTime());

        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return  df.format(c.getTime());
	
	}

	@Override
	public void onStart() {
		super.onStart();
		mGoogleApiClient.connect();
	}

	@Override
	public void onStop() {
		super.onStop();
		//Toast.makeText(this.webView.getContext(), "Connection Stop", Toast.LENGTH_LONG).show();
		
		//mGoogleApiClient.disconnect();
	}
	
	private boolean isGooglePlayServicesAvailable() {
		int status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this.webView.getContext());
		if (ConnectionResult.SUCCESS == status) {
			return true;
		} else {
			GooglePlayServicesUtil.getErrorDialog(status,this.cordova.getActivity(), 0).show();
			return false;
		}
	}
	@Override
	public void onConnected(Bundle bundle) {
		startLocationUpdates();
	}

	protected void startLocationUpdates() {
		PendingResult<Status> pendingResult=LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
	}

	@Override
	public void onConnectionSuspended(int i) {
	//Toast.makeText(this.webView.getContext(), "Connection Suspended: " , Toast.LENGTH_LONG).show();
		
	}

	@Override
	public void onConnectionFailed(ConnectionResult connectionResult) {
		//Toast.makeText(this.webView.getContext(), "Connection failed: " + connectionResult.toString(), Toast.LENGTH_LONG).show();
		//Log.d(TAG, "Connection failed: " + connectionResult.toString());
	}

	@Override
	public void onLocationChanged(Location location) {
			String locAddrs=""; //getAddress(location);
			mCurrentLocation = location;
			mLastUpdateTime = GetDateTime();
			
			//Toast.makeText(this.webView.getContext(), "Location Service Available : "+String.valueOf(location.getLatitude()), Toast.LENGTH_LONG).show();
			this.webView.sendJavascript("getLocation('"+String.valueOf(location.getLatitude())+";"+String.valueOf(location.getLongitude())+";"+String.valueOf(mLastUpdateTime)+";" + String.valueOf(locAddrs)+";"+String.valueOf(location.getAccuracy())+";"+String.valueOf(location.getBearing())+";');");
	}
	
	
	private boolean chkEnaGPS(){
		Context context=this.cordova.getActivity().getApplicationContext();
		LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
		boolean isGPSEnabled = false;
		boolean isNetworkEnabled = false;

		isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
		isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        
		if (!isGPSEnabled && !isNetworkEnabled) {
			//Toast.makeText(this.webView.getContext(),String.valueOf(mShowSettings)+"==0"+ String.valueOf(mShowSettings==0), Toast.LENGTH_LONG).show();
			if(mShowSettings==0){
				Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
				this.webView.getContext().startActivity(intent);
			}
			this.webView.sendJavascript("clearLocation();");

			return false;
		}
		return true;
	}

	private void registerReceiverGPS() {
		if (yourReceiver == null) {
			final IntentFilter theFilter = new IntentFilter();
			theFilter.addAction(ACTION_GPS);
			yourReceiver = new BroadcastReceiver() {
				@Override
				public void onReceive(Context context, Intent intent) {
					if (intent != null) {
						String s = intent.getAction();
						if (s != null) {
							if (s.equals(ACTION_GPS)) {
								chkEnaGPS();
							}
						}
					}
				}
			};
			Context context=this.cordova.getActivity().getApplicationContext();
			context.registerReceiver(yourReceiver, theFilter);
		}
	}
}