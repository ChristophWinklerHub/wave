package at.ac.tuwien.wave;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private Vosk vosk;
    private Wav2Vec2 wav2Vec2;
    private Deepspeech deepspeech;
    private AndroidSTT androidSTT;
    private TextView resultText;
    private boolean isRecordingViaMicrophone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        resultText = findViewById(R.id.ResultText);
        isRecordingViaMicrophone = false;

        checkPermission();

        setupSystems(resultText);
        setupUI();
    }

    private void setupSystems(TextView resultText) {
        vosk = new Vosk(this, resultText);
        wav2Vec2 = new Wav2Vec2(this,resultText);
        deepspeech = new Deepspeech(this,resultText);
        androidSTT = new AndroidSTT(this,resultText);
    }

    private void setupUI() {
        findViewById(R.id.VoskRec).setOnClickListener(v -> {
            // record Audio and print to ResultView
        });
        findViewById(R.id.Wav2vec2Rec).setOnClickListener(v -> {
            // record Audio and print to ResultView
        });
        findViewById(R.id.DeepspeechRec).setOnClickListener(v -> {
            // record Audio and print to ResultView
        });
        findViewById(R.id.AndroidRec).setOnClickListener(v -> {
            vosk.toggleRecording(isRecordingViaMicrophone);
            isRecordingViaMicrophone = !isRecordingViaMicrophone;
        });
    }

    private void checkPermission() {
        if(ContextCompat.checkSelfPermission(this, "android.permission.RECORD_AUDIO") != PackageManager.PERMISSION_GRANTED) {
            resultText.setText(R.string.permission_message);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        vosk.onDestroy();
    }
}