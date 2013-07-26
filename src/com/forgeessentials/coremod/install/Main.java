package com.forgeessentials.coremod.install;


import java.awt.GraphicsEnvironment;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;

import com.forgeessentials.coremod.Data;

public class Main
{
    private static final boolean DEBUG = false;
    static IHazOut out;
    
    public static String comments = "=== READ THIS ===" + "\n# autoUpdate" + "\n#      Default: true" + "\n#      Check with the FE repo to see if there is a new version, if there is, it will be downloaded and used.";
    public static File mclocation;
    public static File FEfolder;
    public static File configFile;
    public static File modulesFolder;
    public static File libsFolder;
    public static String branch;
    public static Properties properties = new Properties();
    public static boolean autoUpdate;
    public static boolean firstRun;
    
    public static void main(String[] args) throws Exception 
    {
        if (GraphicsEnvironment.isHeadless()) out = new NoGui();
        else out = new Gui();
        setup();
        out.init();
        out.println("Welcome to the ForgeEssentials installer.");
        out.println("=========================================");
    }
    
    public static void finish()
    {
        out.println("=========================================");
        out.println("End of the ForgeEssentials installer.");
        out.println("The actual dowloading of the modules and libs will happen the first Minecraft launch.");
    }

    public static void setup() throws Exception
    {
        File mods = new File(".").getAbsoluteFile().getParentFile();
        if (!DEBUG && !mods.getName().equalsIgnoreCase("mods"))
        {
            out.println("You MUST run this in your mods folder.");
            out.stop();
        }
        else
        {
            mclocation = mods.getParentFile();
            FEfolder = new File(mclocation, "ForgeEssentials");
            if (!FEfolder.exists()) FEfolder.mkdirs();
            
            configFile = new File(FEfolder, "Coremod.properties");
            if (!configFile.exists()) configFile.createNewFile();
            
            modulesFolder = new File(FEfolder, "modules");
            if (!modulesFolder.exists()) modulesFolder.mkdirs();
            
            libsFolder = new File(FEfolder, "libs");
            if (!libsFolder.exists()) libsFolder.mkdirs();
            
            FileInputStream in = new FileInputStream(configFile);
            properties.load(in);
            in.close();
            
            if(!properties.containsKey("firstRun")) properties.setProperty("properties", "true");
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
    
    public static void saveProperties() throws Exception
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
