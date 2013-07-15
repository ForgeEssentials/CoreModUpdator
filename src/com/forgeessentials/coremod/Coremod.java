package com.forgeessentials.coremod;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import net.minecraft.launchwrapper.LaunchClassLoader;

import org.apache.commons.io.FileUtils;

import argo.jdom.JdomParser;
import argo.jdom.JsonNode;
import argo.jdom.JsonRootNode;
import argo.jdom.JsonStringNode;

import com.google.common.base.Strings;

import cpw.mods.fml.relauncher.IFMLCallHook;
import cpw.mods.fml.relauncher.IFMLLoadingPlugin;

/**
 * Main class, does all the real work. Look in {@link}Data to change URLs and stuff.
 * 
 * @author Dries007
 * (c) Copyright  Dries007.net 2013
 * 
 * Written for ForgeEssentials, but might be useful for others.
 */
@IFMLLoadingPlugin.Name(Data.NAME)
@IFMLLoadingPlugin.MCVersion(Data.MC_VERSION)
public class Coremod implements IFMLLoadingPlugin, IFMLCallHook
{
    // private static final JsonFormatter JSON_FORMATTER = new PrettyJsonFormatter();
    private static final JdomParser JSON_PARSER = new JdomParser();
    
    public Void call() throws Exception
    {
        boolean classLoad = true;
        try
        {
            if (Data.classLoader.findClass("net.minecraft.world.World") == null)
            {
                System.out.println("[" + Data.NAME + "] Running in deobf enviroment, no classloading...");
                classLoad = true;
            }
        }
        catch (Exception e)
        {   
            
        }
        try
        {
            InputStreamReader reader = new InputStreamReader(new URL(Data.JSONURL + "json.php").openStream());
            JsonRootNode root = JSON_PARSER.parse(reader);
            
            File FEfolder = new File(Data.mclocation, "ForgeEssentials");
            if (!FEfolder.exists()) FEfolder.mkdirs();
            
            File configFile = new File(FEfolder, "Coremod.properties");
            if (!configFile.exists()) configFile.createNewFile();
            
            File modulesFolder = new File(FEfolder, "modules");
            if (!modulesFolder.exists()) modulesFolder.mkdirs();
            
            File libsFolder = new File(FEfolder, "libs");
            if (!libsFolder.exists()) libsFolder.mkdirs();
            
            FileInputStream in = new FileInputStream(configFile);
            Properties properties = new Properties();
            properties.load(in);
            in.close();
            
            String comments = "=== READ THIS ===" + "\n# autoUpdate" + "\n#      Default: true" + "\n#      Check with the FE repo to see if there is a new version, if there is, it will be downloaded and used.";
            if (!properties.containsKey("autoUpdate")) properties.setProperty("autoUpdate", "true");
            boolean autoUpdate = Boolean.parseBoolean(properties.getProperty("autoUpdate"));
            
            if (autoUpdate)
            {
                /*
                 * Branch stuff
                 */
                comments += "\n# Branches" + "\n#      Default: stable" + "\n#      Possible values: dev, beta, stable" + "\n#      Use this to change wich kind of release you want.";
                if (!properties.containsKey("branches")) properties.setProperty("branches", "stable");
                String branch = properties.getProperty("branches");
                if (!branch.equals("stable") && !branch.equals("beta") && !branch.equals("dev"))
                {
                    System.out.println("[" + Data.NAME + "] Branch '" + branch + "' not found! Reverting to default.");
                    properties.setProperty("branches", "stable");
                    branch = "stable";
                }
                
                /*
                 * Modules In config
                 */
                HashMap<String, Boolean> modulesMap = new HashMap<String, Boolean>();
                HashMap<String, URL> toDownload = new HashMap<String, URL>();
                comments += "\n# Modules" + "\n#      Default: true for all modules" + "\n#      Use this to change wich modules you want." + "\n#      Warning, If you set this to false, the module file will be removed.";
                
                JsonNode modules = root.getNode("versions").getNode(Data.MC_VERSION);
                for (JsonStringNode module : modules.getFields().keySet())
                {
                    String filename = null;
                    URL url = null;
                    try
                    {
                        List<JsonNode> list = modules.getNode(module.getText()).getArrayNode(branch);
                        filename = list.get(0).getText();
                        url = new URL(Data.JSONURL + list.get(1).getText());
                    }
                    catch (Exception e)
                    {
                        // don't need to print or warn, is checked below.
                    }
                    if (!Strings.isNullOrEmpty(filename))
                    {
                        String name = "modules." + module.getText();
                        if (!properties.containsKey(name)) properties.setProperty(name, "true");
                        modulesMap.put(filename, Boolean.parseBoolean(properties.getProperty(name)));
                        toDownload.put(filename, url);
                    }
                }
                
                /*
                 * Modules In folder
                 */
                for (File file : modulesFolder.listFiles())
                {
                    if (!modulesMap.containsKey(file.getName()))
                    {
                        file.delete();
                    }
                    else if (!modulesMap.get(file.getName()))
                    {
                        file.delete();
                    }
                    else
                    {
                        toDownload.remove(file.getName());
                    }
                }
                
                /*
                 * Downloading modules
                 */
                for (String name : toDownload.keySet())
                {
                    try
                    {
                        System.out.println("[" + Data.NAME + "] Downloading module " + name);
                        FileUtils.copyURLToFile(toDownload.get(name), new File(modulesFolder, name));
                    }
                    catch (Exception e)
                    {}
                }

                /*
                 * Get all current libs
                 */
                ArrayList<File> libstodelete = new ArrayList<File>();
                for (File lib : libsFolder.listFiles())
                {
                    libstodelete.add(lib);
                }
                
                /*
                 * Get all wanted libs
                 */
                for (File file : modulesFolder.listFiles())
                {
                    if (!file.getName().endsWith(".jar")) file.delete();
                    else
                    {
                        System.out.println(file.getName());
                        
                        JarFile jar = new JarFile(file);
                        Manifest mf = jar.getManifest();
                        if (mf != null)
                        {
                            String libs = mf.getMainAttributes().getValue(Data.LIBKEY);
                            for (String lib : libs.split(";"))
                            {
                                File wannabelib = new File(libsFolder, lib);
                                if (wannabelib.exists())
                                {
                                    libstodelete.remove(wannabelib);
                                }
                                else
                                {
                                    try
                                    {
                                        System.out.println("[" + Data.NAME + "] Downloading lib " + lib);
                                        FileUtils.copyURLToFile(new URL(Data.LIBURL + lib), wannabelib);
                                    }
                                    catch (Exception e)
                                    {}
                                }
                            }
                        }
                        jar.close();
                    }
                }
                /*
                 * Remove bad/old libs
                 */
                for (File lib : libstodelete)
                {
                    System.out.println("[" + Data.NAME + "] Removing unneeded lib " + lib.getName());
                    lib.delete();
                }
                
                /*
                 * Classload libs
                 */
                for (File lib : libsFolder.listFiles())
                {
                    System.out.println("[" + Data.NAME + "] Loading lib " + lib.getName());
                    Data.classLoader.addURL(lib.toURI().toURL());
                }
                System.out.println("[" + Data.NAME + "] Lib Classloading done.");
                
                /*
                 * We don't want to load obf files in a deobf environment...
                 */
                if (classLoad)
                {
                    for (File file : modulesFolder.listFiles())
                    {
                        System.out.println("[" + Data.NAME + "] Loading module " + file.getName());
                        Data.classLoader.addURL(file.toURI().toURL());
                    }
                    System.out.println("[" + Data.NAME + "] Module Classloading done.");
                }
            }
            
            FileOutputStream out = new FileOutputStream(configFile);
            properties.store(out, comments);
            out.close();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        
        return null;
    }
    
    public void injectData(Map<String, Object> data)
    {
        if (data.containsKey("mclocation") && data.get("mclocation") != null)
        {
            Data.mclocation = (File) data.get("mclocation");
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
        return Data.ASMCLASS;
    }
    
    @Override
    public String getModContainerClass()
    {
        return null;
    }
    
    @Override
    public String getSetupClass()
    {
        return Data.SETUPCLASS;
    }
}