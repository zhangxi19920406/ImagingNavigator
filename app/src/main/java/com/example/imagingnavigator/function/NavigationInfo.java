package com.example.imagingnavigator.function;

/**
 * Created by kangkang on 12/10/15.
 */
public class NavigationInfo {
    private int duration;
    private String instruction;
    private int eta;

    public NavigationInfo(int duration, String instruction, int eta){
        this.duration = duration;
        this.instruction = instruction;
        this.eta = eta;
    }

    public int getDuaration(){
        return this.duration;
    }

    public String getInstruction(){
        return this.instruction;
    }

    public int getETA(){
        return this.eta;
    }
}
