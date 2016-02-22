package com.example.imagingnavigator.function;

import android.graphics.Color;
import android.location.Location;
import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.vision.barcode.Barcode.GeoPoint;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;


/**
 * Created by zhangxi on 11/1/15.
 *
 * Router of the navigator.
 */
public class Router {

    private static final String TAG = Router.class.getSimpleName();

    private static final int GEOPOTINT_VERSION = 1;

    private final GoogleMap mMap;
    private final UpdateIntent updateIntent;

    public Router(GoogleMap map, UpdateIntent updateIntent) {
        mMap = map;
        this.updateIntent = updateIntent;
    }

    public GeoPoint locationToGeoPoint(Location location) {
        double latitude = location.getLatitude();
        double longtitude = location.getLongitude();
        return new GeoPoint(GEOPOTINT_VERSION, latitude, longtitude);
    }


    /**
     * Get the directions Url for url request from google
     *
     * @param origin the start point
     * @param dest   the end point
     * @param mode   driving
     * @return url
     */
    private String getDirectionsUrl(LatLng origin, LatLng dest, String mode) {
        // Origin of route
        String str_origin = "origin=" + origin.latitude + ","
                + origin.longitude;

        // Destination of route
        String str_dest = "destination=" + dest.latitude + "," + dest.longitude;

        // Sensor enabled
        String sensor = "sensor=false";

        // Travelling Mode
        String tmpMode = "mode=" + mode;
        //String mode = "mode=driving";

        // String waypointLatLng = "waypoints="+"40.036675"+","+"116.32885";

        // String waypoints = "waypoints=";

        //String parameters = "";
        String parameters;
        // Building the parameters to the web service

        parameters = str_origin + "&" + str_dest + "&" + sensor + "&" + tmpMode;
        // parameters = str_origin + "&" + str_dest + "&" + sensor + "&"
        // + mode+"&"+waypoints;

        // Output format
        String output = "json";
        // String output = "xml";

        // Building the url to the web service
        String url = "https://maps.googleapis.com/maps/api/directions/"
                + output + "?" + parameters;
        System.out.println("getDerectionsURL--->: " + url);
        return url;
    }

    /** A method to download json data from url */
    private String downloadUrl(String strUrl) throws IOException {
        //URL url = new URL(strUrl);
        URL url = new URL(strUrl);

        String data = "";

        // Creating an http connection to communicate with url
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

        // Connecting to url
        urlConnection.connect();

        // Reading data from url
        InputStream iStream = urlConnection.getInputStream();
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(
                    iStream));

            StringBuilder sb = new StringBuilder();

            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }

            data = sb.toString();

            br.close();

        } catch (Exception e) {
            Log.d("Exception download url", e.toString());
            iStream.close();
            urlConnection.disconnect();
        }
        iStream.close();
        urlConnection.disconnect();
        System.out.println("url:" + strUrl + "---->   downloadurl:" + data);
        return data;
    }

    /** A class to parse the Google Places in JSON format */
    private final class ParserTask extends
            AsyncTask<String, Integer, List<List<double[]>>> {

        // Parsing the data in non-ui thread
        @Override
        protected List<List<double[]>> doInBackground(
                String... jsonData) {

            JSONObject jObject;
            List<List<double[]>> routes = null;

            try {
                jObject = new JSONObject(jsonData[0]);
                DirectionsJSONParser parser = new DirectionsJSONParser();

                // Starts parsing data
                routes = parser.parse(jObject);
                System.out.println("do in background:" + routes);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return routes;
        }

        // Executes in UI thread, after the parsing process
        @Override
        protected void onPostExecute(List<List<double[]>> result) {
            ArrayList<LatLng> points;
            PolylineOptions lineOptions = null;
            //MarkerOptions markerOptions = new MarkerOptions();

            // Traversing through all the routes
            for (int i = 0; i < result.size(); i++) {
                //points = new ArrayList<LatLng>();
                points = new ArrayList<>();
                lineOptions = new PolylineOptions();

                // Fetching i-th route
                List<double[]> path = result.get(i);

                // Fetching all the points in i-th route
                for (int j = 0; j < path.size(); j++) {
                    double[] point = path.get(j);

                    double lat = point[0];
                    double lng = point[1];
                    LatLng position = new LatLng(lat, lng);

                    points.add(position);
                }

                // Adding all the points in the route to LineOptions
                lineOptions.addAll(points);
                lineOptions.width(14);

                // Changing the color polyline according to the mode
                lineOptions.color(Color.BLUE);
            }

            // Drawing polyline in the Google Map for the i-th route
            mMap.addPolyline(lineOptions);
        }
    }

    // Fetches data from url passed
    private final class DownloadTask extends AsyncTask<String, Void, String> {

        // Downloading data in non-ui thread
        @Override
        protected String doInBackground(String... url) {

            // For storing data from web service
            String data = "";

            try {
                // Fetching the data from web service
                data = downloadUrl(url[0]);
            } catch (Exception e) {
                Log.d("Background Task", e.toString());
            }
            return data;
        }

        // Executes in UI thread, after the execution of
        // doInBackground()
        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            ParserTask parserTask = new ParserTask();
            updateIntent.updateIntent(result);

            // Invokes the thread for parsing the JSON data
            parserTask.execute(result);
        }
    }

    public void drawRoute(LatLng origin, LatLng dest, String mode) {
        DownloadTask downloadTask = new DownloadTask();
        downloadTask.execute(getDirectionsUrl(origin, dest, mode));
    }

}
