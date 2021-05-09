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

import java.security.GeneralSecurityException;

import org.webpki.crypto.HashAlgorithms;

import org.webpki.json.JSONDecoder;
import org.webpki.json.JSONObjectReader;
import org.webpki.json.JSONOutputFormats;

public abstract class AccountDataDecoder extends JSONDecoder {

    private byte[] optionalNonce;
    
    protected String accountId;

    public final String logLine() throws IOException {
        return getWriter().serializeToString(JSONOutputFormats.NORMALIZED);
    }

    public final byte[] getAccountHash(HashAlgorithms accountHashAlgorithm) throws IOException,
                                                                                   GeneralSecurityException {
        return optionalNonce == null ? null : accountHashAlgorithm.digest(getAccountObject());
    }

    @Override
    public final String getQualifier() {
        return null;
    }

    // All invariant backend payment data (minimally: account number + context)
    // returned as a canonical binary
    protected abstract byte[] getAccountObject() throws IOException;
    
    // Core account number
    public final String getAccountId() {
        return accountId;
    }

    // Must be called in every BackendPaymentDataDecoder.readJSONData()
    protected final void readOptionalNonce(JSONObjectReader rd) throws IOException {
        optionalNonce = rd.getBinaryConditional(BaseProperties.NONCE_JSON);
    }
    
    protected abstract AccountDataEncoder createEncoder();
}
