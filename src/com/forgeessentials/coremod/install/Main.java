package com.forgeessentials.coremod.install;

import java.awt.GraphicsEnvironment;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import com.forgeessentials.coremod.Data;

public class Main
{
    static IHazOut           out;
    
    public static String     comments   = "=== READ THIS ===" + "\n# autoUpdate" + "\n#      Default: true" + "\n#      Check with the FE repo to see if there is a new version, if there is, it will be downloaded and used.";
    public static File       mclocation;
    public static File       FEfolder;
    public static File       configFile;
    public static File       modulesFolder;
    public static File       dependencyFolder;
    public static String     branch;
    public static Properties properties = new Properties();
    public static boolean    autoUpdate;
    public static boolean    firstRun;
    
    public static void main(String[] args) throws Exception
    {
        if (GraphicsEnvironment.isHeadless())
            out = new NoGui();
        else
            out = new Gui();
        out.init();
        out.println("Welcome to the ForgeEssentials installer.");
        out.println("=========================================");
        
        setup();
    }
    
    public static void finish()
    {
        out.println("=========================================");
        out.println("End of the ForgeEssentials installer.");
        out.println("The actual dowloading of the modules and libs will happen the first Minecraft launch.");
    }
    
    /**
     * Makes settings file and does folder structure.
     * 
     * @throws IOException
     */
    public static void setup() throws IOException
    {
        File mods = new File(".").getAbsoluteFile().getParentFile();
        if (!Data.debug && !mods.getName().equalsIgnoreCase("mods"))
        {
            // This should only ever activate when run as standalone jar
            out.println("You MUST run this in your mods folder.");
            out.stop();
        }
        else
        {
            // Will be run by MC and standalone jar
            mclocation = mods.getParentFile();
            FEfolder = new File(mclocation, "ForgeEssentials");
            if (!FEfolder.exists()) FEfolder.mkdirs();
            
            configFile = new File(FEfolder, "Coremod.properties");
            if (!configFile.exists()) configFile.createNewFile();
            
            modulesFolder = new File(FEfolder, "modules");
            if (!modulesFolder.exists()) modulesFolder.mkdirs();
            
            dependencyFolder = new File(FEfolder, "dependency");
            if (!dependencyFolder.exists()) dependencyFolder.mkdirs();
            
            FileInputStream in = new FileInputStream(configFile);
            properties.load(in);
            in.close();
            
            if (!properties.containsKey("firstRun")) properties.setProperty("firstRun", "true");
            firstRun = Boolean.parseBoolean(properties.getProperty("firstRun"));
            
            if (!properties.containsKey("autoUpdate")) properties.setProperty("autoUpdate", "true");
            autoUpdate = Boolean.parseBoolean(properties.getProperty("autoUpdate"));
            
            /*
             * Branch stuff
             */
            comments += "\n# Branch" + "\n#      Default: stable" + "\n#      Possible values: dev, beta, stable" + "\n#      Use this to change wich kind of release you want.";
            if (!properties.containsKey("branch")) properties.setProperty("branch", "stable");
            branch = properties.getProperty("branch");
            if (!branch.equals("stable") && !branch.equals("beta") && !branch.equals("dev"))
            {
                System.out.println("[" + Data.NAME + "] Branch '" + branch + "' not found! Reverting to default.");
                properties.setProperty("branches", "stable");
                branch = "stable";
            }
            
            saveProperties();
        }
    }
    
    public static void saveProperties() throws IOException
    {
        FileOutputStream out = new FileOutputStream(Main.configFile);
        properties.store(out, Main.comments);
        out.close();
    }
    
    public static void setBranch(String branch)
    {
        properties.setProperty("branch", branch.toLowerCase());
        try
        {
            saveProperties();
            out.println("Changed branch to " + branch);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            out.print(e.getMessage());
        }
    }
    
    public static void setAutoUpdate(boolean b)
    {
        properties.setProperty("autoUpdate", b + "");
        try
        {
            saveProperties();
            out.println("Changed autoupdate to " + b);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            out.print(e.getMessage());
        }
    }
}
