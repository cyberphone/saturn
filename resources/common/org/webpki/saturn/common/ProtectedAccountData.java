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

import org.webpki.json.JSONObjectReader;
import org.webpki.json.JSONObjectWriter;

public class ProtectedAccountData implements BaseProperties {
    
    public static JSONObjectWriter encode(AccountDescriptor accountDescriptor,
                                          String accountHolder,
                                          Date expires,
                                          String accountSecurityCode) throws IOException {
        return new JSONObjectWriter()
            .setObject(PAYER_ACCOUNT_JSON, accountDescriptor.writeObject())
            .setString(ACCOUNT_HOLDER_JSON, accountHolder)
            .setDateTime(EXPIRES_JSON, expires, true)
            .setString(ACCOUNT_SECURITY_CODE_JSON, accountSecurityCode);
    }
    
    JSONObjectReader root;

    public ProtectedAccountData(JSONObjectReader rd) throws IOException {
        root = rd;
        accountDescriptor = new AccountDescriptor(rd.getObject(PAYER_ACCOUNT_JSON));
        accountHolder = rd.getString(ACCOUNT_HOLDER_JSON);
        expires = rd.getDateTime(EXPIRES_JSON);
        accountSecurityCode = rd.getString(ACCOUNT_SECURITY_CODE_JSON);
        rd.checkForUnread();
    }

    AccountDescriptor accountDescriptor;
    public AccountDescriptor getAccountDescriptor() {
        return accountDescriptor;
    }

    String accountHolder;
    public String getAccountHolder() {
        return accountHolder;
    }

    GregorianCalendar expires;
    public GregorianCalendar getExpires() {
        return expires;
    }

    String accountSecurityCode;
    public String getAccountSecurityCode() {
        return accountSecurityCode;
    }

    @Override
    public String toString() {
        return root.toString();
    }
}
