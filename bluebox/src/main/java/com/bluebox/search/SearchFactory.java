package com.bluebox.search;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SearchFactory {
	private static SearchIf searcher;
	private static final Logger log = LoggerFactory.getLogger(SearchIndexer.class);

	public static SearchIf getInstance() throws IOException {
		if (searcher==null) {
			log.debug("Instanciating search indexer");

			searcher = new SearchIndexer();
		}
		return searcher;
	}

	protected static void stopInstance() {
		log.debug("Nullifying search indexer");
		searcher = null;	
	}
}
