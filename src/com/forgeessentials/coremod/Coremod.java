package com.forgeessentials.coremod;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import net.minecraft.launchwrapper.LaunchClassLoader;

import org.apache.commons.io.FileUtils;

import argo.jdom.JdomParser;
import argo.jdom.JsonNode;
import argo.jdom.JsonRootNode;
import argo.jdom.JsonStringNode;

import com.forgeessentials.coremod.dependencies.DefaultDependency;
import com.forgeessentials.coremod.dependencies.IDependency;
import com.forgeessentials.coremod.dependencies.MavenDependency;
import com.forgeessentials.coremod.install.Main;
import com.google.common.base.Strings;

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
    // private static final JsonFormatter JSON_FORMATTER = new PrettyJsonFormatter();
    public static final JdomParser JSON_PARSER = new JdomParser();
    
    public Void call() throws Exception
    {
        try
        {
            InputStreamReader reader = new InputStreamReader(new URL(Data.JSONURL + "json.php").openStream());
            JsonRootNode root = JSON_PARSER.parse(reader);
            
            Main.setup();
            
            /*
             * Modules In config
             */
            HashMap<String, Boolean> modulesMap = new HashMap<String, Boolean>();
            HashMap<String, URL> toDownload = new HashMap<String, URL>();
            Main.comments += "\n# Modules" + "\n#      Default: true for all modules" + "\n#      Use this to change wich modules you want." + "\n#      Warning, If you set this to false, the module file will be removed.";
            
            JsonNode modules = root.getNode("versions").getNode(Data.MC_VERSION);
            for (JsonStringNode module : modules.getFields().keySet())
            {
                String filename = null;
                URL url = null;
                try
                {
                    List<JsonNode> list = modules.getNode(module.getText()).getArrayNode(Main.branch);
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
                    if (!Main.properties.containsKey(name)) Main.properties.setProperty(name, "true");
                    modulesMap.put(filename, Boolean.parseBoolean(Main.properties.getProperty(name)));
                    toDownload.put(filename, url);
                }
            }
            
            /*
             * Modules In folder
             */
            for (File file : Main.modulesFolder.listFiles())
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
                    FileUtils.copyURLToFile(toDownload.get(name), new File(Main.modulesFolder, name));
                }
                catch (Exception e)
                {}
            }
            
            /*
             * Map of all the normal libs we want key = filename, value = hash
             */
            HashMap<String, IDependency> libsmap = new HashMap<String, IDependency>();
            /*
             * Sets for all ASM classes and ATs They get added later
             */
            HashSet<String> ASMClasses = new HashSet<String>();
            HashSet<String> ATFiles = new HashSet<String>();
            
            for (File file : Main.modulesFolder.listFiles())
            {
                if (!file.getName().endsWith(".jar"))
                    file.delete();
                else
                {
                    JarFile jar = new JarFile(file);
                    Manifest mf = jar.getManifest();
                    if (mf != null)
                    {
                        /*
                         * Reading NORMAL libs from the modules' manifest files We want: Space sperated pairs of filename:sha1
                         */
                        String libs = mf.getMainAttributes().getValue(Data.NORMALLIBKEY);
                        if (libs != null)
                        {
                            for (String lib : libs.split(" "))
                            {
                                DefaultDependency dependency = new DefaultDependency(lib);
                                libsmap.put(dependency.getFileName(), dependency);
                            }
                        }
                        
                        /*
                         * Reading MAVEN libs from the modules' manifest files We want: the maven name
                         */
                        libs = mf.getMainAttributes().getValue(Data.MAVENLIBKEY);
                        if (libs != null)
                        {
                            for (String lib : libs.split(" "))
                            {
                                MavenDependency dependency = new MavenDependency(lib);
                                libsmap.put(dependency.getFileName(), dependency);
                                libsmap.putAll(getDependencies(dependency));
                            }
                        }
                        
                        /*
                         * Reading ASM classes from the modules' manifest files
                         */
                        String asmclasses = mf.getMainAttributes().getValue(Data.ASMKEY);
                        if (asmclasses != null)
                        {
                            for (String asmclass : asmclasses.split(" "))
                            {
                                ASMClasses.add(asmclass);
                                System.out.println("[" + Data.NAME + "] Added ASM class (" + asmclass + ") for module " + jar.getName());
                            }
                        }
                        
                        /*
                         * Reading AT Files from the modules' manifest files
                         */
                        String ats = mf.getMainAttributes().getValue(Data.ATKEY);
                        if (ats != null)
                        {
                            for (String at : ats.split(" "))
                            {
                                ATFiles.add(at);
                                System.out.println("[" + Data.NAME + "] Added AccessTransformer (" + at + ") for module " + jar.getName());
                            }
                        }
                    }
                    jar.close();
                }
            }
            
            /*
             * Check all current libs
             */
            HashSet<String> usedLibs = new HashSet<String>();
            for (IDependency dependency : libsmap.values())
            {
                File file = new File(Main.libsFolder, dependency.getFileName());
                if (file.exists())
                {
                    /*
                     * Checksum check 1
                     */
                    if (!getChecksum(file).equals(dependency.getHash()))
                    {
                        System.out.println("[" + Data.NAME + "] Lib " + dependency.getFileName() + " had wrong hash " + dependency.getHash() + " != " + getChecksum(file));
                        file.delete();
                    }
                    else
                    {
                        /*
                         * All is good, next!
                         */
                        usedLibs.add(file.getName());
                        continue;
                    }
                }
                if (!file.exists())
                {
                    System.out.println("[" + Data.NAME + "] Downloading lib " + dependency.getFileName() + " from " + dependency.getDownloadURL());
                    FileUtils.copyURLToFile(dependency.getDownloadURL(), file);
                    /*
                     * Checksum check 2
                     */
                    if (!getChecksum(file).equals(dependency.getHash()))
                    {
                        System.out.println("[" + Data.NAME + "] Was not able to download " + dependency.getFileName() + " from " + dependency.getDownloadURL() + " with hash " + dependency.getHash() + ". We got hash " + getChecksum(file));
                        throw new RuntimeException();
                    }
                    /*
                     * Downloaded fine. Next!
                     */
                    usedLibs.add(file.getName());
                }
            }
            
            /*
             * Remove not needed libs
             */
            for (File file : Main.libsFolder.listFiles())
            {
                if (!usedLibs.contains(file.getName()))
                {
                    file.delete();
                    System.out.println("[" + Data.NAME + "] Removing not needed lib " + file.getName());
                }
            }
            
            /*
             * Classload libs
             */
            for (File lib : Main.libsFolder.listFiles())
            {
                System.out.println("[" + Data.NAME + "] Loading lib " + lib.getName());
                Data.classLoader.addURL(lib.toURI().toURL());
            }
            System.out.println("[" + Data.NAME + "] Lib Classloading done.");
            
            /*
             * Classload modules
             */
            for (File file : Main.modulesFolder.listFiles())
            {
                System.out.println("[" + Data.NAME + "] Module: " + file.getName());
                Data.classLoader.addURL(file.toURI().toURL());
            }
            System.out.println("[" + Data.NAME + "] Module Classloading done.");
            
            for (String asmclass : ASMClasses)
            {
                System.out.println("[" + Data.NAME + "] ASM class: " + asmclass);
                Data.classLoader.registerTransformer(asmclass);
            }
            System.out.println("[" + Data.NAME + "] Loading ASM classes done.");
            
            for (String at : ATFiles)
            {
                System.out.println("[" + Data.NAME + "] AT: " + at);
                CustomAT.addTransformerMap(at);
            }
            System.out.println("[" + Data.NAME + "] Loading ATs done.");
            
            Main.saveProperties();
        }
        catch (Exception e)
        {
            if (e instanceof RuntimeException)
                throw new RuntimeException(e);
            else
                e.printStackTrace();
        }
        
        return null;
    }
    
    private Map<? extends String, ? extends IDependency> getDependencies(IDependency dependency)
    {
        HashMap<String, IDependency> map = new HashMap<String, IDependency>();
        
        for (IDependency nd : dependency.getTransitiveDependencies())
        {
            map.put(nd.getFileName(), nd);
            if (dependency.getTransitiveDependencies() != null && !dependency.getTransitiveDependencies().isEmpty()) map.putAll(getDependencies(nd));
        }
        
        return map;
    }
    
    public void injectData(Map<String, Object> data)
    {
        if (data.containsKey("mclocation") && data.get("mclocation") != null)
        {
            Data.mclocation = (File) data.get("mclocation");
        }
        
        if (data.containsKey("runtimeDeobfuscationEnabled") && data.get("runtimeDeobfuscationEnabled") != null)
        {
            Data.indevenv = (Boolean) data.get("runtimeDeobfuscationEnabled");
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
    
    public static String getChecksum(File file) throws Exception
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
}