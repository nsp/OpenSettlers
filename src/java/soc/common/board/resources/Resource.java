package soc.common.board.resources;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

/*
 * Enum design pattern for the resource types. 
 * Each enum member is actually implmented as a class definition
 */
public class Resource
{
    public String name;

    @Override
    public String toString()
    {
        return "Resource [name=" + name + "]";
    }
    
    public Resource copy() throws CloneNotSupportedException
    {
        return (Resource)super.clone();
    }
}
