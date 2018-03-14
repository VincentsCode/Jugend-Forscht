package jufo.vincent.de.app;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public class SettingsActivity extends AppCompatActivity {

    Switch switch_speech;
    Switch switch_emergency;
    TextView txt_hotwort;
    TextView txt_number;
    TextView txt_message;
    Button btn_reset;

    SharedPreferences pref;
    SharedPreferences.Editor editor;

    Context c;

    @SuppressLint("CommitPrefEdits")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings);
        setTitle("Einstellungen");

        c = this;

        pref = getSharedPreferences("Einstellungen", 0);
        editor = pref.edit();

        switch_speech = findViewById(R.id.speechOn);
        switch_emergency = findViewById(R.id.emergencyOn);
        txt_hotwort = findViewById(R.id.hotword);
        txt_number = findViewById(R.id.emergencyNumber);
        txt_message = findViewById(R.id.emergencyText);
        btn_reset = findViewById(R.id.resetBtn);

        loadValues();

        if (switch_emergency.isChecked()) {
            LinearLayout emergencySettings = findViewById(R.id.emergencySettings);
            emergencySettings.setVisibility(View.VISIBLE);
        }


        btn_reset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                txt_hotwort.setText("");
                txt_number.setText("");
                txt_message.setText("");
                switch_speech.setChecked(true);
                switch_emergency.setChecked(false);
                saveValues();
            }
        });

        switch_emergency.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                LinearLayout emergencySettings = findViewById(R.id.emergencySettings);
                if (checked) {
                    emergencySettings.setVisibility(View.VISIBLE);
                    txt_hotwort.setText("");
                    txt_number.setText("");
                    txt_message.setText("");
                } else {
                    emergencySettings.setVisibility(View.INVISIBLE);
                    txt_hotwort.setText("");
                    txt_number.setText("");
                    txt_message.setText("");
                }

            }
        });

        switch_speech.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                saveValues();
            }
        });

    }

    public void saveValues() {
        String hotword = txt_hotwort.getText().toString();
        String number = txt_number.getText().toString();
        String message = txt_message.getText().toString();
        boolean speechChecked = switch_speech.isChecked();
        boolean emergencyChecked = switch_emergency.isChecked();

        if (hotword.trim().equals("") || number.trim().equals("") || message.trim().equals("")) {
            switch_emergency.setChecked(false);
            emergencyChecked = false;
        }

        editor.putString("Hotword", hotword);
        editor.putString("Number", number);
        editor.putString("Message", message);
        editor.putBoolean("SpeechOn", speechChecked);
        editor.putBoolean("EmergencyOn", emergencyChecked);

        editor.apply();
        editor.commit();
    }

    public void loadValues() {
        txt_hotwort.setText(pref.getString("Hotword", ""));
        txt_number.setText(pref.getString("Number", ""));
        txt_message.setText(pref.getString("Message", ""));
        switch_speech.setChecked(pref.getBoolean("SpeechOn", true));
        switch_emergency.setChecked(pref.getBoolean("EmergencyOn", false));
    }

    public double locateMelat() {
        Tracker tracker = new Tracker(this);
        double lat = tracker.getLatidude();
        if (lat == 0) {
            Toast.makeText(this, "GPS aktiviert??", Toast.LENGTH_SHORT).show();
            return 0;
        }
        else {
            return lat;
        }

    }
    public double locateMelon() {
        Tracker tracker = new Tracker(this);
        double lon = tracker.getLongitude();
        if (lon == 0) {
            Toast.makeText(this, "GPS aktiviert??", Toast.LENGTH_SHORT).show();
            return 0;
        } else {
            return lon;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveValues();
    }

    @Override
    protected void onStop() {
        super.onStop();
        saveValues();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        saveValues();
    }
}

