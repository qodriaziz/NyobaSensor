package com.example.nyobasensor;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.PowerManager;
import android.os.SystemClock;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Vector;

public class MainActivity extends AppCompatActivity {

    private SensorManager sensorManager;
    private Sensor mSensorProximity;
    private SensorEventListener proximitySensorListener;
    private TextView mTextSensorProximity;
    private TextView mTextSensorLight;
    private Button mSaveBtn;
    private Vector<String> mDataLog;

    private PowerManager powerManager;
    private PowerManager.WakeLock wakeLock;

    @SuppressLint("InvalidWakeLockTag")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTextSensorLight = findViewById(R.id.label_light);
        mTextSensorProximity = findViewById(R.id.label_proximity);
        mSaveBtn = findViewById(R.id.saveBtn);

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mSensorProximity = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);

        mDataLog = new Vector<>(100);
        Thread myThread = new Thread(new WriteThread());
        myThread.start();

        powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK, "WakeLockTag");

        if (mSensorProximity == null) {
            Toast.makeText(this, "Proximity Sensor is not Available", Toast.LENGTH_LONG).show();
            finish();
        }

        proximitySensorListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent sensorEvent) {
                if (sensorEvent.values[0] < mSensorProximity.getMaximumRange()) {
                    getWindow().getDecorView().setBackgroundColor(Color.RED);
                    mTextSensorLight.setText("Jarak Objek Dekat");
                } else {
                    getWindow().getDecorView().setBackgroundColor(Color.GREEN);
                    mTextSensorLight.setText("Jarak Objek Jauh");
                }

                if (sensorEvent.sensor.getType() == Sensor.TYPE_PROXIMITY) {
                    mTextSensorProximity.setText("Jarak: " + sensorEvent.values[0]);

                    // Save to CSV file immediately
                    String logText = sensorEvent.values[0] + "";
                    mDataLog.add(logText);
                    saveDataToCSV(logText);
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int i) {
                // Not needed for this example
            }
        };

        sensorManager.registerListener(proximitySensorListener, mSensorProximity, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onDestroy() {
        // Ensure the WakeLock is released when the app is stopped
        if (wakeLock.isHeld()) {
            wakeLock.release();
        }

        // Unregister the sensor listener when the app is stopped
        sensorManager.unregisterListener(proximitySensorListener);

        super.onDestroy();
    }

    private void saveDataToCSV(String text) {
        try {
            File csvFile = new File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "Proximity.csv");

            if (!csvFile.exists()) {
                csvFile.createNewFile();
                // Jika file baru dibuat, tambahkan header (opsional)
                BufferedWriter headerWriter = new BufferedWriter(new FileWriter(csvFile, true));
                headerWriter.write("Timestamp,ProximityValue"); // Sesuaikan header sesuai kebutuhan
                headerWriter.newLine();
                headerWriter.close();
            }

            // Tulis data ke file CSV
            BufferedWriter buf = new BufferedWriter(new FileWriter(csvFile, true));
            buf.append(System.currentTimeMillis() + "," + text); // Gunakan timestamp sebagai kolom pertama
            buf.newLine();
            buf.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    class WriteThread implements Runnable {
        @Override
        public void run() {
            while (true) {
                if (mDataLog.size() > 0) {
                    SystemClock.sleep(10);
                    saveDataToCSV(mDataLog.firstElement());
                    mDataLog.remove(0);
                } else {
                    SystemClock.sleep(100);
                }
            }
        }
    }
}
