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
    
    public static File       mclocation;
    public static File       FEfolder;
    public static File       configFile;
    public static File       modulesFolder;
    public static File       dependencyFolder;
    public static String     branch;
    public static Properties properties = new Properties();
    public static boolean    autoUpdate;
    public static boolean    firstRun;
    
    public static void main(final String[] args) throws Exception
    {
        if (GraphicsEnvironment.isHeadless())
            Main.out = new NoGui();
        else
            Main.out = new Gui();
        Main.out.init();
        Main.out.println("Welcome to the ForgeEssentials installer.");
        Main.out.println("=========================================");
        
        Main.setup();
    }
    
    public static void finish()
    {
        Main.out.println("=========================================");
        Main.out.println("End of the ForgeEssentials installer.");
        Main.out.println("The actual dowloading of the modules and libs will happen the first Minecraft launch.");
    }
    
    /**
     * Makes settings file and does folder structure.
     * 
     * @throws IOException
     */
    public static void setup() throws IOException
    {
        final File mods = new File(".").getAbsoluteFile().getParentFile();
        if (!Data.debug && !mods.getName().equalsIgnoreCase("mods"))
        {
            // This should only ever activate when run as standalone jar
            Main.out.println("You MUST run this in your mods folder.");
            Main.out.stop();
        }
        else
        {
            if (Main.mclocation == null)
            {
             // Will be run by MC and standalone jar
                Main.mclocation = mods.getParentFile();
            }
            
            Main.FEfolder = new File(Main.mclocation, "ForgeEssentials");
            if (!Main.FEfolder.exists()) Main.FEfolder.mkdirs();
            
            Main.configFile = new File(Main.FEfolder, "Coremod.properties");
            if (!Main.configFile.exists()) Main.configFile.createNewFile();
            
            Main.modulesFolder = new File(Main.FEfolder, "modules");
            if (!Main.modulesFolder.exists()) Main.modulesFolder.mkdirs();
            
            Main.dependencyFolder = new File(Main.FEfolder, "dependency");
            if (!Main.dependencyFolder.exists()) Main.dependencyFolder.mkdirs();
            
            final FileInputStream in = new FileInputStream(Main.configFile);
            Main.properties.load(in);
            in.close();
            
            if (!Main.properties.containsKey("firstRun")) Main.properties.setProperty("firstRun", "true");
            Main.firstRun = Boolean.parseBoolean(Main.properties.getProperty("firstRun"));
            
            if (!Main.properties.containsKey("autoUpdate")) Main.properties.setProperty("autoUpdate", "true");
            Main.autoUpdate = Boolean.parseBoolean(Main.properties.getProperty("autoUpdate"));
            
            /*
             * Branch stuff
             */
            if (!Main.properties.containsKey("branch")) Main.properties.setProperty("branch", "stable");
            Main.branch = Main.properties.getProperty("branch");
            if (!Main.branch.equals("stable") && !Main.branch.equals("beta") && !Main.branch.equals("dev"))
            {
                System.out.println("[" + Data.NAME + "] Branch '" + Main.branch + "' not found! Reverting to default.");
                Main.properties.setProperty("branches", "stable");
                Main.branch = "stable";
            }
            
            Main.saveProperties();
        }
    }
    
    public static void saveProperties() throws IOException
    {
        final FileOutputStream out = new FileOutputStream(Main.configFile);
        Main.properties.store(out, "Look in the readme file for more info on how to use this.");
        out.close();
    }
    
    public static void setBranch(final String branch)
    {
        Main.properties.setProperty("branch", branch.toLowerCase());
        try
        {
            Main.saveProperties();
            Main.out.println("Changed branch to " + branch);
        }
        catch (final Exception e)
        {
            e.printStackTrace();
            Main.out.print(e.getMessage());
        }
    }
    
    public static void setAutoUpdate(final boolean b)
    {
        Main.properties.setProperty("autoUpdate", b + "");
        try
        {
            Main.saveProperties();
            Main.out.println("Changed autoupdate to " + b);
        }
        catch (final Exception e)
        {
            e.printStackTrace();
            Main.out.print(e.getMessage());
        }
    }
}
