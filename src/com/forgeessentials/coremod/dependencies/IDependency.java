package com.forgeessentials.coremod.dependencies;

import java.net.URL;

public interface IDependency
{   
    public String getFileName();
    public String getHash();
    public URL getDownloadURL();
}
