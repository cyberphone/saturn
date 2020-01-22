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
package se.bankgirot;

import java.io.IOException;

import org.webpki.json.JSONObjectReader;
import org.webpki.json.JSONOutputFormats;

import org.webpki.saturn.common.AuthorizationRequest;
import org.webpki.saturn.common.AuthorizationRequest.BackendPaymentDataEncoder;

public final class BGBackendPaymentDataDecoder extends AuthorizationRequest.BackendPaymentDataDecoder {

    private static final long serialVersionUID = 1L;

    static final String CONTEXT = "https://bankgirot.se/saturn/v3#bpd";

    static final String BG_NUMBER_JSON = "bgNumber";

    String bgNumber;
    
    @Override
    protected void readJSONData(JSONObjectReader rd) throws IOException {
        readOptionalNonce(rd);
        bgNumber = rd.getString(BG_NUMBER_JSON);
    }

    @Override
    public String getPayeeAccount() {
        return bgNumber;
    }

    @Override
    protected byte[] getAccountObject() throws IOException {
        return getWriter().serializeToBytes(JSONOutputFormats.CANONICALIZED);
    }

    @Override
    public String getContext() {
        return CONTEXT;
    }

    @Override
    protected BackendPaymentDataEncoder createEncoder() {
        return new BGBackendPaymentDataEncoder();
    }
}
