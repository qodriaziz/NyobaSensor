package com.example.nyobasensor;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.os.Environment;
import android.os.PowerManager;
import android.os.SystemClock;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Vector;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import android.location.Location;


public class MainActivity extends AppCompatActivity {

    private SensorManager sensorManager;
    private Sensor mSensorProximity;
    private SensorEventListener proximitySensorListener;
    private TextView mTextSensorProximity;
    private TextView mTextSensorLight;
    private Button mSaveBtn;
    private TextView mTextLokasi;
    private Vector<String> mDataLog;

    private PowerManager powerManager;
    private PowerManager.WakeLock wakeLock;

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    @SuppressLint("InvalidWakeLockTag")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTextSensorLight = findViewById(R.id.label_light);
        mTextSensorProximity = findViewById(R.id.label_proximity);
        mSaveBtn = findViewById(R.id.findME);
        mTextLokasi = findViewById(R.id.label_lokasi);

        mDataLog = new Vector<>(100);
        Thread myThread = new Thread(new WriteThread());
        myThread.start();

        powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK, "WakeLockTag");

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mSensorProximity = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);

//        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
//                != PackageManager.PERMISSION_GRANTED ||
//                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
//                        != PackageManager.PERMISSION_GRANTED) {
//            // Jika izin belum diberikan, munculkan dialog permintaan izin
//            ActivityCompat.requestPermissions(this,
//                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
//                    LOCATION_PERMISSION_REQUEST_CODE);
//        } else {
//            // Izin sudah diberikan, lakukan tindakan yang diperlukan
//            // Misalnya, inisialisasi lokasi atau mendaftarkan listener lokasi
//            // ...
//
//        }


        if (mSensorProximity == null) {
            Toast.makeText(this, "Proximity Sensor is not Available", Toast.LENGTH_LONG).show();
            finish();
        }

        proximitySensorListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent sensorEvent) {
                if (sensorEvent.values[0] < mSensorProximity.getMaximumRange()) {
                    //getWindow().getDecorView().setBackgroundColor(Color.BLACK);
                    mTextSensorLight.setText("Jarak Objek Dekat");

                    // Matikan layar jika tidak sudah mati
                    if (!wakeLock.isHeld()) {
                        wakeLock.acquire();
                    }
                } else {
                    //getWindow().getDecorView().setBackgroundColor(Color.GREEN);
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

        // Set up the click listener for the Save button
        mSaveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Check location permission before getting the location
                checkLocationPermission();
            }
        });
    }

    private void checkLocationPermission() {
        // Check if the app has location permission
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // If permission is not granted, request it
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            // If permission is already granted, get the location
            getLocationAndAddress();
        }
    }

    // Callback for when the user responds to the permission request
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // If permission is granted, get the location
                getLocationAndAddress();
            } else {
                // If permission is denied, show a message
                Toast.makeText(this, "Location permission is required to get the address.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void getLocationAndAddress() {
        FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        try {
            fusedLocationClient.getLastLocation()
                    .addOnCompleteListener(this, new OnCompleteListener<Location>() {
                        @Override
                        public void onComplete(@NonNull Task<Location> task) {
                            if (task.isSuccessful() && task.getResult() != null) {
                                Location location = task.getResult();
                                double latitude = location.getLatitude();
                                double longitude = location.getLongitude();
                                String address = getAddressFromLocation(latitude, longitude);

                                // Display the location in mTextLokasi
                                mTextLokasi.setText("Location: " + address);
                            } else {
                                Toast.makeText(MainActivity.this, "Could not get location. Please enable GPS.", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    private String getAddressFromLocation(double latitude, double longitude) {
        // Use Geocoder to get the address from latitude and longitude
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            if (addresses != null && addresses.size() > 0) {
                Address address = addresses.get(0);
                return address.getAddressLine(0); // You can customize the address format as needed
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "Address not found";
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
                // If a new file is created, add a header (optional)
                BufferedWriter headerWriter = new BufferedWriter(new FileWriter(csvFile, true));
                headerWriter.write("Timestamp,ProximityValue,Location"); // Adjust the header as needed
                headerWriter.newLine();
                headerWriter.close();
            }

            // Write data to the CSV file
            BufferedWriter buf = new BufferedWriter(new FileWriter(csvFile, true));
            buf.append(System.currentTimeMillis() + "," + text); // Use timestamp as the first column

            // Additional location column from mTextLokasi
            buf.append(",").append(mTextLokasi.getText().toString());

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
