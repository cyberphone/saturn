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

// Web2Native Bridge emulator Payment Agent (a.k.a. Wallet) application

package org.webpki.saturn.resources;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import java.security.PublicKey;
import java.security.cert.X509Certificate;

import java.util.ArrayList;

import org.webpki.crypto.AsymSignatureAlgorithms;
import org.webpki.crypto.CertificateUtil;
import org.webpki.crypto.CustomCryptoProvider;
import org.webpki.crypto.SignatureWrapper;

import org.webpki.json.JSONArrayReader;
import org.webpki.json.JSONArrayWriter;
import org.webpki.json.JSONCryptoHelper;
import org.webpki.json.JSONDecoderCache;
import org.webpki.json.JSONObjectReader;
import org.webpki.json.JSONObjectWriter;
import org.webpki.json.JSONOutputFormats;
import org.webpki.json.JSONParser;

import org.webpki.saturn.common.BaseProperties;
import org.webpki.saturn.common.KeyStoreEnumerator;
import org.webpki.saturn.common.ServerAsymKeySigner;
import org.webpki.saturn.common.ServerX509Signer;

import org.webpki.util.ArrayUtil;
import org.webpki.util.Base64;

public class JCSPaper implements BaseProperties {
    
    static FileOutputStream fos;
    
    static final String CONTEXT         = "https://json.sample-standards.org/payment";

    static final String STYLE_SIGNATURE = "background:#ffe8e8";
    static final String STYLE_KEY       = "background:#e8ffe8";
    static final String STYLE_MSG       = "font-weight:bold";
    
    static final String AUTHORIZATION   = "Authorization";
    static final String PAYMENT_REQUEST = "PaymentRequest";
    
    static final String JOSE_PAYLOAD    = "payload";
    static final String JOSE_PROTECTED  = "protected";
    static final String JOSE_SIGNATURE  = "signature";
    static final String JOSE_X5C        = "x5c";
    static final String JOSE_ALG        = "alg";
    static final String JOSE_KTY        = "kty";
    static final String JOSE_CRV        = "crv";
    static final String JOSE_JWK        = "jwk";
        
    static void write(byte[] utf8) throws Exception {
        fos.write(utf8);
    }
    
    static void write(String utf8) throws Exception {
        write(utf8.getBytes("UTF-8"));
    }
    static void write(JSONObjectWriter json) throws Exception {
        write(json.serializeToBytes(JSONOutputFormats.PRETTY_HTML));
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 5) {
            System.out.println("\nUsage: " +
                               JCSPaper.class.getCanonicalName() +
                               "jcspaper logotype merchantCertFile mybankCertFile certFilePassword");
            System.exit(-3);
        }
        CustomCryptoProvider.forcedLoad(true);
         
        fos = new FileOutputStream(args[0]);
        
        // Read key/certificate to be imported and create signer
        
        KeyStoreEnumerator paymentRequestKey = new KeyStoreEnumerator (new FileInputStream(args[2]), args[4]);
        ServerAsymKeySigner paymentRequestSigner = new ServerAsymKeySigner(paymentRequestKey);
        KeyStoreEnumerator authorizationKey = new KeyStoreEnumerator (new FileInputStream(args[3]), args[4]);
        ServerX509Signer authorizationSigner = new ServerX509Signer(authorizationKey);

        // Header
        write("<!DOCTYPE html><html><head><title>JSON Signatures</title>" +
              "<meta http-equiv=Content-Type content=\"text/html; charset=utf-8\">" +
              "<style type=\"text/css\">\n" +
              "body {font-size:8pt;color:#000000;font-family:verdana,arial;background-color:white;margin:10pt}\n" +
              "a {font-weight:bold;font-size:8pt;color:blue;font-family:arial,verdana;text-decoration:none}\n" +
              "table {border-spacing:0;border-collapse: collapse;box-shadow:3pt 3pt 3pt #D0D0D0}\n" +
              "td {border-color:black;border-style:solid;border-width:1px;" +
              "padding:2pt 4pt 2pt 4pt;font-size:8pt;font-family:verdana,arial;text-align:center}\n" +
              "div.json {margin-top:8pt;margin-bottom:15pt;word-break:break-all;width:800pt;background:#F8F8F8;border-width:1px;border-style:solid;border-color:grey;padding:10pt;box-shadow:3pt 3pt 3pt #D0D0D0}\n" +
              "div.text {width:800pt}\n" +
              "div.header {font-size:12pt;margin-top:15pt;margin-bottom:8pt;font-family:arial,verdana}\n" +
              "code {font-size:9pt;font-family:'Courier New',Courier}\n" +
              "</style></head><body>" +
              "<div style=\"cursor:pointer;padding:2pt 0 0 0;width:100pt;height:47pt;border-width:1px;" +
              "border-style:solid;border-color:black;box-shadow:3pt 3pt 3pt #D0D0D0\" " +
              "onclick=\"document.location.href='http://webpki.org'\" title=\"Home of WebPKI.org\">");
        write(ArrayUtil.readFile(args[1]));
        write("</div><div class=\"text\" style=\"margin-top:30pt;margin-bottom:20pt;text-align:center;font-size:20pt;font-family:'Times New Roman',Serif\">JSON Signatures</div>" +
           "<div class=\"text\" style=\"margin-bottom:5pt\">" +
           "Signing of " + JSON() + " data essentially comes in two flavors:" +
           "<ul><li>&quot;Freezing&quot; the data and put it in a specific signature container. " + 
           RFC7515() + " (JSON Web Signature) represents such a specification.</li>" +
           "<li>Canonicalize the data and add a signature to it.</li></ul>" +
           "This document briefly outlines an example of the latter called " + JSF() + " (JSON Signature Format). " +
           "<p>Due to the fact that there is currently no generally accepted canonicalization method for JSON, JSF builds on " +
           "a much simpler concept (&quot;Predictable Serialization&quot;) " +
           "which did not become fully practical until " + ECMASCRIPT() + " V6 was launched. " +
           "ECMAScript in short says that JSON (and JavaScript) property order <i>must " +
           "be respected</i> during parsing and serialization which eliminates canonicalization entirely " +
           "and enables the creation of &quot;Crypto&nbsp;Safe&quot; JSON objects that can travel securely " +
           "through different systems without getting corrupted.</p>" +
           "<p>There is also a JSON encryption scheme, " + JEF() + " (JSON Encryption Format), using the same syntax as JSF.</p>" +
           "</div><div class=\"header\">Background<br></div>" +
           "<div class=\"text\">" +
           "Although JSF is not tied to any specific application, it grew from the needs of the " +
           "payment industry where &quot;Stacked&quot; signed messages begin to play an important role. " +
           "Below is a (fictitious) example of such a scheme, where one message embeds another (inner) message:" +
           "</div><div class=\"text\" style=\"text-align:center\">" +
           "<table style=\"margin-left:auto;margin-right:auto;margin-top:10pt;margin-bottom:20pt\"><tr><td><span style=\"" + STYLE_MSG + "\">&nbsp;<br>" + AUTHORIZATION + "</span>" +
              "<table style=\"margin:15pt\"><tr><td style=\"padding:10pt 15pt 10pt 15pt;" + STYLE_MSG + "\">" + PAYMENT_REQUEST + "</td></tr>" +
                "<tr><td style=\"" + STYLE_KEY + "\">Public Key</td></tr>" +
                 "<tr><td style=\"" + STYLE_SIGNATURE + "\">Signature</td></tr>" +
               "</table></td></tr>" +
                "<tr><td style=\"" + STYLE_KEY + "\">X.509 Certificate Path</td></tr>" +
                "<tr><td style=\"" + STYLE_SIGNATURE + "\">Signature</td></tr>" +
           "</table></div><div class=\"text\">" +
           "This scheme could be expressed in JSF like the following (note that the <code>@context</code> and <code>@qualifier</code> properties " +
           "<i>are not a part of JSF</i>, but serve as a possible way to assign a type to a JSON object):" +
           "</div><div class=\"json\">");
        JSONObjectWriter paymentRequest = new JSONObjectWriter();
        paymentRequest.setString(JSONDecoderCache.CONTEXT_JSON, CONTEXT);
        paymentRequest.setString(JSONDecoderCache.QUALIFIER_JSON, PAYMENT_REQUEST);
        paymentRequest.setObject(PAYEE_JSON, new JSONObjectWriter()
            .setString(COMMON_NAME_JSON, "Demo Merchant")
            .setString(HOME_PAGE_JSON, "https://demomerchant.com"));
        paymentRequest.setString(AMOUNT_JSON, "235.50");
        paymentRequest.setString(CURRENCY_JSON, "USD");
        paymentRequest.setString(REFERENCE_ID_JSON, "05630753");
        paymentRequest.setString(TIME_STAMP_JSON, "2019-08-05T10:07:00Z");
        JSONObjectWriter josePaymentRequest = new JSONObjectWriter(JSONParser.parse(paymentRequest.toString()));
        paymentRequest.setSignature(paymentRequestSigner);
        JSONObjectReader joseHeader = JSONParser.parse(paymentRequest.toString())
            .getObject(JSONObjectWriter.SIGNATURE_DEFAULT_LABEL_JSON)
                .getObject(JSONCryptoHelper.PUBLIC_KEY_JSON);
        JSONObjectWriter joseSignedPaymentRequest = 
            jws(josePaymentRequest, 
                new JSONObjectWriter()
                    .setString(JOSE_ALG, AsymSignatureAlgorithms.ECDSA_SHA256.getJoseAlgorithmId())
                    .setObject(JOSE_JWK, new JSONObjectWriter()
                        .setString(JOSE_KTY, joseHeader.getString(JSONCryptoHelper.KTY_JSON))
                        .setString(JOSE_CRV, joseHeader.getString(JSONCryptoHelper.CRV_JSON))
                        .setString(JSONCryptoHelper.X_JSON, joseHeader.getString(JSONCryptoHelper.X_JSON))
                        .setString(JSONCryptoHelper.Y_JSON, joseHeader.getString(JSONCryptoHelper.Y_JSON))),
                 paymentRequestKey);
        JSONObjectWriter writer = new JSONObjectWriter();
        writer.setString(JSONDecoderCache.CONTEXT_JSON, CONTEXT);
        writer.setString(JSONDecoderCache.QUALIFIER_JSON, AUTHORIZATION);
        writer.setObject(PAYMENT_REQUEST_JSON, paymentRequest);
        writer.setString("transactionId", "#1250000005");
        writer.setString(TIME_STAMP_JSON, "2019-02-02T10:07:42Z");
        JSONObjectWriter joseAuthorization = new JSONObjectWriter(JSONParser.parse(writer.toString()));
        joseAuthorization.setupForRewrite(PAYMENT_REQUEST_JSON);
        joseAuthorization.setObject(PAYMENT_REQUEST_JSON, joseSignedPaymentRequest);
        writer.setSignature(authorizationSigner);
        write(writer.serializeToBytes(JSONOutputFormats.PRETTY_HTML));
        write("</div><div class=\"text\">" +
              "The example above would if converted to " + RFC7515() + " be slightly more convoluted " +
               "since data must be Base64-encoded (which was a core rationale for developing JSF). " +
              "Some protocols using " + RFC7515() +
              " even add an <i>extra outer object and property</i> to make the message type readable. This is " +
              "unessesary using JSF because it only manifests itself as a property " +
              "among other properties in a message. " +
              "Anyway, here is the sample message using " +  RFC7515() + " notation:" +
              "</div><div class=\"json\">");
        JSONObjectWriter joseAuthorizationHeader = new JSONObjectWriter()
            .setString(JOSE_ALG, AsymSignatureAlgorithms.ECDSA_SHA256.getJoseAlgorithmId());
        JSONArrayWriter aw = joseAuthorizationHeader.setArray(JOSE_X5C);
        for (X509Certificate cert : authorizationKey.getCertificatePath()) {
            aw.setString(new Base64(false).getBase64StringFromBinary(cert.getEncoded()));
        }
        JSONObjectWriter joseWriter = jws(joseAuthorization,
                                          joseAuthorizationHeader,
                                          authorizationKey);
        JSONObjectReader verifier = JSONParser.parse(joseWriter.toString());
        checkJws (verifier);
        checkJws(JSONParser.parse(verifier.getBinary(JOSE_PAYLOAD)).getObject(PAYMENT_REQUEST_JSON));
        write(joseWriter.serializeToBytes(JSONOutputFormats.PRETTY_HTML));
        System.out.println("JSF=" + writer.serializeToBytes(JSONOutputFormats.NORMALIZED).length +
                           " JWS=" + joseWriter.serializeToBytes(JSONOutputFormats.NORMALIZED).length);
        write("</div><div id=\"jsmode\" class=\"header\" style=\"margin-top:20pt\">JavaScript Usage<br></div>" +
           "<div class=\"text\">"+
              "Since " + JSF() + " is compatible with " + ECMASCRIPT() + " (JavaScript), you can also use " +
               " JSF signatures in browsers. The following shows how the <code>" + PAYMENT_REQUEST +
               "</code> message could be featured inside of an HTML5 document:" +
        
             "</div><div class=\"json\">");
        String jsPaymentRequest = paymentRequest.serializeToString(JSONOutputFormats.PRETTY_HTML);
        jsPaymentRequest = jsPaymentRequest.replaceAll("(^\\{<br>)", "$1&nbsp;&nbsp;&nbsp;&nbsp;<span style=\"color:#008000\">//&nbsp;The&nbsp;Data</span><br>");
        jsPaymentRequest = jsPaymentRequest.replaceAll("(&quot;)(<span style=\"color:#C00000\">)([a-z A-Z]+)(<\\/span>)(&quot;)", "$2$3$4");
        jsPaymentRequest = jsPaymentRequest.replaceAll("(<br>&nbsp;&nbsp;&nbsp;&nbsp;)(<span style=\"color:#C00000\">)(signature)", "$1<span style=\"color:#008000\">//&nbsp;The&nbsp;Signature</span>$1$2$3");
        write("<span style=\"color:orange\">var</span>&nbsp;<span style=\"color:purple\">" +
              PAYMENT_REQUEST_JSON + "</span>&nbsp;=&nbsp;" + jsPaymentRequest.replaceAll("<br>}", "<br>};"));
        write("</div>" +
              "<div id=\"yasmin\" class=\"header\" style=\"margin-top:20pt\">HTTP Usage<br></div>" +
              "<div class=\"text\" style=\"margin-bottom:15pt\">"+
                   "Although just an example, " + YASMIN() +
                   " represents a workable way for using" +
                    " JSF signatures in Web applications." +
              "</div>" +
              "V0.92, A.Rundgren, 2019-12-20" +
              "</body></html>");
        fos.close();
    }

    static String link(String name, String url) {
        return "<a href=\"" + url + "\" target=\"_blank\" title=\"" + name + "\">" + name + "</a>";
    }
 
    private static String RFC7515() {
        return link("RFC7515", "https://tools.ietf.org/rfc/rfc7515.txt");
    }

    private static String JSON() {
        return link("JSON", "https://tools.ietf.org/rfc/rfc7159.txt");
    }

    static String ECMASCRIPT() {
         return link("ECMAScript", "http://www.ecma-international.org/ecma-262/6.0/ECMA-262.pdf");
    }

    static String JSF() {
        return link("JSF", "https://cyberphone.github.io/doc/security/jsf.html");
    }

    static String JEF() {
        return link("JEF", "https://cyberphone.github.io/doc/security/jef.html");
    }

    static String YASMIN() {
        return link("YASMIN", "https://cyberphone.github.io/doc/web/yasmin.html");
    }

    static void checkJws(JSONObjectReader jws) throws Exception {
        JSONObjectReader protectedHeader = JSONParser.parse(jws.getBinary(JOSE_PROTECTED));
        if (!protectedHeader.getString(JOSE_ALG).equals(AsymSignatureAlgorithms.ECDSA_SHA256.getJoseAlgorithmId())) {
            throw new IOException("Bad alg");
        }
        PublicKey publicKey = null;
        if (protectedHeader.hasProperty(JOSE_JWK)) {
            JSONObjectReader jwk = protectedHeader.getObject(JOSE_JWK);
            JSONObjectWriter josePK = new JSONObjectWriter()
                .setObject(JSONCryptoHelper.PUBLIC_KEY_JSON, new JSONObjectWriter()
                    .setString(JSONCryptoHelper.KTY_JSON, jwk.getString(JOSE_KTY))
                    .setString(JSONCryptoHelper.CRV_JSON, jwk.getString(JOSE_CRV))
                    .setString(JSONCryptoHelper.X_JSON, jwk.getString(JSONCryptoHelper.X_JSON))
                    .setString(JSONCryptoHelper.Y_JSON, jwk.getString(JSONCryptoHelper.Y_JSON)));
            publicKey = JSONParser.parse(josePK.toString()).getPublicKey();
        } else {
            JSONArrayReader ar = protectedHeader.getArray(JOSE_X5C);
            ArrayList<X509Certificate> certs = new ArrayList<>();
            while (ar.hasMore()) {
                certs.add(CertificateUtil.getCertificateFromBlob(new Base64().getBinaryFromBase64String(ar.getString())));
            }
            publicKey = certs.get(0).getPublicKey();
        }
        protectedHeader.checkForUnread();
        if (!new SignatureWrapper(AsymSignatureAlgorithms.ECDSA_SHA256, publicKey)
                     .update ((jws.getString(JOSE_PROTECTED) + "." + jws.getString(JOSE_PAYLOAD)).getBytes("UTF-8"))
                     .verify (jws.getBinary(JOSE_SIGNATURE))) {
            throw new IOException ("Verify");
        }
        jws.checkForUnread();
    }

    static JSONObjectWriter jws(JSONObjectWriter payload, 
                                JSONObjectWriter protectedHeader,
                                KeyStoreEnumerator signatureKey) throws Exception {
        JSONObjectWriter signature = new JSONObjectWriter()
            .setBinary(JOSE_PAYLOAD, payload.serializeToBytes(JSONOutputFormats.NORMALIZED))
            .setBinary(JOSE_PROTECTED, protectedHeader.serializeToBytes(JSONOutputFormats.NORMALIZED));
        JSONObjectReader reader = JSONParser.parse(signature.toString());
        signature.setBinary(JOSE_SIGNATURE,
                             new SignatureWrapper(AsymSignatureAlgorithms.ECDSA_SHA256,
                                                  signatureKey.getPrivateKey())
                                 .update((reader.getString(JOSE_PROTECTED) + "." + reader.getString(JOSE_PAYLOAD)).getBytes("UTF-8"))
                                 .sign());
        return signature;
    }
}
