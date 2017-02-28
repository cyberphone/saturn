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

import org.webpki.json.JSONObjectReader;
import org.webpki.json.JSONObjectWriter;

public class ProtectedAccountData implements BaseProperties {
    
    public static JSONObjectWriter encode(AccountDescriptor account,
                                          CardSpecificData cardSpecificData) throws IOException {
        return new JSONObjectWriter()
            .setObject(ACCOUNT_JSON, account.writeObject())
            .setDynamic((wr) -> cardSpecificData == null ? wr : cardSpecificData.writeData(wr));
    }
    
    JSONObjectReader root;

    public ProtectedAccountData(JSONObjectReader rd, PayerAccountTypes payerAccountType) throws IOException {
        root = rd;
        account = new AccountDescriptor(rd.getObject(ACCOUNT_JSON));
        if (payerAccountType.cardPayment) {
            if (!payerAccountType.typeUri.equals(account.typeUri)) {
                throw new IOException("Non-matching card payment type: " + payerAccountType.typeUri);
            }
            cardSpecificData = new CardSpecificData(rd);
        }
        rd.checkForUnread();
    }

    AccountDescriptor account;
    public AccountDescriptor getAccount() {
        return account;
    }

    CardSpecificData cardSpecificData;
    public CardSpecificData getCardSpecificData() {
        return cardSpecificData;
    }

    @Override
    public String toString() {
        return root.toString();
    }
}
