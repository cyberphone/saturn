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
                               String payeeCommonName,
                               String payeeReference,
                               Integer optionalRoundtripId,
                               boolean throwOnOutOfFounds,
                               Connection connection) throws SQLException {
/*
        CREATE PROCEDURE ExternalWithDrawSP (OUT p_Error INT,
                                             OUT p_TransactionId INT,
                                             IN p_OptionalOriginator VARCHAR(50),
                                             IN p_OptionalExtReference VARCHAR(50),
                                             IN p_TransactionType INT,
                                             IN p_OptionalRoundtripId INT,
                                             IN p_Amount DECIMAL(8,2),
                                             IN p_CredentialId VARCHAR(30))
*/
        try (CallableStatement stmt = 
                connection.prepareCall("{call ExternalWithDrawSP(?, ?, ?, ?, ?, ?, ?, ?)}");) {
            stmt.registerOutParameter(1, java.sql.Types.INTEGER);
            stmt.registerOutParameter(2, java.sql.Types.INTEGER);
            stmt.setString(3, payeeCommonName);
            stmt.setString(4, payeeReference);
            stmt.setInt(5, transactionType.getIntValue());
            if (optionalRoundtripId == null) {
                stmt.setNull(6, java.sql.Types.INTEGER);
            } else {
                stmt.setInt(6, optionalRoundtripId);
            }
            stmt.setBigDecimal(7, amount);
            stmt.setString(8, accountId);
            stmt.execute();
            switch (result = stmt.getInt(1)) {
                case 0:
                    transactionId = stmt.getInt(2);
                break;
                
                case 1:
                    throw new SQLException("Unknown account/credential: " + accountId);
                    
                case 4:
                    if (throwOnOutOfFounds) {
                        throw new SQLException("Out of funds");
                    }
                    break;

                default:
                    throw new SQLException("Withdraw returned: " + result);
            }
        }
    }
}
