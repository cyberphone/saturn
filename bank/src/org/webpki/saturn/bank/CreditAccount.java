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
package org.webpki.saturn.bank;

import java.math.BigDecimal;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;

////////////////////////////////////////////////////////////////////////////////////////////////
// Add an amount to the account using a stored procedure                                      //
////////////////////////////////////////////////////////////////////////////////////////////////

public class CreditAccount {
    
    private int result;
    private int transactionId;

    public int getResult() {
        return result;
    }

    public int getTransactionId() {
        return transactionId;
    }

    public CreditAccount(BigDecimal amount, 
                         String accountId,
                         String payeeAccount,
                         String payeeName,
                         String payeeReference,
                         Connection connection) throws SQLException {
/*
CREATE PROCEDURE CreditAccountSP (OUT p_Error INT,
                                  OUT p_TransactionId INT,
                                  IN p_PayeeAccount VARCHAR(50),
                                  IN p_OptionalPayeeName VARCHAR(50),
                                  IN p_OptionalPayeeReference VARCHAR(50),
                                  IN p_Amount DECIMAL(8,2),
                                  IN p_CredentialId VARCHAR(30))
*/
        try (CallableStatement stmt = 
                connection.prepareCall("{call CreditAccountSP(?, ?, ?, ?, ?, ?, ?)}");) {
            stmt.registerOutParameter(1, java.sql.Types.INTEGER);
            stmt.registerOutParameter(2, java.sql.Types.INTEGER);
            stmt.setString(3, payeeAccount);
            stmt.setString(4, payeeName);
            stmt.setString(5, payeeReference);
            stmt.setBigDecimal(6, amount);
            stmt.setString(7, accountId);
            stmt.execute();
            switch (result = stmt.getInt(1)) {
                case 0:
                    transactionId = stmt.getInt(2);
                break;
                
                case 1:
                    throw new SQLException("Unknown account/credential: " + accountId);
                    
                default:
                    throw new SQLException("Credit account returned: " + result);
            }
        }
    }
}
