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
import java.util.Vector;

import org.webpki.crypto.AlgorithmPreferences;
import org.webpki.json.JSONArrayReader;
import org.webpki.json.JSONArrayWriter;
import org.webpki.json.JSONObjectReader;
import org.webpki.json.JSONObjectWriter;
import org.webpki.json.JSONSignatureDecoder;
import org.webpki.json.JSONSignatureTypes;

public class TransactionRequest implements BaseProperties {
    
    public static final String SOFTWARE_NAME    = "WebPKI.org - Bank";
    public static final String SOFTWARE_VERSION = "1.00";

    public TransactionRequest(JSONObjectReader rd) throws IOException {
        Messages.parseBaseMessage(Messages.TRANSACTION_REQUEST, root = rd);
        reserveOrBasicRequest = new ReserveOrBasicRequest(rd.getObject(EMBEDDED_JSON));
        if (reserveOrBasicRequest.message.isCardPayment()) {
            
        } else {
            Vector<AccountDescriptor> accounts = new Vector<AccountDescriptor>();
            JSONArrayReader ar = rd.getArray(PAYEE_ACCOUNTS_JSON);
            do {
                accounts.add(new AccountDescriptor(ar.getObject()));
            } while (ar.hasMore());
            accountDescriptors = accounts.toArray(new AccountDescriptor[0]);
        }
        referenceId = rd.getString(REFERENCE_ID_JSON);
        dateTime = rd.getDateTime(TIME_STAMP_JSON);
        software = new Software(rd);
        signatureDecoder = rd.getSignature(AlgorithmPreferences.JOSE);
        signatureDecoder.verify(JSONSignatureTypes.X509_CERTIFICATE);
        rd.checkForUnread();
    }

    JSONObjectReader root;
    
    Software software;
    
    GregorianCalendar dateTime;

    String referenceId;
    public String getReferenceId() {
        return referenceId;
    }

    AccountDescriptor[] accountDescriptors;
    public AccountDescriptor[] getPayeeAccountDescriptors() {
        return accountDescriptors;
    }

    JSONSignatureDecoder signatureDecoder;
    public JSONSignatureDecoder getSignatureDecoder() {
        return signatureDecoder;
    }

    ReserveOrBasicRequest reserveOrBasicRequest;
    public ReserveOrBasicRequest getReserveOrBasicRequest() {
        return reserveOrBasicRequest;
    }

    public static JSONObjectWriter encode(ReserveOrBasicRequest reserveOrBasicRequest,
                                          AccountDescriptor[] accountDescriptors,
                                          String referenceId,
                                          ServerX509Signer signer) throws IOException {
        JSONObjectWriter wr = Messages.createBaseMessage(Messages.TRANSACTION_REQUEST)
            .setObject(EMBEDDED_JSON, reserveOrBasicRequest.root);
        if (reserveOrBasicRequest.message.isCardPayment()) {
        } else {
            JSONArrayWriter aw = wr.setArray(PAYEE_ACCOUNTS_JSON);
            for (AccountDescriptor account : accountDescriptors) {
                aw.setObject(account.writeObject());
            }
        }
        wr.setString(REFERENCE_ID_JSON, referenceId)
          .setDateTime(TIME_STAMP_JSON, new Date(), true)
          .setObject(SOFTWARE_JSON, Software.encode(SOFTWARE_NAME, SOFTWARE_VERSION))
          .setSignature(signer);
        return wr;
    }
}
