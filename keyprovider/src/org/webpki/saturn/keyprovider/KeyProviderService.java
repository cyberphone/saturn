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

import java.math.BigDecimal;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.security.SecureRandom;

import java.security.cert.X509Certificate;

import java.security.spec.ECGenParameterSpec;

import java.sql.Connection;

import java.util.Vector;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.naming.Context;
import javax.naming.InitialContext;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import javax.sql.DataSource;

import org.webpki.crypto.AlgorithmPreferences;
import org.webpki.crypto.CertificateUtil;
import org.webpki.crypto.AsymSignatureAlgorithms;
import org.webpki.crypto.KeyAlgorithms;

import org.webpki.json.JSONArrayReader;
import org.webpki.json.DataEncryptionAlgorithms;
import org.webpki.json.JSONObjectReader;
import org.webpki.json.JSONParser;
import org.webpki.json.KeyEncryptionAlgorithms;

import org.webpki.util.ArrayUtil;

import org.webpki.saturn.common.KeyStoreEnumerator;
import org.webpki.saturn.common.PaymentMethods;

import org.webpki.webutil.InitPropertyReader;

public class KeyProviderService extends InitPropertyReader implements ServletContextListener {

    static Logger logger = Logger.getLogger(KeyProviderService.class.getCanonicalName());
    
    static final String SATURN_LOGO                 = "saturn_logotype";

    static final String VERSION_CHECK               = "android_webpki_versions";

    static final String MERCHANT_URL                = "merchant_url";

    static final String KEYGEN2_BASE_URL            = "keygen2_base_url";

    static final String KEYSTORE_PASSWORD           = "key_password";

    static final String KEYPROV_KMK                 = "keyprov_kmk";
    
    static final String TLS_CERTIFICATE             = "server_tls_certificate";

    static final String USE_W3C_PAYMENT_REQUEST     = "use_w3c_payment_request";

    static final String W3C_PAYMENT_REQUEST_HOST    = "w3c_payment_request_host";

    static final String LOGGING                     = "logging";

    static KeyStoreEnumerator keyManagementKey;
    
    static Integer serverPortMapping;

    static X509Certificate serverCertificate;

    static KeyPair carrierCaKeyPair;

    static String grantedVersions;

    static DataSource jdbcDataSource;
    
    static boolean logging;

    class CredentialTemplate {

        AsymSignatureAlgorithms signatureAlgorithm;
        AccountTypes accountType;
        KeyAlgorithms keyAlgorithm;
        String paymentMethod;
        boolean cardFormatted;
        byte[] optionalServerPin;
        String friendlyName;
        String authorityUrl;
        String svgCardImage;
        PublicKey encryptionKey;
        DataEncryptionAlgorithms dataEncryptionAlgorithm;
        KeyEncryptionAlgorithms keyEncryptionAlgorithm;
        
        BigDecimal tempBalanceFix;
        
        public CredentialTemplate(JSONObjectReader rd) throws IOException {
            signatureAlgorithm =
                    AsymSignatureAlgorithms.getAlgorithmFromId(rd.getString("signatureAlgorithm"),
                                                               AlgorithmPreferences.JOSE);
            accountType = AccountTypes.valueOf(rd.getString("accountType"));
            keyAlgorithm = 
                    KeyAlgorithms.getKeyAlgorithmFromId(rd.getString("signatureKeyAlgorithm"), 
                                                        AlgorithmPreferences.JOSE);
            PaymentMethods.fromTypeUrl(paymentMethod = rd.getString("paymentMethod"));
            cardFormatted = rd.getBoolean("cardFormatted");
            if (rd.hasProperty("serverSetPIN")) {
                optionalServerPin = rd.getString("serverSetPIN").getBytes("utf-8");
            }
            authorityUrl = rd.getString("authorityUrl");
            friendlyName = rd.getString("friendlyName");
            svgCardImage = new String(ArrayUtil
                    .getByteArrayFromInputStream(getResource(rd.getString("cardImage"))), "utf-8");
            JSONObjectReader encryptionParameters = rd.getObject("encryptionParameters");
            encryptionKey = encryptionParameters.getPublicKey();
            dataEncryptionAlgorithm = 
                    DataEncryptionAlgorithms
                        .getAlgorithmFromId(encryptionParameters
                                .getString("dataEncryptionAlgorithm"));
            keyEncryptionAlgorithm = 
                    KeyEncryptionAlgorithms
                        .getAlgorithmFromId(encryptionParameters
                            .getString("keyEncryptionAlgorithm"));
            
            tempBalanceFix = rd.getBigDecimal("temp.bal.fix");
            rd.checkForUnread();
        }
    }

    static Vector<CredentialTemplate> credentialTemplates = new Vector<CredentialTemplate>();

    static String saturnLogotype;

    static String keygen2RunUrl;

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

    String getResourceAsString(String propertyName) throws IOException {
        return new String(ArrayUtil.getByteArrayFromInputStream(getResource(getPropertyString(propertyName))),
                          "UTF-8");
    }

    void initDataBaseEnums(Connection connection) throws Exception {
        AccountTypes.init(connection);
        connection.close();
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        initProperties (sce);
        try {

            ////////////////////////////////////////////////////////////////////////////////////////////
            // Credentials
            ////////////////////////////////////////////////////////////////////////////////////////////
            JSONArrayReader ar = 
                    JSONParser.parse(ArrayUtil.getByteArrayFromInputStream(getResource("credential-templates.json")))
                            .getJSONArrayReader();
            while (ar.hasMore()) {
                credentialTemplates.add(new CredentialTemplate(ar.getObject()));
            }


            ////////////////////////////////////////////////////////////////////////////////////////////
            // SKS key management key
            ////////////////////////////////////////////////////////////////////////////////////////////
            keyManagementKey = new KeyStoreEnumerator(getResource(getPropertyString(KEYPROV_KMK)),
                                                      getPropertyString(KEYSTORE_PASSWORD));

            ////////////////////////////////////////////////////////////////////////////////////////////
            // Saturn logotype
            ////////////////////////////////////////////////////////////////////////////////////////////
            saturnLogotype = getResourceAsString(SATURN_LOGO);

            ////////////////////////////////////////////////////////////////////////////////////////////
            // Android WebPKI version check (vlow-vhigh)
            ////////////////////////////////////////////////////////////////////////////////////////////
            grantedVersions = getPropertyString(VERSION_CHECK);

            ////////////////////////////////////////////////////////////////////////////////////////////
            // Get TLS server certificate
            ////////////////////////////////////////////////////////////////////////////////////////////
            serverCertificate = CertificateUtil.getCertificateFromBlob(
                    ArrayUtil.readFile(getPropertyString(TLS_CERTIFICATE)));
            
            ////////////////////////////////////////////////////////////////////////////////////////////
            // Create a CA keys.  Note Saturn payment credentials do not use PKI
            ////////////////////////////////////////////////////////////////////////////////////////////
            KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
            ECGenParameterSpec eccgen = new ECGenParameterSpec(KeyAlgorithms.NIST_P_256.getJceName());
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
            w3cPaymentRequestUrl = getPropertyString(W3C_PAYMENT_REQUEST_HOST) + "/method";

            ////////////////////////////////////////////////////////////////////////////////////////////
            // Database
            ////////////////////////////////////////////////////////////////////////////////////////////
            Context initContext = new InitialContext();
            Context envContext  = (Context)initContext.lookup("java:/comp/env");
            jdbcDataSource = (DataSource)envContext.lookup("jdbc/PAYER_BANK");

            initDataBaseEnums(jdbcDataSource.getConnection());

            logger.info("Saturn KeyProvider-server initiated: " + serverCertificate.getSubjectX500Principal().getName());
        } catch (Exception e) {
            logger.log(Level.SEVERE, "********\n" + e.getMessage() + "\n********", e);
        }
    }
}
