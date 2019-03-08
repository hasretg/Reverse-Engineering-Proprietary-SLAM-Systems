package com.example.reverse_engineering_proprietary_slam_systems;

import android.os.Environment;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;


public final class FileManager {

    private static String TAG = "FileManager";

    private File parentFolder;

    private File imgFolder;
    private File imgSubFolder;
    private File imgFile;
    private String poseTextFile="";

    private File poseFolder;
    private File poseFile;

    private String fileId;
    private int imgId = 1;

    private String STORAGE_PATH = Environment.getExternalStorageDirectory()
            + File.separator + "SLAM_data";

    private FileOutputStream myFileOutputStream;
    private OutputStreamWriter myOutputStreamWriter;

    public FileManager(){
        parentFolder = new File(STORAGE_PATH);
        imgFolder = new File(STORAGE_PATH, "frameFolder");
        poseFolder = new File(STORAGE_PATH, "poseFolder");
        fileId = String.valueOf(System.currentTimeMillis());
        imgSubFolder = new File(imgFolder, fileId);
        createDirectory();

        try {
            poseFile = new File(poseFolder, fileId + ".txt");
            poseFile.createNewFile();
            myFileOutputStream = new FileOutputStream(poseFile);
            myOutputStreamWriter = new OutputStreamWriter(myFileOutputStream);
            poseTextFile += "time,pos_X,pos_Y,pos_Z,q_1,q_2,q_3,q_4 \n";
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void saveImage(String imgName, byte[] data) {
        try {
            imgFile = new File(imgSubFolder, imgName + ".jpg");
            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(imgFile));
            bos.write(data);
            bos.flush();
            bos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void writeNewLine(long currTime, float[] camTrans, float[] camRot, int[] imgDim, float[] focalLength, float[] princPt, int frameId){

        String str_line = ""+currTime+","+imgDim[0]+","+imgDim[1]+","+princPt[0]+","+princPt[1]
                +","+focalLength[0]+","+focalLength[1]+","+camTrans[0]+","+camTrans[1]
                +","+camTrans[2]+","+camRot[0]+","+"img_"+frameId+".jpg";
        poseTextFile += str_line + "\n";
    }

    public String savePose() {
        if (isExternalStorageWritable()) {
            try {
                myOutputStreamWriter.write(poseTextFile);
                myOutputStreamWriter.close();
                myFileOutputStream.flush();
                myFileOutputStream.close();
                return "file saved";
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                return "File not found";
            } catch (IOException e) {
                e.printStackTrace();
                return "Error saving";
            } catch (Throwable t) {
                return "Exception: " + t.toString();
            }
        } else {
            Log.e(TAG, "External storage not available to store data!!");
        }
        return "Error in FileManager.savePose()";
    }


    private void createDirectory(){
        if(!parentFolder.exists())
            parentFolder.mkdirs();
        if(!imgFolder.exists())
            imgFolder.mkdirs();
        if(!imgSubFolder.exists())
            imgSubFolder.mkdirs();
        if(!poseFolder.exists())
            poseFolder.mkdirs();
    }

    /*
     * Checks if external storage is available for read and write
     */
    private boolean isExternalStorageWritable(){

        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

}
