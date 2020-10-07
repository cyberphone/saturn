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
import org.webpki.json.JSONCryptoHelper;
import org.webpki.json.JSONObjectReader;
import org.webpki.json.JSONObjectWriter;
import org.webpki.json.JSONOutputFormats;
import org.webpki.json.JSONTypes;

import org.webpki.util.Base64URL;
import org.webpki.util.ISODateTime;

import org.webpki.saturn.common.BaseProperties;
import org.webpki.saturn.common.Messages;
import org.webpki.saturn.common.NonDirectPaymentTypes;
import org.webpki.saturn.common.Version;
import org.webpki.saturn.common.KnownExtensions;

class DebugPrintout implements BaseProperties {
    
    StringBuilder s = new StringBuilder( );

    Point point = new Point();

    boolean clean;
    DebugData debugData;

    String encryptedAccount;

    static final String SUPERCARD_AUTHZ_SAMPLE       = "wallet-supercard-auth.png";
    static final String BANKDIRECT_AUTHZ_SAMPLE      = "wallet-bankdirect-auth.png";
    static final String GASSTATION_AUTHZ             = "wallet-gasstation-auth.png";

  
    static final String REFUND_TRANSACTION = "Refund&nbsp;Transaction";
    static final String PROV_USER_RESPONSE = "Provider&nbsp;User&nbsp;Response";
    static final String UNENCRYPTED_AUTHZ  = "Unencrypted&nbsp;User&nbsp;Authorization";

     
    String getShortenedB64(byte[] bin, int maxLength) throws IOException {
        String b64 = Base64URL.encode(bin);
        if (b64.length() > maxLength) {
            maxLength /= 2;
            b64 = b64.substring(0, maxLength) + "...." + b64.substring(b64.length() - maxLength);
        }
        return b64;
    }
    
    boolean rewrittenUrl(StringBuilder originalBuffer, String pattern, String rewritten) {
        String original = originalBuffer.toString();
        int i = original.indexOf(pattern);
        if (i < 0) return false;
        if (pattern.endsWith("/")) {
            i--;
        }
        originalBuffer.delete(0, i + pattern.length());
        originalBuffer.insert(0, rewritten);
        i = originalBuffer.toString().indexOf("spaceshop-logo.svg");
        if (i > 0) {
            originalBuffer.delete(i, i + 17);
            originalBuffer.insert(i, "logotype");
        }
        return true;
    }
    
    void updateUrls(JSONObjectReader jsonTree, JSONObjectWriter rewriter, String target) throws IOException {
        if (jsonTree.hasProperty(target)) {
            StringBuilder value = new StringBuilder(jsonTree.getString(target));
            if ((!target.equals(LOGOTYPE_URL_JSON) && 
                 rewrittenUrl(value, "/webpay-payerbank/", "https://payments.mybank.com")) ||
                rewrittenUrl(value, "/webpay-keyprovider", "https://enroll.mybank.com") ||
                (!target.equals(LOGOTYPE_URL_JSON) && 
                 rewrittenUrl(value, "/webpay-payeebank/", "https://payments.bigbank.com")) ||
                rewrittenUrl(value, "/webpay-acquirer/", "https://secure.cardprocessor.com") ||
                rewrittenUrl(value, "/webpay-payerbank", "https://mybank.com") ||
                rewrittenUrl(value, "/webpay-payeebank", "https://bigbank.com") ||
                rewrittenUrl(value, "/webpay-acquirer", "https://cardprocessor.com") ||
                rewrittenUrl(value, "/webpay-merchant", "https://spaceshop.com")) {
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
            } else if (property.equals(JSONCryptoHelper.CERTIFICATE_PATH_JSON)) {
                X509Certificate[] path = jsonTree.getCertificatePath();
                rewriter.setupForRewrite(JSONCryptoHelper.CERTIFICATE_PATH_JSON);
                JSONArrayWriter arrayWriter = rewriter.setArray(JSONCryptoHelper.CERTIFICATE_PATH_JSON);
                for (X509Certificate certificate : path) {
                    arrayWriter.setString(getShortenedB64(certificate.getEncoded(), 32));
                }
                if (jsonTree.hasProperty(JSONCryptoHelper.VALUE_JSON)) {
                    byte[] value = jsonTree.getBinary(JSONCryptoHelper.VALUE_JSON);
                    rewriter.setupForRewrite(JSONCryptoHelper.VALUE_JSON);
                    rewriter.setString(JSONCryptoHelper.VALUE_JSON, getShortenedB64(value, 64));
                }
            } else if (property.equals(JSONCryptoHelper.VALUE_JSON)) {
                if (jsonTree.hasProperty(JSONCryptoHelper.PUBLIC_KEY_JSON)) {
                    byte[] value = jsonTree.getBinary(JSONCryptoHelper.VALUE_JSON);
                    rewriter.setupForRewrite(JSONCryptoHelper.VALUE_JSON);
                    rewriter.setString(JSONCryptoHelper.VALUE_JSON, getShortenedB64(value, 64));
                }
            } else if (property.equals(JSONCryptoHelper.CIPHER_TEXT_JSON)) {
                byte[] cipherText = jsonTree.getBinary(JSONCryptoHelper.CIPHER_TEXT_JSON);
                rewriter.setupForRewrite(JSONCryptoHelper.CIPHER_TEXT_JSON);
                rewriter.setString(JSONCryptoHelper.CIPHER_TEXT_JSON, getShortenedB64(cipherText, 64));
            } else if (property.equals(JSONCryptoHelper.N_JSON)) {
                byte[] n = jsonTree.getBinary(JSONCryptoHelper.N_JSON);
                rewriter.setupForRewrite(JSONCryptoHelper.N_JSON);
                rewriter.setString(JSONCryptoHelper.N_JSON, getShortenedB64(n, 64));
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
        updateUrls(jsonTree, rewriter, KnownExtensions.REFUND_REQUEST);
        updateUrls(jsonTree, rewriter, KnownExtensions.HYBRID_PAYMENT);
        updateUrls(jsonTree, rewriter, KnownExtensions.BALANCE_REQUEST);
        updateUrls(jsonTree, rewriter, NO_MATCHING_METHODS_URL_JSON);
        updateUrls(jsonTree, rewriter, HOME_PAGE_JSON);
        updateUrls(jsonTree, rewriter, RECIPIENT_URL_JSON);
        updateUrls(jsonTree, rewriter, SERVICE_URL_JSON);
        updateUrls(jsonTree, rewriter, PROVIDER_AUTHORITY_URL_JSON);
        updateUrls(jsonTree, rewriter, LOGOTYPE_URL_JSON);
        updateUrls(jsonTree, rewriter, PAYEE_AUTHORITY_URL_JSON);
        updateUrls(jsonTree, rewriter, RECEIPT_URL_JSON);
        updateSpecific(jsonTree, rewriter, PAYEE_HOST_JSON, "spaceshop.com");
        updateSpecific(jsonTree, rewriter, CLIENT_IP_ADDRESS_JSON, "220.13.198.144");
    }

    void fancyBox(JSONObjectReader reader) throws IOException, GeneralSecurityException {
        if (clean) {
            reader = reader.clone();
            cleanData(reader);
        }
        String html = reader.serializeToString(JSONOutputFormats.PRETTY_HTML);
        if (html.endsWith("<br>")) {
            html = html.substring(0, html.length() - 4);
        }
        s.append("<div class=\"jsonbox\">")
         .append(html)
         .append("</div>");
    }

    void fancyBox(JSONObjectWriter writer) throws IOException, GeneralSecurityException {
        fancyBox(new JSONObjectReader(writer));
    }

    void description(String string) {
        s.append("<div class=\"dbgdesc\">" + string + "</div>");
    }

    void descriptionStdMargin(String string) {
        s.append("<div class=\"dbgdesc\" style=\"margin-top:10pt\">" + string + "</div>");
    }

    class Point {
        int i;
        char j;
        private String out(String number) {
            return "<div id=\"" + number + "\" class=\"point\">" + number + "</div>";
        }
        public String toString() {
            j = 'a' - 1;
            return out(String.valueOf(++i));
        }
        public String sub() {
            return out(String.valueOf(i) + String.valueOf(++j));
        }
        
        public String ref(boolean subFlag) {
            return "#" + String.valueOf(i) + (subFlag ? String.valueOf(j) : "");
        }
    }

    String keyWord(String keyWord) {
        return "<code>&quot;" + keyWord + "&quot;</code>";
    }
    
    String keyWord(Messages message) {
        return keyWord(message.toString());
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
            (debugData.basicCredit ? debugData.hybridMode ? "Hybrid Payment" :"Bank-to-Bank Payment" : "Card Payment") +
            (debugData.refundRequest == null ? "" : " + Refund") + 
            "</i></p>");
        description(point +
            "<p>The user performs &quot;Checkout&quot; (after <i>optionally</i> selecting payment method), " +
            "causing the <b>Merchant</b> server returning a currently <i>platform dependent</i> " +
            "<b>Wallet</b> invocation Web-page. " +
            "Then the invoking Web-page waits for a ready signal from the <b>Wallet</b>.</p>");

         description(point +
            "<p>When the ready signal has been received the <b>Merchant</b> sends a " +
            "list of accepted payment methods and a " +
            keyWord(PAYMENT_REQUEST_JSON) + " object to the <b>Wallet</b>:</p>");

        if (clean && debugData.InvokeWallet.hasProperty(NO_MATCHING_METHODS_URL_JSON)) {
            debugData.InvokeWallet.removeProperty(NO_MATCHING_METHODS_URL_JSON);
        }
        fancyBox(debugData.InvokeWallet);
        descriptionStdMargin("Note that payment networks would normally host their own " +
            "<b>Merchant</b> authority objects (and associated keys), " +
            "which is why there is a " + keyWord(PAYEE_AUTHORITY_URL_JSON) + " for each " + 
            keyWord(PAYMENT_METHOD_JSON) + ".");
        if (debugData.gasStation) {
            descriptionStdMargin("<p>Note that there is a property " +
                keyWord(NON_DIRECT_PAYMENT_JSON) + 
                " having the " + keyWord(TYPE_JSON) +
                " attribute set to " +
                keyWord(NonDirectPaymentTypes.RESERVATION.toString()) +
                " which means that there is a <i>reservation phase</i> involving the user, " +
                "followed by an actual payment operation for a usually considerably lower " +
                keyWord(AMOUNT_JSON) + ". This mode should preferably be indicated " +
                "in the <b>Wallet</b> user interface. " +
                "See <a target=\"_blank\" href=\"https://cyberphone.github.io/doc/saturn/ui-demo\">[UIDEMO]</a>.</p>");
        }
        
        description(point.sub() +
            "<p>After an optional selection of account (card) in the <b>Wallet</b> UI " +
            "(accomplished through &quot;swiping&quot; card logotypes in the " +
            "reference application), the user " +
            "<i>authorizes</i> the payment request, using a PIN or biometric operation:</p>" +
            "<img style=\"display:block;margin-left:auto;margin-right:auto;margin-bottom:10pt;max-width:250pt;" +
            "border-width:1px;border-style:solid;border-color:grey;box-shadow:3pt 3pt 3pt #d0d0d0\" " +
            "src=\"https://cyberphone.github.io/doc/saturn/" +
            (debugData.gasStation ? GASSTATION_AUTHZ :
                debugData.acquirerMode ? SUPERCARD_AUTHZ_SAMPLE : BANKDIRECT_AUTHZ_SAMPLE) + 
            "\">" +
            "The &quot;Balance&quot; field could also function as a touch button " +
            "for showing a list of recent transactions for the selected virtual card account." +
            "<p>The &quot;Payee&quot; field could also function as a touch button to trigger " +
            "a <b>Merchant</b> lookup service by using the " + keyWord(PAYEE_AUTHORITY_URL_JSON) + 
            " associated with the selected virtual card.</p>");
        description(point.sub() +
            "<p>The result of this process is not supposed be " +
            "directly available to the <b>Merchant</b> since it contains potentially sensitive user data.&nbsp;&nbsp;" +
            "For an example turn to <a href=\"#userauthz\">" +
            UNENCRYPTED_AUTHZ + "</a>.</p>");
        description(point +
            "<p>Therefore the result is <i>encrypted</i> (using a key supplied by the <b>User&nbsp;Bank</b> as a part of the " +
            "payment credential) before it is returned to the <b>Merchant</b>:</p>");

        fancyBox(debugData.walletResponse);
        descriptionStdMargin("For details on the encryption scheme, see <a target=\"_blank\" href=\"https://cyberphone.github.io/doc/security/jef.html\">[ENCRYPTION]</a>." +
             " Note that " +
             keyWord(PROVIDER_AUTHORITY_URL_JSON) + " and " +
             keyWord(PAYMENT_METHOD_JSON) + " are sent in clear as well (otherwise the <b>Merchant</b>" +
             " would not know what to do with the received data). " +
             "To maintain privacy, the issuer specific encryption public key is preferably shared by many (&gt; 100000) users. " +
             "<p>Note that public key data (" + keyWord(JSONCryptoHelper.PUBLIC_KEY_JSON) + 
             ") may equally well be represented by a " + keyWord(JSONCryptoHelper.KEY_ID_JSON) + ".</p>"); 

        description(point.sub() +
            "<p>After receiving the <b>Wallet</b> response, the <b>Merchant</b> uses the supplied " +
            keyWord(PROVIDER_AUTHORITY_URL_JSON) + " to retrieve the associated " +
            keyWord(Messages.PROVIDER_AUTHORITY) +
            " object of the <b>User&nbsp;Bank</b> claimed to be the user's account holder for the selected card:</p>");

        fancyBox(debugData.providerAuthority);
        providerAuthDescription();

        standardMode();

        description("<p id=\"userauthz\" style=\"text-align:center;font-weight:bold;font-size:10pt;font-family:" +
            HTML.FONT_ARIAL + "\">" + UNENCRYPTED_AUTHZ + "</p>" +
            "The following printout shows a sample of <i>internal</i> <b>Wallet</b> user authorization data <i>before</i> it is encrypted:");

        fancyBox(DebugData.userAuthzSample);
        descriptionStdMargin("Explanations:<p>" +
            keyWord(REQUEST_HASH_JSON) + " holds a by the <b>Wallet</b> calculated hash of the " +
            keyWord(PAYMENT_REQUEST_JSON) + " object.</p><p>" +
            keyWord(PAYEE_AUTHORITY_URL_JSON) + " binds a <i>declared</i> " +
            "<b>Merchant</b> authority object (holding keys) to an anticipated " + 
            keyWord(Messages.AUTHORIZATION_REQUEST) + 
            " message.  See also " + keyWord(Messages.PAYMENT_CLIENT_REQUEST) + ".</p><p>" +
            keyWord(PAYEE_HOST_JSON) + 
            " holds the host name of the <b>Merchant</b> as recorded by the <b>Wallet</b>.</p><p>" +
            keyWord(PAYMENT_METHOD_JSON) + 
            " holds the payment method associated with the selected virtual card.</p><p>" +
            keyWord(CREDENTIAL_ID_JSON) + 
            " holds a serial number or similar unique identifier associated with the selected virtual card.</p><p>" +
            keyWord(ACCOUNT_ID_JSON) + 
            " holds an account identifier associated with the selected virtual card. " +
            "See also <a href=\"" + encryptedAccount + "\">Encrypted Account Data</a>.</p><p>" +
            keyWord(USER_AUTHORIZATION_METHOD_JSON) + 
            " holds the method used for authorizing the user like PIN, Fingerprint, etc.</p><p>" + 
            keyWord(ENCRYPTION_PARAMETERS_JSON) + 
            " holds session specific encryption parameters generated by the <b>Wallet</b>. " +
            "See <a href=\"#provuserresp\">" + PROV_USER_RESPONSE + "</a> for more information.</p><p>" + 
            keyWord(TIME_STAMP_JSON) + 
            " holds the date and time for the authorization event in RFC&nbsp;3339 " +
            "(ISO) notation inclding local time offset.</p><p>" + 
            keyWord(SOFTWARE_JSON) + " holds the name and version of the <b>Wallet</b> software.</p><p>" + 
            keyWord(PLATFORM_JSON) + " holds the name and version of the platform software as well " +
            "as the hardware vendor suppying it.</p><p>" + 
            keyWord(AUTHORIZATION_SIGNATURE_JSON) + " holds the user's authorization signature. " +
            "Note that " + keyWord(JSONCryptoHelper.PUBLIC_KEY_JSON) + " <i>may "  +
            "be omitted</i> if " + keyWord(CREDENTIAL_ID_JSON) + " is sufficient for locating the proper " +
            "signature key.</p><p>" +
            "Note that the algorithms to use are stored in the selected virtual card. " +
            "That is, algorithms are exclusively defined by the <i>issuer</i>, although they must " +
            "(of course) be within the limits of the <b>Wallet</b> software.</p>");
        description("<p id=\"provuserresp\" style=\"text-align:center;font-weight:bold;font-size:10pt;font-family:" + 
            HTML.FONT_ARIAL + "\">" +
            PROV_USER_RESPONSE + "</p>" +
            "In the case the <b>User&nbsp;Bank</b> requires additional authentication data from the user it will not " +
            "return an " +
            keyWord(Messages.AUTHORIZATION_RESPONSE) + " message, but the following:");
        fancyBox(DebugData.providerUserResponseSample);
        descriptionStdMargin("Note that the <b>Merchant</b> is supposed to transfer the " +
            keyWord(Messages.PROVIDER_USER_RESPONSE) +
            " to the already open <b>Wallet</b> and be prepared for receiving a renewed " +
            keyWord(Messages.PAYER_AUTHORIZATION) +
            " in order to maintain an unmodified " +
            keyWord(PAYMENT_REQUEST_JSON) +
            " needed for RBA synchronization.");
            description("The message featured in the " +
            keyWord(ENCRYPTED_MESSAGE_JSON) +
            " object is then decrypted using the " +
            keyWord(ENCRYPTION_PARAMETERS_JSON) +
            " the <b>Wallet</b> included in the <i>preceding</i> " +
            "user authentication object:");
        fancyBox(DebugData.encryptedMessageSample.getRoot());
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
            DebugData.encryptedMessageSample.getRequester() +        
            "</div><div style=\"margin:10pt 10pt 0pt 10pt\">" +
            DebugData.encryptedMessageSample.getText() +
            "</div><div style=\"margin:5pt 10pt 0pt 10pt;width:20em;background-color:white;" +
            "border-width:1px;padding:1pt 0pt 2pt 4pt;border-style:solid;border-color:#a9a9a9;margin-top:3pt\">"+
            "&#x25cf;&#x2009;&#x25cf;&#x2009;&#x25cf;&#x2009;&#x25cf;&#x2009;&#x25cf;</div>" +
            "<table style=\"margin-left:auto;margin-right:auto;margin-top:12pt;margin-bottom:12pt\">" +
            "<tr><td><div class=\"button\">Cancel</div></td><td style=\"width:4em\"></td><td><div class=\"button\">Submit!</div></tr></table>" +
            "</div>");
        description("When the user have issued the requested data the <b>Wallet</b> creates a new user authentication object which " +
            "now also contains a matching " +
            keyWord(USER_RESPONSE_ITEMS_JSON) +
            " list:");
        fancyBox(DebugData.userChallAuthzSample);
        descriptionStdMargin("<p>This object is returned to the <b>Merchant</b> in a " +
            keyWord(Messages.PAYER_AUTHORIZATION) + " message, effectively resuming operation at <a href=\"#3\">step&nbsp;3</a>.</p><p>This process may be repeated " +
            "until <b>User&nbsp;Bank</b> is satisfied or blocks further attempts.</p>");

        description("Protocol version: <i>" + Version.PROTOCOL + "</i><br>Date: <i>" + Version.DATE + "</i>");
    }
    
    void providerAuthDescription() {
        descriptionStdMargin(keyWord(Messages.PROVIDER_AUTHORITY) + 
                " is an object that normally would be <i>cached</i> until it has expired.&nbsp;&nbsp;It " +
                "has the following tasks:<ul>" +
                "<li style=\"padding:0pt\">Enabling other parties discovering data about an entity <i>before</i> interacting with the entity.</li>" +
                "<li>Through a signature attesting the authenticity of core parameters including <i>service end points</i>, <i>encryption keys</i>, " +
                "<i>supported payment methods</i>, <i>extensions</i>, and <i>algorithms</i>.</li></ul>");
    }

    void standardMode() throws Exception {
        description(point + 
                    "<p>After receiving the " + keyWord(Messages.PROVIDER_AUTHORITY) +
                    " object including the " + keyWord(SERVICE_URL_JSON) + 
                    ", the <b>Merchant</b> creates and sends a <i>counter signed</i> " +
                    keyWord(Messages.AUTHORIZATION_REQUEST) + " object (comprising of " +
                    "the user's encrypted authorization and the merchant's associated " +
                    keyWord(PAYMENT_REQUEST_JSON) + "), to the <b>User&nbsp;Bank</b>:</p>");
        
        fancyBox(debugData.authorizationRequest);
            descriptionStdMargin("Note the use of " + keyWord(PAYEE_RECEIVE_ACCOUNT_JSON) + 
                    " which holds data needed for the actual payment method.");
        
        description(point.sub() + 
                "<p>After receiving the " + keyWord(Messages.AUTHORIZATION_REQUEST) +
                " object, the <b>User&nbsp;Bank</b> uses the enclosed " +
                keyWord(PAYEE_AUTHORITY_URL_JSON) + " to retrieve the <b>Merchant</b> " +
                keyWord(Messages.PAYEE_AUTHORITY) + " object:</p>");

        fancyBox(debugData.payeeAuthority);
        descriptionStdMargin(keyWord(Messages.PAYEE_AUTHORITY) + 
                " is an object that normally would be <i>cached</i> until it has expired.&nbsp;&nbsp;It " +
                "has the following tasks:<ul>" +
                "<li style=\"padding:0pt\">Enabling other parties discovering data about an entity <i>before</i> interacting with the entity.</li>" +
                "<li>Through an associated <i>provider's</i> signature attesting the authenticity of core parameters including <i>identity</i> and <i>signature keys</i>" +
                ".</li></ul>");

        description(point.sub() + 
                "<p>After receiving the " + keyWord(Messages.PAYEE_AUTHORITY) +
                " object, the <b>User&nbsp;Bank</b> uses the enclosed " +
                keyWord(PROVIDER_AUTHORITY_URL_JSON) + " to retrieve the <b>Merchant</b> " +
                keyWord(Messages.PROVIDER_AUTHORITY) + " object:</p>");

        fancyBox(debugData.payeeProviderAuthority);
        providerAuthDescription();
        description(point.sub() +
                "<p>Now the <b>User&nbsp;Bank</b> (equipped with the " +
                keyWord(Messages.PROVIDER_AUTHORITY) +
                " and " +
                keyWord(Messages.PAYEE_AUTHORITY) +
                " objects), must check the validity of the " +
                keyWord(Messages.AUTHORIZATION_REQUEST) +
                " using the following steps:</p><ul>" +
                "<li style=\"padding:0pt\">Verify that the " +
                keyWord(RECIPIENT_URL_JSON) + " in the " +
                keyWord(Messages.AUTHORIZATION_REQUEST) + " object matches the " +
                keyWord(SERVICE_URL_JSON) + " of the <b>User&nbsp;Bank</b>" +
                ".</li>" +
                "<li>Verify that the " +
                keyWord(JSONCryptoHelper.PUBLIC_KEY_JSON) + " in the " +
                keyWord(ISSUER_SIGNATURE_JSON) +
                " object of the " +
                keyWord(Messages.PAYEE_AUTHORITY) +
                " object and the public key of the first (=signature) certificate in the " +
                keyWord(JSONCryptoHelper.CERTIFICATE_PATH_JSON) +
                " of the " +
                keyWord(Messages.PROVIDER_AUTHORITY) +
                " object are identical.</li>" +
                "<li>Verify that the <b>Merchant</b> is vouched for by a provider belonging to one for the <b>User&nbsp;Bank</b> " +
                "known trust network through the " +
                keyWord(JSONCryptoHelper.CERTIFICATE_PATH_JSON) +
                " in the " +
                keyWord(Messages.PROVIDER_AUTHORITY) +
                " object.</li>" +
                "<li>Verify that the " +
                keyWord(JSONCryptoHelper.PUBLIC_KEY_JSON) +
                " and " +
                keyWord(JSONCryptoHelper.ALGORITHM_JSON) +
                " in the " +
                keyWord(Messages.AUTHORIZATION_REQUEST) +
                " object matches one of the " +
                keyWord(SIGNATURE_PARAMETERS_JSON) +
                " objects in the " +
                keyWord(Messages.PAYEE_AUTHORITY) +
                " object.</li>" +
                "<li>Verify that the " +
                keyWord(PAYEE_RECEIVE_ACCOUNT_JSON) +
                " object is decodable and applicable to the operation in progress.</li>" +
                "<li>Verify that one of the elements in the <i>optional</i> " +
                keyWord(ACCOUNT_VERIFIER_JSON) + " list of the " +
                keyWord(Messages.PAYEE_AUTHORITY) +
                " object matches the hash of the account provided in the " +
                keyWord(PAYEE_RECEIVE_ACCOUNT_JSON) +
                " object.</li>" +
                "</ul>&nbsp;<br>" +
                "After verifying the <b>Merchant</b>'s request data, turn to the <b>User</b>'s authorization:" +
                "<ul>" +
                "<li>Verify that decrypting " +
                keyWord(ENCRYPTED_AUTHORIZATION_JSON) +
                " returns a valid user authorization object including an " +
                keyWord(AUTHORIZATION_SIGNATURE_JSON) +
                " object.</li>" +
                "<li>Verify that the " +
                keyWord(CREDENTIAL_ID_JSON) +
                " points to valid credential.</li>" +
                "<li>Verify that the " +
                keyWord(JSONCryptoHelper.PUBLIC_KEY_JSON) +
                " and " +
                keyWord(ACCOUNT_ID_JSON) +
                " match a <b>User&nbsp;Bank</b> customer account.</li>" +
                "<li>Verify that the " +
                keyWord(REQUEST_HASH_JSON) +
                " in the user authorization object matches the hash of the " +
                keyWord(PAYMENT_REQUEST_JSON) +
                " object.</li>" +
                "<li>Verify that the " +
                keyWord(PAYEE_AUTHORITY_URL_JSON) +
                " matches the copy in the " +
                keyWord(Messages.AUTHORIZATION_REQUEST) +
                " object. Although this this duplication of data indeed is " +
                "<i>technically redundant</i>, it was added " +
                "to enable filtering out &quot;bad&quot; merchants <i>before</i> taking on " +
                "user authorization.</li>" +
                "<li>Verify that the " +
                keyWord(PAYMENT_METHOD_JSON) + " in the " +
                keyWord(Messages.AUTHORIZATION_REQUEST) +
                " object and in the user authorization object are identical.</li>" +
                "<li>Verify that the " +
                keyWord(TIME_STAMP_JSON) +
                " in the user authorization object is within limits like " +
                "<span style=\"white-space:nowrap\">-<i>AllowedClientClockSkew</i>" +
                " to (<i>AllowedClientClockSkew</i> + <i>AuthorizationMaxAge</i>)</span> with respect to current time.</li>" +
                "</ul>");
        description(point.sub() +
                "<p>If the user authorization object also holds RBA (Risk Based Authentication) data, " +
                "this is where such data should be validated.</p><p>" +
                "Note that the inclusion of RBA data means that a related previous " + 
                keyWord(Messages.AUTHORIZATION_REQUEST) +
                " did not result in an " +
                keyWord(Messages.AUTHORIZATION_RESPONSE) + " (indicating success), but in a " +
                keyWord(Messages.PROVIDER_USER_RESPONSE) + 
                " holding specific RBA request data. See <a href=\"#provuserresp\">" +
                PROV_USER_RESPONSE + "</a> for more information.</p>");
        if (providerUserResponse(debugData.authorizationResponse)) {
            return;
        }
        
        if (debugData.acquirerMode) {
            acquirerEndPartStandardMode();
        } else {
            directEndPartStandardMode();
        }
        descriptionStdMargin("The payment authorization process was successful.");
        if (debugData.refundRequest != null) {
            refundMode();
        }
    }
    
    void refundMode() throws IOException, GeneralSecurityException {
        description("<p id=\"refund\" style=\"text-align:center;font-weight:bold;font-size:10pt;font-family:" + 
            HTML.FONT_ARIAL + "\">" + REFUND_TRANSACTION +
            "</p>Refund in Saturn is initiated by the <b>Merchant</b>. " +
            "Note that the originator of a refund request is supposed to " +
            "be <i>authenticated</i> by the <b>Merchant</b> system before being submitted." +
            "<p>A refund message is created by embedding the originating" +
            keyWord(Messages.AUTHORIZATION_RESPONSE) +
            " in a " +
            keyWord(Messages.REFUND_REQUEST) +
            " object and sending the completed and <i>signed</i> result to " +
            "the <i>merchant's</i> payment provider:</p>");
        fancyBox(debugData.refundRequest);
        descriptionStdMargin("See also <a href=\"" + encryptedAccount + "\">Encrypted Account Data</a>. " +
            "Note that " +
            keyWord(RECIPIENT_URL_JSON) +
            " for the refund operation is derived from the " +
            keyWord(EXTENSIONS_JSON) +
            " object of the " +
            keyWord(Messages.PROVIDER_AUTHORITY) +
            " of the <b>Merchant</b>.");
        description("The actual transfer is using existing payment schemes.  If successful a " +
            keyWord(Messages.REFUND_RESPONSE) +
            " message is returned:");
        fancyBox(debugData.refundResponse);
        descriptionStdMargin("Note the difference between authorization and refunding " +
            "with respect to the payment party carrying out the operation!");
    }

    private void authorizationResponse() throws IOException, GeneralSecurityException {
        JSONObjectWriter sampleAccountData = (
            debugData.acquirerMode ?
                new com.supercard.SupercardAccountDataEncoder("4532562005001506",
                                                              "Luke Skywalker", 
                                                              ISODateTime.parseDateTime("2024-03-14T00:00:00Z",
                                                                                        ISODateTime.COMPLETE))
                                   :
                new org.payments.sepa.SEPAAccountDataEncoder("FR7630002111110020050015158")).writeObject();
        description(point.sub() +
                "<p>After a <i>successful</i> preceding step, the <b>User&nbsp;Bank</b> creates an empty " +
                keyWord(Messages.AUTHORIZATION_RESPONSE) +
                " object.</p>");
        description(point.sub() +
                "<p>Then a number of properties are added including " +
                keyWord(ENCRYPTED_ACCOUNT_DATA_JSON) + " which holds " +
                (debugData.acquirerMode ? "the encrypted PAN etc" :
                    "an encrypted version of the <b>User</b> account which can be used for possible <i>refunds</i>") +
                (debugData.refundRequest == null ? "." : " (see <a href=\"#refund\">" + REFUND_TRANSACTION +
                     "</a> for more information).") +
                " Encryption is performed using the " +
                keyWord(ENCRYPTION_PARAMETERS_JSON) + " of the payment " +
                keyWord(Messages.PROVIDER_AUTHORITY) + " associated with the <b>Merchant</b>. " +
                "The following shows typical account data <i>before</i> encryption:</p>");
        encryptedAccount = point.ref(true);
        fancyBox(sampleAccountData);
        if (!debugData.acquirerMode) {
            descriptionStdMargin("<p>Note that encrypted account data always points to a <i>real</i> account " +
                 "which may differ from the card like (&quot;virtual&quot;) account used by the customer.</p>");
        }
        descriptionStdMargin("<p>The <i>optional</i> " +
               keyWord(ACCOUNT_REFERENCE_JSON) +
               " property holds a short version of the used payment account which can be featured in receipts etc.</p>");
        description(point.sub() +
                "<p>The last element to be added is the original " +
                keyWord(Messages.AUTHORIZATION_REQUEST) +
                " object.</p>");
        description(point +
                "<p>Finally <b>User&nbsp;Bank</b> <i>counter-signs</i> the completed object with " +
                "its private key and certificate.  The result is then returned to the <b>Merchant</b>" +
                " as a response to the " +
                keyWord(Messages.AUTHORIZATION_REQUEST) +
                ":</p>");
        fancyBox(debugData.authorizationResponse);
    }

    void cardOrHybridPayment(String recipient) throws IOException, GeneralSecurityException {
        description(point + 
            "<p>To finalize the transaction the <b>Merchant</b> embeds the " +
            keyWord(Messages.AUTHORIZATION_RESPONSE) +
            " in a newly created " +
            keyWord(Messages.TRANSACTION_REQUEST) +
            " including a possibly updated " +
            keyWord(AMOUNT_JSON) +
            " and sends the completed object to the <b>" + recipient + 
            "</b>:</p>");
        fancyBox(debugData.transactionRequest);
        if (debugData.hybridMode) {
            descriptionStdMargin("Note that " +
            keyWord(RECIPIENT_URL_JSON) +
            " for the " +
            keyWord(Messages.TRANSACTION_REQUEST) +
            " is derived from the " +
            keyWord(EXTENSIONS_JSON) +
            " object of the " +
            keyWord(Messages.PROVIDER_AUTHORITY) +
            " of the <b>User&nbsp;Bank</b>.");
            directTransfer(Messages.TRANSACTION_REQUEST);
        } else {
            description(point + 
                    "<p>After successful validation of the " +
                    keyWord(Messages.TRANSACTION_REQUEST) +
                    " the <b>Acquirer</b> performs a request to the associated card network.</p>");
        }
        description(point + 
            "<p>After successful processing of the transaction request the <b>" + recipient + "</b> returns a matching response to the <b>Merchant</b>:</p>");
        fancyBox(debugData.transactionResponse);
    }
    
    void reserveMode() {
        description(point.sub() +
                "<p>After validating the " +
                keyWord(Messages.AUTHORIZATION_REQUEST) +
                " and checking that the <b>User</b> actually has funds matching the request," +
                " the <b>User&nbsp;Bank</b> <i>reserves</i> the specified amount including a reference to the " +
                "<b>Merchant</b> and " +
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
                keyWord(message) +
                " and checking that the <b>User</b> actually has funds matching the request," +
                " the <b>User&nbsp;Bank</b> transfers money to the <b>Merchant</b> bank account given by the " +
                keyWord(PAYEE_RECEIVE_ACCOUNT_JSON) +
                " object.</p>" +
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
       }
    }

    boolean providerUserResponse(JSONObjectReader response) throws IOException, GeneralSecurityException {
        if (debugData.softAuthorizationError) {
            description(point + 
                "<p>The <b>User&nbsp;Bank</b> found some kind of account problem:</p>");
            fancyBox(response);
            descriptionStdMargin("<p>See <a href=\"#provuserresp\">" +
                PROV_USER_RESPONSE + "</a> for more information.</p>" +
                "<p>Although the Saturn protocol may continue after this point the debug mode won't currently show that...</p>");
        }
        return debugData.softAuthorizationError;
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
