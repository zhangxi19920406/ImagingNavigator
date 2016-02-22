package com.example.imagingnavigator.function;

import android.util.Log;

/**
 * Functions to check whether is on route
 */
public class OnRouteCheck {

    private static final String TAG = Router.class.getSimpleName();

    private static final double EARTH_RADIUS = 6378137.0;
    private static final double OUT_OF_ROUTE_DIFFERENCE = 20.0;
    private static final double ARRIVE_POINT_ACCURACY = 10.0;

    private double rad(double d) {
        return d * Math.PI / 180.0;
    }

    /**
     * Get the distance between two points. (in Meter)
     *
     * @param start the first point
     * @param end the first point
     * @return the distance in meter
     */
    private double distanceOfPoints(double[] start, double[] end) {
        double radLat1 = rad(start[0]);
        double radLat2 = rad(end[0]);
        double a = radLat1 - radLat2;
        double b = rad(start[1]) - rad(end[1]);
        double s = 2 * Math.asin(Math.sqrt(Math.pow(Math.sin(a / 2), 2)
                + Math.cos(radLat1) * Math.cos(radLat2)
                * Math.pow(Math.sin(b / 2), 2)));
        s = s * EARTH_RADIUS;
        s = Math.round(s * 10000) / 10000;
        return Math.abs(s);
    }

    public boolean onRoute(double[] currentLocation, double[] start, double[] end) {
        double disStartToEnd = distanceOfPoints(start, end);
        double disToEnd = distanceOfPoints(currentLocation, end);
        double disToStart = distanceOfPoints(currentLocation, start);
        if (disStartToEnd + OUT_OF_ROUTE_DIFFERENCE > disToEnd) {
            Log.i(TAG, "Out of route");
            return false;
        } else if (disStartToEnd + OUT_OF_ROUTE_DIFFERENCE < disToStart) {
            Log.i(TAG, "Out of route");
            return false;
        }
        // Heron's formula
        double p = (disStartToEnd + disToEnd + disToStart) / 2.0;
        double s = Math.sqrt(p * (p - disStartToEnd) * (p - disToEnd) * (p - disToStart));
        double h = s * 2 / disStartToEnd;
        if (h > OUT_OF_ROUTE_DIFFERENCE) {
            Log.i(TAG, "Out of route");
            return false;
        }
        return true;
    }

    public boolean arrivePoint(double[] currentLocation, double[] end) {
        if (distanceOfPoints(currentLocation, end) < ARRIVE_POINT_ACCURACY) {
            Log.i(TAG, "Arrive this point");
            return true;
        }
        return false;
    }

}
