package com.example.android.azimuth;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.hardware.Camera;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity implements SensorEventListener {


    private Camera mCamera;
    private CameraPreview mPreview;
    private SensorManager sManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mCamera = getCameraInstance();

        // Create our Preview view and set it as the content of our activity.
        mPreview = new CameraPreview(this, mCamera);
        FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
        preview.addView(mPreview);

        sManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        // Add a listener to the Capture button
        Button captureButton = (Button) findViewById(R.id.capture_button);
        captureButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // get an image from the camera
                        mCamera.takePicture(null, null, jpegCallBack);

                        if(firstPicTaken == false) {
                            zeroAzimuth = azimuth;
                            firstPicTaken = true;
                        }

                        mCamera.startPreview();
                    }

                });

        // Add a listener to the Reset button
        Button resetButton = (Button) findViewById(R.id.reset_button);
        resetButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if(firstPicTaken == true) {
                            zeroAzimuth = 0;
                            firstPicTaken = false;
                        }

                    }
                }
        );

        // Add a listener to the viewfinder toggle on/off button
        final Button viewfinderButton = (Button) findViewById(R.id.viewfinder_button);
        final ViewFinder viewfinder = (ViewFinder) findViewById(R.id.viewfinder);
        viewfinderButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if(viewfinder.getVisibility() == View.INVISIBLE) {
                            viewfinder.setVisibility(View.VISIBLE);
                        } else {
                            viewfinder.setVisibility(View.INVISIBLE);
                        }
                    }
                }
        );
    }

    //when this Activity starts
    @Override
    protected void onResume()
    {
        super.onResume();
        /*register the sensor listener to listen to the gyroscope sensor, use the
        callbacks defined in this class, and gather the sensor information as quick
        as possible*/
        sManager.registerListener(this, sManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        super.onPause();
        releaseCamera();
    }

    private void releaseCamera(){
        if (mCamera != null){
            mCamera.stopPreview();
            mCamera.release();        // release the camera for other applications
            mCamera = null;
        }
    }

    /** A safe way to get an instance of the Camera object. */
    public static Camera getCameraInstance(){
        Camera c = null;
        try {
            c = Camera.open(); // attempt to get a Camera instance
        }
        catch (Exception e){
            // Camera is not available (in use or does not exist)
        }
        return c; // returns null if camera is unavailable
    }




    File mFile = null;
    String timeStamp;

    private void createImageFile() {
        // Find the default directory for storing images on device
        // Make new directory inside to store images
        mFile = null;
        File imageStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                                        "PolarisationImages");
        if(!imageStorageDir.exists()) {
            if(!imageStorageDir.mkdirs()) {
                Log.d("MainActivity", "Failed to create directory");
                Toast.makeText(MainActivity.this, "Failed to create directory", Toast.LENGTH_LONG).show();
                return;
            }
        }

        // Find the current date and time and use that to create a
        // collision-resistant file name for the image
        timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        mFile = new File(imageStorageDir.getPath(), timeStamp + "_" + String.valueOf(azimuth) + "_IMG" + ".jpg");
    }

    private Camera.PictureCallback jpegCallBack = new Camera.PictureCallback() {

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {

            createImageFile();
            if (mFile == null){
                Log.d("MainActivity", "Error creating media file, check storage permissions");
                Toast.makeText(MainActivity.this, "Error creating media file", Toast.LENGTH_LONG).show();

                return;
            }

            try {
                FileOutputStream outStream = new FileOutputStream(mFile);
                outStream.write(data);
                outStream.flush();
                outStream.close();

                Log.d("MainActivity", "Wrote " + data.length + " bytes to " + mFile.getAbsolutePath());

                Toast.makeText(MainActivity.this, "Image saved to " + mFile.getAbsolutePath(), Toast.LENGTH_SHORT).show();

            } catch (FileNotFoundException e) {
                Log.d("MainActivity", "File not found: " + e.getMessage());
                Toast.makeText(MainActivity.this, "File not found", Toast.LENGTH_LONG).show();
            } catch (IOException e) {
                Log.d("MainActivity", "Error accessing file: " + e.getMessage());
                Toast.makeText(MainActivity.this, "Error accessing file", Toast.LENGTH_LONG).show();
            }
        }
    };


    /**
     * Below are the methods that implement the sensor functionality that allows
     * for the orientation of the device to be measured using the accelerometer
     * and displayed on the screen.
     */

    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    float g[] = new float[3];
    float magnitude;
    int inclination;
    private int azimuth, pitch, roll;
    private int zeroAzimuth = 0;
    boolean firstPicTaken = false;

    @Override
    public void onSensorChanged(SensorEvent event)
    {
        // Retrieve and normalise the acceleration vector from the accelerometer
        g = event.values.clone();
        normalise(g);

        // Find inclination with respect to gravity
        // 90 degrees corresponds to phone being upright
        inclination = (int) Math.round(Math.toDegrees(Math.acos(g[2])));

        // Find appropriate textView for the azimuth, pitch and roll angles
        TextView azimuthText = (TextView) findViewById(R.id.azimuth_angle);
        TextView pitchText = (TextView) findViewById(R.id.pitch_angle);
        TextView rollText = (TextView) findViewById(R.id.roll_angle);

        // When the device is approximately parallel to the ground,
        // the azimuthal rotation cannot be determined accurately
        if(inclination < 25 || inclination > 155) {
            azimuthText.setText(R.string.flat);
            pitchText.setText(R.string.flat);
            rollText.setText(R.string.flat);
        }
        // Otherwise find azimuthal angle from acceleration vector components
        // and display it on the screen
        else {
            azimuth = (int) Math.round(Math.toDegrees(Math.atan2(g[0], g[1]))) - zeroAzimuth;
            pitch = (int) Math.round(Math.toDegrees(Math.atan2(g[2], g[1])));
            roll = (int) Math.round(Math.toDegrees(Math.atan2(g[0], g[2])));
            azimuthText.setText(String.valueOf(azimuth));
            pitchText.setText(String.valueOf(pitch));
            rollText.setText(String.valueOf(roll));
        }
    }

    // Method to normalise a vector of any dimensions
    private void normalise(float[] vec) {
        int length = vec.length;
        float magnitude = 0;

        for(float i : vec) {
            magnitude += i*i;
        }
        magnitude = (float) Math.sqrt(magnitude);

        for(int i = 0; i < vec.length; i++) {
            vec[i] = vec[i]/magnitude;
        }
    }

}
