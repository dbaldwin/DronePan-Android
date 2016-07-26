package com.dronepan.AndroidApp;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.ActivityCompat;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import dji.sdk.Battery.DJIBattery;
import dji.sdk.Camera.DJICamera;
import dji.sdk.Camera.DJICameraSettingsDef;
import dji.sdk.FlightController.DJIFlightController;
import dji.sdk.Gimbal.DJIGimbal;
import dji.sdk.RemoteController.DJIRemoteController;
import dji.sdk.base.DJIBaseComponent;
import dji.sdk.base.DJIBaseProduct;
import dji.sdk.base.DJIError;

import dji.sdk.util.DJIParamCapability;
import timber.log.Timber;
public class MainViewController extends Activity implements View.OnClickListener, ConnectionController.ConnectionControllerInterface, PanoramaController.PanoramaControllerInterface, CameraController.CameraControllerInterface, BatteryController.BatteryControllerInterface {
    
    public static final String FLAG_CONNECTION_CHANGE = "dji_sdk_connection_change";

    private DJIBaseProduct mProduct;
    private ConnectionController connectionController = null;
    private PreviewController previewController = null;
    private BatteryController batteryController = null;
    private PanoramaController panoramaController = null;
    private CameraController cameraController = null;
    private FlightController flightController = null;

    private PanoramaController mPanoramaController = null;

    public static String DRONEPAN_ANDROID_VERSION = "0.1";


    public TextView mConnectStatusTextView;
    protected TextureView mVideoSurface = null;

    public Button mStartPanorama;
    public Button mSwapAEBMode;

    private Handler mHandler;

    // ON CREATE
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // SET CONTENT VIEW
        setContentView(R.layout.activity_dronepan);

        // LOOPER HANDLER
        mHandler = new Handler(Looper.getMainLooper());

        // CONNECTION CONTROLLER
        connectionController = new ConnectionController();
        connectionController.delegate = this;
        // STARTS CONNECTION CONTROLLER
        connectionController.start(this);

        // START UI ELEMENTS
        initUI();

        // PREVIEW CONTROLLER
        previewController = new PreviewController();
        // START WITH VIDEO SURFACE
        previewController.startWithContextSurface(this, mVideoSurface);

        // BATTERY CONTROLLER
        batteryController = new BatteryController();
        batteryController.delegate = this;

        // PANORAMA CONTROLLER
        panoramaController = new PanoramaController(this);
        panoramaController.delegate = this;


        // RECEIVE DEVICE CONNECTION CHANGES
        IntentFilter filter = new IntentFilter();
        filter.addAction(FLAG_CONNECTION_CHANGE);
        registerReceiver(mReceiver, filter);

    }

    protected BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Timber.d("BROADCAST RECEIVED");
            /*DJIBaseProduct product = connectionController.;
            if(product != null && product.getModel() != null) {
                showToast("CONNECTED TO ");
            }*/

            showToast("CONNECTION CHANGED");
        }

    };

    // START UI ELEMENTS
    private void initUI() {
        mConnectStatusTextView = (TextView) findViewById(R.id.ConnectStatusTextView);

        // VIDEO SURFACE
        mVideoSurface = (TextureView)findViewById(R.id.video_previewer_surface);

        // START PANORAMA BUTTON
        mStartPanorama = (Button) findViewById(R.id.btn_startpanorama);
        mStartPanorama.setOnClickListener(this);

        // START PANORAMA BUTTON
        mSwapAEBMode = (Button) findViewById(R.id.btn_settings);
        mSwapAEBMode.setOnClickListener(this);
    }

    // SHOW TOASTER
    public void showToast(final String msg) {
        runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(MainViewController.this, msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ON RESUME
    @Override public void onResume() {
        // INITIALIZE VIDEO CALL BACK ON RESUME
        previewController.initializeVideoCallback();

        super.onResume();


    }

    // ON PAUSE
    @Override public void onPause() {
        // REMOVE VIDEO CALLBACK ON PAUSE
        previewController.removeVideoCallback();

        super.onPause();
    }



    // ON RETURN
    public void onReturn(View view) {
        this.finish();

    }

    // ON DESTROY
    @Override public void onDestroy() {
        // REMOVE VIDEO CALLBACK ON DESTROY
        previewController.removeVideoCallback();
        unregisterReceiver(mReceiver);

        super.onDestroy();
    }

    //
    //  START PANORAMA
    //
    public void startPanorama() {

        // START PANORAMA
        panoramaController.start();

    }

    //
    // CONNECTION CONTROLLER INTERFACE
    //
    public void sdkRegistered() {
        Timber.d("SDK REGISTERED");
        showToast("SDK REGISTERED");
    }

    public void failedToRegister(String reason) {
        Timber.e("FAILED TO REGISTER SDK : %s", reason);
    }

    public void connectedToProduct(DJIBaseProduct product) {
        if(product != null && product.getModel() != null) {
            showToast("CONNECTED TO " + product.getModel().toString());
            mProduct = product;

            // SET CAMERA TO PHOTO MODE
            mProduct.getCamera().setCameraMode(
                    DJICameraSettingsDef.CameraMode.ShootPhoto,
                    new DJIBaseComponent.DJICompletionCallback() {
                        @Override
                        public void onResult(DJIError djiError) {

                        }
                    }
            );
        }
    }

    public void disconnected() {
        Timber.i("Disconnected");
        showToast("DISCONNECTED!");
    }

    public void connectedToBattery(DJIBattery battery) {
        Timber.d("Connected to Battery (%s)", battery.isConnected());
        showToast("CONNECTED TO BATTERY");
        batteryController.init(battery);
    }

    public void connectedToCamera(DJICamera camera) {
        if(camera != null) {
            showToast("CONNECTED TO CAMERA");

            // INIT CAMERA CONTROLLER
            cameraController = new CameraController();
            cameraController.init(camera);
            cameraController.delegate = this;

            // SET PREVIEW CONTROLLER CAMERA
            previewController.setCurrentCamera(camera);
        }
        else {
            cameraController = null;
        }
    }

    public void connectedToGimbal(@NonNull DJIGimbal gimbal) {
        Timber.d("connected to Gimbal (%s)", gimbal.isConnected());
        for (DJIGimbal.DJIGimbalCapabilityKey k : gimbal.gimbalCapability.keySet()) {
            DJIParamCapability v = gimbal.gimbalCapability.get(k);
            Timber.i("Gimbal Capability %s: %s", k.toString(), v.isSuppported());
        }
    }

    public void connectedToRemoteController(@NonNull DJIRemoteController rc) {
        Timber.i("connected to remote controller (%s)", rc.isConnected());
    }

    public void connectedToFlightController(@NonNull DJIFlightController fc) {
        Timber.i("connected to FlightController (%s)", fc.isConnected());
        if(flightController == null) {
            flightController = new FlightController();
            flightController.init(fc);
        }
    }

    public void disconnectedFromBattery() {
        Timber.d("disconnected from Battery");
    }

    public void disconnectedFromCamera() {
        Timber.d("disconnected from camera");
    }

    public void disconnectedFromGimbal() {
        Timber.d("disconnected from gimbal");
    }

    public void disconnectedFromRemote() {
        Timber.d("disconnected from remote");
    }

    public void disconnectedFromFlightController() {
        Timber.d("disconnected from flight controller");
    }

    //
    //  PANORAMA CONTROLLER INTERFACE
    //
    @Override
    public void takePicture() {
        cameraController.takePicture();



    }

    public void postUserMessage(String message) {
        showToast(message);
    }

    public void postUserWarning(String warning) {
        postUserMessage(warning);
    }

    public void panoStarting() {

    }

    public void panoStopping() {

    }

    public void gimbalAttitudeChanged(float pitch, float yaw, float roll) {

    }

    public void aircraftYawChanged(float yaw) {

    }

    public void aircraftSattelitesChanged(int count) {

    }

    public void aircraftDistanceChanged(float lat, float lng) {

    }

    public void aircraftAltitudeChanged(float altitude) {

    }

    public void panoCountChanged(int count, int total) {

    }

    public void panoAvailable(boolean available) {

    }

    //
    // BATTERY CONTROLLER INTERFACE
    //
    @Override
    public void batteryControllerPercentUpdated(int batteryPercent) {
        TextView batteryText = (TextView) findViewById(R.id.txt_battery);
        batteryText.setText("Battery : " + batteryPercent + "% |");
    }

    //
    //  CAMERA CONTROLLER INTERFACE
    //
    public void cameraTakePictureSuccess() {
        showToast("CAPTURED PHOTO");
    }

    public void cameraModeSwapAEB(boolean aebCapture) {
        if(aebCapture == true) {
            showToast("AEB MODE TURNED ON");
        }
        else {
            showToast("AEB MODE TURNED OFF");
        }
    }

    public void cameraControllerInError(String reason) {

    }

    public void cameraControllerOK(boolean fromError) {

    }

    public void cameraControllerNewMedia(String filename) {

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_startpanorama:{
                startPanorama();
                break;
            }

            case R.id.btn_cancelmission:{

                break;
            }

            case R.id.btn_settings:{
                cameraController.swapEABMode();
                break;
            }

            default:
                break;
        }
    }

}
