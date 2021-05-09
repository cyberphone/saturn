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
package org.webpki.saturn.common;

import java.io.IOException;

import java.math.BigDecimal;

import java.security.GeneralSecurityException;

import java.util.GregorianCalendar;

import org.webpki.json.JSONCryptoHelper;
import org.webpki.json.JSONObjectReader;
import org.webpki.json.JSONSignatureDecoder;

import org.webpki.util.ISODateTime;

public class TransactionResponseDecoder extends ProviderResponseDecoder {
    
    public static enum ERROR {OUT_OF_FUNDS, DELETED_AUTHORIZATION}

    public TransactionResponseDecoder(JSONObjectReader rd)
            throws IOException, GeneralSecurityException {
        root = Messages.TRANSACTION_RESPONSE.parseBaseMessage(rd);
        transactionRequest = new TransactionRequestDecoder(
                Messages.TRANSACTION_REQUEST.getEmbeddedMessage(rd), null);
        if (rd.hasProperty(TRANSACTION_ERROR_JSON)) {
            transactionError = ERROR.valueOf(rd.getString(TRANSACTION_ERROR_JSON));
        }
        optionalLogData = rd.getStringConditional(LOG_DATA_JSON);
        referenceId = rd.getString(REFERENCE_ID_JSON);
        timeStamp = rd.getDateTime(TIME_STAMP_JSON, ISODateTime.COMPLETE);
        software = new Software(rd);
        signatureDecoder = rd.getSignature(AUTHORIZATION_SIGNATURE_JSON,
                new JSONCryptoHelper.Options()
                    .setKeyIdOption(JSONCryptoHelper.KEY_ID_OPTIONS.FORBIDDEN)
                    .setPublicKeyOption(JSONCryptoHelper.PUBLIC_KEY_OPTIONS.CERTIFICATE_PATH));
        rd.checkForUnread();
    }

    JSONObjectReader root;

    Software software;

    GregorianCalendar timeStamp;

    ERROR transactionError;
    public ERROR getTransactionError() {
        return transactionError;
    }

    String optionalLogData;
    public String getOptionalLogData() {
        return optionalLogData;
    }

    String referenceId;
    public String getReferenceId() {
        return referenceId;
    }

    JSONSignatureDecoder signatureDecoder;
    public JSONSignatureDecoder getSignatureDecoder() {
        return signatureDecoder;
    }


    TransactionRequestDecoder transactionRequest;
    public TransactionRequestDecoder getTransactionRequest() {
        return transactionRequest;
    }

    @Override
    public BigDecimal getAmount() {
        return transactionRequest.actualAmount;
    }

    @Override
    public AuthorizationResponseDecoder getAuthorizationResponse() {
        return transactionRequest.authorizationResponse;
    }

    @Override
    JSONObjectReader getRoot() {
        return root;
    }

    @Override
    public GregorianCalendar getProviderTimeStamp() {
        return timeStamp;
    }

    @Override
    public String getProviderReferenceId() {
        return referenceId;
    }

    @Override
    public String getPayeeRequestId() {
        return transactionRequest.referenceId;
    }
}
