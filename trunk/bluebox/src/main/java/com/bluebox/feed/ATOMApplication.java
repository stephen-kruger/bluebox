package com.bluebox.feed;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;


public class ATOMApplication extends Application {
	public Set<Class<?>> getClasses() {
        Set<Class<?>> s = new HashSet<Class<?>>();
        s.add(MailATOM.class);
        return s;
    }
}
