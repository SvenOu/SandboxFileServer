package com.sv.appfile.bean;

import fi.iki.elonen.NanoHTTPD;

public enum UrlMapping {
    /**
     *
     */
    GET_FILE_INFO(NanoHTTPD.Method.GET, "/appfile/getFileInfo"),
    GET_FILE_CODE(NanoHTTPD.Method.GET, "/appfile/getFileCode"),
    DOWNLOAD_FILE(NanoHTTPD.Method.GET, "/appfile/downloadFile"),
    UPLOAD_AND_REPLACE_FILE(NanoHTTPD.Method.POST, "/appfile/uploadAndReplaceFile");

    private String url;
    private NanoHTTPD.Method method;

    UrlMapping(NanoHTTPD.Method method, String url) {
        this.url = url;
        this.method = method;
    }

    public String getUrl() {
        return  url;
    }

    public void setUrl(String url) {
        this.url =  url;
    }

    public NanoHTTPD.Method getMethod() {
        return method;
    }

    public void setMethod(NanoHTTPD.Method method) {
        this.method = method;
    }
}
