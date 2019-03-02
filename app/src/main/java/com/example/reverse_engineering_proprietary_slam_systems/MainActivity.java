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
import android.widget.Button;
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
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.Color;
import com.google.ar.sceneform.rendering.MaterialFactory;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.ShapeFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

import static android.arch.lifecycle.Lifecycle.State.INITIALIZED;


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

    private boolean mUserRequestedInstall = true;
    private boolean shouldAddModel;

    FileManager myFileManager;

    private enum  STATUS {
        START,
        INITIALIZE,
        TRACKING
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Defining elements from the UI
        myArFragment = (MyArFragment) getSupportFragmentManager()
                .findFragmentById(R.id.myArFrag);
        captureButton = findViewById(R.id.captureButton);
        initButton = findViewById(R.id.initButton);

        // Store timestamp when application is executed as start time
        startTime = System.currentTimeMillis();

        shouldAddModel = true;
        // Check for the permissions
        this.isStoragePermissionGranted();
        this.isCameraPermissionGranted();

        // Check that ar core is available on the device
        myArFragment.checkArCoreAvailability(this);

        // Create renderable (Red sphere)
        createRenderable();

        /*
         * When plane in AR recognized and taped, create a sphere at the taped location
         */
        myArFragment.setOnTapArPlaneListener((hitResult, plane, motionEvent) -> {
            myArFragment.addAnchorNodeToScene(hitResult.createAnchor(), myRenderable);
        });


        /*
         * Handle camera pose
         */
        captureButton.setOnClickListener(v -> {

            captBtnClicked = !captBtnClicked;

            if (captBtnClicked) {
                myFileManager= new FileManager();
                captureButton.setText("STOP Capture Pose");
            } else {
                Log.i(TAG, myFileManager.savePose());
                captureButton.setText("START Capture Pose");
            }
        });

        initButton.setOnClickListener(v ->{




        });

        /*
         * Get camera pose and image for every frame and save it to the storage of the phone
         */
        myArFragment.getArSceneView().getScene().addOnUpdateListener(frameTime -> {

            // Get the current frame
            Frame currFrame = myArFragment.getArSceneView().getArFrame();

            if (captBtnClicked) {
                long currTime = System.currentTimeMillis() - startTime;

                float[] currCamTrans = currFrame.getCamera().getPose().getTranslation();
                float[] currCamRot = currFrame.getCamera().getPose().getRotationQuaternion();

                myFileManager.writeNewPose(currTime, currCamTrans, currCamRot);

                try {
                    Image currImg = currFrame.acquireCameraImage();
                    byte[] jpegData = ImageUtils.imageToByteArray(currImg);
                    myFileManager.saveImage("img_" + currTime, jpegData);
                    currImg.close();
                } catch (NotYetAvailableException e) {
                    e.printStackTrace();
                }
            }


            Collection<AugmentedImage> augmentedImages = currFrame.getUpdatedTrackables(AugmentedImage.class);
            for (AugmentedImage augmentedImage : augmentedImages) {
                if (augmentedImage.getTrackingState() == TrackingState.TRACKING) {
                    if (augmentedImage.getName().equals("earth") && shouldAddModel) {
                        myArFragment.addTrackableNodeToScene(augmentedImage.createAnchor(augmentedImage.getCenterPose()), myRenderable);
                        shouldAddModel = false;
                        Log.i(TAG, "Extend x: " + augmentedImage.getExtentX() + " ; Extend y: " + augmentedImage.getExtentZ());
                        Log.i(TAG, "Center position: " + augmentedImage.getCenterPose().toString());

                    }
                }
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
