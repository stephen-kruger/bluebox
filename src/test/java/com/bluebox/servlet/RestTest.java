package com.bluebox.servlet;

import com.bluebox.TestUtils;
import com.bluebox.Utils;
import com.bluebox.rest.AutoCompleteResource;
import com.bluebox.rest.InlineResource;
import com.bluebox.rest.MessageResource;
import com.bluebox.smtp.Inbox;
import com.bluebox.smtp.storage.BlueboxMessage;
import com.bluebox.smtp.storage.BlueboxMessage.State;
//import org.apache.wink.client.ClientResponse;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MediaType;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class RestTest extends BaseServletTest {
    private static final Logger log = LoggerFactory.getLogger(RestTest.class);

    @Test
    public void testAutocomplete() throws Exception {
//        setUp();
//        String url = "/" + AutoCompleteResource.PATH + "/list?start=0&count=10&name=*";
//        JSONObject js = getRestJSON(url);
//        JSONArray ja = js.getJSONArray("items");
//        for (int i = 0; i < ja.length(); i++) {
//            assertNotSame("Full name not found", ja.getJSONObject(i).getString("name"), ja.getJSONObject(i).getString("label"));
//        }
//        log.info(js.toString(3));
    }

    @Test
    public void testInbox() throws Exception {
        //		// send some test messages
        //		for (int i = 0; i < COUNT; i++)
        //			TestUtils.sendMailSMTP(Utils.getRandomAddress(), Utils.getRandomAddress(), null, null, "subject", "body");
        //
        //		// first we check directly
        //		TestUtils.waitFor(Inbox.getInstance(),COUNT);
        //
        //		assertEquals("Missing mails",COUNT,getMailCount(BlueboxMessage.State.NORMAL));
        //
        //		// now hit the REST web service
        //		String inboxURL = "/"+JSONFolderHandler.JSON_ROOT;
        //		log.info("Checking URL:"+inboxURL);
        //		JSONObject js = getRestJSON(inboxURL);
        //
        //		JSONObject child = js.getJSONObject(BlueboxMessage.State.ANY.name());
        //		assertEquals("Missing mails",COUNT,child.getInt("count"));
        //		assertTrue(child.has("id"));
        //		assertTrue(child.has("name"));
        //		assertTrue(child.has("style"));
        //		assertTrue(child.has("count"));
        //		assertTrue(child.has("state"));
        //		assertTrue(child.has("email"));
        //
        //		child = js.getJSONObject(BlueboxMessage.State.NORMAL.name());
        //		assertEquals("Missing mails",COUNT,child.getInt("count"));
        //		assertTrue(child.has("id"));
        //		assertTrue(child.has("name"));
        //		assertTrue(child.has("style"));
        //		assertTrue(child.has("count"));
        //		assertTrue(child.has("state"));
        //		assertTrue(child.has("email"));
        //
        //		child = js.getJSONObject(BlueboxMessage.State.DELETED.name());
        //		assertEquals("Missing mails",0,child.getInt("count"));
        //		assertTrue(child.has("id"));
        //		assertTrue(child.has("name"));
        //		assertTrue(child.has("style"));
        //		assertTrue(child.has("count"));
        //		assertTrue(child.has("state"));
        //		assertTrue(child.has("email"));
    }

//    @Test
//    public void testInlineHandler() throws Exception {
//        // This causes a nasty exception trace, but is limited to the Jetty server used for unit tests.
//        clearMail();
//        InputStream emlStream = new FileInputStream("src/test/resources" + File.separator + "test-data" + File.separator + "attachments.eml");
//        Utils.uploadEML(Inbox.getInstance(), emlStream);
//        TestUtils.waitFor(Inbox.getInstance(), 1);
//        assertEquals("Mail was not delivered", 1, Inbox.getInstance().getMailCount(State.ANY));
//
//        List<BlueboxMessage> messages = Inbox.getInstance().listInbox(null, BlueboxMessage.State.ANY, 0, 5, BlueboxMessage.RECEIVED, true);
//        BlueboxMessage msg = messages.get(0);
//
//        ClientResponse response = getJaxResponse(
//                InlineResource.PATH + "/get/" + msg.getIdentifier() + "/DSC_3968.JPG",
//                MediaType.APPLICATION_FORM_URLENCODED,
//                "image/jpeg");
//        response.consumeContent();
//        assertEquals(200, response.getStatusCode());
//
////		// now retrieve the attachment by uid
//        response = getJaxResponse(
//                InlineResource.PATH + "/get/" + msg.getIdentifier() + "/ii_hxqkskb21_147462ce25a92ebf",
//                MediaType.APPLICATION_FORM_URLENCODED,
//                MediaType.MEDIA_TYPE_WILDCARD);
//        response.consumeContent();
//        assertEquals(200, response.getStatusCode());
//    }

//    @Test
//    public void testMessageResource() throws Exception {
//        List<BlueboxMessage> messages = Inbox.getInstance().listInbox(null, BlueboxMessage.State.ANY, 0, 5, BlueboxMessage.RECEIVED, true);
//        for (BlueboxMessage message : messages) {
//            String url = MessageResource.PATH + "/detail/" + message.getIdentifier();
//            JSONObject js = getRestJSON(url);
//
//            log.info(js.toString(3));
//        }
//    }

}
