package com.forgeessentials.coremod.dependencies;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

/**
 * Added by request of AbrarSyed (c) Copyright Dries007.net 2013 Written for ForgeEssentials, but might be useful for others.
 * 
 * @author Dries007
 */
public class MavenDependency implements IDependency
{
    String filename;
    String hash;
    URL    url;
    
    public MavenDependency(String name) throws IOException
    {
        String[] split = name.split(":");
        filename = split[1] + '-' + split[2] + ".jar";
        url = new URL("http://repo1.maven.org/maven2/" + split[0].replace('.', '/') + '/' + split[1] + '/' + split[2] + '/' + filename);
        
        BufferedReader in = new BufferedReader(new InputStreamReader(new URL(url + ".sha1").openStream()));
        hash = in.readLine();
        in.close();
    }
    
    @Override
    public String getFileName()
    {
        return filename;
    }
    
    @Override
    public String getHash()
    {
        return hash;
    }
    
    @Override
    public URL getDownloadURL()
    {
        return url;
    }
}
