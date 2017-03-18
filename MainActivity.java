package com.example.assaf.david;


import android.Manifest;
import com.google.gson.Gson;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;

import android.util.Log;
import android.util.SparseArray;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;


public class MainActivity extends AppCompatActivity{

    private static final String RESULT_POSITIVE = "Success";
    private TextView barcodeInfo;
    private CameraSource cameraSource;
    private BarcodeDetector barcodeDetector;
    private SurfaceView cameraView;
    private String qrString;
    private Button sendButton;
    private EditText amountField;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();
        cameraView = (SurfaceView) findViewById(R.id.camera_view);
        barcodeInfo = (TextView) findViewById(R.id.qrResult);
        sendButton = (Button) findViewById(R.id.sendInputButton);
        amountField = (EditText) findViewById(R.id.amountField);
        cameraView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                try {
                    //If authorisation not granted for camera
                    if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
                        //ask for authorisation
                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA}, 50);
                    else {
                        //start your camera
                        cameraSource.start(cameraView.getHolder());
                    }
                } catch (IOException ie) {
                    Log.e("CAMERA SOURCE", ie.getMessage());
                } catch (RuntimeException e) {
                    Log.e(getString(R.string.app_name), "failed to open Camera");
                    e.printStackTrace();
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                cameraSource.stop();
            }
        });

        scanBarcodeFromCamera();
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Editable s = amountField.getText();
                String amount = s.toString();

                new RequestTask().execute(amount, qrString);
            }
        });
    }

    private void init() {
        barcodeDetector = new BarcodeDetector.Builder(this)
                .setBarcodeFormats(Barcode.QR_CODE)
                .build();

        cameraSource = new CameraSource
                .Builder(this, barcodeDetector)
                .setRequestedPreviewSize(640, 480)
                .build();

    }

    private void scanBarcodeFromCamera() {

        barcodeDetector.setProcessor(new Detector.Processor<Barcode>() {
            @Override
            public void release() {
            }

            @Override
            public void receiveDetections(Detector.Detections<Barcode> detections) {
                final SparseArray<Barcode> barcodes = detections.getDetectedItems();

                if (barcodes.size() > 0) {
//                    barcodeInfo.post(new Runnable() {    // Use the post method of the TextView
//                        public void run() {
//                            barcodeInfo.setText(    // Update the TextView
//                                    barcodes.valueAt(0).displayValue
//                            );
//                        }
//                    });
//                    cameraSource.stop();
//                    cameraSource.release();
//                    cameraSource = null;
//                    cameraView.getHolder().lockCanvas();
                    qrString = barcodes.valueAt(0).displayValue;
//                    refreshSendButton();
                }
            }
        });

    }

//    private void refreshSendButton() {
//        if (!TextUtils.isEmpty(qrString) && !TextUtils.isEmpty(amountField.getText().toString()))
//            sendButton.setClickable(true);
//        else
//        {
//            sendButton.setClickable(false);
//        }
//    }


    private JSONObject generateJson(String amount, String qrValue)
    {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("ReferentId","1");
            jsonObject.put("Amount",amount);
            jsonObject.put("QrString",qrValue);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Log.d("json", jsonObject.toString());
        return jsonObject;
    }

    class RequestTask extends AsyncTask<String, String, String> {
        String response;
        @Override
        protected String doInBackground(String... uri) {
            try {
                Log.d("server", "sending");
                JSONObject json = generateJson(uri[0], uri[1]);
//                String       postUrl       = "http://rands.co.il/alipay/checkPayment/getclientpay";// put in your url
//                Gson         gson          = new Gson();
//                HttpClient   httpClient    = HttpClientBuilder.create().build();
//                HttpPost     post          = new HttpPost(postUrl);
//                StringEntity postingString = new StringEntity(gson.toJson(pojo1));//gson.tojson() converts your pojo to json
//                post.setEntity(postingString);
//                post.setHeader("Content-type", "application/json");
//                HttpResponse  response = httpClient.execute(post);
                URL url = new URL("http://rands.co.il/alipay/checkPayment/getclientpay/" + json.toString());
                Log.d("url", url.toString());
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                if(urlConnection.getResponseCode() == HttpsURLConnection.HTTP_OK){
                    // Do normal input or output stream reading
                    response = urlConnection.getResponseMessage();
                }
                else {
                    response = "Error";
                }
                urlConnection.disconnect();

            }
            catch (MalformedURLException e) {
                Log.e("server", "error");
                e.printStackTrace();
            } catch (IOException e) {
                Log.e("server", "error" );
                e.printStackTrace();
            }

            return response;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            ImageView img = (ImageView) findViewById(R.id.ServerResultImg);
            JSONObject jsonObject = null;
            boolean isSuccess = false;
            try {
                Log.d("Result", result);
                jsonObject = new JSONObject(result);
                isSuccess = jsonObject.getInt("RepCode") == 0;
            }
            catch (JSONException e) {
                e.printStackTrace();
            }
            if (isSuccess) {
                int color = getResources().getColor(R.color.resultOK);
                img.setBackgroundColor(color);
            }
            else {
                int color = getResources().getColor(R.color.resultError);
                img.setBackgroundColor(color);
            }
        }
    }



}
