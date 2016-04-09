package example.chatea.servicecamera;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Bundle;

import android.os.Handler;
import android.os.IBinder;
import android.os.ResultReceiver;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.List;

public class CameraService extends Service {
    private static final String TAG = CameraService.class.getSimpleName();

    private static int lastState = TelephonyManager.CALL_STATE_IDLE;
    public static final String PHONECALL_RECEIVER = "resultReceiver";
    public static final String RESULT_RECEIVER = "resultReceiver";
    public static final String VIDEO_PATH = "recordedVideoPath";

    public static final int RECORD_RESULT_OK = 0;
    public static final int RECORD_RESULT_DEVICE_NO_CAMERA= 1;
    public static final int RECORD_RESULT_GET_CAMERA_FAILED = 2;
    public static final int RECORD_RESULT_ALREADY_RECORDING = 3;
    public static final int RECORD_RESULT_NOT_RECORDING = 4;

    private static final String START_SERVICE_COMMAND = "startServiceCommands";
    private static final int COMMAND_NONE = -1;
    private static final int COMMAND_START_RECORDING = 0;
    private static final int COMMAND_STOP_RECORDING = 1;

    private Camera mCamera;
    private MediaRecorder mMediaRecorder;

    private boolean mRecording = false;
    private String mRecordingPath = null;

    public CameraService() {
    }

    public static void startToStartRecording(Context context, ResultReceiver resultReceiver) {
        Intent intent = new Intent(context, CameraService.class);
        intent.putExtra(START_SERVICE_COMMAND, COMMAND_START_RECORDING);
        intent.putExtra(RESULT_RECEIVER, resultReceiver);
        context.startService(intent);
    }

    public static void startToStopRecording(Context context, ResultReceiver resultReceiver) {
        Intent intent = new Intent(context, CameraService.class);
        intent.putExtra(START_SERVICE_COMMAND, COMMAND_STOP_RECORDING);
        intent.putExtra(RESULT_RECEIVER, resultReceiver);
        context.startService(intent);
    }

//    private void tryToRecording() {
//        if (mRecording) {
//            Toast.makeText(this, "Already recording...", Toast.LENGTH_SHORT).show();
//            return;
//        }
//
//        startRecording();
//    }
//
//    private void startRecording() {
//        setRecording(true);
//
//        ResultReceiver receiver = new ResultReceiver(new Handler()) {
//            @Override
//            protected void onReceiveResult(int resultCode, Bundle resultData) {
//                handleStartRecordingResult(resultCode, resultData);
//            }
//        };
//
//        CameraService.startToStartRecording(this, receiver);
//    }
//
//
//    private void stopRecording() {
//        setRecording(false);
//
//        ResultReceiver receiver = new ResultReceiver(new Handler()) {
//            @Override
//            protected void onReceiveResult(int resultCode, Bundle resultData) {
//                handleStopRecordingResult(resultCode, resultData);
//            }
//        };
//
//        CameraService.startToStopRecording(this, receiver);
//    }
//
//    private void handleStartRecordingResult(int resultCode, Bundle resultData) {
//        if (resultCode == CameraService.RECORD_RESULT_OK) {
//            Toast.makeText(this, "Start recording...", Toast.LENGTH_SHORT).show();
//        } else {
//            // start recording failed.
//            Toast.makeText(this, "Start recording failed...", Toast.LENGTH_SHORT).show();
//            setRecording(false);
//        }
//    }
//
//    private void handleStopRecordingResult(int resultCode, Bundle resultData) {
//
//        Intent intent = new Intent(this, CameraService.class);
//        ResultReceiver receiver = new ResultReceiver(new Handler()) {
//            @Override
//            protected void onReceiveResult(int resultCode, Bundle resultData) {
//                handleRecordingResult(resultCode, resultData);
//            }
//        };
//        intent.putExtra(CameraService.RESULT_RECEIVER, receiver);
//        startService(intent);
//
//        Toast.makeText(this, "Start recording...", Toast.LENGTH_SHORT).show();
//    }
//
//    private void handleRecordingResult(int resultCode, Bundle resultData) {
//        setRecording(false);
//
//        if (resultCode == CameraService.RECORD_RESULT_OK) {
//            String videoPath = resultData.getString(CameraService.VIDEO_PATH);
//            Toast.makeText(this, "Record succeed, file saved in " + videoPath,
//                    Toast.LENGTH_LONG).show();
//        } else {
//
//            Toast.makeText(this, "Record failed...", Toast.LENGTH_SHORT).show();
//
//        }
//    }


    /**
     * Used to take picture.
     */
    private Camera.PictureCallback mPicture = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            File pictureFile = Util.getOutputMediaFile(Util.MEDIA_TYPE_IMAGE);

            if (pictureFile == null) {
                return;
            }

            try {
                FileOutputStream fos = new FileOutputStream(pictureFile);
                fos.write(data);
                fos.close();
            } catch (FileNotFoundException e) {
            } catch (IOException e) {
            }
        }
    };

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        switch (intent.getIntExtra(START_SERVICE_COMMAND, COMMAND_NONE)) {
            //TODO Sometime will crash when answer the call (intent.getIntExtra will be null)
            case COMMAND_START_RECORDING:
                handleStartRecordingCommand(intent);
                break;
            case COMMAND_STOP_RECORDING:
                handleStopRecordingCommand(intent);
                break;
            default: //TODO New Case for PhoneCallReceiver
                throw new UnsupportedOperationException("Cannot start service with illegal commands");
        }

        return START_STICKY;
    }

    private void setRecording(boolean recording) {
        mRecording = recording;
    }


    public static void handlePhoneCallStatus(Intent intent) {
        //PhoneCallReceiver Listener to start/stop recording.
        int phoneCurrentStatus = intent.getIntExtra("PHONE_CURRENT_STATUS",0);
        int phoneLastStatus = intent.getIntExtra("PHONE_LAST_STATUS",0);
        switch (phoneCurrentStatus) {
            case -1: // Calling Status
//                tryToRecording();
                Log.d(TAG, "CALL_STATE_CALLING");
                break;
            case TelephonyManager.CALL_STATE_RINGING:
//                tryToRecording();
                Log.d(TAG, "CALL_STATE_RINGING");
                break;
            case TelephonyManager.CALL_STATE_OFFHOOK:
                //Transition of ringing->offhook are pickups of incoming calls.  Nothing done on them
                if (lastState != TelephonyManager.CALL_STATE_RINGING) {
                    Log.d(TAG, "CALL_STATE_OFFHOOK");
                }
                break;
            case TelephonyManager.CALL_STATE_IDLE:
                Log.d(TAG, "CALL_STATE_IDLE");
                //Went to idle-  this is the end of a call.  What type depends on previous state(s)
                if (lastState == TelephonyManager.CALL_STATE_RINGING) {
                    //Ring but no pickup-  a miss
                }
                break;
        }
    }

    private void handleStartRecordingCommand(Intent intent) {
        final ResultReceiver resultReceiver = intent.getParcelableExtra(RESULT_RECEIVER);

        if (mRecording) {
            // Already recording
            resultReceiver.send(RECORD_RESULT_ALREADY_RECORDING, null);
            return;
        }
        mRecording = true;


        if (Util.checkCameraHardware(this)) {
            mCamera = Util.getCameraInstance();
            if (mCamera != null) {
                SurfaceView sv = new SurfaceView(this);

                WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
                WindowManager.LayoutParams params = new WindowManager.LayoutParams(1, 1,
                        WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
                        WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                        PixelFormat.TRANSLUCENT);

                SurfaceHolder sh = sv.getHolder();

                sv.setZOrderOnTop(true);
                sh.setFormat(PixelFormat.TRANSPARENT);

                sh.addCallback(new SurfaceHolder.Callback() {
                    @Override
                    public void surfaceCreated(SurfaceHolder holder) {
                        Camera.Parameters params = mCamera.getParameters();
                        mCamera.setParameters(params);
                        Camera.Parameters p = mCamera.getParameters();

                        List<Camera.Size> listSize;

                        listSize = p.getSupportedPreviewSizes();
                        Camera.Size mPreviewSize = listSize.get(2);
                        Log.v("TAG", "preview width = " + mPreviewSize.width
                                + " preview height = " + mPreviewSize.height);
                        p.setPreviewSize(mPreviewSize.width, mPreviewSize.height);

                        listSize = p.getSupportedPictureSizes();
                        Camera.Size mPictureSize = listSize.get(2);
                        Log.v("TAG", "capture width = " + mPictureSize.width
                                + " capture height = " + mPictureSize.height);
                        p.setPictureSize(mPictureSize.width, mPictureSize.height);
                        mCamera.setParameters(p);

                        try {
                            mCamera.setPreviewDisplay(holder);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        mCamera.startPreview();

                        mCamera.unlock();

                        mMediaRecorder = new MediaRecorder();
                        mMediaRecorder.setCamera(mCamera);

                        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

                        mMediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH));
                        mRecordingPath = Util.getOutputMediaFile(Util.MEDIA_TYPE_VIDEO).getPath();
                        mMediaRecorder.setOutputFile(mRecordingPath);


                        mMediaRecorder.setPreviewDisplay(holder.getSurface());

                        try {
                            mMediaRecorder.prepare();
                        } catch (IllegalStateException e) {
                            Log.d(TAG, "IllegalStateException when preparing MediaRecorder: " + e.getMessage());
                        } catch (IOException e) {
                            Log.d(TAG, "IOException when preparing MediaRecorder: " + e.getMessage());
                        }
                        mMediaRecorder.start();

                        resultReceiver.send(RECORD_RESULT_OK, null);
                        Log.d(TAG, "Recording is started");

                    }

                    @Override
                    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                    }

                    @Override
                    public void surfaceDestroyed(SurfaceHolder holder) {
                    }
                });


                wm.addView(sv, params);

            } else {
                Log.d(TAG, "Get Camera from service failed");
                resultReceiver.send(RECORD_RESULT_GET_CAMERA_FAILED, null);
            }
        } else {
            Log.d(TAG, "There is no camera hardware on device.");
            resultReceiver.send(RECORD_RESULT_DEVICE_NO_CAMERA, null);
        }
    }

    private void handleStopRecordingCommand(Intent intent) {
        ResultReceiver resultReceiver = intent.getParcelableExtra(RESULT_RECEIVER);

        if (!mRecording) {
            // have not recorded
            resultReceiver.send(RECORD_RESULT_NOT_RECORDING, null);
            return;

        }

        mMediaRecorder.stop();
        mMediaRecorder.release();
        mCamera.stopPreview();
        mCamera.release();

        Bundle b = new Bundle();
        b.putString(VIDEO_PATH, mRecordingPath);

        mRecordingPath = null;

        resultReceiver.send(RECORD_RESULT_OK, b);

        mRecording = false;
        Log.d(TAG, "recording is finished.");
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

}
