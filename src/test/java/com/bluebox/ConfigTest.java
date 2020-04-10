package com.bluebox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import junit.framework.TestCase;

public class ConfigTest extends TestCase {
	private static final Logger log = LoggerFactory.getLogger(TestUtils.class);

	public void testConfig() {
		Config config = Config.getInstance();
		assertNotNull(config.getString(Config.BLUEBOX_VERSION));
	}
	
	public void testLists() {
		Config config = Config.getInstance();
		log.info(Config.toString(config.getStringList(Config.BLUEBOX_SMTPBLACKLIST)));
	}
	
}