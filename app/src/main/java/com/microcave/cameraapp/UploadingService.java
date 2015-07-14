package com.microcave.cameraapp;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.net.UnknownHostException;

public class UploadingService extends IntentService {
    String url = "https://image-judger.herokuapp.com/api/images";
    String path;
    public UploadingService() {
        super("UploadingService");
    }
    @Override
    protected void onHandleIntent(Intent intent) {
        path = intent.getStringExtra("path");
        Log.e("url", url);
        Log.e("path", path);
        sendPost(url, path);
    }
    public void sendPost(String url, String imagePath) {
        try {
            HttpClient httpclient = new DefaultHttpClient();
            httpclient.getParams().setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
            HttpPost httppost = new HttpPost(url);
            File file = new File(imagePath);
            Log.e("File path ", "" + file.toString());
            MultipartEntity mpEntity = new MultipartEntity();
            ContentBody cbFile = new FileBody(file, "image/jpeg");
            mpEntity.addPart("image", cbFile);
            httppost.setEntity(mpEntity);
            Log.e("executing request " + httppost.getRequestLine(), "");
            HttpResponse response = httpclient.execute(httppost);
            HttpEntity resEntity = response.getEntity();
            Log.e("res.entity", response.getStatusLine() + "");
            if (resEntity != null) {
                String val = EntityUtils.toString(resEntity);
                JSONObject obj = new JSONObject(val);
                if (obj.getString("result").equals("ok")) {
                    Log.e("status", "successfully uploaded");
                    File f = new File(imagePath);
                    f.delete();
                    Log.e("status", "successfully Deleted" + imagePath + ".jpg");
                }
            }
            if (resEntity != null) {
                resEntity.consumeContent();
            }
            httpclient.getConnectionManager().shutdown();
        } catch (HttpHostConnectException e) {
            Log.e("SendImage", e.getMessage());
        } catch (UnknownHostException e) {
            Log.e("Camera_app host error", e.getMessage());
            // this is handled when wifi or internet is unavailabe.
        } catch (JSONException e) {
            Log.e("JSon error", e.getMessage());
        } catch (Exception e) {
            Log.e("Camera app", e.getMessage());
        }
    }
}
