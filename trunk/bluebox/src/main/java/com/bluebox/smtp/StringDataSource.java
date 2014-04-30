package com.bluebox.smtp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.activation.DataSource;

import com.bluebox.Utils;

public class StringDataSource implements DataSource {
	private String content;

	public StringDataSource(String content) {
		this.content = content;		
	}

	@Override
	public String getContentType() {
		return "text/plain";
	}

	@Override
	public InputStream getInputStream() throws IOException {
		return Utils.convertStringToStream(content);
	}

	@Override
	public String getName() {
		return "blueboxdatasource";
	}

	@Override
	public OutputStream getOutputStream() throws IOException {
		return null;
	}

}
