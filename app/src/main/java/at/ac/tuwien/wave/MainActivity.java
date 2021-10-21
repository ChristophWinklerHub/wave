package at.ac.tuwien.wave;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Vosk vosk = new Vosk();
        AndroidSTT androidSTT = new AndroidSTT();
        Wav2Vec2 wav2Vec2 = new Wav2Vec2();
    }
}