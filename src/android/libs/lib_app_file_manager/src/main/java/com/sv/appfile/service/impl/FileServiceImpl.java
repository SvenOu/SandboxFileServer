package com.sv.appfile.service.impl;

import android.content.Context;
import android.content.ContextWrapper;
import android.text.TextUtils;
import android.util.Log;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sv.appfile.bean.SourceFileInfo;
import com.sv.appfile.bean.UrlMapping;
import com.sv.appfile.service.FileService;
import com.sv.appfile.utils.FileUtils;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.util.Zip4jConstants;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;

import static fi.iki.elonen.NanoHTTPD.newChunkedResponse;
import static fi.iki.elonen.NanoHTTPD.newFixedLengthResponse;

public class FileServiceImpl implements FileService {
    private static final String TAG = FileServiceImpl.class.getName();
    private final ObjectMapper om;
    private Context context;

    public FileServiceImpl(Context context) {
        om = new ObjectMapper();
        this.context = context;
    }

    @Override
    public NanoHTTPD.Response handerUri(String applicatonId, NanoHTTPD.IHTTPSession session) {
        Map<String, String> headers = session.getHeaders();
        Map<String, String> parms = session.getParms();
        String uri = session.getUri();
        NanoHTTPD.Method method = session.getMethod();

        if(TextUtils.isEmpty(uri) || !uri.startsWith("/"+applicatonId)){
            return newFixedLengthJsonResponse("invalid url:  " + uri +
                    ", url must start with applicatonId: /" + applicatonId);
        }

        Map<String, String> files = new HashMap<>();
        if (NanoHTTPD.Method.POST.equals(method) || NanoHTTPD.Method.PUT.equals(method)) {
            try {
                session.parseBody(files);
            } catch (IOException ioe) {
                return newFixedLengthJsonResponse("Internal Error IO Exception: " + ioe.getMessage());
            } catch (NanoHTTPD.ResponseException re) {
                return newFixedLengthJsonResponse(re.getMessage());
            }
        }

        String url = uri.replace("/"+applicatonId, "");
        String msg = "no available handler for " + url;

        if(method.name().equalsIgnoreCase(UrlMapping.GET_FILE_INFO.getMethod().name())
                && url.equals(UrlMapping.GET_FILE_INFO.getUrl())){
            ContextWrapper c = new ContextWrapper(context);
            SourceFileInfo fInfo = FileUtils.getSourceFileInfo(c.getFilesDir().getParent());
            try {
                return newFixedLengthJsonResponse(om.writeValueAsString(fInfo));
            } catch (JsonProcessingException e) {
                e.printStackTrace();
                msg = e.getMessage();
            }
        }
        else if(method.name().equalsIgnoreCase(UrlMapping.UPLOAD_AND_REPLACE_FILE.getMethod().name())
                && url.equals(UrlMapping.UPLOAD_AND_REPLACE_FILE.getUrl())){
            String targetPath = parms.get("targetPath");
            String fileName = parms.get("file");

            String tempFilePath = files.get("file");

            if (null == tempFilePath || null == targetPath) {
                return newFixedLengthJsonResponse("Error! targetPath and file must not empty");
            }
            File dst = new File(targetPath);
            if (dst.exists()) {
                Log.w(TAG, "tray to replace file: " + targetPath + " with " + fileName);
            }
            File src = new File(tempFilePath);
            try {
                InputStream in = new FileInputStream(src);
                OutputStream out = new FileOutputStream(dst);
                byte[] buf = new byte[65536];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
                in.close();
                out.close();
            }catch (IOException ioe) {
                return newFixedLengthJsonResponse("Error: " + ioe.getMessage());
            }
            return newFixedLengthResponse("success");
        }
        else if(method.name().equalsIgnoreCase(UrlMapping.GET_FILE_CODE.getMethod().name())
                && url.equals(UrlMapping.GET_FILE_CODE.getUrl())){
            String path = parms.get("path");
            File file = new File(path);

            if(file.length() > 5 * 1024 * 1024 || !file.isFile()){
                return newFixedLengthJsonResponse(file.getPath() + ": size greater than 5M");
            }
            StringBuilder builder = new StringBuilder();

            BufferedReader reader = null;
            try {
                reader = new BufferedReader(new InputStreamReader(
                        new FileInputStream(file), StandardCharsets.UTF_8));
                String currentLine = reader.readLine();
                while (currentLine != null) {
                    builder.append(currentLine);
                    builder.append("\n");
                    currentLine = reader.readLine();
                }
                reader.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return newFixedLengthJsonResponse(builder.toString());
        }
        else  if(method.name().equalsIgnoreCase(UrlMapping.DOWNLOAD_FILE.getMethod().name())
                && url.equals(UrlMapping.DOWNLOAD_FILE.getUrl())){
            String path = parms.get("path");
            if(TextUtils.isEmpty(path)){
                path = new ContextWrapper(context).getFilesDir().getParent();
            }
            File file = new File(path);

            if(!file.exists()){
                return newFixedLengthJsonResponse("no file found:  " + file.getAbsolutePath());
            }
            if(file.isDirectory()){
                ContextWrapper c = new ContextWrapper(context);
                String zipFilePath = c.getCacheDir().getAbsolutePath() + "/temp_for_download_all.zip";
                File zipFile = null;
                try {
                    zipFile = generateUserDirZip(file, new File(zipFilePath));
                } catch (ZipException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return downloadFile(zipFile, file.getName() + ".zip", headers);
            }
            return downloadFile(file, file.getName(), headers);
        }
        return newFixedLengthJsonResponse("Error response for " + uri + ": " + msg);
    }

    private File generateUserDirZip(File file, File zip) throws ZipException, IOException {
        if(!zip.exists() || zip.delete()){
            zip.getParentFile().mkdirs();
        }
        String zipFilePath = zip.getAbsolutePath();
        ZipFile zipFile = new ZipFile(zipFilePath);
        // Initiate Zip Parameters which define various properties such
        // as compression method, etc.
        ZipParameters parameters = new ZipParameters();
        // set compression method to store compression
        parameters.setCompressionMethod(Zip4jConstants.COMP_DEFLATE);
        // Set the compression level
        parameters.setCompressionLevel(Zip4jConstants.DEFLATE_LEVEL_NORMAL);
        // Add folder to the zip file
        zipFile.addFolder(file, parameters);
        return zipFile.getFile();
    }

    private NanoHTTPD.Response downloadFile(File file, String fileName,  Map<String, String> header) {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
        }catch (FileNotFoundException ex) {
            return newFixedLengthJsonResponse("Error response for " + file.getAbsolutePath()
                    + ": " + ex.getMessage());
        }
//        NanoHTTPD.Response response = newFixedLengthResponse(NanoHTTPD.Response.Status.OK,
//                "application/octet-stream",  fis, file.getTotalSpace());
        NanoHTTPD.Response response = newChunkedResponse(NanoHTTPD.Response.Status.OK,
                "application/octet-stream",  fis);
        response.addHeader("Accept-Ranges", "bytes");
        response.addHeader("Content-Disposition", "attachment; filename=\""+ fileName + "\"");
        return response;
    }

    private NanoHTTPD.Response newFixedLengthJsonResponse(String msg) {
        NanoHTTPD.Response response = newFixedLengthResponse(msg);
        response.addHeader("Content-Type", "application/json;charset=UTF-8");
        return response;
    }
}
