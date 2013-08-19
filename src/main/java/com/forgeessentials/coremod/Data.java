package com.forgeessentials.coremod;

import net.minecraft.launchwrapper.LaunchClassLoader;

/**
 * Most stuff can be changed here. (c) Copyright Dries007.net 2013 Written for ForgeEssentials, but might be useful for others.
 * 
 * @author Dries007
 */
public class Data
{
    public static final String      VERSION      = "1.0";
    public static final String      MC_VERSION   = "1.6.2";
    public static final String      NAME         = "FE Loader";
    
    public static final String[]    ASMCLASSES   = { CustomAT.class.getName().toString() };
    
    public static final String      BASEURL      = "http://direct.dries007.net/ForgeEssentials/download/";
    public static final String      JSONURL      = Data.BASEURL + "json.php";
    public static final String      LIBURL       = Data.BASEURL + "libs/";
    public static final String      LOCKURL      = Data.BASEURL + "lock.php";
    
    public static final String      NORMALLIBKEY = "UpdatorLibs";
    public static final String      MAVENLIBKEY  = "UpdatorMavenLibs";
    public static final String      ASMKEY       = "UpdatorASMClasses";
    public static final String      ATKEY        = "UpdatorATs";
    
    public static boolean           debug        = false;
    public static LaunchClassLoader classLoader;
}
