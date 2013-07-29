package com.forgeessentials.coremod;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

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
 * Main class, does all the real work. Look in {@link}Data to change URLs and
 * stuff. (c) Copyright Dries007.net 2013 Written for ForgeEssentials, but might
 * be useful for others.
 * 
 * @author Dries007
 */
@IFMLLoadingPlugin.Name(Data.NAME)
@IFMLLoadingPlugin.MCVersion(Data.MC_VERSION)
public class Coremod implements IFMLLoadingPlugin, IFMLCallHook
{
    public static final JdomParser JSON_PARSER = new JdomParser();
    public static boolean          online;
    
    JsonRootNode                   root;
    
    /*
     * Map with all modules
     */
    HashMap<String, Module>        moduleMap   = new HashMap<String, Module>();
    
    public Void call() throws IOException
    {
        try
        {
            root = JSON_PARSER.parse(new InputStreamReader(new URL(Data.JSONURL).openStream()));
            online = true;
            
            /*
             * Version check
             */
            if (!root.getStringValue("CoreMod", Data.MC_VERSION).equals(Data.VERSION))
            {
                System.out.println("[" + Data.NAME + "] ##############################################################");
                System.out.println("[" + Data.NAME + "] ##### WARNING: The version you are using is out of date. #####");
                System.out.println("[" + Data.NAME + "] #####      This might result in issues. Update now!      #####");
                System.out.println("[" + Data.NAME + "] ##############################################################");
            }
        }
        catch (IOException e)
        {
            online = false;
            System.out.println("[" + Data.NAME + "] JSON offline? Check manually: " + Data.JSONURL);
            System.out.println("[" + Data.NAME + "] ###################################################");
            System.out.println("[" + Data.NAME + "] ##### WARNING: The update URL is unavailable. #####");
            System.out.println("[" + Data.NAME + "] #####     Only classloading will be done!     #####");
            System.out.println("[" + Data.NAME + "] ###################################################");
        }
        catch (InvalidSyntaxException e)
        {
            online = false;
            System.out.println("[" + Data.NAME + "] Invalid JSON at target? Check manually: " + Data.JSONURL);
            System.out.println("[" + Data.NAME + "] ###############################################");
            System.out.println("[" + Data.NAME + "] ##### WARNING: The update URL is corrupt. #####");
            System.out.println("[" + Data.NAME + "] #####   Only classloading will be done!   #####");
            System.out.println("[" + Data.NAME + "] ###############################################");
        }
        
        Main.setup();
        
        // We need a valid JSON for first boot.
        if (Main.firstRun && !online)
        {
            System.out.println("[" + Data.NAME + "] I can't do a first run when the data server is offline. Sorry!");
            Runtime.getRuntime().exit(1);
        }
        
        // Status message
        if (Main.firstRun)
            System.out.println("[" + Data.NAME + "] Doing a full first run.");
        else if (!Main.autoUpdate) System.out.println("[" + Data.NAME + "] You are NOT using autoupdate. We will only check dependencies and classload.");
        
        Main.properties.setProperty("firstRun", "false");
        
        if (online)
        {
            try
            {
                if (!Main.properties.containsKey("betaKey")) Main.properties.put("betaKey", "");
                Scanner s = new Scanner(new URL(Data.LOCKURL + "?key=" + Main.properties.getProperty("betaKey")).openStream(), "UTF-8");
                List<String> authBranches = Arrays.asList(s.useDelimiter("\\A").next().split(";"));
                s.close();
                if (!authBranches.contains(Main.branch))
                {
                    Main.branch = authBranches.get(authBranches.size() - 1);
                    System.out.println("[" + Data.NAME + "] ################################################################");
                    System.out.println("[" + Data.NAME + "] #####  WARNING: You are using a non autenticated branch.   #####");
                    System.out.println("[" + Data.NAME + "] #####        Enter your betaKey in the config file!        #####");
                    System.out.println("[" + Data.NAME + "] #####             Will revert back to '" + Main.branch + "'.            #####");
                    System.out.println("[" + Data.NAME + "] ################################################################");
                }
            }
            catch (Exception e)
            {
                System.out.println("[" + Data.NAME + "] Invalid JSON at target? Check manually: " + Data.LOCKURL + "?key=" + Main.properties.getProperty("betaKey"));
                System.out.println("[" + Data.NAME + "] Could not verify the branch key.");
                e.printStackTrace();
            }
            HashSet<File> wantedModuleFiles = new HashSet<File>();
            HashSet<IDependency> wantedDepencies = new HashSet<IDependency>();
            JsonNode modules = root.getNode("modules");
            for (JsonStringNode key : modules.getFields().keySet())
            {
                String moduleName = key.getText();
                
                if (!Main.properties.containsKey("module." + moduleName)) Main.properties.put("module." + moduleName, "true");
                if (Boolean.parseBoolean(Main.properties.getProperty("module." + moduleName)))
                {
                    Module module = new Module(moduleName);
                    /*
                     * Add files the JSON sais we need
                     */
                    for (JsonNode fileNode : modules.getArrayNode(moduleName, Data.MC_VERSION, Main.branch))
                    {
                        File f = new File(Main.modulesFolder, fileNode.getStringValue("file"));
                        wantedModuleFiles.add(f);
                        module.files.add(new ModuleFile(f, new URL(Data.BASEURL + fileNode.getStringValue("url")), fileNode.getStringValue("hash")));
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
                    
                    moduleMap.put(module.name, module);
                }
                
                /*
                 * Removing all non needed module files
                 */
                for (File file : Main.modulesFolder.listFiles())
                {
                    if (wantedModuleFiles.contains(file)) continue;
                    
                    file.delete();
                    System.out.println("[" + Data.NAME + "] Removing not needed module file " + file.getName());
                }
            }
            
            HashSet<String> usedDependencys = new HashSet<String>();
            for (IDependency dependency : wantedDepencies)
            {
                File file = new File(Main.dependencyFolder, dependency.getFileName());
                if (file.exists())
                {
                    if (!getChecksum(file).equals(dependency.getHash()))
                    {
                        System.out.println("[" + Data.NAME + "] Lib " + dependency.getFileName() + " had wrong hash " + dependency.getHash() + " != " + getChecksum(file));
                        file.delete();
                    }
                    else
                    {
                        usedDependencys.add(file.getName());
                    }
                }
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
            for (File file : Main.dependencyFolder.listFiles())
            {
                if (usedDependencys.contains(file.getName())) continue;
                
                file.delete();
                System.out.println("[" + Data.NAME + "] Removing not needed dependency " + file.getName());
            }
        }
        else
        {
            /*
             * No checks for anything.
             */
            for (File file : Main.modulesFolder.listFiles())
            {
                if (!file.getName().endsWith(".jar"))
                    file.delete();
                else
                {
                    Module m = new Module(file);
                    m.parceJarFiles();
                    moduleMap.put(m.name, m);
                }
            }
        }
        
        classloadAll();
        
        Main.saveProperties();
        return null;
    }
    
    /**
     * Returns nested dependencies
     * 
     * @param dependency
     * @return
     */
    public static HashSet<? extends IDependency> getDependencies(IDependency dependency)
    {
        HashSet<IDependency> set = new HashSet<IDependency>();
        
        for (IDependency nd : dependency.getTransitiveDependencies())
        {
            set.add(nd);
            if (dependency.getTransitiveDependencies() != null && !dependency.getTransitiveDependencies().isEmpty()) set.addAll(getDependencies(nd));
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
        for (Module m : moduleMap.values())
        {
            System.out.println("[" + Data.NAME + "] Module " + m.name + " adds:");
            
            for (IDependency dependency : m.dependecies)
            {
                System.out.println("[" + Data.NAME + "] Dependency: " + dependency.getFileName());
                Data.classLoader.addURL(new File(Main.dependencyFolder, dependency.getFileName()).toURI().toURL());
            }
            
            for (ModuleFile mf : m.files)
            {
                System.out.println("[" + Data.NAME + "] Module file: " + mf.file.getName());
                Data.classLoader.addURL(mf.file.toURI().toURL());
            }
            
            for (String asmclass : m.ASMClasses)
            {
                System.out.println("[" + Data.NAME + "] ASM class: " + asmclass);
                Data.classLoader.registerTransformer(asmclass);
            }
            
            for (String at : m.ATFiles)
            {
                System.out.println("[" + Data.NAME + "] AT: " + at);
                CustomAT.addTransformerMap(at);
            }
        }
    }
    
    public static String getChecksum(File file)
    {
        try
        {
            MessageDigest md = MessageDigest.getInstance("SHA1");
            FileInputStream fis = new FileInputStream(file);
            byte[] dataBytes = new byte[1024];
            
            int nread = 0;
            
            while ((nread = fis.read(dataBytes)) != -1)
            {
                md.update(dataBytes, 0, nread);
            }
            
            byte[] mdbytes = md.digest();
            
            // convert the byte to hex format
            StringBuffer sb = new StringBuffer("");
            for (int i = 0; i < mdbytes.length; i++)
            {
                sb.append(Integer.toString((mdbytes[i] & 0xff) + 0x100, 16).substring(1));
            }
            fis.close();
            
            return sb.toString();
        }
        catch (NoSuchAlgorithmException e)
        {
            e.printStackTrace();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        
        return null;
    }
    
    public void injectData(Map<String, Object> data)
    {
        if (data.containsKey("runtimeDeobfuscationEnabled") && data.get("runtimeDeobfuscationEnabled") != null)
        {
            Data.debug = !(Boolean) data.get("runtimeDeobfuscationEnabled");
        }
        
        if (data.containsKey("classLoader") && data.get("classLoader") != null)
        {
            Data.classLoader = (LaunchClassLoader) data.get("classLoader");
        }
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