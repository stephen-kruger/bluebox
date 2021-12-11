package com.bluebox.smtp.storage;


import com.bluebox.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StorageFactory {
    private static final Logger log = LoggerFactory.getLogger(StorageFactory.class);
    private static StorageIf storageInstance;

    public static StorageIf getInstance() {
        if (storageInstance == null) {
            Config config = Config.getInstance();
            if (config.containsKey(Config.BLUEBOX_STORAGE)) {
                log.info("Using config specified storage :{}", config.getString(Config.BLUEBOX_STORAGE));
                // use whatever config specifies
                String storageClassName = config.getString(Config.BLUEBOX_STORAGE);
                log.info("Allocating storage instance for class {}", storageClassName);
                try {
                    storageInstance = (StorageIf) Class.forName(storageClassName).getDeclaredConstructor().newInstance();
                } catch (Throwable e) {
                    log.error(e.getMessage());
                    e.printStackTrace();
                }
            } else {
                // try mongodb, if it fails use H2
                if (com.bluebox.smtp.storage.AbstractStorage.mongoDetected()) {
                    log.info("Using MongoImpl storage driver");
                    storageInstance = new com.bluebox.smtp.storage.mongodb.MongoImpl();
                } else {
                    storageInstance = new com.bluebox.smtp.storage.h2.H2Impl();
                }
            }
            try {
                log.debug("Starting Storage");
                storageInstance.start();
            } catch (Exception e) {
                log.error("Problem starting storage instance", e);
            }
        }

        return storageInstance;
    }

    public static void clearInstance() {
        log.info("Clearing storage instance");
        storageInstance = null;
    }

}
