package com.bluebox.rest;

import com.bluebox.TestUtils;
import com.bluebox.servlet.BaseServletTest;
import com.bluebox.smtp.Inbox;
import com.bluebox.smtp.storage.BlueboxMessage;
import com.bluebox.smtp.storage.LiteMessage;
import com.bluebox.smtp.storage.StorageFactory;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class TestMessageResource extends BaseServletTest {
    private static final Logger log = LoggerFactory.getLogger(TestMessageResource.class);


    @Override
    protected void setUp() throws Exception {
        super.setUp();
        TestUtils.addRandomDirect(StorageFactory.getInstance(), COUNT);
    }


    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }


    public void testGetLinks() throws Exception {
        List<LiteMessage> list = Inbox.getInstance().listInboxLite(null, BlueboxMessage.State.ANY, 0, 5, BlueboxMessage.RECEIVED, true, Locale.getDefault());
        assertEquals("Missing mail", COUNT, list.size());
        for (LiteMessage jo : list) {
            String url = MessageResource.PATH + "/" + MessageResource.LINKS + "/" + jo.getIdentifier();
            JSONObject js = getRestJSON(url);
            JSONArray ja = js.getJSONArray(MessageResource.LINKS);
            for (int i = 0; i < ja.length(); i++) {
                assertNotNull("Link text not found", ja.getJSONObject(i).getString("text"));
                assertNotNull("Link href not found", ja.getJSONObject(i).getString("href"));
            }
            log.info(js.toString(3));
        }
    }

    @Test
    public void testTextLinks() {
        JSONArray res = MessageResource.getTextLinks("This is some test https://test.com and ftp://www.there.com and http://steve.com for unit test purposes");
        log.info("Found {} links {}", res.length(), res);
        assertEquals("Links not detected", 3, res.length());
        res = MessageResource.getTextLinks(null);
        assertEquals("Links detected", 0, res.length());
        res = MessageResource.getTextLinks("");
        assertEquals("Links detected", 0, res.length());
    }

}
