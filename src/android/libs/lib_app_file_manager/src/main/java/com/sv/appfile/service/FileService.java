package com.sv.appfile.service;
import fi.iki.elonen.NanoHTTPD;
public interface FileService {
    NanoHTTPD.Response handerUri(String applicatonId, NanoHTTPD.IHTTPSession session);
}
