package com.forgeessentials.coremod.dependencies;

import java.net.MalformedURLException;
import java.net.URL;

import com.forgeessentials.coremod.Data;

public class DefaultDependency implements IDependency
{
    String filename;
    String hash;
    URL url;
    
    public DefaultDependency(String code) throws MalformedURLException
    {
        String[] split = code.split(":");
        this.filename = split[0];
        this.hash = split[1];
        this.url = new URL(Data.LIBURL + filename);
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
