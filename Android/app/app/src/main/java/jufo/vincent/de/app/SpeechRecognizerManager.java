package jufo.vincent.de.app;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;

import java.util.ArrayList;

class SpeechRecognizerManager {

    private AudioManager mAudioManager;
    private SpeechRecognizer mSpeechRecognizer;
    private Intent mSpeechRecognizerIntent;
    private onResultsReady mListener;

    private boolean mIsListening;
    private boolean mIsStreamSolo;


    SpeechRecognizerManager(Context context, onResultsReady listener) {
        mListener = listener;
        mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        mSpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(context);
        mSpeechRecognizer.setRecognitionListener(new SpeechRecognitionListener());
        mSpeechRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        mSpeechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        mSpeechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.getPackageName());
        startListening();
    }


    private void listenAgain() {
        if (mIsListening) {
            mIsListening = false;
            mSpeechRecognizer.cancel();
            startListening();
        }
    }

    private void startListening() {
        if (!mIsListening) {
            mIsListening = true;
            if (!mIsStreamSolo) {
                mAudioManager.setStreamMute(AudioManager.STREAM_NOTIFICATION, true);
                mAudioManager.setStreamMute(AudioManager.STREAM_ALARM, true);
                mAudioManager.setStreamMute(AudioManager.STREAM_MUSIC, true);
                mAudioManager.setStreamMute(AudioManager.STREAM_RING, true);
                mAudioManager.setStreamMute(AudioManager.STREAM_SYSTEM, true);
                mIsStreamSolo = true;
            }
            mSpeechRecognizer.startListening(mSpeechRecognizerIntent);
        }
    }


    void destroy() {
        mIsListening = false;
        if (!mIsStreamSolo) {
            mAudioManager.setStreamMute(AudioManager.STREAM_NOTIFICATION, false);
            mAudioManager.setStreamMute(AudioManager.STREAM_ALARM, false);
            mAudioManager.setStreamMute(AudioManager.STREAM_MUSIC, false);
            mAudioManager.setStreamMute(AudioManager.STREAM_RING, false);
            mAudioManager.setStreamMute(AudioManager.STREAM_SYSTEM, false);
            mIsStreamSolo = true;
        }
        if (mSpeechRecognizer != null) {
            mSpeechRecognizer.stopListening();
            mSpeechRecognizer.cancel();
            mSpeechRecognizer.destroy();
            mSpeechRecognizer = null;
        }
    }

    boolean ismIsListening() {
        return mIsListening;
    }

    private class SpeechRecognitionListener implements RecognitionListener {

        public void onBeginningOfSpeech() {

        }

        public void onBufferReceived(byte[] buffer) {

        }

        public void onEndOfSpeech() {

        }

        public synchronized void onError(int error) {
            if (error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY) {
                if (mListener != null) {
                    ArrayList<String> errorList = new ArrayList<>(1);
                    errorList.add("ERROR RECOGNIZER BUSY");
                    if (mListener != null)
                        mListener.onResults(errorList);
                }
                return;
            }

            if (error == SpeechRecognizer.ERROR_NO_MATCH) {
                if (mListener != null)
                    mListener.onResults(null);
            }

            if (error == SpeechRecognizer.ERROR_NETWORK) {
                ArrayList<String> errorList = new ArrayList<>(1);
                errorList.add("STOPPED LISTENING");
                if (mListener != null)
                    mListener.onResults(errorList);
            }
            new Handler().postDelayed(new Runnable() {
                public void run() {
                    listenAgain();
                }
            }, 100);
        }

        public void onEvent(int eventType, Bundle params) {

        }

        public void onPartialResults(Bundle partialResults) {

        }

        public void onReadyForSpeech(Bundle params) {

        }

        public void onResults(Bundle results) {
            if (results != null && mListener != null)
                mListener.onResults(results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION));
            listenAgain();
        }

        public void onRmsChanged(float rmsdB) {

        }

    }

    interface onResultsReady {

        void onResults(ArrayList<String> results);

    }

}
