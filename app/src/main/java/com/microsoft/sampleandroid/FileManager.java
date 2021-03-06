package com.microsoft.sampleandroid;

import android.os.Environment;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

final class FileManager {

    private final File parentFolder;

    private final File imgFolder;
    private final File imgSubFolder;
    private String poseTextFile="";

    private final File poseFolder;

    private FileOutputStream myFileOutputStream;
    private OutputStreamWriter myOutputStreamWriter;

    FileManager(String appType){
        String STORAGE_PATH = Environment.getExternalStorageDirectory()
                + File.separator + "SLAM_data";
        parentFolder = new File(STORAGE_PATH);
        imgFolder = new File(STORAGE_PATH, "frameFolder");
        poseFolder = new File(STORAGE_PATH, "poseFolder");
        String fileId = String.valueOf(System.currentTimeMillis());
        imgSubFolder = new File(imgFolder, fileId);
        createDirectory();

        try {
            File poseFile = new File(poseFolder, fileId + ".txt");
            myFileOutputStream = new FileOutputStream(poseFile);
            myOutputStreamWriter = new OutputStreamWriter(myFileOutputStream);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        if (appType.equals("LOCAL")) {
            poseTextFile += "time,sizeX,sizeY,p_x,p_y,f_x,f_y,pos_X,pos_Y,pos_Z,q_x,q_y,q_z,q_w," +
                    "marker_ID,marker_x,marker_y,marker_z,marker_qx,marker_qy,marker_qz,marker_qw," +
                    "marker_ID,marker_x,marker_y,marker_z,marker_qx,marker_qy,marker_qz,marker_qw," +
                    "marker_ID,marker_x,marker_y,marker_z,marker_qx,marker_qy,marker_qz,marker_qw,\n";
        }else if(appType.equals("CLOUD")){
            poseTextFile += "anchor_ID,anchor_x,anchor_y,anchor_z,anchor_qx,anchor_qy,anchor_qz,anchor_qw,\n";
        }
    }

    void saveImage(String imgName, byte[] data) {
        try {
            File imgFile = new File(imgSubFolder, imgName + ".jpg");
            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(imgFile));
            bos.write(data);
            bos.flush();
            bos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    float[] writeLoopClosingResult(MathUtils start, MathUtils end, long id){

        File loopClosingFile = new File(poseFolder, "loopClosing_Result.txt");
        String header;
        float[] d_c = start.getCoordinateDiff(end.initCoord);
        float[] d_q = start.getQuaternionDiff(end.initQuater);
        float[] d_std_c = start.getStdDeviationElem(end.initCoordStdDev);
        float[] d_std_q = start.getStdDeviationElem(end.initQuaterStdDev);
        String txt_data = ""+id+","+d_c[0]+","+d_c[1]+","+d_c[2]+","+d_std_c[0]+","+d_std_c[1]+","
                +d_std_c[2]
                +","+d_q[0]+","+d_q[1]
                +","+d_q[2]+","+d_q[3]
                +","+d_std_q[0]+","+d_std_q[1]
                +","+d_std_q[2]+","+d_std_q[3];

        if(!loopClosingFile.exists()) {
            header = "id,d_x,d_y,d_z,d_qx,d_qy,d_qz,d_qz,d_qw\n";
        }else{
            header = "\n";
        }
        try {
            FileOutputStream myFileOutputStream = new FileOutputStream(loopClosingFile, true);
            OutputStreamWriter myFileOutputStreamWriter = new OutputStreamWriter(myFileOutputStream);
            myFileOutputStreamWriter.write(header+txt_data);
            myFileOutputStreamWriter.flush();
            myFileOutputStreamWriter.close();
            myFileOutputStream.flush();
            myFileOutputStream.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return d_c;
    }

    void writePoseInfo(long currTime, float[] camPose, int[] imgDim, float[] focalLength, float[] princPt, int frameId){

        Log.i("MainActivity", "x: "+MathUtils.round(camPose[0], 3)+"; y: "+MathUtils.round(camPose[1], 3)+"; z: "+MathUtils.round(camPose[2], 3));
        String str_line = ""+currTime+","+imgDim[0]+","+imgDim[1]+","+princPt[0]+","+princPt[1]
                +","+focalLength[0]+","+focalLength[1]+","+MathUtils.round(camPose[0],3)
                +","+MathUtils.round(camPose[1],3)+","+MathUtils.round(camPose[2],3)
                +","+MathUtils.round(camPose[3],6)+","+MathUtils.round(camPose[4],6)
                +","+MathUtils.round(camPose[5],6)+","+MathUtils.round(camPose[6],6)
                +","+"img_"+frameId+".jpg";
        poseTextFile += str_line;
    }

    void writePosterInfo(String name, float[] size, float[] pose){
        String str_line = ","+name+","+size[0]+","+size[1]+","+MathUtils.round(pose[0],3)
                +","+MathUtils.round(pose[1],3)+","+MathUtils.round(pose[2],3)
                +","+MathUtils.round(pose[3],6)+","+MathUtils.round(pose[4],6)
                +","+MathUtils.round(pose[5],6)+","+MathUtils.round(pose[5],6);
        poseTextFile += str_line;
    }

    void finishTextline(){
        poseTextFile += "\n";
    }

    String savePose() {
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
            String TAG = "FileManager";
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
