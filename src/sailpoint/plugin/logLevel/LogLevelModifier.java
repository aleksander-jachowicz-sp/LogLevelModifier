package sailpoint.plugin.logLevel;

import java.util.ArrayList;
import java.util.Collection;
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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;

import sailpoint.rest.plugin.BasePluginResource;
import sailpoint.rest.plugin.SystemAdmin;
import sailpoint.tools.GeneralException;


@Path("logLevelModifier")
public class LogLevelModifier extends BasePluginResource {

	private static Logger log = LogManager.getLogger(LogLevelModifier.class);
	
	@Override
	public String getPluginName() {
		return "loglevelmodifier";
	}
	
	@SuppressWarnings("unchecked")
	
	@GET
	@Path("getLoggers")
	@SystemAdmin
	@Produces(MediaType.APPLICATION_JSON)
	public List<Map<String, String>> gerLoggers() throws GeneralException {
		
		log.debug("Running getLoggers. By user "+ getLoggedInUser().getName()+" having rights: "+getLoggedInUserRights());
		
		final LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
		
		Collection<org.apache.logging.log4j.core.Logger> loggers = ctx.getLoggers();
		List<Map<String, String>> loggersList = new ArrayList<>();
		
		for(org.apache.logging.log4j.core.Logger logger: loggers) {
			Map<String, String> loggerMap = new HashMap<>();
            loggerMap.put("LoggerName", logger.getName());
            loggerMap.put("Parent", (logger.getParent() == null ? null : logger.getParent().getName()));
            loggerMap.put("EffectiveLevel", String.valueOf(logger.getLevel()));
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
	

	@GET
	@Path("setLogLevel")
	@SystemAdmin
	@Produces(MediaType.APPLICATION_JSON)
	public String setLogLevel(@QueryParam("lName") String loggerName, @QueryParam("level") String level) { 
		
		log.debug("Setting logger "+loggerName+ " to "+level);
		
		LogManager.getLogger(loggerName).setLevel(Level.toLevel(level));
		
		return "OK";
	}
	
}
