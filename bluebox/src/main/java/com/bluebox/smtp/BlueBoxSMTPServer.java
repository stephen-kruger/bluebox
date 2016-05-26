package com.bluebox.smtp;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.subethamail.smtp.AuthenticationHandlerFactory;
import org.subethamail.smtp.server.SMTPServer;

import com.bluebox.Config;
import com.bluebox.Utils;

public class BlueBoxSMTPServer extends SMTPServer {
	private static final Logger log = LoggerFactory.getLogger(BlueBoxSMTPServer.class);
//	private SSLContext sslContext;

	public BlueBoxSMTPServer(BlueboxMessageHandlerFactory mhf) {
		super(mhf);
		// set up TLS
//		try {
//			createSSLContext();
//		} 
//		catch (Throwable e) {
//			e.printStackTrace();
//		}
		
		Config bbconfig = Config.getInstance();
		setHostName(Utils.getHostName());
		setPort(bbconfig.getInt(Config.BLUEBOX_PORT));
		log.info("Starting SMTP server on {} and port {}",Utils.getHostName(),bbconfig.getInt(Config.BLUEBOX_PORT));
		setMaxConnections(bbconfig.getInt(Config.BLUEBOX_MAXCONNECTIONS));
		setHideTLS(true);
		setRequireTLS(false);
		setEnableTLS(false);
		setSoftwareName("BlueBox V"+bbconfig.getString(Config.BLUEBOX_VERSION));
		setConnectionTimeout(30000); // wait 10sec before abandoning connection
		
	}

	public BlueBoxSMTPServer(BlueboxMessageHandlerFactory mhf, AuthenticationHandlerFactory ahf) {
		super(mhf, ahf);
	}

	@Override
	public synchronized void stop() {
		super.stop();
	}

//	@Override
//	public SSLSocket createSSLSocket(Socket socket) throws IOException {
//		InetSocketAddress remoteAddress = (InetSocketAddress) socket.getRemoteSocketAddress();
//
//		SSLSocketFactory sf = sslContext.getSocketFactory();
//		SSLSocket s = (SSLSocket) (sf.createSocket(socket, remoteAddress.getHostName(), socket.getPort(), true));
//
//		// we are a server
//		s.setUseClientMode(false);
//
//		// select strong protocols and cipher suites
//		s.setEnabledProtocols(StrongTls.intersection(s.getSupportedProtocols(), StrongTls.ENABLED_PROTOCOLS));
//		s.setEnabledCipherSuites(StrongTls.intersection(s.getSupportedCipherSuites(), StrongTls.ENABLED_CIPHER_SUITES));
//
//		//// Client must authenticate
//		// s.setNeedClientAuth(true);
//
//		return s;
//	}

//	public void createSSLContext() throws KeyManagementException, KeyStoreException, NoSuchAlgorithmException, UnrecoverableKeyException, CertificateException, IOException, InvalidKeyException, NoSuchProviderException, IllegalStateException, SignatureException {
//		// begin: create a default keystore
//		//		KeyStore ks = KeyStore.getInstance("PKCS12");
//		//		char[] password = "blueboxtls".toCharArray();
//		//		ks.load(null, password);
//		//		X509Certificate cert = generateSelfSignedX509Certificate();
//		//		ks.setCertificateEntry("bluebox", cert);
//
//		// Store away the keystore.
//		//		File keystoreFile = File.createTempFile("bluebox", "keystore");
//		//keytool -genkeypair -alias bluebox_certificate -keystore bluebox_keystore.pfx -storepass bluebox -validity 760 -keyalg RSA -keysize 2048 -storetype pkcs12
//		File keystoreFile = new File("C:/workspace/eclipse/bluebox/src/main/webapp/data/bluebox_keystore.pfx");
//		//		FileOutputStream fos = new FileOutputStream(keystoreFile);
//		//		ks.store(fos, password);
//		//		fos.close();
//		// end: create a default keystore
//
//		// Key store for your own private key and signing certificates.
//		InputStream keyStoreIS = new FileInputStream(keystoreFile);
//		char[] keyStorePassphrase ="bluebox".toCharArray();
//		KeyStore ksKeys = KeyStore.getInstance("PKCS12");
//		ksKeys.load(keyStoreIS, keyStorePassphrase);
//
//		// KeyManager decides which key material to use.
//		KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
//		kmf.init(ksKeys, keyStorePassphrase);
//
//		// Trust store contains certificates of trusted certificate authorities.
//		// We'll need this to do client authentication.
//		//		InputStream trustStoreIS = new FileInputStream("C:/workspace/eclipse/bluebox/src/main/webapp/data/bluebox_keystore.pfx");
//		//		char[] trustStorePassphrase = "bluebox".toCharArray();
//		//		KeyStore ksTrust = KeyStore.getInstance("JKS");
//		//		ksTrust.load(trustStoreIS, trustStorePassphrase);
//
//		// TrustManager decides which certificate authorities to use.
//		//		TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
//		//		tmf.init(ksTrust);
//		TrustManagerFactory tmf = 
//				TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
//		KeyStore trustStore = KeyStore.getInstance("JKS");
//		tmf.init(trustStore);
//
//		sslContext = SSLContext.getInstance("TLS");
//		sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
//	}



}
