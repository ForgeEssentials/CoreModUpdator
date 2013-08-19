package com.forgeessentials.coremod.dependencies;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import com.forgeessentials.coremod.Data;

/**
 * (c) Copyright Dries007.net 2013 Written for ForgeEssentials, but might be useful for others.
 * 
 * @author Dries007
 */
public class DefaultDependency implements IDependency
{
    String filename;
    String hash;
    URL    url;
    
    public DefaultDependency(final String code) throws MalformedURLException
    {
        final String[] split = code.split(":");
        this.filename = split[0];
        this.hash = split[1];
        this.url = new URL(Data.LIBURL + this.filename);
    }
    
    @Override
    public String getFileName()
    {
        return this.filename;
    }
    
    @Override
    public String getHash()
    {
        return this.hash;
    }
    
    @Override
    public URL getDownloadURL()
    {
        return this.url;
    }
    
    @Override
    public List<IDependency> getTransitiveDependencies()
    {
        return null;
    }
}
