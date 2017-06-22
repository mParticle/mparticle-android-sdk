package com.mparticle.internal.dto;

public class ReadyUpload {
    private int id;
    private String message;

    public ReadyUpload(int id, String message) {
        this.id = id;
        this.message = message;
    }


    public int getId() {
        return id;
    }

    public String getMessage() {
        return message;
    }
}
