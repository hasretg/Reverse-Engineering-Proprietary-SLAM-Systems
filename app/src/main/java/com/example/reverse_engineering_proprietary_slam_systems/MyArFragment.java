package com.example.reverse_engineering_proprietary_slam_systems;

import android.app.Activity;
import android.os.Handler;

import com.google.ar.core.Anchor;
import com.google.ar.core.ArCoreApk;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.TransformableNode;

public class MyArFragment extends ArFragment{


    void addTrackableNodeToScene(Anchor anchor, ModelRenderable renderable) {
        AnchorNode anchorNode = new AnchorNode(anchor);
        TransformableNode node = new TransformableNode(getTransformationSystem());
        node.setRenderable(renderable);
        node.setParent(anchorNode);
        getArSceneView().getScene().addChild(anchorNode);
        node.select();
    }

    void addAnchorNodeToScene(Anchor anchor, ModelRenderable renderable){
        AnchorNode anchorNode = new AnchorNode(anchor);
        anchorNode.setRenderable(renderable);
        this.getArSceneView().getScene().addChild(anchorNode);
    }


    void checkArCoreAvailability(Activity activity) {
        ArCoreApk.Availability availability = ArCoreApk.getInstance().checkAvailability(activity);
        if (availability.isTransient()) {
            // Re-query at 5Hz while compatibility is checked in the background.
            new Handler().postDelayed(() -> checkArCoreAvailability(activity), 200);
        }
        availability.isSupported();// indicator on the button.
// Unsupported or unknown.
    }




}
