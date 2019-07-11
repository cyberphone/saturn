-- SQL Script for MySQL 5.7
--
-- root privileges are required!!!
--
-- Clear and create DB to begin with
--
DROP DATABASE IF EXISTS PAYER_BANK;
CREATE DATABASE PAYER_BANK CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
--
-- Create a user but remove any existing user first
DROP USER IF EXISTS saturn@localhost;
--
CREATE USER saturn@localhost IDENTIFIED BY 'foo123';
--
-- Let user access
--
GRANT ALL ON PAYER_BANK.* TO saturn@localhost;
GRANT SELECT ON mysql.proc TO saturn@localhost;
--
-- Create tables
--
-- #############################################################
-- # Note: a bank probably uses a much more elaborate database #
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
    SymbolicName VARCHAR(20)  NOT NULL,                                  -- As as symbolic name
    Description VARCHAR(50)   NOT NULL  UNIQUE,                          -- Description
    CappedAt    DECIMAL(8,2)  NOT NULL,                                  -- Capped at
    PRIMARY KEY (Id)
  );


/*=============================================*/
/*                 ACCOUNTS                    */
/*=============================================*/

CREATE TABLE ACCOUNTS
  (
    Id          INT           NOT NULL  AUTO_INCREMENT,                  -- Unique Account ID
    UserId      INT           NOT NULL,                                  -- Unique User ID (account holder)
    AccountType INT           NOT NULL,                                  -- Unique Type ID
    Created     TIMESTAMP     NOT NULL  DEFAULT CURRENT_TIMESTAMP,       -- Administrator data
    Balance     DECIMAL(8,2)  NOT NULL,                                  -- Disponible
    Currency    CHAR(3)       NOT NULL  DEFAULT "EUR",                   -- SEK, USD, EUR etc.
    PRIMARY KEY (Id),
    FOREIGN KEY (AccountType) REFERENCES ACCOUNT_TYPES(Id),
    FOREIGN KEY (UserId) REFERENCES USERS(Id) ON DELETE CASCADE
  ) AUTO_INCREMENT=200500123;


/*=============================================*/
/*             CREDENTIAL_TYPES                */
/*=============================================*/

CREATE TABLE CREDENTIAL_TYPES
  (
    Id          INT           NOT NULL  AUTO_INCREMENT,                  -- Unique Credential Type
    MethodUri   VARCHAR(50)   NOT NULL  UNIQUE,                          -- Payment method
    Format      VARCHAR(80)   NOT NULL,                                  -- Account syntax
    Created     TIMESTAMP     NOT NULL  DEFAULT CURRENT_TIMESTAMP,       -- Administrator data
    PRIMARY KEY (Id)
  );


/*=============================================*/
/*                CREDENTIALS                  */
/*=============================================*/

CREATE TABLE CREDENTIALS
  (
    Id          VARCHAR(30)   NOT NULL  UNIQUE,                          -- Unique Credential ID 
    AccountId   INT           NOT NULL,                                  -- Account ID Reference
    CredentialType  INT       NOT NULL,                                  -- Credential Type Reference
    Created     TIMESTAMP     NOT NULL  DEFAULT CURRENT_TIMESTAMP,       -- Administrator data
    S256PayReq  BINARY(32)    NOT NULL,                                  -- Payment request key hash 
    S256BalReq  BINARY(32)    NULL,                                      -- Optional: balance key hash 
    PRIMARY KEY (Id),
    FOREIGN KEY (CredentialType) REFERENCES CREDENTIAL_TYPES(Id),
    FOREIGN KEY (AccountId) REFERENCES ACCOUNTS(Id) ON DELETE CASCADE
  );


/*=============================================*/
/*             TRANSACTION_TYPES               */
/*=============================================*/

CREATE TABLE TRANSACTION_TYPES
  (
    Id          INT           NOT NULL  AUTO_INCREMENT,                  -- Unique Transaction Type ID
    SymbolicName VARCHAR(20)  NOT NULL,                                  -- As as symbolic name
    Description VARCHAR(80)   NOT NULL,                                  -- A bit more on the topic
    PRIMARY KEY (Id)
  );


/*=============================================*/
/*               TRANSACTIONS                  */
/*=============================================*/

CREATE TABLE TRANSACTIONS
  (
    Id          INT           NOT NULL  UNIQUE,                          -- Unique Transaction ID
    AccountId   INT           NOT NULL,                                  -- Unique Account ID
    TransactionType INT       NOT NULL,                                  -- Unique Transaction Type ID
    Amount      DECIMAL(8,2)  NOT NULL,                                  -- The Amount involved
    Balance     DECIMAL(8,2)  NOT NULL,                                  -- Outgoing Account balance
    Originator  VARCHAR(50),                                             -- Optional Merchant
    ExtReference VARCHAR(50),                                            -- Optional External Ref
    CredentialId VARCHAR(30),                                            -- Optional Credential ID
    ReservationId INT,                                                   -- Reservation Transaction ID
    Created     TIMESTAMP     NOT NULL  DEFAULT CURRENT_TIMESTAMP,       -- Administrator data
    PRIMARY KEY (Id),
    FOREIGN KEY (TransactionType) REFERENCES TRANSACTION_TYPES(Id),
    FOREIGN KEY (AccountId) REFERENCES ACCOUNTS(Id) ON DELETE CASCADE
  );


/*=============================================*/
/*            TRANSACTION_COUNTER              */
/*=============================================*/

CREATE TABLE TRANSACTION_COUNTER
  (
    Next        INT           NOT NULL                                   -- Monotonic counter
  );

INSERT INTO TRANSACTION_COUNTER(Next) VALUES(100345078);

DELIMITER //

-- MySQL 5.7 auto increment is unreliable for this application since it
-- loses track for power fails if TRANSACTIONS is emptied like in the demo

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

CREATE PROCEDURE CreateAccountTypeSP (IN p_SymbolicName VARCHAR(20),
                                      IN p_Description VARCHAR(50),
                                      IN p_CappedAt DECIMAL(8,2))
  BEGIN
    INSERT INTO ACCOUNT_TYPES(SymbolicName, Description, CappedAt)
        VALUES(p_SymbolicName, p_Description, p_CappedAt);
  END
//

CREATE PROCEDURE CreateCredentialTypeSP (IN p_MethodUri VARCHAR(50),
                                         IN p_Format VARCHAR(80))
  BEGIN
    INSERT INTO CREDENTIAL_TYPES(MethodUri, Format)
        VALUES(p_MethodUri, p_Format);
  END
//

CREATE PROCEDURE CreateTransactionTypeSP (IN p_SymbolicName VARCHAR(20),
                                          IN p_Description VARCHAR(80))
  BEGIN
    INSERT INTO TRANSACTION_TYPES(SymbolicName, Description)
        VALUES(p_SymbolicName, p_Description);
  END
//

CREATE FUNCTION GetTransactionTypeSP (p_SymbolicName VARCHAR(20)) RETURNS INT
DETERMINISTIC
  BEGIN
    DECLARE v_Id INT;

    SELECT TRANSACTION_TYPES.Id INTO v_Id FROM TRANSACTION_TYPES
        WHERE TRANSACTION_TYPES.SymbolicName = p_SymbolicName;
    RETURN v_Id;
  END
//

CREATE FUNCTION GetAccountTypeSP (p_SymbolicName VARCHAR(20)) RETURNS INT
DETERMINISTIC
  BEGIN
    DECLARE v_Id INT;

    SELECT ACCOUNT_TYPES.Id INTO v_Id FROM ACCOUNT_TYPES
        WHERE ACCOUNT_TYPES.SymbolicName = p_SymbolicName;
    RETURN v_Id;
  END
//

CREATE FUNCTION FRENCH_IBAN (p_AccountId INT(11)) RETURNS VARCHAR(30)
DETERMINISTIC
  BEGIN
    -- https://fr.wikipedia.org/wiki/Cl%C3%A9_RIB
    set @chk = LPAD(CONVERT(97 - (2836843 + 3 * p_AccountId) % 97, CHAR), 2, '0');
    set @bban = CONCAT('3000211111', LPAD(CONVERT(p_AccountId, DECIMAL(11)), 11, '0'), @chk);
    set @key = LPAD(CONVERT(98 - (CONVERT(CONCAT(@bban, '152700'), DECIMAL(30)) % 97), CHAR), 2, '0');
    RETURN CONCAT('FR', @key, @bban);
  END
//

CREATE FUNCTION CREDIT_CARD (p_IIN VARCHAR(8),
                             p_AccountId INT(11),
                             p_AccountDigits INT) RETURNS VARCHAR(30)
DETERMINISTIC
  BEGIN
    -- https://gefvert.org/blog/archives/57
    DECLARE i, s, r, weight INT;
    DECLARE baseNumber VARCHAR(16);
 
    SET baseNumber = CONCAT(p_IIN, LPAD(CONVERT(p_AccountId, DECIMAL(11)), p_AccountDigits, '0'));
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

CREATE PROCEDURE CreateAccountSP (OUT p_AccountId INT(11),
                                  IN p_UserId INT, 
                                  IN p_AccountType INT)
  BEGIN
    INSERT INTO ACCOUNTS(UserId, AccountType, Balance)
        SELECT p_UserId, p_AccountType, ACCOUNT_TYPES.CappedAt 
        FROM ACCOUNT_TYPES WHERE Id = p_AccountType;
    SET p_AccountId = LAST_INSERT_ID();
  END
//

CREATE PROCEDURE CreateDemoCredentialSP (IN p_AccountId INT(11), 
                                         IN p_CredentialId VARCHAR(30),
                                         IN p_MethodUri VARCHAR(50),
                                         IN p_S256PayReq BINARY(32),
                                         IN p_S256BalReq BINARY(32))
  BEGIN
    INSERT INTO CREDENTIALS(Id, AccountId, S256PayReq, S256BalReq, CredentialType) 
        SELECT p_CredentialId, p_AccountId, p_S256PayReq, p_S256BalReq, CREDENTIAL_TYPES.Id
        FROM CREDENTIAL_TYPES WHERE MethodUri = p_MethodUri;
  END
//

CREATE PROCEDURE CreateAccountAndCredentialSP (OUT p_CredentialId VARCHAR(30),
                                               IN p_UserId INT, 
                                               IN p_AccountType INT,
                                               IN p_MethodUri VARCHAR(50),
                                               IN p_S256PayReq BINARY(32),
                                               IN p_S256BalReq BINARY(32))
  BEGIN
    CALL CreateAccountSP(@accountId, p_UserId, p_AccountType);
    SELECT Id, Format INTO @credentialType, @format FROM CREDENTIAL_TYPES
        WHERE CREDENTIAL_TYPES.MethodUri = p_MethodUri;
    SET @sql = CONCAT('SET @credentialId = ', @format);
    PREPARE stmt FROM @sql;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
    INSERT INTO CREDENTIALS(Id, AccountId, CredentialType, S256PayReq, S256BalReq) 
        VALUES(@credentialId, @accountId, @credentialType, p_S256PayReq, p_S256BalReq);
    SET p_CredentialId = @credentialId;
  END
//

CREATE PROCEDURE AuthenticatePayReqSP (OUT p_Error INT,
                                       IN p_CredentialId VARCHAR(30),
                                       IN p_MethodUri VARCHAR(50),
                                       IN p_S256PayReq BINARY(32))
  BEGIN
    DECLARE v_UserId INT;
    
    SELECT ACCOUNTS.UserId INTO v_UserId FROM ACCOUNTS 
        INNER JOIN CREDENTIALS ON ACCOUNTS.Id = CREDENTIALS.AccountId
        INNER JOIN CREDENTIAL_TYPES ON CREDENTIAL_TYPES.Id = CREDENTIALS.CredentialType
            WHERE CREDENTIALS.Id = p_CredentialId AND
                  CREDENTIAL_TYPES.MethodUri = p_MethodUri AND
                  CREDENTIALS.S256PayReq = p_S256PayReq
            LIMIT 1;
    IF v_UserId IS NULL THEN   -- Failed => Find reason
      IF EXISTS (SELECT * FROM CREDENTIALS WHERE CREDENTIALS.Id = p_CredentialId) THEN
        IF EXISTS (SELECT * FROM ACCOUNTS 
            INNER JOIN CREDENTIALS ON ACCOUNTS.Id = CREDENTIALS.AccountId
            INNER JOIN CREDENTIAL_TYPES ON CREDENTIAL_TYPES.Id = CREDENTIALS.CredentialType
                WHERE CREDENTIALS.Id = p_CredentialId AND
                      CREDENTIAL_TYPES.MethodUri = p_MethodUri) THEN
          SET p_Error = 3;       -- Key does not match account
        ELSE
          SET p_Error = 2;       -- Method does not match account type
        END IF;
      ELSE
        SET p_Error = 1;         -- No such account
      END IF;
    ELSE                       
      SET p_Error = 0;          -- Success => Update access info
      UPDATE USERS SET LastAccess = CURRENT_TIMESTAMP, AccessCount = AccessCount + 1
          WHERE USERS.Id = v_UserId;     
    END IF;
  END
//

CREATE PROCEDURE AuthenticateBalReqSP (OUT p_Error INT,
                                       OUT p_Balance DECIMAL(8,2),
                                       IN p_CredentialId VARCHAR(30),
                                       IN p_S256BalReq BINARY(32))
  BEGIN
    DECLARE v_Balance DECIMAL(8,2);

    SELECT ACCOUNTS.Balance INTO v_Balance FROM ACCOUNTS 
        INNER JOIN CREDENTIALS ON ACCOUNTS.Id = CREDENTIALS.AccountId
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
                                     IN p_OptionalOriginator VARCHAR(50),
                                     IN p_OptionalExtReference VARCHAR(50),
                                     IN p_TransactionType INT,
                                     IN p_OptionalReservationId INT,
                                     IN p_Amount DECIMAL(8,2),
                                     IN p_CredentialId VARCHAR(30))
  BEGIN
    DECLARE v_Balance DECIMAL(8,2);
    DECLARE v_NewBalance DECIMAL(8,2);
    DECLARE v_PreviousAmount DECIMAL(8,2);
    DECLARE v_AccountId INT(11);

    SET p_Error = 0;
    START TRANSACTION;
    SELECT ACCOUNTS.Balance, ACCOUNTS.Id INTO v_Balance, v_AccountId FROM ACCOUNTS
        INNER JOIN CREDENTIALS ON ACCOUNTS.Id = CREDENTIALS.AccountId
        WHERE CREDENTIALS.Id = p_CredentialId
        LIMIT 1
        FOR UPDATE;
    IF v_AccountId IS NULL THEN    -- Failed
      SET p_Error = 1;             -- No such account
    ELSE
      SET v_NewBalance = v_Balance - p_Amount;
      IF p_OptionalReservationId IS NOT NULL THEN
        SELECT TRANSACTIONS.Amount INTO v_PreviousAmount FROM TRANSACTIONS
            INNER JOIN ACCOUNTS ON ACCOUNTS.Id = TRANSACTIONS.AccountId
            INNER JOIN CREDENTIALS ON ACCOUNTS.Id = CREDENTIALS.AccountId
            WHERE TRANSACTIONS.Id = p_OptionalReservationId AND
                  TRANSACTIONS.TransactionType = 2 AND
                  ACCOUNTS.Id = v_AccountId AND
                  CREDENTIALS.Id = p_CredentialId
            LIMIT 1;
        IF v_PreviousAmount IS NULL THEN
          SET p_Error = 5;         -- Reservation not found
        ELSE
          IF EXISTS (SELECT * FROM TRANSACTIONS
              WHERE TRANSACTIONS.ReservationId = p_OptionalReservationId) THEN
            SET p_Error = 6;         -- Reservation already used
          ELSE
            SET v_NewBalance = v_NewBalance + v_PreviousAmount;
          END IF;
        END IF;
      END IF;
      IF p_Error = 0 THEN
        IF v_NewBalance < 0 THEN
          SET p_Error = 4;           -- Out of funds
        ELSE                       -- Success => Withdraw the specified amount
          UPDATE ACCOUNTS SET Balance = v_NewBalance
              WHERE ACCOUNTS.Id = v_AccountId;
          SET p_TransactionId = GetNextTransactionIdSP();
          INSERT INTO TRANSACTIONS(Id,
                                   TransactionType, 
                                   AccountId,
                                   Amount,
                                   Balance,
                                   CredentialId, 
                                   Originator, 
                                   ExtReference,
                                   ReservationId) 
               VALUES(p_TransactionId,
                      p_TransactionType,
                      v_AccountId,
                      p_Amount,
                      v_NewBalance,
                      p_CredentialId,
                      p_OptionalOriginator, 
                      p_OptionalExtReference,
                      p_OptionalReservationId);
        END IF;
      END IF;
    END IF;
    COMMIT;
  END
//

CREATE PROCEDURE InternalWithDrawSP (OUT p_Error INT,
                                     OUT p_TransactionId INT,
                                     IN p_OptionalOriginator VARCHAR(50),
                                     IN p_OptionalExtReference VARCHAR(50),
                                     IN p_TransactionType INT,
                                     IN p_Amount DECIMAL(8,2),
                                     IN p_AccountId INT)
  BEGIN
    DECLARE v_Balance DECIMAL(8,2);

    SET p_Error = 0;
    START TRANSACTION;
    SELECT ACCOUNTS.Balance INTO v_Balance FROM ACCOUNTS
        WHERE ACCOUNTS.Id = p_AccountId
        LIMIT 1
        FOR UPDATE;
    IF v_Balance IS NULL THEN    -- Failed
      SET p_Error = 1;             -- No such account
    ELSE
      IF p_Amount > v_Balance THEN
        SET p_Error = 4;           -- Out of funds
      ELSE                       -- Success => Withdraw the specified amount
        UPDATE ACCOUNTS SET Balance = Balance - p_Amount
            WHERE ACCOUNTS.Id = v_AccountId;
        SET p_TransactionId = GetNextTransactionIdSP();
        INSERT INTO TRANSACTIONS(Id,
                                 TransactionType, 
                                 AccountId,
                                 Amount,
                                 CredentialId, 
                                 Originator, 
                                 ExtReference) 
             VALUES(p_TransactionId,
                    p_TransactionType,
                    p_AccountId,
                    p_Amount,
                    p_CredentialId,
                    p_OptionalOriginator, 
                    p_OptionalExtReference);
      END IF;
    END IF;
    COMMIT;
  END
//

CREATE PROCEDURE ExternalRefundSP (OUT p_Error INT,
                                   OUT p_TransactionId INT,
                                   IN p_OptionalOriginator VARCHAR(50),
                                   IN p_OptionalExtReference VARCHAR(50),
                                   IN p_OptionalReservationId INT,
                                   IN p_Amount DECIMAL(8,2),
                                   IN p_CredentialId VARCHAR(30))
  BEGIN
    DECLARE v_Balance DECIMAL(8,2);
    DECLARE v_NewBalance DECIMAL(8,2);
    DECLARE v_PreviousAmount DECIMAL(8,2);
    DECLARE v_AccountId INT(11);

    SET p_Error = 0;
    START TRANSACTION;
    SELECT ACCOUNTS.Balance, ACCOUNTS.Id INTO v_Balance, v_AccountId FROM ACCOUNTS
        INNER JOIN CREDENTIALS ON ACCOUNTS.Id = CREDENTIALS.AccountId
        WHERE CREDENTIALS.Id = p_CredentialId
        LIMIT 1
        FOR UPDATE;
    IF v_AccountId IS NULL THEN    -- Failed
      SET p_Error = 1;             -- No such account
    ELSE
      SET v_NewBalance = v_Balance + p_Amount;
      UPDATE ACCOUNTS SET Balance = v_NewBalance
          WHERE ACCOUNTS.Id = v_AccountId;
      SET p_TransactionId = GetNextTransactionIdSP();
      INSERT INTO TRANSACTIONS(Id,
                               TransactionType, 
                               AccountId,
                               Amount,
                               Balance,
                               CredentialId, 
                               Originator, 
                               ExtReference,
                               ReservationId) 
           VALUES(p_TransactionId,
                  5,
                  v_AccountId,
                  p_Amount,
                  v_NewBalance,
                  p_CredentialId,
                  p_OptionalOriginator, 
                  p_OptionalExtReference,
                  p_OptionalReservationId);
    END IF;
    COMMIT;
  END
//

CREATE PROCEDURE RestoreUserAccountsSP(IN p_UserId INT)
  BEGIN
    UPDATE USERS SET LastAccess = NULL, AccessCount = 0 WHERE Id = p_UserId;
    SET SQL_SAFE_UPDATES = 0;
    UPDATE ACCOUNTS INNER JOIN ACCOUNT_TYPES ON ACCOUNTS.AccountType = ACCOUNT_TYPES.Id
        SET Balance = ACCOUNT_TYPES.CappedAt
        WHERE ACCOUNTS.UserId = p_UserId;
    DELETE target FROM TRANSACTIONS AS target
        INNER JOIN ACCOUNTS ON ACCOUNTS.Id = target.AccountId
        WHERE ACCOUNTS.UserId = p_UserId;
    SET SQL_SAFE_UPDATES = 1;
  END;
//

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

DELIMITER ;

-- Account Types:

CALL CreateAccountTypeSP("CREDIT_CARD_ACCOUNT",
                         "Credit Card Account (loan)",
                         2390.00);

CALL CreateAccountTypeSP("STANDARD_ACCOUNT",
                         "SEPA Primary Account", 
                         5543.00);

CALL CreateAccountTypeSP("NEW_USER_ACCOUNT",
                         "Low Value Account for New Users", 
                         120.00);


-- Credential Types:

CALL CreateCredentialTypeSP("https://supercard.com",     -- VISA
                            "CREDIT_CARD('453256', @accountId, 9)");

CALL CreateCredentialTypeSP("https://bankdirect.net",    -- LCL 
                            "FRENCH_IBAN(@accountId)");

CALL CreateCredentialTypeSP("https://unusualcard.com",   -- DISCOVER
                            "CREDIT_CARD('601103', @accountId, 9)");


-- Transaction Types:

CALL CreateTransactionTypeSP("DIRECT_DEBIT",
                             "Single step payment operation");

CALL CreateTransactionTypeSP("RESERVE",
                             "Phase one of a two-step payment operation");

CALL CreateTransactionTypeSP("RESERVE_MULTI",
                             "Phase one of an open multi-step payment operation");

CALL CreateTransactionTypeSP("TRANSACT",
                             "Phase two or more of a multi-step payment operation");

CALL CreateTransactionTypeSP("REFUND",
                             "Phase two or more of a multi-step payment operation");

-- Demo data

CALL CreateUserSP(@userid, "Luke Skywalker");

CALL CreateAccountSP(@accountid, @userid, GetAccountTypeSP("CREDIT_CARD_ACCOUNT"));

CALL CreateDemoCredentialSP(@accountid, 
                            "6875056745552109", 
                            "https://supercard.com",
                            x'b3b76a196ced26e7e5578346b25018c0e86d04e52e5786fdc2810a2a10bd104a',
                            NULL);

CALL CreateAccountSP(@accountid, @userid, GetAccountTypeSP("STANDARD_ACCOUNT"));

CALL CreateDemoCredentialSP(@accountid, 
                            "8645-7800239403",
                            "https://bankdirect.net",
                            x'892225decf3038bdbe3a7bd91315930e9c5fc608dd71ab10d0fb21583ab8cadd',
                            x'b3b76a196ced26e7e5578346b25018c0e86d04e52e5786fdc2810a2a10bd104a');

CALL CreateAccountSP(@accountid, @userid, GetAccountTypeSP("NEW_USER_ACCOUNT"));

CALL CreateDemoCredentialSP(@accountid, 
                            "1111222233334444", 
                            "https://unusualcard.com",
                            x'19aed933edacc289d0d63fba788cf424612d346754d110863cd043b52abecd53',
                            NULL);
                            
CALL CreateUserSP(@userid, "Chewbacca");

CALL CreateAccountSP(@accountid, @userid, GetAccountTypeSP("CREDIT_CARD_ACCOUNT"));

CALL CreateDemoCredentialSP(@accountid, 
                            "6875056745552108",
                            "https://supercard.com",  
                            x'f3b76a196ced26e7e5578346b25018c0e86d04e52e5786fdc2810a2a10bd104a', 
                            NULL);

CALL CreateAccountAndCredentialSP(@credid,
                                  @userid,
                                  GetAccountTypeSP("CREDIT_CARD_ACCOUNT"),
                                  "https://supercard.com",  
                                  x'f3b76a196ced26e7e5578346b25018c0e86d04e52e5786fdc2810a2a10bd104a', 
                                  NULL);

CALL CreateAccountAndCredentialSP(@credid,
                                  @userid,
                                  GetAccountTypeSP("STANDARD_ACCOUNT"),
                                  "https://bankdirect.net",
                                  x'892225decf3038bdbe3a7bd91315930e9c5fc608dd71ab10d0fb21583ab8cadd',
                                  x'b3b76a196ced26e7e5578346b25018c0e86d04e52e5786fdc2810a2a10bd104a');
                                                        