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
package org.payments.sepa;

import java.io.IOException;

import org.webpki.json.JSONObjectReader;
import org.webpki.json.JSONOutputFormats;

import org.webpki.saturn.common.AccountDataDecoder;

public final class SEPAAccountDataDecoder extends AccountDataDecoder {

    static final String CONTEXT = "https://sepa.payments.org/saturn/v3#account";

    static final String IBAN_JSON = "iban";

    @Override
    protected void readJSONData(JSONObjectReader rd) throws IOException {
        readOptionalNonce(rd);
        accountId = rd.getString(IBAN_JSON);
    }
    
    @Override
    protected SEPAAccountDataEncoder createEncoder() {
        return new SEPAAccountDataEncoder();
    }

    @Override
    protected byte[] getAccountObject() throws IOException {
        return getWriter().serializeToBytes(JSONOutputFormats.CANONICALIZED);
    }

    @Override
    public String getContext() {
        return CONTEXT;
    }
}
