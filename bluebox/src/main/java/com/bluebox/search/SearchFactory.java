package com.bluebox.search;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SearchFactory {
	private static SearchIf searcher;
	private static final Logger log = LoggerFactory.getLogger(LuceneIndexer.class);

	public static SearchIf getInstance() throws Exception {
		if (searcher==null) {
			log.debug("Instanciating search indexer");
			searcher = new LuceneIndexer();
//			searcher = new StorageIndexer();
		}
		return searcher;
	}

	protected static void stopInstance() {
		log.debug("Nullifying search indexer");
		searcher = null;	
	}
}
