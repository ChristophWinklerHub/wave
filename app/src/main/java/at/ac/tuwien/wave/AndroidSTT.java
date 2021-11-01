package at.ac.tuwien.wave;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.widget.TextView;

import java.util.Locale;

/**
 * This Class implements the native speech to text functionality provided by Android.
 * The implementation was partially inspired by the mentioned GeeksForGeeks blog-post.
 *
 * @Author: Christoph Winkler
 * @Author: GeeksForGeeks
 * @Source: <a href="https://www.geeksforgeeks.org/offline-speech-to-text-without-any-popup-dialog-in-android/">Android STT by GeeksForGeeks</a> (2021-11-01)
 * @Source: <a href="https://developer.android.com/reference/android/speech/RecognitionListener">Android Docs</a> (2021-11-01)
 * @Source: <a href="https://developer.android.com/reference/android/speech/RecognizerIntent">Android Docs</a> (2021-11-01)
 */
public class AndroidSTT implements RecognitionListener {

    private final Context context;
    private final MainActivity mainActivity;
    private final TextView resultText;
    private final TextView debugText;
    private SpeechRecognizer speechRecognizer;
    private final Intent speechRecognizerIntent;
    private final AudioManager audioManager;
    private final int restore_volume;
    private String sentences;
    private String partialSentence;

    public AndroidSTT(MainActivity context, TextView resultText, TextView debugText) {
        this.context = context;
        this.mainActivity = context;
        this.resultText = resultText;
        this.debugText = debugText;
        this.sentences = "";
        this.partialSentence = "";

        // Muting the Recognition Speaker Noise was inspired by this comment on Github:
        // https://github.com/pbakondy/cordova-plugin-speechrecognition/issues/39#issuecomment-377675495
        audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        restore_volume = audioManager.getStreamVolume(AudioManager.STREAM_SYSTEM);
        audioManager.setStreamVolume(AudioManager.STREAM_SYSTEM, AudioManager.ADJUST_MUTE, 0);

        // The speechRecognizerIntent was partially inspired by GeeksForGeeks.
        this.speechRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.US);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1000); // 1 sec silence possible before recording ends
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1000); // 1 sec silence possible before recording ends
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 60000); // listens for at least 1 minute
    }

    /**
     * Starts the microphone recognition.
     *
     * @Author: Christoph Winkler
     */
    public void recognizeMicrophone(boolean isRecording) {
        if (!isRecording) {
            mainActivity.disableOtherUIButtons(R.id.AndroidRec);
            debugText.setText(R.string.Android_listening);

            if (speechRecognizer == null) {
                this.speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context);
            }

            speechRecognizer.setRecognitionListener(this);
            speechRecognizer.startListening(speechRecognizerIntent);
        } else {
            debugText.setText(R.string.DebugText_default);
            speechRecognizer.stopListening();
            audioManager.setStreamVolume(AudioManager.STREAM_SYSTEM, restore_volume, 0);
            mainActivity.enableAllUIButtons();
            destroy();
        }
    }

    /**
     * For properly ending the app.
     *
     * @Author: Christoph Winkler
     */
    public void destroy() {
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
    }

    @Override
    public void onReadyForSpeech(Bundle params) {
    }

    @Override
    public void onBeginningOfSpeech() {
    }

    @Override
    public void onRmsChanged(float rmsdB) {
    }

    @Override
    public void onBufferReceived(byte[] buffer) {
    }

    @Override
    public void onEvent(int eventType, Bundle params) {
    }

    @Override
    public void onEndOfSpeech() {
    }

    /**
     * Displays the error-code on the result view.
     *
     * @Author: Christoph Winkler
     */
    @Override
    public void onError(int error) {
        String errorMessage = getError(error);
        resultText.setText(errorMessage);
    }

    /**
     * Displays the decoded result on the result view.
     *
     * @Author: Christoph Winkler
     */
    @Override
    public void onResults(Bundle results) {
        String result = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).get(0);
        if (result != null && result.length() > 0) {
            result = Character.toUpperCase(result.charAt(0)) + result.substring(1) + ". ";
            sentences += result;
            resultText.setText(sentences);
        }
        recognizeMicrophone(false);
    }

    /**
     * Displays the decoded partial results on the result view.
     *
     * @Author: Christoph Winkler
     */
    @Override
    public void onPartialResults(Bundle partialResults) {
        String partialResult = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).get(0);
        if (partialResult != null && partialResult.length() > 0) {
            partialResult = Character.toUpperCase(partialResult.charAt(0)) + partialResult.substring(1);
            partialSentence = sentences + partialResult;
            resultText.setText(partialSentence);
        }
    }

    /**
     * Takes error-code and returns error message.
     *
     * @Author: Christoph Winkler
     */
    private String getError(int code) {
        switch (code) {
            case 1:
                return "Network operation timed out";
            case 2:
                return "Other network related errors";
            case 3:
                return "Audio recording error";
            case 4:
                return "Server sends error status";
            case 5:
                return "Other client side errors";
            case 6:
                return "No speech input";
            case 7:
                return "No recognition result matched";
            case 8:
                return "RecognitionService busy";
            case 9:
                return "Insufficient permissions";
            case 10:
                return "Too many requests from the same client";
            case 11:
                return "Server has been disconnected, e.g. because the app has crashed";
            case 12:
                return "Requested language is not available to be used with the current recognizer";
            case 13:
                return "Requested language is supported, but not available currently (e.g. not downloaded yet)";
            default:
                return "Error Code Unknown!";
        }
    }
}
