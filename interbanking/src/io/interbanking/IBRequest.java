/*
 *  Copyright 2015-2018 WebPKI.org (http://webpki.org).
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

    public enum Operations {CREDIT_CARD_TRANSACT}

    public IBRequest(JSONObjectReader rd) throws IOException {
        check(rd, INTERBANKING_REQUEST);
        operation = Operations.valueOf(rd.getString(OPERATION_JSON));
        accountId = rd.getString(ACCOUNT_ID_JSON);
        referenceId = rd.getString(REFERENCE_ID_JSON);
        amount = rd.getBigDecimal(AMOUNT_JSON);
        currency = rd.getString(CURRENCY_JSON);
        merchant = rd.getString(MERCHANT_JSON);
        timeStamp = rd.getDateTime(TIME_STAMP_JSON, ISODateTime.COMPLETE);
        testMode = rd.getBooleanConditional(TEST_MODE_JSON);
        signatureDecoder = rd.getSignature(new JSONCryptoHelper.Options());
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

    private String accountId;
    public String getAccountId() {
        return accountId;
    }

    private String currency;
    public String getCurrency() {
        return currency;
    }

    private String merchant;
    public String getMerchant() {
        return merchant;
    }

    private String referenceId;
    public String getReferenceId() {
        return referenceId;
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
                                     String accountId,
                                     String referenceId,
                                     BigDecimal amount,
                                     String currency,
                                     String merchant,
                                     boolean testMode,
                                     ServerX509Signer signer) throws IOException {
        JSONObjectWriter request = new JSONObjectWriter()
            .setString(JSONDecoderCache.CONTEXT_JSON, INTERBANKING_CONTEXT_URI)
            .setString(JSONDecoderCache.QUALIFIER_JSON, INTERBANKING_REQUEST)
            .setString(OPERATION_JSON, operation.toString())
            .setString(ACCOUNT_ID_JSON, accountId)
            .setString(REFERENCE_ID_JSON, referenceId)
            .setBigDecimal(AMOUNT_JSON, amount)
            .setString(CURRENCY_JSON, currency)
            .setString(MERCHANT_JSON, merchant)
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

    public void verifyCallerAuthenticity(JSONX509Verifier paymentRoot) throws IOException {
        signatureDecoder.verify(paymentRoot);
    }
}
