package com.npi.practica;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity implements SensorEventListener {
    private TextView xTextView, yTextView, zTextView;
    private SensorManager sensorManager;
    private Sensor acceleriometerSensor;
    private boolean isAccelerometerAvailable, itIsNotFirstTime = false;
    private float currentX, currentY, currentZ, lastX, lastY, lastZ;
    private float Xdifference, Ydifference;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        xTextView = findViewById(R.id.xTextView);
        yTextView = findViewById(R.id.yTextView);
        zTextView = findViewById(R.id.zTextView);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        if(sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)!= null)
        {
            acceleriometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        } else {
            xTextView.setText("SENSOR IS NOT AVAILABLE");
            isAccelerometerAvailable = false;
        }


        /*
                botones de QR Y NFC
         */


        Button buttonQR = findViewById(R.id.button_qr);
        buttonQR.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), QRActivity.class);
                startActivity(intent);
            }
        });

        Button buttonNFC = findViewById(R.id.button_nfc);
        buttonNFC.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), PaymentsActivity.class);
                startActivity(intent);
            }
        });


    }

    public void goSelectPub(View view) {
        Intent intent = new Intent(this, SelectPub.class);
        startActivity(intent);
    }


    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this, acceleriometerSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        xTextView.setText(event.values[0]+"m/s2");
        yTextView.setText(event.values[1]+"m/s2");
        zTextView.setText(event.values[2]+"m/s2");

        currentX = event.values[0];
        currentY = event.values[1];
        currentZ = event.values[2];
        if(itIsNotFirstTime)
        {
            Xdifference = Math.abs(currentX - lastX);
            Ydifference = Math.abs(currentY-lastY);

            if(Xdifference > 60){
                Intent intent = new Intent(this, SelectPub.class);
                startActivity(intent);
            }
        }
        lastX = currentX;
        lastY = currentY;
        lastZ = currentZ;
        itIsNotFirstTime = true;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}