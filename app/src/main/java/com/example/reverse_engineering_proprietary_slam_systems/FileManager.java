package com.example.reverse_engineering_proprietary_slam_systems;

import android.os.Environment;
import android.util.Log;

import com.google.ar.sceneform.math.MathHelper;

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

    private File loopClosingFile;

    private String fileId;

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
            poseTextFile += "time,sizeX,sizeY,p_x,p_y,f_x,f_y,pos_X,pos_Y,pos_Z,q_1,q_2,q_3,q_4 \n";

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

    public void writeLoopClosingResult(MathUtils start, MathUtils end, long id){

        loopClosingFile = new File(poseFolder, "loopClosing_Result.txt");
        String header;
        float[] d_c = start.getCoordinateDiff(end.initCoord);
        float[] d_q = start.getQuaternionDiff(end.initQuater);
        float[] d_std_c = start.getStdDeviationElem(end.initCoordStdDev);
        float[] d_std_q = start.getStdDeviationElem(end.initQuaterStdDev);
        String txt_data = ""+id+","+d_c[0]+","+d_c[1]+","+d_c[2]+","+d_std_c[0]+","+d_std_c[1]
                +","+d_std_c[2]+","+d_q[0]+","+d_q[1]+","+d_q[2]+","+d_q[3]+","+d_std_q[0]
                +","+d_std_q[1]+","+d_std_q[2]+","+d_std_q[3];

        if(!loopClosingFile.exists()) {
            header = "id,d_x,d_y,d_z,d_qx,d_qy,d_qz,d_qz,d_qw \n";
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
    }

    public void writePoseInfo(long currTime, float[] camTrans, float[] camRot, int[] imgDim, float[] focalLength, float[] princPt, int frameId){

        String str_line = ""+currTime+","+imgDim[0]+","+imgDim[1]+","+princPt[0]+","+princPt[1]
                +","+focalLength[0]+","+focalLength[1]+","+camTrans[0]+","+camTrans[1]
                +","+camTrans[2]+","+camRot[0]+","+camRot[1]+","+camRot[2]+","+camRot[3]
                +","+"img_"+frameId+".jpg";
        poseTextFile += str_line;
    }

    public void writePosterInfo(String name, float[] size, float[] coord, float[] quat){
        String str_line = ","+name+","+size[0]+","+size[1]+","+coord[0]+","+coord[1]+","+coord[2]
                +","+quat[0]+","+quat[1]+","+quat[2]+","+quat[3];
        poseTextFile += str_line;
    }

    public void finishTextline(){
        poseTextFile += "\n";
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
