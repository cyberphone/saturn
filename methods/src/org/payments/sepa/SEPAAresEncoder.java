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

import org.webpki.saturn.common.AccountDataEncoder;

public final class SEPAAresEncoder extends AccountDataEncoder {

    static final String ARES_CONTEXT       = "https://sepa.payments.org/saturn/v3#ares";

    static final String PAYER_IBAN_JSON    = "payerIban";    // Payer IBAN

    String payerIban;

    public SEPAAresEncoder(String payerIban) {
        this.payerIban = payerIban;
    }

    @Override
    protected JSONObjectWriter writeObject(JSONObjectWriter wr) throws IOException {
        return wr.setString(payerIban, payerIban);
    }

    @Override
    public String getContext() {
        return ARES_CONTEXT;
    }
}
