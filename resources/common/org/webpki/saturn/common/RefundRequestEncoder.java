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

import java.math.BigDecimal;

import java.security.GeneralSecurityException;

import java.util.GregorianCalendar;

import org.webpki.json.JSONObjectWriter;

import org.webpki.util.ISODateTime;

public class RefundRequestEncoder implements BaseProperties {
    
    public static JSONObjectWriter encode(AuthorizationResponseDecoder authorizationResponse,
                                          String recipientUrl,
                                          BigDecimal amount,
                                          AccountDataEncoder payeeSourceAccount,
                                          String referenceId,
                                          ServerAsymKeySigner signer) 
            throws IOException, GeneralSecurityException {
        return Messages.REFUND_REQUEST.createBaseMessage()
            .setString(RECIPIENT_URL_JSON, recipientUrl)
            .setMoney(AMOUNT_JSON,
                    amount,
                    authorizationResponse.authorizationRequest.paymentRequest.currency.decimals)
            .setObject(PAYEE_SOURCE_ACCOUNT_JSON, payeeSourceAccount.writeObject())
            .setString(REFERENCE_ID_JSON, referenceId)
            .setDateTime(TIME_STAMP_JSON, new GregorianCalendar(), ISODateTime.UTC_NO_SUBSECONDS)
            .setDynamic((wr) -> Software.encode(wr,
                                                AuthorizationRequestEncoder.SOFTWARE_NAME, 
                                                AuthorizationRequestEncoder.SOFTWARE_VERSION))
            .setObject(Messages.AUTHORIZATION_RESPONSE.lowerCamelCase(), 
                       authorizationResponse.root)
            .setSignature(REQUEST_SIGNATURE_JSON, signer);
    }
}
