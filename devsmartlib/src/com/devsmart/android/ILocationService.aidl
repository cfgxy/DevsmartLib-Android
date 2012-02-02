package com.devsmart.android;

interface ILocationService {
  Location getBestLocation();
  Location getMarsLocation();
  String getAddress();
  String getLocationCode();
  String getMarsLocationCode();
  void requestGsmLocation();
  void stop();
  void start();
  void pause();
  void resume();
  boolean isRunning();
}