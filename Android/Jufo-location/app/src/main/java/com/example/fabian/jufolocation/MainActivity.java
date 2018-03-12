package com.example.fabian.jufolocation;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void locateMe(View view) {
        Tracker tracker = new Tracker(this);
        double lat = tracker.getLatidude();
        double lon = tracker.getLongitude();
        if (lat == 0 && lon == 0) {
            Toast.makeText(this, "GPS activated?", Toast.LENGTH_SHORT).show();
            return;
        }
        ((TextView)findViewById(R.id.textView)).setText("latitude: " + lat);
        ((TextView)findViewById(R.id.textView2)).setText("longitude: " + lon);
    }
}