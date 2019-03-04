package com.example.reverse_engineering_proprietary_slam_systems;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;

import android.net.Uri;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.StateSet;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.ar.core.ArCoreApk;
import com.google.ar.core.AugmentedImage;
import com.google.ar.core.AugmentedImageDatabase;
import com.google.ar.core.Config;
import com.google.ar.core.Frame;
import com.google.ar.core.Session;
import com.google.ar.core.TrackingState;
import com.google.ar.core.exceptions.NotYetAvailableException;
import com.google.ar.core.exceptions.UnavailableApkTooOldException;
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException;
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException;
import com.google.ar.core.exceptions.UnavailableSdkTooOldException;
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException;
import com.google.ar.sceneform.rendering.ModelRenderable;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;


public class MainActivity extends AppCompatActivity {

    /* Define global variables */
    private static final String TAG = "MainActivity";
    private long startTime;
    private MyArFragment myArFragment;
    private ModelRenderable myRenderable;
    private Session mySession;

    private Button initButton;
    private boolean isInitialized = false;
    private Button captureButton;
    private boolean captBtnClicked = false;
    private TextView myStatusTxt;

    private boolean mUserRequestedInstall = true;
    private boolean shouldAddModel;

    FileManager myFileManager;
    MathUtils mathUtilStart;
    MathUtils mathUtilEnd;

    private enum STATUS {
        START,
        INITIALIZATION,
        TRACKING,
        CLOSELOOP,
    }

    private STATUS myStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Defining elements from the UI
        myArFragment = (MyArFragment) getSupportFragmentManager()
                .findFragmentById(R.id.myArFrag);
        captureButton = findViewById(R.id.captureButton);
        captureButton.setVisibility(View.GONE);
        initButton = findViewById(R.id.initButton);
        myStatusTxt = findViewById(R.id.statusText);

        // Store timestamp when application is executed as start time and set status
        startTime = System.currentTimeMillis();

        shouldAddModel = true;

        // New instance of class MathUtils. Set max iteration number for the initialization to 10
        mathUtilStart = new MathUtils(10);
        mathUtilEnd = new MathUtils(10);


        // Check for the permissions
        this.isStoragePermissionGranted();
        this.isCameraPermissionGranted();

        // Check that ar core is available on the device
        myArFragment.checkArCoreAvailability(this);

        // Create renderable (Red sphere)
        createRenderable();

        /**
         * When plane in AR recognized and taped, create a sphere at the taped location
         */
        myArFragment.setOnTapArPlaneListener((hitResult, plane, motionEvent) -> {
            myArFragment.addAnchorNodeToScene(hitResult.createAnchor(), myRenderable);
        });

        /**
         * Set to initialization mode when button is clicked and current status is START
         */
        initButton.setOnClickListener(v -> {
            Log.i(TAG, "Test7");
            if (myStatus == STATUS.START)
                Log.i(TAG, "Test8");
                myStatusTxt.setText("STATUS: Initialization");
                myStatus = STATUS.INITIALIZATION;
                initButton.setVisibility(View.GONE);
        });

        /**
         * Handle camera pose and image capture
         */
        captureButton.setOnClickListener(v -> {
            Log.i(TAG, "Test6");
            if (myStatus == STATUS.TRACKING) {

                captBtnClicked = !captBtnClicked;

                if (captBtnClicked) {
                    Log.i(TAG, "Test4");
                    myFileManager = new FileManager();
                    captureButton.setText("STOP Capture Pose");
                } else {
                    Log.i(TAG, "Test5");
                    String poseInfo = myFileManager.savePose();
                    Log.i(TAG, poseInfo);
                    captureButton.setText("START Capture Pose");
                    myStatusTxt.setText("STATUS: CLOSE LOOP");
                    myStatus = STATUS.CLOSELOOP;
                }
            } else
                Toast.makeText(this, "Not in tracking status", Toast.LENGTH_LONG);
        });

        /**
         * Get camera pose and image for every frame and save it to the storage of the phone
         */
        myArFragment.getArSceneView().getScene().addOnUpdateListener(frameTime -> {

            // Get the current frame
            Frame currFrame = myArFragment.getArSceneView().getArFrame();

            switch (myStatus) {
                case TRACKING:
                    if (captBtnClicked) {
                        long currTime = System.currentTimeMillis() - startTime;

                        float[] currCamTrans = currFrame.getCamera().getPose().getTranslation();
                        float[] currCamRot = currFrame.getCamera().getPose().getRotationQuaternion();

                        myFileManager.writeNewPose(currTime, mathUtilStart.getRelativeTranslation(currCamTrans),
                                mathUtilStart.getRelativeOrientation(currCamRot));
                        try {
                            Image currImg = currFrame.acquireCameraImage();
                            byte[] jpegData = ImageUtils.imageToByteArray(currImg);
                            myFileManager.saveImage("img_" + currTime, jpegData);
                            currImg.close();
                        } catch (NotYetAvailableException e) {
                            e.printStackTrace();
                        }
                    }
                    break;

                case INITIALIZATION:
                    Collection<AugmentedImage> augmentedImages = currFrame.getUpdatedTrackables(AugmentedImage.class);
                    for (AugmentedImage augmentedImage : augmentedImages) {
                        if (augmentedImage.getTrackingState() == TrackingState.TRACKING) {
                            if (augmentedImage.getName().equals("earth") && shouldAddModel) {
                                float[] initCoord = augmentedImage.getCenterPose().getTranslation();
                                float[] initQuat = currFrame.getCamera().getPose().getRotationQuaternion();

                                if (!mathUtilStart.addCoord(initCoord, initQuat)) {
                                    Toast.makeText(this, "Initialization successfull!", Toast.LENGTH_LONG);
                                    myStatus = STATUS.TRACKING;
                                    myStatusTxt.setText("STATUS: tracking");
                                    captureButton.setVisibility(View.VISIBLE);
                                    myArFragment.addTrackableNodeToScene(augmentedImage.createAnchor(augmentedImage.getCenterPose()), myRenderable);
                                    //shouldAddModel = false;
                                    Log.i(TAG, "Init coord: " + mathUtilStart.initCoord[0] + " ; " + mathUtilStart.initCoord[1] + " ; " + mathUtilStart.initCoord[2]);
                                    Log.i(TAG, "coord std dev: " + mathUtilStart.initCoordStdDev[0] + " ; " + mathUtilStart.initCoordStdDev[1] + " ; " + mathUtilStart.initCoordStdDev[2]);

                                    Log.i(TAG, "Init quat: " + mathUtilStart.initQuater.x + " ; " + mathUtilStart.initQuater.y + " ; " + mathUtilStart.initQuater.z + " ; " + mathUtilStart.initQuater.w);
                                    Log.i(TAG, "Quat std dev: " + mathUtilStart.initQuaterStdDev[0] + " ; " + mathUtilStart.initQuaterStdDev[1] + " ; " + mathUtilStart.initQuaterStdDev[2] + " ; " + mathUtilStart.initQuaterStdDev[3]);

                                }

                            }
                        }
                    }
                    break;

                case CLOSELOOP:
                    Collection<AugmentedImage> augmentedImages2 = currFrame.getUpdatedTrackables(AugmentedImage.class);
                    for (AugmentedImage augmentedImage2 : augmentedImages2) {
                        if (augmentedImage2.getTrackingState() == TrackingState.TRACKING) {
                            if (augmentedImage2.getName().equals("earth")) {

                                float[] initCoord = augmentedImage2.getCenterPose().getTranslation();
                                float[] initQuat = currFrame.getCamera().getPose().getRotationQuaternion();
                                if (!mathUtilEnd.addCoord(initCoord, initQuat)) {
                                    Log.i(TAG, "End coord: " + mathUtilEnd.initCoord[0] + " ; " + mathUtilEnd.initCoord[1] + " ; " + mathUtilEnd.initCoord[2]);
                                    Log.i(TAG, "End coord std dev: " + mathUtilEnd.initCoordStdDev[0] + " ; " + mathUtilEnd.initCoordStdDev[1] + " ; " + mathUtilEnd.initCoordStdDev[2]);
                                    Log.i(TAG, "End quat: " + mathUtilEnd.initQuater.x + " ; " + mathUtilEnd.initQuater.y + " ; " + mathUtilEnd.initQuater.z + " ; " + mathUtilEnd.initQuater.w);
                                    Log.i(TAG, "End Quat std dev: " + mathUtilEnd.initQuaterStdDev[0] + " ; " + mathUtilEnd.initQuaterStdDev[1] + " ; " + mathUtilEnd.initQuaterStdDev[2] + " ; " + mathUtilEnd.initQuaterStdDev[3]);
                                    myStatus = STATUS.START;
                                    myStatusTxt.setText("STATUS: Start");
                                    initButton.setVisibility(View.VISIBLE);
                                }
                            }
                        }
                    }
                    break;
            }
        });
    }

    @Override
    public void onResume(){
        super.onResume();

        // Make sure ARCore is installed and create a new Session and define all the configurations
        // for this session
        if(createSession()){
            defineConfiguration();
            myStatusTxt.setText("STATUS: START");
            myStatus =  STATUS.START;

        }

    }

    /**
     * Create a global renderable for the ArView
     */
    private void createRenderable() {

        ModelRenderable.builder()
                // To load as an asset from the 'assets' folder ('src/main/assets/andy.sfb'):
                .setSource(this, Uri.parse("earth.sfb"))

                // Instead, load as a resource from the 'res/raw' folder ('src/main/res/raw/andy.sfb'):
                //.setSource(this, R.raw.andy)

                .build()
                .thenAccept(renderable -> myRenderable = renderable)
                .exceptionally(
                        throwable -> {
                            Log.e(TAG, "Unable to load Renderable.", throwable);
                            return null;
                        });
        /* MaterialFactory.makeOpaqueWithColor(this, new Color(android.graphics.Color.RED))
                .thenAccept(material -> {
                    myRenderable = ShapeFactory.makeSphere(0.1f,
                            new Vector3(0f, 0f, 0f), material);
                });
        */
    }


    /**
     * Create e new session and make sure ArCore is installed
     * @return boolean if a new session could be created
     */
    private Boolean createSession(){

        Boolean sessionCreated = false;
        try {
            if (mySession == null) {
                switch (ArCoreApk.getInstance().requestInstall(this, mUserRequestedInstall)) {
                    case INSTALLED:
                        // Success, create the AR session.
                        mySession = new Session(this);

                        Log.v(TAG, "Ar session installed");
                        sessionCreated = true;
                        break;
                    case INSTALL_REQUESTED:
                        Log.v(TAG, "Ar session not installed");
                        // Ensures next invocation of requestInstall() will either return
                        // INSTALLED or throw an exception.
                        mUserRequestedInstall = false;
                }
            } else
                sessionCreated = true;
        } catch (UnavailableUserDeclinedInstallationException e) {
            Log.v(TAG, "Ar session not installed user declined");
            // Display an appropriate message to the user and return gracefully.
            Toast.makeText(this, "Handle exception " + e, Toast.LENGTH_LONG)
                    .show();
        } catch (UnavailableArcoreNotInstalledException e) {
            e.printStackTrace();
        } catch (UnavailableDeviceNotCompatibleException e) {
            e.printStackTrace();
        } catch (UnavailableSdkTooOldException e) {
            e.printStackTrace();
        } catch (UnavailableApkTooOldException e) {
            e.printStackTrace();
        }
        return sessionCreated;
    }


    /**
     * Define configuration for the main session
     */
    private void defineConfiguration(){
        // Define new configuration and set autofocus
        Config config = new Config(mySession);
        config.setFocusMode(Config.FocusMode.AUTO);
        config.setUpdateMode(Config.UpdateMode.LATEST_CAMERA_IMAGE);

        // Set the camera configurations for the session
        mySession.setCameraConfig(mySession.getSupportedCameraConfigs().get(1));
        myArFragment.getArSceneView().setupSession(mySession);

        if (setupAugmentedImagesDb(config, mySession)){
            Log.i(TAG, "Image database setup successful!");
        } else{
            Log.i(TAG, "Image database setup not successful!");
        }

        mySession.configure(config);
    }


    /**
     * Setup a database with images to track
     * @param config Configuration to set up database
     * @param session Session which includes the database
     * @return boolean if an image database could be set-up
     */
    private boolean setupAugmentedImagesDb(Config config, Session session) {
        AugmentedImageDatabase augmentedImageDatabase;
        Bitmap bitmap = loadAugmentedImage();
        if (bitmap == null) {
            return false;
        }
        augmentedImageDatabase = new AugmentedImageDatabase(session);
        augmentedImageDatabase.addImage("earth", bitmap);
        config.setAugmentedImageDatabase(augmentedImageDatabase);
        return true;
    }


    /**
     * Load images from the asset folder
     * @return Bitmap of the image in the asset folder
     */
    private Bitmap loadAugmentedImage() {
        try (InputStream is = getAssets().open("earth.jpg")) {
            return BitmapFactory.decodeStream(is);
        } catch (IOException e) {
            Log.e(TAG, "IO Exception", e);
        }
        return null;
    }


    /**
     * Check if app has permission to write on storage
     **/
    private void isStoragePermissionGranted() {
        if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) {
            Log.v(TAG,"Permission is granted");
        } else {

            Log.v(TAG,"Permission is revoked");
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }
    }


    /**
     * Check if app has permission for the camera
     **/
    private void isCameraPermissionGranted() {
        if (checkSelfPermission(Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            Log.i(TAG,"Camera permission is granted");
        } else {

            Log.w(TAG,"Camera permission is revoked");
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, 1);
        }
    }


        /*
     * Used as a handler for onClick, so the signature must match onClickListener.

    private void toggleRecording() {

        boolean recording = videoRecorder.onToggleRecord();
        if (!recording) {
            String videoPath = videoRecorder.getVideoPath().getAbsolutePath();
            Toast.makeText(this, "Video saved: " + videoPath, Toast.LENGTH_SHORT).show();
            Log.d("Video status", "Video saved: " + videoPath);

            // Send  notification of updated content.
            ContentValues values = new ContentValues();
            values.put(MediaStore.Video.Media.TITLE, "Sceneform Video");
            values.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4");
            values.put(MediaStore.Video.Media.DATA, videoPath);
            getContentResolver().insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values);
        }
    }
    */
}
