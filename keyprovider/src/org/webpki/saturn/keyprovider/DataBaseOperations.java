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
package org.webpki.saturn.keyprovider;

import java.io.IOException;

import java.security.GeneralSecurityException;
import java.security.PublicKey;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.webpki.crypto.HashAlgorithms;

import org.webpki.saturn.common.Currencies;

import org.webpki.webutil.DNSReverseLookup;

public class DataBaseOperations {

    static Logger logger = Logger.getLogger(DataBaseOperations.class.getCanonicalName());
    
    static int createUser(String userName, String clientIpAddress) throws SQLException {
        try {
/*
            CREATE PROCEDURE CreateUserSP (OUT p_UserId INT,
                                           IN p_UserName VARCHAR(50),
                                           IN p_ClientIpAddress VARCHAR(50))
*/
            try (Connection connection = KeyProviderService.jdbcDataSource.getConnection();
                 CallableStatement stmt = 
                    connection.prepareCall("{call CreateUserSP(?,?,?)}");) {
                stmt.registerOutParameter(1, java.sql.Types.INTEGER);
                stmt.setString(2, userName);
                stmt.setString(3, clientIpAddress);
                stmt.execute();
                int userId = stmt.getInt(1);
                
                // Potentially very slow operation, perform it in the background!
                new Thread(new Runnable() {
            
                    @Override
                    public void run() {
                        try {
                            String host = DNSReverseLookup.getHostName(clientIpAddress);
                            if (host.equals(clientIpAddress)) {
                                return;
                            }
                            try (PreparedStatement stmt = 
                                    KeyProviderService.jdbcDataSource.getConnection().prepareStatement(
                                        "UPDATE USERS SET ClientHost = ? WHERE Id = ?;");) {
                                stmt.setString(1, host);
                                stmt.setInt(2, userId);
                                stmt.executeUpdate();
                            }
                        } catch (SQLException | IOException | InterruptedException e) {
                            logger.log(Level.SEVERE, "Database problem", e);
                            throw new RuntimeException(e);
                        }
                    }
                   
                }).start(); 

                return userId;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Database problem", e);
            throw e;
        }            
    }
    
    static class AccountAndCredential {
        String accountId;
        String credentialId;
        Currencies currency;
    }

    static AccountAndCredential createAccountAndCredential(int userId,
                                                           String accountType,
                                                           String paymentMethodUrl,
                                                           PublicKey payReq,
                                                           PublicKey optionalBalReq) 
    throws SQLException, IOException, GeneralSecurityException {
        try {
/*
        CREATE PROCEDURE CreateAccountAndCredentialSP (OUT p_AccountId VARCHAR(30),
                                                       OUT p_CredentialId INT,
                                                       OUT p_Currency CHAR(3),
                                                       IN p_UserId INT, 
                                                       IN p_AccountTypeName VARCHAR(20),
                                                       IN p_PaymentMethodUrl VARCHAR(50),
                                                       IN p_S256PayReq BINARY(32),
                                                       IN p_S256BalReq BINARY(32))
*/
            try (Connection connection = KeyProviderService.jdbcDataSource.getConnection();
                 CallableStatement stmt = 
                    connection.prepareCall("{call CreateAccountAndCredentialSP(?,?,?,?,?,?,?,?)}");) {
                stmt.registerOutParameter(1, java.sql.Types.VARCHAR);
                stmt.registerOutParameter(2, java.sql.Types.INTEGER);
                stmt.registerOutParameter(3, java.sql.Types.VARCHAR);
                stmt.setInt(4, userId);
                stmt.setString(5, accountType);
                stmt.setString(6, paymentMethodUrl);
                stmt.setBytes(7, s256(payReq));
                stmt.setBytes(8, s256(optionalBalReq));
                stmt.execute();
                AccountAndCredential accountAndCredential = new AccountAndCredential();
                accountAndCredential.accountId = stmt.getString(1);
                accountAndCredential.credentialId = String.valueOf(stmt.getString(2));
                accountAndCredential.currency = Currencies.valueOf(stmt.getString(3));
                return accountAndCredential;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Database problem", e);
            throw e;
        }            
    }

    private static byte[] s256(PublicKey publicKey) throws IOException, GeneralSecurityException {
        return publicKey == null ? null : HashAlgorithms.SHA256.digest(publicKey.getEncoded());
    }

    static void testConnection() throws SQLException {
        try (Connection connection = KeyProviderService.jdbcDataSource.getConnection();) { }
    }
}
