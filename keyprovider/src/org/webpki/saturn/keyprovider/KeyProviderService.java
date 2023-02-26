/*
 *  Copyright 2015-2020 WebPKI.org (http://webpki.org).
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
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

import java.net.URL;

import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.security.SecureRandom;

import java.security.cert.X509Certificate;

import java.security.spec.ECGenParameterSpec;

import java.util.ArrayList;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.naming.Context;
import javax.naming.InitialContext;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import javax.sql.DataSource;

import org.webpki.crypto.CertificateUtil;
import org.webpki.crypto.CustomCryptoProvider;
import org.webpki.crypto.HashAlgorithms;
import org.webpki.crypto.AsymSignatureAlgorithms;
import org.webpki.crypto.KeyAlgorithms;
import org.webpki.crypto.ContentEncryptionAlgorithms;
import org.webpki.crypto.KeyEncryptionAlgorithms;

import org.webpki.json.JSONArrayReader;
import org.webpki.json.JSONObjectReader;
import org.webpki.json.JSONParser;

import org.webpki.util.ArrayUtil;

import org.webpki.saturn.common.CryptoUtils;
import org.webpki.saturn.common.KeyStoreEnumerator;

import org.webpki.webutil.InitPropertyReader;

public class KeyProviderService extends InitPropertyReader implements ServletContextListener {

    static Logger logger = Logger.getLogger(KeyProviderService.class.getCanonicalName());
    
    static final String SATURN_LOGO                 = "saturn_logotype";

    static final String ANDROID_WEBPKI_VERSIONS     = "android_webpki_versions";

    static final String ANDROID_CHROME_VERSION      = "android_chrome_version";

    static final String MERCHANT_URL                = "merchant_url";

    static final String KEYGEN2_BASE_URL            = "keygen2_base_url";
    
    static final String AUTHORITY_URL               = "authority_url";

    static final String WELL_KNOWN_URL              = "well_known_url";

    static final String KEYSTORE_PASSWORD           = "key_password";

    static final String KEYPROV_KMK                 = "keyprov_kmk";
    
    static final String TLS_CERTIFICATE             = "server_tls_certificate";

    static final String USE_W3C_PAYMENT_REQUEST     = "use_w3c_payment_request";

    static final String W3C_PAYMENT_REQUEST_URL     = "w3c_payment_request_url";

    static final String LOGGING                     = "logging";

    static final String BIOMETRIC_SUPPORT           = "biometric_support";

    static final String INHOUSE_LOGO                = "inhouse_logo";

    static final String UI_STRESS                   = "ui_stress";

    static KeyStoreEnumerator keyManagementKey;
    
    static Integer serverPortMapping;

    static String serverCertificatePath;

    static KeyPair carrierCaKeyPair;

    static String androidWebPkiVersions;

    static int androidChromeVersion;

    static DataSource jdbcDataSource;

    static boolean inHouseLogo;
    
    static boolean biometricSupport;

    static boolean logging;
    
    static X509Certificate getServerCertificate() throws IOException {
        try {
            return CertificateUtil.getCertificateFromBlob(
                    ArrayUtil.readFile(serverCertificatePath));
        } catch (GeneralSecurityException e) {
            throw new IOException(e);
        }
    }

    class CredentialTemplate {

        AsymSignatureAlgorithms signatureAlgorithm;
        String accountType;
        KeyAlgorithms keyAlgorithm;
        String paymentMethod;
        boolean cardFormatted;
        boolean providerWellKnownFlag;
        byte[] optionalServerPin;
        String friendlyName;
        String svgCardImage;
        PublicKey encryptionKey;
        HashAlgorithms requestHashAlgorithm;
        ContentEncryptionAlgorithms dataEncryptionAlgorithm;
        KeyEncryptionAlgorithms keyEncryptionAlgorithm;
        boolean biometricOption;
        boolean balanceService;
        
        public CredentialTemplate(JSONObjectReader rd) throws IOException,
                                                              GeneralSecurityException {
            paymentMethod = rd.getString("paymentMethod");
            providerWellKnownFlag = rd.getBoolean("providerWellKnownFlag");
            signatureAlgorithm = CryptoUtils.getSignatureAlgorithm(rd, "signatureAlgorithm");
            accountType = rd.getString("accountType");
            requestHashAlgorithm = CryptoUtils.getHashAlgorithm(rd, "requestHashAlgorithm");
            keyAlgorithm = CryptoUtils.getKeyAlgorithm(rd, "signatureKeyAlgorithm");
            cardFormatted = rd.getBoolean("cardFormatted");
            biometricOption = rd.getBoolean("biometricOption");
            if (rd.hasProperty("serverSetPIN")) {
                // Note: numbers only
                optionalServerPin = rd.getString("serverSetPIN").getBytes("utf-8");
            }
            friendlyName = rd.getString("friendlyName");
            svgCardImage = getResourceAsString(rd.getString("cardImage"));
            if (inHouseLogo) {
                URL hostUrl = new URL(authorityUrl);
                String host = hostUrl.getHost() + (hostUrl.getPort() < 0 ? "" : ":" +  hostUrl.getPort());
                svgCardImage = svgCardImage.substring(0, svgCardImage.lastIndexOf("</svg>")) +
                        getResourceAsString("inhouse-flag.txt").replace("HOST", host);
            }
            svgCardImage = svgCardImage.replaceAll("\n", "");
            JSONObjectReader encryptionParameters = rd.getObject("encryptionParameters");
            encryptionKey = encryptionParameters.getPublicKey();
            dataEncryptionAlgorithm = 
                    ContentEncryptionAlgorithms
                        .getAlgorithmFromId(encryptionParameters
                                .getString("dataEncryptionAlgorithm"));
            keyEncryptionAlgorithm = 
                    KeyEncryptionAlgorithms
                        .getAlgorithmFromId(encryptionParameters
                            .getString("keyEncryptionAlgorithm"));
            
            balanceService = rd.getBoolean("balanceService");
            rd.checkForUnread();
        }
    }

    static ArrayList<CredentialTemplate> credentialTemplates = new ArrayList<>();

    static String saturnLogotype;

    static String keygen2RunUrl;
    
    static String authorityUrl;

    static boolean useW3cPaymentRequest;

    static String w3cPaymentRequestUrl;

    static String successMessage;

    InputStream getResource(String name) throws IOException {
        InputStream is = this.getClass().getResourceAsStream(name);
        if (is == null) {
            throw new IOException("Resource fail for: " + name);
        }
        return is;
    }

    String getResourceAsString(String name) throws IOException {
        return new String(ArrayUtil.getByteArrayFromInputStream(getResource(name)),
                          "UTF-8");
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        initProperties (sce);
        CustomCryptoProvider.forcedLoad(false);
        try {

            ////////////////////////////////////////////////////////////////////////////////////////////
            // In house operation?
            ////////////////////////////////////////////////////////////////////////////////////////////
            inHouseLogo = getPropertyBoolean(INHOUSE_LOGO);

            ////////////////////////////////////////////////////////////////////////////////////////////
            // Authority URL.  Assumption: all virtual cards are issued by the same entity
            ////////////////////////////////////////////////////////////////////////////////////////////
            authorityUrl = getPropertyString(AUTHORITY_URL);

            ////////////////////////////////////////////////////////////////////////////////////////////
            // We support biometric authentication?
            ////////////////////////////////////////////////////////////////////////////////////////////
            biometricSupport = getPropertyBoolean(BIOMETRIC_SUPPORT);

            ////////////////////////////////////////////////////////////////////////////////////////////
            // Credentials
            ////////////////////////////////////////////////////////////////////////////////////////////
            JSONArrayReader ar = JSONParser.parse(getResourceAsString("credential-templates.json"))
                    .getJSONArrayReader();
            boolean uiStress = getPropertyBoolean(UI_STRESS);
            while (ar.hasMore()) {
                CredentialTemplate credentialTemplate = new CredentialTemplate(ar.getObject());
                if (uiStress) {
                    for (int q = 0; q < 19; q++) {
                        credentialTemplates.add(credentialTemplate);
                    }
                    uiStress = false;
                }
                credentialTemplates.add(credentialTemplate);
            }

            ////////////////////////////////////////////////////////////////////////////////////////////
            // SKS key management key
            ////////////////////////////////////////////////////////////////////////////////////////////
            keyManagementKey = new KeyStoreEnumerator(getResource(getPropertyString(KEYPROV_KMK)),
                                                      getPropertyString(KEYSTORE_PASSWORD));

            ////////////////////////////////////////////////////////////////////////////////////////////
            // Saturn logotype
            ////////////////////////////////////////////////////////////////////////////////////////////
            saturnLogotype = getResourceAsString(getPropertyString(SATURN_LOGO));

            ////////////////////////////////////////////////////////////////////////////////////////////
            // Android WebPKI version check (vlow-vhigh)
            ////////////////////////////////////////////////////////////////////////////////////////////
            androidWebPkiVersions = getPropertyString(ANDROID_WEBPKI_VERSIONS);

            ////////////////////////////////////////////////////////////////////////////////////////////
            // Android Chrome version check
            ////////////////////////////////////////////////////////////////////////////////////////////
            androidChromeVersion = getPropertyInt(ANDROID_CHROME_VERSION);

            ////////////////////////////////////////////////////////////////////////////////////////////
            // Get path to TLS server certificate
            ////////////////////////////////////////////////////////////////////////////////////////////
            serverCertificatePath = getPropertyString(TLS_CERTIFICATE);
            
            ////////////////////////////////////////////////////////////////////////////////////////////
            // Create a CA keys.  Note Saturn payment credentials do not use PKI
            ////////////////////////////////////////////////////////////////////////////////////////////
            KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
            ECGenParameterSpec eccgen = new ECGenParameterSpec(KeyAlgorithms.P_256.getJceName());
            generator.initialize(eccgen, new SecureRandom());
            carrierCaKeyPair = generator.generateKeyPair();

            ////////////////////////////////////////////////////////////////////////////////////////////
            // Own URL
            ////////////////////////////////////////////////////////////////////////////////////////////
            keygen2RunUrl = getPropertyString(KEYGEN2_BASE_URL) + "/getkeys";

            ////////////////////////////////////////////////////////////////////////////////////////////
            // Success message
            ////////////////////////////////////////////////////////////////////////////////////////////
            successMessage = 
                    new StringBuilder(
                                "<div style=\"text-align:center\"><div class=\"label\" " +
                                "style=\"margin-bottom:10pt\">Enrollment Succeeded!</div><div><a href=\"")
                        .append(getPropertyString(MERCHANT_URL))
                        .append("\" class=\"link\">Continue to merchant site</a></div></div>").toString();

            ////////////////////////////////////////////////////////////////////////////////////////////
            // Are we logging?
            ////////////////////////////////////////////////////////////////////////////////////////////
            logging = getPropertyBoolean(LOGGING);

            ////////////////////////////////////////////////////////////////////////////////////////////
            // W3C PaymentRequest
            ////////////////////////////////////////////////////////////////////////////////////////////
            useW3cPaymentRequest = getPropertyBoolean(USE_W3C_PAYMENT_REQUEST);
            w3cPaymentRequestUrl = getPropertyString(W3C_PAYMENT_REQUEST_URL);

            ////////////////////////////////////////////////////////////////////////////////////////////
            // Database
            ////////////////////////////////////////////////////////////////////////////////////////////
            Context initContext = new InitialContext();
            Context envContext  = (Context)initContext.lookup("java:/comp/env");
            jdbcDataSource = (DataSource)envContext.lookup("jdbc/PAYER_BANK");
            DataBaseOperations.testConnection();

            logger.info("Saturn KeyProvider-server initiated: " + 
                        getServerCertificate().getSubjectX500Principal().getName());
        } catch (Exception e) {
            logger.log(Level.SEVERE, "********\n" + e.getMessage() + "\n********", e);
        }
    }
}
