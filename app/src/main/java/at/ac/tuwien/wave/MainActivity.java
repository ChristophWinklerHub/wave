package at.ac.tuwien.wave;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSIONS_REQUEST_RECORD_AUDIO = 1;

    private Vosk vosk;
    private Wav2Vec2 wav2Vec2;
    private Deepspeech deepspeech;
    private AndroidSTT androidSTT;
    private TextView resultText;
    private TextView debugText;
    private boolean permissionIsGranted;
    private boolean isAndroidRecording;
    private boolean shallWav2Vec2StartNewRecording;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        resultText = findViewById(R.id.ResultText);
        debugText = findViewById(R.id.DebugText);
        isAndroidRecording = false;
        shallWav2Vec2StartNewRecording = true;

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
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSIONS_REQUEST_RECORD_AUDIO);
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
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSIONS_REQUEST_RECORD_AUDIO);
            }
        });
        findViewById(R.id.Wav2vec2Rec).setOnClickListener(v -> {
            if (permissionIsGranted) {
                wav2Vec2.startNewRecording(shallWav2Vec2StartNewRecording);
                if(shallWav2Vec2StartNewRecording) {
                    wav2Vec2.recognizeMicrophone();
                }
                shallWav2Vec2StartNewRecording = !shallWav2Vec2StartNewRecording;
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSIONS_REQUEST_RECORD_AUDIO);
            }
        });
        findViewById(R.id.DeepspeechRec).setOnClickListener(v -> {
            if (permissionIsGranted) {
                deepspeech.recognizeMicrophone();
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSIONS_REQUEST_RECORD_AUDIO);
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
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSIONS_REQUEST_RECORD_AUDIO);
            }
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
     * @Author: Team at Vosk
     * @Source: @Source: <a href="https://github.com/alphacep/vosk-android-demo">vosk-android-demo on Github</a> (2021-10-29)
     */
    private void checkPermission() {
        // Check if user has given permission to record audio, init the model after permission is granted
        int permissionCheck = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.RECORD_AUDIO);
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            permissionIsGranted();
        } else {
            permissionIsDenied(true);
        }
    }

    /**
     * Callback to the result of the permission request. If permission is given, the models are get
     * set up, if not, a text explaining how to give permission manually is being displayed.
     *
     * @Author: Team at Vosk
     * @Source: @Source: <a href="https://github.com/alphacep/vosk-android-demo">vosk-android-demo on Github</a> (2021-10-29)
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_REQUEST_RECORD_AUDIO) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                permissionIsGranted();
            } else {
                permissionIsDenied(false);
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
    }
}