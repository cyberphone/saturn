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

-- SQL Script for MySQL 5.7
--
-- root privileges are required!!!
--
-- Clear and create DB to begin with
--
DROP DATABASE IF EXISTS PAYER_BANK;
CREATE DATABASE PAYER_BANK CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
--
-- Create our single user
--
DROP USER IF EXISTS saturn@localhost;
CREATE USER saturn@localhost IDENTIFIED BY 'foo123';
--
-- Give this user access
--
GRANT ALL ON PAYER_BANK.* TO saturn@localhost;
GRANT SELECT ON mysql.proc TO saturn@localhost;
--
-- Create tables and stored procedures
--
-- #############################################################
-- # This is the Payer side of a PoC database holding data for #
-- # Users, Accounts, Credentials and Transactions             #
-- #############################################################

USE PAYER_BANK;

/*=============================================*/
/*                   USERS                     */
/*=============================================*/

CREATE TABLE USERS
  (
    Id          INT           NOT NULL  AUTO_INCREMENT,                  -- Unique User ID

    Created     TIMESTAMP     NOT NULL  DEFAULT CURRENT_TIMESTAMP,       -- Administrator data

    AccessCount INT           NOT NULL  DEFAULT 0,                       -- Administrator data

    LastAccess  TIMESTAMP     NULL,                                      -- Administrator data

    Name        VARCHAR(50)   NOT NULL,                                  -- Human name

    PRIMARY KEY (Id)
  );


/*=============================================*/
/*                 ACCOUNT_TYPES               */
/*=============================================*/

CREATE TABLE ACCOUNT_TYPES
  (
    Id          INT           NOT NULL  AUTO_INCREMENT,                  -- Unique Type ID

    Name        VARCHAR(20)   NOT NULL,                                  -- As as symbolic name

    Description VARCHAR(50)   NOT NULL  UNIQUE,                          -- Description

    CappedAt    DECIMAL(8,2)  NOT NULL,                                  -- Capped at

    PRIMARY KEY (Id)
  );


/*=============================================*/
/*                 ACCOUNTS                    */
/*=============================================*/

CREATE TABLE ACCOUNTS
  (
    Id          INT           NOT NULL  AUTO_INCREMENT,                  -- Unique (internal) Account ID

-- One could imagine a more flexible arrangement supporting multiple owners
-- but this seems to be out of scope for a proof-of-concept design.
    UserId      INT           NOT NULL,                                  -- User (account holder) reference

    AccountTypeId INT         NOT NULL,                                  -- Account Type reference

    Created     TIMESTAMP     NOT NULL  DEFAULT CURRENT_TIMESTAMP,       -- Administrator data

    Balance     DECIMAL(8,2)  NOT NULL,                                  -- Currently available

    Currency    CHAR(3)       NOT NULL  DEFAULT "EUR",                   -- SEK, USD, EUR etc.

    PRIMARY KEY (Id),
    FOREIGN KEY (AccountTypeId) REFERENCES ACCOUNT_TYPES(Id),
    FOREIGN KEY (UserId) REFERENCES USERS(Id) ON DELETE CASCADE
  ) AUTO_INCREMENT=200500123;


/*=============================================*/
/*              PAYMENT_METHODS                */
/*=============================================*/

CREATE TABLE PAYMENT_METHODS
  (
    Id          INT           NOT NULL  AUTO_INCREMENT,                  -- Unique Payment Method ID

    Name        VARCHAR(50)   NOT NULL  UNIQUE,                          -- Payment method (URL)

    Format      VARCHAR(80)   NOT NULL,                                  -- Account syntax

    Created     TIMESTAMP     NOT NULL  DEFAULT CURRENT_TIMESTAMP,       -- Administrator data

    PRIMARY KEY (Id)
  );


/*=============================================*/
/*                CREDENTIALS                  */
/*=============================================*/

CREATE TABLE CREDENTIALS
  (

    Id          INT           NOT NULL  AUTO_INCREMENT,                  -- Unique Credential ID

-- Note: an AccountId is an external representation of an InternalAccountId
-- like an IBAN or Card Number.  Their contents are (database-wise)
-- independent of the actual Account IDs.  This means that a bank
-- account may serve as a Card account as well as SEPA account

    AccountId VARCHAR(30)     NOT NULL,                                  -- May be shared with other users 

    InternalAccountId INT     NOT NULL,                                  -- Account Reference

    PaymentMethodId INT       NOT NULL,                                  -- Payment Method reference

    Created     TIMESTAMP     NOT NULL  DEFAULT CURRENT_TIMESTAMP,       -- Administrator data

-- Authentication of user authorization signatures is performed
-- by verifying that both SHA256 of the public key (in X.509 DER
-- format) and claimed Id (Card number, IBAN) match.

    S256PayReq  BINARY(32)    NOT NULL,                                  -- Payment request key hash 

    S256BalReq  BINARY(32)    NULL,                                      -- Optional: balance key hash 

    PRIMARY KEY (Id),
    FOREIGN KEY (PaymentMethodId) REFERENCES PAYMENT_METHODS(Id),
    FOREIGN KEY (InternalAccountId) REFERENCES ACCOUNTS(Id) ON DELETE CASCADE
  ) AUTO_INCREMENT=100000000;
  
CREATE INDEX SpeedUpAccountId ON CREDENTIALS (AccountId);


/*=============================================*/
/*             TRANSACTION_TYPES               */
/*=============================================*/

CREATE TABLE TRANSACTION_TYPES
  (
    Id          INT           NOT NULL  AUTO_INCREMENT,                  -- Unique Transaction Type ID

    Name        VARCHAR(20)   NOT NULL,                                  -- As as symbolic name

    Description VARCHAR(80)   NOT NULL,                                  -- A bit more on the topic

    PRIMARY KEY (Id)
  );


/*=============================================*/
/*               TRANSACTIONS                  */
/*=============================================*/

CREATE TABLE TRANSACTIONS
  (
    Id          INT           NOT NULL  UNIQUE,                          -- Unique Transaction ID

    InternalAccountId INT     NOT NULL,                                  -- Referring to an Account

    CredentialId INT,                                                    /* Optional Credential reference
                                                                            linked to the Account ID */

    ReservationId INT,                                                   /* Reservation Transaction ID
                                                                            linked to a previous
                                                                            Transaction ID */

    TransactionTypeId INT     NOT NULL,                                  -- Transaction Type reference

    Amount      DECIMAL(8,2)  NOT NULL,                                  -- The Amount involved

    Balance     DECIMAL(8,2)  NOT NULL,                                  -- Outgoing Account balance
    
    PayeeAccount VARCHAR(50)  NOT NULL,                                  -- Destination account ID

    PayeeName  VARCHAR(50),                                              -- Optional Payee name

    PayeeReference VARCHAR(50),                                          /* Optional Payee reference
                                                                            like internal order ID */

    Created     TIMESTAMP     NOT NULL  DEFAULT CURRENT_TIMESTAMP,       -- Administrator data

    PRIMARY KEY (Id),
    FOREIGN KEY (TransactionTypeId) REFERENCES TRANSACTION_TYPES(Id),
    FOREIGN KEY (InternalAccountId) REFERENCES ACCOUNTS(Id) ON DELETE CASCADE
  );


/*=============================================*/
/*            TRANSACTION_COUNTER              */
/*=============================================*/

CREATE TABLE TRANSACTION_COUNTER
  (
-- MySQL 5.7 auto increment is unreliable for this application since it
-- loses track for power fails if TRANSACTIONS are emptied like in the demo.

-- Therefore we use a regular table with a single column and row.

    Next        INT           NOT NULL                                   -- Monotonic counter

  );

INSERT INTO TRANSACTION_COUNTER(Next) VALUES(100345078);

DELIMITER //


-- This particular implementation builds on creating the pending transaction ID
-- before the transaction is performed.  There are pros and cons with all such
-- schemes. This one leave "holes" in the sequence for failed transactions.

CREATE FUNCTION GetNextTransactionIdSP () RETURNS INT
  BEGIN
     UPDATE TRANSACTION_COUNTER SET Next = LAST_INSERT_ID(Next + 1) LIMIT 1;
     RETURN LAST_INSERT_ID();
  END
//

CREATE PROCEDURE CreateUserSP (OUT p_UserId INT,
                               IN p_Name VARCHAR(50))
  BEGIN
    INSERT INTO USERS(Name) VALUES(p_Name);
    SET p_UserId = LAST_INSERT_ID();
  END
//

CREATE PROCEDURE CreateAccountTypeSP (IN p_AccountTypeName VARCHAR(20),
                                      IN p_Description VARCHAR(50),
                                      IN p_CappedAt DECIMAL(8,2))
  BEGIN
    INSERT INTO ACCOUNT_TYPES(Name, Description, CappedAt)
        VALUES(p_AccountTypeName, p_Description, p_CappedAt);
  END
//

CREATE PROCEDURE CreatePaymentMethodSP (IN p_MethodUrl VARCHAR(50),
                                        IN p_Format VARCHAR(80))
  BEGIN
    INSERT INTO PAYMENT_METHODS(Name, Format)
        VALUES(p_MethodUrl, p_Format);
  END
//

CREATE PROCEDURE CreateTransactionTypeSP (IN p_TransactionTypeName VARCHAR(20),
                                          IN p_Description VARCHAR(80))
  BEGIN
    INSERT INTO TRANSACTION_TYPES(Name, Description)
        VALUES(p_TransactionTypeName, p_Description);
  END
//

CREATE FUNCTION GetTransactionTypeId (p_TransactionTypeName VARCHAR(20)) RETURNS INT
DETERMINISTIC
  BEGIN
    DECLARE v_Id INT;

    SELECT TRANSACTION_TYPES.Id INTO v_Id FROM TRANSACTION_TYPES
        WHERE TRANSACTION_TYPES.Name = p_TransactionTypeName;
    RETURN v_Id;
  END
//

CREATE FUNCTION GetPaymentMethodId (p_PaymentMetodUrl VARCHAR(50)) RETURNS INT
DETERMINISTIC
  BEGIN
    DECLARE v_Id INT;

    SELECT PAYMENT_METHODS.Id INTO v_Id FROM PAYMENT_METHODS
        WHERE PAYMENT_METHODS.Name = p_PaymentMetodUrl;
    RETURN v_Id;
  END
//

CREATE FUNCTION GetAccountTypeId (p_AccountTypeName VARCHAR(20)) RETURNS INT
DETERMINISTIC
  BEGIN
    DECLARE v_Id INT;

    SELECT ACCOUNT_TYPES.Id INTO v_Id FROM ACCOUNT_TYPES
        WHERE ACCOUNT_TYPES.Name = p_AccountTypeName;
    RETURN v_Id;
  END
//

CREATE FUNCTION FRENCH_IBAN (p_InternalAccountId INT) RETURNS VARCHAR(30)
DETERMINISTIC
  BEGIN

-- https://fr.wikipedia.org/wiki/Cl%C3%A9_RIB
-- Hard coded financial institution and branch office (LCL)

    set @chk = LPAD(CONVERT(97 - (2836843 + 3 * p_InternalAccountId) % 97, CHAR), 2, '0');
    set @bban = CONCAT('3000211111', LPAD(CONVERT(p_InternalAccountId, DECIMAL(11)), 11, '0'), @chk);
    set @key = LPAD(CONVERT(98 - (CONVERT(CONCAT(@bban, '152700'), DECIMAL(30)) % 97), CHAR), 2, '0');
    RETURN CONCAT('FR', @key, @bban);
  END
//

CREATE FUNCTION CREDIT_CARD (p_IIN VARCHAR(8),
                             p_InternalAccountId INT,
                             p_AccountDigits INT) RETURNS VARCHAR(30)
DETERMINISTIC
  BEGIN

-- https://gefvert.org/blog/archives/57
-- Luhn number calculation

    DECLARE i, s, r, weight INT;
    DECLARE baseNumber VARCHAR(16);
 
    SET baseNumber = CONCAT(p_IIN, LPAD(CONVERT(p_InternalAccountId, DECIMAL(11)), p_AccountDigits, '0'));
    SET weight = 2;
    SET s = 0;
    SET i = LENGTH(baseNumber);
 
    WHILE i > 0 DO
      SET r = SUBSTRING(baseNumber, i, 1) * weight;
      SET s = s + IF(r > 9, r - 9, r);
      SET i = i - 1;
      SET weight = 3 - weight;
    END WHILE;
    RETURN CONCAT(baseNumber, (10 - s % 10) % 10);
  END 
//

CREATE PROCEDURE CreateAccountSP (OUT p_InternalAccountId INT,
                                  IN p_UserId INT, 
                                  IN p_AccountTypeId INT)
  BEGIN
    INSERT INTO ACCOUNTS(UserId, AccountTypeId, Balance)
        SELECT p_UserId, p_AccountTypeId, ACCOUNT_TYPES.CappedAt 
        FROM ACCOUNT_TYPES WHERE Id = p_AccountTypeId;
    SET p_InternalAccountId = LAST_INSERT_ID();
  END
//

CREATE PROCEDURE CreateDemoCredentialSP (OUT p_CredentialId INT,
                                         OUT p_AccountId VARCHAR(30),
                                         IN p_InternalAccountId INT, 
                                         IN p_PaymentMethodUrl VARCHAR(50),
                                         IN p_S256PayReq BINARY(32),
                                         IN p_S256BalReq BINARY(32))
  BEGIN
    SELECT Id, Format INTO @paymentMethodId, @format FROM PAYMENT_METHODS
        WHERE PAYMENT_METHODS.Name = p_PaymentMethodUrl;
    SET @accountNumber = p_InternalAccountId;
    SET @sql = CONCAT('SET @accountId = ', @format);
    PREPARE stmt FROM @sql;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
    INSERT INTO CREDENTIALS(InternalAccountId, 
                            AccountId,
                            S256PayReq,
                            S256BalReq,
                            PaymentMethodId) 
        VALUES(p_InternalAccountId,
               @accountId,
               p_S256PayReq,
               p_S256BalReq,
               @paymentMethodId);
    SET p_CredentialId = LAST_INSERT_ID();
    SET p_AccountId = @accountId; 
  END
//

CREATE PROCEDURE CreateAccountAndCredentialSP (OUT p_AccountId VARCHAR(30),
                                               OUT p_CredentialId INT,
                                               IN p_UserId INT, 
                                               IN p_AccountTypeName VARCHAR(20),
                                               IN p_PaymentMethodUrl VARCHAR(50),
                                               IN p_S256PayReq BINARY(32),
                                               IN p_S256BalReq BINARY(32))
  BEGIN
    CALL CreateAccountSP(@accountNumber, p_UserId, GetAccountTypeId(p_AccountTypeName));
    SELECT Id, Format INTO @paymentMethodId, @format FROM PAYMENT_METHODS
        WHERE PAYMENT_METHODS.Name = p_PaymentMethodUrl;
    SET @sql = CONCAT('SET @accountId = ', @format);
    PREPARE stmt FROM @sql;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
    INSERT INTO CREDENTIALS(InternalAccountId,
                            AccountId,
                            PaymentMethodId,
                            S256PayReq, 
                            S256BalReq) 
        VALUES(@accountNumber, 
               @accountId,
               @paymentMethodId,
               p_S256PayReq,
               p_S256BalReq);
    SET p_CredentialId = LAST_INSERT_ID();
    SET p_AccountId = @accountId;
  END
//

CREATE PROCEDURE AuthenticatePayReqSP (OUT p_Error INT,
                                       OUT p_UserName VARCHAR(50),
                                       IN p_CredentialId INT,
                                       IN p_AccountId VARCHAR(30),
                                       IN p_PaymentMethodId INT,
                                       IN p_S256PayReq BINARY(32))
  BEGIN
    DECLARE v_UserId INT;
    DECLARE v_AccountId VARCHAR(30);
    DECLARE v_S256PayReq BINARY(32);
    DECLARE v_PaymentMethodId INT;
    DECLARE v_InternalAccountId INT;
    
    SELECT AccountId, S256PayReq, InternalAccountId, PaymentMethodId 
        INTO v_AccountId, v_S256PayReq, v_InternalAccountId, v_PaymentMethodId
        FROM CREDENTIALS WHERE CREDENTIALS.Id = p_CredentialId;
    IF v_AccountId IS NULL THEN
      SET p_Error = 1;    -- No such credential
    ELSEIF v_AccountId <> p_AccountId THEN
      SET p_Error = 2;    -- Non-matching account
    ELSEIF v_S256PayReq <> p_S256PayReq THEN
      SET p_Error = 3;    -- Non-matching key
    ELSEIF v_PaymentMethodId <> IFNULL(p_PaymentMethodId,0) THEN
      SET p_Error = 4;    -- Non-matching payment method
    ELSE                       
      SET p_Error = 0;    -- Success => Update access info
      SELECT USERS.Id, USERS.Name INTO v_UserId, p_UserName FROM USERS
        INNER JOIN ACCOUNTS ON USERS.Id = ACCOUNTS.UserId
            WHERE ACCOUNTS.Id = v_InternalAccountId
            LIMIT 1;
      UPDATE USERS SET LastAccess = CURRENT_TIMESTAMP, AccessCount = AccessCount + 1
          WHERE USERS.Id = v_UserId;
    END IF;
  END
//

CREATE PROCEDURE AuthenticateBalReqSP (OUT p_Error INT,
                                       OUT p_Balance DECIMAL(8,2),
                                       IN p_CredentialId INT,
                                       IN p_S256BalReq BINARY(32))
  BEGIN
    DECLARE v_Balance DECIMAL(8,2);

    SELECT ACCOUNTS.Balance INTO v_Balance FROM ACCOUNTS 
        INNER JOIN CREDENTIALS ON ACCOUNTS.Id = CREDENTIALS.InternalAccountId
            WHERE CREDENTIALS.Id = p_CredentialId AND
                  CREDENTIALS.S256BalReq = p_S256BalReq
            LIMIT 1;
    IF v_Balance IS NULL THEN    -- Failed => Find reason
      IF EXISTS (SELECT * FROM CREDENTIALS WHERE CREDENTIALS.Id = p_CredentialId) THEN
        SET p_Error = 3;           -- Key does not match account
      ELSE
        SET p_Error = 1;           -- No such account
      END IF;
    ELSE
      SET p_Error = 0;           -- Success
    END IF;
    SET p_Balance = v_Balance;
  END
//

CREATE PROCEDURE ExternalWithDrawSP (OUT p_Error INT,
                                     OUT p_TransactionId INT,
                                     IN p_PayeeAccount VARCHAR(50),
                                     IN p_OptionalPayeeName VARCHAR(50),
                                     IN p_OptionalPayeeReference VARCHAR(50),
                                     IN p_TransactionTypeId INT,
                                     IN p_OptionalReservationId INT,
                                     IN p_Amount DECIMAL(8,2),
                                     IN p_AccountId VARCHAR(30))
  BEGIN
    DECLARE v_Balance DECIMAL(8,2);
    DECLARE v_NewBalance DECIMAL(8,2);
    DECLARE v_PreviousAmount DECIMAL(8,2);
    DECLARE v_InternalAccountId INT;
    DECLARE v_CredentialId INT;

    SELECT Id, InternalAccountId INTO v_CredentialId, v_InternalAccountId FROM CREDENTIALS
        WHERE CREDENTIALS.AccountId = p_AccountId
        LIMIT 1;
    SET p_Error = 0;
    START TRANSACTION;
    -- Lock the actual account record for updates
    SELECT Balance INTO v_Balance FROM ACCOUNTS
        WHERE ACCOUNTS.Id = v_InternalAccountId
        LIMIT 1
        FOR UPDATE;
    IF v_Balance IS NULL THEN      -- Failed
      SET p_Error = 1;             -- No such account
    ELSE
      SET v_NewBalance = v_Balance - p_Amount;
      IF p_OptionalReservationId IS NOT NULL THEN
        SELECT TRANSACTIONS.Amount INTO v_PreviousAmount FROM TRANSACTIONS
            WHERE TRANSACTIONS.InternalAccountId = v_InternalAccountId AND
                  TRANSACTIONS.Id = p_OptionalReservationId AND
                  TRANSACTIONS.TransactionTypeId = 2
            LIMIT 1;
        IF v_PreviousAmount IS NULL THEN
          SET p_Error = 5;         -- Reservation not found
        ELSEIF EXISTS (SELECT * FROM TRANSACTIONS
            WHERE TRANSACTIONS.ReservationId = p_OptionalReservationId) THEN
          SET p_Error = 6;         -- Reservation already used
        ELSE
          SET v_NewBalance = v_NewBalance - v_PreviousAmount;
        END IF;
      END IF;
      IF p_Error = 0 THEN
        IF v_NewBalance < 0 THEN
          SET p_Error = 4;         -- Out of funds
        ELSE                       -- Success => Withdraw the specified amount
          UPDATE ACCOUNTS SET Balance = v_NewBalance
              WHERE ACCOUNTS.Id = v_InternalAccountId;
          SET p_TransactionId = GetNextTransactionIdSP();
          INSERT INTO TRANSACTIONS(Id,
                                   TransactionTypeId, 
                                   InternalAccountId,
                                   Amount,
                                   Balance,
                                   CredentialId,
                                   PayeeAccount,
                                   PayeeName,
                                   PayeeReference,
                                   ReservationId)
               VALUES(p_TransactionId,
                      p_TransactionTypeId,
                      v_InternalAccountId,
                      -p_Amount,
                      v_NewBalance,
                      v_CredentialId,
                      p_PayeeAccount,
                      p_OptionalPayeeName,
                      p_OptionalPayeeReference,
                      p_OptionalReservationId);
        END IF;
      END IF;
    END IF;
    COMMIT;
    -- Perform the transaction if it was not rejected
    -- Unlock the account 
  END
//

CREATE PROCEDURE CreditAccountSP (OUT p_Error INT,
                                  OUT p_TransactionId INT,
                                  IN p_PayeeAccount VARCHAR(50),
                                  IN p_OptionalPayeeName VARCHAR(50),
                                  IN p_OptionalPayeeReference VARCHAR(50),
                                  IN p_Amount DECIMAL(8,2),
                                  IN p_AccountId VARCHAR(30))
  BEGIN
    DECLARE v_Balance DECIMAL(8,2);
    DECLARE v_NewBalance DECIMAL(8,2);
    DECLARE v_PreviousAmount DECIMAL(8,2);
    DECLARE v_InternalAccountId INT;

    SET p_Error = 0;
    START TRANSACTION;
    SELECT ACCOUNTS.Balance, ACCOUNTS.Id INTO v_Balance, v_InternalAccountId FROM ACCOUNTS
        INNER JOIN CREDENTIALS ON ACCOUNTS.Id = CREDENTIALS.InternalAccountId
        WHERE CREDENTIALS.AccountId = p_AccountId
        LIMIT 1
        FOR UPDATE;
    IF v_InternalAccountId IS NULL THEN    -- Failed
      SET p_Error = 1;             -- No such account
    ELSE
      SET v_NewBalance = v_Balance + p_Amount;
      UPDATE ACCOUNTS SET Balance = v_NewBalance
          WHERE ACCOUNTS.Id = v_InternalAccountId;
      SET p_TransactionId = GetNextTransactionIdSP();
      INSERT INTO TRANSACTIONS(Id,
                               TransactionTypeId, 
                               InternalAccountId,
                               Amount,
                               Balance,
                               PayeeAccount,
                               PayeeName,
                               PayeeReference)
           VALUES(p_TransactionId,
                  5,
                  v_InternalAccountId,
                  p_Amount,
                  v_NewBalance,
                  p_PayeeAccount,
                  p_OptionalPayeeName,
                  p_OptionalPayeeReference);
    END IF;
    COMMIT;
  END
//

-- Although it would (maybe) be cool doing all internal and external operations
-- as a super transaction, this also complicates the implementation. 
-- Therefore we rather do a "cleanup" of our internal data in case an external
-- operation failed.

CREATE PROCEDURE NullifyTransactionSP (OUT p_Error INT, IN p_FailedTransactionId INT)
  BEGIN
    DECLARE v_Balance DECIMAL(8,2);
    DECLARE v_Amount DECIMAL(8,2);
    DECLARE v_PreviousAmount DECIMAL(8,2);
    DECLARE v_NewBalance DECIMAL(8,2);
    DECLARE v_InternalAccountId INT;
    DECLARE v_OptionalReservationId INT;

    SET p_Error = 0;
    START TRANSACTION;
    SELECT ACCOUNTS.Balance, 
           ACCOUNTS.Id,
           TRANSACTIONS.Amount,
           TRANSACTIONS.ReservationId
        INTO 
           v_Balance, 
           v_InternalAccountId,
           v_Amount,
           v_OptionalReservationId
        FROM ACCOUNTS INNER JOIN TRANSACTIONS ON ACCOUNTS.Id = TRANSACTIONS.InternalAccountId
        WHERE TRANSACTIONS.Id = p_FailedTransactionId
        LIMIT 1
        FOR UPDATE;
    IF v_InternalAccountId IS NULL THEN    -- Failed
      SET p_Error = 1;             -- No such account
    ELSE
      SET v_NewBalance = v_Balance - v_Amount;
      IF v_OptionalReservationId IS NOT NULL THEN
        SELECT TRANSACTIONS.Amount INTO v_PreviousAmount FROM TRANSACTIONS
            WHERE TRANSACTIONS.Id = v_OptionalReservationId;
        SET v_NewBalance = v_NewBalance + v_PreviousAmount;
      END IF;
      UPDATE ACCOUNTS SET Balance = v_NewBalance WHERE ACCOUNTS.Id = v_InternalAccountId;
      INSERT INTO TRANSACTIONS(Id,
                               TransactionTypeId, 
                               InternalAccountId,
                               Amount,
                               Balance,
                               PayeeAccount,
                               ReservationId)
           VALUES(GetNextTransactionIdSP(),
                  6,
                  v_InternalAccountId,
                  v_Amount,
                  v_NewBalance,
                  "Payee100",
                  p_FailedTransactionId);
    END IF;
    COMMIT;
  END
// 

-- For the PoC demo only, see "RestoreAccountsSP".

CREATE PROCEDURE RestoreUserAccountsSP(IN p_UserId INT)
  BEGIN
    UPDATE USERS SET LastAccess = NULL, AccessCount = 0 WHERE Id = p_UserId;
    SET SQL_SAFE_UPDATES = 0;
    UPDATE ACCOUNTS INNER JOIN ACCOUNT_TYPES ON ACCOUNTS.AccountTypeId = ACCOUNT_TYPES.Id
        SET Balance = ACCOUNT_TYPES.CappedAt
        WHERE ACCOUNTS.UserId = p_UserId;
    DELETE target FROM TRANSACTIONS AS target
        INNER JOIN ACCOUNTS ON ACCOUNTS.Id = target.InternalAccountId
        WHERE ACCOUNTS.UserId = p_UserId;
    SET SQL_SAFE_UPDATES = 1;
  END;
//

-- For the PoC demo only, restore a user's account after 30 minutes of idling
-- so they can test again without creating a new account :-)

CREATE PROCEDURE RestoreAccountsSP (IN p_Unconditionally BOOLEAN)
  BEGIN
    DECLARE v_LastAccess TIMESTAMP;
    DECLARE v_Balance DECIMAL(8,2);
    DECLARE v_Done BOOLEAN DEFAULT FALSE;
    DECLARE v_UserId INT;
    DECLARE v_UserId_cursor CURSOR FOR SELECT Id FROM USERS;
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET v_Done = TRUE;

    OPEN v_UserId_cursor;
  ReadLoop:
    LOOP
      FETCH v_UserId_cursor INTO v_UserId;
      IF v_Done THEN
        LEAVE ReadLoop;
      END IF;
      SELECT USERS.LastAccess INTO v_LastAccess FROM USERS WHERE USERS.Id = v_UserId;
      IF p_Unconditionally OR (IFNULL(v_LastAccess, FALSE) AND 
                              (v_LastAccess < (NOW() - INTERVAL 30 MINUTE))) THEN
        CALL RestoreUserAccountsSP(v_UserId);
      END IF;
    END LOOP;
    CLOSE v_UserId_cursor;
  END
//

-- Test code only called by this script
CREATE PROCEDURE ASSERT_TRUE (IN p_DidIt BOOLEAN, IN p_Message VARCHAR(100))
  BEGIN
    IF p_DidIt = FALSE THEN
      SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = p_Message, MYSQL_ERRNO = 1001;
    END IF;
  END
//

-- Test code only called by this script
CREATE PROCEDURE ASSERT_TRANSACTION(IN p_Error INT,
                                    IN p_TransactionId INT,
                                    IN p_OptionalReference INT,
                                    IN p_ResultBalance DECIMAL(8,2),
                                    IN p_TransactionTypeName VARCHAR(30))
  BEGIN
    CALL ASSERT_TRUE(p_Error = 0, "Transaction error");
    SELECT TRANSACTIONS.Amount, 
           TRANSACTIONS.Balance, 
           TRANSACTIONS.TransactionTypeId,
           TRANSACTIONS.ReservationId,
           ACCOUNTS.Balance
        INTO @tramount,
             @trbalance, 
             @trtypid,
             @trreserve,
             @acbalance FROM
        TRANSACTIONS INNER JOIN ACCOUNTS ON TRANSACTIONS.InternalAccountId = ACCOUNTS.Id
        WHERE TRANSACTIONS.Id = p_TransactionId;
    CALL ASSERT_TRUE(GetTransactionTypeId(p_TransactionTypeName) = @trtypid, "Transaction type");
    CALL ASSERT_TRUE(IFNULL(@trreserve, 0) = IFNULL(p_OptionalReference, 0), "Transaction reference");
    CALL ASSERT_TRUE(@trbalance = p_ResultBalance, "Transaction balance");
    CALL ASSERT_TRUE(@acbalance = p_ResultBalance, "Account balance");
  END
//

DELIMITER ;

-- Account Types:

CALL CreateAccountTypeSP("CREDIT_CARD_ACCOUNT",
                         "Credit Card Account (loan)",
                         2390.00);

SET @sepaAccountBalance = 5543.00;
CALL CreateAccountTypeSP("STANDARD_ACCOUNT",
                         "SEPA Primary Account", 
                         5543.00);

CALL CreateAccountTypeSP("NEW_USER_ACCOUNT",
                         "Low Value Account for New Users", 
                         @sepaAccountBalance);


-- Payment Methods:

CALL CreatePaymentMethodSP("https://supercard.com",     -- VISA
                           "CREDIT_CARD('453256', @accountNumber, 9)");

CALL CreatePaymentMethodSP("https://bankdirect.net",    -- LCL 
                           "FRENCH_IBAN(@accountNumber)");

CALL CreatePaymentMethodSP("https://unusualcard.com",   -- DISCOVER
                           "CREDIT_CARD('601103', @accountNumber, 9)");


-- Transaction Types:

CALL CreateTransactionTypeSP("DIRECT_DEBIT",
                             "Single step payment operation");

CALL CreateTransactionTypeSP("RESERVE",
                             "Phase one of a two-step payment operation");

CALL CreateTransactionTypeSP("RESERVE_MULTI",
                             "Phase one of an open multi-step payment operation");

CALL CreateTransactionTypeSP("TRANSACT",
                             "Phase two or more of a multi-step payment operation");

CALL CreateTransactionTypeSP("CREDIT_ACCOUNT",
                             "Money sent to the account");

-- Internal (database) use only
CALL CreateTransactionTypeSP("*FAILED*",
                             "Referenced transaction failed and was nullified");

-- Demo and test data

CALL CreateUserSP(@userid, "Luke Skywalker");

CALL CreateAccountSP(@internalAccountId, @userid, GetAccountTypeId("CREDIT_CARD_ACCOUNT"));

CALL CreateDemoCredentialSP(@credentialid,
                            @accountId,
                            @internalAccountId, 
                            "https://supercard.com",
                            x'b3b76a196ced26e7e5578346b25018c0e86d04e52e5786fdc2810a2a10bd104a',
                            NULL);
SELECT @credentialid, @accountId, @internalAccountId;

CALL CreateAccountSP(@internalAccountId, @userid, GetAccountTypeId("STANDARD_ACCOUNT"));

CALL CreateDemoCredentialSP(@credentialid,
                            @accountId,
                            @internalAccountId, 
                            "https://bankdirect.net",
                            x'892225decf3038bdbe3a7bd91315930e9c5fc608dd71ab10d0fb21583ab8cadd',
                            x'b3b76a196ced26e7e5578346b25018c0e86d04e52e5786fdc2810a2a10bd104a');
SELECT @credentialid, @accountId, @internalAccountId;

CALL CreateAccountSP(@internalAccountId, @userid, GetAccountTypeId("NEW_USER_ACCOUNT"));

CALL CreateDemoCredentialSP(@credentialid,
                            @accountId,
                            @internalAccountId, 
                            "https://unusualcard.com",
                            x'19aed933edacc289d0d63fba788cf424612d346754d110863cd043b52abecd53',
                            NULL);
SELECT @credentialid, @accountId, @internalAccountId;
                            
CALL CreateUserSP(@userid, "Chewbacca");

CALL CreateAccountAndCredentialSP(@accountId,
                                  @credentialid,
                                  @userid,
                                  "CREDIT_CARD_ACCOUNT",
                                  "https://supercard.com",  
                                  x'f3b76a196ced26e7e5578346b25018c0e86d04e52e5786fdc2810a2a10bd104a', 
                                  NULL);
SELECT @credentialid, @accountId;

CALL CreateAccountAndCredentialSP(@accountId,
                                  @credentialid,
                                  @userid,
                                  "STANDARD_ACCOUNT",
                                  "https://bankdirect.net",
                                  x'892225decf3038bdbe3a7bd91315930e9c5fc608dd71ab10d0fb21583ab8cadd',
                                  x'b3b76a196ced26e7e5578346b25018c0e86d04e52e5786fdc2810a2a10bd104a');
SELECT @credentialid, @accountId;

CALL AuthenticatePayReqSP(@error,
                          @userName,
                          @credentialId + 1,
                          @accountId,
                          GetPaymentMethodId("https://bankdirect.net"),
                          x'892225decf3038bdbe3a7bd91315930e9c5fc608dd71ab10d0fb21583ab8cadd');
CALL ASSERT_TRUE(@error = 1, "Auth");

CALL AuthenticatePayReqSP(@error,
                          @userName,
                          @credentialId,
                          "BLAH",
                          GetPaymentMethodId("https://bankdirect.net"),
                          x'892225decf3038bdbe3a7bd91315930e9c5fc608dd71ab10d0fb21583ab8cadd');
CALL ASSERT_TRUE(@error = 2, "Auth");

CALL AuthenticatePayReqSP(@error,
                          @userName,
                          @credentialId,
                          @accountId,
                          GetPaymentMethodId("https://bankdirect.net"),
                          x'892225decf3038bdbe3a7bd91315930e9c5fc608dd71ab10d0fb21583ab8cade');
CALL ASSERT_TRUE(@error = 3, "Auth");

CALL AuthenticatePayReqSP(@error,
                          @userName,
                          @credentialId,
                          @accountId,
                          GetPaymentMethodId("https://supercard.com"),
                          x'892225decf3038bdbe3a7bd91315930e9c5fc608dd71ab10d0fb21583ab8cadd');
CALL ASSERT_TRUE(@error = 4, "Auth");

CALL AuthenticatePayReqSP(@error,
                          @userName,
                          @credentialId,
                          @accountId,
                          GetPaymentMethodId("blah"),
                          x'892225decf3038bdbe3a7bd91315930e9c5fc608dd71ab10d0fb21583ab8cadd');
CALL ASSERT_TRUE(@error = 4, "Auth");

CALL AuthenticatePayReqSP(@error,
                          @userName,
                          @credentialId,
                          @accountId,
                          GetPaymentMethodId("https://bankdirect.net"),
                          x'892225decf3038bdbe3a7bd91315930e9c5fc608dd71ab10d0fb21583ab8cadd');
CALL ASSERT_TRUE(@error = 0, "Auth");
CALL ASSERT_TRUE(@userName = "Chewbacca", "UserName");

CALL AuthenticateBalReqSP(@error,
                          @balance,
                          @credentialId,
                          x'b3b76a196ced26e7e5578346b25018c0e86d04e52e5786fdc2810a2a10bd104a');
CALL ASSERT_TRUE(@error = 0, "Auth balance");
CALL ASSERT_TRUE(@balance = @sepaAccountBalance, "Check balance"); 

CALL ExternalWithDrawSP(@error,
                        @transactionId,
                        "Payee100",
                        "Demo Merchant",
                        "#1064",
                        GetTransactionTypeId("DIRECT_DEBIT"),
                        NULL,
                        @sepaAccountBalance + 1.00,
                        @accountId);
CALL ASSERT_TRUE(@error = 4, "No funds");

CALL ExternalWithDrawSP(@error,
                        @transactionId,
                        "Payee100",
                        "Demo Merchant",
                        "#1064",
                        GetTransactionTypeId("TRANSACT"),
                        555555555,
                        100.25,
                        @accountId);
CALL ASSERT_TRUE(@error = 5, "No reservation found");

CALL ExternalWithDrawSP(@error,
                        @transactionId,
                        "Payee100",
                        "Demo Merchant",
                        "#1064",
                        GetTransactionTypeId("DIRECT_DEBIT"),
                        NULL,
                        100.25,
                        @accountId);
CALL ASSERT_TRANSACTION(@error, @transactionId, null, @sepaAccountBalance - 100.25, "DIRECT_DEBIT");

CALL ExternalWithDrawSP(@error,
                        @transactionId,
                        "Payee100",
                        "Demo Merchant",
                        "#1064",
                        GetTransactionTypeId("RESERVE"),
                        NULL,
                        200.00,
                        @accountId);
CALL ASSERT_TRANSACTION(@error, @transactionId, null, @sepaAccountBalance - 300.25, "RESERVE");

SET @reftrans = @transactionId;
CALL ExternalWithDrawSP(@error,
                        @transactionId,
                        "Payee100",
                        "Demo Merchant",
                        "#1064",
                        GetTransactionTypeId("TRANSACT"),
                        @reftrans,
                        100.00,
                        @accountId);
CALL ASSERT_TRANSACTION(@error, @transactionId, @reftrans, @sepaAccountBalance - 200.25, "TRANSACT");

CALL CreditAccountSP(@error,
                     @transactionId,
                     "Payee100",
                     "Demo Merchant",
                     "#1064",
                     200.25,
                     @accountId);
CALL ASSERT_TRANSACTION(@error, @transactionId, null, @sepaAccountBalance, "CREDIT_ACCOUNT");

CALL ExternalWithDrawSP(@error,
                        @transactionId,
                        "Payee100",
                        "Demo Merchant",
                        "#1064",
                        GetTransactionTypeId("DIRECT_DEBIT"),
                        NULL,
                        500.00,
                        @accountId);
CALL NullifyTransactionSP(@error, @transactionId);
CALL ASSERT_TRANSACTION(@error, @transactionId + 1, @transactionId, @sepaAccountBalance, "*FAILED*");

CALL ExternalWithDrawSP(@error,
                        @transactionId,
                        "Payee100",
                        "Demo Merchant",
                        "#1064",
                        GetTransactionTypeId("RESERVE"),
                        NULL,
                        200.00,
                        @accountId);
CALL ASSERT_TRANSACTION(@error, @transactionId, null, @sepaAccountBalance - 200.00, "RESERVE");

SET @reftrans = @transactionId;
CALL ExternalWithDrawSP(@error,
                        @transactionId,
                        "Payee100",
                        "Demo Merchant",
                        "#1064",
                        GetTransactionTypeId("TRANSACT"),
                        @reftrans,
                        100.00,
                        @accountId);
CALL ASSERT_TRANSACTION(@error, @transactionId, @reftrans, @sepaAccountBalance - 100.00, "TRANSACT");

CALL NullifyTransactionSP(@error, @transactionId);
-- SELECT * from TRANSACTIONS;
CALL ASSERT_TRANSACTION(@error, @transactionId + 1, @transactionId, @sepaAccountBalance - 200.00, "*FAILED*");
