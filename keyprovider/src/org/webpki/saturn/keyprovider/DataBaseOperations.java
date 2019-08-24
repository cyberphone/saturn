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

import java.io.IOException;

import java.security.PublicKey;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.webpki.crypto.HashAlgorithms;

public class DataBaseOperations {

    static Logger logger = Logger.getLogger(KeyProviderService.class.getCanonicalName());
    
    public static int createUser(String userName) throws SQLException {
        try {
/*
            CREATE PROCEDURE CreateUserSP (OUT p_UserId INT,
                                           IN p_Name VARCHAR(50))
*/
            int userId;
            try (Connection connection = KeyProviderService.jdbcDataSource.getConnection();
                 CallableStatement stmt = 
                    connection.prepareCall("{call CreateUserSP(?,?)}");) {
                stmt.registerOutParameter(1, java.sql.Types.INTEGER);
                stmt.setString(2, userName);
                stmt.execute();
                userId = stmt.getInt(1);
            }
            return userId;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Database problem", e);
            throw e;
        }            
    }

    public static String createAccountAndCredential(int userId,
                                                    int accountType,
                                                    String methodUri,
                                                    PublicKey payReq,
                                                    PublicKey optionalBalReq) 
    throws SQLException, IOException {
        try {
/*
            CREATE PROCEDURE CreateAccountAndCredentialSP (OUT p_CredentialId VARCHAR(30),
                                                           IN p_UserId INT, 
                                                           IN p_AccountType INT,
                                                           IN p_MethodUri VARCHAR(50),
                                                           IN p_S256PayReq BINARY(32),
                                                           IN p_S256BalReq BINARY(32))
*/
            String credentialId;
            try (Connection connection = KeyProviderService.jdbcDataSource.getConnection();
                 CallableStatement stmt = 
                    connection.prepareCall("{call CreateAccountAndCredentialSP(?,?,?,?,?,?)}");) {
                stmt.registerOutParameter(1, java.sql.Types.VARCHAR);
                stmt.setInt(2, userId);
                stmt.setInt(3, accountType);
                stmt.setString(4, methodUri);
                stmt.setBytes(5, s256(payReq));
                stmt.setBytes(6, s256(optionalBalReq));
                stmt.execute();
                credentialId = stmt.getString(1);
            }
            return credentialId;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Database problem", e);
            throw e;
        }            
    }

    private static byte[] s256(PublicKey publicKey) throws IOException {
        return publicKey == null ? null : HashAlgorithms.SHA256.digest(publicKey.getEncoded());
    }
}
