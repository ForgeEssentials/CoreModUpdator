package com.forgeessentials.coremod;

import net.minecraft.launchwrapper.LaunchClassLoader;

import java.io.*;
import java.util.Map;
import java.util.Properties;

/**
 * Most stuff can be changed here. (c) Copyright Dries007.net 2013 Written for ForgeEssentials, but might be useful for others.
 *
 * @author Dries007
 */
public class Data
{
    /**
     * Key constants
     */
    public static final String NAME = "name";
    public static final String VERSION = "version";
    public static final String MC_VERSION = "mcversion";
    public static final String FORCEONLINE = "forceOnline";
    public static final String DEBUG = "debug";

    public static final String LIBKEY_NORMAL = "libkey.normal";
    public static final String LIBKEY_MAVEN = "libkey.maven";

    public static final String LIBURL_NORMAL = "liburl.normal";
    public static final String LIBURL_MAVEN = "liburl.maven";

    public static final String CLASSKEY_ASM = "classkey.ASM";

    public static final String FILEKEY_TA = "filekey.AT";

    public static final String BRANCH_DEFAULT = "branch.default";
    public static final String BRANCHLOCK_ENABLE = "branch.lock.enable";
    public static final String BRANCHLOCK_URL = "branch.lock.url";

    public static final String JSONNURL = "json.URL";
    public static final String JSONKEY_VERSION = "json.key.version";
    public static final String JSONKEY_BRANCHES = "json.key.branches";

    public static final String USERKEY_FIRSTRUN = "firstRun";
    public static final String USERKEY_AUTOUPDATE = "autoUpdate";
    public static final String USERKEY_BRANCH = "branch";
    public static final String USERKEY_BRANCH_KEY = "branchKey";

    /**
     * Internal crap
     */
    protected static final String[] ASMCLASSES = {CustomAT.class.getName().toString()};

    protected static LaunchClassLoader classLoader;
    protected static boolean firstRun;
    protected static String branch;
    protected static Properties userSettings = new Properties();
    protected static Properties modules = new Properties();
    protected static boolean autoUpdate;
    protected static File mclocation;
    protected static File FEfolder;
    protected static File configFile;
    protected static File modulesFolder;
    protected static File dependencyFolder;
    protected static File modulesFile;

    private static Properties internalSettings;


    public static void loadSettings() throws IOException
    {
        internalSettings = new Properties();
        internalSettings.load(Data.class.getResourceAsStream("settings.userSettings"));
    }

    public static void injectData(final Map<String, Object> data)
    {
        if (data.containsKey("mcLocation"))
        {
            mclocation = (File) data.get("mcLocation");
            FEfolder = new File(mclocation, "ForgeEssentials");
        }

        if (data.containsKey("runtimeDeobfuscationEnabled") && data.get("runtimeDeobfuscationEnabled") != null) debug = !(Boolean) data.get("runtimeDeobfuscationEnabled");

        if (data.containsKey("classLoader") && data.get("classLoader") != null) classLoader = (LaunchClassLoader) data.get("classLoader");
    }

    public static void readConfigs() throws IOException
    {
        FEfolder = new File(mclocation, Data.get(Data.NAME));
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

        if (!userSettings.containsKey(USERKEY_FIRSTRUN)) userSettings.setProperty(USERKEY_FIRSTRUN, "true");
        firstRun = Boolean.parseBoolean(userSettings.getProperty(USERKEY_FIRSTRUN));

        if (!userSettings.containsKey(USERKEY_AUTOUPDATE)) userSettings.setProperty(USERKEY_AUTOUPDATE, "true");
        autoUpdate = Boolean.parseBoolean(userSettings.getProperty(USERKEY_AUTOUPDATE));

        if (!userSettings.containsKey(USERKEY_BRANCH)) userSettings.setProperty(USERKEY_BRANCH, get(Data.BRANCH_DEFAULT));
        branch = userSettings.getProperty(USERKEY_BRANCH);
    }

    public static void saveConfigs() throws IOException
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
     * @param key
     * @return true if the INTERNAL settings have the key specified
     */
    public static boolean hasKey(String key)
    {
        return internalSettings.containsKey(key);
    }


}
