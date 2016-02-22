package com.example.imagingnavigator.activities;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.imagingnavigator.R;
import com.example.imagingnavigator.function.CameraView;
import com.example.imagingnavigator.function.DirectionsJSONParser;
import com.example.imagingnavigator.function.NavigationInfo;
import com.example.imagingnavigator.function.Navigator;
import com.example.imagingnavigator.function.OnRouteCheck;
import com.example.imagingnavigator.function.Step;
import com.example.imagingnavigator.function.StringAnalyzer;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by zhangxi on 10/23/15.
 *
 * The camera based view.
 */
public class CameraBasedViewActivity extends Activity {

    private static final String TAG = CameraBasedViewActivity.class.getSimpleName();

    private static final String ROUTE_JSON_DATA = "routeJsonData";

    private Camera camera;
    private CameraView cameraView;


    private ImageView imageView;
    float angle = 0.0F;

    // Sensors
    private SensorManager sm;

    private Sensor aSensor;
    private Sensor mSensor;

    float[] accelerometerValues = new float[3];
    float[] magneticFieldValues = new float[3];

    float value = 0;

    private List<double[]> route;
    private String routeStr;

    private double[] curPosition;
    private double[] nextStep;
    private double nextStepAngle;

    private OnRouteCheck checker;

    LocationManager locationManager;
    String locProvider;
    Location curLocation;

    double curOrientation;
    List<Step> steps;

    Timer showNaviInfoTimer;
    boolean isArrival = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_based_view);


        try{
            this.camera = Camera.open();
        }catch(Exception e){
            e.printStackTrace();
        }

        if(this.camera != null){
            this.cameraView = new CameraView(this, this.camera);
            FrameLayout camera_view = (FrameLayout)findViewById(R.id.camera_view);

            camera_view.addView(this.cameraView);
        }

        imageView = (ImageView)findViewById(R.id.camera_based_vew_navigation_arrow);

        sm = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        aSensor = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mSensor = sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        sm.registerListener(myListener, aSensor, SensorManager.SENSOR_DELAY_NORMAL);
        sm.registerListener(myListener, mSensor, SensorManager.SENSOR_DELAY_NORMAL);
        //update displaying data method
        curOrientation = calculateOrientation();

        initLocProvider();


        Intent intent = getIntent();
        routeStr = intent.getStringExtra(ROUTE_JSON_DATA);
        Log.e(TAG, "=======onCreate::raw route string get from previous activity:" + routeStr + "=============");

        route = getRoute(routeStr);

        steps = getSteps(routeStr);

        // Will be used to check whether user arrived at
        checker = new OnRouteCheck();

        double[] curLoc = {40.707247, -73.990728};
        Step curStep = Navigator.getCurrentStep(steps, curLoc);
        Log.e(TAG, "=========onCreate::cur step start from : [" + curStep.getStart()[0] + "," + curStep.getStart()[1] + "]==========");
        Log.e(TAG, "=========onCreate:: with instruction: " + curStep.getInstruction() + "============");

        int index = steps.indexOf(curStep);
        Log.e(TAG, "=========onCreate::current step is the: " + index + " th step in the path============");

        Log.e(TAG, "=========onCreate::current step's total duration is: " + curStep.getDuration() + "============");
        int eta = Navigator.getRemainingDuration(curStep, curLoc);
        Log.e(TAG, "=========onCreate::current step's remaining duration is: " + eta + "============");

        showNaviInfoTimer = new Timer();
        // Show navigation information periodically
        showNaviInfoTimer.schedule(new ShowNaviInfoTimerTask(), 0, 5*1000);
    }

    @Override
    public void onPause(){
        sm.unregisterListener(myListener);
        super.onPause();
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        if(showNaviInfoTimer != null){
            showNaviInfoTimer.cancel();
        }
    }

    /**
     * Stop the camera based view navigator, and go back to the map based.
     */
    private void backMapBasedView() {
        setResult(RESULT_CANCELED);
        finish();
    }

    /**
     * Callback function for on-click map button
     * Will start map based router
     * */
    public void onClickRouterMap(View view){
        startMapBasedRouter();
    }

    // This function is just for image rotation test, need to be removed latter
    public void onClickNavigationArrow(View view){
        this.angle = (float) (Math.random() * 360);
        Log.i(TAG, "will rotate arrow by angle: " + angle + " degree.");
        this.imageView.setRotation(angle);
    }

    public void startMapBasedRouter(){
        Intent intent = new Intent();
        intent.setClass(this, MapBasedViewActivity.class);
        intent.putExtra("curLocation", curPosition);
        intent.putExtra("isArrival",isArrival);
        setResult(Activity.RESULT_OK, intent);
        finish();
    }

    /**
     * This listener will be registered to sensor manager to
     * interactive with sensor events
     * */
    final SensorEventListener myListener = new SensorEventListener() {
        public void onSensorChanged(SensorEvent sensorEvent) {

            if (sensorEvent.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
                magneticFieldValues = sensorEvent.values;
            if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
                accelerometerValues = sensorEvent.values;

            curOrientation = calculateOrientation();

            curPosition = getCurPosition();
//            Log.e(TAG,"========Current position is: [" + curPosition[0] +
//                    " " + curPosition[1] + "]============");

            nextStep = Navigator.getTargetPoint(route,curPosition);
            nextStepAngle = Navigator.getTargetAngle(curPosition, nextStep);

            imageView.setRotation((float)(nextStepAngle - curOrientation));
        }
        public void onAccuracyChanged(Sensor sensor, int accuracy) {}
    };


    private double calculateOrientation() {
        float[] values = new float[3];
        float[] R = new float[9];
        SensorManager.getRotationMatrix(R, null, accelerometerValues, magneticFieldValues);
        SensorManager.getOrientation(R, values);

        values[0] = (float) Math.toDegrees(values[0]);

        return values[0];
    }


    private void setValue(float v) {
        value = v;
    }


    private List<double[]> getRoute(String routeString){
        List<List<double[]>> route;
        // Simulate navigation from
        // https://maps.googleapis.com/maps/api/directions/json?origin=40.694533,%20-73.986865&
        //      destination=40.729846,%20-73.997482&sensor=false&mode=driving----

        // This field is for test
//        List<List<double[]>> testRoute;

//        route.add(new double[]{40.6945413, -73.98718579999999});
//        route.add(new double[]{40.6960476,-73.9871132});
//        route.add(new double[]{40.6960476,-73.9871132});
//        route.add(new double[]{40.6959467,-73.9845715});
//        route.add(new double[]{40.6959467,-73.9845715});
//        route.add(new double[]{40.6971449,-73.9849648});
//        route.add(new double[]{40.6971449,-73.9849648});
//        route.add(new double[]{40.7154786,-73.99523050000001});
//        route.add(new double[]{40.7154786,-73.99523050000001});
//        route.add(new double[]{40.7159842,-73.99544139999999});
//        route.add(new double[]{40.7159842,-73.99544139999999});
//        route.add(new double[]{40.72026,-73.9940697});
//        route.add(new double[]{40.72026,-73.9940697});
//        route.add(new double[]{40.7240559,-73.99256249999999});
//        route.add(new double[]{40.7240559,-73.99256249999999});
//        route.add(new double[]{40.72699859999999,-73.9998762});
//        route.add(new double[]{40.72699859999999,-73.9998762});
//        route.add(new double[]{40.7298372,-73.9974637});

        DirectionsJSONParser jsonParser = new DirectionsJSONParser();
        try{
            JSONObject jsonObj = new JSONObject(routeString);
            route = jsonParser.parse(jsonObj);

            Log.e(TAG, "=============getRoute::The parsed path is as following========");
            for(List<double[]> path: route){
                for(double[] point: path){
                    Log.e(TAG, "=============[" + point[0] + "," + point[1] + "]========");
                }
            }
        }catch(JSONException je){
            je.printStackTrace();
            return null;
        }

        return route.get(0);
    }

    private List<Step> getSteps(String JSONStr){
        List<Step> result = new ArrayList<Step>();

        DirectionsJSONParser jsonParser = new DirectionsJSONParser();
        try{
            JSONObject jObj = new JSONObject(JSONStr);
            result = jsonParser.parseForStep(jObj);
            Log.e(TAG, "=============getSteps::The parsed step is as following========");
            for(Step s: result){
                Log.e(TAG, "==========[" + s.getStart()[0] + "," + s.getStart()[1] + "]==========");
            }

        }catch(JSONException je){
            je.printStackTrace();
            return null;
        }
        return result;
    }

    private double[] getCurPosition(){
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return null;
        }

        curLocation = locationManager.getLastKnownLocation(locProvider);

        return new double[]{curLocation.getLatitude(), curLocation.getLongitude()};


    }

    private void initLocProvider(){
        this.locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        //getting GPS status
        boolean isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        //getting network status
        boolean isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);


        if(!isNetworkEnabled && !isGPSEnabled){
            return;
        }else if(isNetworkEnabled){
            this.locProvider = LocationManager.NETWORK_PROVIDER;
        }else{
            this.locProvider = LocationManager.GPS_PROVIDER;
        }

//        Criteria criteria = new Criteria();
//        this.bestLocProvider = locationManager.getBestProvider(criteria, false);
        this.curLocation = locationManager.getLastKnownLocation(locProvider);

        // set the listener, update the location per 3 seconds(3*1000) automatically or moving more than 8 meters
        locationManager.requestLocationUpdates(locProvider, 3 * 1000, 8, new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {

            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

                if (ContextCompat.checkSelfPermission(CameraBasedViewActivity.this,
                        Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                        ContextCompat.checkSelfPermission(CameraBasedViewActivity.this,
                                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                curLocation = locationManager.getLastKnownLocation(provider);
            }

            @Override
            public void onProviderDisabled(String provider) {

            }
        });
    }

    public NavigationInfo getNavigationInfo(List<Step> steps){
        double[] curLoc = getCurPosition();
        Log.e(TAG, "=============getNavigationInfo::current location is: [" +
                curLoc[0] + "," + curLoc[1] + "===============");

        if(checker.arrivePoint(curLoc, steps.get(steps.size()-1).getEnd())){
            return new NavigationInfo(0, "Congratulations, destination arrived!!!", 0);
        }

        Step curStep = Navigator.getCurrentStep(steps, curLoc);



        int duration = Navigator.getRemainingDuration(curStep, curLoc);
        Log.e(TAG, "=============getNavigationInfo::time remaining for current step is: " + (duration/60 + 1) + " mins ===============");

        int eta = Navigator.getETA(steps, curStep, curLoc);
        Log.e(TAG, "=============getNavigationInfo::will get to the destination in: " + (eta / 60 + 1) + " mins ===============");

        StringAnalyzer analyzer = new StringAnalyzer();

        String instruction = analyzer.delHTMLTag(analyzer.decode(curStep.getInstruction()));
        Log.e(TAG, "=============getNavigationInfo::current instruction: [" + instruction + "]===============");



        return new NavigationInfo(duration, instruction, eta);

    }

    private class ShowNaviInfoTimerTask extends TimerTask {
        @Override
        public void run(){
            new ShowNaviInfoAsyncTask().execute(steps);
//            getNavigationInfo(steps);
        }
    }

    private class ShowNaviInfoAsyncTask extends AsyncTask<List<Step>, Void, NavigationInfo> {
        @Override
        protected NavigationInfo doInBackground(List<Step>... steps){
            return getNavigationInfo(steps[0]);
        }

        @Override
        protected void onPostExecute(NavigationInfo result) {

            /**
             * All information need to be shown on camera screen can get from result
             * Result is of NavigationInfo type
             * */
            TextView camera_location_info = (TextView) findViewById(R.id.camera_location_info);
            TextView location_duration = (TextView) findViewById(R.id.location_duration);
            TextView camera_ETA = (TextView) findViewById(R.id.camera_ETA);

            camera_location_info.setText(result.getInstruction());

            // arrive destination
            // cancel listener
            if (result.getETA() == 0) {
                sm.unregisterListener(myListener);
                // set arrow to be un-visible
                ImageView arrow = (ImageView)findViewById(R.id.camera_based_vew_navigation_arrow);
                arrow.setVisibility(View.INVISIBLE);
                showNaviInfoTimer.cancel();

                location_duration.setText("0 mins last");
                camera_ETA.setText("Total time: 0 mins");
                isArrival = true;
            }else{
                location_duration.setText(String.valueOf(result.getDuaration()/60 +1)+" mins last");
                camera_ETA.setText("Total time: " + String.valueOf(result.getETA()/60 +1) + " mins");
            }
        }
    }
}