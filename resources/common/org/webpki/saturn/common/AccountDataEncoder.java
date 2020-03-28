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

import org.webpki.json.JSONObjectReader;
import org.webpki.json.JSONObjectWriter;
import org.webpki.json.JSONDecoderCache;

public abstract class AccountDataEncoder {
    
    private String context;
    public final String getContext() {
        return context;
    }

    protected JSONObjectWriter writer;
    public final JSONObjectWriter writeObject() throws IOException {
        return writer;
    }
    
    protected final JSONObjectWriter setInternal(String context) throws IOException {
        return writer = new JSONObjectWriter().setString(JSONDecoderCache.CONTEXT_JSON, 
                                                         this.context = context);
    }

    public abstract String getPartialAccountIdentifier(String accountId);  // Like ************4567
    
    public final static AccountDataEncoder create(AccountDataDecoder accountDataDecoder, 
                                                  boolean keepNonce) throws IOException {
        AccountDataEncoder accountDataEncoder = accountDataDecoder.createEncoder();
        accountDataEncoder.context = accountDataDecoder.getContext();
        JSONObjectReader reader = new JSONObjectReader(accountDataDecoder.getWriter()).clone();
        if (!keepNonce && reader.hasProperty(BaseProperties.NONCE_JSON)) {
            reader.removeProperty(BaseProperties.NONCE_JSON);
        }
        accountDataEncoder.writer = new JSONObjectWriter(reader);
        return accountDataEncoder;
    }

    public static String visualFormattedAccountId(String accountId) {
        StringBuilder s = new StringBuilder();
        int q = 0;
        for (char c : accountId.toCharArray()) {
            if (q != 0 && q % 4 == 0) {
                s.append(' ');
            }
            s.append(c);
            q++;
        }
        return s.toString();
    }
}
