package com.longer.idrecognize;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.UrlQuerySanitizer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.common.Priority;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.DownloadListener;
import com.androidnetworking.interfaces.DownloadProgressListener;
import com.androidnetworking.interfaces.JSONArrayRequestListener;
import com.androidnetworking.interfaces.JSONObjectRequestListener;
import com.androidnetworking.interfaces.UploadProgressListener;
import com.googlecode.tesseract.android.TessBaseAPI;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.net.URL;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends AppCompatActivity {

    static {
        System.loadLibrary("OpenCV");
    }

    private TessBaseAPI tessBaseApi;
    private String language = "ck";
    private AsyncTask<Void, Void, Boolean> asyncTask;
    private ProgressDialog progressDialog;////////////////
    private ImageView idCard;
    private TextView tesstext,runtime0,runtime1,runtime2,runtime3,runtime4;
    private int index = 0;
    private int[] ids = {
            R.drawable.id_card0,
            R.drawable.id_card1,
            R.drawable.id_card2,
            R.drawable.id_card3,
            R.drawable.id_card4,
            R.drawable.id_card5
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        idCard = findViewById(R.id.idcard);
        tesstext = findViewById(R.id.tv_idcard);
        runtime0 = findViewById(R.id.tv_time0);
        runtime1 = findViewById(R.id.tv_time1);
        runtime2 = findViewById(R.id.tv_time2);
        runtime3 = findViewById(R.id.tv_time3);
        runtime4 = findViewById(R.id.tv_time4);
        idCard.setImageResource(R.drawable.id_card0);
        //15
        tessBaseApi = new TessBaseAPI();
        methodRequiresTwoPermission();

        //网络连接初始化
        AndroidNetworking.initialize(getApplicationContext());


    }

    private native String stringFromJNI();


    //初始化OCR和识别字典
    @SuppressLint("StaticFieldLeak")
    private void initTess() {
        //让它在后台去初始化 记得加读写权限
        this.asyncTask = new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... params) {
                //目录+文件名 目录下需要tessdata目录
                InputStream is = null;
                FileOutputStream fos = null;
                try {
                    is = getAssets().open(language + ".traineddata");
                    File file = new File("/sdcard/tess/tessdata/" + language + ".traineddata");
                    if (!file.exists()) {
                        file.getParentFile().mkdirs();

                        file.createNewFile();

                        fos = new FileOutputStream(file);
                        byte[] buffer = new byte[2048];
                        int len;
                        while ((len = is.read(buffer)) != -1) {
                            fos.write(buffer, 0, len);
                        }
                        fos.close();
                    }
                    is.close();
                    return tessBaseApi.init("/sdcard/tess", language);
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        if (null != is)
                            is.close();
                        if (null != fos)
                            fos.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                return false;
            }

            @Override
            protected void onPreExecute() {
                showProgress();
            }

            @Override
            protected void onPostExecute(Boolean aBoolean) {
                dismissProgress();
                if (aBoolean) {
                    Toast.makeText(MainActivity.this, "初始化OCR成功", Toast.LENGTH_SHORT).show();
                } else {
                    finish();
                }
            }
        };
        asyncTask.execute();
    }


    //处理读取权限问题
    public static final int RC_CAMERA_AND_LOCATION = 0x0001;

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // Forward results to EasyPermissions
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @AfterPermissionGranted(RC_CAMERA_AND_LOCATION)
    private void methodRequiresTwoPermission() {
        String[] perms = {Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE};
        if (EasyPermissions.hasPermissions(this, perms)) {
            Toast.makeText(MainActivity.this, "有权限", Toast.LENGTH_SHORT).show();


            //选择性initTess
            long startTime = System.currentTimeMillis(); //起始时间

            initTess();

            long endTime = System.currentTimeMillis(); //结束时间
            long runTime = endTime - startTime;
            Log.i("test", String.format("初始化ocr 使用时间 %d ms", runTime));


        } else {
            // Do not have permissions, request them now
            EasyPermissions.requestPermissions(this, "我要权限",
                    RC_CAMERA_AND_LOCATION, perms);
        }
    }

    private void showProgress() {
        if (null != progressDialog) {
            progressDialog.show();
        } else {
            progressDialog = new ProgressDialog(this);
            progressDialog.setMessage("请稍候...");
            progressDialog.setIndeterminate(true);
            progressDialog.setCancelable(false);
            progressDialog.show();
        }
    }

    private void dismissProgress() {
        if (null != progressDialog) {
            progressDialog.dismiss();
        }
    }

    //上一张图片
    public void previous(View view) {
        tesstext.setText(null);
        runtime0.setText(null);
        runtime1.setText(null);
        runtime2.setText(null);
        runtime3.setText(null);
        runtime4.setText(null);
        index--;
        if (index < 0) {
            index = ids.length - 1;
        }
        idCard.setImageResource(ids[index]);
    }

    //下一张图片
    public void next(View view) {
        tesstext.setText(null);
        runtime0.setText(null);
        runtime1.setText(null);
        runtime2.setText(null);
        runtime3.setText(null);
        runtime4.setText(null);
        index++;
        if (index >= ids.length) {
            index = 0;
        }
        idCard.setImageResource(ids[index]);
    }


    /**
     * 将Bitmap转成本地图片
     * @param path 保存为本地图片的地址
     * @param bitmap 要转化的Bitmap
     */
    public static void saveImage(String path, Bitmap bitmap){
        try {
            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(path));
            bitmap.compress(Bitmap.CompressFormat.JPEG,80,bos);
            bos.flush();
            bos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * 将本地图片转成Bitmap
     * @param path 已有图片的路径
     * @return
     */
    public static Bitmap openImage(String path){
        Bitmap bitmap = null;
        try {
            BufferedInputStream bis = new BufferedInputStream(new FileInputStream(path));
            bitmap = BitmapFactory.decodeStream(bis);
            bis.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bitmap;
    }


    //识别过程 全部部署到服务器上
    public void rt0(View view) {
        //不需要initTess()
        final long startTime = System.currentTimeMillis(); //起始时间

        //图像识别主要调用区域
        //从原图Bitmap中查找，得到号码的Bitmap
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), ids[index]);

        saveImage("/sdcard/Pictures/bitmap.jpeg",bitmap);

        File file = new File("/sdcard/Pictures/bitmap.jpeg");

        AndroidNetworking.upload("http://127.0.0.1:5000/rt0")
                .addMultipartFile("image",file)
//                .addMultipartParameter("key","value")
                .setTag("uploadTest")
                .setPriority(Priority.HIGH)
                .build()
                .setUploadProgressListener(new UploadProgressListener() {
                    @Override
                    public void onProgress(long bytesUploaded, long totalBytes) {
                        // do anything with progress
                    }
                })
                .getAsJSONObject(new JSONObjectRequestListener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        // do anything with response
                        try{
                            progressDialog.dismiss();

                            System.out.println(response);
                            String str = response.getString("text");
                            System.out.println(str);
                            if(str == null){
                                Toast.makeText(MainActivity.this, "未识别到号码区域", Toast.LENGTH_SHORT).show();
                            }
                            else{
                                tesstext.setText("身份证号码：" + str);
                            }
                            long endTime = System.currentTimeMillis(); //结束时间
                            long runTime = endTime - startTime;
                            runtime0.setText(String.format("ABC识别使用时间 %d ms", runTime));
                        }catch (JSONException e){
                            e.printStackTrace();
                            Toast.makeText(MainActivity.this,"Error!",Toast.LENGTH_SHORT).show();
                        }
                    }
                    @Override
                    public void onError(ANError error) {
                        // handle error
                    }
                });

    }


    //识别过程 全部在本地
    public void rt1(View view) {
        //需要initTess()
        final long startTime = System.currentTimeMillis(); //起始时间

        //图像识别主要调用区域
        //从原图Bitmap中查找，得到号码的Bitmap
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), ids[index]);

//
//        // 灰度化
//        Bitmap bitmap1 = removeColor(bitmap, Bitmap.Config.ARGB_8888);
//
//        // 二值化
//        Bitmap bitmap2 = twoColor(bitmap1, Bitmap.Config.ARGB_8888);
//
//        // 膨胀处理
//        Bitmap bitmap3 = swellImg(bitmap2, Bitmap.Config.ARGB_8888);
//
//        // 轮廓监测
//        Bitmap bitmap4 = outSideImage(bitmap3, Bitmap.Config.ARGB_8888);

        // 图像切割
        Bitmap bitmap5 = cropImage(bitmap, Bitmap.Config.ARGB_8888);
        if (bitmap5 == null) {
            Toast.makeText(MainActivity.this, "未识别到号码区域", Toast.LENGTH_SHORT).show();
            return;
        }

        //OCR文字识别
        //14 用之前得先初始化
        //15 文字识别
        tessBaseApi.setImage(bitmap5);
        tesstext.setText("身份证号码：" + tessBaseApi.getUTF8Text());


        long endTime = System.currentTimeMillis(); //结束时间
        long runTime = endTime - startTime;
        runtime1.setText(String.format("abc识别使用时间 %d ms", runTime));
    }

    //识别过程 Abc
    public void rt2(View view) {
        //需要initTess()
        final long startTime = System.currentTimeMillis(); //起始时间

        //图像识别主要调用区域
        //从原图Bitmap中查找，得到号码的Bitmap
        final Bitmap bitmap = BitmapFactory.decodeResource(getResources(), ids[index]);

        saveImage("/sdcard/Pictures/bitmap.jpeg",bitmap);

        File file = new File("/sdcard/Pictures/bitmap.jpeg");
//        File fileResult = new File("/sdcard/Pictures/result.jpeg");


        //上传一张未处理图片
        AndroidNetworking.upload("http://127.0.0.1:5000/rt2")
                .addMultipartFile("image",file)
//                .addMultipartParameter("key","value")
                .setTag("uploadTest")
                .setPriority(Priority.HIGH)
                .build()
                .setUploadProgressListener(new UploadProgressListener() {
                    @Override
                    public void onProgress(long bytesUploaded, long totalBytes) {
                        // do anything with progress
                    }
                })
                .getAsJSONObject(new JSONObjectRequestListener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        // do anything with response
                        try{
                            progressDialog.dismiss();

                            //返回一个图片的下载url
//                            System.out.println(response);
                            String url = response.getString("url");
//                            String url = new String("http:\\127.0.0.1:5000\\static\\result.jpeg");
                            //下载图片
                            AndroidNetworking.download(url,"/sdcard/Pictures/","result.jpeg")
                                    .setTag("downloadImage")
                                    .setPriority(Priority.MEDIUM)
                                    .build()
                                    .setDownloadProgressListener(new DownloadProgressListener() {
                                        @Override
                                        public void onProgress(long bytesDownloaded, long totalBytes) {
                                            // do anything with progress
                                        }
                                    })
                                    .startDownload(new DownloadListener() {
                                        @Override
                                        public void onDownloadComplete() {
                                            // do anything after completion
//                                            File fileResult = new File("/sdcard/Pictures/result.jpeg");
                                            Toast.makeText(MainActivity.this,"Download Success!",Toast.LENGTH_SHORT).show();

                                            Bitmap bitmapResult = openImage("/sdcard/Pictures/result.jpeg");
                                            Bitmap bitmap5 = cutImage(bitmapResult,bitmap, Bitmap.Config.ARGB_8888);
//                                            Bitmap bitmap5 = cropImage(bitmap, Bitmap.Config.ARGB_8888);
                                            tessBaseApi.setImage(bitmap5);
                                            tesstext.setText("身份证号码：" + tessBaseApi.getUTF8Text());

//                                            idCard.setImageBitmap(bitmap5);
                                            long endTime = System.currentTimeMillis(); //结束时间
                                            long runTime = endTime - startTime;
                                            runtime2.setText(String.format("Abc识别使用时间 %d ms", runTime));
                                        }
                                        @Override
                                        public void onError(ANError error) {
                                            error.printStackTrace();
                                            Toast.makeText(MainActivity.this,"Download Error!",Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        }catch (JSONException e){
                            e.printStackTrace();
                            Toast.makeText(MainActivity.this,"Network Error!",Toast.LENGTH_SHORT).show();
                        }

                    }
                    @Override
                    public void onError(ANError error) {
                        // handle error
                    }
                });
    }

    //识别过程 ABc
    public void rt3(View view) {
        //需要initTess()
        final long startTime = System.currentTimeMillis(); //起始时间

        //图像识别主要调用区域
        //从原图Bitmap中查找，得到号码的Bitmap
        final Bitmap bitmap = BitmapFactory.decodeResource(getResources(), ids[index]);

        saveImage("/sdcard/Pictures/bitmap.jpeg",bitmap);

        File file = new File("/sdcard/Pictures/bitmap.jpeg");
//        File fileResult = new File("/sdcard/Pictures/result.jpeg");


        //上传一张未处理图片
        AndroidNetworking.upload("http://127.0.0.1:5000/rt3")
                .addMultipartFile("image",file)
//                .addMultipartParameter("key","value")
                .setTag("uploadTest")
                .setPriority(Priority.HIGH)
                .build()
                .setUploadProgressListener(new UploadProgressListener() {
                    @Override
                    public void onProgress(long bytesUploaded, long totalBytes) {
                        // do anything with progress
                    }
                })
                .getAsJSONObject(new JSONObjectRequestListener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        // do anything with response
                        try{
                            progressDialog.dismiss();

                            //返回一个图片的下载url
//                            System.out.println(response);
                            String url = response.getString("url");
//                            String url = new String("http:\\127.0.0.1:5000\\static\\result.jpeg");
                            //下载图片
                            AndroidNetworking.download(url,"/sdcard/Pictures/","result.jpeg")
                                    .setTag("downloadImage")
                                    .setPriority(Priority.MEDIUM)
                                    .build()
                                    .setDownloadProgressListener(new DownloadProgressListener() {
                                        @Override
                                        public void onProgress(long bytesDownloaded, long totalBytes) {
                                            // do anything with progress
                                        }
                                    })
                                    .startDownload(new DownloadListener() {
                                        @Override
                                        public void onDownloadComplete() {
                                            // do anything after completion
//                                            File fileResult = new File("/sdcard/Pictures/result.jpeg");
                                            Toast.makeText(MainActivity.this,"Download Success!",Toast.LENGTH_SHORT).show();

                                            Bitmap bitmapResult = openImage("/sdcard/Pictures/result.jpeg");
//                                            Bitmap bitmap5 = cutImage(bitmapResult,bitmap, Bitmap.Config.ARGB_8888);
//                                            Bitmap bitmap5 = cropImage(bitmap, Bitmap.Config.ARGB_8888);
                                            tessBaseApi.setImage(bitmapResult);
                                            tesstext.setText("身份证号码：" + tessBaseApi.getUTF8Text());

//                                            idCard.setImageBitmap(bitmap5);
                                            long endTime = System.currentTimeMillis(); //结束时间
                                            long runTime = endTime - startTime;
                                            runtime3.setText(String.format("ABc识别使用时间 %d ms", runTime));
                                        }
                                        @Override
                                        public void onError(ANError error) {
                                            error.printStackTrace();
                                            Toast.makeText(MainActivity.this,"Download Error!",Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        }catch (JSONException e){
                            e.printStackTrace();
                            Toast.makeText(MainActivity.this,"Network Error!",Toast.LENGTH_SHORT).show();
                        }

                    }
                    @Override
                    public void onError(ANError error) {
                        // handle error
                    }
                });
    }


    //识别过程 aBC
    public void rt4(View view) {
        //需要initTess()
        final long startTime = System.currentTimeMillis(); //起始时间

        //图像识别主要调用区域
        //从原图Bitmap中查找，得到号码的Bitmap
        final Bitmap bitmap = BitmapFactory.decodeResource(getResources(), ids[index]);

        Bitmap bitmap4 = cropImage(bitmap, Bitmap.Config.ARGB_8888);

        saveImage("/sdcard/Pictures/bitmap.jpeg",bitmap4);

        File file = new File("/sdcard/Pictures/bitmap.jpeg");
//        File fileResult = new File("/sdcard/Pictures/result.jpeg");


        //上传一张未处理图片
        AndroidNetworking.upload("http://127.0.0.1:5000/rt4")
                .addMultipartFile("image",file)
//                .addMultipartParameter("key","value")
                .setTag("uploadTest")
                .setPriority(Priority.HIGH)
                .build()
                .setUploadProgressListener(new UploadProgressListener() {
                    @Override
                    public void onProgress(long bytesUploaded, long totalBytes) {
                        // do anything with progress
                    }
                })
                .getAsJSONObject(new JSONObjectRequestListener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        // do anything with response
                        try{
                            progressDialog.dismiss();

                            System.out.println(response);
                            String str = response.getString("text");
                            System.out.println(str);
                            if(str == null){
                                Toast.makeText(MainActivity.this, "未识别到号码区域", Toast.LENGTH_SHORT).show();
                            }
                            else{
                                tesstext.setText("身份证号码：" + str);
                            }
                            long endTime = System.currentTimeMillis(); //结束时间
                            long runTime = endTime - startTime;
                            runtime4.setText(String.format("aBC识别使用时间 %d ms", runTime));
                        }catch (JSONException e){
                            e.printStackTrace();
                            Toast.makeText(MainActivity.this,"Error!",Toast.LENGTH_SHORT).show();
                        }
                    }
                    @Override
                    public void onError(ANError error) {
                        // handle error
                    }
                });
    }


    private native Bitmap cropImage(Bitmap bitmap4, Bitmap.Config argb8888);

    private native Bitmap outSideImage(Bitmap bitmap3, Bitmap.Config argb8888);

    private native Bitmap swellImg(Bitmap bitmap2, Bitmap.Config argb8888);

    private native Bitmap twoColor(Bitmap bitmap1, Bitmap.Config argb8888);

    private native Bitmap removeColor(Bitmap bitmap, Bitmap.Config argb8888);

    private native Bitmap cutImage(Bitmap bitmapResult, Bitmap bitmapOrigin, Bitmap.Config argb8888);

}