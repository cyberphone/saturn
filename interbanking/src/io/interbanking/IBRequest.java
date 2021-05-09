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
package io.interbanking;

import java.io.IOException;

import java.math.BigDecimal;

import java.security.GeneralSecurityException;

import java.util.GregorianCalendar;

import java.util.logging.Logger;

import javax.servlet.http.HttpServletResponse;

import org.webpki.json.JSONCryptoHelper;
import org.webpki.json.JSONDecoderCache;
import org.webpki.json.JSONObjectReader;
import org.webpki.json.JSONObjectWriter;
import org.webpki.json.JSONOutputFormats;
import org.webpki.json.JSONParser;
import org.webpki.json.JSONSignatureDecoder;
import org.webpki.json.JSONX509Verifier;

import org.webpki.net.HTTPSWrapper;

import org.webpki.saturn.common.BaseProperties;
import org.webpki.saturn.common.HttpSupport;
import org.webpki.saturn.common.ServerX509Signer;

import org.webpki.util.ISODateTime;

// Note that this class is deliberately not aligned with Saturn because
// it should be possible using existing payment rails.

public class IBRequest extends IBCommon {

    static final int TIMEOUT_FOR_REQUEST           = 5000;
    
    static boolean logging;
    static Logger logger;
    
    public static void setLogging(boolean logging, Logger logger) {
        IBRequest.logging = logging;
        IBRequest.logger = logger;
    }

    static final String OPERATION_JSON                   = "operation";                  // What is actually requested

    static final String RECIPIENT_URL_JSON               = "recipientUrl";               // Target

    static final String ACCOUNT_JSON                     = "account";                    // Reference to a payer account/credential ID

    static final String TRANSACTION_REFERENCE_JSON       = "transactionReference";       // Reference to a payer bank transaction ID
                                                                                         // Only applies to two phase payments

    static final String AMOUNT_JSON                      = "amount";                     // Money
    static final String CURRENCY_JSON                    = "currency";                   // In this format

    static final String PAYEE_NAME_JSON                  = "payeeName";                  // Common name of payee/merchant

    static final String PAYEE_REFERENCE_JSON             = "payeeReference";             // Payee/merchant internal order ID etc.

    static final String PAYEE_ACCOUNT_JSON               = "payeeAccount";               // Source or destination account ID


    static final String INTERBANKING_REQUEST             = "InterbankingRequest";

    public enum Operations {CREDIT_CARD_TRANSACT, 
                            CREDIT_CARD_REFUND,
                            CREDIT_TRANSFER,
                            REVERSE_CREDIT_TRANSFER}

    public IBRequest(JSONObjectReader rd) throws IOException, GeneralSecurityException {
        check(rd, INTERBANKING_REQUEST);
        operation = Operations.valueOf(rd.getString(OPERATION_JSON));
        recipientUrl = rd.getString(RECIPIENT_URL_JSON);
        account = rd.getString(ACCOUNT_JSON);
        transactionReference = rd.getStringConditional(TRANSACTION_REFERENCE_JSON);
        amount = rd.getMoney(AMOUNT_JSON);
        currency = rd.getString(CURRENCY_JSON);
        payeeName = rd.getString(PAYEE_NAME_JSON);
        payeeReference = rd.getString(PAYEE_REFERENCE_JSON);
        payeeAccount = rd.getString(PAYEE_ACCOUNT_JSON);
        timeStamp = rd.getDateTime(TIME_STAMP_JSON, ISODateTime.COMPLETE);
        testMode = rd.getBooleanConditional(TEST_MODE_JSON);
        signatureDecoder = rd.getSignature(new JSONCryptoHelper.Options()
                .setKeyIdOption(JSONCryptoHelper.KEY_ID_OPTIONS.FORBIDDEN)
                .setPublicKeyOption(JSONCryptoHelper.PUBLIC_KEY_OPTIONS.CERTIFICATE_PATH));
        rd.checkForUnread();
    }

    GregorianCalendar timeStamp;
    public GregorianCalendar getTimeStamp() {
        return timeStamp;
    }

    BigDecimal amount;
    public BigDecimal getAmount() {
        return amount;
    }

    private String account;
    public String getAccount() {
        return account;
    }

    private String recipientUrl;
    public String getRecipientUrl() {
        return recipientUrl;
    }

    private String currency;
    public String getCurrency() {
        return currency;
    }

    private String payeeName;
    public String getPayeeName() {
        return payeeName;
    }

    private String payeeReference;
    public String getPayeeReference() {
        return payeeReference;
    }

    private String payeeAccount;
    public String getPayeeAccount() {
        return payeeAccount;
    }

    private String transactionReference;
    public String getTransactionReference() {
        return transactionReference;
    }

    private Operations operation;
    public Operations getOperation() {
        return operation;
    }

    private boolean testMode;
    public boolean getTestMode() {
        return testMode;
    }

    private JSONSignatureDecoder signatureDecoder;
    public JSONSignatureDecoder getSignatureDecoder() {
        return signatureDecoder;
    }

    public static IBResponse perform(String url,
                                     Operations operation,
                                     String account,
                                     String transactionReference,
                                     BigDecimal amount,
                                     String currency,
                                     String payeeName,
                                     String payeeReference,
                                     String payeeAccount,
                                     boolean testMode,
                                     ServerX509Signer signer)
            throws IOException, GeneralSecurityException {
        JSONObjectWriter request = new JSONObjectWriter()
            .setString(JSONDecoderCache.CONTEXT_JSON, INTERBANKING_CONTEXT_URI)
            .setString(JSONDecoderCache.QUALIFIER_JSON, INTERBANKING_REQUEST)
            .setString(OPERATION_JSON, operation.toString())
            .setString(RECIPIENT_URL_JSON, url)
            .setString(ACCOUNT_JSON, account)
            .setDynamic((wr) -> transactionReference == null ?
                    wr : wr.setString(TRANSACTION_REFERENCE_JSON, transactionReference))
            .setMoney(AMOUNT_JSON, amount)
            .setString(CURRENCY_JSON, currency)
            .setString(PAYEE_NAME_JSON, payeeName)
            .setString(PAYEE_REFERENCE_JSON, payeeReference)
            .setString(PAYEE_ACCOUNT_JSON, payeeAccount)
            .setDynamic((wr) -> testMode ? wr.setBoolean(TEST_MODE_JSON, true) : wr)
            .setDateTime(TIME_STAMP_JSON, new GregorianCalendar(), ISODateTime.UTC_NO_SUBSECONDS)
            .setSignature(signer);
        if (logging) {
            logger.info("About to call " + url + "\nwith data:\n" + request);
        }

        // Now calling...
        HTTPSWrapper wrap = new HTTPSWrapper();
        wrap.setTimeout(TIMEOUT_FOR_REQUEST);
        wrap.setHeader(HttpSupport.HTTP_CONTENT_TYPE_HEADER, BaseProperties.JSON_CONTENT_TYPE);
        wrap.setHeader(HttpSupport.HTTP_ACCEPT_HEADER, BaseProperties.JSON_CONTENT_TYPE);
        wrap.setRequireSuccess(false);
        wrap.makePostRequest(url, request.serializeToBytes(JSONOutputFormats.NORMALIZED));
        if (wrap.getResponseCode() != HttpServletResponse.SC_OK) {
            throw new IOException("HTTP error " + wrap.getResponseCode() + " " + wrap.getResponseMessage() + ": " +
                                  (wrap.getData() == null ? "No other information available" : wrap.getDataUTF8()));
        }
        // We expect JSON, yes
        if (!wrap.getRawContentType().equals(BaseProperties.JSON_CONTENT_TYPE)) {
            throw new IOException("Content-Type must be \"" + BaseProperties.JSON_CONTENT_TYPE + 
                                  "\" , found: " + wrap.getRawContentType());
        }

        // Now getting the result...
        JSONObjectReader result = JSONParser.parse(wrap.getData());
        if (logging) {
            logger.info("Call to " + url +
                        "\nreturned:\n" + result);
        }
        return new IBResponse(result);
    }

    public void verifyCallerAuthenticity(JSONX509Verifier paymentRoot)
            throws IOException, GeneralSecurityException {
        signatureDecoder.verify(paymentRoot);
    }
}
