package com.example.reverse_engineering_proprietary_slam_systems;

import com.google.ar.core.AugmentedImage;
import com.google.ar.core.Pose;

public class AugmentedImg {


    private AugmentedImage augmentedImage;
    private Pose pose;
    private float[] coordinates;
    private float[] quaternion;
    private float[] size;


    public AugmentedImg(AugmentedImage augmentedImage){
        this.augmentedImage = augmentedImage;
        this.pose = augmentedImage.getCenterPose();
        this.coordinates = augmentedImage.getCenterPose().getTranslation();
        this.quaternion = augmentedImage.getCenterPose().getRotationQuaternion();
        this.size = new float[]{augmentedImage.getExtentX(), augmentedImage.getExtentZ()};
    }

    public AugmentedImage getAugmentedImage() {
        return augmentedImage;
    }

    public Pose getPose() {
        return pose;
    }

    public float[] getCoordinates() {
        return coordinates;
    }

    public float[] getQuaternion() {
        return quaternion;
    }

    public float[] getSize() {
        return size;
    }
}
