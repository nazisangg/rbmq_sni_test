import com.rabbitmq.client.impl.nio.NioHelper;
import com.rabbitmq.client.impl.nio.NioParams;
import java.io.*;
import java.net.URL;
import java.nio.channels.SocketChannel;
import java.security.*;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.net.SocketFactory;
import sun.security.ssl.*;
import javax.net.ssl.*;

import com.rabbitmq.client.*;
import org.apache.http.ssl.SSLContexts;


public class main {


  private static final String CLIENT_CERTIFICATE = "oclient_without_ca.p12";

  /**
   * Client password from certificate. This INFORMATION should be stored safely!!!!
   */
  private static final String CLIENT_PASSWORD_CERTIFICATE = "secret";

  /**
   * Given file format from client certificates.
   */
  private static final String KEYSTORE_CLIENT = "PKCS12";

  /**
   * Server certificate as java keystore.
   */
  private static final String SERVER_CERTIFICATE = "client-truststore_cer.jks";

  /**
   * Password from java keystore. This INFORMATION should be stored safely!!!!
   */
  private static final String SERVER_CERTIFICATE_PASSWORD = "secret";

  /**
   * Java keystore type.
   */
  private static final String SERVER_CERTIFICATE_TYPE = "JKS";

  /**
   * TLS version which should be used.
   */
  private static final String TLS_TYPE = "TLSv1.2";

  /**
   * Rabbitmq server ip.
   */
  private static final String RABBIT_MQ_HOST = "rabbitmq-sni-cdci-dev.f172.telstra-ice-osd.openshiftapps.com";

  /**
   * Rabbitmq port to listen.
   */
  private static final int RABBIT_MQ_PORT = 443;

  /**
   * Rabbitmq user to login.
   */
  private static final String RABBIT_MQ_USER = "admin";

  /**
   * Password from rabbitmq user. This INFORMATION should be stored safely!!!!
   */
  private static final String RABBIT_MQ_PASSWORD = "admin123!";

  /**
   * Rabbitmq example channel to send and receive a message.
   */
  private static final String RABBIT_MQ_CHANNEL = "admin";

  public static void main(String[] args) throws Exception
  {
    URL serverCertificate = ClassLoader.getSystemClassLoader().getResource(SERVER_CERTIFICATE);
    URL clientCertificate = ClassLoader.getSystemClassLoader().getResource(CLIENT_CERTIFICATE);

    char[] keyPassphrase = CLIENT_PASSWORD_CERTIFICATE.toCharArray();
    KeyStore ks = KeyStore.getInstance(KEYSTORE_CLIENT);
    assert clientCertificate != null;
    //ks.load(new FileInputStream(clientCertificate.getFile()), keyPassphrase);
    ks.load(new FileInputStream(clientCertificate.getFile()), keyPassphrase);

    KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
    kmf.init(ks, keyPassphrase);

    char[] trustPassphrase = SERVER_CERTIFICATE_PASSWORD.toCharArray();
    KeyStore tks = KeyStore.getInstance(SERVER_CERTIFICATE_TYPE);
    assert serverCertificate != null;
    tks.load(new FileInputStream(serverCertificate.getFile()), trustPassphrase);

    TrustManagerFactory tmf = TrustManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
    tmf.init(tks);

    // 应该是要把host 放到 ssl paramters 里面

    SSLContext c = SSLContext.getInstance(TLS_TYPE);
    SSLContext c2 = SSLContexts.custom().loadKeyMaterial(ks,keyPassphrase).build();

    c.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

    List sniHostNames = new ArrayList();
    URL url = new URL("https://rabbitmq-sni-cdci-dev.f172.telstra-ice-osd.openshiftapps.com/");
    sniHostNames.add(new SNIHostName(url.getHost()));
    SSLParameters sslParameters = c.getDefaultSSLParameters();
    System.out.println(Arrays.asList(sslParameters.getProtocols()));
    sslParameters.setServerNames(sniHostNames);


    ConnectionFactory factory = new ConnectionFactory();
//    factory.setHost("localhost");
//    factory.setPort(56711);
    factory.setHost(RABBIT_MQ_HOST);
    factory.setPort(RABBIT_MQ_PORT);
    factory.setUsername(RABBIT_MQ_USER);
    factory.setPassword(RABBIT_MQ_PASSWORD);
    factory.useSslProtocol(c);
    SSLSocketFactory sslSocketFactory = new SSLSocketFactoryWrapper(c.getSocketFactory(),sslParameters);

    factory.setSocketFactory(sslSocketFactory);


    Connection conn = factory.newConnection();
    Channel channel = conn.createChannel();

    //non-durable, exclusive, auto-delete queue
    channel.queueDeclare(RABBIT_MQ_CHANNEL, false, true, true, null);
    channel.basicPublish("", RABBIT_MQ_CHANNEL, null, "Hello SSL World :-)".getBytes());

    GetResponse chResponse = channel.basicGet(RABBIT_MQ_CHANNEL, false);
    if(chResponse == null) {
      System.out.println("No message retrieved");
    } else {
      byte[] body = chResponse.getBody();
      System.out.println("Received: " + new String(body));
    }

    channel.close();
    conn.close();
  }



//  synchronized public void setHost(String host) {
//    this.host = host;
//    this.serverNames =
//        Utilities.addToSNIServerNameList(this.serverNames, this.host);
//  }



}
