package com.lbb.lmps.dto;

public class SecurityContext {

    private String channel;

    public SecurityContext() {}

    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }

    @Override
    public String toString() {
        return "SecurityContext{channel='" + channel + "'}";
    }
}