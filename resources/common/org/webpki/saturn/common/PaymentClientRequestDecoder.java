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

import java.util.ArrayList;
import java.util.List;

import org.webpki.json.JSONArrayReader;
import org.webpki.json.JSONObjectReader;

/**
 * Decoder for the Saturn wallet invocation message
 */
public class PaymentClientRequestDecoder implements BaseProperties {
    
    public static class SupportedPaymentMethod {
        public String paymentMethod;
        public String payeeAuthorityUrl;
    }
    
    public PaymentClientRequestDecoder(JSONObjectReader rd) throws IOException {
        Messages.PAYMENT_CLIENT_REQUEST.parseBaseMessage(rd);
        JSONArrayReader ar = rd.getArray(SUPPORTED_PAYMENT_METHODS_JSON);
        supportedPaymentMethods = new ArrayList<>();
        do {
            SupportedPaymentMethod supportedPaymentMethod = new SupportedPaymentMethod();
            JSONObjectReader spm = ar.getObject();
            supportedPaymentMethod.paymentMethod = spm.getString(PAYMENT_METHOD_JSON);
            supportedPaymentMethod.payeeAuthorityUrl = spm.getString(PAYEE_AUTHORITY_URL_JSON);
            supportedPaymentMethods.add(supportedPaymentMethod);
        } while (ar.hasMore());
        paymentRequest = new PaymentRequestDecoder(rd.getObject(PAYMENT_REQUEST_JSON));
        optionalReceiptUrl = rd.getStringConditional(RECEIPT_URL_JSON);
        noMatchingMethodsUrl = rd.getStringConditional(NO_MATCHING_METHODS_URL_JSON);
        rd.checkForUnread();
    }

    List<SupportedPaymentMethod> supportedPaymentMethods;
    public List<SupportedPaymentMethod> getSupportedPaymentMethods() {
        return supportedPaymentMethods;
    }

    String optionalReceiptUrl;
    public String getReceiptUrl() {
        return optionalReceiptUrl;
    }

    PaymentRequestDecoder paymentRequest;
    public PaymentRequestDecoder getPaymentRequest() {
        return paymentRequest;
    }

    String noMatchingMethodsUrl;
    public String getNoMatchingMethodsUrl() {
        return noMatchingMethodsUrl;
    }
}
