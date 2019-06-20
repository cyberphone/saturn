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
package org.webpki.saturn.bank;

import java.math.BigDecimal;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;

////////////////////////////////////////////////////////////////////////////////////////////////
// Withdraw an amount from the database                                                       //
////////////////////////////////////////////////////////////////////////////////////////////////

public class WithDrawFromAccount {
    
    int result;
    int transactionId;

    public int getResult() {
        return result;
    }

    public int getTransactionId() {
        return transactionId;
    }

    public WithDrawFromAccount(BigDecimal amount, 
                               String accountId,
                               TransactionTypes transactionType,
                               Connection connection) throws SQLException {
/*
        CREATE PROCEDURE ExternalWithDrawSP (OUT p_Error INT,
                                             OUT p_TransactionId INT,
                                             IN p_OptionalOriginator VARCHAR(50),
                                             IN p_OptionalReference VARCHAR(50),
                                             IN p_TransactionType INT,
                                             IN p_Amount DECIMAL(8,2),
                                             IN p_CredentialId VARCHAR(30))
*/
        CallableStatement stmt = connection.prepareCall("{call ExternalWithDrawSP(?, ?, ?, ?, ?, ?, ?)}");
        stmt.registerOutParameter(1, java.sql.Types.INTEGER);
        stmt.registerOutParameter(2, java.sql.Types.INTEGER);
        stmt.setString(3, "demoblaha");
        stmt.setString(4, null);
        stmt.setInt(5, transactionType.getIntValue());
        stmt.setBigDecimal(6, amount);
        stmt.setString(7, accountId);
        stmt.execute();
        int result = stmt.getInt(1);
        if (result == 0) {
            transactionId = stmt.getInt(2);
        } else if (result != 4) {
            stmt.close ();
            throw new SQLException("Unknown account/credential: " + accountId);
        }
        stmt.close ();
    }
}
