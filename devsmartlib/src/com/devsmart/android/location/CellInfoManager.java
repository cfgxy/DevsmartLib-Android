package com.devsmart.android.location;

import java.lang.reflect.Method;
import java.util.ArrayList;
import android.content.Context;
import android.telephony.CellLocation;
import android.telephony.NeighboringCellInfo;
import android.telephony.TelephonyManager;
import android.telephony.gsm.GsmCellLocation;

public class CellInfoManager {  
  
  /* China - CN
  * MCC    MNC    Brand    Operator                Status        Bands (MHz)                                    References and notes
  * 460   00  China Mobile  China Mobile    Operational   GSM 900 / GSM 1800 / TD-SCDMA 1880 / TD-SCDMA 2010
  * 460   01  China Unicom  China Unicom    Operational   GSM 900 / GSM 1800 / UMTS 2100  CDMA network sold to China Telecom, WCDMA commercial trial started in May 2009 and in full commercial operation as of October 2009.
  * 460   02  China Mobile  China Mobile    Operational   GSM 900 / GSM 1800 / TD-SCDMA 1880 / TD-SCDMA 2010  
  * 460   03  China Telecom China Telecom   Operational   CDMA2000 800 / CDMA2000 2100  EV-DO
  * 460   05  China Telecom China Telecom   Operational     
  * 460   06  China Unicom  China Unicom    Operational   GSM 900 / GSM 1800 / UMTS 2100  
  * 460   07  China Mobile  China Mobile    Operational   GSM 900 / GSM 1800 / TD-SCDMA 1880 / TD-SCDMA 010   
  * 460   20  China Tietong China Tietong   Operational   GSM-R    
  */ 
  @SuppressWarnings({"rawtypes"})
  public static ArrayList<CellInfo> getCellIDInfo(Context mContext){  
    TelephonyManager manager = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
    CellLocation location = manager.getCellLocation();
    
    
    ArrayList<CellInfo> CellID = new ArrayList<CellInfo>();  
    CellInfo cell = null;
    int mcc = CellInfoManager.getMcc(mContext);
    int mnc = CellInfoManager.getMnc(mContext);
    
    String radio = CellInfoManager.getRadioType(mContext);
    
    if("gsm".equals(radio) || "wcdma".equals(radio)) {
      if(!(location instanceof GsmCellLocation) || location == null) return CellID;
      GsmCellLocation gsm = (GsmCellLocation)location;
      
      cell = new CellInfo();
      cell.cellId = gsm.getCid();
      cell.locationAreaCode = gsm.getLac();
      cell.mobileCountryCode = mcc;
      cell.mobileNetworkCode = mnc;
      cell.radioType = manager.getNetworkType() > 2 ? "wcdma" : "gsm";
      
      CellID.add(cell);

      for (NeighboringCellInfo info : manager.getNeighboringCellInfo()) {  
        cell = new CellInfo();  
        cell.radioType = manager.getNetworkType() > 2 ? "wcdma" : "gsm";
        cell.cellId = info.getCid();  
        cell.mobileCountryCode = mcc;
        cell.mobileNetworkCode = mnc;
        cell.strenth = info.getRssi();  
        cell.locationAreaCode = gsm.getLac();
        CellID.add(cell);  
      }
    } else if("cdma".equals(radio)) {
      if(location == null) return CellID;
      try {
        Class cls = location.getClass();
        Method mGetBaseStationId = cls.getMethod("getBaseStationId", new Class[0]);
        Method mGetNetworkId = cls.getMethod("getNetworkId", new Class[0]);
        Method mGetSystemId = cls.getMethod("getSystemId", new Class[0]);
        
        if(mGetBaseStationId == null || mGetNetworkId == null || mGetSystemId == null) return CellID;
        
        cell = new CellInfo();
        cell.cellId = (Integer)mGetBaseStationId.invoke(location, new Object[0]);
        cell.locationAreaCode = (Integer)mGetNetworkId.invoke(location, new Object[0]);
        cell.mobileCountryCode = mcc;
        cell.mobileNetworkCode = (Integer)mGetSystemId.invoke(location, new Object[0]);
        cell.radioType = "cdma";
        
        CellID.add(cell);
      } catch(Exception e) {
        return CellID;
      }
    }
    
    return CellID;
  }
  
  public static String getCarrier(Context mContext) {
    TelephonyManager manager = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
    return manager.getNetworkOperatorName();
  }
  
  
  public static int getMcc(Context mContext) {
    return 460;
  }
  
  public static int getMnc(Context mContext) {
    TelephonyManager manager = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
    return Integer.valueOf(manager.getNetworkOperator().substring(3, 5));
  }
  
  
  public static String getRadioType(Context mContext) {
    TelephonyManager manager = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
    switch(manager.getNetworkType()) {
      case 1://GPRS
      case 2://EDGE
        return "gsm";
      case 3://UMTS
      case 8://HSDPA
      case 9://HSUPA
      case 10://HSPA
        return "wcdma";
      case 4://CDMA
      case 7://1xRTT
      case 5://EVDO_0
      case 6://EVDO_A
      case 12://EVDO_B
        return "cdma";
      default:
        return null;
    }
  }
    
}  