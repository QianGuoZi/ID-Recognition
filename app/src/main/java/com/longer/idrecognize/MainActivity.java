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

        //?????????????????????
        AndroidNetworking.initialize(getApplicationContext());


    }

    private native String stringFromJNI();


    //?????????OCR???????????????
    @SuppressLint("StaticFieldLeak")
    private void initTess() {
        //??????????????????????????? ?????????????????????
        this.asyncTask = new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... params) {
                //??????+????????? ???????????????tessdata??????
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
                    Toast.makeText(MainActivity.this, "?????????OCR??????", Toast.LENGTH_SHORT).show();
                } else {
                    finish();
                }
            }
        };
        asyncTask.execute();
    }


    //????????????????????????
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
            Toast.makeText(MainActivity.this, "?????????", Toast.LENGTH_SHORT).show();


            //?????????initTess
            long startTime = System.currentTimeMillis(); //????????????

            initTess();

            long endTime = System.currentTimeMillis(); //????????????
            long runTime = endTime - startTime;
            Log.i("test", String.format("?????????ocr ???????????? %d ms", runTime));


        } else {
            // Do not have permissions, request them now
            EasyPermissions.requestPermissions(this, "????????????",
                    RC_CAMERA_AND_LOCATION, perms);
        }
    }

    private void showProgress() {
        if (null != progressDialog) {
            progressDialog.show();
        } else {
            progressDialog = new ProgressDialog(this);
            progressDialog.setMessage("?????????...");
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

    //???????????????
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

    //???????????????
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
     * ???Bitmap??????????????????
     * @param path ??????????????????????????????
     * @param bitmap ????????????Bitmap
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
     * ?????????????????????Bitmap
     * @param path ?????????????????????
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


    //???????????? ???????????????????????????
    public void rt0(View view) {
        //?????????initTess()
        final long startTime = System.currentTimeMillis(); //????????????

        //??????????????????????????????
        //?????????Bitmap???????????????????????????Bitmap
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
                                Toast.makeText(MainActivity.this, "????????????????????????", Toast.LENGTH_SHORT).show();
                            }
                            else{
                                tesstext.setText("??????????????????" + str);
                            }
                            long endTime = System.currentTimeMillis(); //????????????
                            long runTime = endTime - startTime;
                            runtime0.setText(String.format("ABC?????????????????? %d ms", runTime));
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


    //???????????? ???????????????
    public void rt1(View view) {
        //??????initTess()
        final long startTime = System.currentTimeMillis(); //????????????

        //??????????????????????????????
        //?????????Bitmap???????????????????????????Bitmap
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), ids[index]);

//
//        // ?????????
//        Bitmap bitmap1 = removeColor(bitmap, Bitmap.Config.ARGB_8888);
//
//        // ?????????
//        Bitmap bitmap2 = twoColor(bitmap1, Bitmap.Config.ARGB_8888);
//
//        // ????????????
//        Bitmap bitmap3 = swellImg(bitmap2, Bitmap.Config.ARGB_8888);
//
//        // ????????????
//        Bitmap bitmap4 = outSideImage(bitmap3, Bitmap.Config.ARGB_8888);

        // ????????????
        Bitmap bitmap5 = cropImage(bitmap, Bitmap.Config.ARGB_8888);
        if (bitmap5 == null) {
            Toast.makeText(MainActivity.this, "????????????????????????", Toast.LENGTH_SHORT).show();
            return;
        }

        //OCR????????????
        //14 ????????????????????????
        //15 ????????????
        tessBaseApi.setImage(bitmap5);
        tesstext.setText("??????????????????" + tessBaseApi.getUTF8Text());


        long endTime = System.currentTimeMillis(); //????????????
        long runTime = endTime - startTime;
        runtime1.setText(String.format("abc?????????????????? %d ms", runTime));
    }

    //???????????? Abc
    public void rt2(View view) {
        //??????initTess()
        final long startTime = System.currentTimeMillis(); //????????????

        //??????????????????????????????
        //?????????Bitmap???????????????????????????Bitmap
        final Bitmap bitmap = BitmapFactory.decodeResource(getResources(), ids[index]);

        saveImage("/sdcard/Pictures/bitmap.jpeg",bitmap);

        File file = new File("/sdcard/Pictures/bitmap.jpeg");
//        File fileResult = new File("/sdcard/Pictures/result.jpeg");


        //???????????????????????????
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

                            //???????????????????????????url
//                            System.out.println(response);
                            String url = response.getString("url");
//                            String url = new String("http:\\127.0.0.1:5000\\static\\result.jpeg");
                            //????????????
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
                                            tesstext.setText("??????????????????" + tessBaseApi.getUTF8Text());

//                                            idCard.setImageBitmap(bitmap5);
                                            long endTime = System.currentTimeMillis(); //????????????
                                            long runTime = endTime - startTime;
                                            runtime2.setText(String.format("Abc?????????????????? %d ms", runTime));
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

    //???????????? ABc
    public void rt3(View view) {
        //??????initTess()
        final long startTime = System.currentTimeMillis(); //????????????

        //??????????????????????????????
        //?????????Bitmap???????????????????????????Bitmap
        final Bitmap bitmap = BitmapFactory.decodeResource(getResources(), ids[index]);

        saveImage("/sdcard/Pictures/bitmap.jpeg",bitmap);

        File file = new File("/sdcard/Pictures/bitmap.jpeg");
//        File fileResult = new File("/sdcard/Pictures/result.jpeg");


        //???????????????????????????
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

                            //???????????????????????????url
//                            System.out.println(response);
                            String url = response.getString("url");
//                            String url = new String("http:\\127.0.0.1:5000\\static\\result.jpeg");
                            //????????????
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
                                            tesstext.setText("??????????????????" + tessBaseApi.getUTF8Text());

//                                            idCard.setImageBitmap(bitmap5);
                                            long endTime = System.currentTimeMillis(); //????????????
                                            long runTime = endTime - startTime;
                                            runtime3.setText(String.format("ABc?????????????????? %d ms", runTime));
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


    //???????????? aBC
    public void rt4(View view) {
        //??????initTess()
        final long startTime = System.currentTimeMillis(); //????????????

        //??????????????????????????????
        //?????????Bitmap???????????????????????????Bitmap
        final Bitmap bitmap = BitmapFactory.decodeResource(getResources(), ids[index]);

        Bitmap bitmap4 = cropImage(bitmap, Bitmap.Config.ARGB_8888);

        saveImage("/sdcard/Pictures/bitmap.jpeg",bitmap4);

        File file = new File("/sdcard/Pictures/bitmap.jpeg");
//        File fileResult = new File("/sdcard/Pictures/result.jpeg");


        //???????????????????????????
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
                                Toast.makeText(MainActivity.this, "????????????????????????", Toast.LENGTH_SHORT).show();
                            }
                            else{
                                tesstext.setText("??????????????????" + str);
                            }
                            long endTime = System.currentTimeMillis(); //????????????
                            long runTime = endTime - startTime;
                            runtime4.setText(String.format("aBC?????????????????? %d ms", runTime));
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