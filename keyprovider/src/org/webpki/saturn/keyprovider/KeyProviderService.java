/*
 *  Copyright 2006-2016 WebPKI.org (http://webpki.org).
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.webpki.saturn.keyprovider;

import java.io.IOException;
import java.io.InputStream;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URL;

import java.security.PublicKey;

import java.security.cert.X509Certificate;

import java.security.interfaces.RSAPublicKey;

import java.util.Enumeration;
import java.util.Vector;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.webpki.crypto.CertificateUtil;
import org.webpki.crypto.CustomCryptoProvider;

import org.webpki.json.JSONDecoderCache;

import org.webpki.json.encryption.DataEncryptionAlgorithms;
import org.webpki.json.encryption.KeyEncryptionAlgorithms;

import org.webpki.keygen2.CredentialDiscoveryResponseDecoder;
import org.webpki.keygen2.InvocationResponseDecoder;
import org.webpki.keygen2.KeyCreationResponseDecoder;
import org.webpki.keygen2.ProvisioningFinalizationResponseDecoder;
import org.webpki.keygen2.ProvisioningInitializationResponseDecoder;

import org.webpki.net.HTTPSWrapper;

import org.webpki.util.ArrayUtil;
import org.webpki.util.MIMETypedObject;

import org.webpki.saturn.common.KeyStoreEnumerator;
import org.webpki.saturn.common.PaymentMethods;

import org.webpki.webutil.InitPropertyReader;

public class KeyProviderService extends InitPropertyReader implements ServletContextListener {

    static Logger logger = Logger.getLogger(KeyProviderService.class.getCanonicalName());
    
    static final String WEBPKI_LOGO           = "webpki_logotype";

    static final String SATURN_LOGO           = "saturn_logotype";

    static final String VERSION_CHECK         = "android_webpki_versions";

    static final String KEYSTORE_PASSWORD     = "key_password";

    static final String BANK_HOST             = "bank_host";
    
    static final String KEYPROV_BASE_URL      = "keyprov_base_url";

    static final String KEYPROV_KMK           = "keyprov_kmk";
    
    static final String SERVER_PORT_MAP       = "server_port_map";
    
    static final String[] CREDENTIALS         = {"paycred1", "paycred2", "paycred3"};
    
    static final String BOUNCYCASTLE_FIRST    = "bouncycastle_first";

    static KeyStoreEnumerator keyManagemenentKey;
    
    static String keygen2EnrollmentUrl;
    
    static String successImageAndMessage;
    
    static Integer serverPortMapping;

    static JSONDecoderCache keygen2JSONCache;
    
    static X509Certificate tlsCertificate;

    static String[] grantedVersions;

    static class PaymentCredential {
        KeyStoreEnumerator signatureKey;
        String paymentMethod;
        String accountId;
        boolean cardFormatted;
        byte[] optionalServerPin;
        String authorityUrl;
        MIMETypedObject cardImage;
        PublicKey encryptionKey;
        DataEncryptionAlgorithms dataEncryptionAlgorithm;
        KeyEncryptionAlgorithms keyEncryptionAlgorithm;
    }

    static Vector<PaymentCredential> paymentCredentials = new Vector<PaymentCredential>();

    public static String webpkiLogotype;

    InputStream getResource(String name) throws IOException {
        InputStream is = this.getClass().getResourceAsStream(name);
        if (is == null) {
            throw new IOException("Resource fail for: " + name);
        }
        return is;
    }

    String getResourceAsString(String propertyName) throws IOException {
        return new String(ArrayUtil.getByteArrayFromInputStream(getResource(getPropertyString(propertyName))), "UTF-8");
        
    }

    String getURL (String inUrl) throws IOException {
        URL url = new URL(inUrl);
        if (!url.getHost().equals("localhost")) {
            return inUrl;
        }
        String autoHost = null;
        Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
        int foundAddresses = 0;
        while (networkInterfaces.hasMoreElements()) {
            NetworkInterface networkInterface = networkInterfaces.nextElement();
            if (networkInterface.isUp() && !networkInterface.isVirtual() && !networkInterface.isLoopback() &&
                networkInterface.getDisplayName().indexOf("VMware") < 0) {  // Well.... 
                Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();
                while (inetAddresses.hasMoreElements()) {
                    InetAddress inetAddress = inetAddresses.nextElement();
                    if (inetAddress instanceof Inet4Address) {
                        foundAddresses++;
                        autoHost = inetAddress.getHostAddress();
                    }
                }
            }
        }
        if (foundAddresses != 1) throw new IOException("Couldn't determine network interface");
        logger.info("Host automagically set to: " + autoHost);
        return new URL(url.getProtocol(),
                       autoHost,
                       url.getPort(),
                       url.getFile()).toExternalForm();
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        initProperties (sce);
        try {
            CustomCryptoProvider.forcedLoad(getPropertyBoolean(BOUNCYCASTLE_FIRST));

            ////////////////////////////////////////////////////////////////////////////////////////////
            // KeyGen2
            ////////////////////////////////////////////////////////////////////////////////////////////
            keygen2JSONCache = new JSONDecoderCache ();
            keygen2JSONCache.addToCache (InvocationResponseDecoder.class);
            keygen2JSONCache.addToCache (ProvisioningInitializationResponseDecoder.class);
            keygen2JSONCache.addToCache (CredentialDiscoveryResponseDecoder.class);
            keygen2JSONCache.addToCache (KeyCreationResponseDecoder.class);
            keygen2JSONCache.addToCache (ProvisioningFinalizationResponseDecoder.class);

            ////////////////////////////////////////////////////////////////////////////////////////////
            // Credentials
            ////////////////////////////////////////////////////////////////////////////////////////////
            String bankHost = getPropertyString(BANK_HOST);
            for (String credentialEntry : CREDENTIALS) {
                final String[] arguments = getPropertyStringList(credentialEntry);
                PaymentCredential paymentCredential = new PaymentCredential();
                paymentCredentials.add(paymentCredential);
                paymentCredential.authorityUrl = bankHost + "/" + arguments[5] + "/authority";
                paymentCredential.optionalServerPin = arguments[6].equals("@") ? null : arguments[6].getBytes("utf-8");
                paymentCredential.signatureKey =
                    new KeyStoreEnumerator(getResource(arguments[0]),
                                           getPropertyString(KEYSTORE_PASSWORD));
                paymentCredential.paymentMethod = PaymentMethods.valueOf(arguments[1]).getPaymentMethodUri();
                boolean cardFormatted = true;
                if (arguments[2].charAt(0) == '!') {
                    cardFormatted = false;
                    arguments[2] = arguments[2].substring(1);
                }
                paymentCredential.accountId = arguments[2];
                paymentCredential.cardFormatted = cardFormatted;
                paymentCredential.cardImage = new MIMETypedObject() {
                    @Override
                    public byte[] getData() throws IOException {
                        return ArrayUtil.getByteArrayFromInputStream(getResource(arguments[3]));
                    }
                    @Override
                    public String getMimeType() throws IOException {
                        return "image/svg+xml";
                    }
                };
                paymentCredential.encryptionKey =
                    CertificateUtil.getCertificateFromBlob(
                        ArrayUtil.getByteArrayFromInputStream(getResource(arguments[4]))).getPublicKey();
                paymentCredential.keyEncryptionAlgorithm = 
                        paymentCredential.encryptionKey instanceof RSAPublicKey ?
                        KeyEncryptionAlgorithms.JOSE_RSA_OAEP_256_ALG_ID 
                                                                                : 
                        KeyEncryptionAlgorithms.JOSE_ECDH_ES_ALG_ID;
                paymentCredential.dataEncryptionAlgorithm = 
                        DataEncryptionAlgorithms.getAlgorithmFromId(arguments[7]);
            }


            ////////////////////////////////////////////////////////////////////////////////////////////
            // SKS key management key
            ////////////////////////////////////////////////////////////////////////////////////////////
            keyManagemenentKey = new KeyStoreEnumerator(getResource(getPropertyString(KEYPROV_KMK)),
                                                                    getPropertyString(KEYSTORE_PASSWORD));

            if (getPropertyString(SERVER_PORT_MAP).length () > 0) {
                serverPortMapping = getPropertyInt(SERVER_PORT_MAP);
            }

            ////////////////////////////////////////////////////////////////////////////////////////////
            // Get KeyGen2 protocol entry
            ////////////////////////////////////////////////////////////////////////////////////////////
            keygen2EnrollmentUrl = getURL(getPropertyString(KEYPROV_BASE_URL)) + "/getkeys";

            ////////////////////////////////////////////////////////////////////////////////////////////
            // WebPKI.org logotype
            ////////////////////////////////////////////////////////////////////////////////////////////
            webpkiLogotype = getResourceAsString(WEBPKI_LOGO);

            ////////////////////////////////////////////////////////////////////////////////////////////
            // Android WebPKI version check
            ////////////////////////////////////////////////////////////////////////////////////////////
            grantedVersions = getPropertyStringList(VERSION_CHECK);
 
            ////////////////////////////////////////////////////////////////////////////////////////////
            // Show a sign that the user succeeded getting Saturn credentials
            ////////////////////////////////////////////////////////////////////////////////////////////
            URL hostUrl = new URL(keygen2EnrollmentUrl);
            String merchantHost = hostUrl.getHost();
            if (merchantHost.equals("mobilepki.org")) {
                merchantHost = "test.webpki.org";
            }
            String merchantUrl = new URL(hostUrl.getProtocol(), merchantHost, hostUrl.getPort(), "/webpay-merchant").toExternalForm(); 
            logger.info(merchantUrl);
            successImageAndMessage = new StringBuffer("<div style=\"width:150pt;height:" + (150 * 170 / 420) + "pt;margin-bottom:5pt\">")
                .append(getResourceAsString(SATURN_LOGO))
                .append("</div><b>Enrollment Succeeded!</b><p><a href=\"")
                .append(merchantUrl)
                .append("\">Continue to merchant site</a></p>").toString();

            ////////////////////////////////////////////////////////////////////////////////////////////
            // Get TLS server certificate (if necessary)
            ////////////////////////////////////////////////////////////////////////////////////////////
            if (keygen2EnrollmentUrl.startsWith("https")) {
                new Thread() {
                    @Override
                    public void run() {
                        try {
                            HTTPSWrapper wrapper = new HTTPSWrapper();
                            String url = keygen2EnrollmentUrl;
                            wrapper.setRequireSuccess(false);
                            if (serverPortMapping != null) {
                                URL url2 = new URL(url);
                                url = new URL(url2.getProtocol(),
                                              url2.getHost(),
                                              serverPortMapping,
                                              url2.getFile()).toExternalForm();
                            }
                            wrapper.makeGetRequest(url);
                            tlsCertificate = wrapper.getServerCertificate();
                            logger.info("TLS cert: " + tlsCertificate.getSubjectX500Principal().getName());
                        } catch (Exception e) {
                            logger.log(Level.SEVERE, "********\n" + e.getMessage() + "\n********", e);
                        }
                    }
                }.start();
            }
            
            logger.info("Saturn KeyProvider-server initiated");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "********\n" + e.getMessage() + "\n********", e);
        }
    }

    static boolean isDebug() {
        return true;
    }
}
