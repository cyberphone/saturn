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

import java.util.GregorianCalendar;

import org.webpki.json.JSONObjectWriter;

import org.webpki.util.ISODateTime;

public class RefundResponseEncoder implements BaseProperties {
    
    public static JSONObjectWriter encode(RefundRequestDecoder refundRequest,
                                          String referenceId,
                                          String optionalLogData,
                                          ServerX509Signer signer)
            throws IOException, GeneralSecurityException {
        return Messages.REFUND_RESPONSE.createBaseMessage()
            .setDynamic((wr) -> optionalLogData == null ?
                    wr : wr.setString(LOG_DATA_JSON, optionalLogData))
            .setString(REFERENCE_ID_JSON, referenceId)
            .setDateTime(TIME_STAMP_JSON, new GregorianCalendar(), ISODateTime.UTC_NO_SUBSECONDS)
            .setDynamic((wr) -> Software.encode(wr,
                                                TransactionResponseEncoder.SOFTWARE_NAME,
                                                TransactionResponseEncoder.SOFTWARE_VERSION))
            .setObject(Messages.REFUND_REQUEST.lowerCamelCase(), refundRequest.root)
            .setSignature(AUTHORIZATION_SIGNATURE_JSON, signer);
    }
}
