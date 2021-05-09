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

import org.webpki.json.JSONObjectReader;
import org.webpki.json.JSONObjectWriter;

import org.webpki.util.ISODateTime;

public class AuthorizationRequestEncoder implements BaseProperties {
    
    public static final String SOFTWARE_NAME    = "WebPKI.org - Payee";
    public static final String SOFTWARE_VERSION = "1.00";

    public static JSONObjectWriter encode(Boolean testMode,
                                          String recipientUrl,
                                          String payeeAuthorityUrl,
                                          PaymentMethods paymentMethod,
                                          JSONObjectReader encryptedAuthorizationData,
                                          String clientIpAddress,
                                          PaymentRequestDecoder paymentRequest,
                                          AccountDataEncoder payeeReceiveAccount,
                                          ServerAsymKeySigner signer)
            throws IOException, GeneralSecurityException {
        return Messages.AUTHORIZATION_REQUEST.createBaseMessage()
            .setDynamic((wr) -> testMode == null ? wr : wr.setBoolean(TEST_MODE_JSON, testMode))
            .setString(RECIPIENT_URL_JSON, recipientUrl)
            .setString(PAYEE_AUTHORITY_URL_JSON, payeeAuthorityUrl)
            .setString(PAYMENT_METHOD_JSON, paymentMethod.getPaymentMethodUrl())
            // Note: we reuse the referenceId of PaymentRequest
            // since these objects are "inseparable" anyway
            .setObject(PAYMENT_REQUEST_JSON, paymentRequest.root)
            .setObject(ENCRYPTED_AUTHORIZATION_JSON, encryptedAuthorizationData)
            .setObject(PAYEE_RECEIVE_ACCOUNT_JSON, payeeReceiveAccount.writeObject())
            .setString(CLIENT_IP_ADDRESS_JSON, clientIpAddress)
            .setDateTime(TIME_STAMP_JSON, new GregorianCalendar(), ISODateTime.UTC_NO_SUBSECONDS)
            .setDynamic((wr) -> Software.encode(wr, SOFTWARE_NAME, SOFTWARE_VERSION))
            .setSignature(REQUEST_SIGNATURE_JSON, signer);
    }
}
