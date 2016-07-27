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

import java.math.BigDecimal;

import java.security.GeneralSecurityException;
import java.security.PublicKey;

import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Vector;

import org.webpki.crypto.AlgorithmPreferences;

import org.webpki.json.encryption.DecryptionKeyHolder;

import org.webpki.json.JSONObjectReader;
import org.webpki.json.JSONObjectWriter;
import org.webpki.json.JSONParser;
import org.webpki.json.JSONX509Verifier;

public class FinalizeRequest implements BaseProperties {
    
    static Messages matching(ReserveOrBasicResponse reserveOrBasicResponse) throws IOException {
        switch (reserveOrBasicResponse.message) {
            case RESERVE_CREDIT_RESPONSE:
                return Messages.FINALIZE_CREDIT_REQUEST;

            case RESERVE_CARDPAY_RESPONSE:
                return Messages.FINALIZE_CARDPAY_REQUEST;

            default:
                throw new IOException("Unexpected message type: " + reserveOrBasicResponse.message.toString());
        }
    }

    static BigDecimal checkAmount(BigDecimal amount, ReserveOrBasicResponse reserveOrBasicResponse) throws IOException {
        BigDecimal reservedAmount = reserveOrBasicResponse
            .transactionResponse
                .transactionRequest
                     .reserveOrBasicRequest.getPaymentRequest().amount;
        if (amount.compareTo(reservedAmount) > 0) {
            throw new IOException("The requested amount exceeds the reserved amount");
        }
        return amount;
    }

    static int getDecimals(ReserveOrBasicResponse reserveOrBasicResponse) {
        return reserveOrBasicResponse
                   .transactionResponse
                       .transactionRequest
                           .reserveOrBasicRequest
                               .paymentRequest
                                   .currency.getDecimals();
    }

    public FinalizeRequest(JSONObjectReader rd) throws IOException, GeneralSecurityException {
        reserveOrBasicResponse = new ReserveOrBasicResponse(rd.getObject(EMBEDDED_JSON));
        message = Messages.parseBaseMessage(matching(reserveOrBasicResponse), root = rd);
        amount = checkAmount(rd.getBigDecimal(AMOUNT_JSON, 
                                              getDecimals(reserveOrBasicResponse)),
                             reserveOrBasicResponse);
        referenceId = rd.getString(REFERENCE_ID_JSON);
        timeStamp = rd.getDateTime(TIME_STAMP_JSON);
        software = new Software(rd);
        publicKey = rd.getSignature(AlgorithmPreferences.JOSE).getPublicKey();
        ReserveOrBasicRequest.comparePublicKeys(publicKey,
                                                reserveOrBasicResponse
                                                    .transactionResponse
                                                        .transactionRequest
                                                            .reserveOrBasicRequest.getPaymentRequest());
        rd.checkForUnread();
    }

    GregorianCalendar timeStamp;

    Software software;
    
    JSONObjectReader root;
    
    Messages message;
    
    ReserveOrBasicResponse reserveOrBasicResponse;
    public ReserveOrBasicResponse getReserveOrBasicResponse() {
        return reserveOrBasicResponse;
    }

    BigDecimal amount;
    public BigDecimal getAmount() {
        return amount;
    }
    
    String referenceId;
    public String getReferenceId() {
        return referenceId;
    }

    PublicKey publicKey;
    public PublicKey getPublicKey() {
        return publicKey;
    }

    // Convenience method
    public String getProviderAuthorityUrl() {
        return reserveOrBasicResponse
            .transactionResponse
                .transactionRequest
                    .reserveOrBasicRequest
                        .providerAuthorityUrl;
    }

    // Convenience method
    public ProtectedAccountData getProtectedAccountData(Vector<DecryptionKeyHolder> decryptionKeys)
    throws IOException, GeneralSecurityException {
        return new ProtectedAccountData(JSONParser.parse(reserveOrBasicResponse
                                                             .transactionResponse
                                                                 .encryptedCardData.getDecryptedData(decryptionKeys)));
    }

    // Convenience method
    public String getMerchantKeyIssuer() throws IOException {
        return reserveOrBasicResponse
            .signatureDecoder.getCertificatePath()[0].getSubjectX500Principal().getName();
    }

    // Convenience method
    public void verifyMerchantBank(JSONX509Verifier verifier) throws IOException {
        reserveOrBasicResponse.signatureDecoder.verify(verifier);
    }

    // Convenience method
    public Payee getPayee() {
        return reserveOrBasicResponse
            .transactionResponse
                .transactionRequest
                    .reserveOrBasicRequest
                        .paymentRequest.getPayee();
    }

    public static JSONObjectWriter encode(ReserveOrBasicResponse reserveOrBasicResponse,
                                          BigDecimal amount,
                                          String referenceId,
                                          ServerAsymKeySigner signer)
    throws IOException, GeneralSecurityException {
        return Messages.createBaseMessage(matching(reserveOrBasicResponse))
            .setObject(EMBEDDED_JSON, reserveOrBasicResponse.root)
            .setBigDecimal(AMOUNT_JSON,
                           checkAmount(amount, reserveOrBasicResponse),
                           getDecimals(reserveOrBasicResponse))
            .setString(REFERENCE_ID_JSON, referenceId)
            .setDateTime(TIME_STAMP_JSON, new Date(), true)
            .setObject(SOFTWARE_JSON, Software.encode(TransactionRequest.SOFTWARE_NAME,
                                                      TransactionRequest.SOFTWARE_VERSION))
            .setSignature(signer);
    }
}
