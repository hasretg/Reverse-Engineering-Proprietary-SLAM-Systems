package com.example.reverse_engineering_proprietary_slam_systems;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public final class FileManager {
    public static void writeFrame(File folderName, String fileName, byte[] data) {
        try {
            File fileOutputName = new File(folderName, fileName);
            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(fileOutputName));
            bos.write(data);
            bos.flush();
            bos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
