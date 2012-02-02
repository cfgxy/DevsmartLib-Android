package com.devsmart.android.location;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;

public class WifiInfoManager {  

  public static List<WifiInfo> getWifiInfo(Context mContext){ 
    List<WifiInfo> wifi = new ArrayList<WifiInfo>();  
    WifiManager wm = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);  
    if(!wm.isWifiEnabled()) {
      return wifi;
    }
    
    WifiInfo info = new WifiInfo();  
    info.mac = wm.getConnectionInfo().getBSSID();
    info.ssid = wm.getConnectionInfo().getSSID();
    info.strenth = wm.getConnectionInfo().getRssi();
    
    wifi.add(info);  
    
    for(ScanResult sr : wm.getScanResults()) {
      WifiInfo inf = new WifiInfo();  
      inf.mac = sr.BSSID;
      inf.ssid = sr.SSID;
      inf.strenth = sr.level;
      wifi.add(inf);  
    }
    
    return wifi;  
  }  
}  