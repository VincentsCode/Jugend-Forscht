package jufo.vincent.de.app;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.widget.Toast;

class PermissionHandler {

    final static int RECORD_AUDIO = 1;

    //Check for Permission to record Audio on newer Devices
    static boolean checkPermission(Activity activity, int which) {
        if (Build.VERSION.SDK_INT < 23) {
            return true;
        } else {
            if (which == RECORD_AUDIO) {
                return ContextCompat.checkSelfPermission(activity, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
            }
        }
        return false;
    }

}
