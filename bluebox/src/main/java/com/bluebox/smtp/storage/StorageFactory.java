package com.bluebox.smtp.storage;



import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bluebox.Config;

public class StorageFactory {
	private static final Logger log = LoggerFactory.getLogger(StorageFactory.class);
	private static StorageIf storageInstance;

	public static StorageIf getInstance() {
		if (storageInstance==null) {
			Config config = Config.getInstance();
			if (config.containsKey(Config.BLUEBOX_STORAGE)) {
				log.info("Using config specified storage :{}",config.getString(Config.BLUEBOX_STORAGE));
				// use whatever config specifies
				String storageClassName = config.getString(Config.BLUEBOX_STORAGE);
				log.info("Allocating storage instance for class {}",storageClassName);
				try {
					storageInstance = (StorageIf) Class.forName(storageClassName).newInstance();
				} 
				catch (Throwable e) {
					log.error(e.getMessage());
					e.printStackTrace();
				}
			}
			else {
				// try mongodb, if it fails use derby
				if (com.bluebox.smtp.storage.mongodb.StorageImpl.mongoDetected()) {
					storageInstance = new com.bluebox.smtp.storage.mongodb.StorageImpl();
//					storageInstance = new com.bluebox.smtp.storage.mongodb.MongoImpl();
				} 
				else {
					storageInstance = new com.bluebox.smtp.storage.derby.StorageImpl();
				}
			}
			try {
				log.debug("Starting Storage");
				storageInstance.start();
			} 
			catch (Exception e) {
				log.error("Problem starting storage instance",e);
			}
		}

		return storageInstance;
	}

	public static void clearInstance() {
		storageInstance=null;
	}

}
