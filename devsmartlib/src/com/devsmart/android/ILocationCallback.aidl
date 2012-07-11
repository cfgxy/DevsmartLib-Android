package com.devsmart.android;

oneway interface ILocationCallback {
  void onLocationReturn(String json);
  void onMarsReturn(String json);
  void onAddressReturn(String json);
}
