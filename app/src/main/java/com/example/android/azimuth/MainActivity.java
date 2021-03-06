package com.example.android.azimuth;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.hardware.SensorManager;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity implements SensorEventListener, OnItemSelectedListener {
    private final String TAG = "MainActivity";

    private Camera mCamera;
    private CameraPreview mPreview;
    private SensorManager sManager;
    private static boolean exposureLowered = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mCamera = getCameraInstance();

        // Create our Preview view and set it as the content of our activity.
        mPreview = new CameraPreview(this, mCamera);
        FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
        preview.addView(mPreview);

        // Default focus setting is continuous auto
        setAutoFocus();

        // Create the sensor manager
        sManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        // Drop-down menu for focus mode selection
        Spinner focusSpinner = (Spinner) findViewById(R.id.focus_spinner);
        focusSpinner.setOnItemSelectedListener(this);

        // Add a listener to the Capture button
        Button captureButton = (Button) findViewById(R.id.capture_button);
        captureButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // get an image from the camera
                        mCamera.takePicture(null, null, jpegCallBack);

                        // zero the azimuth angle if this is the first picture taken
                        if(firstPicTaken == false) {
                            zeroAzimuth = azimuth;
                            firstPicTaken = true;
                        }
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

        if (mCamera == null) {
            mCamera = getCameraInstance();
            mPreview = new CameraPreview(this, mCamera);
            FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
            preview.addView(mPreview);
            setAutoFocus();
        }

        // register the sensor listener to listen to the accelerometer
        sManager.registerListener(this, sManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        super.onPause();
        FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
        preview.removeView(mPreview);
        releaseCamera();
    }

    private void releaseCamera(){
        if (mCamera != null){
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            mCamera.release();        // release the camera for other applications
            mCamera = null;
        }
    }

    // A safe way to get an instance of the Camera object.
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

    // Set the focus according to option selected in drop-down menu
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        // On selecting a spinner item
        String item = parent.getItemAtPosition(position).toString();

        if (item.equals("Macro")) {
            setMacroFocus();
        } else if (item.equals("Infinity")) {
            setInfinityFocus();
        } else if (item.equals("Auto")) {
            setAutoFocus();
        }

        // Showing selected spinner item
        Toast.makeText(parent.getContext(), "Focus Mode: " + item, Toast.LENGTH_SHORT).show();
    }

    public void onNothingSelected(AdapterView<?> arg0) {
        
    }


    // Set the focus mode to continuous picture auto
    private void setAutoFocus() {
        Camera.Parameters params = mCamera.getParameters();
        List<String> focusModes = params.getSupportedFocusModes();
        if (focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
            // set the focus mode
            params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            // set Camera parameters
            mCamera.setParameters(params);
        }
    }

    // Set the focus mode to macro
    private void setMacroFocus() {
        Camera.Parameters params = mCamera.getParameters();
        List<String> focusModes = params.getSupportedFocusModes();
        if (focusModes.contains(Camera.Parameters.FOCUS_MODE_MACRO)) {
            // set the focus mode
            params.setFocusMode(Camera.Parameters.FOCUS_MODE_MACRO);
            // set Camera parameters
            mCamera.setParameters(params);
        }
    }

    // Set the focus mode to infinity
    private void setInfinityFocus() {
        Camera.Parameters params = mCamera.getParameters();
        List<String> focusModes = params.getSupportedFocusModes();
        if (focusModes.contains(Camera.Parameters.FOCUS_MODE_INFINITY)) {
            // set the focus mode
            params.setFocusMode(Camera.Parameters.FOCUS_MODE_INFINITY);
            // set Camera parameters
            mCamera.setParameters(params);
        }
    }

    // Lower the exposure setting to the lowest possible one
    private void lowerExposure() {
        Camera.Parameters params = mCamera.getParameters();
        int exposureCompensation = params.getExposureCompensation();
        int minExposure = params.getMinExposureCompensation();
        int maxExposure = params.getMaxExposureCompensation();
        float exposureStep = params.getExposureCompensationStep();

        params.setExposureCompensation(minExposure);
        mCamera.setParameters(params);
        exposureLowered = true;
    }

    // Set exposure setting back to the initial one (auto)
    private void resetExposure() {
        Camera.Parameters params = mCamera.getParameters();
        params.setExposureCompensation(0);
        mCamera.setParameters(params);
        exposureLowered = false;
    }

    // File to hold the jpeg picture and its timestamp
    File mFile = null;
    String timeStamp;

    // Method that creates a file to store the image in the appropriate location
    private void createImageFile() {
        // Find the default directory for storing images on device
        // Make new directory inside to store images
        mFile = null;
        File imageStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                                        "PolarisationImages");
        if(!imageStorageDir.exists()) {
            if(!imageStorageDir.mkdirs()) {
                Log.d(TAG, "Failed to create directory");
                Toast.makeText(MainActivity.this, "Failed to create directory", Toast.LENGTH_LONG).show();
                return;
            }
        }

        // Find the current date and time and use that to create a
        // collision-resistant file name for the image
        timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        mFile = new File(imageStorageDir.getPath(), timeStamp + "_" + String.valueOf(azimuth) + "_IMG" + ".jpg");
    }

    // Callback method that is called when the capture button is pressed
    private Camera.PictureCallback jpegCallBack = new Camera.PictureCallback() {

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {

            createImageFile();
            if (mFile == null){
                Log.d(TAG, "Error creating media file, check storage permissions");
                Toast.makeText(MainActivity.this, "Error creating media file", Toast.LENGTH_LONG).show();

                return;
            }

            try {
                FileOutputStream outStream = new FileOutputStream(mFile);
                outStream.write(data);
                outStream.flush();
                outStream.close();

                Log.d(TAG, "Wrote " + data.length + " bytes to " + mFile.getAbsolutePath());

                Toast.makeText(MainActivity.this, "Image saved to " + mFile.getAbsolutePath(), Toast.LENGTH_SHORT).show();

            } catch (FileNotFoundException e) {
                Log.d(TAG, "File not found: " + e.getMessage());
                Toast.makeText(MainActivity.this, "File not found", Toast.LENGTH_LONG).show();
            } catch (IOException e) {
                Log.d(TAG, "Error accessing file: " + e.getMessage());
                Toast.makeText(MainActivity.this, "Error accessing file", Toast.LENGTH_LONG).show();
            }

            if(!exposureLowered) {
                lowerExposure();
                camera.startPreview();
                camera.takePicture(null, null, jpegCallBack);
            } else {
                resetExposure();
                camera.startPreview();
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
