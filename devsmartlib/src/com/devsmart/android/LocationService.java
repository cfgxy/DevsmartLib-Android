package com.devsmart.android;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.Date;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import com.devsmart.android.location.CellInfo;
import com.devsmart.android.location.CellInfoManager;
import com.devsmart.android.location.WifiInfo;
import com.devsmart.android.location.WifiInfoManager;

public class LocationService extends Service {
  private Location mBestLocation;
  private Location mMarsLocation;
  private String mAddress;
  private String mLocationCode;
  private String mMarsLocationCode;
  private boolean started;

	ILocationService.Stub mService = new ILocationService.Stub() {

		public Location getBestLocation() throws RemoteException {
			return mBestLocation;
		}

    public String getAddress() throws RemoteException {
      return mAddress;
    }

    public void requestGsmLocation() throws RemoteException {
      LocationService.this.requestGsmLocation();
    }

    public void stop() throws RemoteException {
      started = false;
      mLocationManager.removeUpdates(mLocationListener);
    }

    public void start() throws RemoteException {
      doStart();
    }

    public Location getMarsLocation() throws RemoteException {
      return mMarsLocation;
    }

    public String getLocationCode() throws RemoteException {
      return mLocationCode;
    }
    
    public String getMarsLocationCode() throws RemoteException {
      return mMarsLocationCode;
    }

    public boolean isRunning() throws RemoteException {
      return started;
    }

    public void pause() throws RemoteException {
      started = false;
      mLocationManager.removeUpdates(mLocationListener);
    }

    public void resume() throws RemoteException {
      started = true;
      mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, mLocationListener);
      mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, mLocationListener);
    }

    
	};
	private LocationManager mLocationManager;


	@Override
	public IBinder onBind(Intent arg0) {
		return mService;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		doStart();
	}
	
	public void doStart() {
	  if(started) return;
	  started = true;
	  
    mLocationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
    mBestLocation = null;
    if(mBestLocation == null) {
      mBestLocation = mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
    }
    if(mBestLocation == null) {
      mBestLocation = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
    }
    if(mBestLocation != null) {
      requestMars(toJSON(mBestLocation));
      try {
        mLocationCode = Base64.encodeToString(toJSON(mBestLocation).toJSONString().getBytes("utf-8"), Base64.NO_WRAP);
      } catch (UnsupportedEncodingException e) {
      }
    }
    requestGsmLocation();
    mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, mLocationListener);
    mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, mLocationListener);
	}

	private LocationListener mLocationListener = new LocationListener() {

		public void onStatusChanged(String provider, int status, Bundle extras) {
			// TODO Auto-generated method stub

		}

		public void onProviderEnabled(String provider) {
			// TODO Auto-generated method stub

		}

		public void onProviderDisabled(String provider) {
			// TODO Auto-generated method stub

		}

		public void onLocationChanged(Location location) {
			if(isBetterLocation(location, mBestLocation)){
				mBestLocation = location;
        try {
          mLocationCode = Base64.encodeToString(toJSON(mBestLocation).toJSONString().getBytes("utf-8"), Base64.NO_WRAP);
        } catch (UnsupportedEncodingException e) {
        }
	      mMarsLocation = null;
	      mMarsLocationCode = null;
	      mAddress = null;
				requestMars(toJSON(mBestLocation));
			}

		}

	};

	@Override
	public void onDestroy() {
		super.onDestroy();
		mLocationManager.removeUpdates(mLocationListener);
	}

	private static final int TWO_MINUTES = 1000 * 60 * 2;

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
	

  public static JSONObject toJSON(Location location) {
    if(location == null) return null;
    
    JSONObject loc = new JSONObject();
    loc.put("provider", location.getProvider());
    loc.put("lat", location.getLatitude());
    loc.put("lng", location.getLongitude());
    loc.put("accuracy", location.getAccuracy());
    loc.put("timestamp", location.getTime());
    
    return loc;
  }
  
  public static Location toLocation(JSONObject loc) {
    Location loc1 = new Location((String)loc.get("provider"));
    loc1.setAccuracy(((Number)loc.get("accuracy")).floatValue());
    loc1.setLatitude(((Number)loc.get("lat")).doubleValue());
    loc1.setLongitude(((Number)loc.get("lng")).doubleValue());
    loc1.setTime(((Number)loc.get("timestamp")).longValue());
    
    return loc1;
  }
  
	/** Checks whether two providers are the same */
	private boolean isSameProvider(String provider1, String provider2) {
		if (provider1 == null) {
			return provider2 == null;
		}
		return provider1.equals(provider2);
	}


  private void requestMars(JSONObject loc1) {
    final JSONObject loc = loc1;
    
    if(loc == null || loc.get("lat") == null || ((Number)loc.get("lat")).intValue() == 0) return;
    String params = "lat=" + ((Number)loc.get("lat")).doubleValue() + "&lng=" +  ((Number)loc.get("lng")).doubleValue();
    final HttpGet get = new HttpGet("http://365jia.cn/api/common/maps/get_mars.json?" + params);
    
    Log.d("LocationService", "requestMars");
    
    
    new Thread(new Runnable() {

      public void run() {
        DefaultHttpClient client = new DefaultHttpClient();
        JSONObject jo = null;
        try {
          
          HttpResponse  resp = client.execute(get);
          jo = (JSONObject) JSONValue.parse(new BufferedReader(new InputStreamReader(resp.getEntity().getContent(), "UTF-8")));
          Log.d("LocationService", jo.toJSONString());
          jo = (JSONObject) jo.get("message");
        } catch (Exception e) {e.printStackTrace();}
        
        if(jo == null) return;
        mMarsLocation = toLocation(loc);
        mMarsLocation.setLatitude(((Number)jo.get("lat")).doubleValue());
        mMarsLocation.setLongitude(((Number)jo.get("lng")).doubleValue());
        try {
          mMarsLocationCode = Base64.encodeToString(toJSON(mMarsLocation).toJSONString().getBytes("utf-8"), Base64.NO_WRAP);
        } catch (Exception e) {
        }
        requestAddress(toJSON(mMarsLocation));
        
        if(mMarsLocationCode != null) Log.d("LocationService", mMarsLocationCode);
      }
      
    }).start();
    
  }

	private void requestGsmLocation() {
    Log.d("LocationService", "requestGsmLocation");


    new Thread(new Runnable() {

      public void run() {
        HttpPost post = new HttpPost("http://www.google.com/loc/json");
        
        DefaultHttpClient client = new DefaultHttpClient();
        JSONObject params = new JSONObject();
        if(!prepareGsmParams(params)) return;
        
        JSONObject jo = null;
        try {
          StringEntity se = new StringEntity(params.toString());
          post.setEntity(se);
          HttpResponse  resp = client.execute(post);
          jo = (JSONObject) JSONValue.parse(new BufferedReader(new InputStreamReader(resp.getEntity().getContent(), "UTF-8")));
          jo = (JSONObject) jo.get("location");
        } catch (Exception e) {}
        
        if(jo == null || jo.get("latitude") == null) return;
        
        Location loc = new Location("gsm");
        loc.setAccuracy(((Double)jo.get("accuracy")).floatValue());
        loc.setLatitude((Double)jo.get("latitude"));
        loc.setLongitude((Double)jo.get("longitude"));
        loc.setTime(new Date().getTime());
        mLocationListener.onLocationChanged(loc);
        
        Log.d("LocationService", mLocationCode);
      }
    }).start();
    

  }
  
  private boolean prepareGsmParams(JSONObject params) {

    try{
      JSONArray cells = new JSONArray();
      JSONArray wifis = new JSONArray();
      
      for(CellInfo info : CellInfoManager.getCellIDInfo(getApplicationContext())) {
        JSONObject n = new JSONObject();
        n.put("cell_id", info.cellId);
        n.put("mobile_country_code", info.mobileCountryCode);
        n.put("mobile_network_code", info.mobileNetworkCode);
        n.put("location_area_code", info.locationAreaCode);
        cells.add(n);
      }
      
      for(WifiInfo info: WifiInfoManager.getWifiInfo(getApplicationContext())) {
        JSONObject n = new JSONObject();
        n.put("mac_address", info.mac);
        n.put("ssid", info.ssid);
        n.put("signal_strength", info.strenth);
        wifis.add(n);
      }
        
      
      params.put("version", "1.1.0");
      params.put("host", "maps.google.com");
      params.put("address_language", "zh_CN");
      params.put("request_address", false);
      
      if(cells.size() > 0) {
        params.put("radio_type", CellInfoManager.getRadioType(getApplicationContext()));
        params.put("home_mobile_country_code", CellInfoManager.getMcc(getApplicationContext()));
        params.put("home_mobile_network_code", CellInfoManager.getMnc(getApplicationContext()));
        params.put("carrier", CellInfoManager.getCarrier(getApplicationContext()));
        params.put("cell_towers", cells);
      }
      
      if(wifis.size() > 0) {
        params.put("wifi_towers", wifis);
      }
      
    } catch(Exception e) {
      return false;
    }
    
    return true;
    
  }
  
  private void requestAddress(JSONObject loc1) {
    Log.d("LocationService", "requestAddress");
    
    final JSONObject loc = loc1;

    new Thread(new Runnable() {

      public void run() {
        HttpPost post = new HttpPost("http://www.google.com/loc/json");
        
        DefaultHttpClient client = new DefaultHttpClient();
        JSONObject params = new JSONObject();
        
        if(loc != null) {
          JSONObject data = new JSONObject();
          data.put("latitude", loc.get("lat"));
          data.put("longitude", loc.get("lng"));
          
          params.put("version", "1.1.0");
          params.put("host", "maps.google.com");
          params.put("location", data);
        } else {
          if(!prepareGsmParams(params)) return;
        }
        
        params.put("address_language", "zh_CN");
        params.put("request_address", true);

        JSONObject jo = null;
        try {
          StringEntity se = new StringEntity(params.toString());
          post.setEntity(se);
          HttpResponse  resp = client.execute(post);
          jo = (JSONObject) JSONValue.parse(new BufferedReader(new InputStreamReader(resp.getEntity().getContent(), "UTF-8")));
          jo = (JSONObject) jo.get("location");
          jo = (JSONObject) jo.get("address");
        } catch (Exception e) {}
        
        if(jo == null) return;
        mAddress = jo.toJSONString();
        
        Log.d("LocationService", mAddress);
      }
    }).start();
    

  }
  
  
}
