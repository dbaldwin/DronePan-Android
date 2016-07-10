package com.dronepan.AndroidApp;

import android.os.Handler;
import java.util.LinkedList;

import dji.sdk.Gimbal.DJIGimbal;
import dji.sdk.MissionManager.DJICustomMission;
import dji.sdk.MissionManager.DJIMission;
import dji.sdk.MissionManager.DJIMissionManager;
import dji.sdk.MissionManager.MissionStep.DJIGimbalAttitudeStep;
import dji.sdk.MissionManager.MissionStep.DJIMissionStep;
import dji.sdk.MissionManager.MissionStep.DJIShootPhotoStep;
import dji.sdk.MissionManager.MissionStep.DJIAircraftYawStep;
import dji.sdk.base.DJIBaseComponent;
import dji.sdk.base.DJIError;
import timber.log.Timber;

public class PanoramaController {

    interface PanoramaControllerInterface {
        void postUserMessage(String message);
        void postUserWarning(String warning);
        void panoStarting();
        void panoStopping();
        void gimbalAttitudeChanged(float pitch, float yaw, float roll);
        void aircraftYawChanged(float yaw);
        void aircraftSattelitesChanged(int count);
        void aircraftDistanceChanged(float lat, float lng);
        void aircraftAltitudeChanged(float altitude);
        void panoCountChanged(int count, int total);
        void panoAvailable(boolean available);
        void takePicture();

    }

    public PanoramaControllerInterface delegate = null;

    private MainViewController mContext = null;

    private DJIMission mDJIMission;

    private float lastGimbalPtich = 0.0f;
    private float lastGimbalYaw = 0.0f;
    private float lastGimbalRoll = 0.0f;
    private float lastACYaw = 0.0f;

    private Handler mHandler = new Handler();

    private boolean isRunningState = false;
    private boolean isRunningOK = false;

    private float currentGimbalRotation = 0f;

    private LinkedList<DJIMission> curretMissions = null;

    public PanoramaController(MainViewController ctx) {
        mContext = ctx;
    }

    // START PANORAMA
    public void start() {
        Timber.i("Starting Pano");
        if (!checkProduct()) {
            return;
        }

        if (!checkCamera()) {
            return;
        }

        if (!checkSpace()) {
            return;
        }

        if (!checkGimbal()) {
            return;
        }

        if (!checkFC()) {
            return;
        }

        if (!checkRemote()) {
            return;
        }

        if (!checkRCMode()) {
            return;
        }

        // PANORAMA STARTING
        doPanoLoop();

    }

    // DO PANO LOOP
    protected void doPanoLoop() {
        Timber.i("Checks passed - starting panoloop");
        /*int currentCount = 0;

        DJIMissionManager missionManager = DJIMissionManager.getInstance();

        if (missionManager == null) {
            Timber.e("MissionManager instance is null");
            delegate.postUserMessage("Error: Could not get mission manager instance");
            return;
        }

        mDJIMission = createCustomMission();
        if (mDJIMission == null) {
            Timber.e("Created null mission");
            delegate.postUserMessage("Please choose a mission type");
            //Utils.setResultToToast(mContext, "Please choose a mission type...");
        }

        missionManager.prepareMission(mDJIMission, new DJIMission.DJIMissionProgressHandler() {

            @Override
            public void onProgress(DJIMission.DJIProgressType progressType, float missionProgress) {
                //setProgressBar((int)(progress * 100f));
                Timber.i("Mission Progress (%s) (%d)", progressType.toString(), missionProgress);
            }

        }, new DJIBaseComponent.DJICompletionCallback() {


            @Override
            public void onResult(DJIError error) {
                if (error == null) {
                    //Utils.setResultToToast(mContext, "Success!");
                    Timber.i("Mission prepared");
                    delegate.postUserMessage("Success preparing mission");

                    startMission();
                } else {
                    Timber.e("Mission failed to prepare: %s", error.getDescription());
                    delegate.postUserMessage("Error preparing mission " + error.getDescription());
                }
            }
        });*/

        delegate.postUserMessage("STARTING PANORAMA");

       curretMissions = new LinkedList<DJIMission>();

        createColumnMission();
        createColumnMission();
        createColumnMission();
        createColumnMission();
        createColumnMission();
        createColumnMission();

        // LAST DOWN SHOT
        currentGimbalRotation = -90;
        curretMissions.add(createGimbalMission());

        executeNextMission();
    }

    protected void createColumnMission() {
        currentGimbalRotation = 0;
        curretMissions.add(createGimbalMission());
        currentGimbalRotation -= 30;
        curretMissions.add(createGimbalMission());
        currentGimbalRotation -= 30;
        curretMissions.add(createGimbalMission());

        curretMissions.add(createYawMission());

    }

    protected void executeNextMission() {

            mHandler.postDelayed(new Runnable() {
                public void run() {
                    if (!curretMissions.isEmpty()) {
                        DJIMission nextMission = curretMissions.pop();
                        executeMission(nextMission);
                    }
                }
            }, 5000);


    }

    protected void executeMission(DJIMission mission) {
        int currentCount = 0;

        DJIMissionManager missionManager = DJIMissionManager.getInstance();

        if (missionManager == null) {
            delegate.postUserMessage("Error: Could not get mission manager instance");
            return;
        }

        mDJIMission = mission;
        if (mDJIMission == null) {
            delegate.postUserMessage("Please choose a mission type");
            //Utils.setResultToToast(mContext, "Please choose a mission type...");
        }

        missionManager.prepareMission(mDJIMission, new DJIMission.DJIMissionProgressHandler() {

            @Override
            public void onProgress(DJIMission.DJIProgressType type, float progress) {
                //setProgressBar((int)(progress * 100f));
            }

        }, new DJIBaseComponent.DJICompletionCallback() {


            @Override
            public void onResult(DJIError error) {
                if (error == null) {
                    //delegate.postUserMessage("PREPARING MISSION SUCCESS!");

                    startMission();
                } else {
                    delegate.postUserMessage("ERROR PREPARING MISSION " + error.getDescription());
                }
            }
        });
    }

    protected void startMission() {
        Timber.i("Starting mission");
        //delegate.postUserMessage("STARTING MISSION");

        DJIMissionManager missionManager = DJIMissionManager.getInstance();

        if (mDJIMission != null) {
            missionManager.setMissionExecutionFinishedCallback(new DJIBaseComponent.DJICompletionCallback() {

                @Override
                public void onResult(DJIError error) {
                    if(error == null) {
                        //delegate.postUserMessage("Mission executing OK");
                        Timber.i("Mission Execution finished without error.");

                        if(curretMissions.isEmpty()) {
                            delegate.postUserMessage("PANORAMA CAPTURED OK");
                        }
                        else {
                            // EXECUTE NEXT MISSION
                            executeNextMission();
                        }
                    } else {
                        Timber.i("Mission Execution finished with error :%s", error.getDescription());
                    }

                }
            });

            missionManager.startMissionExecution(new DJIBaseComponent.DJICompletionCallback()

            {

                @Override
                public void onResult(DJIError mError) {

                    if (mError == null) {
                        Timber.i("Mission execution success!");
                        //delegate.postUserMessage("SUCCESS EXECUTING MISSION");
                    } else {
                        Timber.e("Error in mission execution: %s", mError.getDescription());
                        delegate.postUserMessage("Error mission execution: " + mError.getDescription());
                    }
                }
            });
        }
    }

    protected DJICustomMission createGimbalMission() {
        LinkedList<DJIMissionStep> steps = new LinkedList<DJIMissionStep>();

        // RESET GIMBAL STATE
        steps.add(new DJIGimbalAttitudeStep(
                DJIGimbal.DJIGimbalRotateAngleMode.AbsoluteAngle,
                new DJIGimbal.DJIGimbalAngleRotation(true, currentGimbalRotation, DJIGimbal.DJIGimbalRotateDirection.Clockwise),
                null,
                null,
                new DJIBaseComponent.DJICompletionCallback() {
                    @Override
                    public void onResult(DJIError error) {
                        if (error != null) {
                            delegate.postUserMessage("RESET GIMBAL ERROR: " + error.getDescription());
                        } else {

                            // TAKE PICTURE
                            delegate.takePicture();
                        }
                        //delegate.postUserMessage("RESET GIMBAL: " + DJIGimbal.DJIGimbalRotateDirection.Clockwise + " " + (error == null ? "Success" : error.getDescription()));
                    }

                }));

        DJICustomMission customMission = new DJICustomMission(steps);
        return customMission;
    }

    protected DJICustomMission createYawMission() {
        LinkedList<DJIMissionStep> steps = new LinkedList<DJIMissionStep>();

        steps.add(new DJIAircraftYawStep(60.0f, 18.0f, new DJIBaseComponent.DJICompletionCallback() {
            @Override
            public void onResult(DJIError error) {
                //delegate.postUserMessage("AIRCRAFT YAW STEP");
            }
        }));

        DJICustomMission customMission = new DJICustomMission(steps);
        return customMission;
    }

    /*protected DJICustomMission createCustomMission1() {
        // CREATE STEP LIST
        LinkedList<DJIMissionStep> steps = new LinkedList<DJIMissionStep>();

        // CREATE 6 STEPS
        createRowStep(steps);
        createRowStep(steps);
        createRowStep(steps);
        createRowStep(steps);
        createRowStep(steps);
        createRowStep(steps);

        // SET GIMBAL STATE
        steps.add(new DJIGimbalAttitudeStep(
                DJIGimbal.DJIGimbalRotateAngleMode.AbsoluteAngle,
                new DJIGimbal.DJIGimbalAngleRotation(true, -90f, DJIGimbal.DJIGimbalRotateDirection.Clockwise),
                null,
                null,
                new DJIBaseComponent.DJICompletionCallback() {
                    @Override
                    public void onResult(DJIError error) {
                        //delegate.postUserMessage("RESET GIMBAL: " + DJIGimbal.DJIGimbalRotateDirection.Clockwise + " " + (error == null ? "Success" : error.getDescription()));
                        Timber.d("Finished setting gimbal attitude. (%s)", (error == null ? "success" : error.getDescription()));
                    }

                }));

        // SHOOT PHOTO
        steps.add(new DJIShootPhotoStep(new DJIBaseComponent.DJICompletionCallback() {
            @Override
            public void onResult(DJIError error) {
                //Utils.setResultToToast(mContext, "Take single photo step: " + (error == null ? "Success" : error.getDescription()));
                Timber.d("Last photo completed.");
                delegate.postUserMessage("PANORAMA SUCCESS");
            }
        }));

        return new DJICustomMission(steps);
    }

    protected void createRowStep(LinkedList<DJIMissionStep> steps) {
        float currentGimbalAngle = 0.0f;

        // RESET GIMBAL STATE
        steps.add(new DJIGimbalAttitudeStep(
                DJIGimbal.DJIGimbalRotateAngleMode.AbsoluteAngle,
                new DJIGimbal.DJIGimbalAngleRotation(true, currentGimbalAngle, DJIGimbal.DJIGimbalRotateDirection.Clockwise),
                null,
                null,
                new DJIBaseComponent.DJICompletionCallback() {
                    @Override
                    public void onResult(DJIError error) {
                        Timber.d("RESET GIMBAL: " + DJIGimbal.DJIGimbalRotateDirection.Clockwise + " " + (error == null ? "Success" : error.getDescription()));
                    }

                }));

        // SHOOT PHOTO
        steps.add(new DJIShootPhotoStep(new DJIBaseComponent.DJICompletionCallback() {
            @Override
            public void onResult(DJIError error) {
                Timber.d("Take single photo step: " + (error == null ? "Success" : error.getDescription()));
                //delegate.postUserMessage("SHOOT PHOTO STEP");
            }
        }));

        currentGimbalAngle -= 30.0f;
        // -30

        // SET GIMBAL STATE
        steps.add(new DJIGimbalAttitudeStep(
                DJIGimbal.DJIGimbalRotateAngleMode.AbsoluteAngle,
                new DJIGimbal.DJIGimbalAngleRotation(true, currentGimbalAngle, DJIGimbal.DJIGimbalRotateDirection.Clockwise),
                null,
                null,
                new DJIBaseComponent.DJICompletionCallback() {
                    @Override
                    public void onResult(DJIError error) {
                        Timber.d("RESET GIMBAL: " + DJIGimbal.DJIGimbalRotateDirection.Clockwise + " " + (error == null ? "Success" : error.getDescription()));
                    }

                }));

        // SHOOT PHOTO
        steps.add(new DJIShootPhotoStep(new DJIBaseComponent.DJICompletionCallback() {
            @Override
            public void onResult(DJIError error) {
                Timber.d("Take single photo step: " + (error == null ? "Success" : error.getDescription()));
                //delegate.postUserMessage("SHOOT PHOTO STEP");
            }
        }));

        currentGimbalAngle -= 30.0f;
        // -60

        // SET GIMBAL STATE
        steps.add(new DJIGimbalAttitudeStep(
                DJIGimbal.DJIGimbalRotateAngleMode.AbsoluteAngle,
                new DJIGimbal.DJIGimbalAngleRotation(true, currentGimbalAngle, DJIGimbal.DJIGimbalRotateDirection.Clockwise),
                null,
                null,
                new DJIBaseComponent.DJICompletionCallback() {
                    @Override
                    public void onResult(DJIError error) {
                        Timber.d("RESET GIMBAL: " + DJIGimbal.DJIGimbalRotateDirection.Clockwise + " " + (error == null ? "Success" : error.getDescription()));
                    }

                }));

        // SHOOT PHOTO
        steps.add(new DJIShootPhotoStep(new DJIBaseComponent.DJICompletionCallback() {
            @Override
            public void onResult(DJIError error) {
                Timber.d("Take single photo step: " + (error == null ? "Success" : error.getDescription()));
                delegate.postUserMessage("LAST PANORAMA SHOOT PHOTO STEP");
            }
        }));

        // END ROW

        // YAW AIRCRAFT
        steps.add(new DJIAircraftYawStep(60.0f, 10.0f, new DJIBaseComponent.DJICompletionCallback() {
            @Override
            public void onResult(DJIError error) {
                Timber.d("AIRCRAFT YAW STEP");
            }
        }));

    }

    public void buildMissionSteps() {

    }*/

    public boolean checkProduct() {

        return true;
    }

    public boolean checkCamera() {

        return true;
    }

    public boolean checkSpace() {

        return true;
    }

    public boolean checkGimbal() {

        return true;
    }

    public boolean checkFC() {

        return true;
    }

    public boolean checkRemote() {

        return true;
    }

    public boolean checkRCMode() {

        return true;
    }


    // CAPTURE PHOTO
    public void capturePhoto() {

        // SET CAMERA MODE TO SHOOT PHOTO
        //mDJIController.switchCameraMode(DJICameraSettingsDef.CameraMode.ShootPhoto);

        // CAPTURE PHOTO
        //mDJIController.capturePhoto();

    }

    // UPDATE VISUAL DEBUG DATA
    public void updateVisualDebugData() {

        /*if(mDJIController.isAircrafConnected()) {
            mMainActivity.setTitleBar(mDJIController.getAircraftModel() + " connected!");
        }
        else {
            mMainActivity.setTitleBar("No aircraft connected!");
        }*/

    }

}
