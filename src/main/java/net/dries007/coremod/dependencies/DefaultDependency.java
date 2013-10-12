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

package net.dries007.coremod.dependencies;

import net.dries007.coremod.Data;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

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
        this.url = new URL(Data.get(Data.LIBURL_NORMAL) + this.filename);
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
