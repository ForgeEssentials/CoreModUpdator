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

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MavenDependency implements IDependency
{
    public static final String       XMLTAG_dependency = "dependency";
    public static final String       XMLTAG_groupId    = "groupId";
    public static final String       XMLTAG_artifactId = "artifactId";
    public static final String       XMLTAG_version    = "version";
    public static final String       XMLTAG_scope      = "scope";
    public static final List<String> unwantedScope     = Arrays.asList("provided", "test");

    String repoURL;
    List<IDependency> transitiveDependencies = new ArrayList<IDependency>();
    String filename;
    String hash;
    URL    dlurl;

    public MavenDependency(final String repoURL, final String name) throws IOException
    {
        this.repoURL = repoURL;
        final String[] split = name.split(":");
        this.filename = split[1] + '-' + split[2];
        this.dlurl = new URL(repoURL + split[0].replace('.',
                '/') + '/' + split[1] + '/' + split[2] + '/' + this.filename + ".jar");

        final BufferedReader in = new BufferedReader(new InputStreamReader(new URL(this.dlurl + ".sha1").openStream()));
        this.hash = in.readLine();
        in.close();

        if (this.hash.contains("  ")) this.hash = this.hash.split(" ", 2)[0];

        try
        {
            final URL pomurl = new URL(repoURL + split[0].replace('.',
                    '/') + '/' + split[1] + '/' + split[2] + '/' + this.filename + ".pom");
            final XMLInputFactory inputFactory = XMLInputFactory.newInstance();
            final XMLStreamReader reader = inputFactory.createXMLStreamReader(pomurl.openStream());

            while (reader.hasNext())
            {
                reader.next();
                if (reader.getEventType() == XMLStreamConstants.START_ELEMENT && reader.getName().getLocalPart().equals(
                        MavenDependency.XMLTAG_dependency))
                {
                    /*
                     * Got a dependency
                     */
                    String groupId = null, artifactId = null, version = null, scope = null;
                    while (reader.hasNext())
                    {
                        reader.next();

                        if (reader.getEventType() == XMLStreamConstants.END_ELEMENT && reader.getName().getLocalPart().equals(
                                MavenDependency.XMLTAG_dependency)) break;
                        else if (reader.getEventType() == XMLStreamConstants.START_ELEMENT && reader.getName().getLocalPart().equals(
                                MavenDependency.XMLTAG_groupId))
                        {
                            while (reader.next() != XMLStreamConstants.CHARACTERS)
                            {
                            }
                            groupId = reader.getText();
                            while (reader.next() != XMLStreamConstants.END_ELEMENT)
                            {
                            }
                        }
                        else if (reader.getEventType() == XMLStreamConstants.START_ELEMENT && reader.getName().getLocalPart().equals(
                                MavenDependency.XMLTAG_artifactId))
                        {
                            while (reader.next() != XMLStreamConstants.CHARACTERS)
                            {
                            }
                            artifactId = reader.getText();
                            while (reader.next() != XMLStreamConstants.END_ELEMENT)
                            {
                            }
                        }
                        else if (reader.getEventType() == XMLStreamConstants.START_ELEMENT && reader.getName().getLocalPart().equals(
                                MavenDependency.XMLTAG_version))
                        {
                            while (reader.next() != XMLStreamConstants.CHARACTERS)
                            {
                            }
                            version = reader.getText();
                            while (reader.next() != XMLStreamConstants.END_ELEMENT)
                            {
                            }
                        }
                        else if (reader.getEventType() == XMLStreamConstants.START_ELEMENT && reader.getName().getLocalPart().equals(
                                MavenDependency.XMLTAG_scope))
                        {
                            while (reader.next() != XMLStreamConstants.CHARACTERS)
                            {
                            }
                            scope = reader.getText();
                            while (reader.next() != XMLStreamConstants.END_ELEMENT)
                            {
                            }
                        }
                    }

                    if (MavenDependency.unwantedScope.contains(scope)) continue;

                    this.transitiveDependencies.add(new MavenDependency(repoURL,
                            groupId + ":" + artifactId + ":" + version));
                }
            }

            reader.close();
        }
        catch (final Exception e)
        {
            if (e instanceof RuntimeException) throw new RuntimeException(e);
            else e.printStackTrace();
        }
    }

    @Override
    public String getFileName()
    {
        return this.filename + ".jar";
    }

    @Override
    public String getHash()
    {
        return this.hash;
    }

    @Override
    public URL getDownloadURL()
    {
        return this.dlurl;
    }

    @Override
    public List<IDependency> getTransitiveDependencies()
    {
        return this.transitiveDependencies;
    }
}
