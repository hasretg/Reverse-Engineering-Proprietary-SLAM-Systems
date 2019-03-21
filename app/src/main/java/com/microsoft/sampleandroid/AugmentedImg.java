package com.microsoft.sampleandroid;

import com.google.ar.core.AugmentedImage;
import com.google.ar.core.Pose;

class AugmentedImg {


    private final AugmentedImage augmentedImage;
    private final Pose pose;
    private final float[] coordinates;
    private final float[] quaternion;
    private final float[] size;


    AugmentedImg(AugmentedImage augmentedImage){
        this.augmentedImage = augmentedImage;
        this.pose = augmentedImage.getCenterPose();
        this.coordinates = augmentedImage.getCenterPose().getTranslation();
        this.quaternion = augmentedImage.getCenterPose().getRotationQuaternion();
        this.size = new float[]{augmentedImage.getExtentX(), augmentedImage.getExtentZ()};
    }

    AugmentedImage getAugmentedImage() {
        return augmentedImage;
    }

    Pose getPose() {
        return pose;
    }

    float[] getCoordinates() {
        return coordinates;
    }

    float[] getQuaternion() {
        return quaternion;
    }

    float[] getSize() {
        return size;
    }
}
