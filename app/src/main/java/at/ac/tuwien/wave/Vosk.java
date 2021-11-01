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
    private final MainActivity mainActivity;
    private final TextView resultText;
    private final TextView debugText;
    private Model model;
    private SpeechService speechService; // for microphone input
    private SpeechStreamService speechStreamService; // for file input
    private String partialSentence, sentences;

    public Vosk(MainActivity context, TextView resultText, TextView debugText) {
        this.context = context;
        this.mainActivity = context;
        this.resultText = resultText;
        this.debugText = debugText;

        partialSentence = "";
        sentences = "";

        initModel();
    }

    /**
     * Part of sentence output.
     *
     * @Author: Christoph Winkler
     */
    @Override
    public void onPartialResult(String s) {
        if (s != null && s.length() > 17) {
            partialSentence = sentences + s.substring(17, s.length() - 3) + " ";
            resultText.setText(partialSentence);
        }
    }

    /**
     * Full Sentence text output.
     *
     * @Author: Christoph Winkler
     */
    @Override
    public void onResult(String s) {
        if (s != null && s.length() >= 15) {
            sentences += Character.toUpperCase(s.charAt(14)) + s.substring(15, s.length() - 3) + ". ";
            resultText.setText(sentences);
        }
    }

    /**
     * Final text output. This is called once the Microphone input shall be closed.
     *
     * @Author: Team at Vosk
     * @Source: @Source: <a href="https://github.com/alphacep/vosk-android-demo">vosk-android-demo on Github</a> (2021-10-29)
     */
    @Override
    public void onFinalResult(String s) {
        resultText.setText(sentences);
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
        resultText.setText(sentences);
        if (speechStreamService != null) {
            speechStreamService = null;
        }
    }

    /**
     * Starts recording input via the microphone. Stops the recording if it is already started.
     *
     * @Author: Team at Vosk
     * @Source: @Source: <a href="https://github.com/alphacep/vosk-android-demo">vosk-android-demo on Github</a> (2021-10-29)
     */
    public void recognizeMicrophone() {
        if (speechService != null) {
            speechService.stop();
            speechService = null;
            debugText.setText(R.string.DebugText_default);
            mainActivity.enableAllUIButtons();
        } else {
            try {
                mainActivity.disableOtherUIButtons(R.id.VoskRec);
                debugText.setText(R.string.Vosk_listening);
                sentences = "";
                Recognizer rec = new Recognizer(model, 16000.0f);
                speechService = new SpeechService(rec, 16000.0f);
                speechService.startListening(this);
            } catch (IOException e) {
                resultText.setText(e.getMessage());
            }
        }
    }

    /**
     * For properly ending the app.
     *
     * @Author: Team at Vosk
     * @Source: @Source: <a href="https://github.com/alphacep/vosk-android-demo">vosk-android-demo on Github</a> (2021-10-29)
     */
    public void destroy() {
        if (speechService != null) {
            speechService.stop();
            speechService.shutdown();
        }

        if (speechStreamService != null) {
            speechStreamService.stop();
        }
    }

    /**
     * Before Vosk can recognize anything, the model needs to be set up.
     *
     * @Author: Team at Vosk
     * @Source: @Source: <a href="https://github.com/alphacep/vosk-android-demo">vosk-android-demo on Github</a> (2021-10-29)
     */
    void initModel() {
        StorageService.unpack(context, "model-en-us", "model",
                (model) -> this.model = model,
                (exception) -> resultText.setText(exception.getMessage()));
    }
}
