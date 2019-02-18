package com.example.reverse_engineering_proprietary_slam_systems;

import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.google.ar.core.Anchor;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.Color;
import com.google.ar.sceneform.rendering.MaterialFactory;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.ShapeFactory;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.core.*;
import com.google.ar.sceneform.ux.BaseArFragment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.sql.Time;


public class MainActivity extends AppCompatActivity {


    long startTime;
    File file = new File("demo222.txt");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        startTime= System.currentTimeMillis();

        ArFragment arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.arFragment);
        Button myButton = findViewById(R.id.myButton);



        arFragment.setOnTapArPlaneListener((hitResult, plane, motionEvent) -> {
            Anchor anchor = hitResult.createAnchor();

            MaterialFactory.makeOpaqueWithColor(this, new Color(android.graphics.Color.RED))
                    .thenAccept(material -> {
                        ModelRenderable renderable = ShapeFactory.makeSphere(0.1f, new Vector3(0f, 0f, 0f), material);

                        AnchorNode anchorNode = new AnchorNode(anchor);
                        anchorNode.setRenderable(renderable);
                        arFragment.getArSceneView().getScene().addChild(anchorNode);
                    });
        });

        myButton.setOnClickListener(v -> {
            Vector3 currCameraPose = arFragment.getArSceneView().getScene().getCamera().getWorldPosition();
            long currTime= System.currentTimeMillis() - startTime;
            Log.v("Time Class ", " Time value when clicking button: " + currTime);
            Log.v("pose ", "global coordinates when clicking button: " + currCameraPose.toString());
            String dataToStore = "" + currTime + ":  " + currCameraPose.toString();
            writeToFile(dataToStore);
        });
    }

    private void writeToFile(String data) {
        try {
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(new FileOutputStream(file));
            outputStreamWriter.write(data);
            outputStreamWriter.close();

        }
        catch (IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
        }
    }
}
