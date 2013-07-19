package com.forgeessentials.coremod.dependencies;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;

/**
 * Added by request of AbrarSyed (c) Copyright Dries007.net 2013 Written for ForgeEssentials, but might be useful for others.
 * 
 * @author Dries007
 */
public class MavenDependency implements IDependency
{
    public static final String       XMLTAG_dependency  = "dependency";
    public static final String       XMLTAG_groupId     = "groupId";
    public static final String       XMLTAG_artifactId  = "artifactId";
    public static final String       XMLTAG_version     = "version";
    public static final String       XMLTAG_scope       = "scope";
    public static final List<String> unwantedScope      = Arrays.asList("provided");
    
    List<IDependency>                nestedDependencies = new ArrayList<IDependency>();
    String                           filename;
    String                           hash;
    URL                              dlurl;
    
    public MavenDependency(String name) throws IOException
    {
        String[] split = name.split(":");
        filename = split[1] + '-' + split[2];
        dlurl = new URL("http://repo1.maven.org/maven2/" + split[0].replace('.', '/') + '/' + split[1] + '/' + split[2] + '/' + filename + ".jar");
        
        BufferedReader in = new BufferedReader(new InputStreamReader(new URL(dlurl + ".sha1").openStream()));
        hash = in.readLine();
        in.close();
        
        if (hash.contains("  ")) hash = hash.split(" ", 2)[0];
        
        try
        {
            URL pomurl = new URL("http://repo1.maven.org/maven2/" + split[0].replace('.', '/') + '/' + split[1] + '/' + split[2] + '/' + filename + ".pom");
            XMLInputFactory inputFactory = XMLInputFactory.newInstance();
            XMLStreamReader reader = inputFactory.createXMLStreamReader(pomurl.openStream());
            
            while (reader.hasNext())
            {
                reader.next();
                if (reader.getEventType() == XMLStreamReader.START_ELEMENT && reader.getName().getLocalPart().equals(XMLTAG_dependency))
                {
                    /*
                     * Got a dependency
                     */
                    String groupId = null, artifactId = null, version = null, scope = null;
                    while (reader.hasNext())
                    {
                        reader.next();
                        
                        if (reader.getEventType() == XMLStreamReader.END_ELEMENT && reader.getName().getLocalPart().equals(XMLTAG_dependency))
                        {
                            break;
                        }
                        else if (reader.getEventType() == XMLStreamReader.START_ELEMENT && reader.getName().getLocalPart().equals(XMLTAG_groupId))
                        {
                            while (reader.next() != XMLStreamReader.CHARACTERS)
                            {}
                            groupId = reader.getText();
                            while (reader.next() != XMLStreamReader.END_ELEMENT)
                            {}
                        }
                        else if (reader.getEventType() == XMLStreamReader.START_ELEMENT && reader.getName().getLocalPart().equals(XMLTAG_artifactId))
                        {
                            while (reader.next() != XMLStreamReader.CHARACTERS)
                            {}
                            artifactId = reader.getText();
                            while (reader.next() != XMLStreamReader.END_ELEMENT)
                            {}
                        }
                        else if (reader.getEventType() == XMLStreamReader.START_ELEMENT && reader.getName().getLocalPart().equals(XMLTAG_version))
                        {
                            while (reader.next() != XMLStreamReader.CHARACTERS)
                            {}
                            version = reader.getText();
                            while (reader.next() != XMLStreamReader.END_ELEMENT)
                            {}
                        }
                        else if (reader.getEventType() == XMLStreamReader.START_ELEMENT && reader.getName().getLocalPart().equals(XMLTAG_scope))
                        {
                            while (reader.next() != XMLStreamReader.CHARACTERS)
                            {}
                            scope = reader.getText();
                            while (reader.next() != XMLStreamReader.END_ELEMENT)
                            {}
                        }
                    }
                    
                    if (unwantedScope.contains(scope)) continue;
                    
                    nestedDependencies.add(new MavenDependency(groupId + ":" + artifactId + ":" + version));
                }
            }
            
            reader.close();
        }
        catch (Exception e)
        {
            if (e instanceof RuntimeException)
                throw new RuntimeException(e);
            else
                e.printStackTrace();
        }
    }
    
    @Override
    public String getFileName()
    {
        return filename + ".jar";
    }
    
    @Override
    public String getHash()
    {
        return hash;
    }
    
    @Override
    public URL getDownloadURL()
    {
        return dlurl;
    }
    
    @Override
    public List<IDependency> getNestedDependencies()
    {
        return nestedDependencies;
    }
}
