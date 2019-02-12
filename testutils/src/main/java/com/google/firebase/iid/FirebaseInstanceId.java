package com.google.firebase.iid;

public class FirebaseInstanceId {
    private static String token;

    public static FirebaseInstanceId getInstance() {
        return new FirebaseInstanceId();
    }

    public static void setToken(String token) {
        FirebaseInstanceId.token = token;
    }

    public String getToken() {
        return token;
    }

    public String getToken(String authority, String scope) {
        return token;
    }
}
