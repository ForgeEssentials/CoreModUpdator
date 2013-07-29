package com.forgeessentials.coremod.dependencies;

import java.net.URL;
import java.util.List;

/**
 * (c) Copyright Dries007.net 2013 Written for ForgeEssentials, but might be
 * useful for others.
 * 
 * @author Dries007
 */
public interface IDependency
{
    public String getFileName();
    
    public String getHash();
    
    public URL getDownloadURL();
    
    public List<IDependency> getTransitiveDependencies();
}
