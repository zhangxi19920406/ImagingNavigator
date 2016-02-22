package com.example.imagingnavigator.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.example.imagingnavigator.R;

public class MainActivity extends Activity {

    private static final String TAG = MainActivity.class.getSimpleName();

    /**
     * Start activity type for start the MapBasedViewActivity.
     */
    private static final int MAP_BASED_VIEW = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        startMapBasedView();
    }

    /**
     * The main activity will automatic start the map based navigator.
     */
    private void startMapBasedView() {
        Intent intent = new Intent();
        intent.setClass(MainActivity.this, MapBasedViewActivity.class);
        startActivityForResult(intent, MAP_BASED_VIEW);
    }



}
