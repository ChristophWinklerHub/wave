package at.ac.tuwien.wave;

import android.content.Context;
import android.widget.TextView;

import org.vosk.Model;
import org.vosk.Recognizer;
import org.vosk.android.RecognitionListener;
import org.vosk.android.SpeechService;
import org.vosk.android.SpeechStreamService;
import org.vosk.android.StorageService;

import java.io.IOException;

/**
 * This Class implements functionality provided by the Team at Vosk:
 *
 * @Author: Christoph Winkler
 * @Author: Team at Vosk
 * @Source: <a href="https://github.com/alphacep/vosk-android-demo">vosk-android-demo on Github</a> (2021-10-29)
 */
public class Vosk implements RecognitionListener {

    private final Context context;
    private final TextView resultText;
    private Model model;
    private SpeechService speechService; // for microphone input
    private SpeechStreamService speechStreamService; // for file input

    public Vosk(Context context, TextView resultText) {
        this.context = context;
        this.resultText = resultText;

        initModel();
    }

    /**
     * Before Vosk can recognize anything, the model needs to be set up.
     *
     * @Author: Team at Vosk
     * @Source: @Source: <a href="https://github.com/alphacep/vosk-android-demo">vosk-android-demo on Github</a> (2021-10-29)
     */
    private void initModel() {
        StorageService.unpack(context, "model-en-us", "model",
                (model) -> {
                    this.model = model;
                    //setUiState(STATE_READY);
                },
                (exception) -> resultText.setText(exception.getMessage()));
    }

    /**
     * Part of sentence output. //TODO: check this
     *
     * @Author: Team at Vosk
     * @Source: @Source: <a href="https://github.com/alphacep/vosk-android-demo">vosk-android-demo on Github</a> (2021-10-29)
     */
    @Override
    public void onPartialResult(String s) {
        resultText.setText(s);
    }

    /**
     * Full Sentence text output. //TODO: check this
     *
     * @Author: Team at Vosk
     * @Source: @Source: <a href="https://github.com/alphacep/vosk-android-demo">vosk-android-demo on Github</a> (2021-10-29)
     */
    @Override
    public void onResult(String s) {
        resultText.setText(s);
    }

    /**
     * Final text output.
     *
     * @Author: Team at Vosk
     * @Source: @Source: <a href="https://github.com/alphacep/vosk-android-demo">vosk-android-demo on Github</a> (2021-10-29)
     */
    @Override
    public void onFinalResult(String s) {
        resultText.setText(s);
        //setUiState(STATE_DONE);
        if (speechStreamService != null) {
            speechStreamService = null;
        }
    }

    /**
     * Displays error on TextView.
     *
     * @Author: Team at Vosk
     * @Source: @Source: <a href="https://github.com/alphacep/vosk-android-demo">vosk-android-demo on Github</a> (2021-10-29)
     */
    @Override
    public void onError(Exception e) {
        resultText.setText(e.getMessage());
    }

    /**
     * Called to set UI to back to a "ready" state after an idle period.
     *
     * @Author: Team at Vosk
     * @Source: @Source: <a href="https://github.com/alphacep/vosk-android-demo">vosk-android-demo on Github</a> (2021-10-29)
     */
    @Override
    public void onTimeout() {

    }
    /**
     * Toggles recording input via the microphone.
     *
     * @Author: Christoph Winkler
     */
    public void toggleRecording(boolean isRecordingViaMicrophone) {
        if(!isRecordingViaMicrophone){
            pause(false);
            recognizeMicrophone();
        } else {
            pause(true);
        }
    }

    /**
     * Starts recording input via the microphone.
     *
     * @Author: Team at Vosk
     * @Source: @Source: <a href="https://github.com/alphacep/vosk-android-demo">vosk-android-demo on Github</a> (2021-10-29)
     */
    private void recognizeMicrophone() {
        if (speechService != null) {
            //setUiState(STATE_DONE);
            speechService.stop();
            speechService = null;
        } else {
            //setUiState(STATE_MIC);
            try {
                Recognizer rec = new Recognizer(model, 16000.0f);
                speechService = new SpeechService(rec, 16000.0f);
                speechService.startListening(this);
            } catch (IOException e) {
                resultText.setText(e.getMessage());
            }
        }
    }

    /**
     * Toggles recording input via the microphone. Used to initially stop the recording.
     *
     * @Author: Team at Vosk
     * @Source: @Source: <a href="https://github.com/alphacep/vosk-android-demo">vosk-android-demo on Github</a> (2021-10-29)
     */
    private void pause(boolean checked) {
        if (speechService != null) {
            speechService.setPause(checked);
        }
    }

    /**
     * For properly ending the app.
     *
     * @Author: Team at Vosk
     * @Source: @Source: <a href="https://github.com/alphacep/vosk-android-demo">vosk-android-demo on Github</a> (2021-10-29)
     */
    public void onDestroy() {
        if (speechService != null) {
            speechService.stop();
            speechService.shutdown();
        }

        if (speechStreamService != null) {
            speechStreamService.stop();
        }
    }
}
