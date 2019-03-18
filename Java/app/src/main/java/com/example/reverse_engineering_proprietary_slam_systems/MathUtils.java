package com.example.reverse_engineering_proprietary_slam_systems;

import android.util.Log;

import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.schemas.lull.Quat;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.Arrays;

class MathUtils {

    private final int maxIter; // Maximum number of iterations for initializing a pose
    private int counter = 0;

    // Final initial coordinates of the start point
    float[] initCoord;
    float[] initCoordStdDev;
    Quaternion initQuater;
    float[] initQuaterStdDev;

    Quaternion imgToCam_quat = new Quaternion(0.5f, -0.5f, -0.5f, -0.5f);
    // Class pose with 3D coordinate and 4D quaternion
    private final float[][] pose;

    /**
     * Constructor
     * @param maxIter defines the maximum initialization steps before fixing the initial pose
     */
    MathUtils(int maxIter){
        this.maxIter = maxIter;
        pose = new float[7][maxIter];
    }

    /**
     * Add coordinates to the
     * @param coord2 is the 3D coordinate for adding to a new pose
     * @param quater2 is the 4D quaternion for adding to a new pose
     */
    boolean addCoord(float[] coord2, float[] quater2){

        if(counter < maxIter) {
            pose[0][counter] = coord2[0];
            pose[1][counter] = coord2[1];
            pose[2][counter] = coord2[2];
            pose[3][counter] = quater2[0];
            pose[4][counter] = quater2[1];
            pose[5][counter] = quater2[2];
            pose[6][counter] = quater2[3];

            counter++;
            return false;
        }else {
            setMedianAndStdDev();
            return true;
        }
    }

    /**
     * Set a median pose from the array and compute its standard deviation
     */
    private void setMedianAndStdDev()
    {
        initCoord = new float[]{median(pose[0]), median(pose[1]), median(pose[2])};
        initQuater = new Quaternion(median(pose[3]), median(pose[4]), median(pose[5]), median(pose[6]));
        Log.i("MathUtils", "Init coord: "+"x: "+initCoord[0]+" y: "+initCoord[1]+" z: "+initCoord[2]);
        Log.i("MathUtils", "Init quat: "+initQuater.toString());

        initQuater = Quaternion.multiply(initQuater, imgToCam_quat);
        Log.i("MathUtils", "img_to_cam quat: "+initQuater.toString());

        //initQuater = Quaternion.multiply(initQuater, initQuater);
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

        for (float anArr : arr) {
            sum += anArr;
        }
        float mean = sum / (arr.length-1);

        for (float anArr : arr) {
            newSum += ((anArr - mean) * (anArr - mean));
        }

        return (float)(Math.sqrt((newSum) / (arr.length)));
    }

    /**
     * Calculate relative position with respect to the initial position
     * @param arr is a 3D translation vector
     * @return relative translation with respect to the initial position
     */
    float[] getRelativeTranslation(float[] arr){

        float[] tmp = new float[arr.length];
        for(int i=0; i<arr.length; i++){
            tmp[i] = (arr[i] - initCoord[i]);
        }
        // Only for testing
        tmp = arr;
       return tmp;
    }

    /**
     * Calculate relative orienation with respect to the initial orientation
     * @param arr is a 4D rotation vector (quaternion)
     * @return relative orientation with respect to the initial orientation
     */
    float[] getRelativeOrientation(float[] arr){

        Quaternion arrQuater = new Quaternion(arr[0], arr[1], arr[2], arr[3]);

        //Quaternion tmp = Quaternion.multiply(initQuater.inverted(), arrQuater);

        // Only for testing
        Quaternion tmp = arrQuater;
        return new float[]{tmp.x, tmp.y, tmp.z, tmp.w};
    }

    /**
     * Get residual of the coordinates
     * @param end is the coordinates from the same 3D point (same target)
     * @return difference of the 3D coordinate after closing loop
     */
    float[] getCoordinateDiff(float[] end){

        float[] diff = new float[end.length];
        for(int i=0; i<end.length; i++){
            diff[i] = end[i] - initCoord[i];
        }
        return diff;
    }

    /**
     * Get residuals of the quaternion
     * @param end is the quaternion from the same 3D point (same target)
     * @return difference of the 4D quaternion after closing loop
     */
    float[] getQuaternionDiff(Quaternion end){

        float[] diff = new float[4];
        diff[0] = end.x - initQuater.x;
        diff[1] = end.y - initQuater.y;
        diff[2] = end.z - initQuater.z;
        diff[3] = end.w - initQuater.w;

        return diff;
    }

    /**
     * Get mean standard deviation of each element (coordinates or quaternions)
     * @param end is a vector of the standard deviations(coordinates or quaternion)
     * @return mean standard deviation of each element after closing loop
     */
    float[] getStdDeviationElem(float[] end){
        float[] devElem = new float[end.length];
        if(devElem.length == 4){
            for(int i=0; i<end.length; i++){
                devElem[i] = (end[i] + initQuaterStdDev[i])/(2f);
            }
        }else if(devElem.length == 3){
            for(int i=0; i<end.length; i++){
                devElem[i] = (end[i] + initQuaterStdDev[i])/(2f);
            }
        }

        return devElem;

    }

    static BigDecimal round(float d, int decimalPlace) {
        BigDecimal bd = new BigDecimal(Float.toString(d));
        bd = bd.setScale(decimalPlace, BigDecimal.ROUND_HALF_UP);
        return bd;
    }
}
