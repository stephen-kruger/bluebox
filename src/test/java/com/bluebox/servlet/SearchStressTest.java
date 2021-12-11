package com.bluebox.servlet;

import com.bluebox.TestUtils;
import com.bluebox.rest.AutoCompleteResource;
import com.bluebox.search.SearchFactory;
import com.bluebox.smtp.Inbox;
import org.codehaus.jettison.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class SearchStressTest extends BaseServletTest {
    private static final Logger log = LoggerFactory.getLogger(SearchStressTest.class);
    private final int STRESS_LEVEL = 500;

    public void setUp() throws Exception {
        super.setUp();
        TestUtils.addRandomNoThread(Inbox.getInstance(), COUNT);
        TestUtils.waitFor(Inbox.getInstance(), COUNT);
        SearchFactory.getInstance().commit(true);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testStressSearchHandler() throws Exception {
        String url = "/" + AutoCompleteResource.PATH + "/list?" + "label=&start=0&count=" + COUNT;
        log.info("Stressing to " + STRESS_LEVEL);
        for (int i = 0; i < STRESS_LEVEL; i++) {
            JSONObject js = getRestJSON(url);
            assertEquals("Autocomplete did not find expected items", COUNT, js.getJSONArray("items").length());
        }
        log.info("Done search stressing to " + STRESS_LEVEL);
    }

}
