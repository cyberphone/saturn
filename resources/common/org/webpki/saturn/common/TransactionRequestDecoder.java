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

import org.webpki.json.JSONObjectReader;
import org.webpki.json.JSONSignatureDecoder;
import org.webpki.json.JSONX509Verifier;

import org.webpki.util.ISODateTime;

public class TransactionRequestDecoder implements BaseProperties {
    
    public TransactionRequestDecoder(JSONObjectReader rd, Boolean cardPayment)
            throws IOException, GeneralSecurityException {
        root = Messages.TRANSACTION_REQUEST.parseBaseMessage(rd);
        authorizationResponse = new AuthorizationResponseDecoder(
                Messages.AUTHORIZATION_RESPONSE.getEmbeddedMessage(rd));
        recipientUrl = rd.getString(RECIPIENT_URL_JSON);
        actualAmount = rd.getMoney(AMOUNT_JSON,
                                   authorizationResponse
                                       .authorizationRequest.paymentRequest.currency.decimals);
        referenceId = rd.getString(REFERENCE_ID_JSON);
        timeStamp = rd.getDateTime(TIME_STAMP_JSON, ISODateTime.COMPLETE);
        software = new Software(rd);
        signatureDecoder = rd.getSignature(REQUEST_SIGNATURE_JSON,
                                           AuthorizationRequestDecoder.signatureOptions);
        if (cardPayment != null &&
            authorizationResponse
                .authorizationRequest.paymentMethod.isCardPayment() ^ cardPayment) {
            throw new IOException("Incompatible payment method: " + 
                authorizationResponse.authorizationRequest.paymentMethod.getPaymentMethodUrl());
        }
        rd.checkForUnread();
    }

    Software software;

    JSONObjectReader root;

    String recipientUrl;
    public String getRecipientUrl() {
        return recipientUrl;
    }

    GregorianCalendar timeStamp;
    public GregorianCalendar getTimeStamp() {
        return timeStamp;
    }

    BigDecimal actualAmount;
    public BigDecimal getAmount() {
        return actualAmount;
    }

    String referenceId;
    public String getReferenceId() {
        return referenceId;
    }

    public boolean getTestMode() {
        return authorizationResponse.authorizationRequest.testMode;
    }

    public PaymentRequestDecoder getPaymentRequest() {
        return authorizationResponse.authorizationRequest.paymentRequest;
    }

    JSONSignatureDecoder signatureDecoder;
    public JSONSignatureDecoder getSignatureDecoder() {
        return signatureDecoder;
    }

    AuthorizationResponseDecoder authorizationResponse;
    public AuthorizationResponseDecoder getAuthorizationResponse() {
        return authorizationResponse;
    }

    public void verifyPayerBank(JSONX509Verifier paymentRoot)
            throws IOException, GeneralSecurityException {
        authorizationResponse.signatureDecoder.verify(paymentRoot);
    }
}
