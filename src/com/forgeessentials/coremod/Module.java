package com.forgeessentials.coremod;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.apache.commons.io.FileUtils;

import com.forgeessentials.coremod.dependencies.DefaultDependency;
import com.forgeessentials.coremod.dependencies.IDependency;
import com.forgeessentials.coremod.dependencies.MavenDependency;

public class Module
{   
    public String name;
    // MUST only conain .jar files!
    public ArrayList<ModuleFile> files = new ArrayList<ModuleFile>();
    public boolean wanted;
    
    public HashMap<String, IDependency> dependecies = new HashMap<String, IDependency>();
    public HashSet<String> ASMClasses = new HashSet<String>();
    public HashSet<String> ATFiles = new HashSet<String>();
    
    public Module(String name)
    {
        this.name = name;
    }
    
    public Module(File file)
    {
        files.add(new ModuleFile(file, null, null));
        this.name = file.getName();
        this.wanted = true;
    }
    
    /**
     * This method gets all the dependencies, ASM classes and ATs from the files associated with this module.
     */
    public void parceJarFiles()
    {
        if (!wanted) return;
        for (ModuleFile mFile : files)
        {
            try
            {
                JarFile jar = new JarFile(mFile.file);
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
                            dependecies.put(dependency.getFileName(), dependency);
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
                            dependecies.put(dependency.getFileName(), dependency);
                            dependecies.putAll(Coremod.getDependencies(dependency));
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
            catch (MalformedURLException e)
            {
                e.printStackTrace();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Check to see if all module files exist.
     * Checks hash, downloads new one if necessary.
     * @throws IOException 
     */
    public void checkJarFiles() throws IOException
    {
        if (!wanted) return;
        for (ModuleFile mFile : files)
        {
            String sum;
            if (mFile.file.exists() && mFile.hash != null && (sum = Coremod.getChecksum(mFile.file)) != null)
            {
                if (!sum.equals(mFile.hash))
                {
                    System.out.println("[" + Data.NAME + "] Module " + name + "'s file " + mFile.file.getName() + " has wrong hash. Removing.");
                    mFile.file.delete();
                }
            }
            
            if (!mFile.file.exists())
            {
                System.out.println("[" + Data.NAME + "] Module " + name + "'s file " + mFile.file.getName() + " is downloading.");
                FileUtils.copyURLToFile(mFile.url, mFile.file);
            }
        }
    }
    
    public static class ModuleFile
    {
        public File file;
        public URL url;
        public String hash;
        
        public ModuleFile(File file, URL url, String hash)
        {
            this.file = file;
            this.url = url;
            this.hash = hash;
        }
    }
}
