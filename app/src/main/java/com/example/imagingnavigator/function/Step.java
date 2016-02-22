package com.example.imagingnavigator.function;

/**
 * Created by kangkang on 12/9/15.
 */
public class Step {
    private double[] start;
    private double[] end;
    private int duration;
    private String instruction;
    private String maneuver;

    public Step(double[] start, double[] end,
                int duration, String instruction){
        this.start = new double[]{start[0], start[1]};
        this.end = new double[]{end[0], end[1]};
        this.duration = duration;
        this.instruction = instruction;
        this.maneuver = maneuver;
    }

    public void setStart(double[] loc){
        this.start[0] = loc[0];
        this.start[1] = loc[1];
    }

    public double[] getStart(){
        return this.start;
    }

    public double[] getEnd(){
        return this.end;
    }

    public int getDuration(){
        return this.duration;
    }

    public String getInstruction(){
        return this.instruction;
    }

    public String getManeuver(){
        return this.maneuver;
    }
}
