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
    
    public static JSONObjectWriter encode(AccountDescriptor accountDescriptor,
                                          CardSpecificData cardSpecificData) throws IOException {
        JSONObjectWriter wr = new JSONObjectWriter()
            .setObject(ACCOUNT_JSON, accountDescriptor.writeObject());
        if (cardSpecificData != null) {
            cardSpecificData.writeData(wr);
        }
        return wr;
    }
    
    JSONObjectReader root;

    public ProtectedAccountData(JSONObjectReader rd, boolean cardAccount) throws IOException {
        root = rd;
        accountDescriptor = new AccountDescriptor(rd.getObject(ACCOUNT_JSON));
        if (cardAccount) {
            cardSpecificData = new CardSpecificData(rd);
        }
        rd.checkForUnread();
    }

    AccountDescriptor accountDescriptor;
    public AccountDescriptor getAccountDescriptor() {
        return accountDescriptor;
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
