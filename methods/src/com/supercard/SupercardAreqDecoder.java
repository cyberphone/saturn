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
package com.supercard;

import java.io.IOException;

import org.webpki.json.JSONObjectReader;

import org.webpki.saturn.common.PayerAccountTypes;
import org.webpki.saturn.common.PaymentMethodDecoder;

public final class SupercardAreqDecoder extends PaymentMethodDecoder {

    private static final long serialVersionUID = 1L;

    @Override
    protected void readJSONData(JSONObjectReader rd) throws IOException {
    }

    @Override
    public String getContext() {
        return SupercardAreqEncoder.AREQ_CONTEXT;
    }

    @Override
    public boolean match(PayerAccountTypes payerAccountType) throws IOException {
        return PayerAccountTypes.SUPER_CARD == payerAccountType;
    }
}
