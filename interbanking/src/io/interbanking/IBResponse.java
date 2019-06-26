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
package io.interbanking;

import java.io.IOException;

import java.util.GregorianCalendar;

import org.webpki.json.JSONDecoderCache;
import org.webpki.json.JSONObjectReader;
import org.webpki.json.JSONObjectWriter;

import org.webpki.util.ISODateTime;

// Note that this class is deliberately not aligned with Saturn because
// it should be possible using existing payment rails.

public class IBResponse extends IBCommon {
    
    public IBResponse(JSONObjectReader rd) throws IOException {
        check(rd, INTERBANKING_RESPONSE);
        referenceId = rd.getString(REFERENCE_ID_JSON);
        timeStamp = rd.getDateTime(TIME_STAMP_JSON, ISODateTime.COMPLETE);
        testMode = rd.getBooleanConditional(TEST_MODE_JSON);
        rd.checkForUnread();
    }

    private GregorianCalendar timeStamp;
    public GregorianCalendar getTimeStamp() {
        return timeStamp;
    }

    private String referenceId;
    public String getReferenceId() {
        return referenceId;
    }

    private boolean testMode;
    public boolean getTestMode() {
        return testMode;
    }

    public static JSONObjectWriter encode(String referenceId,
                                          boolean testMode) throws IOException {
        return new JSONObjectWriter()
            .setString(JSONDecoderCache.CONTEXT_JSON, INTERBANKING_CONTEXT_URI)
            .setString(JSONDecoderCache.QUALIFIER_JSON, INTERBANKING_RESPONSE)
            .setString(REFERENCE_ID_JSON, referenceId)
            .setDynamic((wr) -> testMode ? wr.setBoolean(TEST_MODE_JSON, true) : wr)
            .setDateTime(TIME_STAMP_JSON, new GregorianCalendar(), ISODateTime.UTC_NO_SUBSECONDS);
    }
}
