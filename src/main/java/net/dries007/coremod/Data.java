/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2013 Dries K. Aka Dries007
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package net.dries007.coremod;

import net.minecraft.launchwrapper.LaunchClassLoader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;

public class Data
{
    /**
     * Key constants
     */
    public static final String NAME        = "name";
    public static final String DATAFOLDER  = "dataFolder";
    public static final String VERSION     = "version";
    public static final String MC_VERSION  = "mcversion";
    public static final String FORCEONLINE = "forceOnline";

    public static final String LIBKEY_NORMAL = "libkey.normal";
    public static final String LIBKEY_MAVEN  = "libkey.maven";

    public static final String LIBURL_NORMAL = "liburl.normal";
    public static final String LIBURL_MAVEN  = "liburl.maven";

    public static final String CLASSKEY_ASM = "classkey.ASM";

    public static final String FILEKEY_TA = "filekey.AT";

    public static final String BRANCH_DEFAULT    = "branch.default";
    public static final String BRANCHLOCK_ENABLE = "branch.lock.enable";
    public static final String BRANCHLOCK_URL    = "branch.lock.url";

    public static final String JSONNURL             = "json.URL";
    public static final String JSONKEY_VERSION      = "json.key.version";
    public static final String JSONKEY_BRANCHES     = "json.key.branches";
    public static final String JSONKEY_MODULES      = "json.key.modules";
    public static final String JSONKEY_DEFAULT      = "json.key.default";
    public static final String JSONKEY_DEPENDENCIES = "json.key.dependencies";

    public static final String JSONKEY_FILES     = "json.key.files";
    public static final String JSONKEY_FILE_NAME = "json.key.file.name";
    public static final String JSONKEY_FILE_URL  = "json.key.file.URL";
    public static final String JSONKEY_FILE_HASH = "json.key.file.hash";

    public static final String USERKEY_FIRSTRUN   = "settings.key.firstRun";
    public static final String USERKEY_AUTOUPDATE = "settings.key.autoUpdate";
    public static final String USERKEY_BRANCH     = "settings.key.branch";
    public static final String USERKEY_BRANCH_KEY = "settings.key.branchKey";

    /**
     * Internal crap
     */
    protected static final String[] ASMCLASSES = {CustomAT.class.getName()};

    protected static Properties userSettings = new Properties();
    protected static Properties modules      = new Properties();
    protected static boolean           debug;
    protected static LaunchClassLoader classLoader;
    protected static boolean           firstRun;
    protected static String            branch;
    protected static boolean           autoUpdate;
    protected static File              mclocation;
    protected static File              FEfolder;
    protected static File              modulesFolder;
    protected static File              dependencyFolder;

    private static File configFile;
    private static File modulesFile;

    private static Properties internalSettings;

    protected static void loadSettings() throws IOException
    {
        internalSettings = new Properties();
        internalSettings.load(Data.class.getResourceAsStream("settings.properties"));
    }

    protected static void injectData(final Map<String, Object> data)
    {
        if (data.containsKey("mcLocation"))
        {
            mclocation = (File) data.get("mcLocation");
            FEfolder = new File(mclocation, "ForgeEssentials");
        }

        if (data.containsKey("runtimeDeobfuscationEnabled") && data.get("runtimeDeobfuscationEnabled") != null)
            debug = !(Boolean) data.get("runtimeDeobfuscationEnabled");

        if (data.containsKey("classLoader") && data.get("classLoader") != null)
            classLoader = (LaunchClassLoader) data.get("classLoader");
    }

    protected static void readConfigs() throws IOException
    {
        FEfolder = new File(mclocation, Data.get(Data.DATAFOLDER));
        if (!FEfolder.exists()) FEfolder.mkdirs();

        configFile = new File(FEfolder, "Launcher.properties");
        if (!configFile.exists()) configFile.createNewFile();

        modulesFile = new File(FEfolder, "Modules.properties");
        if (!modulesFile.exists()) modulesFile.createNewFile();

        modulesFolder = new File(FEfolder, "modules");
        if (!modulesFolder.exists()) modulesFolder.mkdirs();

        dependencyFolder = new File(FEfolder, "dependency");
        if (!dependencyFolder.exists()) dependencyFolder.mkdirs();

        userSettings.load(new FileInputStream(configFile));
        modules.load(new FileInputStream(modulesFile));

        if (!userSettings.containsKey(get(USERKEY_FIRSTRUN))) userSettings.setProperty(get(USERKEY_FIRSTRUN), "true");
        firstRun = Boolean.parseBoolean(userSettings.getProperty(get(USERKEY_FIRSTRUN)));

        if (!userSettings.containsKey(get(USERKEY_AUTOUPDATE))) userSettings.setProperty(get(USERKEY_AUTOUPDATE), "true");
        autoUpdate = Boolean.parseBoolean(userSettings.getProperty(get(USERKEY_AUTOUPDATE)));

        if (!userSettings.containsKey(get(USERKEY_BRANCH)))
            userSettings.setProperty(get(USERKEY_BRANCH), get(BRANCH_DEFAULT));
        branch = userSettings.getProperty(get(USERKEY_BRANCH));
    }

    protected static void saveConfigs() throws IOException
    {
        userSettings.store(new FileOutputStream(configFile), "Expert settings. Please use the wiki when editing.");
        modules.store(new FileOutputStream(modulesFile), "Modules you want = true; modules you don't want = false.");
    }

    /**
     * Gets values from the INTERNAL settings file. Use the constants!
     *
     * @param key
     * @return null if key not present
     */
    public static String get(String key)
    {
        if (!hasKey(key)) throw new RuntimeException("Key (" + key + ") not in config file!");
        return internalSettings.getProperty(key);
    }

    /**
     * Gets values from the INTERNAL settings file. Use the constants!
     *
     * @param key
     * @param defaultValue
     * @return defaultValue if key not present
     */
    public static String get(String key, String defaultValue)
    {
        return internalSettings.getProperty(key, defaultValue);
    }

    /**
     * Use the constants!
     *
     * @param keys
     * @return true if the INTERNAL settings have all the keys specified
     */
    public static boolean hasKey(String... keys)
    {
        for (String key : keys)
        {
            if (!internalSettings.containsKey(key)) return false;
        }

        return true;
    }
}
