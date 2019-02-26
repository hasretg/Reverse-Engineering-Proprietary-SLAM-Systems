package com.example.reverse_engineering_proprietary_slam_systems;

import android.Manifest;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.media.CamcorderProfile;
import android.os.Environment;

import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import com.google.ar.core.Anchor;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.Color;
import com.google.ar.sceneform.rendering.MaterialFactory;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.ShapeFactory;
import com.google.ar.sceneform.ux.ArFragment;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Calendar;


public class MainActivity extends AppCompatActivity {

    /* Define global variables */
    private static final String TAG = "SLAM_thesis";
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
        //videoRecorder.setFrameRate(5);

        // Store timestamp when application is executed as start time
        startTime = System.currentTimeMillis();

        // Check for the permissions
        this.isStoragePermissionGranted();
        this.isCameraPermissionGranted();

        // Make sure the path directory exists.
        folderPose = new File(PATH_POSEDIR);
        if(!folderPose.exists())
            folderPose.mkdirs();

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

            Log.v("test1", "Timestamp: " + arCameraPoseText);
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
         * Get camera pose for every frame
         */
        arFragment.getArSceneView().getScene().addOnUpdateListener(frameTime -> {

            Vector3 currCameraPose = arFragment.getArSceneView().getScene().getCamera()
                    .getWorldPosition();
            long currTime = System.currentTimeMillis() - startTime;
            arCameraPoseText += "" + currTime + "[ Delta sec: " + frameTime.getDeltaSeconds() +
                    " ]" + "  Pos: " + currCameraPose.toString() + "\n";
        });
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
}
