package com.forgeessentials.coremod.install;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class NoGui implements IHazOut
{
    
    static BufferedReader read;
    public void init() throws Exception
    {
        read = new BufferedReader(new InputStreamReader(System.in));
        
        autoUpdate();
        branchMenu();
        
        read.close();
    }
    
    private void autoUpdate() throws Exception
    {
        System.out.println("> Do you want automatic updates? <");
        System.out.println("[0] Yes");
        System.out.println("[1] No");
        
        String line = read.readLine();
        try
        {
            switch (Integer.parseInt(line))
            {
                case 0:
                    Main.setAutoUpdate(true);
                    break;
                case 1:
                    Main.setAutoUpdate(false);
                    break;
                default :
                    System.out.println("That is not a valid choise.");
                    autoUpdate();
            }
        }
        catch (Exception e)
        {
            System.out.println("That is not a valid number.");
            autoUpdate();
        }
    }
    
    private void branchMenu() throws Exception
    {
        System.out.println("> Branch menu <");
        System.out.println("-------------");
        System.out.println("[0] Stable");
        System.out.println("[1] Beta");
        System.out.println("[2] Dev");
        
        String line = read.readLine();
        try
        {
            switch (Integer.parseInt(line))
            {
                case 0:
                    Main.setBranch("stable");
                    break;
                case 1:
                    Main.setBranch("beta");
                    break;
                case 2:
                    Main.setBranch("dev");
                    break;
                default :
                    System.out.println("That is not a valid choise.");
                    branchMenu();
            }
        }
        catch (Exception e)
        {
            System.out.println("That is not a valid number.");
            branchMenu();
        }
    }

    @Override
    public void print(Object o)
    {
        System.out.print(o);
    }

    @Override
    public void println(Object o)
    {
        System.out.println(o);
    }

    @Override
    public void stop()
    {
        System.exit(1);
    }
}
