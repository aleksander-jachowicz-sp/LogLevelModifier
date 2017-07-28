package sailpoint.plugin.logLevel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import sailpoint.rest.plugin.AllowAll;
import sailpoint.rest.plugin.BasePluginResource;


@Path("logLevelModifier")
public class LogLevelModifier extends BasePluginResource {

	@Override
	public String getPluginName() {
		return "loglevelmodifier";
	}
	
	@AllowAll	
	@GET
	@Path("getLoggers")
	@Produces(MediaType.APPLICATION_JSON)
	public List<Map<String, String>> gerLoggers() {
		
		Enumeration<Logger> loggers = LogManager.getCurrentLoggers();
		List<Map<String, String>> loggersList = new ArrayList<>();
		
        while (loggers.hasMoreElements()) {
            Logger logger = loggers.nextElement();
            
            Map<String, String> loggerMap = new HashMap<>();
            loggerMap.put("LoggerName", logger.getName());
            loggerMap.put("Parent", (logger.getParent() == null ? null : logger.getParent().getName()));
            loggerMap.put("EffectiveLevel", String.valueOf(logger.getEffectiveLevel()));
            loggerMap.put("show","true");
            
            loggersList.add(loggerMap);  
        }
		
        Comparator<Map<String, String>> mapComparator = new Comparator<Map<String, String>>() {
            public int compare(Map<String, String> m1, Map<String, String> m2) {
                return m1.get("LoggerName").compareTo(m2.get("LoggerName"));
            }
        };
        
        Collections.sort(loggersList, mapComparator);
        
        return loggersList;
	}
	
	@AllowAll	
	@GET
	@Path("setLogLevel")
	@Produces(MediaType.APPLICATION_JSON)
	public String setLogLevel(@QueryParam("lName") String loggerName, @QueryParam("level") String level) { 
		
		System.out.println("Setting logger "+loggerName+ " to "+level);
		LogManager.getLogger(loggerName).setLevel(Level.toLevel(level));
		
		return "OK";
	}
	
}
