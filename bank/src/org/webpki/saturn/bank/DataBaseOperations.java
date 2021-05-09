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

import java.io.IOException;

import java.math.BigDecimal;

import java.security.GeneralSecurityException;
import java.security.PublicKey;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;

import java.util.HashMap;

import java.util.logging.Logger;

import org.webpki.crypto.HashAlgorithms;

import org.webpki.saturn.common.Currencies;
import org.webpki.saturn.common.PaymentMethods;
import org.webpki.saturn.common.TransactionTypes;

// Common class for SQL database operations                                             //

public class DataBaseOperations {
    
    private DataBaseOperations() {}

    static Logger logger = Logger.getLogger(DataBaseOperations.class.getCanonicalName());

    // Speed-up solutions, looking up static data every time seems unnecessary
    private static HashMap<PaymentMethods,Integer> paymentMethod2DbInt = new HashMap<>();
    
    private static HashMap<TransactionTypes,Integer> transactionType2DbInt = new HashMap<>();

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Authenticate user authorization using a stored procedure                                   //
    ////////////////////////////////////////////////////////////////////////////////////////////////
    static String authenticateAuthorization(String credentialId,
                                            String accountId,
                                            PaymentMethods paymentMethod,
                                            PublicKey authorizationKey,
                                            Connection connection) 
    throws SQLException, IOException, NormalException, GeneralSecurityException {
/*
        CREATE PROCEDURE AuthenticatePayReqSP (OUT p_Error INT,
                                               OUT p_UserName VARCHAR(50),
                                               IN p_CredentialId INT,
                                               IN p_AccountId VARCHAR(30),
                                               IN p_PaymentMethodId INT,
                                               IN p_S256AuthKey BINARY(32))
*/
        try (CallableStatement stmt = 
                connection.prepareCall("{call AuthenticatePayReqSP(?,?,?,?,?,?)}");) {
            stmt.registerOutParameter(1, java.sql.Types.INTEGER);
            stmt.registerOutParameter(2, java.sql.Types.VARCHAR);
            stmt.setInt(3, Integer.parseInt(credentialId));
            stmt.setString(4, accountId);
            stmt.setInt(5, paymentMethod2DbInt.get(paymentMethod));
            stmt.setBytes(6, s256(authorizationKey));
            stmt.execute();
            switch (stmt.getInt(1)) {
                case 0:
                    return stmt.getString(2);

                case 1:
                    logger.severe("No such credential ID: " + credentialId);
                    throw new NormalException("No such user credential");

                case 2:
                    logger.severe("No such account ID: " + accountId);
                    throw new NormalException("No such user account");

                case 4:
                    logger.severe("Wrong payment method: " + paymentMethod.toString() + " for account ID: " + accountId);
                    throw new NormalException("Wrong payment method");

                default:
                    logger.severe("Wrong public key for account ID: " + accountId);
                    throw new NormalException("Wrong user public key");
            }
        }
    }

    
    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Withdraw an amount from the account using a stored procedure                               //
    ////////////////////////////////////////////////////////////////////////////////////////////////
    static int externalWithDraw(BigDecimal amount,
                                String accountId,
                                TransactionTypes transactionType,
                                String payeeAccount,
                                String payeeName,
                                String payeeReference,
                                Integer optionalReservationId,
                                boolean throwOnOutOfFounds,
                                Connection connection) throws SQLException {
/*
        CREATE PROCEDURE ExternalWithDrawSP (OUT p_Error INT,
                                             OUT p_TransactionId INT,
                                             IN p_PayeeAccount VARCHAR(50),
                                             IN p_OptionalPayeeName VARCHAR(50),
                                             IN p_OptionalPayeeReference VARCHAR(50),
                                             IN p_TransactionTypeId INT,
                                             IN p_OptionalReservationId INT,
                                             IN p_Amount DECIMAL(8,2),
                                             IN p_AccountId VARCHAR(30))
*/
        try (CallableStatement stmt = 
                connection.prepareCall("{call ExternalWithDrawSP(?,?,?,?,?,?,?,?,?)}");) {
            stmt.registerOutParameter(1, java.sql.Types.INTEGER);
            stmt.registerOutParameter(2, java.sql.Types.INTEGER);
            stmt.setString(3, payeeAccount);
            stmt.setString(4, payeeName);
            stmt.setString(5, payeeReference);
            stmt.setInt(6, transactionType2DbInt.get(transactionType));
            if (optionalReservationId == null) {
                stmt.setNull(7, java.sql.Types.INTEGER);
            } else {
                stmt.setInt(7, optionalReservationId);
            }
            stmt.setBigDecimal(8, amount);
            stmt.setString(9, accountId);
            stmt.execute();
            int result = stmt.getInt(1);
            switch (result) {
                case 0:
                    return stmt.getInt(2);
                 
                case 4:
                    if (throwOnOutOfFounds) {
                        throw new SQLException("Out of funds");
                    }
                    return 0;  // Incorrect transaction ID => soft fail and rollbacked transaction

                default:
                    throw new SQLException("Withdraw returned: " + result);
            }
        }
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Something bad happened on the way so we must restore the balance to its previous state     //
    ////////////////////////////////////////////////////////////////////////////////////////////////
    static void nullifyTransaction(int failedTransactionId, 
                                   Connection connection) throws SQLException {
/*
        CREATE PROCEDURE NullifyTransactionSP (OUT p_Error INT, 
                                               IN p_FailedTransactionId INT)
*/
        try (CallableStatement stmt = connection.prepareCall("{call NullifyTransactionSP(?,?)}");) {
            stmt.registerOutParameter(1, java.sql.Types.INTEGER);
            stmt.setInt(2, failedTransactionId);
            stmt.execute();
            int result = stmt.getInt(1);
            if (result != 0) {
                throw new SQLException("NullifyTransactionSP returned: " + result);
            }
        }
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Authenticate and perform a balance request using a stored procedure                                   //
    ////////////////////////////////////////////////////////////////////////////////////////////////
    static BigDecimal requestAccountBalance(String credentialId,
                                            String accountId,
                                            PublicKey balanceKey,
                                            Currencies currency,
                                            Connection connection) 
    throws SQLException, IOException, NormalException, GeneralSecurityException {
/*
        CREATE PROCEDURE RequestAccountBalanceSP (OUT p_Error INT,
                                                  OUT p_Balance DECIMAL(8,2),
                                                  IN p_CredentialId INT,
                                                  IN p_AccountId VARCHAR(30),
                                                  IN p_S256BalKey BINARY(32),
                                                  IN p_Currency CHAR(3))
*/
        try (CallableStatement stmt = 
                connection.prepareCall("{call RequestAccountBalanceSP(?,?,?,?,?,?)}");) {
            stmt.registerOutParameter(1, java.sql.Types.INTEGER);
            stmt.registerOutParameter(2, java.sql.Types.DECIMAL);
            stmt.setInt(3, Integer.parseInt(credentialId));
            stmt.setString(4, accountId);
            stmt.setBytes(5, s256(balanceKey));
            stmt.setString(6, currency.toString());
            stmt.execute();
            switch (stmt.getInt(1)) {
                case 0:
                    return stmt.getBigDecimal(2);

                case 1:
                    logger.severe("No such credential ID: " + credentialId);
                    throw new NormalException("No such user credential");

                case 2:
                    logger.severe("No such account ID: " + accountId);
                    throw new NormalException("No such user account");

                case 5:
                    logger.severe("Wrong currency: " + currency.toString() + " for account ID: " + accountId);
                    throw new NormalException("Wrong currency");

                default:
                    logger.severe("Wrong public key for account ID: " + accountId);
                    throw new NormalException("Wrong user public key");
            }
        }
    }

    
    private static int init(Connection connection,
                            String functionName,
                            String name) throws SQLException {
        try (CallableStatement stmt = 
         connection.prepareCall("{? = call " + functionName + "(?)}");) {
            stmt.registerOutParameter(1, java.sql.Types.INTEGER);
            stmt.setString(2, name);
            stmt.execute();
            int outputValue = stmt.getInt(1);
            if (outputValue == 0) {
                throw new SQLException("Failed getting a value for: " + name);
            }
            return outputValue;
        }
    }

    private static byte[] s256(PublicKey publicKey) throws IOException, GeneralSecurityException {
        return publicKey == null ? null : HashAlgorithms.SHA256.digest(publicKey.getEncoded());
    }
    
    // Run once to set up constants to match the database
    static void initiateStaticTypes(Connection connection) throws SQLException {
        for (TransactionTypes transactionType : TransactionTypes.values()) {
            transactionType2DbInt.put(transactionType,
                    init(connection, 
                         "GetTransactionTypeId",
                         transactionType.toString()));
        }
        for (PaymentMethods paymentMethod : PaymentMethods.values()) {
            paymentMethod2DbInt.put(paymentMethod,  
                    init(connection, 
                         "GetPaymentMethodId", 
                         paymentMethod.getPaymentMethodUrl()));
        }
    }
}
