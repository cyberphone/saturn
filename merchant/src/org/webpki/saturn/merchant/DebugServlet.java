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

import java.security.GeneralSecurityException;

import java.security.cert.X509Certificate;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.webpki.json.JSONArrayReader;
import org.webpki.json.JSONArrayWriter;
import org.webpki.json.JSONObjectReader;
import org.webpki.json.JSONObjectWriter;
import org.webpki.json.JSONOutputFormats;
import org.webpki.json.JSONSignatureDecoder;
import org.webpki.json.JSONTypes;
import org.webpki.json.JSONDecryptionDecoder;

import org.webpki.util.Base64URL;
import org.webpki.util.ISODateTime;

import org.webpki.saturn.common.BaseProperties;
import org.webpki.saturn.common.Messages;
import org.webpki.saturn.common.Version;
import org.webpki.saturn.common.ProtectedAccountData;
import org.webpki.saturn.common.PayerAccountTypes;
import org.webpki.saturn.common.AccountDescriptor;
import org.webpki.saturn.common.CardSpecificData;
import org.webpki.saturn.common.KnownExtensions;

class DebugPrintout implements BaseProperties {
    
    StringBuffer s = new StringBuffer( );

    Point point = new Point();

    boolean clean;
    DebugData debugData;

    static final String STATIC_BOX = "font-size:8pt;word-break:break-all;width:800pt;background:#F8F8F8;";
    static final String COMMON_BOX = "border-width:1px;border-style:solid;border-color:grey;padding:10pt;box-shadow:3pt 3pt 3pt #D0D0D0";
    
    String getShortenedB64(byte[] bin, int maxLength) throws IOException {
        String b64 = Base64URL.encode(bin);
        if (b64.length() > maxLength) {
            maxLength /= 2;
            b64 = b64.substring(0, maxLength) + "...." + b64.substring(b64.length() - maxLength);
        }
        return b64;
    }
    
    boolean rewrittenUrl(StringBuffer originalBuffer, String pattern, String rewritten) {
        String original = originalBuffer.toString();
        int i = original.indexOf(pattern);
        if (i < 0) return false;
        originalBuffer.delete(0, i + pattern.length() - 1);
        originalBuffer.insert(0, rewritten);
        return true;
    }
    
    void updateUrls(JSONObjectReader jsonTree, JSONObjectWriter rewriter, String target) throws IOException {
        if (jsonTree.hasProperty(target)) {
            StringBuffer value = new StringBuffer(jsonTree.getString(target));
            if (rewrittenUrl(value, "/webpay-payerbank/", "https://payments.mybank.com") ||
                rewrittenUrl(value, "/webpay-payeebank/", "https://payments.bigbank.com") ||
                rewrittenUrl(value, "/webpay-acquirer/", "https://https://cardprocessor.com")) {
                rewriter.setupForRewrite(target);
                rewriter.setString(target, value.toString());
            }
        }
    }

    void updateSpecific(JSONObjectReader jsonTree, JSONObjectWriter rewriter, String target, String value) throws IOException {
        if (jsonTree.hasProperty(target)) {
            rewriter.setupForRewrite(target);
            rewriter.setString(target, value);
        }
    }

    void cleanData(JSONObjectReader jsonTree) throws IOException, GeneralSecurityException {
        JSONObjectWriter rewriter = new JSONObjectWriter(jsonTree);
        for (String property : jsonTree.getProperties()) {
            if (jsonTree.getPropertyType(property) == JSONTypes.OBJECT) {
                cleanData(jsonTree.getObject(property));
            } else if (property.equals(JSONSignatureDecoder.CERTIFICATE_PATH_JSON)) {
                X509Certificate[] path = jsonTree.getCertificatePath();
                rewriter.setupForRewrite(JSONSignatureDecoder.CERTIFICATE_PATH_JSON);
                JSONArrayWriter arrayWriter = rewriter.setArray(JSONSignatureDecoder.CERTIFICATE_PATH_JSON);
                for (X509Certificate certificate : path) {
                    arrayWriter.setString(getShortenedB64(certificate.getEncoded(), 32));
                }
                if (jsonTree.hasProperty(JSONSignatureDecoder.VALUE_JSON)) {
                    byte[] value = jsonTree.getBinary(JSONSignatureDecoder.VALUE_JSON);
                    rewriter.setupForRewrite(JSONSignatureDecoder.VALUE_JSON);
                    rewriter.setString(JSONSignatureDecoder.VALUE_JSON, getShortenedB64(value, 64));
                }
            } else if (property.equals(JSONSignatureDecoder.VALUE_JSON)) {
                if (jsonTree.hasProperty(JSONSignatureDecoder.PUBLIC_KEY_JSON)) {
                    byte[] value = jsonTree.getBinary(JSONSignatureDecoder.VALUE_JSON);
                    rewriter.setupForRewrite(JSONSignatureDecoder.VALUE_JSON);
                    rewriter.setString(JSONSignatureDecoder.VALUE_JSON, getShortenedB64(value, 64));
                }
            } else if (property.equals(JSONDecryptionDecoder.CIPHER_TEXT_JSON)) {
                byte[] cipherText = jsonTree.getBinary(JSONDecryptionDecoder.CIPHER_TEXT_JSON);
                rewriter.setupForRewrite(JSONDecryptionDecoder.CIPHER_TEXT_JSON);
                rewriter.setString(JSONDecryptionDecoder.CIPHER_TEXT_JSON, getShortenedB64(cipherText, 64));
            } else if (property.equals(JSONSignatureDecoder.N_JSON)) {
                byte[] n = jsonTree.getBinary(JSONSignatureDecoder.N_JSON);
                rewriter.setupForRewrite(JSONSignatureDecoder.N_JSON);
                rewriter.setString(JSONSignatureDecoder.N_JSON, getShortenedB64(n, 64));
            } else if (jsonTree.getPropertyType(property) == JSONTypes.ARRAY) {
                JSONArrayReader array = jsonTree.getArray(property);
                while (array.hasMore()) {
                    if (array.getElementType() == JSONTypes.OBJECT) {
                        cleanData(array.getObject());
                    } else {
                        array.scanAway();
                    }
                }
            }
        }
        updateUrls(jsonTree, rewriter, KnownExtensions.HYBRID_PAYMENT);
        updateUrls(jsonTree, rewriter, RECEPIENT_URL_JSON);
        updateUrls(jsonTree, rewriter, AUTHORITY_URL_JSON);
        updateUrls(jsonTree, rewriter, SERVICE_URL_JSON);
        updateUrls(jsonTree, rewriter, EXTENDED_SERVICE_URL_JSON);
        updateUrls(jsonTree, rewriter, PROVIDER_AUTHORITY_URL_JSON);
        updateUrls(jsonTree, rewriter, ACQUIRER_AUTHORITY_URL_JSON);
        updateSpecific(jsonTree, rewriter, DOMAIN_NAME_JSON, "demomerchant.com");
        updateSpecific(jsonTree, rewriter, CLIENT_IP_ADDRESS_JSON, "220.13.198.144");
    }

    void fancyBox(JSONObjectReader reader) throws IOException, GeneralSecurityException {
        if (clean) {
            reader = reader.clone();
            cleanData(reader);
        }
        s.append("<div style=\"" + STATIC_BOX + COMMON_BOX + "\">" +
              reader.serializeToString(JSONOutputFormats.PRETTY_HTML) +
              "</div>");
    }

    void fancyBoxNoClean(JSONObjectReader reader) throws IOException, GeneralSecurityException {
        boolean temp = clean;
        clean = false;
        fancyBox(reader);
        clean = temp;
    }

    void fancyBox(JSONObjectWriter writer) throws IOException, GeneralSecurityException {
        fancyBox(new JSONObjectReader(writer));
    }

    void description(String string) {
        s.append("<div style=\"word-wrap:break-word;width:800pt;margin-bottom:10pt;margin-top:20pt\">" + string + "</div>");
    }

    void descriptionStdMargin(String string) {
        s.append("<div style=\"word-wrap:break-word;width:800pt;margin-bottom:10pt;margin-top:10pt\">" + string + "</div>");
    }

    class Point {
        int i;
        char j;
        private String out(String number) {
            return "<div id=\"" + number + "\" class=\"point\">" + number + "</div>";
        }
        public String toString() {
            j = 'a';
            return out(String.valueOf(++i));
        }
        public String sub() {
            return out(String.valueOf(i) + String.valueOf(j++));
        }
        
        public String var(boolean subFlag) {
            return subFlag ? sub() : toString();
        }
    }

    String keyWord(String keyWord) {
        return "<code style=\"font-size:10pt\">&quot;" + keyWord + "&quot;</code>";
    }

    String errorDescription(boolean bank) {
        return "The operation failed causing the <b>" +
               (bank? "Bank" : "Acquirer") +
               "</b> to return a standardized error code.&nbsp;&nbsp;This " +
                "response is <i>unsigned</i> since the associated request is assumed to be <i>rolled-back</i>:";
    }

    DebugPrintout(DebugData debugData, boolean clean) throws Exception {
        this.clean = clean;
        this.debugData = debugData;
        description("<p>The following page shows the messages " + (clean? "(<i>here slightly edited for brevity</i>) " : "") +
            "exchanged between a " +
            "<b>Merchant</b> (Payee), <b>Merchant&nbsp;Bank</b>, <b>Wallet</b> (Payer), and <b>User&nbsp;Bank</b> (Payment provider).&nbsp;&nbsp;" +
            "For traditional card payments there is also an <b>Acquirer</b> (aka &quot;card processor&quot;) involved. " +
            "The numbers shown in the different steps are supposed to match those of the " +
            "<a href=\"https://cyberphone.github.io/doc/saturn/saturn-v3-presentation.pdf\" target=\"_blank\">[SATURN]</a> presentation.</p>" +
            "<p>Saturn uses a JSON based message notation described in " +
            "<a target=\"_blank\" href=\"https://cyberphone.github.io/doc/web/yasmin.html\">[YASMIN]</a>.</p>" +
            "<p>Current mode: <i>" +
            (debugData.nativeMode ? "Saturn &quot;Native&quot; " +
            (debugData.acquirerMode ? "Card payment" : "Account-2-Account payment using " + (debugData.basicCredit ? "direct debit" : "reserve+finalize")) :
            (debugData.basicCredit ? "Bank-to-Bank Payment" + (debugData.hybridMode ? " + Hybrid" :"") : "Card Payment")) +
            "</i></p>");
        description(point +
            "<p>The user performs &quot;Checkout&quot; (after <i>optionally</i> selecting payment method), " +
            "causing the <b>Merchant</b> server returning a " +
            "<b>Wallet</b> invocation Web-page featuring a call to the " +
            keyWord("navigator.nativeConnect()") + 
            " <a target=\"_blank\" href=\"https://github.com/cyberphone/web2native-bridge#api\">[CONNECT]</a> browser API. " +
            "Then the invoking Web-page waits for a ready signal from the <b>Wallet</b>.</p>");

         description(point +
            "<p>When the ready signal has been received the <b>Merchant</b> Web-page sends a " +
            "list of accepted account types (aka payment instruments) and associated <i>signed</i> " + 
            "<a target=\"_blank\" href=\"https://cyberphone.github.io/doc/security/jcs.html\">[SIGNATURE]</a> " +
            keyWord(PAYMENT_REQUEST_JSON) + " objects to the <b>Wallet</b>:</p>");

        fancyBox(debugData.InvokeWallet);
        
        description(point.sub() +
            "<p>After an <i>optional</i> selection of account (card) in the <b>Wallet</b> UI, the user " +
            "authorizes the payment request (typically using a PIN):</p>" +
            "<img style=\"display:block;margin-left:auto;margin-right:auto;height:33%;width:33%\" src=\"" +
            (debugData.acquirerMode ? MerchantService.walletSupercardAuthz : MerchantService.walletBankdirectAuthz) + 
            "\">");
        description(point.sub() +
            "<p>The result of this process is not supposed be " +
            "directly available to the <b>Merchant</b> since it contains potentially sensitive user data.&nbsp;&nbsp;" +
            "For an example turn to <a href=\"#userauthz\">Unencrypted User Authorization</a>.</p>");
        description(point +
            "<p>Therefore the result is <i>encrypted</i> (using a key supplied by the <b>User&nbsp;Bank</b> as a part of the " +
            "payment credential) before it is returned to the <b>Merchant</b>:</p>");

        fancyBox(debugData.walletResponse);
        descriptionStdMargin("Note that " +
             keyWord(PROVIDER_AUTHORITY_URL_JSON) + " and " +
             keyWord(ACCOUNT_TYPE_JSON) + " are sent in clear as well (otherwise the <b>Merchant</b>" +
             " would not know what to do with the received data)."); 

        description(point.sub() +
            "<p>After receiving the <b>Wallet</b> response, the <b>Merchant</b> uses the supplied " +
             keyWord(PROVIDER_AUTHORITY_URL_JSON) + " to retrieve the associated " + keyWord(Messages.PROVIDER_AUTHORITY.toString()) +
             " object of the <b>User&nbsp;Bank</b> claimed to be the user's account holder for the selected card:</p>");

        fancyBox(debugData.providerAuthority);
        descriptionStdMargin(keyWord(Messages.PROVIDER_AUTHORITY.toString()) + 
            " is an object that typically would be <i>cached</i>.&nbsp;&nbsp;It " +
            "has the following tasks:<ul>" +
            "<li style=\"padding:0pt\">Provide credentials of an entity allowing relying parties verifying such before interacting with the entity.</li>" +
            "<li>Through a signature attest the authenticity of core parameters including <i>service end points</i>, <i>encryption keys</i>, " +
            "<i>supported payment methods</i>, <i>extensions</i>, and <i>algorithms</i>.</li></ul>");
        if (debugData.nativeMode) {
            nativeMode();
        } else {
            standardMode();
        }
        description("<p id=\"userauthz\" style=\"text-align:center;font-weight:bold;font-size:10pt;font-family:" + HTML.FONT_ARIAL + "\">Unencrypted User Authorization</p>" +
            "The following printout shows a sample of <i>internal</i> <b>Wallet</b> user authorization data <i>before</i> it is encrypted:");

        fancyBoxNoClean(MerchantService.userAuthzSample);
        descriptionStdMargin("Explanations:<p>" +
            keyWord(REQUEST_HASH_JSON) + " holds the hash of the " +
            keyWord(PAYMENT_REQUEST_JSON) + " object.</p><p>" +
            keyWord(DOMAIN_NAME_JSON) + " holds the DNS name of the <b>Merchant</b>.</p><p>" +
            keyWord(ACCOUNT_JSON) + " holds the user account information usually only known by <b>User&nbsp;Bank</b>" +
            " but is also handed over to the <i>provider</i> (of the <b>Merchant</b>) using the " +
            keyWord(ENCRYPTED_ACCOUNT_DATA_JSON) +
            " object.</p><p>" +
            keyWord(ENCRYPTION_PARAMETERS_JSON) + " holds session specific encryption parameters generated by the <b>Wallet</b>. " +
            "See <a href=\"#provuserresp\">Provider User Response</a> for more information.</p><p>" + 
            keyWord(JSONSignatureDecoder.SIGNATURE_JSON) + " holds the user's authorization signature.</p>");
        description("<p id=\"provuserresp\" style=\"text-align:center;font-weight:bold;font-size:10pt;font-family:" + HTML.FONT_ARIAL + "\">" +
            "Provider User Response</p>" +
            "In the case the <b>User&nbsp;Bank</b> requires additional authentication data from the user it will not " +
            "return an " +
            keyWord(Messages.AUTHORIZATION_RESPONSE.toString()) + " message, but the following:");
        fancyBoxNoClean(MerchantService.providerUserResponseSample);
        descriptionStdMargin("Note that the <b>Merchant</b> is supposed to transfer the " +
        keyWord(Messages.PROVIDER_USER_RESPONSE.toString()) +
        " to the already open <b>Wallet</b> and be prepared for receiving a renewed " +
        keyWord(Messages.PAYER_AUTHORIZATION.toString()) +
        " in order to maintain an unmodified " +
        keyWord(PAYMENT_REQUEST_JSON) +
        " needed for RBA synchronization.");
        description("The message featured in the " +
        keyWord(ENCRYPTED_MESSAGE_JSON) +
        " object is then decrypted using the " +
        keyWord(ENCRYPTION_PARAMETERS_JSON) +
        " the <b>Wallet</b> included in the <i>preceding</i> " +
        "user authentication object:");
        fancyBoxNoClean(MerchantService.encryptedMessageSample.getRoot());
        descriptionStdMargin("<p>Note that if there are no " +
        keyWord(USER_CHALLENGE_ITEMS_JSON) +
        " elements, there is only a text message to the user like &quot;Out of funds&quot; " +
        "and the payment process terminates.</p><p>" +
        "However, in the case above there is a " +
        keyWord(USER_CHALLENGE_ITEMS_JSON) +
        " list which must be handled by a specific RBA dialog:</p>");
        descriptionStdMargin("<div style=\"margin-left:auto;margin-right:auto;width:30em;background-color:#f0f0f0;border-width:1px;" +
        "border-style:solid;border-color:black;box-shadow:3pt 3pt 3pt #D0D0D0;\">" +
        "<style scoped>" +
        " .button {border-radius:3pt;background:linear-gradient(to bottom, #eaeaea 14%,#fcfcfc 52%,#e5e5e5 89%);width:6em;text-align:center;border-width:1px;border-style:solid;border-color:#a9a9a9;padding:2pt}" +
        "</style>" +
        "<div style=\"background-color:green;color:white;font-size:larger;text-align:center;padding:5pt\">Requester: " +
        MerchantService.encryptedMessageSample.getRequester() +        
        "</div><div style=\"margin:10pt 10pt 0pt 10pt\">" +
        MerchantService.encryptedMessageSample.getText() +
        "</div><div style=\"margin:5pt 10pt 0pt 10pt;width:" + 
        MerchantService.encryptedMessageSample.getOptionalUserChallengeItems()[0].getLength() +
        "em;background-color:white;border-width:1px;padding:1pt 0pt 2pt 4pt;border-style:solid;border-color:#a9a9a9;margin-top:3pt\">"+
        "\u25cf\u2009\u25cf\u2009\u25cf\u2009\u25cf\u2009\u25cf</div>" +
        "<table style=\"margin-left:auto;margin-right:auto;margin-top:12pt;margin-bottom:12pt\"><tr><td><div class=\"button\">Cancel</div></td><td style=\"width:4em\"></td><td><div class=\"button\">Submit!</div></tr></table>" +
        "</div>");
        description("When the user have issued the requested data the <b>Wallet</b> creates a new user authentication object which " +
        "now also contains a matching " +
        keyWord(USER_RESPONSE_ITEMS_JSON) +
        " list:");
        fancyBoxNoClean(MerchantService.userChallAuthzSample);
        descriptionStdMargin("<p>This object is returned to the <b>Merchant</b> in a " +
        keyWord(Messages.PAYER_AUTHORIZATION.toString()) + " message, effectively returning to <a href=\"#3\">step&nbsp;3</a>.</p><p>This process may be repeated " +
        "until <b>User&nbsp;Bank</b> is satisfied or blocks further attempts.</p>");

        description("Protocol version: <i>" + Version.PROTOCOL + "</i><br>Date: <i>" + Version.DATE + "</i>");
    }
    
    void standardMode() throws Exception {
        description(point + 
                    "<p>After receiving the " + keyWord(Messages.PROVIDER_AUTHORITY.toString()) +
                    " object including the " + keyWord(SERVICE_URL_JSON) + 
                    ", the <b>Merchant</b> creates and sends an " +
                    keyWord(Messages.AUTHORIZATION_REQUEST.toString()) + " object (comprising of " +
                    "the user's encrypted authorization and the merchant's associated " +
                    keyWord(PAYMENT_REQUEST_JSON) + "), to the <b>User&nbsp;Bank</b>:</p>");
        
        fancyBox(debugData.authorizationRequest);
        if (!debugData.acquirerMode) {
            descriptionStdMargin("Note the use of " + keyWord(PAYEE_ACCOUNT_JSON) + 
                    " which holds an object that is compatible with the " + 
                    keyWord(PROVIDER_ACCOUNT_TYPES_JSON) + " of the (by the <b>Merchant</b>) retrieved " + 
                    keyWord(Messages.PROVIDER_AUTHORITY.toString()) + " object.");
        }
        
        description(point.sub() + 
                "<p>After receiving the " + keyWord(Messages.AUTHORIZATION_REQUEST.toString()) +
                " object, the <b>User&nbsp;Bank</b> uses the enclosed " +
                keyWord(AUTHORITY_URL_JSON) + " to retrieve the <b>Merchant</b> " +
                keyWord(Messages.PAYEE_AUTHORITY.toString()) + " object:</p>");

        fancyBox(debugData.payeeAuthority);

        description(point.sub() + 
                "<p>After receiving the " + keyWord(Messages.PAYEE_AUTHORITY.toString()) +
                " object, the <b>User&nbsp;Bank</b> uses the enclosed " +
                keyWord(PROVIDER_AUTHORITY_URL_JSON) + " to retrieve the <b>Merchant</b> " +
                keyWord(Messages.PROVIDER_AUTHORITY.toString()) + " object:</p>");

        fancyBox(debugData.payeeProviderAuthority);
        description(point.sub() +
                "<p>Now the <b>User&nbsp;Bank</b> (equipped with the " +
                keyWord(Messages.PROVIDER_AUTHORITY.toString()) +
                " and " +
                keyWord(Messages.PAYEE_AUTHORITY.toString()) +
                " objects), can check the validity of the " +
                keyWord(Messages.AUTHORIZATION_REQUEST.toString()) +
                " including:</p><ul>" +
                "<li style=\"padding:0pt\">Verifying that the " +
                keyWord(RECEPIENT_URL_JSON) + " in the " +
                keyWord(Messages.AUTHORIZATION_REQUEST.toString()) + " matches the " +
                keyWord(SERVICE_URL_JSON) + " of the <b>User&nbsp;Bank</b>" +
                ".</li>" +
                "<li>Verifying that the <b>Merchant</b> is vouched for by a provider belonging to one for the <b>User&nbsp;Bank</b> " +
                "known trust network through " +
                keyWord(JSONSignatureDecoder.CERTIFICATE_PATH_JSON) +
                " in " +
                keyWord(Messages.PAYEE_AUTHORITY.toString()) +
                ".</li>" +
                "<li>Verifying that the " +
                keyWord(JSONSignatureDecoder.CERTIFICATE_PATH_JSON) + " in " +
                keyWord(Messages.PAYEE_AUTHORITY.toString()) +
                " and " +
                keyWord(Messages.PROVIDER_AUTHORITY.toString()) +
                " are identical.</li>" +
                "<li>Verifying that the " +
                keyWord(JSONSignatureDecoder.PUBLIC_KEY_JSON) + " in " +
                keyWord(Messages.PAYEE_AUTHORITY.toString()) +
                " and " +
                keyWord(Messages.AUTHORIZATION_REQUEST.toString()) +
                " are identical.</li>" +
                "<li>Verifying that the " +
                keyWord(JSONSignatureDecoder.PUBLIC_KEY_JSON) + " in " +
                keyWord(Messages.PAYEE_AUTHORITY.toString()) +
                " and " +
                keyWord(PAYMENT_REQUEST_JSON) +
                " are identical.</li>" +
                "<li>Verifying that decrypting " +
                keyWord(ENCRYPTED_AUTHORIZATION_JSON) +
                " returns a valid user authorization object including " +
                keyWord(JSONSignatureDecoder.SIGNATURE_JSON) +
                ".</li>" +
                "<li>Verifying that the " +
                keyWord(REQUEST_HASH_JSON) +
                " in the user authorization object matches the hash of the " +
                keyWord(PAYMENT_REQUEST_JSON) +
                " object.</li>" +
                "<li>Verifying that the " +
                keyWord(ACCOUNT_TYPE_JSON) + " in the " +
                keyWord(Messages.AUTHORIZATION_REQUEST.toString()) + " and the " +
                keyWord(TYPE_JSON) +
                " in the user authorization object are identical.</li>" +
                "<li>Verifying that the " +
                keyWord(TIME_STAMP_JSON) +
                " in the user authorization object is within limits like " +
                "<span style=\"white-space:nowrap\">-(<i>AllowedClientClockSkew</i> + <i>AuthorizationMaxAge</i>)" +
                " to <i>AllowedClientClockSkew</i></span> with respect to current time.</li>" +
                "<li>Verifying that the " +
                keyWord(JSONSignatureDecoder.PUBLIC_KEY_JSON) +
                " and " +
                keyWord(ID_JSON) +
                " in (" +
                keyWord(ACCOUNT_JSON) +
                ") in the user authorization object match a <b>User&nbsp;Bank</b> customer account.</li>" +
                "</ul>");
        description(point.sub() +
                "<p>If the user authorization object also holds RBA (Risk Based Authentication) data, " +
                "this is where such data should be validated.</p><p>" +
                "Note that the inclusion of RBA data means that a related previous " + 
                keyWord(Messages.AUTHORIZATION_REQUEST.toString()) +
                " did not result in an " +
                keyWord(Messages.AUTHORIZATION_RESPONSE.toString()) + " (indicating success), but in a " +
                keyWord(Messages.PROVIDER_USER_RESPONSE.toString()) + 
                " holding specific RBA request data. See <a href=\"#provuserresp\">Provider User Response</a> for more information.</p>");
        if (providerUserResponse(debugData.authorizationResponse)) {
            return;
        }
        
        if (debugData.acquirerMode) {
            acquirerEndPartStandardMode();
        } else {
            directEndPartStandardMode();
        }
        descriptionStdMargin("The payment authorization process was successful.");
    }
    
    private void authorizationResponse() throws IOException, GeneralSecurityException {
        JSONObjectWriter sampleAccountData = ProtectedAccountData.encode(
            new AccountDescriptor(debugData.acquirerMode ?
                    PayerAccountTypes.SUPER_CARD.getTypeUri() : PayerAccountTypes.BANK_DIRECT.getTypeUri(),
                                  debugData.acquirerMode ? "6875056745552109" : "8645-7800239403"),
            debugData.acquirerMode ?
                new CardSpecificData("Luke Skywalker", 
                                     ISODateTime.parseDateTime("2022-03-14T00:00:00Z"),
                                     "953") : null);
        description(point.sub() +
                "<p>After a <i>successful</i> preceding step, the <b>User&nbsp;Bank</b> wraps the " +
                keyWord(Messages.AUTHORIZATION_REQUEST.toString()) +
                " in a newly created " +
                keyWord(Messages.AUTHORIZATION_RESPONSE.toString()) +
                "object.</p><p>" +
                "Then a number of properties are added including " +
                keyWord(ENCRYPTED_ACCOUNT_DATA_JSON) + " which holds " +
                (debugData.acquirerMode ? "the encrypted PAN etc" : "an encrypted version of the <b>User</b> account which can be used for possible reversals") +
                ". Note that the encryption is performed using the " +
                keyWord(ENCRYPTION_PARAMETERS_JSON) + " of the <b>Merchant</b> " +
                keyWord(Messages.PROVIDER_AUTHORITY.toString()) + " object. " +
                "The following shows typical account data <i>before</i> encryption:</p>");
        fancyBox(sampleAccountData);
        description(point +
                "<p>Finally <b>User&nbsp;Bank</b> signs the completed object with " +
                "its private key and certificate.  The result is then returned to the <b>Merchant</b>" +
                " as a response to the " +
                keyWord(Messages.AUTHORIZATION_REQUEST.toString()) +
                ":</p>");
        fancyBox(debugData.authorizationResponse);
    }

    void cardOrHybridPayment(String recepient) throws IOException, GeneralSecurityException {
        description(point + 
            "<p>To finalize the transaction the <b>Merchant</b> wraps the " +
            keyWord(Messages.AUTHORIZATION_RESPONSE.toString()) +
            " in a newly created " +
            keyWord(Messages.CARD_PAYMENT_REQUEST.toString()) +
            " including a possibly updated " +
            keyWord(AMOUNT_JSON) +
            " and sends the completed object to the <b>" + recepient + 
            "</b>:</p>");
        fancyBox(debugData.cardPaymentRequest);
        if (debugData.hybridMode) {
            directTransfer(Messages.CARD_PAYMENT_REQUEST);
        } else {
            description(point + 
                    "<p>After successful validation of the " +
                    keyWord(Messages.CARD_PAYMENT_REQUEST.toString()) +
                    " the <b>Acquirer</b> performs a request to the associated card network.</p>");
        }
        description(point + 
            "<p>After successful processing of the payment request the <b>" + recepient + "</b> returns a matching response to the <b>Merchant</b>:</p>");
        fancyBox(debugData.cardPaymentResponse);
    }
    
    void reserveMode() {
        description(point.sub() +
                "<p>After validating the " +
                keyWord(Messages.AUTHORIZATION_REQUEST.toString()) +
                " and checking that the <b>User</b> actually has funds matching the request," +
                " the <b>User&nbsp;Bank</b> <i>reserves</i> the specified amount including a reference to the " +
                keyWord(PAYEE_JSON) + " and " +
                keyWord(REFERENCE_ID_JSON) + " of the " +
                keyWord(PAYMENT_REQUEST_JSON) +
                ".</p> ");
    }
    
    void acquirerEndPartStandardMode() throws IOException, GeneralSecurityException {
        reserveMode();
        authorizationResponse();
        cardOrHybridPayment("Acquirer");
    }
    
    void directTransfer(Messages message) {
        description(point +
                "<p>After validating the " +
                keyWord(message.toString()) +
                " and checking that the <b>User</b> actually has funds matching the request," +
                " the <b>User&nbsp;Bank</b> transfers money to the <b>Merchant</b> bank account given by " +
                keyWord(PAYEE_ACCOUNT_JSON) +
                " using a with both parties compatible payment scheme.</p>" +
                "<p>Note that the actual payment process may be fully <i>asynchronous</i> where the " +
                "authorization is only used for <i>initiation</i>.</p> ");
    }

    void directEndPartStandardMode() throws IOException, GeneralSecurityException {
        if (debugData.hybridMode) {
            reserveMode();
        } else {
            directTransfer(Messages.AUTHORIZATION_REQUEST);
        }
        authorizationResponse();
        if (debugData.hybridMode) {
            cardOrHybridPayment("User&nbspBank");
            descriptionStdMargin(
                "<p>Note that the actual payment process may be fully <i>asynchronous</i> where the " +
                "authorization is only used for <i>initiation</i>.</p>");
       }
    }

    boolean providerUserResponse(JSONObjectReader response) throws IOException, GeneralSecurityException {
        if (debugData.softReserveOrBasicError) {
            description(point + 
                "<p>The <b>User&nbsp;Bank</b> found some kind of account problem:</p>");
            fancyBox(response);
            descriptionStdMargin("<p>See <a href=\"#provuserresp\">Provider User Response</a> for more information.</p>" +
                "<p>Although the Saturn protocol may continue after this point the debug mode won't currently show that...</p>");
        }
        return debugData.softReserveOrBasicError;
    }

    void nativeMode() throws Exception {
        description(point +
            "<p>Now the <b>Merchant</b> creates a <i>signed</i> request and sends it to the " + keyWord(EXTENDED_SERVICE_URL_JSON) +
            " extracted from the " + keyWord(Messages.PROVIDER_AUTHORITY.toString()) + " object.&nbsp;&nbsp;" +
            "Since the <b>Wallet</b> response is encrypted, the <b>Merchant</b> needs to prove to the <b>User&nbsp;Bank</b> " +
            "that it knows the embedded " + keyWord(PAYMENT_REQUEST_JSON) + " which it does through the " + keyWord(REQUEST_HASH_JSON) +
            " construct and " + keyWord(REFERENCE_ID_JSON) + " which must match the hash of the request and property respectively" +
            (debugData.acquirerMode ? ".&nbsp;&nbsp;Since this particular session was a card transaction, a pre-configured " + 
            keyWord(ACQUIRER_AUTHORITY_URL_JSON) + " is also supplied" : "") + ":</p>");

        fancyBox(debugData.reserveOrBasicRequest);

        if (debugData.acquirerMode) {
            description(point +
                             "<p>In the <b>Acquirer</b> mode the received " + keyWord(ACQUIRER_AUTHORITY_URL_JSON) + " is used by the <b>User&nbsp;Bank</b> " +
                             "to retrieve the designated card processor's encryption keys:</p>");
            fancyBox(debugData.acquirerAuthority);
        }
        description("<p>After retrieving the <a href=\"#userauthz\">Unencrypted User Authorization</a>, " +
            "the called <b>User&nbsp;Bank</b> invokes the local payment backend (to verify the account, check funds, etc.) " +
            "<i>which is outside of this specification and implementation</i>.</p><p>" +
            point +
            "</p><p>" + (debugData.softReserveOrBasicError? errorDescription(true):
            "If the operation is successful, the <b>Bank</b> responds with a <i>signed</i> message containing both the original <b>Merchant</b> " +
            keyWord(PAYMENT_REQUEST_JSON) + " as well as a minimal set of user account data.</p>" +
            (debugData.acquirerMode ?
                 "Also note the inclusion of " +
                 keyWord(ENCRYPTED_ACCOUNT_DATA_JSON) + " which only the <b>Acquirer</b> can decrypt"
                                    :
                 "Also note the inclusion of the (by the <b>Bank</b>) selected <b>Merchant</b> receiver account (" +
                 keyWord(PAYEE_ACCOUNT_JSON) + ")") +
                     (debugData.basicCredit?
                             ".<p>This is the final interaction in the direct debit mode:</p>"
                                           :
                             ":")));

        fancyBox(debugData.reserveOrBasicResponse);

        if (!debugData.softReserveOrBasicError) {
            if (!debugData.basicCredit) {
                description(point +
                     "<p>For finalization of the payment, the <b>Merchant</b> sets an " + keyWord(AMOUNT_JSON) + 
                     " which must be <i>equal or lower</i> than in the original request, <i>counter-signs</i> the request, " +
                     "and sends it to the " + (debugData.acquirerMode ? keyWord(EXTENDED_SERVICE_URL_JSON) +
                     " retrievable from the <b>Acquirer</b> " + keyWord(Messages.PROVIDER_AUTHORITY.toString()) + " object:" :
                     "<b>Bank</b> again:</p>"));

                fancyBox(debugData.finalizeRequest);

                String finalDescription = null;
                if (debugData.acquirerMode) {
                    descriptionStdMargin("After receiving the request, the " +
                         keyWord(ENCRYPTED_ACCOUNT_DATA_JSON) + " object is <i>decrypted</i>.&nbsp;&nbsp;" +
                        "This mechanism effectively replaces a <b>Merchant</b>-based &quot;tokenization&quot; scheme with the added advantage "+
                        "that the <b>Acquirer</b> also can be included in a protection model by " +
                        "for example randomizing CCVs per request (&quot;upstreams tokenization&quot;).<p>" +
                        point +
                        "</p><p>The following printout " +
                        "shows a <i>sample</i> of protected account data:</p>");

     //               fancyBox(MerchantService.protectedAccountData);
                
                    finalDescription = "<p>After this step the card network is invoked <i>which is outside of this specification and implementation</i>.</p>";
                } else {
                    finalDescription = "<p>After receiving the request, the actual payment operation is performed " +
                        "<i>which is outside of this specification and implementation</i>.</p>";
                }
                descriptionStdMargin(finalDescription + 
                     point + "<p>" +
                    (debugData.softFinalizeError ? errorDescription(!debugData.acquirerMode) : 
                        "If the operation is successful, a <i>signed</i> hash of the request is returned:") +
                     "</p>");

                fancyBox(debugData.finalizeResponse);

                if (!debugData.softFinalizeError) {
                    descriptionStdMargin("Not embedding the request in the response may appear illogical but since<ul>" +
                        "<li>the communication is assumed to be <i>synchronous</i> (using HTTP).</li>" +
                        "<li>there is no additional information needed by the transaction, only a sender-unique " +
                        keyWord(REFERENCE_ID_JSON) +
                        ".</li><li>the signed 256-bit hash fully binds the response to the request.</li></ul>this would not add any security, " +
                        "assuming that logging is working.");
                }
            }
        }
    }

    public String toString() {
        return s.toString();
    }
}

public class DebugServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    
    static Logger logger = Logger.getLogger(DebugServlet.class.getName());


    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        try {
            DebugData debugData = null;
            HttpSession session = request.getSession(false);
            if (session == null ||
                (debugData = (DebugData)session.getAttribute(W2NBWalletServlet.DEBUG_DATA_SESSION_ATTR)) == null) {
                ErrorServlet.sessionTimeout(response);
                return;
            }
            boolean clean = request.getParameter("clean") != null;
            HTML.debugPage(response, new DebugPrintout(debugData, clean).toString(), clean);
            
         } catch (Exception e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            response.setContentType("text/plain; charset=utf-8");
            response.setHeader("Pragma", "No-Cache");
            response.setDateHeader("EXPIRES", 0);
            response.getOutputStream().println("Error: " + e.getMessage());
        }
    }
}
