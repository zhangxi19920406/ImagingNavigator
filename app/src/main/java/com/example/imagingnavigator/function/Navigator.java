package com.example.imagingnavigator.function;
import android.location.Location;
import android.util.Log;

import java.util.*;

/**
 * Created by kangkang on 11/28/15.
 * This class will provide util functions for navigation
 * 1. Calculate next step location according to current position
 *  and pre-calculated path
 * 2. Calculate next step orientation according to current position
 *  and next step position
 */
public class Navigator {
    private static final String TAG = Navigator.class.getSimpleName();
    private static final int ERROR_ALLOWED = 10;
    private static final double EARTH_RADIUS = 6378137.0;

    //Compute the dot product AB . AC
    // A->B stands for the segment, C stands for current location
//    static private double dotProduct(double[] pointA, double[] pointB, double[] pointC)
//    {
//        double[] AB = new double[2];
//        double[] BC = new double[2];
//        AB[0] = pointB[0] - pointA[0];
//        AB[1] = pointB[1] - pointA[1];
//        BC[0] = pointC[0] - pointB[0];
//        BC[1] = pointC[1] - pointB[1];
//        double dot = AB[0] * BC[0] + AB[1] * BC[1];
//
//        return dot;
//    }
//
//    //Compute the cross product AB x AC
//    // A->B stands for the segment, C stands for current location
//    static private double CrossProduct(double[] pointA, double[] pointB, double[] pointC)
//    {
//        double[] AB = new double[2];
//        double[] AC = new double[2];
//        AB[0] = pointB[0] - pointA[0];
//        AB[1] = pointB[1] - pointA[1];
//        AC[0] = pointC[0] - pointA[0];
//        AC[1] = pointC[1] - pointA[1];
//        double cross = AB[0] * AC[1] - AB[1] * AC[0];
//
//        return cross;
//    }

    //Compute the distance from A to B
    static double distance(double[] pointA, double[] pointB)
    {
        Location locA = new Location("");
        Location locB = new Location("");

        locA.setLatitude(pointA[0]);
        locA.setLongitude(pointA[1]);

        locB.setLatitude(pointB[0]);
        locB.setLongitude(pointB[1]);
        
        return locA.distanceTo(locB);
    }

    //Compute the distance from AB to C
    // A->B stands for the segment, C stands for current location
//    static private double pointToSegmentDistance(double[] pointA, double[] pointB, double[] pointC)
//    {
//        double dist = CrossProduct(pointA, pointB, pointC) / distance(pointA, pointB);
//
//        double dot1 = dotProduct(pointA, pointB, pointC);
//        if (dot1 > 0)
//            return distance(pointB, pointC);
//
//        double dot2 = dotProduct(pointB, pointA, pointC);
//        if (dot2 > 0)
//            return distance(pointA, pointC);
//
//        return Math.abs(dist);
//    }

    /*
     * 1. First we find the nearest segment among all steps in the path, we take this is the current
     *  segment user belongs to according to current position
     * 2. Then we take the second point of that segment to be that target location
     */
    static public double[] getTargetPoint(List<double[]> path,
                                   double[] curPosition){
        double min = Double.MAX_VALUE;
        double dist;
        double[] target = path.get(0);
        for(int i = 1; i < path.size(); i++){
            // For each segement [path[i-1], path[i]], calculate distance from current position.
            // Keep recording the min distance, as well as the ending point of that segment.
            dist = crossTrackDistance(path.get(i - 1), path.get(i), curPosition);
            if(dist < min){
                min = dist;
                target = path.get(i);
            }
        }
        return target;

//        if(arrived(curPosition, path.get(0))){
//            if(path.size() > 1){
//                path.remove(0);
//
//            }
//        }
//        return path.get(0);
    }

    static public Step getCurrentStep(List<Step> steps, double[] curPosition){
        double min = Double.MAX_VALUE;
        double dist;
        Step result = steps.get(0);
        Step cur;

        for(int i = 0; i < steps.size(); i++){
            cur = steps.get(i);
            dist = crossTrackDistance(cur.getStart(), cur.getEnd(), curPosition);
            Log.e(TAG, "~~~~~getCurrentStep::distance to " + (i+1) + " th step is: "
                + dist + "~~~~~~~~");
            if(dist < min){
                min = dist;
                result = cur;
            }
        }

        Log.e(TAG, "=========getCurrentStep::current step is the " + (steps.indexOf(result)+1)
            + " th step out of " + steps.size() + "==========");

        return result;

//        if(arrived(curPosition, steps.get(0).getEnd())){
//            if(steps.size() > 1){
//                steps.remove(0);
//            }
//        }
//        return steps.get(0);
    }


    /*
     * This function will return the angle from given position to the target position
     * The angle is North based and in the direction of clockwise
     */
    static public double getTargetAngle(double[] cur, double[] target){
        // Get angle from current position to target position
        // Based on the orientation of x axis
        // It's anticlockwise
        Location curLoc = new Location("");
        curLoc.setLatitude(cur[0]);
        curLoc.setLongitude(cur[1]);

        Location destLoc = new Location("");
        destLoc.setLatitude(target[0]);
        destLoc.setLongitude(target[1]);

        double angle = curLoc.bearingTo(destLoc);
//        Log.e(TAG, ">>>>>>>getTargetAngle::Current location is: " + curLoc.getLatitude() + " "  + curLoc.getLongitude() + "<<<<<<<<<<");
//        Log.e(TAG, ">>>>>>>getTargetAngle::Destination location is: " + destLoc.getLatitude() + " "  + destLoc.getLongitude() + "<<<<<<<<<<");
//        Log.e(TAG, ">>>>>>>getTargetAngle::The distance is: " + curLoc.distanceTo(destLoc) + "<<<<<<<<<<<<");
//        Log.e(TAG, ">>>>>>>getTargetAngle::The angle is: " + angle + "<<<<<<<<<<");
        return angle;
    }

    /**
     * This function will return remaining duration time for current step
     * according to given current step and current location information.
     * */
    static public int getRemainingDuration(Step step, double[] curLoc){
        int duration = step.getDuration();
        if(duration <= 60 ){
            // for short step, do not calculate
            return duration;
        }

        double percentage = distance(curLoc, step.getEnd()) /
                distance(step.getStart(), step.getEnd());

        int result = (int)(step.getDuration() * percentage);

        return result;
    }

    /**
     * This function will return the estimate time to arriving
     * with given current step, current location
     * */
    static public int getETA(List<Step> steps, Step curStep, double[] curLoc){
        int eta = getRemainingDuration(curStep, curLoc);

        int index = steps.indexOf(curStep);
        for(int i = index+1; i < steps.size(); i++){
            eta += steps.get(i).getDuration();
        }
        return eta;
    }

    static public boolean arrived(double[] cur, double[] dest){
        return distance(cur, dest) <= ERROR_ALLOWED;
    }

    // calculate angular distance by d/R
    static private double angularDistance(
            double[] pointA, double[] pointB){

        return distance(pointA, pointB)/EARTH_RADIUS;
    }

    static public double crossTrackDistance(
            double[] start, double[] end, double[] third){
        // angular distance from start to third
        double d13 = angularDistance(start, third);
        // bearing from start to third
        double b13 = getTargetAngle(start, third);
        // bearing from start to end
        double b12 = getTargetAngle(start, end);

        double distance = Math.abs(Math.asin(
                Math.sin(d13)*Math.sin(b13-b12)
        )*EARTH_RADIUS);

        return distance;
    }
}
