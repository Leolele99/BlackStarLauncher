package com.skcraft.launcher.util;

import com.google.common.collect.Maps;
import com.moandjiezana.toml.Toml;
import com.moandjiezana.toml.TomlWriter;
import com.skcraft.launcher.Launcher;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public class LauncherOptions
{
    //Variable Declarations
    private File optionsFile;
    private Toml options;
    private boolean updateModpack;
    private boolean showConsole;

    //Constructor
    public LauncherOptions(Launcher launcher)
    {
        super();

        //Initialize variables
        optionsFile = new File(launcher.getBaseDir(), "options.toml");

        //Setup defaults
        updateModpack = true;
        showConsole = false;

        //Check if options file exists
        if (!optionsFile.exists())
        {
            saveOptionsToFile();
        }

        //Load options from file
        loadOptionsFromFile();
    }

    //Methods
    public void saveOptionsToFile()
    {
        //Create options file
        TomlWriter writer = new TomlWriter();

        //Create options map
        Map<String, Object> optionsMap = Maps.newHashMap();
        optionsMap.put("updateModpack", updateModpack);
        optionsMap.put("showConsole", showConsole);

        try
        {
            writer.write(optionsMap, optionsFile);
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    private void loadOptionsFromFile()
    {
        options = new Toml().read(optionsFile);

        updateModpack = options.getBoolean("updateModpack");
        showConsole = options.getBoolean("showConsole");
    }

    //Getter Methods
    public boolean isUpdateModpack()
    {
        return updateModpack;
    }

    public boolean isShowConsole()
    {
        return showConsole;
    }

    //Setter Methods
    public void setUpdateModpack(boolean updateModpack)
    {
        this.updateModpack = updateModpack;
    }

    public void setShowConsole(boolean showConsole)
    {
        this.showConsole = showConsole;
    }
}

