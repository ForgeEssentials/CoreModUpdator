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

import argo.jdom.JdomParser;
import argo.jdom.JsonNode;
import argo.jdom.JsonStringNode;
import argo.saj.InvalidSyntaxException;
import cpw.mods.fml.relauncher.FMLLaunchHandler;
import cpw.mods.fml.relauncher.IFMLCallHook;
import cpw.mods.fml.relauncher.IFMLLoadingPlugin;
import net.dries007.coremod.Module.ModuleFile;
import net.dries007.coremod.dependencies.IDependency;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;

public class Coremod implements IFMLLoadingPlugin, IFMLCallHook
{
    protected static final JdomParser JSON_PARSER = new JdomParser();
    protected static       boolean    online      = false;
    protected static JsonNode root;
    protected static HashMap<String, Module> moduleMap       = new HashMap<String, Module>();
    protected static HashSet<File>           moduleFiles     = new HashSet<File>();
    protected static HashSet<IDependency>    depencies       = new HashSet<IDependency>();
    protected static HashSet<String>         usedDependencys = new HashSet<String>();

    public static HashMap<String, Module> getModuleMap()
    {
        return moduleMap;
    }

    @Override
    public Void call() throws IOException
    {
        Data.loadSettings();
        msg("Version " + Data.get(Data.VERSION) + " for MC " + Data.get(Data.MC_VERSION));

        /**
         * When wanting  to test the JSON and download functionality, comment the line below.
         */
        Data.readConfigs();
        if (checkDev()) return null;

        getJSON();

        if (online)
        {
            doVersionCheck();
            branchCheck();
            parseOnlineModules();
        }
        else
        {
            if (Data.firstRun)
            {
                msg("#################################################################",
                        "##### No first run without connection to the update server. #####",
                        "#################################################################");
                System.exit(1);
            }
            parseOfflineModules();
        }

        removeUnwantedFiles();

        Data.saveConfigs();
        classloadAll();
        return null;
    }

    private static void getJSON()
    {
        try
        {
            InputStreamReader isr = new InputStreamReader(new URL(Data.get(Data.JSONNURL)).openStream());
            root = Coremod.JSON_PARSER.parse(isr).getNode(Data.get(Data.NAME));
            Coremod.online = true;
            isr.close();
        }
        catch (final IOException e)
        {
            msg("JSON offline? Check manually: " + Data.get(Data.JSONNURL),
                    "###################################################",
                    "##### WARNING: The update URL is unavailable. #####",
                    "#####     Only classloading will be done!     #####",
                    "###################################################");
        }
        catch (final InvalidSyntaxException e)
        {
            msg("Invalid JSON at target? Check manually: " + Data.get(Data.JSONNURL),
                    "###############################################",
                    "##### WARNING: The update URL is corrupt. #####",
                    "#####   Only classloading will be done!   #####",
                    "###############################################");
        }

        if (!online && Boolean.parseBoolean(Data.get(Data.FORCEONLINE)))
        {
            msg("################################################",
                    "##### The update server must be available. #####",
                    "################################################");
            System.exit(1);
        }
    }

    private static boolean checkDev() throws IOException
    {
        if (!Data.debug) return false;

        msg("###########################################################",
                "#### DEV MODE ENGAGED. NO CLASSLOADING OR LIB LOADING. ####",
                "####       ONLY USE IN A DEVELOPMENT ENVIRONMENT       ####",
                "###########################################################");

        Properties properties = new Properties();
        properties.load(new FileInputStream(new File(Data.FEfolder, "dev.properties")));

        if (properties.containsKey(Data.get(Data.CLASSKEY_ASM)))
        {
            if (!properties.getProperty(Data.get(Data.CLASSKEY_ASM)).equals(""))
            {
                for (String className : properties.getProperty(Data.get(Data.CLASSKEY_ASM)).split(" "))
                {
                    msg("DEV ASM class: " + className);
                    try
                    {
                        Data.classLoader.registerTransformer(className);
                    }
                    catch (Exception e)
                    {
                        msg("DEV ASM class ERROR.");
                        e.printStackTrace();
                    }
                }
            }
        }

        if (properties.containsKey(Data.get(Data.FILEKEY_TA)))
        {
            if (!properties.getProperty(Data.get(Data.FILEKEY_TA)).equals(""))
            {
                for (String ATfile : properties.getProperty(Data.get(Data.FILEKEY_TA)).split(" "))
                {
                    msg("DEV AccessTransformer: " + ATfile);
                    try
                    {
                        CustomAT.addTransformerMap(ATfile);
                    }
                    catch (Exception e)
                    {
                        msg("DEV AccessTransformer ERROR.");
                        e.printStackTrace();
                    }
                }
            }
            for (String ATfile : properties.getProperty(Data.get(Data.FILEKEY_TA)).split(" "))
            {
                msg("DEV AccessTransformer: " + ATfile);
                try
                {
                    CustomAT.addTransformerMap(ATfile);
                }
                catch (Exception e)
                {
                    msg("DEV AccessTransformer ERROR.");
                    e.printStackTrace();
                }
            }
        }
        return true;
    }

    private static void doVersionCheck()
    {
        try
        {
            if (!root.getStringValue(Data.get(Data.JSONKEY_VERSION),
                    Data.get(Data.MC_VERSION)).equals(Data.get(Data.VERSION))) msg(
                    "##############################################################",
                    "##### WARNING: The version you are using is out of date. #####",
                    "#####      This might result in issues. Update now!      #####",
                    "##############################################################");
        }
        catch (IllegalArgumentException e)
        {
            msg("Tag missing: [" + Data.get(Data.NAME) + "] [" + Data.get(Data.JSONKEY_VERSION) + "] [" + Data.get(Data.MC_VERSION) + "]");
        }
    }

    private static void branchCheck() throws IOException
    {
        boolean knownBranch = false;
        for (JsonNode node : root.getArrayNode(Data.get(Data.JSONKEY_BRANCHES)))
        {
            if (Data.branch.equals(node.getText())) knownBranch = true;
        }

        if (!knownBranch)
        {
            msg("Branch (" + Data.branch + ") not found! Reverting to default (" + Data.get(Data.BRANCH_DEFAULT) + ")");
            Data.userSettings.setProperty(Data.USERKEY_BRANCH, Data.get(Data.BRANCH_DEFAULT));
            Data.branch = Data.get(Data.BRANCH_DEFAULT);
        }

        if (Data.hasKey(Data.BRANCHLOCK_URL) && Boolean.parseBoolean(Data.get(Data.BRANCHLOCK_ENABLE, "false")))
        {
            try
            {
                if (!Boolean.parseBoolean(new BufferedReader(new InputStreamReader(new URL(Data.BRANCHLOCK_URL + "?key=" + Data.userSettings.getProperty(
                        Data.USERKEY_BRANCH_KEY) + "&branch=" + Data.branch).openStream())).readLine()))
                {
                    msg("Branch (" + Data.branch + ") not allowed! Reverting to default (" + Data.get(Data.BRANCH_DEFAULT) + ")");
                    Data.userSettings.setProperty(Data.USERKEY_BRANCH, Data.get(Data.BRANCH_DEFAULT));
                    Data.branch = Data.get(Data.BRANCH_DEFAULT);
                }
            }
            catch (Exception e)
            {
                msg("Something went wrong with the branch lock.");
                e.printStackTrace();
            }
        }
    }

    public static void classloadAll() throws MalformedURLException
    {
        for (final Module m : Coremod.moduleMap.values())
        {
            msg("Module " + m.name + " adds:");

            for (final IDependency dependency : m.dependecies)
            {
                msg("Dependency: " + dependency.getFileName());
                Data.classLoader.addURL(new File(Data.dependencyFolder, dependency.getFileName()).toURI().toURL());
            }

            for (final ModuleFile mf : m.files)
            {
                msg("Module file: " + mf.file.getName());
                Data.classLoader.addURL(mf.file.toURI().toURL());
            }

            for (final String asmclass : m.ASMClasses)
            {
                msg("ASM class: " + asmclass);
                Data.classLoader.registerTransformer(asmclass);
            }

            for (final String at : m.ATFiles)
            {
                msg("AT: " + at);
                CustomAT.addTransformerMap(at);
            }
        }
    }

    private static void parseOnlineModules() throws IOException
    {
        JsonNode modules = root.getNode(Data.get(Data.JSONKEY_MODULES));

        for (JsonStringNode key : modules.getFields().keySet())
        {
            if (!moduleMap.containsKey(key.getText()))
            {
                parseModule(modules, key.getText());
            }
        }

        for (IDependency dependency : depencies)
        {
            File file = new File(Data.dependencyFolder, dependency.getFileName());
            if (file.exists())
            {
                if (!getChecksum(file).equals(dependency.getHash()))
                {
                    msg("Lib " + dependency.getFileName() + " had wrong hash! " + dependency.getHash() + " != " + getChecksum(
                            file));
                    file.delete();
                }
            }
            if (!file.exists())
            {
                msg("Downloading lib " + dependency.getFileName() + " from " + dependency.getDownloadURL());
                FileUtils.copyURLToFile(dependency.getDownloadURL(), file);
            }
            usedDependencys.add(file.getName());
        }
    }

    private static void parseOfflineModules()
    {
        msg("The update server is offline. No checking done, just loading.");
        for (File file : Data.modulesFolder.listFiles())
        {
            Module m = new Module(file);
            m.parceJarFiles();
            moduleMap.put(m.name, m);
        }

        for (File file : Data.dependencyFolder.listFiles())
        {
            usedDependencys.add(file.getName());
        }
    }

    private static void parseModule(JsonNode modulesJSON, String moduleName)
    {
        msg("Parsing module " + moduleName);
        try
        {
            JsonNode moduleJSON = modulesJSON.getNode(moduleName);
            // Getting default from JSON
            if (!Data.modules.containsKey(moduleName))
            {
                if (Data.hasKey(Data.JSONKEY_DEFAULT)) Data.modules.put(moduleName,
                        moduleJSON.getBooleanValue(Data.get(Data.JSONKEY_DEFAULT),
                                FMLLaunchHandler.side().name().toLowerCase()).toString());
                else Data.modules.put(moduleName, false);
            }

            // Actual parsing if we want the module
            if (Boolean.parseBoolean(Data.modules.getProperty(moduleName)))
            {
                Module module = new Module(moduleName);

                if (!moduleJSON.isNode(Data.get(Data.MC_VERSION)))
                {
                    msg("Module (" + moduleName + ") not available for your MC version. Skipping.");
                    return;
                }

                // Get the files from the JSON
                for (JsonNode fileJSON : moduleJSON.getArrayNode(Data.get(Data.MC_VERSION),
                        Data.branch,
                        Data.get(Data.JSONKEY_FILES)))
                {
                    File file = new File(Data.modulesFolder, fileJSON.getStringValue(Data.get(Data.JSONKEY_FILE_NAME)));
                    moduleFiles.add(file);
                    module.files.add(new ModuleFile(file,
                            new URL(fileJSON.getStringValue(Data.get(Data.JSONKEY_FILE_URL))),
                            fileJSON.getStringValue(Data.get(Data.JSONKEY_FILE_HASH))));
                }

                // Get the INTERMODULE dependencies from the JSON
                for (JsonNode dependencyJSON : moduleJSON.getArrayNode(Data.get(Data.MC_VERSION),
                        Data.branch,
                        Data.get(Data.JSONKEY_DEPENDENCIES)))
                {
                    if (Boolean.parseBoolean(Data.modules.getProperty(dependencyJSON.getText(), "false")))
                    {
                        msg("Module (" + moduleName + ") needs another module (" + dependencyJSON.getText() + ") as dependency that is not enabled. Enabling!");
                        Data.modules.setProperty(dependencyJSON.getText(), "true");
                        if (!moduleMap.containsKey(dependencyJSON.getText()))
                            parseModule(modulesJSON, dependencyJSON.getText());
                    }
                }

                module.checkJarFiles();
                module.parceJarFiles();

                depencies.addAll(module.dependecies);
                moduleMap.put(moduleName, module);
            }
        }
        catch (IllegalArgumentException e)
        {
            msg("Parsing module (" + moduleName + ") FAILED");
            e.printStackTrace();
        }
        catch (MalformedURLException e)
        {
            msg("Parsing module (" + moduleName + ") FAILED");
            e.printStackTrace();
        }
        catch (IOException e)
        {
            msg("Parsing module (" + moduleName + ") FAILED");
            e.printStackTrace();
        }
    }

    private static void removeUnwantedFiles() throws IOException
    {
        for (final File file : Data.modulesFolder.listFiles())
        {
            if (moduleFiles.contains(file)) continue;

            file.delete();
            msg("Removing not needed module file " + file.getName());
        }

        for (final File file : Data.dependencyFolder.listFiles())
        {
            if (usedDependencys.contains(file)) continue;

            file.delete();
            msg("Removing not needed dependency " + file.getName());
        }
    }

    public static HashSet<? extends IDependency> getDependencies(IDependency dependency)
    {
        final HashSet<IDependency> set = new HashSet<IDependency>();

        for (final IDependency nd : dependency.getTransitiveDependencies())
        {
            set.add(nd);
            if (dependency.getTransitiveDependencies() != null && !dependency.getTransitiveDependencies().isEmpty())
                set.addAll(getDependencies(nd));
        }

        return set;
    }

    public static String getChecksum(File file)
    {
        try
        {
            final MessageDigest md = MessageDigest.getInstance("SHA1");
            final FileInputStream fis = new FileInputStream(file);
            final byte[] dataBytes = new byte[1024];

            int nread = 0;

            while ((nread = fis.read(dataBytes)) != -1)
            {
                md.update(dataBytes, 0, nread);
            }

            final byte[] mdbytes = md.digest();

            // convert the byte to hex format
            final StringBuffer sb = new StringBuffer("");
            for (int i = 0; i < mdbytes.length; i++)
            {
                sb.append(Integer.toString((mdbytes[i] & 0xff) + 0x100, 16).substring(1));
            }
            fis.close();

            return sb.toString();
        }
        catch (final NoSuchAlgorithmException e)
        {
            e.printStackTrace();
        }
        catch (final IOException e)
        {
            e.printStackTrace();
        }

        return null;
    }

    public static void msg(String... lines)
    {
        for (String msg : lines)
        {
            System.out.println("[" + Data.get(Data.NAME) + "] " + msg);
        }
    }

    @Override
    public void injectData(final Map<String, Object> data)
    {
        Data.injectData(data);
    }

    @Override
    @Deprecated
    public String[] getLibraryRequestClass()
    {
        return null;
    }

    @Override
    public String[] getASMTransformerClass()
    {
        return Data.ASMCLASSES;
    }

    @Override
    public String getModContainerClass()
    {
        return null;
    }

    @Override
    public String getSetupClass()
    {
        return this.getClass().getName();
    }
}