/**
 * Java Settlers - An online multiplayer version of the game Settlers of Catan
 * Copyright (C) 2003  Robert S. Thomas
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 * The author of this program can be reached at thomas@infolab.northwestern.edu
 **/
package soc.server;

import soc.disableDebug.D;
import soc.server.database.SOCDBHelper;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

public class Main
{
    private static final String DEFAULT_PROPERTIES_NAME = "jsettlersd.properties";
    
    /** Server port, which must be passed to constructor of server. */
    private static int port = SOCServer.DEFAULT_PORT;
    
    /**
     * Starting the server from the command line
     *
     * @param args see output of printUsage
     */
    static public void main(String[] args)
    {
    	//System.out.println("i am in main");
    	
        port = SOCServer.DEFAULT_PORT;
        
        //System.out.println("i am after port");
        
        try {
            Properties props = processArgs(args);
            
            //System.out.println("i am after process args");
            
            //System.out.println(props);
        	
            //System.out.println("port is :: "+port);
            
            SOCServer server = new SOCServer(port, props);
                       
//            
            server.start();
            
        }
        catch (Throwable t) {
        	//System.out.println("exception man!");
            printMessage(t);
            System.exit(1);
        }
    }

    /**
     * Print the message of the throwable if it is not null. 't' must not be
     * null.
     */
    private static void printMessage(Throwable t) {
        String message = t.getMessage();
        if (message != null) {
            System.err.println(message);
        }
    }

    /**
     * Read commandline args and store them into a Properties object for the
     * server. Reads property file with is shadowed by arguments on command line.
     */
    private static Properties processArgs(String[] args)
    {
        // properties which will be read from property file
        Properties props = new Properties();
        
        // command line properties take precidence over property file values
        Properties cmdProps = new Properties(props);
        
        // File containing server properties
        File propertyFile = null;

        // process command line args
        int i=0;
        for (i=0; i<args.length; i++) {
            String arg = args[i];

            if (arg.equals("-help") || arg.equals("-h")) {
                printUsage();
                System.exit(0);
            }
            else if (arg.equals("-version")) {

            }
            else if (arg.equals("-port") || arg.equals("-p")) {
                if (i < args.length -1) {
                    cmdProps.setProperty(SOCServer.JSETTLERS_PORT, args[++i]);
                }
                else {
                    String msg = "You must specify a port number when " +
                        "using the -port option";
                    throw new RuntimeException(msg);
                }
            }
            else if (arg.equals("-propertyfile") || arg.equals("-f")) {
                if (i < args.length -1) {
                    propertyFile = new File(args[++i]);
                }
                else {
                    String msg = "You must specify a property file when " +
                        "using the -propertyfile option";
                    throw new RuntimeException(msg);
                }
            }
            else if (arg.startsWith("-D")) {
                
                // We get to here when a user uses -Dname=value. However, in
                // some cases, the OS goes ahead and parses this out to args
                //   {"-Dname", "value"}
                // so instead of parsing on "=", we just make the "-D"
                // characters go away and skip one argument forward.

                String name = arg.substring(2, arg.length());
                String value = null;
                int posEq = name.indexOf("=");
                if (posEq > 0) {
                    value = name.substring(posEq + 1);
                    name = name.substring(0, posEq);
                }
                else if (i < args.length - 1) {
                    value = args[++i];
                }
                else {
                    throw new RuntimeException("Missing value for property " + name);
                }

                cmdProps.put(name, value);
            }
            else if (arg.equals("-nice")) {
                if (i < args.length - 1) {
                    cmdProps.setProperty(SOCServer.JSETTLERS_NICE, args[++i]);
                }
                else {
                    throw new RuntimeException("You must specify a nice value (1-10) " +
                                               "when using the -nice option");
                }
            }
            else if (arg.startsWith("-")) {
                // we don't have any more args to recognize!
                String msg = "Unknown argument: " + arg;
                System.out.println(msg);
                printUsage();
                System.exit(1);
            }
            else {
                // done reading arguments
                break;
            }
        }

        // old style command line arguments are deprecated, but take precidence
        if (i < args.length)
            cmdProps.setProperty(SOCServer.JSETTLERS_PORT, args[i++]);
        if (i < args.length)
            cmdProps.setProperty(SOCServer.JSETTLERS_CONNECTIONS, args[i++]);
        if (i < args.length)
            cmdProps.setProperty(SOCDBHelper.JSETTLERS_DB_USER, args[i++]);
        if (i < args.length)
            cmdProps.setProperty(SOCDBHelper.JSETTLERS_DB_PASS, args[i++]);

        // read propertyfile into props, which is shadowed by command line args
        readProperties(props, propertyFile);

        // we need the port to create the server
        String portVal = cmdProps.getProperty(SOCServer.JSETTLERS_PORT);
        try {
            port = (portVal == null ? port : Integer.parseInt(portVal));
        }
        catch (NumberFormatException x) {
            throw new RuntimeException("Invalid port specification: "+portVal, x);
        }

        return cmdProps;
    }

    private static void printUsage()
    {
        String nl = System.getProperty("line.separator");
        StringBuffer m = new StringBuffer()
          .append("USAGE: jsettlersd [options]"+nl)
          .append("   or: jsettlersd [options] [port max-connections db-user db-pass]]"+nl)
          .append("Startup options are read from jsettersd.properties in JSETTLERS_HOME,"+nl)
          .append("unless the -porpertyfile option us used. Command line option (keys)"+nl)
          .append("are shown for options which may be set in the properties file."+nl+nl)
          .append("Options take precidence over arguments, which take precidence over"+nl)
          .append("property file values."+nl+nl)
          .append("Options:"+nl)
          .append("  -help, -h              print this message"+nl)
          .append("  -version               print the version information and exit"+nl)
          //.append("  -verbose, -v           be extra verbose (")
          //.append               (SOCServer.JSETTLERS_OUTPUT+")"+nl)
          //.append("  -debug, -d             print debugging information (")
          //.append               (SOCServer.JSETTLERS_OUTPUT+")"+nl)
          .append("  -port, -p              listen on the specified port (")
          .append               (SOCServer.JSETTLERS_PORT+")"+nl)
          .append("  -propertyfile <file>   use given property file"+nl)
          .append("    -f          <file>         ''"+nl)
          .append("  -D<property>=<value>   use value for given property"+nl)
          .append("  -nice <number>         A niceness value for main thread priority:"+nl)
          .append("                           1 (lowest) to 10 (highest). default=5"+nl)
          .append("                           ("+SOCServer.JSETTLERS_NICE+")"+nl);
    }

    /**
     * Read the specified propertyfile into the Properties object. If propFile
     * is <code>null</code> then the default property file is read from
     * JSETTLERS_HOME.
     *
     * @param props non-null properties to hold contents of property file
     * @param propFile property file to read, which may be null, to read the default
     */
    private static void readProperties(Properties props, File propFile)
    {
    	
    		//System.out.println("propFile :: "+propFile.getName());
        // Fail if a manually specified property file doesn't exist
        if (propFile != null) {
            if (! propFile.exists())
                throw new RuntimeException("Property file does not exist: "+propFile);
        }
        else {
            // jsettlers.home should be set by startup script
            //String home = System.getProperty("jsettlers.home");
        	String home = "C:\\JBuilder2007\\workspace\\jsettlers\\target";
        	
            if (home != null) {
            	
            	
                propFile = new File(home,  DEFAULT_PROPERTIES_NAME);
                if (! propFile.exists()) {
                    System.err.println ("Property file not found: " + propFile);
                    propFile = null;
                }
            }
            else
            {
                System.err.println("jsettlers.home unspecified: using default settings");
            }
        }

        try {
            if (propFile != null)
                props.load(new BufferedInputStream(new FileInputStream(propFile)));
        }
        catch (IOException x) {
            throw new RuntimeException("Error reading property file: "+propFile, x);
        }
    }
}
