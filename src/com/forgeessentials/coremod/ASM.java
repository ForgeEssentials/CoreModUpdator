package com.forgeessentials.coremod;

import net.minecraft.launchwrapper.IClassTransformer;

/**
 * TODO Add ASM code.
 * 
 * @author Dries007
 * (c) Copyright  Dries007.net 2013
 * 
 * Written for ForgeEssentials, but might be useful for others.
 */
public class ASM implements IClassTransformer
{
    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes)
    {
        return bytes;
    }
}
