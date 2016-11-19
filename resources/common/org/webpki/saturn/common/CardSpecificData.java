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

import java.util.GregorianCalendar;

import org.webpki.json.JSONObjectReader;
import org.webpki.json.JSONObjectWriter;

// Holds card specific account data

public class CardSpecificData implements BaseProperties {
    String accountHolder;                   // Name
    GregorianCalendar expirationDate;       // When the card expirationDate
    String securityCode;                    // CCV or similar
    
    public CardSpecificData(String accountHolder, GregorianCalendar expirationDate, String securityCode) throws IOException {
        this.accountHolder = accountHolder;
        this.expirationDate = expirationDate;
        this.securityCode = securityCode;
    }

    public CardSpecificData(JSONObjectReader rd) throws IOException {
        this(rd.getString(ACCOUNT_HOLDER_JSON), rd.getDateTime(EXPIRES_JSON), rd.getString(ACCOUNT_SECURITY_CODE_JSON));
    }

    public JSONObjectWriter writeData(JSONObjectWriter wr) throws IOException {
        return wr.setString(ACCOUNT_HOLDER_JSON, accountHolder)
                 .setDateTime(EXPIRES_JSON, expirationDate.getTime(), true)
                 .setString(ACCOUNT_SECURITY_CODE_JSON, securityCode);
    }

    public String getAccountHolder() {
        return accountHolder;
    }

    public GregorianCalendar getExpirationDate() {
        return expirationDate;
    }

    public String getSecurityCode() {
        return securityCode;
    }
}
