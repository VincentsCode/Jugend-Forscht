package jufo.vincent.de.app;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;


public class BackgroundRecognizerService extends Service {

    private SpeechRecognizerManager mSpeechManager;
    static String finalResult;

    public int onStartCommand(Intent intent, int flags, int startId) {
        return Service.START_NOT_STICKY;
    }

    public IBinder onBind(Intent intent) {
        return null;
    }

    public void onCreate() {
        super.onCreate();
        if (mSpeechManager == null) {
            setSpeechListener();
        } else if (!mSpeechManager.ismIsListening()) {
            mSpeechManager.destroy();
            setSpeechListener();
        }

        Toast.makeText(BackgroundRecognizerService.this, "Spracherkennung gestartet", Toast.LENGTH_SHORT).show();
    }

    public void onDestroy() {
        //End SpeechRecognizerManager
        if (mSpeechManager != null) {
            mSpeechManager.destroy();
            mSpeechManager = null;
        }
        super.onDestroy();
    }

    private void setSpeechListener() {
        //Create an Instance of SpeechRecognizerManager.class and listen for Results
        mSpeechManager = new SpeechRecognizerManager(this, new SpeechRecognizerManager.onResultsReady() {
            public void onResults(ArrayList<String> results) {
                if (results != null && results.size() > 0) {
                    boolean found = false;
                    for (int i = 0; i < results.size() && !found; i++) {

                        //Hotword for Emergency if enabled
                        if (getSharedPreferences("Einstellungen", 0).getBoolean("EmergencyOn", false)) {
                            if (results.get(i).toLowerCase().contains(getSharedPreferences("Einstellungen", 0).getString("Hotword", "").toLowerCase())) {
                                found = true;
                                MainActivity.sendEmergencySMS();
                                Toast.makeText(BackgroundRecognizerService.this, "Notfall-SMS wird gesendet...", Toast.LENGTH_LONG).show();
                            }
                        }


                        //Stop
                        if (results.get(i).toLowerCase().contains("Stop".toLowerCase())) {
                            found = true;
                            try {
                                MainActivity.onSend("stop");
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            Toast.makeText(BackgroundRecognizerService.this, "Roboter wird gestoppt...", Toast.LENGTH_LONG).show();
                        }

                        //Weiter oder Starten
                        if (results.get(i).toLowerCase().contains("Weiter".toLowerCase()) || results.get(i).toLowerCase().contains("Start".toLowerCase())) {
                            found = true;
                            try {
                                MainActivity.onSend("go");
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            Toast.makeText(BackgroundRecognizerService.this, "Roboter fährt los...", Toast.LENGTH_SHORT).show();
                        }

                        //Produkt hinzufügen
                        if (results.get(i).toLowerCase().contains("hinzufügen".toLowerCase())) {
                            found = true;
                            String result = results.get(i);
                            finalResult = result.replace("hinzufügen", "");
                            finalResult = finalResult.trim();
                            MainActivity.addItem(finalResult);
                        }

                        //Produkt entfernen
                        if (results.get(i).toLowerCase().contains("entfernen".toLowerCase())) {
                            found = true;
                            String result = results.get(i);
                            finalResult = result.replace("entfernen", "");
                            finalResult = finalResult.trim();
                            if (finalResult.toLowerCase().equals("alles")) {
                                MainActivity.removeAll();
                            } else {
                                MainActivity.removeItem(finalResult);
                            }
                        }

                        if (results.get(i).toLowerCase().contains("Liste leeren".toLowerCase())) {
                            found = true;
                            MainActivity.removeAll();
                        }



                        //Produkt-Liste vorlesen
                        if (results.get(i).toLowerCase().contains("Liste vorlesen".toLowerCase())) {
                            found = true;
                            MainActivity.readList();
                        }

                    }
                }
            }
        });
    }

}
