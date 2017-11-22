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
package org.payments.sepa;

import java.io.IOException;

import org.webpki.json.JSONObjectWriter;

import org.webpki.saturn.common.AuthorizationRequest;
import org.webpki.saturn.common.BaseProperties;

public final class SEPAPaymentMethodEncoder extends AuthorizationRequest.PaymentMethodEncoder {

    static final String PAYMENT_METHOD  = "https://sepa.payments.org/saturn/v3#pms";

    static final String PAYEE_IBAN_JSON = "payeeIban";

    String payeeIban;
    
    byte[] nonce;

    public SEPAPaymentMethodEncoder(SEPAPaymentMethodDecoder sepaPaymentMethodDecoder) {
        nonce = sepaPaymentMethodDecoder.nonce;
        payeeIban = sepaPaymentMethodDecoder.payeeIban;
    }

    public SEPAPaymentMethodEncoder(String payeeIban) {
        this.payeeIban = payeeIban;
    }

    @Override
    protected JSONObjectWriter writeObject(JSONObjectWriter wr) throws IOException {
        wr.setString(PAYEE_IBAN_JSON, payeeIban);
        if (nonce != null) {
            wr.setBinary(BaseProperties.NONCE_JSON, nonce);
        }
        return wr;
    }

    @Override
    public String getContext() {
        return PAYMENT_METHOD;
    }
}
