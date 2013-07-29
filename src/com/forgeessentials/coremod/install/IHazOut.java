package com.forgeessentials.coremod.install;

public interface IHazOut
{
    public void init() throws Exception;
    
    public void print(Object o);
    
    public void println(Object o);
    
    public void stop();
}
