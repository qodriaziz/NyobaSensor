package com.example.nyobasensor;

import androidx.appcompat.app.AppCompatActivity;

import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private SensorManager sensorManager;

    private Sensor mSensorProximity;

    private SensorEventListener proximitySensorListener;

    private TextView mTextSensorProximity;
    private TextView mTextSensorLight;
    Button mSaveBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTextSensorLight = (TextView) findViewById(R.id.label_light);
        mTextSensorProximity = (TextView) findViewById(R.id.label_proximity);
        mSaveBtn = (Button) findViewById(R.id.saveBtn);

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mSensorProximity = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);

        mSaveBtn.setOnClickListener((view) ->{
            if (!mTextSensorProximity.getText().toString().isEmpty()){
                File file = new File(MainActivity.this.getFilesDir(), "text");
                if (!file.exists()){
                    file.mkdir();
                }
                try{
                    File gpxfile = new File(file, "sample");
                    FileWriter tulis = new FileWriter(gpxfile);
                    tulis.append(mTextSensorProximity.getText().toString() + "\t");
                    tulis.append(mTextSensorLight.getText().toString() + "\t");
                    tulis.flush();
                    tulis.close();

                    Toast.makeText(MainActivity.this, "menyimpan data", Toast.LENGTH_LONG).show();


                }catch (Exception e){

                }
            }

        });
        if(mSensorProximity == null){
            Toast.makeText(this, "Proximity Sensor is not Available", Toast.LENGTH_LONG).show();
            finish();
        }
        proximitySensorListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent sensorEvent) {
                if(sensorEvent.values[0]< mSensorProximity.getMaximumRange()){
                    getWindow().getDecorView().setBackgroundColor(Color.RED);
                    ((TextView)findViewById(R.id.label_light)).setText("Jarak Objek Deket");
                }else{
                    getWindow().getDecorView().setBackgroundColor(Color.GREEN);
                    ((TextView)findViewById(R.id.label_light)).setText("Jarak Objek Jauh");
                }

                if (sensorEvent.sensor.getType()==Sensor.TYPE_PROXIMITY){
                    ((TextView)findViewById(R.id.label_proximity)).setText("Jarak :" + " " + sensorEvent.values[0]);

                }

            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int i) {

            }

        };

        sensorManager.registerListener(proximitySensorListener, mSensorProximity, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        super.onPause();

        sensorManager.unregisterListener(proximitySensorListener);
    }
//agar file terbaca
    private String readFIle() {
        File fileEvents = new File(MainActivity.this.getFilesDir() + "/text/sample");
        StringBuilder text = new StringBuilder();
        try {
            BufferedReader br = new BufferedReader(new FileReader(fileEvents));
            String line;
            while ((line = br.readLine()) != null) {
                text.append(line);
                text.append(' ');
            }
            br.close();
        } catch (IOException e) { }
        String result = text.toString();
        return result;
    }


}