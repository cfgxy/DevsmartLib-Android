package com.devsmart.android;
import com.devsmart.android.ILocationCallback;

interface ILocationService {
  Location getBestLocation();
  Location getMarsLocation();
  String getAddress();
  String getLocationCode();
  String getMarsLocationCode();
  void registerCallback(ILocationCallback callback);
  void unregisterCallback(ILocationCallback callback);
  void requestGsmLocation();
  void stop();
  void start();
  void pause();
  void resume();
  boolean isRunning();
}