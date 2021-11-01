package at.ac.tuwien.wave;

import android.content.Context;
import android.content.Intent;
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
 */
public class AndroidSTT implements RecognitionListener {

    Context context;
    TextView resultView;
    TextView debugText;
    SpeechRecognizer speechRecognizer;
    Intent speechRecognizerIntent;

    public AndroidSTT(Context context, TextView resultText, TextView debugText) {
        this.context = context;
        this.resultView = resultText;
        this.debugText = debugText;

        // The speechRecognizerIntent was inspired by GeeksForGeeks.
        this.speechRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.US);
    }

    /**
     * Starts the microphone recognition.
     *
     * @Author: Christoph Winkler
     */
    public void recognizeMicrophone() {
        debugText.setText(R.string.Android_listening);

        if (speechRecognizer == null) {
            this.speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context);
        }

        speechRecognizer.startListening(speechRecognizerIntent);
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

    /**
     * Resets the debug text to its default state.
     *
     * @Author: Christoph Winkler
     */
    @Override
    public void onEndOfSpeech() {
        debugText.setText(R.string.DebugText_default);
    }

    /**
     * Displays the error-code on the result view.
     *
     * @Author: Christoph Winkler
     */
    @Override
    public void onError(int error) {
        String errorMessage = "An error occurred with error-code: " + error;
        resultView.setText(errorMessage);
    }

    /**
     * Displays the decoded result on the result view.
     *
     * @Author: Christoph Winkler
     */
    @Override
    public void onResults(Bundle results) {
        String result = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).get(0);
        if (result != null) {
            result = Character.toUpperCase(result.charAt(0)) + result.substring(1) + ".";
            resultView.setText(result);
        }
    }

    @Override
    public void onPartialResults(Bundle partialResults) {
    }
}
