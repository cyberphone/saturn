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

import java.math.BigDecimal;

import java.net.URLEncoder;

import java.security.KeyPair;

import java.util.ArrayList;
import java.util.Base64;
import java.util.GregorianCalendar;
import java.util.LinkedHashMap;
import java.util.List;

import java.util.logging.Logger;

import javax.servlet.ServletException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.webpki.json.JSONCryptoHelper;
import org.webpki.json.JSONDecryptionDecoder;
import org.webpki.json.JSONObjectReader;
import org.webpki.json.JSONObjectWriter;
import org.webpki.json.JSONOutputFormats;
import org.webpki.json.JSONParser;

import org.webpki.saturn.common.MobileProxyParameters;
import org.webpki.saturn.common.AuthorizationDataDecoder;
import org.webpki.saturn.common.BaseProperties;
import org.webpki.saturn.common.Currencies;
import org.webpki.saturn.common.NonDirectPaymentEncoder;
import org.webpki.saturn.common.PaymentMethods;
import org.webpki.saturn.common.RecurringPaymentIntervals;
import org.webpki.saturn.common.ReservationSubTypes;
import org.webpki.saturn.common.TimeUtils;
import org.webpki.saturn.common.PaymentRequestEncoder;
import org.webpki.saturn.common.PaymentClientRequestEncoder;

import org.webpki.saturn.common.PaymentClientRequestEncoder.SupportedPaymentMethod;

import org.webpki.util.ArrayUtil;
import org.webpki.util.PEMDecoder;

import org.webpki.webutil.ServletUtil;

public class WalletUiTestServlet extends HttpServlet implements BaseProperties {

    private static final long serialVersionUID = 1L;
    
    static Logger logger = Logger.getLogger(WalletUiTestServlet.class.getName ());
    
    private static final String W3C_MODE = "w3c";

    private static final String TYPE = "type";
    
    private static final String KEY      =  "key";
    private static final String KEY_TEXT =  "keytxt";
    
    private static final String AUTHZ = "authz";
    private static final String REQUEST = "req";
    
    static final String THIS_SERVLET    = "walletuitest";
 
    private static final String INIT_TAG  = "init";
    private static final String ABORT_TAG = "abort";
    private static final String PARAM_TAG = "msg";
    private static final String ERROR_TAG = "err";

    private static final String BUTTON_ID  = "start";
    private static final String ERROR_ID   = "err";
    private static final String WAITING_ID = "wait";

    static class PaymentType {
        String payeeCommonName;
        BigDecimal amount;
        Currencies currency;
        NonDirectPaymentEncoder nonDirectPayments;
    }
    
    static LinkedHashMap<String, PaymentType> sampleTests = new LinkedHashMap<>();
    
    static void initMerchant(String typeOfPayment,
                             String payeeCommonName, 
                             String amount,
                             Currencies currency,
                             NonDirectPaymentEncoder nonDirectPayments) {
        PaymentType o = new PaymentType();
        o.payeeCommonName = payeeCommonName;
        o.amount = new BigDecimal(amount);
        o.currency = currency;
        o.nonDirectPayments = nonDirectPayments;
        sampleTests.put(typeOfPayment, o);
    }
    
    static {
         try {
            initMerchant("Direct - EUR",  "Space Shop",
                                          "550",
                                          Currencies.EUR,
                                          null);

            initMerchant("Direct - USD",  "Space Shop",
                                          "550",
                                          Currencies.USD,
                                          null);

            initMerchant("Gas Station",   "Planet Gas",
                                          "200",
                                          Currencies.EUR,
                                          NonDirectPaymentEncoder.reservation(
                                                  ReservationSubTypes.GAS_STATION,
                                                  TimeUtils.inMinutes(45),
                                                  true));

            initMerchant("Hotel Booking", "Best Lodging",
                                          "1200",
                                          Currencies.EUR,
                                          NonDirectPaymentEncoder.reservation(
                                                  ReservationSubTypes.BOOKING,
                                                  TimeUtils.inDays(10), 
                                                  true));
            initMerchant("Electricy",     "Power to You",
                                          "0",
                                          Currencies.EUR,
                                          NonDirectPaymentEncoder.recurring(
                                                  RecurringPaymentIntervals.MONTHLY,                                           
                                                  12, 
                                                  false));

            initMerchant("Phone Subscription", 
                                          "ET Phone Home",
                                          "129.99",
                                          Currencies.EUR,
                                          NonDirectPaymentEncoder.recurring(
                                                  RecurringPaymentIntervals.MONTHLY,                                             
                                                  24, 
                                                  true));
            initMerchant("Multiple Paments", 
                                          "Amazon",
                                          "34.75",
                                          Currencies.EUR,
                                          NonDirectPaymentEncoder.recurring(
                                                  RecurringPaymentIntervals.UNSPECIFIED,                                             
                                                  null, 
                                                  false));
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        if (request.getParameter(INIT_TAG) == null) {
            guiGetInit(request, response);
        } else {
            walletGetInit(request, response);
        }
    }

    static void printHtml(HttpServletResponse response, 
                          String javascript, 
                          String bodyscript, 
                          String box) throws IOException, ServletException {
        String html = KeyProviderInitServlet.getHTML(javascript, bodyscript, box)
                .replaceFirst("position:absolute;top:15pt;left:15pt;z-index:5;", "padding:1em;")
                .replaceFirst("displayContainer \\{  display:block;  height:100%;",
                              "displayContainer {  display:block;");
        KeyProviderInitServlet.output(response, html);
    }

    private void returnJson(HttpServletResponse response, JSONObjectWriter json) throws IOException {
        logger.info(json.toString());
        response.setContentType(JSON_CONTENT_TYPE);
        response.setHeader("Pragma", "No-Cache");
        response.setDateHeader("EXPIRES", 0);
        response.getOutputStream().write(json.serializeToBytes(JSONOutputFormats.NORMALIZED));
    }

    private void walletGetInit(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        HttpSession session = getSession(request);
        PaymentType paymentData = sampleTests.get((String)session.getAttribute(TYPE));
        GregorianCalendar timeStamp = new GregorianCalendar();
        GregorianCalendar expires = TimeUtils.inMinutes(30);
        // Create a payment request
        JSONObjectWriter paymentRequest =
            PaymentRequestEncoder.encode(paymentData.payeeCommonName, 
                                         paymentData.amount,
                                         paymentData.currency,
                                         paymentData.nonDirectPayments,
                                         "754329",
                                         timeStamp,
                                         expires);
        List<SupportedPaymentMethod> supportedPaymentMethods = new ArrayList<>();
        supportedPaymentMethods.add(
                new SupportedPaymentMethod(PaymentMethods.BANK_DIRECT,
                                           "https://payments.bigbank.com/payees/86344"));
        supportedPaymentMethods.add(
                new SupportedPaymentMethod(PaymentMethods.SUPER_CARD, 
                                           "https://secure.cardprocessor.com/payees/1077342"));
        supportedPaymentMethods.add(
                new SupportedPaymentMethod(PaymentMethods.UNUSUAL_CARD, 
                                           "https://payments.bigbank.com/payees/86344"));
        JSONObjectWriter requestObject = 
                PaymentClientRequestEncoder.encode(supportedPaymentMethods,
                                                   paymentRequest,
                                                   null, 
                                                   null);
        session.setAttribute(REQUEST, requestObject);
        returnJson(response, requestObject);
    }


    private String testAlternatives() {
        boolean first = true;
        StringBuilder html = new StringBuilder("<select id='" + TYPE + "'>");
        for (String merchant : sampleTests.keySet()) {
             html.append("<option value='")
                 .append(merchant)
                 .append("'")
                 .append(first ? " selected>" : ">")
                 .append(merchant)
                 .append("</option>");
             first = false;
        }
        return html.append("</select>").toString();
    }
    
    private String decryptionKey(HttpSession session) {
        String key = (session != null && session.getAttribute(KEY_TEXT) != null) ?
                (String)session.getAttribute(KEY_TEXT)
                            : 
                "-----BEGIN PRIVATE KEY-----\n" +
                    Base64.getMimeEncoder().encodeToString(
                        KeyProviderService.keyManagementKey.getPrivateKey().getEncoded()) +
                        "\n-----END PRIVATE KEY-----";
        return new StringBuilder(
                "<textarea" +
                " rows='10' maxlength='100000'" +
                " style='box-sizing:border-box;width:100%;white-space:nowrap;overflow:scroll;" +
                "border-width:1px;border-style:solid;border-color:grey;padding:10pt' " +
                "id='" + KEY + "'>")
            .append(key)
            .append("</textarea>").toString();
    }

    private void guiGetInit(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        HttpSession session = request.getSession(false);
        StringBuilder html = new StringBuilder(
            "<div style='padding:0 1em'>" +
                    "This application is intended for testing the Saturn wallet." +
            "</div>" +
            "<div style='display:flex;justify-content:center;padding-top:1em'>" +
              "<div>" +
                "<div style='margin-bottom:2pt'>Select payment type:</div>" + 
                testAlternatives() +
              "</div>" +
            "</div>" +
            "<div style='flex:none;display:block;width:inherit'>" +
              "<div style='padding:1em'>" +
                "<div style='margin-bottom:2pt'>Bank decryption key:</div>" + 
                 decryptionKey(session) + 
              "</div>" +
            "</div>" +
            "<div id='" + ERROR_ID + "' " +
              "style='color:red;font-weight:bold;padding:1em 0 2em 0" + 
                (request.getParameter(ABORT_TAG) == null ? 
                       ";display:none'>" : "'>User cancelled the operation") +
            "</div>" +
            "<img id='" + WAITING_ID + "' src='waiting.gif' " +
            "style='padding-bottom:1em;display:none' alt='waiting'>" +
            "<div style='display:flex;justify-content:center'>" +
              "<div id='" + BUTTON_ID + "' class='stdbtn' onclick=\"invokeWallet()\">" +
                "Invoke Wallet!" + 
              "</div>" +
            "</div>");
        String bodyScript = null;
        if (session != null) {
            byte[] jsonBlob = (byte[])session.getAttribute(AUTHZ);
            if (jsonBlob != null) {
                html.append("</div><div style='padding:1.5em 1em'>");
                JSONObjectReader walletRequest =
                        new JSONObjectReader((JSONObjectWriter)session.getAttribute(REQUEST));
                fancyPrint(html, "Wallet Request", walletRequest);
                JSONObjectReader walletResponse = JSONParser.parse(jsonBlob);
                fancyPrint(html, "Wallet Response", walletResponse);
                try {
                    JSONDecryptionDecoder decoder =
            walletResponse.getObject(ENCRYPTED_AUTHORIZATION_JSON)
                .getEncryptionObject(new JSONCryptoHelper.Options()
                     .setPublicKeyOption(JSONCryptoHelper.PUBLIC_KEY_OPTIONS.KEY_ID_XOR_PUBLIC_KEY)
                     .setKeyIdOption(JSONCryptoHelper.KEY_ID_OPTIONS.OPTIONAL));
                   KeyPair keyPair = (KeyPair)session.getAttribute(KEY);
                    if (!decoder.getPublicKey().equals(keyPair.getPublic())) {
                        throw new IOException("Non-matching public key");
                    }
                    JSONObjectReader userAuthorization = JSONParser.parse(
                            decoder.getDecryptedData(keyPair.getPrivate()));
                    fancyPrint(html, "User Attestation", userAuthorization);
                    JSONObjectReader signatureObject = 
                            userAuthorization.getObject(AUTHORIZATION_SIGNATURE_JSON);
                    JSONCryptoHelper.Options options = new JSONCryptoHelper.Options();
                    boolean verifiableSignature = 
                            signatureObject.hasProperty(JSONCryptoHelper.PUBLIC_KEY_JSON);
                    options.setPublicKeyOption(JSONCryptoHelper.PUBLIC_KEY_OPTIONS.OPTIONAL);
                    options.setKeyIdOption(JSONCryptoHelper.KEY_ID_OPTIONS.FORBIDDEN);
                    AuthorizationDataDecoder authorizationData =
                            new AuthorizationDataDecoder(userAuthorization, options);
                    if (!verifiableSignature) {
                        html.append("<div>Signature was not verified (public key " +
                                    "is associated with \"" + CREDENTIAL_ID_JSON + "\").</div>");
                    }
                    if (!ArrayUtil.compare(authorizationData.getRequestHashAlgorithm().digest(
                            walletRequest.getObject(PAYMENT_REQUEST_JSON)
                                .serializeToBytes(JSONOutputFormats.CANONICALIZED)), 
                                           authorizationData.getRequestHash())) {
                        throw new IOException("\"" + REQUEST_HASH_JSON + "\" mismatch");
                    }
                    html.append("<div style='text-align:center;font-size:11pt'>Successful Operation</div>");
                 } catch (Exception e) {
                    bodyScript = "onload=\"applicationError('" + e.getMessage() + "')\"";
                }
            }
            session.removeAttribute(AUTHZ);
        }
        printHtml(response,
                  "history.pushState(null, null, '" + THIS_SERVLET + "');\n" +
                  "window.addEventListener('popstate', function(event) {\n" +
                  "  history.pushState(null, null, '" + THIS_SERVLET + "');\n" +
                  "});\n" +
                  "function applicationError(msg) {\n" +
                  "  console.error(msg);\n" +
                  "  let element = document.getElementById('" + ERROR_ID + "');\n" +
                  "  element.innerHTML = msg;\n" +
                  "  element.style.display = 'block';\n" +
                  "  document.getElementById('" + WAITING_ID + "').style.display = 'none';\n" +
                  "}\n\n" +

                  "async function invokeWallet() {\n" +
                  "  if (window.PaymentRequest) {\n" +
                  // It takes a second or two to get PaymentRequest up and running.
                  // Show that to the user.
                  "    document.getElementById('" + WAITING_ID + "').style.display = 'block';\n" +
                  // This code may seem strange but the Web application does not create
                  // an HttpSession so we do this immediately after the user hit the
                  // invokeWallet button.  Using fetch this becomes invisible UI wise.
                  // Read current FORM data as well and add that to the HttpSession
                  // to be created on the server.
                  "    var formData = new URLSearchParams();\n" +
                  "    formData.append('" + TYPE +
                    "', document.getElementById('" + TYPE + "').value);\n" +
                    "    formData.append('" + KEY +
                    "', document.getElementById('" + KEY + "').value);\n" +
                  "    try {\n" +
                  "      const httpResponse = await fetch('" + THIS_SERVLET + "', {\n" +
                  "        method: 'POST',\n" +
                  "        body: formData\n" +
                  "      });\n" +
                  "      if (httpResponse.status == " + HttpServletResponse.SC_OK + ") {\n" +
                  "        const invocationUrl = await httpResponse.text();\n" +
                  // Success! Now we hook into the W3C PaymentRequest using "dummy" payment data
                  "        const dummyDetails = {total:{label:'total',amount:{currency:'USD',value:'1.00'}}};\n" +
                  "        const methodData = [{\n" +
                  "          supportedMethods: '" + KeyProviderService.w3cPaymentRequestUrl + "',\n" +
            // Test data
//                  "          supportedMethods: 'weird-pay',\n" +
                  "          data: [invocationUrl]\n" +
            // Test data
//                  "          supportedMethods: 'basic-card',\n" +
//                  "          data: {supportedNetworks: ['visa', 'mastercard']}\n" +
                  "        }];\n" +
                  "        const payRequest = new PaymentRequest(methodData, dummyDetails);\n" +
                  "        if (await payRequest.canMakePayment()) {\n" +
                  "          const payResponse = await payRequest.show();\n" +
                  "          payResponse.complete('success');\n" +
                  // Note that success does not necessarily mean that the enrollment succeeded,
                  // it just means that the result is a URL to be redirected to.
                  "          document.location.href = payResponse.details." +
                    MobileProxyParameters.W3CPAY_GOTO_URL + ";\n" +
                  "        } else {\n" +
                  "          applicationError('App does not seem to be installed');\n" +
                  "        }\n" +
                  "      } else {\n" +
                  " console.error(httpResponse.status);\n" +
                  "        applicationError(httpResponse.status == " +
                    HttpServletResponse.SC_BAD_REQUEST + 
                    " ? await httpResponse.text() : 'Server error, try again');\n" +
                  "      }\n" +
                  "    } catch (err) {\n" +
                  "      console.error(err);\n" +
                  "      applicationError(err.message);\n" +
                  "    }\n" +
                  "  } else {\n" +
                  // The browser does not support PaymentRequest
                  "      applicationError('Browser does not support PaymentRequest!');\n" +
                  "  }\n" +
                  "}",
                  bodyScript,
                  html.toString());
    }
    
    private void fancyPrint(StringBuilder html, String header, JSONObjectReader json) throws IOException {
        html.append("<div style='text-align:center;font-size:11pt'>")
            .append(header)
            .append("</div><div style='margin:2pt 0;max-width:100%;white-space:nowrap;overflow:scroll'>")
            .append(json.serializeToString(JSONOutputFormats.PRETTY_HTML))
            .append("</div>");
    }

    private HttpSession getSession(HttpServletRequest request) throws IOException {
        HttpSession session = request.getSession(false);
        if (session == null) {
            throw new IOException("Session not found, timeout?");
        }
        return session;
    }

    String getParameter(HttpServletRequest request, String name) throws IOException {
        String value = request.getParameter(name);
        if (value == null) {
            throw new IOException("Missing: " + name);
        }
        return value;
    }

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        if (request.getParameter(TYPE) == null) {
            // Return from Wallet
            HttpSession session = getSession(request);
            if (!request.getContentType().equals(JSON_CONTENT_TYPE)) {
                throw new IOException("Bad content type: " + request.getContentType());
            }
            session.setAttribute(AUTHZ, ServletUtil.getData(request));
            response.sendRedirect(THIS_SERVLET);
        } else {
            // Here the real action begins...
            HttpSession session = request.getSession(true);
            session.setAttribute(TYPE, getParameter(request, TYPE));
            String key = getParameter(request, KEY).trim();
            session.setAttribute(KEY_TEXT, key);
            KeyPair keyPair;
            try {
                keyPair = key.startsWith("{") ?
                    JSONParser.parse(key).getKeyPair()
                                              :
                    PEMDecoder.getKeyPair(key.getBytes("utf-8"));
            } catch (Exception e) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                KeyProviderInitServlet.output(response, "Supplied key problem: " + e.getMessage());
                return;
            }
            session.setAttribute(KEY, keyPair);
            logger.info(new JSONObjectWriter().setPublicKey(keyPair.getPublic()).toString());
            String url = KeyProviderService.keygen2RunUrl.substring(0, 
                    KeyProviderService.keygen2RunUrl.lastIndexOf('/')) + '/' + THIS_SERVLET;
            String urlEncoded = URLEncoder.encode(url, "utf-8");
            String invocationUrl = MobileProxyParameters.SCHEME_W3CPAY +
                "://" + MobileProxyParameters.HOST_SATURN + 
                "?" + MobileProxyParameters.PUP_COOKIE     + "=" + "JSESSIONID%3D" + session.getId() +
                "&" + MobileProxyParameters.PUP_INIT_URL   + "=" + urlEncoded + "%3F" + INIT_TAG + "%3Dtrue" +
                "&" + MobileProxyParameters.PUP_MAIN_URL   + "=" + urlEncoded +
                "&" + MobileProxyParameters.PUP_CANCEL_URL + "=" + urlEncoded + "%3F" + ABORT_TAG + "%3Dtrue" +
                "&" + MobileProxyParameters.PUP_VERSIONS   + "=" + KeyProviderService.androidWebPkiVersions;
            logger.info("POST return=" + invocationUrl);
            KeyProviderInitServlet.output(response, invocationUrl);
        }
    }
}
