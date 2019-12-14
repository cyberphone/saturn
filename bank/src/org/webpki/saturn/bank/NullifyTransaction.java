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

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;

////////////////////////////////////////////////////////////////////////////////////////////////
// Restore the account after a failed inter-banking request                                   //
////////////////////////////////////////////////////////////////////////////////////////////////

public class NullifyTransaction {
    
    public NullifyTransaction(int failedTransactionId, 
                              Connection connection) throws SQLException {
/*
        CREATE PROCEDURE NullifyTransactionSP (OUT p_Error INT, IN p_FailedTransactionId INT)
*/
        try (CallableStatement stmt = 
                connection.prepareCall("{call NullifyTransactionSP(?, ?)}");) {
            stmt.registerOutParameter(1, java.sql.Types.INTEGER);
            stmt.setInt(2, failedTransactionId);
            stmt.execute();
            int result = stmt.getInt(1);
            if (result != 0) {
                throw new SQLException("NullifyTransactionSP returned: " + result);
            }
        }
    }
}
