package com.example.reverse_engineering_proprietary_slam_systems;

import android.support.v4.content.res.TypedArrayUtils;
import android.util.Log;

import com.google.ar.core.Pose;
import com.google.ar.sceneform.math.Quaternion;

import java.util.Arrays;


public class MathUtils {

    private int maxIter; // Maximum number of iterations for initializing a pose
    private int counter = 0;

    // Final initial coordinates of the start point
    public float[] initCoord;
    public float[] initCoordStdDev;
    public Quaternion initQuater;
    public float[] initQuaterStdDev;

    // Class pose with 3D coordinate and 4D quaternion
    private float[][] pose;

    /**
     * Constructor
     * @param maxIter defines the maximum initialization steps before fixing the initial pose
     */
    public MathUtils(int maxIter){
        this.maxIter = maxIter;
        pose = new float[7][maxIter];
    }

    /**
     * Add coordinates to the
     * @param coord2 is the 3D coordinate for adding to a new pose
     * @param quater2 is the 4D quaternion for adding to a new pose
     */
    public boolean addCoord(float[] coord2, float[] quater2){

        if(counter < maxIter) {
            pose[0][counter] = coord2[0];
            pose[1][counter] = coord2[1];
            pose[2][counter] = coord2[2];
            pose[3][counter] = quater2[0];
            pose[5][counter] = quater2[2];
            pose[6][counter] = quater2[3];
            counter++;
            return true;
        }else {
            setMedianAndStdDev();
            return false;
        }
    }

    /**
     * Set a median pose from the array and compute its standard deviation
     */
    public void setMedianAndStdDev()
    {
        initCoord = new float[]{median(pose[0]), median(pose[1]), median(pose[2])};
        initQuater = new Quaternion(median(pose[3]), median(pose[4]), median(pose[5]), median(pose[6]));
        initQuater = initQuater.normalized();

        initCoordStdDev = new float[]{stdDeviation(pose[0]), stdDeviation(pose[1]), stdDeviation(pose[2])};
        initQuaterStdDev = new float[]{stdDeviation(pose[3]), stdDeviation(pose[4]), stdDeviation(pose[5]), stdDeviation(pose[6])};
    }

    /**
     * Calculating the median of an array
     * @param arr is an array of float
     * @return median of the input array
     */
    private float median(float[] arr){
        int n = arr.length;
        Arrays.sort(arr);

        if(n % 2 != 0)
            return arr[n/2];

        return arr[(n-1)/2] + arr[n/2] / 2.0f;
    }

    /**
     * Calculate standard deviation of an array
     * @param arr is the n-dimensional array which contains all the values
     * @return standard deviation of the input array
     */
    private float stdDeviation(float[] arr){
        float sum = 0;
        float newSum = 0;

        for (int i = 0; i<arr.length; i++){
            sum += arr[i];
        }
        float mean = sum / (arr.length-1);

        for (int j = 0; j<arr.length; j++){
            newSum += ((arr[j] - mean) * (arr[j] - mean));
        }
        float standardDev = (float)(Math.sqrt((newSum) / (arr.length)));

        return standardDev;
    }

    /**
     * Calculate relative position with respect to the initial position
     * @param arr is a 3D translation vector
     * @return relative translation with respect to the initial position
     */
    public float[] getRelativeTranslation(float[] arr){

        float[] tmp = new float[arr.length];
        for(int i=0; i<arr.length; i++){
            tmp[i] = arr[i] - initCoord[i];
        }
       return tmp;
    }

    /**
     * Calculate relative orienation with respect to the initial orientation
     * @param arr is a 4D rotation vector (quaternion)
     * @return relative orientation with respect to the initial orientation
     */
    public float[] getRelativeOrientation(float[] arr){

        Quaternion arrQuater = new Quaternion(arr[0], arr[1], arr[2], arr[3]);
        Quaternion tmp = Quaternion.multiply(arrQuater, initQuater.inverted());

        return new float[]{tmp.x, tmp.y, tmp.z, tmp.w};
    }
}
