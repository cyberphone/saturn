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

import java.util.List;

import org.webpki.json.JSONArrayWriter;
import org.webpki.json.JSONObjectWriter;

/**
 * Encoder for the Saturn wallet invocation message
 */
public class PaymentClientRequestEncoder implements BaseProperties {

    public static class SupportedPaymentMethod {
        String paymentMethod;
        String payeeAuthorityUrl;
        
        public SupportedPaymentMethod(String paymentMethod, String payeeAuthorityUrl) {
            this.paymentMethod = paymentMethod;
            this.payeeAuthorityUrl = payeeAuthorityUrl;
        }
 
        public SupportedPaymentMethod(PaymentMethods paymentMethod, String payeeAuthorityUrl) {
            this(paymentMethod.getPaymentMethodUrl(), payeeAuthorityUrl);
        }
    }

    public static JSONObjectWriter encode(List<SupportedPaymentMethod> supportedPaymentMethods,
                                          JSONObjectWriter paymentRequest, 
                                          String optionalReceiptUrl,
                                          String noMatchingMethodsUrl) throws IOException {
        return Messages.PAYMENT_CLIENT_REQUEST.createBaseMessage()
            .setDynamic((wr) -> {
                JSONArrayWriter aw = wr.setArray(SUPPORTED_PAYMENT_METHODS_JSON);
                for (SupportedPaymentMethod supportedPaymentMethod : supportedPaymentMethods) {
                    aw.setObject()
                        .setString(PAYMENT_METHOD_JSON, 
                                   supportedPaymentMethod.paymentMethod)
                        .setString(PAYEE_AUTHORITY_URL_JSON, 
                                   supportedPaymentMethod.payeeAuthorityUrl);
                }
                return wr;
            })
            .setObject(PAYMENT_REQUEST_JSON, paymentRequest)
            .setDynamic((wr) -> optionalReceiptUrl == null ?
                    wr : wr.setString(RECEIPT_URL_JSON, optionalReceiptUrl))
            .setDynamic((wr) -> noMatchingMethodsUrl == null ?
                    wr : wr.setString(NO_MATCHING_METHODS_URL_JSON, noMatchingMethodsUrl));
    }
}
