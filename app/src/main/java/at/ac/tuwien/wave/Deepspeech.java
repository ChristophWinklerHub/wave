package at.ac.tuwien.wave;

import android.content.Context;
import android.widget.TextView;

public class Deepspeech {

    Context context;
    MainActivity mainActivity;
    TextView resultText;
    TextView debugText;

    public Deepspeech(MainActivity context, TextView resultText, TextView debugText) {
        this.context = context;
        this.mainActivity = context;
        this.resultText = resultText;
        this.debugText = debugText;
    }

    public void recognizeMicrophone() {
        String temp = "Not Implemented Yet!";
        resultText.setText(temp);
    }

}
