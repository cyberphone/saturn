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
package io.interbanking;

import java.io.IOException;

import org.webpki.json.JSONDecoderCache;
import org.webpki.json.JSONObjectReader;

// Note that this class is deliberately not aligned with Saturn because
// it should be possible using existing payment rails.

class IBCommon {

    static final String TIME_STAMP_JSON                  = "timeStamp";                  // Everywhere

    static final String TEST_MODE_JSON                   = "testMode";                   // Test mode = no real money involved

    static final String INTERBANKING_CONTEXT_URI         = "https://webpki.github.io/interbanking.io";
    
    void check(JSONObjectReader rd, String expectedQualifier) throws IOException {
        String context = rd.getString(JSONDecoderCache.CONTEXT_JSON);
        if (!context.equals(INTERBANKING_CONTEXT_URI)) {
            throw new IOException("Wrong '" + JSONDecoderCache.CONTEXT_JSON + "' :" + context);
        }
        String qualifier = rd.getString(JSONDecoderCache.QUALIFIER_JSON);
        if (!qualifier.equals(expectedQualifier)) {
            throw new IOException("Wrong '" + JSONDecoderCache.QUALIFIER_JSON + "' :" + qualifier);
        }
    }
}
