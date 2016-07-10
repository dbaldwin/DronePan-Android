package com.dronepan.AndroidApp;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Looper;

import dji.sdk.Battery.DJIBattery;
import dji.sdk.Camera.DJICamera;
import dji.sdk.FlightController.DJIFlightController;
import dji.sdk.Gimbal.DJIGimbal;
import dji.sdk.RemoteController.DJIRemoteController;
import dji.sdk.SDKManager.DJISDKManager;
import dji.sdk.base.DJIBaseComponent;
import dji.sdk.base.DJIBaseProduct;
import dji.sdk.base.DJIError;
import dji.sdk.base.DJISDKError;
import timber.log.Timber;

public class ConnectionController {

    private static DJIBaseProduct mProduct;
    protected String model = "";

    private Handler mHandler;

    private MainViewController viewController = null;

    public enum ProductType {
        Aircraft,
        Handheld,
        Unknown
    }

    // INTERFACE METHODS
    interface ConnectionControllerInterface {
        void sdkRegistered();
        void failedToRegister(String reason);
        void connectedToProduct(DJIBaseProduct product);
        void disconnected();
        void connectedToBattery(DJIBattery battery);
        void connectedToCamera(DJICamera camera);
        void connectedToGimbal(DJIGimbal gimbal);
        void connectedToRemoteController(DJIRemoteController rc);
        void connectedToFlightController(DJIFlightController flightController);
        void disconnectedFromBattery();
        void disconnectedFromCamera();
        void disconnectedFromGimbal();
        void disconnectedFromRemote();
        void disconnectedFromFlightController();
    }

    public ConnectionControllerInterface delegate = null;


    public void start(MainViewController ctx) {
        mHandler = new Handler(Looper.getMainLooper());

        viewController = ctx;

        Timber.d("STARTING SDK MANAGER");
        // INIT DJI SDK MANAGER
        DJISDKManager.getInstance().initSDKManager(ctx, mDJISDKMangerCallback);
    }

    // DJI SDK MANAGER CALLBACK
    private DJISDKManager.DJISDKManagerCallback mDJISDKMangerCallback = new DJISDKManager.DJISDKManagerCallback() {
        @Override public void onGetRegisteredResult(DJIError error) {
            Timber.d("SDK Manger Registered Result  err:%s",error.getDescription());

            if(error == DJISDKError.REGISTRATION_SUCCESS) {
                // START CONNECTION TO PRODUCT
                DJISDKManager.getInstance().startConnectionToProduct();

                Handler handler = new Handler(Looper.getMainLooper());
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        Timber.d("SUCCESS REGISTRATION SDK");
                        delegate.sdkRegistered();

                    }
                });
            }
            else {
                Handler handler = new Handler(Looper.getMainLooper());
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        Timber.d("ERROR REGISTRATION SDK");
                    }
                });
            }
        }

        // ON PRODUCT CHANGED
        @Override public void onProductChanged(DJIBaseProduct oldProduct, DJIBaseProduct newProduct) {
            mProduct = newProduct;

            if(mProduct != null) {
                mProduct.setDJIBaseProductListener(mDJIBaseProductListener);

                // CALLS INTERFACE CALLBACK
                delegate.connectedToProduct(mProduct);
                delegate.connectedToBattery(mProduct.getBattery());
                delegate.connectedToCamera(mProduct.getCamera());
            }
        }
    };

    // PRODUCT LISTENER
    private DJIBaseProduct.DJIBaseProductListener mDJIBaseProductListener = new DJIBaseProduct.DJIBaseProductListener() {
        @Override public void onComponentChange(DJIBaseProduct.DJIComponentKey key, DJIBaseComponent oldComponent , DJIBaseComponent newComponent) {
            // SWITCH COMPONENT KEYS

            switch (key) {
                case Camera:
                    // CALLS INTERFACE CALLBACK
                        DJICamera camera = (DJICamera)newComponent;
                        delegate.connectedToCamera(camera);
                    break;
                case Gimbal:
                    // CALLS INTERFACE CALLBACK
                    DJIGimbal gimbal = (DJIGimbal)newComponent;
                    delegate.connectedToGimbal(gimbal);
                    break;
                case RemoteController:
                    // CALLS INTERFACE CALLBACK
                    DJIRemoteController rc = (DJIRemoteController)newComponent;
                    delegate.connectedToRemoteController(rc);
                    break;
                case FlightController:
                    // CALLS INTERFACE CALLBACK
                    DJIFlightController fc = (DJIFlightController)newComponent;
                    delegate.connectedToFlightController(fc);
                    break;

            }
            if(newComponent != null) {
                newComponent.setDJIComponentListener(mDJIComponentListener);
            }

            notifyStatusChange();
        }

        @Override public void onProductConnectivityChanged(boolean isConnected) {
            notifyStatusChange();
        }
    };

    // COMPONENT LISTENER
    private DJIBaseComponent.DJIComponentListener mDJIComponentListener = new DJIBaseComponent.DJIComponentListener() {
        @Override public void onComponentConnectivityChanged(boolean isConnected) {
            notifyStatusChange();
        }
    };

    private void notifyStatusChange() {
        mHandler.removeCallbacks(updateRunnable);
        mHandler.postDelayed(updateRunnable, 500);
    }

    private Runnable updateRunnable = new Runnable() {

        @Override
        public void run() {
            Intent intent = new Intent(MainViewController.FLAG_CONNECTION_CHANGE);
            viewController.sendBroadcast(intent);
        }
    };
}
