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
package org.webpki.saturn.common;

import java.io.IOException;

import java.math.BigDecimal;

import java.security.GeneralSecurityException;
import java.security.PublicKey;

import java.util.Date;
import java.util.GregorianCalendar;

import org.webpki.crypto.AlgorithmPreferences;

import org.webpki.json.JSONDecoderCache;
import org.webpki.json.JSONObjectReader;
import org.webpki.json.JSONObjectWriter;

public class FinalizeRequest implements BaseProperties {
    
    static final Messages[] valid = {Messages.FINALIZE_DEBIT_REQUEST, Messages.FINALIZE_DEBIT_REQUEST};
    
    public FinalizeRequest(JSONObjectReader rd) throws IOException, GeneralSecurityException {
        message = Messages.parseBaseMessage(valid, root = rd);
        embeddedResponse = new ReserveOrBasicResponse(rd.getObject(PROVIDER_AUTHORIZATION_JSON));
        amount = rd.getBigDecimal(AMOUNT_JSON,
                                  embeddedResponse.getPaymentRequest().getCurrency().getDecimals());
        referenceId = rd.getString(REFERENCE_ID_JSON);
        timeStamp = rd.getDateTime(TIME_STAMP_JSON);
        software = new Software(rd);
        outerPublicKey = rd.getSignature(AlgorithmPreferences.JOSE).getPublicKey();
        PaymentRequest paymentRequest = embeddedResponse.getPaymentRequest();
        ReserveOrBasicRequest.comparePublicKeys(outerPublicKey, paymentRequest);
        if (amount.compareTo(paymentRequest.getAmount()) > 0) {
            throw new IOException("Final amount must be less or equal to reserved amount");
        }
        rd.checkForUnread();
    }

    GregorianCalendar timeStamp;

    Software software;
    
    PublicKey outerPublicKey;
    
    JSONObjectReader root;
    
    Messages message;
    
    ReserveOrBasicResponse embeddedResponse;
    public ReserveOrBasicResponse getEmbeddedResponse() {
        return embeddedResponse;
    }

    BigDecimal amount;
    public BigDecimal getAmount() {
        return amount;
    }
    
    String referenceId;
    public String getReferenceId() {
        return referenceId;
    }

    public static JSONObjectWriter encode(ReserveOrBasicResponse providerResponse,
                                          BigDecimal amount,  // Less or equal the reserved amount
                                          String referenceId,
                                          ServerAsymKeySigner signer)
    throws IOException, GeneralSecurityException {
        return Messages.createBaseMessage(Messages.FINALIZE_REQUEST)
            .setBigDecimal(AMOUNT_JSON,
                           amount,
                           providerResponse.getPaymentRequest().getCurrency().getDecimals())
            .setObject(PROVIDER_AUTHORIZATION_JSON, providerResponse.root)
            .setString(REFERENCE_ID_JSON, referenceId)
            .setDateTime(TIME_STAMP_JSON, new Date(), true)
            .setObject(SOFTWARE_JSON, Software.encode(PaymentRequest.SOFTWARE_NAME,
                                                      PaymentRequest.SOFTWARE_VERSION))
            .setSignature(signer);
    }
}
