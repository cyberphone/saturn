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

import org.webpki.json.JSONObjectReader;
import org.webpki.json.JSONOutputFormats;

import org.webpki.saturn.common.BaseProperties;
import org.webpki.saturn.common.PaymentMethods;
import org.webpki.saturn.common.AuthorizationRequest;

public final class SEPAPaymentMethodDecoder extends AuthorizationRequest.PaymentMethodDecoder {

    private static final long serialVersionUID = 1L;

    String payeeIban;
    
    byte[] nonce;
    
    JSONObjectReader account;

    @Override
    protected void readJSONData(JSONObjectReader rd) throws IOException {
        if (rd.hasProperty(BaseProperties.ACCOUNT_JSON)) {
            account = rd.getObject(BaseProperties.ACCOUNT_JSON);
            nonce = account.getBinary(BaseProperties.NONCE_JSON);
            rd = account;
        }
        payeeIban = rd.getString(SEPAPaymentMethodEncoder.PAYEE_IBAN_JSON);
    }

    public String getPayeeIban() {
        return payeeIban;
    }

    @Override
    protected JSONObjectReader getAccountObject() {
        return account;
    }

    @Override
    public String getContext() {
        return SEPAPaymentMethodEncoder.PAYMENT_METHOD;
    }

    @Override
    public boolean match(PaymentMethods payerAccountType) throws IOException {
        return PaymentMethods.BANK_DIRECT == payerAccountType;
    }
}
