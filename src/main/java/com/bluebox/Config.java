package com.bluebox;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
//import java.util.prefs.Preferences;
import java.util.StringTokenizer;

import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.configuration.SystemConfiguration;
import org.apache.commons.configuration.reloading.FileChangedReloadingStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Config extends CompositeConfiguration {
	private static final Logger log = LoggerFactory.getLogger(Config.class);
	public static final String CONFIG_NAME = "bluebox.properties";
	public static final String HCUSTOM_PROPERTIES   = "bluebox.custom_properties";

	private static Config configInstance;
	public static final String BLUEBOX_VERSION 			= "bluebox_version";
	public static final String BLUEBOX_PORT 			= "bluebox_port";
	public static final String BLUEBOX_MAIL_LIMIT		= "bluebox_mail_limit";
	public static final String BLUEBOX_SMTPBLACKLIST	= "bluebox_smtp_blacklist";
	public static final String BLUEBOX_FROMBLACKLIST	= "bluebox_from_blacklist";
	public static final String BLUEBOX_TOBLACKLIST		= "bluebox_to_blacklist";
	public static final String BLUEBOX_TOWHITELIST		= "bluebox_to_whitelist";
	public static final String BLUEBOX_FROMWHITELIST	= "bluebox_from_whitelist";
	public static final String BLUEBOX_MAXCONNECTIONS 	= "bluebox_maxconnections";
	public static final String BLUEBOX_MESSAGE_AGE  	= "bluebox_message_age";
	public static final String BLUEBOX_MESSAGE_MAX  	= "bluebox_message_max";
	public static final String BLUEBOX_TRASH_AGE	 	= "bluebox_trash_age";
	public static final String BLUEBOX_DAEMON_DELAY	 	= "bluebox_daemon_delay";
	public static final String BLUEBOX_STORAGE          = "bluebox_storage";
	public static final String BLUEBOX_STORAGE_HOST     = "bluebox_storage_host";
	public static final String BLUEBOX_STORAGE_HOME     = "bluebox_storage_home";
	public static final String BLUEBOX_STORAGE_CONFIG   = "bluebox_storage_config";
	public static final String BLUEBOX_STRICT_CHECKING  = "bluebox_strict";
	public static final String BLUEBOX_HELPNAME  		= "bluebox_helpname";
	public static final String BLUEBOX_HELPMAIL  		= "bluebox_helpmail";
	public static final String BLUEBOX_HELPPHOTO  		= "bluebox_helpphoto";

	//	Preferences prefs = Preferences.systemNodeForPackage(Config.class);

	private Config() {
		setupSystem();
		setupCustom();
		setupBuiltin();
	}

	/**
	 * Setup builtin.
	 */
	private void setupBuiltin() {
		try {
			log.info("Loading built-in config ({})",getClass().getResource("/"+CONFIG_NAME));
			addConfiguration(new PropertiesConfiguration(getClass().getResource("/"+CONFIG_NAME)));
		}
		catch (Throwable t) {
			log.warn("Problem loading built-in config :{}",t.getMessage());
		}
	}

	/**
	 * Setup custom.
	 */
	private void setupCustom() {
		try {
			PropertiesConfiguration builtin = new PropertiesConfiguration(getClass().getResource("/"+CONFIG_NAME));
			PropertiesConfiguration pconfig;
			String fileName = builtin.getString(HCUSTOM_PROPERTIES);
			log.debug("Looking for custom config in {}",builtin.getString(HCUSTOM_PROPERTIES));
			if (new File(fileName).exists()) {
				log.info("Loading custom config from home ({})",builtin.getString(HCUSTOM_PROPERTIES));
				pconfig = new PropertiesConfiguration(builtin.getString(HCUSTOM_PROPERTIES));
				FileChangedReloadingStrategy strategy = new FileChangedReloadingStrategy();
				strategy.setRefreshDelay(30000);
				pconfig.setReloadingStrategy(strategy);
			}
			else {
				log.info("No custom over-ride found in {}",fileName);
				pconfig = new PropertiesConfiguration();
			}

			addConfiguration(pconfig);
		} 
		catch (Throwable e) {
			log.error("Problem loading custom configuration {}",e);
		}		
	}

	/**
	 * Setup system.
	 */
	private void setupSystem() {
		try {
			log.debug("Loading system config");
			addConfiguration(new SystemConfiguration());
		}
		catch (Throwable t) {
			log.error("Problem loading system configuration {}",t.getMessage());			
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

	public void setStringList(String key, List<String> list) {
		StringBuffer s = new StringBuffer();
		for (String entry : list) {
			s.append(entry).append(super.getListDelimiter());
		}
		if (s.length()>0) {
			setProperty(key, s.substring(0, s.length()-1));
		}
		else {
			setProperty(key,"");
		}
	}

	//	public String getFlatList(String key) {
	//		StringBuffer list = new StringBuffer();
	//		for (String s : getStringList(key)) {
	//			list.append(s).append(',');
	//		}
	//		if (list.length()>0)
	//			return list.substring(0, list.length()-1).toString();
	//		return "";
	//	}

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
		super.setProperty(key, value);
	}

	public static List<String> toList(String props) {
		List<String> res = new ArrayList<String>();
		StringTokenizer tok = new StringTokenizer(props,",");
		while (tok.hasMoreTokens()) {
			res.add(tok.nextToken().trim());
		}
		return res;
	}

	public static String toString(List<String> list) {
		StringBuffer sb = new StringBuffer();
		for (String s : list) {
			sb.append(s).append(',');
		}
		if (list.size()>0)
			return sb.substring(0, sb.length()-1);
		else
			return sb.toString();
	}
}
