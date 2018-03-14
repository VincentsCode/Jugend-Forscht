package jufo.vincent.de.app;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

public class CameraActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        setTitle("Einkaufsliste scannen (PRE-ALPHA)");
        if (null == savedInstanceState) {
            getSupportFragmentManager().beginTransaction().replace(R.id.container, Camera2BasicFragment.newInstance()).commit();
        }
    }

}
