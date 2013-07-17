package com.forgeessentials.coremod;

import java.io.File;

import net.minecraft.launchwrapper.LaunchClassLoader;

/**
 * Most stuff can be changed here. (c) Copyright Dries007.net 2013 Written for ForgeEssentials, but might be useful for others.
 * 
 * @author Dries007
 */
public class Data
{
    public static final String      MC_VERSION   = "1.6.2";
    public static final String      NAME         = "FE Core";
    
    public static final String[]    ASMCLASSES   = { CustomAT.class.getName().toString() };
    
    public static final String      JSONURL      = "http://driesgames.game-server.cc/ForgeEssentials/";
    public static final String      LIBURL       = "http://driesgames.game-server.cc/ForgeEssentials/libs/";
    
    public static final String      NORMALLIBKEY = "UpdatorLibs";
    public static final String      MAVENLIBKEY  = "UpdatorMavenLibs";
    public static final String      ASMKEY       = "UpdatorASMClasses";
    public static final String      ATKEY        = "UpdatorATs";
    
    public static boolean           indevenv;
    public static File              mclocation;
    public static LaunchClassLoader classLoader;
}
