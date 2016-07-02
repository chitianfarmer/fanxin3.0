package com.fanxin.app.main.utils;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;

import java.io.File;
import java.io.IOException;
import java.net.FileNameMap;
import java.net.URLConnection;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Created by huangfangyi/qq84543217 on 2016/6/30.
 */
public class OkHttpManager {
    public static Context context;
    public static OkHttpManager serverTask;
    private static OkHttpClient okHttpClient;
    private static final int RESULT_ERROR = 1000;
    private static final int RESULT_SUCESS = 2000;
    private HttpCallBack httpCallBack;
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            int reusltCode = msg.what;
            switch (reusltCode) {

                case RESULT_ERROR:
                    httpCallBack.onFailure((String) msg.obj);

                    Toast.makeText(context, "服务器端无响应", Toast.LENGTH_SHORT).show();
                    Log.d("result", (String) msg.obj);
                    break;
                case RESULT_SUCESS:
                    String result = (String) msg.obj;

                    try {
                        JSONObject jsonObject = JSONObject.parseObject(result);
                        httpCallBack.onResponse(jsonObject);
                    } catch (JSONException e) {
                        httpCallBack.onFailure((String) msg.obj);
                        Toast.makeText(context, "响应数据解析错误", Toast.LENGTH_SHORT).show();
                    }
                    Log.d("result", result);
                    break;
            }

        }
    };

    public OkHttpManager(Context context) {
        this.context = context;
        okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(10000L, TimeUnit.MILLISECONDS)
                .readTimeout(10000L, TimeUnit.MILLISECONDS)
                //其他配置
                .build();
    }

    public static synchronized void init(Context context) {
        if (serverTask == null) {
            serverTask = new OkHttpManager(context);
        }

    }

    public static OkHttpManager getInstance() {
        if (serverTask == null) {
            throw new RuntimeException("please init first!");
        }
        return serverTask;
    }

    //纯粹键值对post请求
    public void post(List<Param> params, String url, HttpCallBack httpCallBack) {
        this.httpCallBack = httpCallBack;
        FormBody.Builder bodyBulder = new FormBody.Builder();
        for (Param param : params) {
            bodyBulder.add(param.getKey(), param.getValue());

        }
        RequestBody requestBody = bodyBulder.build();
        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();

        startRequest(request);

    }

    //键值对+文件 post请求
    public void post(List<Param> params, List<File> files, String url, HttpCallBack httpCallBack) {
        Log.d("url", url);
        this.httpCallBack = httpCallBack;
        MultipartBody.Builder builder = new MultipartBody.Builder();
        builder.setType(MultipartBody.FORM);
        for (Param param : params) {

            builder.addPart(Headers.of("Content-Disposition", "form-data; name=\"" + param.getKey() + "\""), RequestBody.create(MediaType.parse(guessMimeType(param.getKey())), param.getValue()));
        }
        for (File file : files) {
            if (file != null && file.exists()) {

                //TODO 根据文件名设置contentType
                builder.addPart(Headers.of("Content-Disposition",
                        "form-data; name=\"" + file + "\"; filename=\"" + file.getName() + "\""),
                        RequestBody.create(MediaType.parse(guessMimeType(file.getName())), file));


            }

        }
        RequestBody requestBody = builder.build();
        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();

        startRequest(request);

    }

    private void startRequest(Request request) {

        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Message message = handler.obtainMessage();
                message.what = RESULT_ERROR;
                message.obj = e.getMessage().toString();
                message.sendToTarget();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                Message message = handler.obtainMessage();
                message.what = RESULT_SUCESS;
                message.obj = response.body().string();
                message.sendToTarget();

            }
        });
    }


    public interface HttpCallBack {
        void onResponse(JSONObject jsonObject);

        void onFailure(String errorMsg);
    }

    private String guessMimeType(String path) {
        FileNameMap fileNameMap = URLConnection.getFileNameMap();
        String contentTypeFor = fileNameMap.getContentTypeFor(path);
        if (contentTypeFor == null) {
            contentTypeFor = "application/octet-stream";
        }
        return contentTypeFor;
    }
}