package at.ac.tuwien.wave;

import android.app.Activity;
import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.widget.TextView;

import org.pytorch.IValue;
import org.pytorch.LiteModuleLoader;
import org.pytorch.Module;
import org.pytorch.Tensor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.FloatBuffer;
import java.util.Locale;

/**
 * This Class implements functionality provided by the Team at Wav2Vec 2.0:
 *
 * @Author: Christoph Winkler
 * @Author: Team at Wav2Vec 2.0
 * @Source: <a href="https://github.com/pytorch/android-demo-app/tree/master/SpeechRecognition">wav2vec2 on Github</a> (2021-10-31)
 */
public class Wav2Vec2 extends Activity implements Runnable {

    private final Context context;
    private final TextView resultText;
    private final TextView debugText;
    private Module mModuleEncoder;

    private final static int AUDIO_LEN_IN_SECOND = 6;
    private final static int SAMPLE_RATE = 16000;
    private final static int RECORDING_LENGTH = SAMPLE_RATE * AUDIO_LEN_IN_SECOND;

    private final static String LOG_TAG = MainActivity.class.getSimpleName();

    private int mStart = 1;
    private HandlerThread mTimerThread;
    private Handler mTimerHandler;
    private final Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            mTimerHandler.postDelayed(mRunnable, 1000);

            Wav2Vec2.this.runOnUiThread(
                    () -> {
                        debugText.setText(String.format(Locale.US,"Listening - %ds left", AUDIO_LEN_IN_SECOND - mStart));
                        mStart += 1;
                    });
        }
    };

    public Wav2Vec2(Context context, TextView resultText, TextView debugText) {
        this.context = context;
        this.resultText = resultText;
        this.debugText = debugText;
    }

    /**
     * This starts the microphone recording process:
     *
     * @Author: Christoph Winkler
     * @Author: Team at Wav2Vec 2.0
     * @Source: <a href="https://github.com/pytorch/android-demo-app/tree/master/SpeechRecognition">wav2vec2 on Github</a> (2021-10-31)
     */
    protected void recognizeMicrophone() {
        debugText.setText(String.format(Locale.US,"Listening - %ds left", AUDIO_LEN_IN_SECOND));

        Thread thread = new Thread(Wav2Vec2.this);
        thread.start();

        mTimerThread = new HandlerThread("Timer");
        mTimerThread.start();
        mTimerHandler = new Handler(mTimerThread.getLooper());
        mTimerHandler.postDelayed(mRunnable, 1000);
    }

    /**
     * For properly ending the app.
     *
     * @Author: Team at Wav2Vec 2.0
     * @Source: <a href="https://github.com/pytorch/android-demo-app/tree/master/SpeechRecognition">wav2vec2 on Github</a> (2021-10-31)
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();

        stopTimerThread();
    }

    /**
     * Stops the timer thread that displays how much time to record is left.
     *
     * @Author: Team at Wav2Vec 2.0
     * @Source: <a href="https://github.com/pytorch/android-demo-app/tree/master/SpeechRecognition">wav2vec2 on Github</a> (2021-10-31)
     */
    protected void stopTimerThread() {
        mTimerThread.quitSafely();
        try {
            mTimerThread.join();
            mTimerThread = null;
            mTimerHandler = null;
            mStart = 1;
        } catch (InterruptedException e) {
            resultText.setText(e.getMessage());
        }
    }

    /**
     * Executes the microphone recording.
     *
     * @Author: Team at Wav2Vec 2.0
     * @Source: <a href="https://github.com/pytorch/android-demo-app/tree/master/SpeechRecognition">wav2vec2 on Github</a> (2021-10-31)
     */
    public void run() {
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO);

        int bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        AudioRecord record = new AudioRecord(MediaRecorder.AudioSource.DEFAULT, SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT,
                bufferSize);

        if (record.getState() != AudioRecord.STATE_INITIALIZED) {
            Log.e(LOG_TAG, "Audio Record can't initialize!");
            return;
        }
        record.startRecording();

        long shortsRead = 0;
        int recordingOffset = 0;
        short[] audioBuffer = new short[bufferSize / 2];
        short[] recordingBuffer = new short[RECORDING_LENGTH];

        while (shortsRead < RECORDING_LENGTH) {
            int numberOfShort = record.read(audioBuffer, 0, audioBuffer.length);
            shortsRead += numberOfShort;
            System.arraycopy(audioBuffer, 0, recordingBuffer, recordingOffset, numberOfShort);
            recordingOffset += numberOfShort;
        }

        record.stop();
        record.release();
        stopTimerThread();

        runOnUiThread(() -> debugText.setText(R.string.DebugText_Recognizing));

        float[] floatInputBuffer = new float[RECORDING_LENGTH];

        // feed in float values between -1.0f and 1.0f by dividing the signed 16-bit inputs.
        for (int i = 0; i < RECORDING_LENGTH; ++i) {
            floatInputBuffer[i] = recordingBuffer[i] / (float) Short.MAX_VALUE;
        }

        final String result = recognize(floatInputBuffer);

        showTranslationResult(result);
    }

    /**
     * Takes the recording and returns the models output as the result string.
     *
     * @Author: Team at Wav2Vec 2.0
     * @Source: <a href="https://github.com/pytorch/android-demo-app/tree/master/SpeechRecognition">wav2vec2 on Github</a> (2021-10-31)
     */
    private String recognize(float[] floatInputBuffer) {
        if (mModuleEncoder == null) {
            mModuleEncoder = LiteModuleLoader.load(assetFilePath(context.getApplicationContext()));
        }

        double[] wav2vecInput = new double[RECORDING_LENGTH];
        for (int n = 0; n < RECORDING_LENGTH; n++)
            wav2vecInput[n] = floatInputBuffer[n];

        FloatBuffer inTensorBuffer = Tensor.allocateFloatBuffer(RECORDING_LENGTH);
        for (double val : wav2vecInput)
            inTensorBuffer.put((float) val);

        Tensor inTensor = Tensor.fromBlob(inTensorBuffer, new long[]{1, RECORDING_LENGTH});

        return mModuleEncoder.forward(IValue.from(inTensor)).toStr();
    }

    /**
     * Reads in the model from the assets folder.
     *
     * @Author: Team at Wav2Vec 2.0
     * @Source: <a href="https://github.com/pytorch/android-demo-app/tree/master/SpeechRecognition">wav2vec2 on Github</a> (2021-10-31)
     */
    private String assetFilePath(Context context) {
        File file = new File(context.getFilesDir(), "wav2vec2.ptl");
        if (file.exists() && file.length() > 0) {
            return file.getAbsolutePath();
        }

        try (InputStream is = context.getAssets().open("wav2vec2.ptl")) {
            try (OutputStream os = new FileOutputStream(file)) {
                byte[] buffer = new byte[4 * 1024];
                int read;
                while ((read = is.read(buffer)) != -1) {
                    os.write(buffer, 0, read);
                }
                os.flush();
            }
            return file.getAbsolutePath();
        } catch (IOException e) {
            resultText.setText(e.getMessage());
        }
        return null;
    }

    /**
     * Returns the final result string to the UI.
     *
     * @Author: Christoph Winkler
     */
    private void showTranslationResult(String result) {
        result = result.charAt(0) + result.substring(1, result.length() - 1).toLowerCase() + ".";
        String finalResult = result;
        runOnUiThread(() -> resultText.setText(finalResult));

        runOnUiThread(() -> debugText.setText(R.string.DebugText_default));
    }

}
