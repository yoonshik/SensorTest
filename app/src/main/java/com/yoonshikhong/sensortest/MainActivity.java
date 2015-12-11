package com.yoonshikhong.sensortest;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.ActionBarActivity;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;


public class MainActivity extends ActionBarActivity implements SensorEventListener{

    private static int ACC_THRESH = 15;
    private final int TIME_DELAY = 3000;

    private String deviceId;
    private RequestQueue queue;
    private int TAKE_PHOTO_CODE = 0;
    private static int count=0;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.deviceId = generateDeviceId(); //get unique id for this android device
        queue = Volley.newRequestQueue(this); // Instantiate the RequestQueue.

        createCheckAlertTimerTask(); // checks for alert every TIME_DELAY seconds
        setUpCamera();
    }

    private void setUpCamera() {
        //here,we are making a folder named picFolder to store pics taken by the camera using this application
        final String dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + "/picFolder/";
        File newdir = new File(dir);
        newdir.mkdirs();

        Button capture = (Button) findViewById(R.id.btnCapture);
        capture.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                // here,counter will be incremented each time,and the picture taken by camera will be stored as 1.jpg,2.jpg and likewise.
                count++;
                String file = dir + count + ".jpg";
                File newfile = new File(file);
                try {
                    newfile.createNewFile();
                } catch (IOException e) {
                }

                Uri outputFileUri = Uri.fromFile(newfile);

                Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri);

                startActivityForResult(cameraIntent, TAKE_PHOTO_CODE);
            }
        });
    }

    private String generateDeviceId() {
        final TelephonyManager tm = (TelephonyManager) getBaseContext().getSystemService(Context.TELEPHONY_SERVICE);
        final String tmDevice, tmSerial, androidId;
        tmDevice = "" + tm.getDeviceId();
        tmSerial = "" + tm.getSimSerialNumber();
        androidId = "" + android.provider.Settings.Secure.getString(getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);

        UUID deviceUuid = new UUID(androidId.hashCode(), ((long)tmDevice.hashCode() << 32) | tmSerial.hashCode());
        return deviceUuid.toString();
    }

    private void createCheckAlertTimerTask() {
        new Timer().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                checkAlert();
            }
        }, 0, 3000);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == TAKE_PHOTO_CODE && resultCode == RESULT_OK) {
            Log.d("CameraDemo", "Pic saved");
        }
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() { //this activity is on focus
        super.onResume();
        SensorManager sensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE),
                SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            handleAccelerometer(event);
        }

//        if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
//            TextView accelerometer_data = (TextView) findViewById(R.id.gyroscope_data);
//            Double omegaMagnitude = getOmegaMagnitude(event);
//            String str = "Gyroscope: \n" + omegaMagnitude.toString();
//            accelerometer_data.setText(str.toCharArray(), 0, str.length());
//        }
    }

    private void handleAccelerometer(SensorEvent event) {
        TextView accelerometer_data = (TextView) findViewById(R.id.accelerometer_data);
        Double acceleration_magnitude = getAccelerationMagnitude(event);
        if (acceleration_magnitude > ACC_THRESH) {
            String acceleration_string = "Accelerometer: \n" + acceleration_magnitude.toString();
            accelerometer_data.setText(acceleration_string.toCharArray(), 0, acceleration_string.length());
            String timestamp = getCurrentTime();
            postTrigger(this, "accelerometer", acceleration_string, timestamp);

            setBackgroundColor(Color.YELLOW);
        }
    }

    private void setBackgroundColor(int color) {
        getWindow().getDecorView().setBackgroundColor(color);
    }

    static double getAccelerationMagnitude(SensorEvent event) {
        if (event.sensor.getType() != Sensor.TYPE_ACCELEROMETER) {
            throw new UnsupportedOperationException();
        }
        //event.values is a vector v = xi + yj + zk
        Float x = event.values[0];
        Float y = event.values[1];
        Float z = event.values[2];
        return Math.sqrt(x*x + y*y + z*z) - 9.8; //acceleration magnitude
    }

    public String getCurrentTime() {
        Calendar c = Calendar.getInstance();
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String formattedDate = df.format(c.getTime());
        return formattedDate;
    }

    /*
     * Omega magnitude is the rotational acceleration magnitude.
     */
    static double getOmegaMagnitude(SensorEvent event) {
        if (event.sensor.getType() != Sensor.TYPE_GYROSCOPE) {
            throw new UnsupportedOperationException();
        }
        Float x = event.values[0];
        Float y = event.values[1];
        Float z = event.values[2];
        return Math.sqrt(x*x + y*y + z*z); //omegaMagnitude
    }

    public void postTrigger(Context context, final String sensor, final String data, final String timestamp){
        RequestQueue queue = Volley.newRequestQueue(context);
        StringRequest sr = new StringRequest(Request.Method.POST,"http://alertserver-1150.appspot.com/sendtrigger", new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                TextView response_data = (TextView) findViewById(R.id.response_data);
                response_data.setText("Response is: "+ response.toString());
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
            }
        }){
            @Override
            protected Map<String,String> getParams(){

                Map<String,String> params = new HashMap<String, String>();
                params.put("user", "yoonshik");
                params.put("device_id", deviceId);
                params.put("time", timestamp);
                params.put(sensor, data);


                return params;
            }

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String,String> params = new HashMap<String, String>();
                params.put("Content-Type","application/x-www-form-urlencoded");
                return params;
            }
        };
        queue.add(sr);
    }

    public void checkAlert() {
        String url ="http://alertserver-1150.appspot.com/poll";

        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        TextView poll_data = (TextView) findViewById(R.id.poll_data);
                        if (response.equalsIgnoreCase("true")) {
                            poll_data.setText("Alert.");
                            setBackgroundColor(Color.RED);
                        } else if (response.equalsIgnoreCase("false")) {
                            poll_data.setText("No alert.");
                            setBackgroundColor(Color.WHITE);
                        } else {
                            poll_data.setText("Unexpected output: " + response);
                        }

                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                TextView poll_data = (TextView) findViewById(R.id.poll_data);
                poll_data.setText("Error");
            }
        });
        // Add the request to the RequestQueue.
        queue.add(stringRequest);
    }


    /*
     * This method is called when the accuracy level is changed. We only use
     * SensorManager.SENSOR_DELAY_NORMAL and not changing the accuracy level,
     * so we tentatively leave this method blank.
     */
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
