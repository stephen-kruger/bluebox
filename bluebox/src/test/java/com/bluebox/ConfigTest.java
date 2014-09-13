package com.bluebox;

import com.bluebox.Config;

import junit.framework.TestCase;

public class ConfigTest extends TestCase {
	
	public void testConfig() {
		Config config = Config.getInstance();
		assertNotNull(config.getString(Config.BLUEBOX_VERSION));
	}
	
}