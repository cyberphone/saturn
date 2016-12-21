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
package org.webpki.saturn.merchant;

import java.io.IOException;
import java.io.InputStream;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URL;

import java.security.GeneralSecurityException;

import java.security.KeyStore;
import java.security.PublicKey;

import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Vector;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.webpki.crypto.CertificateUtil;
import org.webpki.crypto.CustomCryptoProvider;
import org.webpki.crypto.KeyStoreVerifier;

import org.webpki.json.JSONObjectReader;
import org.webpki.json.JSONOutputFormats;
import org.webpki.json.JSONParser;
import org.webpki.json.JSONX509Verifier;

import org.webpki.util.ArrayUtil;
import org.webpki.util.Base64;
import org.webpki.util.ISODateTime;

import org.webpki.saturn.common.AccountDescriptor;
import org.webpki.saturn.common.AuthorizationData;
import org.webpki.saturn.common.PayerAccountTypes;
import org.webpki.saturn.common.CardSpecificData;
import org.webpki.saturn.common.Currencies;
import org.webpki.saturn.common.KeyStoreEnumerator;
import org.webpki.saturn.common.ProtectedAccountData;
import org.webpki.saturn.common.ServerAsymKeySigner;

import org.webpki.webutil.InitPropertyReader;

public class MerchantService extends InitPropertyReader implements ServletContextListener {

    static Logger logger = Logger.getLogger(MerchantService.class.getCanonicalName());
    
    static final String MERCHANT_KEY                 = "merchant_key";
    
    static final String MERCHANT_CN                  = "merchant_cn";

    static final String MERCHANT_ID                  = "merchant_id";

    static final String OTHERNETWORK_KEY             = "othernetwork_key";

    static final String OTHERNETWORK_ID              = "othernetwork_id";

    static final String KEYSTORE_PASSWORD            = "key_password";

    static final String PAYMENT_ROOT                 = "payment_root";
    
    static final String ACQUIRER_ROOT                = "acquirer_root";
    
    static final String PAYEE_PROVIDER_AUTHORITY_URL = "payee_provider_authority_url";

    static final String PAYEE_ACQUIRER_AUTHORITY_URL = "payee_acquirer_authority_url";

    static final String SERVER_PORT_MAP              = "server_port_map";
    
    static final String MERCHANT_BASE_URL            = "merchant_base_url";
    
    static final String CURRENCY                     = "currency";

    static final String ADD_UNUSUAL_CARD             = "add_unusual_card";

    static final String SLOW_OPERATION               = "slow_operation";

    static final String W2NB_WALLET                  = "w2nb_wallet";
    
    static final String USER_AUTH_SAMPLE             = "user-authorization.json";

    static final String SUPERCARD_AUTH_SAMPLE        = "wallet-supercard-auth.png";

    static final String BANKDIRECT_AUTH_SAMPLE       = "wallet-bankdirect-auth.png";

    static final String VERSION_CHECK                = "android_webpki_versions";

    static final String BOUNCYCASTLE_FIRST           = "bouncycastle_first";
    
    static final String LOGGING                      = "logging";

    static final String TEST_MODE                    = "test-mode";

    static JSONX509Verifier paymentRoot;
    
    static JSONX509Verifier acquirerRoot;
    
    static LinkedHashMap<PublicKey,PaymentNetwork> paymentNetworks = new LinkedHashMap<PublicKey,PaymentNetwork>();
    
    static String merchantCommonName;
    
    static String payeeAcquirerAuthorityUrl;
    
    static String payeeProviderAuthorityUrl;

    static Integer serverPortMapping;
    
    static String merchantBaseUrl;  // For QR and Android only
    
    static Currencies currency;

    // Web2Native Bridge constants
    static String w2nbWalletName;
    
    // Debug mode samples
    static JSONObjectReader userAuthorizationSample;

    static byte[] protectedAccountData;

    static String walletSupercardAuth;

    static String walletBankdirectAuth;
    
    static PaymentNetwork primaryMerchant;
    
    static Boolean testMode;

    static boolean logging;
    
    private static boolean slowOperation;

    private static int referenceId = 1000000;

    static String[] grantedVersions;

    static String getReferenceId() {
        return "#" + (referenceId++);
    }

    static void slowOperationSimulator() throws InterruptedException {
        if (slowOperation) {
            Thread.sleep(5000);
        }
    }

    InputStream getResource(String name) throws IOException {
        return this.getClass().getResourceAsStream(getPropertyString(name));
    }

    String getImageDataURI (String name) throws IOException  {
        byte[] image = ArrayUtil.getByteArrayFromInputStream (this.getClass().getResourceAsStream(name));
        return "data:image/" + name.substring(name.lastIndexOf('.') + 1) +
               ";base64," +
               new Base64 (false).getBase64StringFromBinary (image);
    }

    JSONX509Verifier getRoot(String name) throws IOException, GeneralSecurityException {
        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load (null, null);
        keyStore.setCertificateEntry ("mykey",
                                      CertificateUtil.getCertificateFromBlob (
                                           ArrayUtil.getByteArrayFromInputStream (getResource(name))));        
        return new JSONX509Verifier(new KeyStoreVerifier(keyStore));
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

    PaymentNetwork addPaymentNetwork(String keyIdProperty, String merchantIdProperty, String[] acceptedAccountTypes) throws IOException {
        KeyStoreEnumerator kse = new KeyStoreEnumerator(getResource(keyIdProperty),
                                                        getPropertyString(KEYSTORE_PASSWORD));
        PaymentNetwork paymentNetwork = new PaymentNetwork(new ServerAsymKeySigner(kse),
                                                           getPropertyString(merchantIdProperty),
                                                           acceptedAccountTypes);
        paymentNetworks.put(kse.getPublicKey(), paymentNetwork);
        return paymentNetwork;
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
    }

    @Override
    public void contextInitialized(ServletContextEvent event) {
        initProperties (event);
        try {
            CustomCryptoProvider.forcedLoad(getPropertyBoolean(BOUNCYCASTLE_FIRST));

            if (getPropertyString(SERVER_PORT_MAP).length () > 0) {
                serverPortMapping = getPropertyInt(SERVER_PORT_MAP);
            }

            // Should be common for all payment networks...
            merchantCommonName = getPropertyString(MERCHANT_CN);

            // An optional payment network (for client testing purposes)
            if (getPropertyString(OTHERNETWORK_KEY).length () > 0) {
                addPaymentNetwork(OTHERNETWORK_KEY, OTHERNETWORK_ID, new String[]{"http://othernetworkpay"});
            }

            // The standard payment network supported by the Saturn demo
            Vector<String> acceptedAccountTypes = new Vector<String>();
            for (PayerAccountTypes card : PayerAccountTypes.values()) {
                if (card != PayerAccountTypes.UNUSUAL_CARD || getPropertyBoolean(ADD_UNUSUAL_CARD)) {
                    acceptedAccountTypes.add(card.getTypeUri());
                }
            }
            primaryMerchant = addPaymentNetwork(MERCHANT_KEY, MERCHANT_ID, acceptedAccountTypes.toArray(new String[0]));

            paymentRoot = getRoot(PAYMENT_ROOT);

            acquirerRoot = getRoot(ACQUIRER_ROOT);
        
            currency = Currencies.valueOf(getPropertyString(CURRENCY));

            w2nbWalletName = getPropertyString(W2NB_WALLET);

            payeeProviderAuthorityUrl = getPropertyString(PAYEE_PROVIDER_AUTHORITY_URL);

            payeeAcquirerAuthorityUrl = getPropertyString(PAYEE_ACQUIRER_AUTHORITY_URL);

            merchantBaseUrl = getURL(getPropertyString(MERCHANT_BASE_URL));

            new AuthorizationData(userAuthorizationSample = JSONParser.parse(
                    ArrayUtil.getByteArrayFromInputStream (this.getClass().getResourceAsStream(USER_AUTH_SAMPLE))));

            walletSupercardAuth = getImageDataURI(SUPERCARD_AUTH_SAMPLE);

            walletBankdirectAuth = getImageDataURI(BANKDIRECT_AUTH_SAMPLE);

            protectedAccountData = 
                ProtectedAccountData.encode(new AccountDescriptor(PayerAccountTypes.SUPER_CARD.getTypeUri(),
                                                                  "6875056745552109"),
                                            new CardSpecificData("Luke Skywalker",
                                                                 ISODateTime.parseDateTime("2019-12-31T00:00:00Z"),
                                                                 "943")).serializeToBytes(JSONOutputFormats.NORMALIZED);

            if (getPropertyString(TEST_MODE).length () > 0) {
                testMode = getPropertyBoolean(TEST_MODE);
            }

            logging = getPropertyBoolean(LOGGING);

            slowOperation = getPropertyBoolean(SLOW_OPERATION);

            ////////////////////////////////////////////////////////////////////////////////////////////
            // Android WebPKI version check
            ////////////////////////////////////////////////////////////////////////////////////////////
            grantedVersions = getPropertyStringList(VERSION_CHECK);

            logger.info("Saturn Merchant-server initiated");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "********\n" + e.getMessage() + "\n********", e);
        }
    }
}
