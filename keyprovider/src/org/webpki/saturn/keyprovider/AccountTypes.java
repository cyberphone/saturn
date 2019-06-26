/*
 *  Copyright 2015-2018 WebPKI.org (http://webpki.org).
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
package org.webpki.saturn.keyprovider;

import java.sql.Connection;

import org.webpki.saturn.common.EnumDataBase;

public enum AccountTypes implements EnumDataBase {

    CREDIT_CARD_ACCOUNT(),
    STANDARD_ACCOUNT(),
    NEW_USER_ACCOUNT();

    AccountTypes () {
    }

    private static int[] dataBaseValues = new int[values().length];

    @Override
    public int getIntValue() {
        return dataBaseValues[ordinal()];
    }

    public static void init(Connection connection) throws Exception {
        for (AccountTypes accountTypes : AccountTypes.values()) {
            dataBaseValues[accountTypes.ordinal()] = 
                    EnumDataBase.init(connection, 
                                      "GetAccountTypeSP",
                                      accountTypes.toString());
        }
    }
}
