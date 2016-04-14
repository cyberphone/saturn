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

import java.util.Date;
import java.util.GregorianCalendar;

import org.webpki.crypto.AlgorithmPreferences;

import org.webpki.json.JSONObjectReader;
import org.webpki.json.JSONObjectWriter;
import org.webpki.json.JSONSignatureDecoder;
import org.webpki.json.JSONSignatureTypes;

public class TransactionResponse implements BaseProperties {
    
    public TransactionResponse(JSONObjectReader rd) throws IOException {
        Messages.parseBaseMessage(Messages.TRANSACTION_RESPONSE, root = rd);
        transactionRequest = new TransactionRequest(rd.getObject(EMBEDDED_JSON));
        payerAccountReference = rd.getString(PAYER_ACCOUNT_REFERENCE_JSON);
        if (transactionRequest.reserveOrBasicRequest.message.isCardPayment()) {
            
        } else {
            accountDescriptor = new AccountDescriptor(rd.getObject(PAYEE_ACCOUNT_JSON));
        }
        dateTime = rd.getDateTime(TIME_STAMP_JSON);
        software = new Software(rd);
        signatureDecoder = rd.getSignature(AlgorithmPreferences.JOSE);
        signatureDecoder.verify(JSONSignatureTypes.X509_CERTIFICATE);
        rd.checkForUnread();
    }

    JSONObjectReader root;
    
    Software software;
    
    GregorianCalendar dateTime;

    AccountDescriptor accountDescriptor;
    public AccountDescriptor getPayeeAccountDescriptor() {
        return accountDescriptor;
    }

    String payerAccountReference;
    public String getPayerAccountReference() {
        return payerAccountReference;
    }

    JSONSignatureDecoder signatureDecoder;
    public JSONSignatureDecoder getSignatureDecoder() {
        return signatureDecoder;
    }

    TransactionRequest transactionRequest;
    public TransactionRequest getTransactionRequest() {
        return transactionRequest;
    }

    public static JSONObjectWriter encode(TransactionRequest transactionRequest,
                                          AccountDescriptor payeeAccount,
                                          String payerAccountReference,
                                          ServerX509Signer signer) throws IOException {
        JSONObjectWriter wr = Messages.createBaseMessage(Messages.TRANSACTION_RESPONSE)
            .setObject(EMBEDDED_JSON, transactionRequest.root)
            .setString(PAYER_ACCOUNT_REFERENCE_JSON, payerAccountReference);
        if (transactionRequest.reserveOrBasicRequest.message.isCardPayment()) {
            
        } else {
            wr.setObject(PAYEE_ACCOUNT_JSON, payeeAccount.writeObject());
        }
        wr.setDateTime(TIME_STAMP_JSON, new Date(), true)
          .setObject(SOFTWARE_JSON, Software.encode(TransactionRequest.SOFTWARE_NAME,
                                                    TransactionRequest.SOFTWARE_VERSION))
          .setSignature(signer);
        return wr;
    }
}
