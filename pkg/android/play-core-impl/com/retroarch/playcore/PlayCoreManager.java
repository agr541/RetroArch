package com.retroarch.playcore;

import com.google.android.play.core.splitinstall.SplitInstallManager;
import com.google.android.play.core.splitinstall.SplitInstallManagerFactory;
import com.google.android.play.core.splitinstall.SplitInstallRequest;
import com.google.android.play.core.splitinstall.SplitInstallStateUpdatedListener;
import com.google.android.play.core.splitinstall.SplitInstallSessionState;
import com.google.android.play.core.splitinstall.model.SplitInstallSessionStatus;

import com.retroarch.browser.retroactivity.RetroActivityCommon;

import java.util.Collections;
import java.util.List;

public class PlayCoreManager {

    private PlayCoreManager() {}

    private RetroActivityCommon activity;
    private static PlayCoreManager instance;

    public static final int INSTALL_STATUS_DOWNLOADING = 0;
    public static final int INSTALL_STATUS_INSTALLING = 1;
    public static final int INSTALL_STATUS_INSTALLED = 2;
    public static final int INSTALL_STATUS_FAILED = 3;

    private final SplitInstallStateUpdatedListener listener = state -> {
        List<String> moduleNames = state.moduleNames();
        String[] coreNames = new String[moduleNames.size()];

        for (int i = 0; i < moduleNames.size(); i++) {
            coreNames[i] = activity.unsanitizeCoreName(moduleNames.get(i));
        }

        int status;

        switch (state.status()) {
            case SplitInstallSessionStatus.DOWNLOADING:
                status = INSTALL_STATUS_DOWNLOADING;
                break;

            case SplitInstallSessionStatus.INSTALLING:
                status = INSTALL_STATUS_INSTALLING;
                break;

            case SplitInstallSessionStatus.INSTALLED:
                activity.updateSymlinks();
                status = INSTALL_STATUS_INSTALLED;
                break;

            case SplitInstallSessionStatus.FAILED:
            default:
                status = INSTALL_STATUS_FAILED;
                break;
        }

        activity.coreInstallStatusChanged(
                coreNames,
                status,
                state.bytesDownloaded(),
                state.totalBytesToDownload()
        );
    };

    public static PlayCoreManager getInstance() {
        if (instance == null) {
            instance = new PlayCoreManager();
        }
        return instance;
    }

    public void onCreate(RetroActivityCommon newActivity) {
        activity = newActivity;

        SplitInstallManager manager =
                SplitInstallManagerFactory.create(activity);

        manager.registerListener(listener);
    }

    public void onDestroy() {
        SplitInstallManager manager =
                SplitInstallManagerFactory.create(activity);

        manager.unregisterListener(listener);

        activity = null;
    }

    public String[] getInstalledModules() {
        SplitInstallManager manager =
                SplitInstallManagerFactory.create(activity);

        return manager.getInstalledModules().toArray(new String[0]);
    }

    public void downloadCore(final String coreName) {
        SplitInstallManager manager =
                SplitInstallManagerFactory.create(activity);

        SplitInstallRequest request =
                SplitInstallRequest.newBuilder()
                        .addModule(activity.sanitizeCoreName(coreName))
                        .build();

        manager.startInstall(request)
                .addOnSuccessListener(result ->
                        activity.coreInstallInitiated(coreName, true)
                )
                .addOnFailureListener(e ->
                        activity.coreInstallInitiated(coreName, false)
                );
    }

    public void deleteCore(String coreName) {
        SplitInstallManager manager =
                SplitInstallManagerFactory.create(activity);

        manager.deferredUninstall(
                Collections.singletonList(activity.sanitizeCoreName(coreName))
        );
    }
}
