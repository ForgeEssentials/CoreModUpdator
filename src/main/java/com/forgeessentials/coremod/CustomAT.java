package com.forgeessentials.coremod;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;

import cpw.mods.fml.common.asm.transformers.AccessTransformer;

/**
 * Thank ChickenBones for this one. (CodeChickenCore/codechicken/core/asm/CodeChickenAccessTransformer.java)
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
        CustomAT.instance = this;
        for (final String file : CustomAT.mapFileList)
            this.readMapFile(file);
        
        CustomAT.mapFileList = null;
    }
    
    public static void addTransformerMap(final String mapFile)
    {
        if (CustomAT.instance == null)
            CustomAT.mapFileList.add(mapFile);
        else
            CustomAT.instance.readMapFile(mapFile);
    }
    
    private void readMapFile(final String mapFile)
    {
        try
        {
            final Method parentMapFile = AccessTransformer.class.getDeclaredMethod("readMapFile", String.class);
            parentMapFile.setAccessible(true);
            parentMapFile.invoke(this, mapFile);
        }
        catch (final Exception e)
        {
            throw new RuntimeException(e);
        }
    }
}
