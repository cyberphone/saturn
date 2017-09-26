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
package org.webpki.saturn.common;

import java.io.IOException;

import org.webpki.json.JSONDecoderCache;
import org.webpki.json.JSONObjectWriter;

public abstract class AccountDataEncoder {

    protected abstract JSONObjectWriter writeObject(JSONObjectWriter wr) throws IOException;
    
    public abstract String getContext();
    
    public String getQualifier() {
        return null;  // Optional
    }
    
    public JSONObjectWriter writeObject() throws IOException {
        return new JSONObjectWriter()
            .setString(JSONDecoderCache.CONTEXT_JSON, getContext())
            .setDynamic((wr) -> {
                if (getQualifier() != null) {
                    wr.setString(JSONDecoderCache.QUALIFIER_JSON, getQualifier());
                }
                return writeObject(wr);
            });
    }
}
