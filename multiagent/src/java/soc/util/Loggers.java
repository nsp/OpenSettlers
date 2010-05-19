/**
*	This class is used to get Loggers for the particular Agent classes
*	
**/

package soc.util;

import org.apache.log4j.*;

public class Loggers {
	
	public static Logger InterfaceAgentLogger; // root logger
	public static Logger PlanAgentLogger; // logger used in Plan Agent
	public static Logger NodeAgentLogger; // logger used in NodeAgent
	public static Logger MailAgentLogger; // logger used in MailAgent
	public static Logger TraderAgentLogger; // logger used in TraderAgent
	public static Logger RobberAgentLogger; // logger used in RobberAgent
	public static Logger CardsAgentLogger; // logger used in CardsAgent
	
	public static void init() {
		
		// initialize all the Loggers 
		InterfaceAgentLogger = Logger.getRootLogger();
		
	     //SimpleLayout layout = new SimpleLayout();

		PatternLayout layout = new PatternLayout();
		
		layout.setConversionPattern("[%p]   %C   %t   %d{dd MMM yyyy HH:mm:ss,SSS}   %m%n");
		
		 FileAppender appender = null;
		 
		 try {
		      
			 System.out.println("LOCATION :: "+ApplicationConstants.Log.STANDARD_LOG);
			 appender = new FileAppender(layout,ApplicationConstants.Log.STANDARD_LOG,false);
		     
		 } catch(Exception e) {
			 
			 e.printStackTrace();
		 }
		 
		 // DEBUG < INFO < WARN < ERROR < FATAL
		 
		 InterfaceAgentLogger.addAppender(appender); 
		 InterfaceAgentLogger.setLevel((Level) Level.DEBUG);
		 
		 PlanAgentLogger = Logger.getLogger("PlanAgent");
		 
		 //PlanAgentLogger.setAdditivity(false);
		
		 NodeAgentLogger = Logger.getLogger("PlanAgent.NodeAgent");
		 NodeAgentLogger.addAppender(appender);
		 
		 MailAgentLogger = Logger.getLogger("PlanAgent.MailAgent");
		 MailAgentLogger.addAppender(appender);

		 TraderAgentLogger = Logger.getLogger("PlanAgent.TraderAgent");
		 TraderAgentLogger.addAppender(appender);
		 
		 RobberAgentLogger = Logger.getLogger("PlanAgent.RobberAgent");
		 RobberAgentLogger.addAppender(appender);
		 
		 CardsAgentLogger = Logger.getLogger("PlanAgent.CardsAgent");
		 CardsAgentLogger.addAppender(appender);
		 
		 
	}	
}