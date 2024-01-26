package com.github.shafiqsadat.IPTV.utils;

public class IPTVModel {
    private String name;
    private String count;
    private String streamLink;

    public IPTVModel(String name, String count, String streamLink) {
        this.name = name;
        this.count = count;
        this.streamLink = streamLink;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCount() {
        return count;
    }

    public void setCount(String count) {
        this.count = count;
    }

    public String getStreamLink() {
        return streamLink;
    }

    public void setStreamLink(String streamLink) {
        this.streamLink = streamLink;
    }
}
