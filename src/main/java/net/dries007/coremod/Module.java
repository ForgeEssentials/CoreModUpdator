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

import com.google.common.collect.HashMultimap;
import net.dries007.coremod.dependencies.DefaultDependency;
import net.dries007.coremod.dependencies.IDependency;
import net.dries007.coremod.dependencies.MavenDependency;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

public class Module
{
    public String name;
    // MUST only conain .jar files!
    public ArrayList<ModuleFile> files = new ArrayList<ModuleFile>();

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
        for (ModuleFile mFile : this.files)
        {
            try
            {
                JarFile jar = new JarFile(mFile.file);
                Manifest mf = jar.getManifest();
                if (mf != null)
                {
                    for (final Entry<Object, Object> attribute : mf.getMainAttributes().entrySet())
                    {
                        for (final String value : attribute.getValue().toString().split(" "))
                        {
                            attributes.put(attribute.getKey().toString(), value);
                        }
                    }
                    /**
                     * Reading NORMAL libs from the modules' manifest files.
                     * We want: Space sperated pairs of filename:sha1
                     */
                    if (Data.hasKey(Data.LIBKEY_NORMAL, Data.LIBURL_NORMAL))
                    {
                        String libs = mf.getMainAttributes().getValue(Data.get(Data.LIBKEY_NORMAL));
                        if (libs != null) for (final String lib : libs.split(" "))
                        {
                            DefaultDependency dependency = new DefaultDependency(lib);
                            dependecies.add(dependency);
                        }
                    }

                    /**
                     * Reading MAVEN libs from the modules' manifest files.
                     * We want: the maven name
                     */
                    if (Data.hasKey(Data.LIBKEY_MAVEN, Data.LIBURL_MAVEN))
                    {
                        String libs = mf.getMainAttributes().getValue(Data.get(Data.LIBKEY_MAVEN));
                        if (libs != null) for (final String lib : libs.split(" "))
                        {
                            MavenDependency dependency = new MavenDependency(Data.get(Data.LIBURL_MAVEN), lib);
                            dependecies.add(dependency);
                            dependecies.addAll(Coremod.getDependencies(dependency));
                        }
                    }
                    
                    /*
                     * Reading ASM classes from the modules' manifest files
                     */
                    if (Data.hasKey(Data.CLASSKEY_ASM))
                    {
                        String asmclasses = mf.getMainAttributes().getValue(Data.get(Data.CLASSKEY_ASM));
                        if (asmclasses != null) for (final String asmclass : asmclasses.split(" "))
                        {
                            this.ASMClasses.add(asmclass);
                            System.out.println("[" + Data.NAME + "] Added ASM class (" + asmclass + ") for module file " + jar.getName());
                        }
                    }
                    
                    /*
                     * Reading AT Files from the modules' manifest files
                     */
                    if (Data.hasKey(Data.FILEKEY_TA))
                    {
                        String ats = mf.getMainAttributes().getValue(Data.FILEKEY_TA);
                        if (ats != null) for (final String at : ats.split(" "))
                        {
                            this.ATFiles.add(at);
                            System.out.println("[" + Data.NAME + "] Added AccessTransformer (" + at + ") for module file " + jar.getName());
                        }
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
            if (mFile.file.exists() && mFile.hash != null && (sum = Coremod.getChecksum(mFile.file)) != null)
                if (!sum.equals(mFile.hash))
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
