package sailpoint.plugin.logLevel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.AppenderRef;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.Level;

import sailpoint.rest.plugin.BasePluginResource;
import sailpoint.rest.plugin.SystemAdmin;
import sailpoint.tools.GeneralException;


@Path("logLevelModifier")
public class LogLevelModifier extends BasePluginResource {

	protected static final org.apache.logging.log4j.Logger log = LogManager.getLogger();

	
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
		log.trace("Setting logger "+loggerName+ " to "+level);
		
		final LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
		Configuration configuration = ctx.getConfiguration();
		LoggerConfig lc2 = configuration.getLoggerConfig(loggerName);
		
		lc2.setLevel(Level.toLevel(level));
		ctx.updateLoggers(configuration);

		return "OK";
	}
	
	public void deleteme() {

		// parameters
		String logNameFilter = null;
		String logNameFilterType = null;
		String containsFilter = "Contains";
		String targetOperation = null;
		String targetLogger = null;
		String targetLogLevel = null;
		// end parameters

		final LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
		Logger rootLogger = ctx.getRootLogger();
		LoggerConfig rootLoggerConfig = rootLogger.get();
		Collection<Logger> allLoggers = new ArrayList<Logger>();

		// add all of the active loggers
		allLoggers.addAll(ctx.getLoggers());

		// add all of the loggers explicitly defined in the config file
		Configuration configuration = ctx.getConfiguration();
		Map<String, LoggerConfig> loggerConfigMap = configuration.getLoggers();
		if (loggerConfigMap != null) {
			for (String loggerName : loggerConfigMap.keySet()) {
				Logger logger = ctx.getLogger(loggerName);
				allLoggers.add(logger);
			}
		}
		
		// respect the filter, if present
		HashMap loggersMap = new HashMap(128);
		for (Logger logger : allLoggers) {
			String loggerName = (logger == rootLogger ? "root" : logger.getName());
			if (logNameFilter == null || logNameFilter.trim().length() == 0) {
				if (loggerName != null && loggerName.length() > 0) {
					loggersMap.put(loggerName, logger);
				}
			} else if (containsFilter.equals(logNameFilterType)) {
				if (loggerName.toUpperCase().indexOf(logNameFilter.toUpperCase()) >= 0) {
					loggersMap.put(loggerName, logger);
				}
			} else {
				// Either was no filter in IF, contains filter in ELSE IF, or begins with in
				// ELSE
				if (logger.getName().startsWith(logNameFilter)) {
					loggersMap.put(loggerName, logger);
				}
			}
		}
		Set loggerKeys = loggersMap.keySet();
		String[] keys = new String[loggerKeys.size()];
		keys = (String[]) loggerKeys.toArray(keys);
		Arrays.sort(keys, String.CASE_INSENSITIVE_ORDER);
		for (int i = 0; i < keys.length; i++) {
			Logger logger = (Logger) loggersMap.get(keys[i]);
			String loggerName = (logger == rootLogger ? "root" : logger.getName());

			// MUST CHANGE THE LOG LEVEL ON LOGGER BEFORE GENERATING THE LINKS AND THE
			// CURRENT LOG LEVEL OR DISABLED LINK WON'T MATCH THE NEWLY CHANGED VALUES
			if ("changeLogLevel".equals(targetOperation) && targetLogger.equals(loggerName)) {
				LoggerConfig lc2 = configuration.getLoggerConfig(logger.getName());
				if (!lc2.getName().equals(logger.getName())) {
					// We have to create a new LoggerConfig
					Level level = Level.toLevel(targetLogLevel);
					LoggerConfig current = lc2;
					List<AppenderRef> appenderRefs = new ArrayList<AppenderRef>();
					while (current != null) {
						List<AppenderRef> currentRefs = current.getAppenderRefs();
						appenderRefs.addAll(currentRefs);
						current = current.getParent();
					}
					AppenderRef[] refs = (AppenderRef[]) appenderRefs.toArray(new AppenderRef[0]);
					current = lc2;
					Map<String, Appender> appenders = new HashMap<>();
					while (current != null) {
						Map<String, Appender> currentAppenders = current.getAppenders();
						appenders.putAll(currentAppenders);
						current = current.getParent();
					}
					LoggerConfig loggerConfig = LoggerConfig.createLogger(false, level, loggerName, "true", refs, null,
							configuration, null);
					for (String key : appenders.keySet()) {
						Appender appender = appenders.get(key);
						loggerConfig.addAppender(appender, null, null);
					}
					configuration.addLogger(loggerName, loggerConfig);
					ctx.updateLoggers(configuration);
				} else {
					// just need to update the existing LoggerConfig
					lc2.setLevel(Level.toLevel(targetLogLevel));
					ctx.updateLoggers(configuration);
				}
			}

			// get values for the table columns
			String loggerEffectiveLevel = null;
			String loggerParent = null;
			if (logger != null) {
				LoggerConfig cfg = logger.get();
				loggerEffectiveLevel = String.valueOf(cfg.getLevel());
				if (cfg.getParent() == null) {
					// root logger
					loggerParent = "root";
				} else {
					if (cfg.getName() != null && cfg.getName().trim().length() > 0) {
						if (cfg.getName().equals(loggerName)) {
							loggerParent = cfg.getParent().getName();
							if (rootLoggerConfig == cfg.getParent()) {
								loggerParent = "root";
							}
						} else {
							loggerParent = cfg.getName();
						}
					} else {
						loggerParent = cfg.getParent().getName();
					}
				}
			}
			System.out.println(loggerName);
			System.out.println(loggerParent);
			System.out.println(loggerEffectiveLevel);
		}
	}
}
