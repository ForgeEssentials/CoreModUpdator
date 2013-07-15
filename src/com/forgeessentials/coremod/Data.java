package com.forgeessentials.coremod;

import java.io.File;

import net.minecraft.launchwrapper.LaunchClassLoader;

/**
 * Most stuff can be changed here.
 * 
 * @author Dries007
 * (c) Copyright  Dries007.net 2013
 * 
 * Written for ForgeEssentials, but might be useful for others.
 */
public class Data
{
    public static final String      MC_VERSION  = "1.6.2";
    public static final String      NAME        = "FE Core";
    
    public static final String      BASEPACKAGE = Data.class.getPackage().getName();
    public static final String      SETUPCLASS  = Coremod.class.getName();
    public static final String[]    ASMCLASS    = { ASM.class.getName() };
    
    public static final String      JSONURL     = "http://driesgames.game-server.cc/ForgeEssentials/";
    public static final String      LIBURL      = "http://driesgames.game-server.cc/ForgeEssentials/libs/";
    
    public static final String      LIBKEY      =  "UpdatorLibs";
    
    public static File              mclocation;
    public static LaunchClassLoader classLoader;
}
