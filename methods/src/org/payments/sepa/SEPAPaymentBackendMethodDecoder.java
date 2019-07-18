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
package org.payments.sepa;

import java.io.IOException;

import org.webpki.json.JSONObjectReader;

import org.webpki.saturn.common.BaseProperties;
import org.webpki.saturn.common.AuthorizationRequest;

public final class SEPAPaymentBackendMethodDecoder extends AuthorizationRequest.PaymentBackendMethodDecoder {

    private static final long serialVersionUID = 1L;

    String payeeIban;
    
    byte[] nonce;
    
    @Override
    protected void readJSONData(JSONObjectReader rd) throws IOException {
        payeeIban = rd.getString(SEPAPaymentBackendMethodEncoder.PAYEE_IBAN_JSON);
        nonce = rd.getBinaryConditional(BaseProperties.NONCE_JSON);
    }

    @Override
    public String getPayeeAccount() {
        return payeeIban;
    }

    @Override
    protected JSONObjectReader getAccountObject() throws IOException {
        return nonce == null ? null : new JSONObjectReader(getWriter());
    }

    @Override
    public String getContext() {
        return SEPAPaymentBackendMethodEncoder.CONTEXT;
    }
}
