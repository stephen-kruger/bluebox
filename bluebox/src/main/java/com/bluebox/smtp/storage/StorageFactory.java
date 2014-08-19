package com.bluebox.smtp.storage;



import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bluebox.Config;

public class StorageFactory {
	private static final Logger log = LoggerFactory.getLogger(StorageFactory.class);
	private static StorageIf storageInstance;

	public static StorageIf getInstance() {
		if (storageInstance==null) {
			//storageInstance = new StorageImpl2();
			Config config = Config.getInstance();
			String storageClassName = config.getString(Config.BLUEBOX_STORAGE);
			log.info("Allocating storage instance for class "+storageClassName);
			try {
				storageInstance = (StorageIf) Class.forName(storageClassName).newInstance();
			} 
			catch (Throwable e) {
				log.error(e.getMessage());
				e.printStackTrace();
			}
		}

		return storageInstance;
	}
	
	public static void clearInstance() {
		storageInstance=null;
	}

}
