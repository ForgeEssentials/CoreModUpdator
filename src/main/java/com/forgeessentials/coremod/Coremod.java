package com.forgeessentials.coremod;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;

import cpw.mods.fml.relauncher.FMLLaunchHandler;

import org.apache.commons.io.FileUtils;

import argo.jdom.JdomParser;
import argo.jdom.JsonNode;
import argo.jdom.JsonRootNode;
import argo.jdom.JsonStringNode;
import argo.saj.InvalidSyntaxException;

import com.forgeessentials.coremod.Module.ModuleFile;
import com.forgeessentials.coremod.dependencies.IDependency;

import cpw.mods.fml.relauncher.IFMLCallHook;
import cpw.mods.fml.relauncher.IFMLLoadingPlugin;

/**
 * Mod class, does all the real work. Look in {@link}Data to change URLs and stuff. (c) Copyright Dries007.net 2013 Written for ForgeEssentials, but might be useful for others.
 * 
 * @author Dries007
 */
@IFMLLoadingPlugin.Name(Data.NAME)
@IFMLLoadingPlugin.MCVersion(Data.MC_VERSION)
public class Coremod implements IFMLLoadingPlugin, IFMLCallHook
{
    public static final JdomParser          JSON_PARSER = new JdomParser();
    public static boolean                   online = false;
    public static JsonNode                  root;

    public static HashMap<String, Module>   moduleMap   = new HashMap<String, Module>();
    public static HashSet<File>             wantedModuleFiles = new HashSet<File>();
    public static HashSet<IDependency>      wantedDepencies = new HashSet<IDependency>();

    @Override
    public Void call() throws IOException
    {
        Data.loadSettings();
        if (checkDev()) return null;

        /**
         * Get the JSON data and with that the online status
         */
        try
        {
            root = Coremod.JSON_PARSER.parse(new InputStreamReader(new URL(Data.get(Data.JSONNURL)).openStream())).getNode(Data.get(Data.NAME));
            Coremod.online = true;
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
        if (online) doVersionCheck();
        Data.readConfigs();

        if (online) branchCheck();
        if (!online && Data.firstRun)
        {
            msg("#################################################################",
                "##### No first run without connection to the update server. #####",
                "#################################################################");
            System.exit(1);
        }

        Data.saveConfigs();
        return null;
    }

    private boolean checkDev()
    {
        try
        {
            if (Boolean.parseBoolean(Data.get(Data.DEBUG, "false")))
            {
                msg("###########################################################",
                    "#### DEV MODE ENGAGED. NO CLASSLOADING OR LIB LOADING. ####",
                    "####       ONLY USE IN A DEVELOPMENT ENVIRONMENT       ####",
                    "###########################################################");
                File dev = new File(Data.FEfolder, "checkDev.properties");
                if (dev.exists())
                {
                    final FileInputStream in = new FileInputStream(dev);
                    Properties properties = new Properties();
                    properties.load(in);
                    in.close();

                    if (properties.containsKey(Data.get(Data.CLASSKEY_ASM)))
                    {
                        for (String className : properties.getProperty(Data.get(Data.CLASSKEY_ASM)).split(" "))
                        {
                            System.out.println("[" + Data.NAME + "] DEV ASM class: " + className);
                            try
                            {
                                Data.classLoader.registerTransformer(className);
                            }
                            catch (Exception e)
                            {
                                System.out.println("[" + Data.NAME + "] DEV ASM class ERROR.");
                                e.printStackTrace();
                            }
                        }
                    }

                    if (properties.containsKey(Data.get(Data.FILEKEY_TA)))
                    {
                        for (String ATfile : properties.getProperty(Data.get(Data.FILEKEY_TA)).split(" "))
                        {
                            System.out.println("[" + Data.NAME + "] DEV AccessTransformer: " + ATfile);
                            try
                            {
                                CustomAT.addTransformerMap(ATfile);
                            }
                            catch (Exception e)
                            {
                                System.out.println("[" + Data.NAME + "] DEV AccessTransformer ERROR.");
                                e.printStackTrace();
                            }
                        }
                    }
                }
                return true;
            }
            else return false;
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return false;
        }
    }

    private void doVersionCheck()
    {
        try
        {
            if (!root.getStringValue(Data.get(Data.JSONKEY_VERSION), Data.get(Data.MC_VERSION)).equals(Data.get(Data.VERSION)))
                msg();
        }
        catch (IllegalArgumentException e)
        {
            msg("Tag missing: [" + Data.get(Data.NAME) + "] [" + Data.get(Data.JSONKEY_VERSION) + "] [" + Data.get(Data.MC_VERSION) + "]");
        }
    }

    private void branchCheck() throws IOException
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
                if (!Boolean.parseBoolean(new BufferedReader(new InputStreamReader(new URL(Data.BRANCHLOCK_URL + "?key=" + Data.userSettings.getProperty(Data.USERKEY_BRANCH_KEY) + "&branch=" + Data.branch).openStream())).readLine()))
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

    public static void sdqfsdf()
    {
            final JsonNode modules = this.root.getNode("modules");

            for (final JsonStringNode key : modules.getFields().keySet())
            {
                if (!moduleMap.containsKey(key.getText()))
                    parseModule(modules, key.getText());
            }
            
            final HashSet<String> usedDependencys = new HashSet<String>();
            for (final IDependency dependency : wantedDepencies)
            {
                final File file = new File(Data.dependencyFolder, dependency.getFileName());
                if (file.exists()) if (!Coremod.getChecksum(file).equals(dependency.getHash()))
                {
                    System.out.println("[" + Data.NAME + "] Lib " + dependency.getFileName() + " had wrong hash " + dependency.getHash() + " != " + Coremod.getChecksum(file));
                    file.delete();
                }
                else
                    usedDependencys.add(file.getName());
                if (!file.exists())
                {
                    System.out.println("[" + Data.NAME + "] Downloading lib " + dependency.getFileName() + " from " + dependency.getDownloadURL());
                    FileUtils.copyURLToFile(dependency.getDownloadURL(), file);
                    usedDependencys.add(file.getName());
                }
            }
            
            /*
             * Remove not needed dependencies
             */
            for (final File file : Data.dependencyFolder.listFiles())
            {
                if (usedDependencys.contains(file.getName())) continue;
                
                file.delete();
                System.out.println("[" + Data.NAME + "] Removing not needed dependency " + file.getName());
            }
        }
        else
            /*
             * No checks for anything.
             */
            for (final File file : Data.modulesFolder.listFiles())
                if (!file.getName().endsWith(".jar"))
                    file.delete();
                else
                {
                    final Module m = new Module(file);
                    m.parceJarFiles();
                    Coremod.moduleMap.put(m.name, m);
                }
        
        this.classloadAll();
        
        saveProperties();
        return null;
    }

    private void parseModule(JsonNode modules, String moduleName) throws IOException
    {
        System.out.println("Parsing module " + moduleName);
        if (!Data.modules.containsKey(moduleName))
        {
            if (manualInstallAndFirstRun)
            {
                if (new File(Data.modulesFolder, modules.getArrayNode(moduleName, Data.MC_VERSION, Data.branch, "files").get(0).getStringValue("file")).exists())
                    Data.modules.put(moduleName, "true");
                else
                    Data.modules.put(moduleName, "false");
            }
            else
            {
                Data.modules.put(moduleName, modules.getBooleanValue(moduleName, "default", FMLLaunchHandler.side().name().toLowerCase()).toString());
            }
        }
        if (Boolean.parseBoolean(Data.modules.getProperty(moduleName)))
        {
            final Module module = new Module(moduleName);
            /*
             * Add files the JSON sais we need
             */
            for (final JsonNode fileNode : modules.getArrayNode(moduleName, Data.MC_VERSION, Data.branch, "files"))
            {
                final File f = new File(Data.modulesFolder, fileNode.getStringValue("file"));
                wantedModuleFiles.add(f);
                module.files.add(new ModuleFile(f, new URL(Data.BASEURL + fileNode.getStringValue("url")), fileNode.getStringValue("hash")));
            }

            /*
             * Intermodule dependedcendy. Forcing setting on module file to true and rerun the module parsing if not yet loaded
             */
            for (final JsonNode dependency : modules.getArrayNode(moduleName, Data.MC_VERSION, Data.branch, "dependencies"))
            {
                Data.modules.put(dependency.getText(), "true");
                if (!moduleMap.containsKey(dependency.getText()))
                    parseModule(modules, dependency.getText());
            }

            /*
             * Check to see if said files exist
             */
            module.checkJarFiles();

            /*
             * Parse the modules jar files for interesting things
             */
            module.parceJarFiles();

            wantedDepencies.addAll(module.dependecies);

            Coremod.moduleMap.put(module.name, module);
        }

        /*
         * Removing all non needed module files
         */
        for (final File file : Data.modulesFolder.listFiles())
        {
            if (wantedModuleFiles.contains(file)) continue;

            file.delete();
            System.out.println("[" + Data.NAME + "] Removing not needed module file " + file.getName());
        }
    }

    public void msg(String... lines)
    {
        for (String msg : lines)
            System.out.println(Data.hasKey(Data.NAME) ? "[" + Data.get(Data.NAME) + "] " + msg : msg);
    }

    /**
     * Returns nested dependencies
     * 
     * @param dependency
     * @return
     */
    public static HashSet<? extends IDependency> getDependencies(final IDependency dependency)
    {
        final HashSet<IDependency> set = new HashSet<IDependency>();
        
        for (final IDependency nd : dependency.getTransitiveDependencies())
        {
            set.add(nd);
            if (dependency.getTransitiveDependencies() != null && !dependency.getTransitiveDependencies().isEmpty()) set.addAll(Coremod.getDependencies(nd));
        }
        
        return set;
    }
    
    /**
     * Classloads all of the things!
     * 
     * @throws MalformedURLException
     */
    public void classloadAll() throws MalformedURLException
    {
        for (final Module m : Coremod.moduleMap.values())
        {
            System.out.println("[" + Data.NAME + "] Module " + m.name + " adds:");
            
            for (final IDependency dependency : m.dependecies)
            {
                System.out.println("[" + Data.NAME + "] Dependency: " + dependency.getFileName());
                Data.classLoader.addURL(new File(Data.dependencyFolder, dependency.getFileName()).toURI().toURL());
            }
            
            for (final ModuleFile mf : m.files)
            {
                System.out.println("[" + Data.NAME + "] Module file: " + mf.file.getName());
                Data.classLoader.addURL(mf.file.toURI().toURL());
            }
            
            for (final String asmclass : m.ASMClasses)
            {
                System.out.println("[" + Data.NAME + "] ASM class: " + asmclass);
                Data.classLoader.registerTransformer(asmclass);
            }
            
            for (final String at : m.ATFiles)
            {
                System.out.println("[" + Data.NAME + "] AT: " + at);
                CustomAT.addTransformerMap(at);
            }
        }
    }
    
    public static String getChecksum(final File file)
    {
        try
        {
            final MessageDigest md = MessageDigest.getInstance("SHA1");
            final FileInputStream fis = new FileInputStream(file);
            final byte[] dataBytes = new byte[1024];
            
            int nread = 0;
            
            while ((nread = fis.read(dataBytes)) != -1)
                md.update(dataBytes, 0, nread);
            
            final byte[] mdbytes = md.digest();
            
            // convert the byte to hex format
            final StringBuffer sb = new StringBuffer("");
            for (int i = 0; i < mdbytes.length; i++)
                sb.append(Integer.toString((mdbytes[i] & 0xff) + 0x100, 16).substring(1));
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

    public static void saveProperties() throws IOException
    {
        FileOutputStream out = new FileOutputStream(Data.configFile);
        Data.properties.store(out, "Look in the readme file for more info on how to use this.");
        out.close();

        out = new FileOutputStream(Data.modulesFile);
        Data.modules.store(out, "This list defines what modules get downloaded.\n" +
                "If a module depends on another module, that module will be downloaded and that module's setting will be overwritten.");
        out.close();
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