// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT license.
package com.microsoft.sampleandroid;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.os.Handler;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Button;
import android.view.MotionEvent;

import com.google.ar.core.ArCoreApk;
import com.google.ar.core.AugmentedImage;
import com.google.ar.core.AugmentedImageDatabase;
import com.google.ar.core.Config;
import com.google.ar.core.Frame;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.core.Session;
import com.google.ar.core.TrackingState;
import com.google.ar.core.exceptions.UnavailableApkTooOldException;
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException;
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException;
import com.google.ar.core.exceptions.UnavailableSdkTooOldException;
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException;
import com.google.ar.sceneform.Scene;
import com.google.ar.sceneform.ux.ArFragment;
import com.microsoft.azure.spatialanchors.AnchorLocateCriteria;
import com.microsoft.azure.spatialanchors.AnchorLocatedEvent;
import com.microsoft.azure.spatialanchors.CloudSpatialAnchor;
import com.microsoft.azure.spatialanchors.CloudSpatialAnchorSession;
import com.microsoft.azure.spatialanchors.LocateAnchorsCompletedEvent;
import com.nimbusds.jose.util.ArrayUtils;

import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.concurrent.Future;

public class Shared extends AzureSpatialAnchorsActivity
{
    enum DemoStep
    {
        DemoStepInitialization, // Initialize position to reference
        DemoStepChoosing, // Choosing to create or locate
        DemoStepCreating, // Creating an anchor
        DemoStepSaving,   // Saving an anchor to the cloud
        DemoStepEnteringAnchorNumber, // Picking an anchor to find
        DemoStepLocating  // Looking for an anchor
    }

    // Set this string to the URL created when publishing your Shared anchor service in the Sharing sample.
    private static final String SharingAnchorsServiceUrl = "http://microsoft-slam.azurewebsites.net/api/anchors";

    private TextView mTextView;
    private Button mLocateButton;
    private Button mCreateButton;
    private Button mInitButton;
    private TextView mEditTextInfo;
    private EditText mAnchorNumInput;
    private Button mSaveButton;
    private DecimalFormat mDecimalFormat = new DecimalFormat("00");
    private String mFeedbackText;
    private DemoStep currentStep = DemoStep.DemoStepInitialization;

    private String mAnchorId = "";
    private final Handler mHandler = new Handler();

    private long startTime;
    private int frameID;
    private MathUtils mMathUtils;
    private FileManager mFileManager;
    private boolean mUserRequestedInstall = true;
    private Session mySession;
    private String markerName = "aruco_1";
    private ArFragment mArFragment;
    Scene.OnUpdateListener onUpdateListener;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {

        super.onCreate(savedInstanceState);
        startTime = System.currentTimeMillis();
        // Set the enumeration of the frames to 1
        frameID = 1;

        // New instance of class MathUtils. Set max iteration number for the initialization to 10
        mMathUtils = new MathUtils(10);

        // Make sure ARCore is installed and create a new Session and define all the configurations
        // for this session
        if (createSession()) {
            defineConfiguration();
        }
    }



    @Override
    protected void SetContentView()
    {
        setContentView(R.layout.activity_shared);
    }

    @Override
    protected void ConfigureUI()
    {
        if (SharingAnchorsServiceUrl == "") {
            Toast.makeText(this, "Set the SharingAnchorsServiceUrl in Shared.java", Toast.LENGTH_LONG)
                    .show();

            finish();
        }

        mTextView = findViewById(R.id.textView);
        mTextView.setVisibility(View.VISIBLE);
        mLocateButton = findViewById(R.id.locateButton);
        mCreateButton = findViewById(R.id.createButton);
        mInitButton = findViewById(R.id.initButton1);
        mSaveButton = findViewById(R.id.saveButton);
        mAnchorNumInput = findViewById(R.id.anchorNumText);
        mEditTextInfo = findViewById(R.id.editTextInfo);
        mArFragment = (ArFragment) getSupportFragmentManager()
                .findFragmentById(R.id.ux_fragment);
        EnableCorrectUIControls();
    }

    private void EnableCorrectUIControls()
    {
        switch (currentStep) {
            case DemoStepInitialization:
                mTextView.setVisibility(View.VISIBLE);
                mLocateButton.setVisibility(View.GONE);
                mCreateButton.setVisibility(View.GONE);
                mInitButton.setVisibility(View.VISIBLE);
                mSaveButton.setVisibility(View.GONE);
                mAnchorNumInput.setVisibility(View.GONE);
                mEditTextInfo.setVisibility(View.GONE);
                break;
            case DemoStepChoosing:
                mTextView.setVisibility(View.VISIBLE);
                mLocateButton.setVisibility(View.VISIBLE);
                mCreateButton.setVisibility(View.VISIBLE);
                mInitButton.setVisibility(View.GONE);
                mSaveButton.setVisibility(View.VISIBLE);
                mAnchorNumInput.setVisibility(View.GONE);
                mEditTextInfo.setVisibility(View.GONE);
                break;
            case DemoStepCreating:
                mTextView.setVisibility(View.VISIBLE);
                mLocateButton.setVisibility(View.GONE);
                mCreateButton.setVisibility(View.GONE);
                mInitButton.setVisibility(View.GONE);
                mSaveButton.setVisibility(View.GONE);
                mAnchorNumInput.setVisibility(View.GONE);
                mEditTextInfo.setVisibility(View.GONE);
                break;
            case DemoStepLocating:
                mTextView.setVisibility(View.VISIBLE);
                mLocateButton.setVisibility(View.GONE);
                mCreateButton.setVisibility(View.GONE);
                mInitButton.setVisibility(View.GONE);
                mSaveButton.setVisibility(View.GONE);
                mAnchorNumInput.setVisibility(View.GONE);
                mEditTextInfo.setVisibility(View.GONE);
                break;
            case DemoStepSaving:
                mTextView.setVisibility(View.VISIBLE);
                mLocateButton.setVisibility(View.GONE);
                mCreateButton.setVisibility(View.GONE);
                mInitButton.setVisibility(View.GONE);
                mSaveButton.setVisibility(View.GONE);
                mAnchorNumInput.setVisibility(View.GONE);
                mEditTextInfo.setVisibility(View.GONE);
                break;
            case DemoStepEnteringAnchorNumber:
                mTextView.setVisibility(View.VISIBLE);
                mLocateButton.setVisibility(View.VISIBLE);
                mCreateButton.setVisibility(View.GONE);
                mInitButton.setVisibility(View.GONE);
                mSaveButton.setVisibility(View.GONE);
                mAnchorNumInput.setVisibility(View.VISIBLE);
                mEditTextInfo.setVisibility(View.VISIBLE);
                break;
        }
    }

    @Override
    protected void UpdateStatic()
    {
        new android.os.Handler().postDelayed(() -> {
                    switch (currentStep) {
                        case DemoStepChoosing:
                            mTextView.setText("Chose between creating or locating a cloud anchor");
                            break;
                        case DemoStepCreating:
                            mTextView.setText(mFeedbackText);
                            break;
                        case DemoStepLocating:
                            mTextView.setText("searching for\n"+mAnchorId);
                            break;
                        case DemoStepSaving:
                            mTextView.setText("saving...");
                            break;
                        case DemoStepEnteringAnchorNumber:

                            break;
                    }

                    UpdateStatic();
                },
                500);
    }

    public void initButtonClicked(View source){

        mInitButton.setVisibility(View.GONE);
        mTextView.setText("Initializing...");
         mArFragment.getArSceneView().getScene().addOnUpdateListener(onUpdateListener -> {
             Collection<AugmentedImage> augmentedImages = mArFragment.getArSceneView().getArFrame().
                     getUpdatedTrackables(AugmentedImage.class);

             for (AugmentedImage augmentedImage : augmentedImages) {
                 if (augmentedImage.getTrackingState() == TrackingState.TRACKING) {
                     if(augmentedImage.getName().equals(markerName)){
                         if (mMathUtils.addCoord(augmentedImage.getCenterPose().getTranslation(),
                                 augmentedImage.getCenterPose().getRotationQuaternion()) &&
                                 (currentStep == DemoStep.DemoStepInitialization)){
                             Toast.makeText(this, "Initialization successfull!", Toast.LENGTH_LONG);
                             currentStep = DemoStep.DemoStepChoosing;
                             EnableCorrectUIControls();
                             Log.d("SHARING", "UpdateListener");
                             mFileManager = new FileManager("CLOUD");
                         }
                     }
                 }
             }
         });
    }

    public void saveButtonClicked(View view) {

        if(currentStep == DemoStep.DemoStepChoosing){
            mFileManager.savePose();
            ExitDemoClicked(view);
        }

    }
    public void LocateButtonClicked(View source)
    {
        if (currentStep == DemoStep.DemoStepChoosing) {
            currentStep = DemoStep.DemoStepEnteringAnchorNumber;
            mTextView.setText("Enter an anchor number and press locate");
            EnableCorrectUIControls();
        } else {
            String inputVal = mAnchorNumInput.getText().toString();
            if (inputVal != "") {

                AnchorGetter anchorExchanger = new AnchorGetter(SharingAnchorsServiceUrl, this);
                anchorExchanger.execute(inputVal);

                currentStep = DemoStep.DemoStepLocating;
                EnableCorrectUIControls();
            }
        }
    }

    public void CreateButtonClicked(View source)
    {
        mTextView.setText("Scan your environment and place an anchor");
        mCloudSession = new CloudSpatialAnchorSession();
        configureSession();

        mCloudSession.addSessionUpdatedListener(args -> {
            if (currentStep == DemoStep.DemoStepCreating) {
                float progress = args.getStatus().getRecommendedForCreateProgress();
                if (progress >= 1.0) {
                    AnchorVisual visual = mAnchorVisuals.get("");
                    if (visual != null) {
                        //Transition to saving...
                        TransitionToSaving(visual);
                    } else {
                        mFeedbackText = "Tap somewhere to place an anchor.";
                    }
                } else {
                    mFeedbackText = "Progress is " + mDecimalFormat.format(progress * 100) + "%";
                }
            }
        });

        mCloudSession.start();
        currentStep = DemoStep.DemoStepCreating;
        EnableCorrectUIControls();
    }

    public void ExitDemoClicked(View v)
    {
        synchronized (renderLock) {
            destroySession();

            finish();
        }
    }

    @Override
    protected void HandleTap(HitResult hitResult, Plane plane, MotionEvent motionEvent)
    {
        if (currentStep == DemoStep.DemoStepCreating) {
            AnchorVisual visual = mAnchorVisuals.get("");
            if (visual == null) {
                createAnchor(hitResult);
            }
        }
    }

    @Override
    protected void CreateAnchorCustomCompletion(CloudSpatialAnchor anchor)
    {
        Log.d("ASADemo", "recording anchor with web service");
        AnchorPoster poster = new AnchorPoster(SharingAnchorsServiceUrl, this);
        String anchorId = anchor.getIdentifier();
        Log.d("ASADemo", "anchorId: "+anchorId);
        poster.execute(anchor.getIdentifier());
    }

    @Override
    protected void CreateAnchorExceptionCompletion(String message)
    {
        mTextView.setText(message);
        currentStep = DemoStep.DemoStepChoosing;
        mCloudSession.stop();
        mCloudSession = null;
        EnableCorrectUIControls();
    }

    private void TransitionToSaving(AnchorVisual visual)
    {
        Log.d("ASADemo:", "transition to saving");
        currentStep = DemoStep.DemoStepSaving;
        EnableCorrectUIControls();
        mHandler.post(() ->
        {
            Log.d("ASADemo", "creating anchor");
            visual.cloudAnchor = new CloudSpatialAnchor();
            Date now = new Date();
            Calendar cal = Calendar.getInstance();
            cal.setTime(now);
            cal.add(Calendar.DATE, 100);
            Date oneDayFromNow = cal.getTime();
            visual.cloudAnchor.setExpiration(oneDayFromNow);

            visual.cloudAnchor.setLocalAnchor(visual.getLocalAnchor());
            Log.i("ASADemo", "Transition to saving: "+visual.anchorNode.getAnchor().getPose().toString());
            Log.i("ASADemo", "Transition to saving world: "+visual.anchorNode.getWorldPosition().toString());
            float[] mCoord = visual.anchorNode.getAnchor().getPose().getTranslation();
            float[] mQuat = visual.anchorNode.getAnchor().getPose().getRotationQuaternion();

            mFileManager.writePosterInfo(visual.identifier, new float[]{0f, 0f},
                    mMathUtils.getRelativePose(mCoord, mQuat));
            mFileManager.finishTextline();

            Future createAnchorFuture = mCloudSession.createAnchorAsync(visual.cloudAnchor);
            CheckForCompletion(createAnchorFuture);
        });
    }

    public void AnchorPosted(String anchorNumber)
    {
        mTextView.setText("Anchor Number: " + anchorNumber);
        currentStep = DemoStep.DemoStepChoosing;
        mCloudSession.stop();
        mCloudSession = null;
        mAnchorVisuals.clear();
        EnableCorrectUIControls();
    }

    public void AnchorLookedUp(String anchorId)
    {
        Log.d("ASADemo", "anchor "+anchorId);
        mAnchorId = anchorId;
        mCloudSession = new CloudSpatialAnchorSession();
        configureSession();

        mCloudSession.addAnchorLocatedListener((AnchorLocatedEvent event) -> {
            runOnUiThread(() -> {
                switch (event.getStatus()) {
                    case AlreadyTracked:
                    case Located:
                        AnchorVisual foundVisual = new AnchorVisual();
                        foundVisual.cloudAnchor = event.getAnchor();
                        foundVisual.setLocalAnchor(foundVisual.cloudAnchor.getLocalAnchor());
                        foundVisual.anchorNode.setParent(arFragment.getArSceneView().getScene());
                        foundVisual.identifier = foundVisual.cloudAnchor.getIdentifier();
                        foundVisual.setColor(FoundColor);
                        foundVisual.render(arFragment);
                        Log.i("ASADemo", "Anchor looked up: "+foundVisual.anchorNode.getAnchor().getPose().toString());

                        float[] mCoord = foundVisual.anchorNode.getAnchor().getPose().getTranslation();
                        float[] mQuat = foundVisual.anchorNode.getAnchor().getPose().getRotationQuaternion();
                        mFileManager.writePosterInfo(foundVisual.identifier, new float[]{0f, 0f},
                                mMathUtils.getRelativePose(mCoord, mQuat));
                        mFileManager.finishTextline();


                        mAnchorVisuals.put(foundVisual.identifier, foundVisual);
                        break;
                    case NotLocatedAnchorDoesNotExist:
                        break;
                }
            });
        });

        mCloudSession.addLocateAnchorsCompletedListener((LocateAnchorsCompletedEvent event) -> {
            mHandler.post(() ->
            {
                mTextView.setText("Anchor located!");
                currentStep = DemoStep.DemoStepChoosing;
                EnableCorrectUIControls();
            });
        });

        mCloudSession.start();
        AnchorLocateCriteria criteria = new AnchorLocateCriteria();
        criteria.setIdentifiers(new String[]{anchorId});
        mCloudSpatialAnchorWatcher = mCloudSession.createWatcher(criteria);
    }



    private Boolean createSession() {

        boolean sessionCreated = false;
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

    private void defineConfiguration() {
        // Define new configuration and set autofocus
        Config config = new Config(mySession);
        config.setFocusMode(Config.FocusMode.AUTO);
        config.setUpdateMode(Config.UpdateMode.LATEST_CAMERA_IMAGE);


        // Set the camera configurations for the session
        mySession.setCameraConfig(mySession.getSupportedCameraConfigs().get(1));
        mArFragment.getArSceneView().setupSession(mySession);
        if (setupAugmentedImagesDb(config, mySession)) {
            Log.i(TAG, "Image database setup successful!");
        } else {
            Log.i(TAG, "Image database setup not successful!");
        }

        mySession.configure(config);
    }

    private boolean setupAugmentedImagesDb(Config config, Session session) {


        AugmentedImageDatabase augmentedImageDatabase = new AugmentedImageDatabase(session);
        try (InputStream is = getAssets().open(markerName + ".jpg")) {
            Bitmap bitmap = BitmapFactory.decodeStream(is);
            augmentedImageDatabase.addImage(markerName, bitmap);
            config.setAugmentedImageDatabase(augmentedImageDatabase);
            return true;
        } catch (IOException e) {
            Log.e(TAG, "IO Exception", e);
            return false;
        }
    }
}
