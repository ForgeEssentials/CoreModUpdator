package com.forgeessentials.coremod;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.apache.commons.io.FileUtils;

import com.forgeessentials.coremod.dependencies.DefaultDependency;
import com.forgeessentials.coremod.dependencies.IDependency;
import com.forgeessentials.coremod.dependencies.MavenDependency;
import com.google.common.collect.HashMultimap;

public class Module
{
    public String                       name;
    // MUST only conain .jar files!
    public ArrayList<ModuleFile>        files       = new ArrayList<ModuleFile>();
    
    public HashSet<IDependency>         dependecies = new HashSet<IDependency>();
    public HashSet<String>              ASMClasses  = new HashSet<String>();
    public HashSet<String>              ATFiles     = new HashSet<String>();
    public HashMultimap<String, String> attributes  = HashMultimap.create();
    
    public Module(final String name)
    {
        this.name = name;
    }
    
    public Module(final File file)
    {
        this.files.add(new ModuleFile(file, null, null));
        this.name = file.getName();
    }
    
    /**
     * This method gets all the dependencies, ASM classes and ATs from the files associated with this module.
     */
    public void parceJarFiles()
    {
        for (final ModuleFile mFile : this.files)
            try
            {
                final JarFile jar = new JarFile(mFile.file);
                final Manifest mf = jar.getManifest();
                if (mf != null)
                {
                    for (final Entry<Object, Object> attribute : mf.getMainAttributes().entrySet())
                        for (final String value : attribute.getValue().toString().split(" "))
                            this.attributes.put(attribute.getKey().toString(), value);
                    /*
                     * Reading NORMAL libs from the modules' manifest files We want: Space sperated pairs of filename:sha1
                     */
                    String libs = mf.getMainAttributes().getValue(Data.NORMALLIBKEY);
                    if (libs != null) for (final String lib : libs.split(" "))
                    {
                        final DefaultDependency dependency = new DefaultDependency(lib);
                        this.dependecies.add(dependency);
                    }
                    /*
                     * Reading MAVEN libs from the modules' manifest files We want: the maven name
                     */
                    libs = mf.getMainAttributes().getValue(Data.MAVENLIBKEY);
                    if (libs != null) for (final String lib : libs.split(" "))
                    {
                        final MavenDependency dependency = new MavenDependency(lib);
                        this.dependecies.add(dependency);
                        this.dependecies.addAll(Coremod.getDependencies(dependency));
                    }
                    
                    /*
                     * Reading ASM classes from the modules' manifest files
                     */
                    final String asmclasses = mf.getMainAttributes().getValue(Data.ASMKEY);
                    if (asmclasses != null) for (final String asmclass : asmclasses.split(" "))
                    {
                        this.ASMClasses.add(asmclass);
                        System.out.println("[" + Data.NAME + "] Added ASM class (" + asmclass + ") for module file " + jar.getName());
                    }
                    
                    /*
                     * Reading AT Files from the modules' manifest files
                     */
                    final String ats = mf.getMainAttributes().getValue(Data.ATKEY);
                    if (ats != null) for (final String at : ats.split(" "))
                    {
                        this.ATFiles.add(at);
                        System.out.println("[" + Data.NAME + "] Added AccessTransformer (" + at + ") for module file " + jar.getName());
                    }
                }
                jar.close();
            }
            catch (final MalformedURLException e)
            {
                e.printStackTrace();
            }
            catch (final IOException e)
            {
                e.printStackTrace();
            }
    }
    
    /**
     * Check to see if all module files exist. Checks hash, downloads new one if necessary.
     * 
     * @throws IOException
     */
    public void checkJarFiles() throws IOException
    {
        for (final ModuleFile mFile : this.files)
        {
            String sum;
            if (mFile.file.exists() && mFile.hash != null && (sum = Coremod.getChecksum(mFile.file)) != null) if (!sum.equals(mFile.hash))
            {
                System.out.println("[" + Data.NAME + "] Module " + this.name + "'s file " + mFile.file.getName() + " has wrong hash. Removing.");
                mFile.file.delete();
            }
            
            if (!mFile.file.exists())
            {
                System.out.println("[" + Data.NAME + "] Module " + this.name + "'s file " + mFile.file.getName() + " is downloading.");
                FileUtils.copyURLToFile(mFile.url, mFile.file);
            }
        }
    }
    
    public static class ModuleFile
    {
        public File   file;
        public URL    url;
        public String hash;
        
        public ModuleFile(final File file, final URL url, final String hash)
        {
            this.file = file;
            this.url = url;
            this.hash = hash;
        }
    }
}
