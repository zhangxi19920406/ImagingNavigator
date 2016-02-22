package com.example.imagingnavigator.function;

import android.content.Context;
import android.hardware.Camera;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;

/**
 * Created by kangkang on 11/1/15.
 */
public class CameraView extends SurfaceView implements SurfaceHolder.Callback{

    private SurfaceHolder holder;
    private Camera camera;

    public CameraView(Context context, Camera camera){
        super(context);

        this.camera = camera;
        camera.setDisplayOrientation(90);

        // get holder and set callback, so that we can get camera data
        holder = getHolder();
        holder.addCallback(this);
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder){
        try{
            //when the surface is created, we can set the camera to draw images in this surfaceholder
            this.camera.setPreviewDisplay(holder);
            this.camera.startPreview();
        }catch(IOException e){
            e.printStackTrace();
            this.camera.stopPreview();
            this.camera.release();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h){
        // before changing orientation
        // Need to stop it, rotate it and start it again

        // check whether the surface is ready to receive camera data
        if(holder.getSurface() == null){
            return;
        }

        try{
            this.camera.stopPreview();
        }catch(Exception e){
            e.printStackTrace();
        }

        try{
            this.camera.setPreviewDisplay(holder);
            this.camera.startPreview();
        }catch(IOException e){
            e.printStackTrace();
            this.camera.stopPreview();
            this.camera.release();
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder){
        // release camera for other app
        this.camera.stopPreview();
        this.camera.release();
    }
}
