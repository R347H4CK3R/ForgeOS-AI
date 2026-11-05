package dev.forgeos.ai;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/** Wi-Fi-only starter download. A successful DownloadManager job still needs verification. */
public final class ModelManagerActivity extends Activity {
    static final String MODEL_URL = "https://huggingface.co/bartowski/SmolLM2-135M-Instruct-GGUF/resolve/09816acd5d99df7be770d85ea30822623dab342c/SmolLM2-135M-Instruct-Q4_K_M.gguf";
    static final long MODEL_BYTES = 105454432L;
    static final String MODEL_SHA = "2e8040ceae7815abe0dcb3540b9995eaa1fa0d2ca9e797d0a635ae4433c68c2d";
    private static final String PREFS = "starter-model-manager";
    private static final ExecutorService VERIFIER = Executors.newSingleThreadExecutor();
    private static Verification verification;
    private SharedPreferences preferences;
    private DownloadManager downloads;
    private TextView status, location;
    private ProgressBar progress;
    private Button start, cancel, use;
    private File verifiedFile;
    private Verification verifiedResult;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean resumed;
    private final Runnable poll = new Runnable() {
        @Override public void run() {
            if (!resumed) return;
            refresh();
            handler.postDelayed(this, 750);
        }
    };

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        setResult(RESULT_CANCELED);
        preferences = getSharedPreferences(PREFS, MODE_PRIVATE);
        downloads = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        ScrollView scroll = new ScrollView(this);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(24, 36, 24, 24);
        box.setFitsSystemWindows(true);
        scroll.addView(box);
        TextView title = new TextView(this);
        title.setText("Starter model · SmolLM2 135M");
        title.setTextSize(24);
        box.addView(title);
        TextView info = new TextView(this);
        info.setText("Q4_K_M GGUF · 105,454,432 bytes. Download uses Wi-Fi only, with metered networks and roaming disabled. Android continues the download in the background. SHA256 verification runs before selection. No model runs automatically.");
        box.addView(info);
        location = new TextView(this);
        location.setTextIsSelectable(true);
        box.addView(location);
        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setMax(1000);
        box.addView(progress);
        status = new TextView(this);
        status.setTextIsSelectable(true);
        box.addView(status);
        start = new Button(this);
        start.setOnClickListener(v -> beginDownload());
        box.addView(start);
        cancel = new Button(this);
        cancel.setText("Cancel / remove starter download");
        cancel.setOnClickListener(v -> {
            try {
                removeCurrent();
                preferences.edit().putString("message", "Starter download cancelled / removed.").apply();
            } catch (Exception error) {
                preferences.edit().putString("message", "Could not remove starter: " + error.getMessage()).apply();
            }
            refresh();
        });
        box.addView(cancel);
        use = new Button(this);
        use.setText("Use verified model");
        use.setOnClickListener(v -> selectVerified());
        box.addView(use);
        setContentView(scroll);
        refresh();
    }

    @Override protected void onResume() {
        super.onResume();
        resumed = true;
        handler.removeCallbacks(poll);
        handler.post(poll);
    }

    @Override protected void onPause() {
        resumed = false;
        handler.removeCallbacks(poll);
        super.onPause();
    }

    private static File modelDirectory(Context context) throws IOException {
        File external = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        if (external == null) throw new IOException("App-owned external storage is unavailable");
        File root = external.getCanonicalFile();
        File directory = new File(root, "starter-models").getCanonicalFile();
        if (!root.equals(directory.getParentFile())) {
            throw new IOException("Model directory must remain inside app-owned storage");
        }
        return directory;
    }

    private File currentFile() throws IOException {
        String name = preferences.getString("filename", null);
        if (name == null || !name.matches("starter-[0-9a-f-]{36}\\.gguf")) return null;
        File directory = modelDirectory(this);
        File candidate = new File(directory, name).getCanonicalFile();
        return directory.equals(candidate.getParentFile()) ? candidate : null;
    }

    /** Used only for the explicit in-app result; external picker URIs keep their own grants. */
    static boolean isManagedModel(Context context, Uri uri) {
        if (uri == null || !"file".equals(uri.getScheme()) || uri.getPath() == null) return false;
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREFS, MODE_PRIVATE);
            String name = prefs.getString("filename", null);
            if (name == null || !name.matches("starter-[0-9a-f-]{36}\\.gguf")) return false;
            File directory = modelDirectory(context);
            File expected = new File(directory, name).getCanonicalFile();
            return directory.equals(expected.getParentFile())
                    && expected.equals(new File(uri.getPath()).getCanonicalFile())
                    && expected.isFile() && expected.length() == MODEL_BYTES;
        } catch (IOException | SecurityException error) {
            return false;
        }
    }

    private void beginDownload() {
        try {
            removeCurrent();
            File directory = modelDirectory(this);
            if (!directory.isDirectory() && !directory.mkdirs()) {
                throw new IOException("Cannot create app-owned model directory");
            }
            String name = "starter-" + UUID.randomUUID() + ".gguf";
            File target = new File(directory, name);
            if (target.exists()) throw new IOException("Download destination already exists");
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(MODEL_URL))
                    .setTitle("ForgeOS starter model")
                    .setDescription("SmolLM2 135M Q4_K_M; verification required before use")
                    .setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI)
                    .setAllowedOverMetered(false)
                    .setAllowedOverRoaming(false)
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    .setDestinationInExternalFilesDir(this, Environment.DIRECTORY_DOWNLOADS,
                            "starter-models/" + name);
            long id = downloads.enqueue(request);
            if (!preferences.edit().putLong("downloadId", id).putString("filename", name)
                    .remove("message").commit()) {
                downloads.remove(id);
                throw new IOException("Could not persist download state; download cancelled");
            }
        } catch (Exception error) {
            preferences.edit().putString("message", "Download could not start: " + error.getMessage()).apply();
        }
        refresh();
    }

    private void removeCurrent() throws IOException {
        File file = currentFile();
        long id = preferences.getLong("downloadId", -1);
        synchronized (ModelManagerActivity.class) {
            if (verification != null && file != null && verification.file.equals(file)) {
                verification.future.cancel(true);
                verification = null;
            }
        }
        // The private preferences record only IDs enqueued here and UUID destinations we create.
        // Never accept a path or download ID from an Intent or the external file picker.
        if (id >= 0 && file != null) downloads.remove(id);
        if (file != null && file.exists() && !file.delete()) {
            throw new IOException("Cannot delete the app-created starter file");
        }
        preferences.edit().remove("downloadId").remove("filename").remove("message").commit();
        verifiedFile = null;
        verifiedResult = null;
    }

    private void refresh() {
        verifiedFile = null;
        verifiedResult = null;
        use.setEnabled(false);
        start.setEnabled(true);
        start.setText("Download starter model");
        cancel.setEnabled(false);
        progress.setIndeterminate(false);
        progress.setProgress(0);
        long id = preferences.getLong("downloadId", -1);
        try {
            File file = currentFile();
            location.setText("Storage: " + (file == null ? modelDirectory(this) : file)
                    + "\nThis app-owned external-files path does not prove adopted-card use. Android chooses the storage volume; uninstalling the app removes its files.");
            if (id < 0 || file == null) {
                status.setText(preferences.getString("message", "No starter download. Select Download to begin."));
                return;
            }
            cancel.setEnabled(true);
            start.setText("Retry download");
            try (Cursor row = downloads.query(new DownloadManager.Query().setFilterById(id))) {
                if (row == null || !row.moveToFirst()) {
                    status.setText("Download no longer exists or was cancelled. Retry to download again.");
                    return;
                }
                int jobStatus = row.getInt(row.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS));
                int reason = row.getInt(row.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON));
                long received = Math.max(0, row.getLong(row.getColumnIndexOrThrow(
                        DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)));
                long total = row.getLong(row.getColumnIndexOrThrow(
                        DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
                String bytes = String.format(Locale.US, "%,d / %,d bytes%s",
                        received, total > 0 ? total : MODEL_BYTES,
                        total > 0 ? "" : " (expected total)");
                long denominator = total > 0 ? total : MODEL_BYTES;
                progress.setProgress((int) Math.min(1000, 1000.0 * received / denominator));
                if (jobStatus == DownloadManager.STATUS_SUCCESSFUL) {
                    Verification result = verificationFor(file, id);
                    if (!result.finished) {
                        start.setEnabled(false);
                        progress.setIndeterminate(true);
                        status.setText("Download complete (" + bytes + "). Verifying exact size and SHA256 in background…");
                    } else if (!result.valid) {
                        status.setText("Verification failed: " + result.error
                                + "\nThis file cannot be selected. Retry downloads a new copy.");
                    } else if (result.matches(file, id)) {
                        progress.setProgress(1000);
                        verifiedFile = file;
                        verifiedResult = result;
                        use.setEnabled(true);
                        status.setText("Verified: exact 105,454,432 bytes and expected SHA256. Ready to select; inference has not run.");
                    }
                } else if (jobStatus == DownloadManager.STATUS_FAILED) {
                    status.setText("Download failed (Android reason " + reason + "). " + bytes + "\nRetry to start a new download.");
                } else if (jobStatus == DownloadManager.STATUS_PAUSED) {
                    start.setEnabled(false);
                    status.setText("Download paused: " + pauseReason(reason) + "\n" + bytes);
                } else if (jobStatus == DownloadManager.STATUS_PENDING
                        || jobStatus == DownloadManager.STATUS_RUNNING) {
                    start.setEnabled(false);
                    status.setText((jobStatus == DownloadManager.STATUS_PENDING
                            ? "Download queued; waiting for unmetered Wi-Fi." : "Downloading over Wi-Fi.")
                            + "\n" + bytes);
                } else {
                    status.setText("Download status unavailable. Retry to start again.");
                }
            }
        } catch (Exception error) {
            status.setText("Model manager error: " + error.getMessage());
        }
    }

    private static String pauseReason(int reason) {
        switch (reason) {
            case DownloadManager.PAUSED_WAITING_FOR_NETWORK: return "waiting for unmetered Wi-Fi";
            case DownloadManager.PAUSED_QUEUED_FOR_WIFI: return "queued for Wi-Fi";
            case DownloadManager.PAUSED_WAITING_TO_RETRY: return "Android is waiting to retry";
            default: return "Android reason " + reason;
        }
    }

    private static synchronized Verification verificationFor(File file, long id) {
        if (verification == null || !verification.matches(file, id)) {
            if (verification != null) verification.future.cancel(true);
            verification = new Verification(file, id);
        }
        return verification;
    }

    private static final class Verification {
        final File file;
        final long id, bytes, modified;
        final Future<?> future;
        volatile boolean finished;
        boolean valid;
        String error;

        Verification(File file, long id) {
            this.file = file;
            this.id = id;
            bytes = file.length();
            modified = file.lastModified();
            future = VERIFIER.submit(() -> {
                try {
                    valid = ModelIntegrity.verify(file, MODEL_SHA, MODEL_BYTES);
                    if (!valid) error = "size or SHA256 did not match";
                    if (valid && !matches(file, id)) {
                        valid = false;
                        error = "file changed during verification";
                    }
                } catch (Exception failure) {
                    valid = false;
                    error = failure.getMessage() == null ? failure.toString() : failure.getMessage();
                } finally {
                    finished = true;
                }
            });
        }

        boolean matches(File candidate, long downloadId) {
            return id == downloadId && file.equals(candidate)
                    && bytes == candidate.length() && modified == candidate.lastModified();
        }
    }

    private void selectVerified() {
        refresh(); // Re-query Android; never treat a cancelled/failed job as a completed model.
        if (verifiedFile == null || verifiedResult == null || !verifiedResult.finished
                || !verifiedResult.valid
                || !verifiedResult.matches(verifiedFile, preferences.getLong("downloadId", -1))) return;
        // Keep the result explicitly in our application, including the review application ID.
        setResult(RESULT_OK, new Intent(this, LocalActivity.class).setData(Uri.fromFile(verifiedFile)));
        finish();
    }
}
