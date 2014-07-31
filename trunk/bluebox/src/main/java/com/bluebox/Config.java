package com.bluebox;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.configuration.SystemConfiguration;
import org.apache.commons.configuration.reloading.FileChangedReloadingStrategy;

public class Config extends CompositeConfiguration {
	private static final Logger log = Logger.getAnonymousLogger();
	private static Config configInstance;
	public static final String BLUEBOX_VERSION 			= "bluebox_version";
	public static final String BLUEBOX_PORT 			= "bluebox_port";
	public static final String BLUEBOX_FROMBLACKLIST	= "bluebox_from_blacklist";
	public static final String BLUEBOX_TOBLACKLIST		= "bluebox_to_blacklist";
	public static final String BLUEBOX_TOWHITELIST		= "bluebox_to_whitelist";
	public static final String BLUEBOX_FROMWHITELIST	= "bluebox_from_whitelist";
	public static final String BLUEBOX_MAXCONNECTIONS 	= "bluebox_maxconnections";
	public static final String BLUEBOX_MESSAGE_AGE  	= "bluebox_message_age";
	public static final String BLUEBOX_TRASH_AGE	 	= "bluebox_trash_age";
	public static final String BLUEBOX_DAEMON_DELAY	 	= "bluebox_daemon_delay";
	public static final String BLUEBOX_STORAGE          = "bluebox_storage";
	public static final String BLUEBOX_STORAGE_HOST     = "bluebox_storage_host";
	public static final String BLUEBOX_STORAGE_HOME     = "bluebox_storage_home";
	public static final String BLUEBOX_STORAGE_CONFIG   = "bluebox_storage_config";
	public static final String BLUEBOX_STRICT_CHECKING  = "bluebox_strict";

	Preferences prefs = Preferences.systemNodeForPackage(Config.class);

	private Config() {
		addConfiguration(new SystemConfiguration());
		try {
			PropertiesConfiguration pconfig;
			pconfig = new PropertiesConfiguration("bluebox.properties");
			FileChangedReloadingStrategy strategy = new FileChangedReloadingStrategy();
			strategy.setRefreshDelay(30000);
			pconfig.setReloadingStrategy(strategy);
			addConfiguration(pconfig);
		} 
		catch (ConfigurationException e) {
			log.severe("Problem loading configuration");
			e.printStackTrace();
		}

	}

	public static Config getInstance() {
		if (configInstance==null) {
			log.info("Instanciating configuration");
			configInstance = new Config();
		}
		return configInstance;
	}
	
	public List<String> getStringList(String key) {
		List<Object> l = getList(key);
		List<String> res = new ArrayList<String>();
		for (Object o : l) {
			if (o.toString().length()>0)
				res.add(o.toString());
		}
		return res;
	}
	
	public String getFlatList(String key) {
		StringBuffer list = new StringBuffer();
		for (String s : getStringList(key)) {
			list.append(s).append(',');
		}
		if (list.length()>0)
			return list.substring(0, list.length()-1).toString();
		return "";
	}
	
	public String getString(String key) {
		return super.getString(key);
	}
	
//	public String getSavedString(String key) {
//		// check for user saved value first
//		if (StorageFactory.getInstance().hasProperty(key)) {
//			return StorageFactory.getInstance().getProperty(key,"");
//		}
//		return getString(key);
//	}
	
	public void setString(String key, String value) {
		// save the setting so it's available across restarts
		prefs.put(key, value);
		super.setProperty(key, value);
	}
	
//	public List<String> getList(String key) {
//		return super.getL
//		String props = getString(key);
//		List<String> res = new ArrayList<String>();
//		StringTokenizer tok = new StringTokenizer(props,",");
//		while (tok.hasMoreTokens()) {
//			res.add(tok.nextToken().trim());
//		}
//		return res;
//	}

}