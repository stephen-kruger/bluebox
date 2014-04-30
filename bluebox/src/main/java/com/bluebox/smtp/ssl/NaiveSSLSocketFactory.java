package com.bluebox.smtp.ssl;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.logging.Logger;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;

public class NaiveSSLSocketFactory extends SSLSocketFactory {
	private static final Logger log = Logger.getAnonymousLogger();
	private static SSLSocketFactory sslSocketFactory;

	/**
	 * Returns a SSL Factory instance that accepts all server certificates.
	 * <pre>SSLSocket sock =
	 *     (SSLSocket) getSocketFactory.createSocket ( host, 443 ); </pre>
	 * @return  An SSL-specific socket factory. 
	 **/
	public static final SSLSocketFactory getSocketFactory() {
	  if ( sslSocketFactory == null ) {
	    try {
	      TrustManager[] tm = new TrustManager[] { 
	    		  new NaiveTrustManager() 
	    		  };
	      SSLContext context = SSLContext.getInstance ("SSL");
	      context.init( new KeyManager[0], tm, new SecureRandom( ) );

	      sslSocketFactory = (SSLSocketFactory) context.getSocketFactory();

	    } 
	    catch (KeyManagementException e) {
	      log.severe("No SSL algorithm support: " + e.getMessage()); 
	    } 
	    catch (NoSuchAlgorithmException e) {
	      log.severe ("Exception when setting up the Naive key management.");
	    }
	  }
	  return new NaiveSSLSocketFactory();
	}

	public SSLSocket createSocket(Socket socket) throws IOException {
		InetSocketAddress remoteAddress = (InetSocketAddress) socket.getRemoteSocketAddress();
        SSLSocket s = (SSLSocket) (createSocket(socket, remoteAddress.getHostName(), socket.getPort(), true));

        // we are a server
        s.setUseClientMode(false);

        // allow all supported cipher suites
        s.setEnabledCipherSuites(s.getSupportedCipherSuites());
        return s;
	}

	@Override
	public String[] getDefaultCipherSuites() {
		return sslSocketFactory.getDefaultCipherSuites();
	}

	@Override
	public String[] getSupportedCipherSuites() {
		return sslSocketFactory.getSupportedCipherSuites();
	}

	@Override
	public Socket createSocket(String arg0, int arg1) throws IOException,
			UnknownHostException {
		log.info("E>>>>>>>>>>>>>>>");
		return sslSocketFactory.createSocket(arg0, arg1); 
	}

	@Override
	public Socket createSocket(InetAddress arg0, int arg1) throws IOException {
		log.info("D>>>>>>>>>>>>>>>");
		return sslSocketFactory.createSocket(arg0, arg1); 
	}

	@Override
	public Socket createSocket(String arg0, int arg1, InetAddress arg2, int arg3)
			throws IOException, UnknownHostException {
		log.info("C>>>>>>>>>>>>>>>");
		return sslSocketFactory.createSocket(arg0, arg1, arg2, arg3); 
	}

	@Override
	public Socket createSocket(InetAddress arg0, int arg1, InetAddress arg2, int arg3) throws IOException {
		log.info("B>>>>>>>>>>>>>>>");
		return sslSocketFactory.createSocket(arg0, arg1, arg2, arg3); 
	}

	@Override
	public Socket createSocket(Socket s, String host, int port, boolean autoClose) throws IOException {
		log.info("A>>>>>>>>>>>>>>>");
		return sslSocketFactory.createSocket(s, host, port, autoClose);
	}

}
