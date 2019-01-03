package com.qucetel.andrew.serialjnidemo;

public interface IMQTTCallback {
    void setStatusState(String state);
    void notifyUser(String msg);
    void proccessPost(String msg);
}
