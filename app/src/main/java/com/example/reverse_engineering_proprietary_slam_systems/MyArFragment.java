package com.example.reverse_engineering_proprietary_slam_systems;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.shapes.Shape;
import android.nfc.Tag;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.google.ar.core.Anchor;
import com.google.ar.core.ArCoreApk;
import com.google.ar.core.AugmentedImageDatabase;
import com.google.ar.core.Config;
import com.google.ar.core.Session;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.Color;
import com.google.ar.sceneform.rendering.Material;
import com.google.ar.sceneform.rendering.MaterialFactory;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.Renderable;
import com.google.ar.sceneform.rendering.ShapeFactory;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.TransformableNode;

import java.io.IOException;
import java.io.InputStream;

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
        if (availability.isSupported()) {
            // indicator on the button.
        } else { // Unsupported or unknown.

        }
    }




}
