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
import net.minecraft.launchwrapper.LaunchClassLoader;

import org.apache.commons.io.FileUtils;

import argo.jdom.JdomParser;
import argo.jdom.JsonNode;
import argo.jdom.JsonRootNode;
import argo.jdom.JsonStringNode;
import argo.saj.InvalidSyntaxException;

import com.forgeessentials.coremod.Module.ModuleFile;
import com.forgeessentials.coremod.dependencies.IDependency;
import com.forgeessentials.coremod.install.Main;

import cpw.mods.fml.relauncher.IFMLCallHook;
import cpw.mods.fml.relauncher.IFMLLoadingPlugin;

/**
 * Main class, does all the real work. Look in {@link}Data to change URLs and stuff. (c) Copyright Dries007.net 2013 Written for ForgeEssentials, but might be useful for others.
 * 
 * @author Dries007
 */
@IFMLLoadingPlugin.Name(Data.NAME)
@IFMLLoadingPlugin.MCVersion(Data.MC_VERSION)
public class Coremod implements IFMLLoadingPlugin, IFMLCallHook
{
    public static final JdomParser        JSON_PARSER = new JdomParser();
    public static boolean                 online;
    
    JsonRootNode                          root;
    
    /*
     * Map with all modules
     */
    public static HashMap<String, Module> moduleMap   = new HashMap<String, Module>();
    private boolean manualInstallAndFirstRun;
    final HashSet<File> wantedModuleFiles = new HashSet<File>();
    final HashSet<String> intermoduleDependencies = new HashSet<String>();
    final HashSet<IDependency> wantedDepencies = new HashSet<IDependency>();

    @Override
    public Void call() throws IOException
    {
        if (dev()) return null;

        try
        {
            this.root = Coremod.JSON_PARSER.parse(new InputStreamReader(new URL(Data.JSONURL).openStream()));
            Coremod.online = true;
            
            /*
             * Version check
             */
            if (!this.root.getStringValue("CoreMod", Data.MC_VERSION).equals(Data.VERSION))
            {
                System.out.println("[" + Data.NAME + "] ##############################################################");
                System.out.println("[" + Data.NAME + "] ##### WARNING: The version you are using is out of date. #####");
                System.out.println("[" + Data.NAME + "] #####      This might result in issues. Update now!      #####");
                System.out.println("[" + Data.NAME + "] ##############################################################");
            }
        }
        catch (final IOException e)
        {
            Coremod.online = false;
            System.out.println("[" + Data.NAME + "] JSON offline? Check manually: " + Data.JSONURL);
            System.out.println("[" + Data.NAME + "] ###################################################");
            System.out.println("[" + Data.NAME + "] ##### WARNING: The update URL is unavailable. #####");
            System.out.println("[" + Data.NAME + "] #####     Only classloading will be done!     #####");
            System.out.println("[" + Data.NAME + "] ###################################################");
        }
        catch (final InvalidSyntaxException e)
        {
            Coremod.online = false;
            System.out.println("[" + Data.NAME + "] Invalid JSON at target? Check manually: " + Data.JSONURL);
            System.out.println("[" + Data.NAME + "] ###############################################");
            System.out.println("[" + Data.NAME + "] ##### WARNING: The update URL is corrupt. #####");
            System.out.println("[" + Data.NAME + "] #####   Only classloading will be done!   #####");
            System.out.println("[" + Data.NAME + "] ###############################################");
        }
        
        Main.setup();
        
        // We need a valid JSON for first boot.
        if (Main.firstRun && !Coremod.online)
        {
            System.out.println("[" + Data.NAME + "] I can't do a first run when the data server is offline. Sorry!");
            Runtime.getRuntime().exit(1);
        }

        // Status message
        if (Main.modulesFolder.exists() && Main.firstRun)
        {
            System.out.println("[" + Data.NAME + "] Doing a full first run on manual install.");
            manualInstallAndFirstRun = true;
        }
        else if (Main.firstRun)
        {
            System.out.println("[" + Data.NAME + "] Doing a full first run.");
        }
        else if (!Main.autoUpdate)
        {
            System.out.println("[" + Data.NAME + "] You are NOT using autoupdate. We will only check dependencies and classload.");
        }
        
        Main.properties.setProperty("firstRun", "false");
        
        if (Coremod.online)
        {
            try
            {
                if (!Main.properties.containsKey("betaKey")) Main.properties.put("betaKey", "");
                final Scanner s = new Scanner(new URL(Data.LOCKURL + "?key=" + Main.properties.getProperty("betaKey")).openStream(), "UTF-8");
                final List<String> authBranches = Arrays.asList(s.useDelimiter("\\A").next().split(";"));
                s.close();
                if (!authBranches.contains(Main.branch))
                {
                    System.out.println("[" + Data.NAME + "] ################################################################");
                    System.out.println("[" + Data.NAME + "] #####  WARNING: You are using a non autenticated branch.   #####");
                    System.out.println("[" + Data.NAME + "] #####        Enter your betaKey in the config file!        #####");
                    System.out.println("[" + Data.NAME + "] ################################################################");
                    Runtime.getRuntime().exit(42);
                }
            }
            catch (final Exception e)
            {
                System.out.println("[" + Data.NAME + "] Invalid JSON at target? Check manually: " + Data.LOCKURL + "?key=" + Main.properties.getProperty("betaKey"));
                System.out.println("[" + Data.NAME + "] Could not verify the branch key.");
                e.printStackTrace();
            }


            final JsonNode modules = this.root.getNode("modules");

            for (final JsonStringNode key : modules.getFields().keySet())
            {
                if (!moduleMap.containsKey(key.getText()))
                    parseModule(modules, key.getText());
            }
            
            final HashSet<String> usedDependencys = new HashSet<String>();
            for (final IDependency dependency : wantedDepencies)
            {
                final File file = new File(Main.dependencyFolder, dependency.getFileName());
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
            for (final File file : Main.dependencyFolder.listFiles())
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
            for (final File file : Main.modulesFolder.listFiles())
                if (!file.getName().endsWith(".jar"))
                    file.delete();
                else
                {
                    final Module m = new Module(file);
                    m.parceJarFiles();
                    Coremod.moduleMap.put(m.name, m);
                }
        
        this.classloadAll();
        
        Main.saveProperties();
        return null;
    }

    private void parseModule(JsonNode modules, String moduleName) throws IOException
    {
        System.out.println("Parsing module " + moduleName);
        if (!Main.modules.containsKey(moduleName))
        {
            if (manualInstallAndFirstRun)
            {
                if (new File(Main.modulesFolder, modules.getArrayNode(moduleName, Data.MC_VERSION, Main.branch, "files").get(0).getStringValue("file")).exists())
                    Main.modules.put(moduleName, "true");
                else
                    Main.modules.put(moduleName, "false");
            }
            else
            {
                Main.modules.put(moduleName, modules.getBooleanValue(moduleName, "default", FMLLaunchHandler.side().name().toLowerCase()).toString());
            }
        }
        if (Boolean.parseBoolean(Main.modules.getProperty(moduleName)))
        {
            final Module module = new Module(moduleName);
            /*
             * Add files the JSON sais we need
             */
            for (final JsonNode fileNode : modules.getArrayNode(moduleName, Data.MC_VERSION, Main.branch, "files"))
            {
                final File f = new File(Main.modulesFolder, fileNode.getStringValue("file"));
                wantedModuleFiles.add(f);
                module.files.add(new ModuleFile(f, new URL(Data.BASEURL + fileNode.getStringValue("url")), fileNode.getStringValue("hash")));
            }

            /*
             * Intermodule dependedcendy. Forcing setting on module file to true and rerun the module parsing if not yet loaded
             */
            for (final JsonNode dependency : modules.getArrayNode(moduleName, Data.MC_VERSION, Main.branch, "dependencies"))
            {
                Main.modules.put(dependency.getText(), "true");
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
        for (final File file : Main.modulesFolder.listFiles())
        {
            if (wantedModuleFiles.contains(file)) continue;

            file.delete();
            System.out.println("[" + Data.NAME + "] Removing not needed module file " + file.getName());
        }
    }

    private boolean dev()
    {
        File dev = new File(Main.FEfolder, "dev.properties");
        if (dev.exists())
        {
            try
            {
                System.out.println("[" + Data.NAME + "] ###########################################################");
                System.out.println("[" + Data.NAME + "] #### DEV MODE ENGAGED. NO CLASSLOADING OR LIB LOADING. ####");
                System.out.println("[" + Data.NAME + "] ####       ONLY USE IN A DEVELOPMENT ENVIRONMENT       ####");
                System.out.println("[" + Data.NAME + "] ###########################################################");

                final FileInputStream in = new FileInputStream(dev);
                Properties properties = new Properties();
                properties.load(in);
                in.close();

                if (properties.containsKey(Data.ASMKEY))
                {
                    for (String className : properties.getProperty(Data.ASMKEY).split(" "))
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

                if (properties.containsKey(Data.ATKEY))
                {
                    for (String ATfile : properties.getProperty(Data.ATKEY).split(" "))
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
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }

        return dev.exists();
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
                Data.classLoader.addURL(new File(Main.dependencyFolder, dependency.getFileName()).toURI().toURL());
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
    
    @Override
    public void injectData(final Map<String, Object> data)
    {
        if (data.containsKey("mcLocation"))
        {
            Main.mclocation = (File) data.get("mcLocation");
            Main.FEfolder = new File(Main.mclocation, "ForgeEssentials");
        }
        
        if (data.containsKey("runtimeDeobfuscationEnabled") && data.get("runtimeDeobfuscationEnabled") != null) Data.debug = !(Boolean) data.get("runtimeDeobfuscationEnabled");
        
        if (data.containsKey("classLoader") && data.get("classLoader") != null) Data.classLoader = (LaunchClassLoader) data.get("classLoader");
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