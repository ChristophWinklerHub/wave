package at.ac.tuwien.wave;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSIONS_REQUEST_CODE = 1;
    private final List<String> permissions = new ArrayList<>();

    private Vosk vosk;
    private Wav2Vec2 wav2Vec2;
    private Deepspeech deepspeech;
    private AndroidSTT androidSTT;
    private TextView resultText;
    private TextView debugText;
    private boolean permissionIsGranted;
    private boolean isAndroidRecording;
    private boolean isDeepspeechRecording;
    private WordSequenceAligner werEval;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        resultText = findViewById(R.id.ResultText);
        debugText = findViewById(R.id.DebugText);
        isAndroidRecording = false;
        isDeepspeechRecording = false;
        werEval = new WordSequenceAligner();

        checkPermission();

        setupUI();
    }

    /**
     * Asks again, if permission is denied.
     *
     * @Author: Christoph Winkler
     */
    private void permissionIsDenied(boolean firstAsking) {
        permissionIsGranted = false;
        if (!firstAsking)
            Toast.makeText(this, "Wave must have permission to access the microphone.", Toast.LENGTH_LONG).show();
        if (firstAsking)
            ActivityCompat.requestPermissions(this, permissions.toArray(new String[0]), PERMISSIONS_REQUEST_CODE);
    }

    /**
     * Finishes model setup for systems if permission is granted.
     *
     * @Author: Christoph Winkler
     */
    private void permissionIsGranted() {
        permissionIsGranted = true;
        resultText.setHint(R.string.RecResult_default);
        setupSystems();
    }

    /**
     * Sets up the STT technologies.
     *
     * @Author: Christoph Winkler
     */
    private void setupSystems() {
        vosk = new Vosk(this, resultText, debugText);
        wav2Vec2 = new Wav2Vec2(this, resultText, debugText);
        deepspeech = new Deepspeech(this, resultText, debugText);
        androidSTT = new AndroidSTT(this, resultText, debugText);
    }

    /**
     * Sets up the User Interface.
     *
     * @Author: Christoph Winkler
     */
    private void setupUI() {
        findViewById(R.id.VoskRec).setOnClickListener(v -> {
            if (permissionIsGranted) {
                vosk.recognizeMicrophone();
            } else {
                permissionIsDenied(true);
            }
        });
        findViewById(R.id.Wav2vec2Rec).setOnClickListener(v -> {
            if (permissionIsGranted) {
                wav2Vec2.recognizeMicrophone();
            } else {
                permissionIsDenied(true);
            }
        });
        findViewById(R.id.DeepspeechRec).setOnClickListener(v -> {
            if (permissionIsGranted) {
                deepspeech.recognizeMicrophone(isDeepspeechRecording);
                isDeepspeechRecording = !isDeepspeechRecording;
            } else {
                permissionIsDenied(true);
            }
        });
        findViewById(R.id.AndroidRec).setOnClickListener(v -> {
            if (permissionIsGranted) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    androidSTT.recognizeMicrophone(isAndroidRecording);
                    isAndroidRecording = !isAndroidRecording;
                } else {
                    resultText.setText(R.string.Android_API_less_then_8);
                }
            } else {
                permissionIsDenied(true);
            }
        });
        findViewById(R.id.clear).setOnClickListener(v -> {
            resultText.setText("");
        });
        findViewById(R.id.wer).setOnClickListener(v -> {
            String wer = "\nWER: " + calcWER(resultText.getText().toString());
            resultText.append(wer);
        });
    }

    /**
     * Ensures safe closing of the app.
     *
     * @Author: Christoph Winkler
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();

        vosk.destroy();
        wav2Vec2.destroy();
        androidSTT.destroy();
    }

    /**
     * Initial permission-to-record-audio check. If it's not given yet, a new request will be issued
     * for which the onRequestPermissionResult will listen.
     *
     * @Author: Team at Deepspeech
     * @Source: @Source: <a href="https://github.com/mozilla/androidspeech">androidSpeech on Github</a> (2021-11-12)
     */
    private void checkPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.RECORD_AUDIO);
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        if (permissions.size() > 0) {
            permissionIsDenied(true);
        } else {
            permissionIsGranted();
        }
    }

    /**
     * Callback to the result of the permission request. If permission is given, the models are get
     * set up, if not, a text explaining how to give permission manually is being displayed.
     *
     * @Author: Christoph Winkler
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            if (grantResults.length > 0) {
                boolean allPermissionsGiven = true;
                for (int e : grantResults) {
                    if (e != PackageManager.PERMISSION_GRANTED) {
                        allPermissionsGiven = false;
                        break;
                    }
                }
                if (allPermissionsGiven) {
                    permissionIsGranted();
                } else {
                    permissionIsDenied(false);
                }
            }
        }
    }

    /**
     * Disables all buttons except for the one that was pressed.
     *
     * @Author: Christoph Winkler
     */
    public void disableOtherUIButtons(int ButtonID) {
        if (ButtonID != findViewById(R.id.DeepspeechRec).getId()) {
            findViewById(R.id.DeepspeechRec).setEnabled(false);
        }
        if (ButtonID != findViewById(R.id.VoskRec).getId()) {
            findViewById(R.id.VoskRec).setEnabled(false);
        }
        if (ButtonID != findViewById(R.id.Wav2vec2Rec).getId()) {
            findViewById(R.id.Wav2vec2Rec).setEnabled(false);
        }
        if (ButtonID != findViewById(R.id.AndroidRec).getId()) {
            findViewById(R.id.AndroidRec).setEnabled(false);
        }
        if (ButtonID != findViewById(R.id.clear).getId()) {
            findViewById(R.id.clear).setEnabled(false);
        }
        if (ButtonID != findViewById(R.id.wer).getId()) {
            findViewById(R.id.wer).setEnabled(false);
        }
    }

    /**
     * Enables all buttons.
     *
     * @Author: Christoph Winkler
     */
    public void enableAllUIButtons() {
        findViewById(R.id.DeepspeechRec).setEnabled(true);
        findViewById(R.id.VoskRec).setEnabled(true);
        findViewById(R.id.Wav2vec2Rec).setEnabled(true);
        findViewById(R.id.AndroidRec).setEnabled(true);
        findViewById(R.id.clear).setEnabled(true);
        findViewById(R.id.wer).setEnabled(true);
    }

    /**
     * Calculates the Word-Error-Rate using the WordSequenceAligner class by Brian Romanowski.
     *
     * @Author: Christoph Winkler
     * @Author: Brian Romanowski
     * @Source: @Source: <a href="https://github.com/romanows/WordSequenceAligner">WordSequenceAligner on Github</a> (2021-11-18)
     */
    public float calcWER(String input) {
        String groundTruth = "the quick brown fox jumps over the lazy dog the dog yawned and " +
                "catherine baked a cake for yelena's boston shake off car honks sounded um " +
                "through the green glassed window miss mississippi missed my message by a " +
                "minute the plane flew under the bridge but the ship sailed through the sand";

        input = input.toLowerCase().replace(".", "");

        String[] ref = groundTruth.split(" ");
        String[] hyp = input.split(" ");
        WordSequenceAligner.Alignment a = werEval.align(ref, hyp);

        return ((float) (a.numSubstitutions + a.numInsertions + a.numDeletions)) / (float) a.getReferenceLength();
    }
}