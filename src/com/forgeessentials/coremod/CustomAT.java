package com.forgeessentials.coremod;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;

import cpw.mods.fml.common.asm.transformers.AccessTransformer;

/**
 * Thank ChickenBones for this one.
 * (CodeChickenCore/codechicken/core/asm/CodeChickenAccessTransformer.java)
 * 
 * @author Dries007
 */
public class CustomAT extends AccessTransformer
{
    private static CustomAT     instance;
    private static List<String> mapFileList = new LinkedList<String>();
    
    public CustomAT() throws IOException
    {
        super();
        instance = this;
        for (String file : mapFileList)
            readMapFile(file);
        
        mapFileList = null;
    }
    
    public static void addTransformerMap(String mapFile)
    {
        if (instance == null)
            mapFileList.add(mapFile);
        else
            instance.readMapFile(mapFile);
    }
    
    private void readMapFile(String mapFile)
    {
        try
        {
            Method parentMapFile = AccessTransformer.class.getDeclaredMethod("readMapFile", String.class);
            parentMapFile.setAccessible(true);
            parentMapFile.invoke(this, mapFile);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }
}
