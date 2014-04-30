package com.bluebox.smtp.storage;

import java.util.logging.Logger;

import com.bluebox.Config;

public class StorageFactory {
	private static final Logger log = Logger.getAnonymousLogger();
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
				e.printStackTrace();
			}
		}

		return storageInstance;
	}
	
	public static void clearInstance() {
		storageInstance=null;
	}

//	public static void stop() {
//		log.info("Stopping storage factory instance");
//		if (storageInstance!=null) {
//			try {
//				storageInstance.stop();
//			} 
//			catch (Exception e) {
//				e.printStackTrace();
//			}
//		}
//		storageInstance = null;
//	}
}
