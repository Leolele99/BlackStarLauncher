package com.skcraft.launcher.dialog;

import com.google.common.base.Strings;
import com.skcraft.launcher.Instance;
import com.skcraft.launcher.Launcher;
import com.skcraft.launcher.LauncherException;
import com.skcraft.launcher.install.FeatureCache;
import com.skcraft.launcher.model.modpack.Feature;
import com.skcraft.launcher.model.modpack.Manifest;
import com.skcraft.launcher.persistence.Persistence;
import com.skcraft.launcher.util.HttpRequest;
import com.skcraft.launcher.util.SharedLocale;
import lombok.SneakyThrows;
import lombok.extern.java.Log;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

@Log
public class OptionalFeaturesDialogueOpener extends Thread
{
    private final Instance selected;
    private final List<Runnable> executeOnCompletion = new ArrayList<Runnable>();
    final File featuresPath;
    final FeatureCache featuresCache;

    public OptionalFeaturesDialogueOpener(final Instance selected)
    {
        this.selected = selected;
        featuresPath = new File(selected.getDir(), "features.json");
        featuresCache = Persistence.read(featuresPath, FeatureCache.class);
    }

    @SneakyThrows
    @Override
    public void run()
    {
        executeOnCompletion.add(new Runnable() {
            @Override
            public void run() {
                writeDataFile(featuresPath, featuresCache);
            }
        });

        Manifest manifest = null;
        try
        {
            manifest = HttpRequest
                    .get(selected.getManifestURL())
                    .execute()
                    .expectResponseCode(200)
                    .returnContent()
                    .saveContent(selected.getManifestPath())
                    .asJson(Manifest.class);
        } catch (IOException ex)
        {
            ex.printStackTrace();
        } catch (InterruptedException ex)
        {
            ex.printStackTrace();
        }

        if (manifest.getMinimumVersion() > Launcher.PROTOCOL_VERSION)
        {
            try
            {
                throw new LauncherException("Update required", SharedLocale.tr("errors.updateRequiredError"));
            } catch (LauncherException ex)
            {
                ex.printStackTrace();
            }
        }

        if (manifest.getBaseUrl() == null)
        {
            manifest.setBaseUrl(selected.getManifestURL());
        }

        final List<Feature> features = manifest.getFeatures();
        if (!features.isEmpty())
        {
            for (Feature feature : features)
            {
                Boolean last = featuresCache.getSelected().get(feature.getName());
                if (last != null)
                {
                    feature.setSelected(last);
                }
            }

            Collections.sort(features);

            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    new FeatureSelectionDialog(ProgressDialog.getLastDialog(), features, OptionalFeaturesDialogueOpener.this)
                            .setVisible(true);
                }
            });

            synchronized (this) {
                this.wait();
            }

            for (Feature feature : features)
            {
                featuresCache.getSelected().put(Strings.nullToEmpty(feature.getName()), feature.isSelected());
            }

            complete();
        }
    }

    public void doneWithWindowCallback()
    {

    }

    private static void writeDataFile(File path, Object object) {
        try {
            Persistence.write(path, object);
        } catch (IOException e) {
            log.log(Level.WARNING, "Failed to write to " + path.getAbsolutePath() +
                    " for object " + object.getClass().getCanonicalName(), e);
        }
    }

    protected void complete() {
        for (Runnable runnable : executeOnCompletion) {
            runnable.run();
        }
    }
}
