package com.example.reverse_engineering_proprietary_slam_systems;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import java.io.Console;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        System.out.println("test");
    }
}
