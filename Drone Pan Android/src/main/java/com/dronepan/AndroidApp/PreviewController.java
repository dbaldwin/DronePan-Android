package com.dronepan.AndroidApp;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.view.TextureView;

import dji.sdk.Camera.DJICamera;
import dji.sdk.Codec.DJICodecManager;
import timber.log.Timber;

public class PreviewController implements TextureView.SurfaceTextureListener {

    protected TextureView mVideoSurface = null;
    protected Context mainCtx = null;

    private DJICodecManager mCodecManager = null;
    private DJICamera mCurrentCamera = null;

    protected DJICamera.CameraReceivedVideoDataCallback mReceivedVideoDataCallBack = null;

    public PreviewController() {

    }

    public void startWithContextSurface(Context ctx, TextureView texture) {
        mainCtx = ctx;

        // RECEIVED VIDEO DATA CALLBACK
        mReceivedVideoDataCallBack = new DJICamera.CameraReceivedVideoDataCallback() {
            @Override public void onResult(byte[] videoBuffer, int size) {
                if(mCodecManager != null) {
                    mCodecManager.sendDataToDecoder(videoBuffer, size);
                }
                else {
                    Timber.e("ERROR CREATING CODEC MANAGER");
                }
            }
        };

        mVideoSurface = texture;
        mVideoSurface.setSurfaceTextureListener(this);
    }

    //
    //  SURFACE TEXTURE EVENTS
    @Override public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        // CREATE CODEC ON SURFACE
        if(mCodecManager == null) {
            mCodecManager = new DJICodecManager(mainCtx, surface, width, height);
        }
    }

    @Override public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

    }

    @Override public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }

    @Override public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        if(mCodecManager != null) {
            mCodecManager.cleanSurface();
            mCodecManager = null;
        }

        return false;
    }

    // SET CURRENT PREVIEW CAMERA
    public void setCurrentCamera(DJICamera camera) {
        mCurrentCamera = camera;
    }

    // SET VIDEO SURFACE
    public void initializeVideoCallback() {
        if (mCurrentCamera != null){
            // SET DJI CAMERA VIDEO DATA CALLBACK
            mCurrentCamera.setDJICameraReceivedVideoDataCallback(mReceivedVideoDataCallBack);
        }
    }

    // UNSET VIDEO SURFACE
    public void removeVideoCallback() {
        if (mCurrentCamera != null){
            // SET DJI CAMERA VIDEO DATA CALLBACK
            mCurrentCamera.setDJICameraReceivedVideoDataCallback(null);
        }
    }
}
