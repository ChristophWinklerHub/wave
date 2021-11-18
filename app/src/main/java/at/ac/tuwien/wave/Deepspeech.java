package at.ac.tuwien.wave;

import android.app.DownloadManager;
import android.content.Context;
import android.text.method.ScrollingMovementMethod;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mozilla.speechlibrary.SpeechResultCallback;
import com.mozilla.speechlibrary.SpeechService;
import com.mozilla.speechlibrary.SpeechServiceSettings;
import com.mozilla.speechlibrary.stt.STTResult;
import com.mozilla.speechlibrary.utils.ModelUtils;
import com.mozilla.speechlibrary.utils.download.Download;
import com.mozilla.speechlibrary.utils.download.DownloadJob;
import com.mozilla.speechlibrary.utils.download.DownloadsManager;
import com.mozilla.speechlibrary.utils.storage.StorageUtils;
import com.mozilla.speechlibrary.utils.zip.UnzipCallback;
import com.mozilla.speechlibrary.utils.zip.UnzipTask;

import java.io.File;
import java.util.List;

/**
 * This Class implements functionality provided by the Team at Deepspeech:
 *
 * @Author: Christoph Winkler
 * @Author: Team at Deepspeech
 * @Source: @Source: <a href="https://github.com/mozilla/androidspeech">androidSpeech on Github</a> (2021-11-12)
 */
public class Deepspeech implements SpeechResultCallback, DownloadsManager.DownloadsListener, UnzipCallback {

    private static final @StorageUtils.StorageType
    int STORAGE_TYPE = StorageUtils.INTERNAL_STORAGE;
    private static final String modelLanguage = "en-US";

    Context context;
    MainActivity mainActivity;
    TextView resultText;
    TextView debugText;
    private final SpeechService mSpeechService;
    private final UnzipTask mUnzip;
    private final DownloadsManager mDownloadManager;
    private String sentences;

    public Deepspeech(MainActivity context, TextView resultText, TextView debugText) {
        this.context = context;
        this.mainActivity = context;
        this.resultText = resultText;
        this.debugText = debugText;
        this.sentences = "";

        mSpeechService = new SpeechService(context);
        mUnzip = new UnzipTask(context);
        mDownloadManager = new DownloadsManager(context);
        resultText.setMovementMethod(new ScrollingMovementMethod());
    }

    /**
     * This starts the microphone recording process.
     *
     * @Author: Christoph Winkler
     * @Author: Team at Deepspeech
     * @Source: @Source: <a href="https://github.com/mozilla/androidspeech">androidSpeech on Github</a> (2021-11-12)
     */
    public void recognizeMicrophone() {
        mainActivity.disableOtherUIButtons(R.id.DeepspeechRec);
        SpeechServiceSettings.Builder builder = new SpeechServiceSettings.Builder()
                .withLanguage(modelLanguage)
                .withStoreSamples(true)
                .withStoreTranscriptions(true)
                .withProductTag("product-tag")
                .withUseDeepSpeech(true);

        String modelPath = ModelUtils.modelPath(context, modelLanguage);

        if (ModelUtils.isReady(modelPath)) {
            // The model is already downloaded and unzipped
            builder.withModelPath(modelPath);
            mSpeechService.start(builder.build(), this);
        } else {
            String zipPath = ModelUtils.modelDownloadOutputPath(context, modelLanguage, STORAGE_TYPE);
            if (new File(zipPath).exists()) {
                String zipOutputPath = ModelUtils.modelPath(context, modelLanguage);
                if (zipOutputPath != null) {
                    mUnzip.start(zipPath, zipOutputPath);
                } else {
                    resultText.append("Output model path error");
                }
            } else {
                // The model needs to be downloaded
                downloadModel();
            }
        }
    }

    /**
     * Downloads the model if it has not been downloaded yet.
     *
     * @Author: Team at Deepspeech
     * @Source: @Source: <a href="https://github.com/mozilla/androidspeech">androidSpeech on Github</a> (2021-11-12)
     */
    private void downloadModel() {
        String modelUrl = ModelUtils.modelDownloadUrl(modelLanguage);

        // Check if the model is already downloaded
        Download download = mDownloadManager.getDownloads().stream()
                .filter(item ->
                        item.getStatus() == DownloadManager.STATUS_SUCCESSFUL &&
                                item.getUri().equals(modelUrl))
                .findFirst().orElse(null);
        if (download != null) {
            onDownloadCompleted(download);
        } else {
            // Check if the model is in progress
            boolean isInProgress = mDownloadManager.getDownloads().stream()
                    .anyMatch(item ->
                            item.getStatus() != DownloadManager.STATUS_FAILED &&
                                    item.getUri().equals(modelUrl));
            if (!isInProgress) {
                // Download model
                DownloadJob job = DownloadJob.create(
                        modelUrl,
                        "application/zip",
                        0,
                        null,
                        ModelUtils.modelDownloadOutputPath(context, modelLanguage, STORAGE_TYPE));
                mDownloadManager.startDownload(job, STORAGE_TYPE);
                resultText.append("Model not available, downloading...\n");
            } else {
                resultText.append("Model download already in progress\n");
            }
        }
    }

    // SpeechResultCallback

    /**
     * Notifies user that the recording has started.
     *
     * @Author: Team at Deepspeech
     * @Source: @Source: <a href="https://github.com/mozilla/androidspeech">androidSpeech on Github</a> (2021-11-12)
     */
    @Override
    public void onStartListen() {
        debugText.setText("Started to listen\n");
    }

    /**
     * Notifies user that the microphone picks up on incoming speech.
     *
     * @Author: Team at Deepspeech
     * @Source: @Source: <a href="https://github.com/mozilla/androidspeech">androidSpeech on Github</a> (2021-11-12)
     */
    @Override
    public void onMicActivity(double fftsum) {

    }

    /**
     * Notifies user that the decoding process has started.
     *
     * @Author: Team at Deepspeech
     * @Source: @Source: <a href="https://github.com/mozilla/androidspeech">androidSpeech on Github</a> (2021-11-12)
     */
    @Override
    public void onDecoding() {
        debugText.setText("Decoding... \n");
    }

    /**
     * Prints the decoded result text to the result TextView.
     *
     * @Author: Team at Deepspeech
     * @Source: @Source: <a href="https://github.com/mozilla/androidspeech">androidSpeech on Github</a> (2021-11-12)
     */
    @Override
    public void onSTTResult(@Nullable STTResult result) {
        if (result != null) {
            String message = result.mTranscription.substring(0, 1).toUpperCase() + result.mTranscription.substring(1) + ". ";
            sentences += message;
            resultText.setText(sentences);
            debugText.setText(R.string.DebugText_default);
            endService();
            mainActivity.enableAllUIButtons();
        }
    }

    /**
     * Notifies user that no voice can be detected.
     *
     * @Author: Team at Deepspeech
     * @Source: @Source: <a href="https://github.com/mozilla/androidspeech">androidSpeech on Github</a> (2021-11-12)
     */
    @Override
    public void onNoVoice() {
        resultText.append("No Voice detected\n");
    }

    /**
     * Notifies user that an error has occurred. If the error is caused by a missing model, the model gets downloaded.
     *
     * @Author: Team at Deepspeech
     * @Source: @Source: <a href="https://github.com/mozilla/androidspeech">androidSpeech on Github</a> (2021-11-12)
     */
    @Override
    public void onError(@SpeechResultCallback.ErrorType int errorType, @Nullable String error) {
        if (errorType == SPEECH_ERROR) {
            resultText.append("Speech recognition Error:" + error + " \n");
        } else if (errorType == MODEL_NOT_FOUND) {
            downloadModel();
        }
    }

    // DownloadsManager

    /**
     * Notifies user about the model downloads progress.
     *
     * @Author: Team at Deepspeech
     * @Source: @Source: <a href="https://github.com/mozilla/androidspeech">androidSpeech on Github</a> (2021-11-12)
     */
    @Override
    public void onDownloadsUpdate(@NonNull List<Download> downloads) {
        downloads.forEach(download -> {
            if (download.getStatus() == DownloadManager.STATUS_RUNNING &&
                    ModelUtils.isModelUri(download.getUri())) {
                resultText.append("Downloading " + download.getFilename() + ": " + download.getProgress() + " \n");
            }
        });
    }

    /**
     * Notifies user that the model download has been completed starts the to unzip it.
     *
     * @Author: Team at Deepspeech
     * @Source: @Source: <a href="https://github.com/mozilla/androidspeech">androidSpeech on Github</a> (2021-11-12)
     */
    @Override
    public void onDownloadCompleted(@NonNull Download download) {
        String language = ModelUtils.languageForUri(download.getUri());
        if (download.getStatus() == DownloadManager.STATUS_SUCCESSFUL &&
                ModelUtils.isModelUri(download.getUri())) {
            try {
                File file = new File(download.getOutputFile());
                if (file.exists()) {
                    String zipOutputPath = ModelUtils.modelPath(context, modelLanguage);
                    if (zipOutputPath != null) {
                        resultText.append("Download completed! \n Unzipping model.");
                        mUnzip.start(download.getOutputFile(), zipOutputPath);
                    } else {
                        resultText.append("Output model path error");
                    }
                } else {
                    mDownloadManager.removeDownload(download.getId(), true);
                }
            } catch (NullPointerException e) {
                resultText.append("Model not available: " + language);
            }
        } else {
            resultText.append("Model download error: " + language);
        }
    }

    /**
     * Notifies user that an error occurred during the model downloading.
     *
     * @Author: Team at Deepspeech
     * @Source: @Source: <a href="https://github.com/mozilla/androidspeech">androidSpeech on Github</a> (2021-11-12)
     */
    @Override
    public void onDownloadError(@NonNull String error, @NonNull String file) {
        resultText.append(error + "\n");
    }

    // UnzipTask

    /**
     * Notifies user that the model unzipping process has been started.
     *
     * @Author: Team at Deepspeech
     * @Source: @Source: <a href="https://github.com/mozilla/androidspeech">androidSpeech on Github</a> (2021-11-12)
     */
    @Override
    public void onUnzipStart(@NonNull String zipFile) {
        resultText.append("Unzipping started" + "\n");
    }

    /**
     * Notifies user about the model unzipping progress.
     *
     * @Author: Team at Deepspeech
     * @Source: @Source: <a href="https://github.com/mozilla/androidspeech">androidSpeech on Github</a> (2021-11-12)
     */
    @Override
    public void onUnzipProgress(@NonNull String zipFile, double progress) {
        resultText.append("Unzipping: " + progress + "\n");
    }

    /**
     * Notifies user that the model unzipping has been successfully completed.
     *
     * @Author: Team at Deepspeech
     * @Source: @Source: <a href="https://github.com/mozilla/androidspeech">androidSpeech on Github</a> (2021-11-12)
     */
    @Override
    public void onUnzipFinish(@NonNull String zipFile, @NonNull String outputPath) {
        resultText.append("Unzipping finished" + "\n");
        File file = new File(zipFile);
        if (file.exists()) {
            if (!file.delete()) {
                resultText.setText(R.string.DeepSpeechErrorFileDeleteFail);
            }
        }
    }

    /**
     * Notifies user that the model unzipping process has been cancelled.
     *
     * @Author: Team at Deepspeech
     * @Source: @Source: <a href="https://github.com/mozilla/androidspeech">androidSpeech on Github</a> (2021-11-12)
     */
    @Override
    public void onUnzipCancelled(@NonNull String zipFile) {
        resultText.append("Unzipping cancelled" + "\n");
    }

    /**
     * Notifies user that an error occurred during the model unzipping.
     *
     * @Author: Team at Deepspeech
     * @Source: @Source: <a href="https://github.com/mozilla/androidspeech">androidSpeech on Github</a> (2021-11-12)
     */
    @Override
    public void onUnzipError(@NonNull String zipFile, @Nullable String error) {
        resultText.append("Unzipping error: " + error + "\n");
        File file = new File(zipFile);
        if (file.exists()) {
            if (!file.delete()) {
                resultText.setText(R.string.DeepSpeechErrorFileDeleteFail);
            }
        }
    }

    /**
     * Ends Deepspeech service.
     *
     * @Author: Christoph Winkler
     * @Source: @Source: <a href="https://github.com/mozilla/androidspeech">androidSpeech on Github</a> (2021-11-12)
     */
    public void endService() {
        try {
            mSpeechService.stop();
            mDownloadManager.getDownloads().forEach(download -> {
                if (download.getStatus() != DownloadManager.STATUS_SUCCESSFUL) {
                    mDownloadManager.removeDownload(download.getId(), true);
                }
            });
            mUnzip.cancel();
            mainActivity.enableAllUIButtons();
        } catch (Exception e) {
            resultText.setText(e.getMessage());
        }
    }

    /**
     * For clearing the sentences after recording.
     *
     * @Author: Christoph Winkler
     */
    public void setSentences(String sentences) {
        this.sentences = sentences;
    }
}
