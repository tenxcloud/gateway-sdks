package com.apigateway.sdk.utils;

/**
 * @author hpy
 * @date 2021
 */
public class SdkLogger {
  private static boolean DEBUG = Boolean.getBoolean("sdk.DEBUG") || Boolean.getBoolean("http.caller.DEBUG");

  // for performance considering, pls add this check method before invoke print method
  public static boolean isLoggable() {
    return DEBUG;
  }

  public static void print(String msg) {
    if(isLoggable()) {
      System.out.println(msg);
    }
  }
}
