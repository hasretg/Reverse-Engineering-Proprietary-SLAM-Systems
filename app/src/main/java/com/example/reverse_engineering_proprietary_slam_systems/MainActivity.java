package com.example.reverse_engineering_proprietary_slam_systems;

import android.Manifest;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.CamcorderProfile;
import android.media.Image;
import android.net.Uri;
import android.nfc.Tag;
import android.os.Build;
import android.os.Environment;

import android.os.Handler;
import android.provider.MediaStore;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import com.google.ar.core.Anchor;
import com.google.ar.core.ArCoreApk;
import com.google.ar.core.AugmentedImage;
import com.google.ar.core.AugmentedImageDatabase;
import com.google.ar.core.CameraConfig;
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
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.Camera;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.Color;
import com.google.ar.sceneform.rendering.Material;
import com.google.ar.sceneform.rendering.MaterialFactory;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.Renderable;
import com.google.ar.sceneform.rendering.ShapeFactory;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.TransformableNode;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.nio.ByteBuffer;
import java.util.Calendar;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;


public class MainActivity extends AppCompatActivity {

    /* Define global variables */
    private static final String TAG = "MainActivity";
    private static String TEXT_FILE_NAME = "arCameraPose";
    private static String PATH_POSEDIR = Environment.getExternalStorageDirectory()
            + File.separator + "PoseDir";
    private File folderPose;
    private String arCameraPoseText = "";
    protected OutputStreamWriter myOutputStreamWriter;
    protected FileOutputStream myFileOutputStream;
    private long startTime;
    private ArFragment arFragment;
    private Button recordButton;
    private boolean recBtnClicked = false;
    private Button poseCaptureButton;
    private boolean captBtnClicked = false;
    private VideoRecorder videoRecorder;

    private static String path_imgDir = Environment.getExternalStorageDirectory()
            + File.separator + "ImgCapture";
    private File folderImg;
    private static String TEXT_IMG_NAME = "currImg_";
    private Session arSession;
    private boolean mUserRequestedInstall = true;
    private boolean shouldAddModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Defining elements from the UI
        arFragment = (ArFragment) getSupportFragmentManager()
                .findFragmentById(R.id.arFragment);
        recordButton = findViewById(R.id.recButton);
        poseCaptureButton = findViewById(R.id.poseCaptureButton);


        // Initialize the VideoRecorder
        videoRecorder = new VideoRecorder();
        int orientation = getResources().getConfiguration().orientation;
        videoRecorder.setVideoQuality(CamcorderProfile.QUALITY_1080P, orientation);
        videoRecorder.setSceneView(arFragment.getArSceneView());

        // Store timestamp when application is executed as start time
        startTime = System.currentTimeMillis();

        shouldAddModel = true;
        // Check for the permissions
        this.isStoragePermissionGranted();
        this.isCameraPermissionGranted();

        // Check that ar core is available on the device
        this.checkArCoreAvailability();

        // Make sure the path directory exists.
        folderPose = new File(PATH_POSEDIR);
        if(!folderPose.exists())
            folderPose.mkdirs();

        folderImg = new File(path_imgDir);
        if(!folderImg.exists())
            folderImg.mkdir();

        /*
         * When plane in AR recognized and taped, create a sphere at the taped location
         */
        arFragment.setOnTapArPlaneListener((hitResult, plane, motionEvent) -> {

            Anchor anchor = hitResult.createAnchor();

            MaterialFactory.makeOpaqueWithColor(this, new Color(android.graphics.Color.RED))
                    .thenAccept(material -> {
                        ModelRenderable renderable = ShapeFactory.makeSphere(0.1f,
                                new Vector3(0f, 0f, 0f), material);

                        AnchorNode anchorNode = new AnchorNode(anchor);
                        anchorNode.setRenderable(renderable);
                        arFragment.getArSceneView().getScene().addChild(anchorNode);
                    });
        });

        /*
         * Handle video recording
         */
        recordButton.setOnClickListener(v -> {

            recBtnClicked = !recBtnClicked;
            this.toggleRecording();
            if (recBtnClicked) {
                recordButton.setText("STOP Recording");
            } else {
                recordButton.setText("START Recording");
            }
        });

        /*
         * Handle camera pose
         */
        poseCaptureButton.setOnClickListener(v -> {

            captBtnClicked = !captBtnClicked;

            if (captBtnClicked) {
                try {
                    // Create the file.
                    File file = new File(folderPose, TEXT_FILE_NAME + "_" +
                            Calendar.getInstance().getTime().toString()+".txt");
                    file.createNewFile();
                    myFileOutputStream = new FileOutputStream(file);
                    myOutputStreamWriter = new OutputStreamWriter(myFileOutputStream);
                    poseCaptureButton.setText("STOP Capture Pose");
                } catch (Throwable t) {
                    Log.e(TAG, "Text file could not be generated.");
                }
            } else {
                try {
                    this.writeToFile(arCameraPoseText);
                    myOutputStreamWriter.close();
                    myFileOutputStream.flush();
                    myFileOutputStream.close();
                    poseCaptureButton.setText("START Capture Pose");
                } catch (Throwable t) {
                    Log.w(TAG, "Text file could not be saved.");
                }
            }
        });

        /*
         * Get camera pose and image for every frame and save it to the storage of the phone
         */
        arFragment.getArSceneView().getScene().addOnUpdateListener(frameTime -> {

            if (captBtnClicked) {

                Vector3 currCameraPose = arFragment.getArSceneView().getScene().getCamera()
                        .getWorldPosition();
                long currTime = System.currentTimeMillis() - startTime;
                arCameraPoseText += "" + currTime + "[ Delta sec: " + frameTime.getDeltaSeconds() +
                        " ]" + "  Pos: " + currCameraPose.toString() + "\n";
                try {
                    Image currImg = arFragment.getArSceneView().getArFrame().acquireCameraImage();

                    byte[] jpegData = ImageUtils.imageToByteArray(currImg);

                    FileManager.writeFrame(folderImg, TEXT_IMG_NAME + "_" +
                            currTime + "_.jpg", jpegData);
                    currImg.close();
                    Log.i(TAG, "Img saved");

                    // Get image size of the current frame
                    int[] imgSize = arFragment.getArSceneView().getArFrame().getCamera().getImageIntrinsics().getImageDimensions();
                    Log.i(TAG, "Img dim: " + imgSize[0] + "  " + imgSize[1]);

                } catch (NotYetAvailableException e) {
                    e.printStackTrace();
                    Log.i(TAG, "Img could not be saved");
                }

            }

            
            Frame frame = arFragment.getArSceneView().getArFrame();
            Collection<AugmentedImage> augmentedImages = frame.getUpdatedTrackables(AugmentedImage.class);
            for (AugmentedImage augmentedImage : augmentedImages) {
                if (augmentedImage.getTrackingState() == TrackingState.TRACKING) {
                    if (augmentedImage.getName().equals("earth") && shouldAddModel) {
                        this.placeObject(arFragment, augmentedImage.createAnchor(augmentedImage.getCenterPose()));
                        shouldAddModel = false;
                    }
                }
            }

        });
    }


    @Override
    public void onResume(){
        super.onResume();

        // Make sure ARCore is installed and create a new Session
        try {
            if (arSession == null) {
                switch (ArCoreApk.getInstance().requestInstall(this, mUserRequestedInstall)) {
                    case INSTALLED:
                        // Success, create the AR session.
                        arSession = new Session(this);

                        Log.v(TAG, "Ar session installed");
                        break;
                    case INSTALL_REQUESTED:
                        Log.v(TAG, "Ar session not installed");
                        // Ensures next invocation of requestInstall() will either return
                        // INSTALLED or throw an exception.
                        mUserRequestedInstall = false;
                        return;
                }
            }
        } catch (UnavailableUserDeclinedInstallationException e) {
            Log.v(TAG, "Ar session not installed user declined");
            // Display an appropriate message to the user and return gracefully.
            Toast.makeText(this, "TODO: handle exception " + e, Toast.LENGTH_LONG)
                    .show();
            return;
        } catch (UnavailableArcoreNotInstalledException e) {
            e.printStackTrace();
        } catch (UnavailableDeviceNotCompatibleException e) {
            e.printStackTrace();
        } catch (UnavailableSdkTooOldException e) {
            e.printStackTrace();
        } catch (UnavailableApkTooOldException e) {
            e.printStackTrace();
        }

        // Define new configuration and set autofocus
        Config config = new Config(arSession);
        config.setFocusMode(Config.FocusMode.AUTO);
        config.setUpdateMode(Config.UpdateMode.LATEST_CAMERA_IMAGE);

        /*
         * Set the camera configurations for the session
         * get(0) = 640 x 480 pixel --> 30 Hz
         * get(1) = 1280 x 720 pixel --> 12 Hz
         * get(2) = 1920 x 1080 pixel --> 7 Hz
         */
        arSession.setCameraConfig(arSession.getSupportedCameraConfigs().get(1));
        arFragment.getArSceneView().setupSession(arSession);

        if (setupAugmentedImagesDb(config, arSession)){
            Log.i(TAG, "Image database setup successful!");
        } else{
            Log.i(TAG, "Image database setup not successful!");
        }

        arSession.configure(config);


    }

    /*
     * Used as a handler for onClick, so the signature must match onClickListener.
     */
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

    /*
     * Setup a database with images to track
     */
    public boolean setupAugmentedImagesDb(Config config, Session session) {
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

    /*
     * Load images from the assets folder
     */
    private Bitmap loadAugmentedImage() {
        try (InputStream is = getAssets().open("earth.jpg")) {
            return BitmapFactory.decodeStream(is);
        } catch (IOException e) {
            Log.e("ImageLoad", "IO Exception", e);
        }
        return null;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void placeObject(ArFragment arFragment, Anchor anchor) {
        MaterialFactory.makeOpaqueWithColor(this, new Color(android.graphics.Color.RED))
                .thenAccept(material -> {
                            ModelRenderable renderable = ShapeFactory.makeSphere(0.1f,
                                    new Vector3(0f, 0f, 0f), material);
                            addNodeToScene(arFragment, anchor, renderable);
                        });
    }


    private void addNodeToScene(ArFragment arFragment, Anchor anchor, Renderable renderable) {
        AnchorNode anchorNode = new AnchorNode(anchor);
        TransformableNode node = new TransformableNode(arFragment.getTransformationSystem());
        node.setRenderable(renderable);
        node.setParent(anchorNode);
        arFragment.getArSceneView().getScene().addChild(anchorNode);
        node.select();
    }


    /*
     * Write a string in a given file
     */
    private void writeToFile(String data) {

        if (isExternalStorageWritable()){
            try {
                myOutputStreamWriter.write(data);

                Toast.makeText(this, "File saved" ,Toast.LENGTH_SHORT).show();
            }
            catch (FileNotFoundException e) {
                e.printStackTrace();
                Toast.makeText(this, "File not found" ,Toast.LENGTH_SHORT).show();
            }
            catch (IOException e){
                e.printStackTrace();
                Toast.makeText(this, "Error saving" ,Toast.LENGTH_SHORT).show();
            }
            catch(Throwable t) {
                Toast.makeText(this, "Exception: "+t.toString(), Toast.LENGTH_LONG).show();
            }
        } else{
            Log.e("StorageException", "External storage not available to store data!!");
        }
    }

    /*
     * Checks if external storage is available for read and write
     */
    public boolean isExternalStorageWritable(){

        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    /*
     * Check if app has permission to write on storage
     */
    public void isStoragePermissionGranted() {
        if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) {
            Log.v(TAG,"Permission is granted");
        } else {

            Log.v(TAG,"Permission is revoked");
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }
    }

    /*
     * Check if app has permission for the camera
     */
    public void isCameraPermissionGranted() {
        if (checkSelfPermission(Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            Log.v(TAG,"Permission is granted");
        } else {

            Log.v(TAG,"Permission is revoked");
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, 1);
        }
    }

    void checkArCoreAvailability() {
        ArCoreApk.Availability availability = ArCoreApk.getInstance().checkAvailability(this);
        if (availability.isTransient()) {
            // Re-query at 5Hz while compatibility is checked in the background.
            new Handler().postDelayed(() -> checkArCoreAvailability(), 200);
        }
        if (availability.isSupported()) {
            // indicator on the button.
        } else { // Unsupported or unknown.

        }
    }
}
