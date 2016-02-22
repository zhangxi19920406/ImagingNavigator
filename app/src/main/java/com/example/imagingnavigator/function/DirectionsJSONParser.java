package com.example.imagingnavigator.function;

import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by kangkang on 11/25/15.
 */
public final class DirectionsJSONParser {

    private static final String TAG =  DirectionsJSONParser.class.getSimpleName();

    /**
     * Receives a JSONObject and returns a list of lists containing latitude and
     * longitude
     */
    public List<List<double[]>> parse(JSONObject jObject) {

        List<List<double[]>> routes = new ArrayList<>();
//            JSONArray jRoutes = null;
//            JSONArray jLegs = null;
//            JSONArray jSteps = null;
        JSONArray jRoutes;
        JSONArray jLegs;
        JSONArray jSteps;

        try {

            jRoutes = jObject.getJSONArray("routes");

            /** Traversing all routes */
            for (int i = 0; i < jRoutes.length(); i++) {
                jLegs = ((JSONObject) jRoutes.get(i)).getJSONArray("legs");
//                    List path = new ArrayList<HashMap<String, String>>();
                List<double[]> path = new ArrayList<>();

                /** Traversing all legs */
                for (int j = 0; j < jLegs.length(); j++) {
                    jSteps = ((JSONObject) jLegs.get(j)).getJSONArray("steps");

                    /** Traversing all steps */
                    for (int k = 0; k < jSteps.length(); k++) {
                        String polyline;
                        polyline = (String) ((JSONObject) ((JSONObject) jSteps
                                .get(k)).get("polyline")).get("points");
                        List<LatLng> list = decodePoly(polyline);

                        /** Traversing all points */
                        for (int l = 0; l < list.size(); l++) {
//                                HashMap<String, String> hm = new HashMap<String, String>();
//                                hm.put("lat",
//                                        Double.toString(((LatLng) list.get(l)).latitude));
//                                hm.put("lng",
//                                        Double.toString(((LatLng) list.get(l)).longitude));
//                                path.add(hm);

                            double[] position = new double[]{list.get(l).latitude, list.get(l).longitude};
                            path.add(position);
                        }
                    }
                    
                    routes.add(path);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return routes;
    }

    public List<Step> parseForStep(JSONObject jObj){
        List<Step> result = new ArrayList<Step>();

        JSONArray jRoutes;
        JSONArray jLegs;
        JSONArray jSteps;

        try {

            jRoutes = jObj.getJSONArray("routes");

            /** Traversing all routes */
            for (int i = 0; i < jRoutes.length(); i++) {
                jLegs = ((JSONObject) jRoutes.get(i)).getJSONArray("legs");
//                    List path = new ArrayList<HashMap<String, String>>();
                List<double[]> path = new ArrayList<>();

                /** Traversing all legs */
                for (int j = 0; j < jLegs.length(); j++) {
                    jSteps = ((JSONObject) jLegs.get(j)).getJSONArray("steps");

                    /** Traversing all steps */
                    for (int k = 0; k < jSteps.length(); k++) {
//                        String polyline;
//                        polyline = (String) ((JSONObject) ((JSONObject) jSteps
//                                .get(k)).get("polyline")).get("points");
//                        List<LatLng> list = decodePoly(polyline);
//
//                        /** Traversing all points */
//                        for (int l = 0; l < list.size(); l++) {
////
//
//                            double[] position = new double[]{list.get(l).latitude, list.get(l).longitude};
//                            path.add(position);
//                        }

                        JSONObject jStepObj = (JSONObject)jSteps.get(k);

                        double[] start = new double[2];
                        double[] end = new double[2];

                        JSONObject startLoc = (JSONObject)jStepObj.get("start_location");
                        start[0] = (Double)startLoc.get("lat");
                        start[1] = (Double)startLoc.get("lng");

                        JSONObject endLoc = (JSONObject)jStepObj.get("end_location");
                        end[0] = (Double)endLoc.get("lat");
                        end[1] = (Double)endLoc.get("lng");

                        JSONObject durationObj = (JSONObject)jStepObj.get("duration");
                        int duration = (Integer)durationObj.get("value");

//                        String maneuver = (String)jStepObj.get("maneuver");
                        String instruction = (String)jStepObj.get("html_instructions");

                        Step step = new Step(start, end, duration, instruction);
                        result.add(step);
                    }
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        return result;
    }

    /**
     * Method to decode polyline points Courtesy :
     * jeffreysambells.com/2010/05/27
     * /decoding-polylines-from-google-maps-direction-api-with-java
     * */
    private List<LatLng> decodePoly(String encoded) {

        List<LatLng> poly = new ArrayList<>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            LatLng p = new LatLng((((double) lat / 1E5)),
                    (((double) lng / 1E5)));
            poly.add(p);
        }
        return poly;
    }
}