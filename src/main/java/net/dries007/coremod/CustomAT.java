/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2013 Dries K. Aka Dries007
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package net.dries007.coremod;

import cpw.mods.fml.common.asm.transformers.AccessTransformer;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;

/**
 * Thank ChickenBones for this one. (CodeChickenCore/codechicken/core/asm/CodeChickenAccessTransformer.java)
 *
 * @author Dries007
 */
public class CustomAT extends AccessTransformer
{
    private static CustomAT instance;
    private static List<String> mapFileList = new LinkedList<String>();

    public CustomAT() throws IOException
    {
        super();
        CustomAT.instance = this;
        for (final String file : CustomAT.mapFileList)
        {
            this.readMapFile(file);
        }

        CustomAT.mapFileList = null;
    }

    public static void addTransformerMap(final String mapFile)
    {
        if (CustomAT.instance == null) CustomAT.mapFileList.add(mapFile);
        else CustomAT.instance.readMapFile(mapFile);
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
