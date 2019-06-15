-- SQL Script for MySQL 5.7
--
-- root privileges are required!!!
--
-- Clear and create DB to begin with
--
DROP DATABASE IF EXISTS PAYER_BANK;
CREATE DATABASE PAYER_BANK CHARACTER SET utf8;
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

USE PAYER_BANK;

/*=============================================*/
/*                   USERS                     */
/*=============================================*/

CREATE TABLE USERS
  (
    UserId      INT           NOT NULL  AUTO_INCREMENT,                  -- Unique User ID
    Created     TIMESTAMP     NOT NULL  DEFAULT CURRENT_TIMESTAMP,       -- Administrator data
    AccessCount INT           NOT NULL  DEFAULT 0,                       -- Administrator data
    LastAccess  TIMESTAMP     NULL,                                      -- Administrator data
    Name        VARCHAR(50)   NOT NULL,                                  -- Human name
    PRIMARY KEY (UserId)
  );


/*=============================================*/
/*                 ACCOUNT_TYPES               */
/*=============================================*/

CREATE TABLE ACCOUNT_TYPES
  (
    AccountType INT           NOT NULL  AUTO_INCREMENT,                  -- Unique Type ID
    MethodUri   VARCHAR(50)   NOT NULL  UNIQUE,                          -- Unique Method URI
    CappedAt    DECIMAL(8,2)  NOT NULL,                                  -- Capped at
    Format      VARCHAR(80)   NOT NULL,                                  -- Account Number Syntax
    NextNumber  INT(11)       NOT NULL,                                  -- Account Number Core
    PRIMARY KEY (AccountType)
  );


/*=============================================*/
/*                 ACCOUNTS                    */
/*=============================================*/

CREATE TABLE ACCOUNTS
  (
    AccountId   VARCHAR(30)   NOT NULL  UNIQUE,                          -- Unique Account ID
    UserId      INT           NOT NULL,                                  -- Unique User ID (account holder)
    AccountType INT           NOT NULL,                                  -- Unique Type ID
    Created     TIMESTAMP     NOT NULL  DEFAULT CURRENT_TIMESTAMP,       -- Administrator data
    Balance     DECIMAL(8,2)  NOT NULL,                                  -- Disponible
    Currency    CHAR(3)       NOT NULL  DEFAULT "EUR",                   -- SEK, USD, EUR etc.
    PRIMARY KEY (AccountId),
    FOREIGN KEY (AccountType) REFERENCES ACCOUNT_TYPES(AccountType),
    FOREIGN KEY (UserId) REFERENCES USERS(UserId) ON DELETE CASCADE
  );


/*=============================================*/
/*                CREDENTIALS                  */
/*=============================================*/

CREATE TABLE CREDENTIALS
  (
    AccountId   VARCHAR(30)   NOT NULL,                                  -- Unique Account ID
    Created     TIMESTAMP     NOT NULL  DEFAULT CURRENT_TIMESTAMP,       -- Administrator data
    S256PayReq  BINARY(32)    NOT NULL,                                  -- Payment request key hash 
    S256BalReq  BINARY(32)    NULL,                                      -- Optional: balance key hash 
    FOREIGN KEY (AccountId) REFERENCES ACCOUNTS(AccountId) ON DELETE CASCADE
  );


DELIMITER //

CREATE PROCEDURE CreateUserSP (IN p_Name VARCHAR(50), 
                               OUT p_UserId INT)
  BEGIN
    INSERT INTO USERS(Name) VALUES(p_Name);
    SET p_UserID = LAST_INSERT_ID();
  END
//


CREATE PROCEDURE CreateAccountSP (IN p_UserId INT, 
                                  IN p_AccountId VARCHAR(30),
                                  IN p_MethodUri VARCHAR(50))
  BEGIN
    INSERT INTO ACCOUNTS(UserId, AccountId, AccountType, Balance)
        SELECT p_UserId, p_AccountId, ACCOUNT_TYPES.AccountType, ACCOUNT_TYPES.CappedAt 
        FROM ACCOUNT_TYPES WHERE MethodUri = p_MethodUri;
  END
//

CREATE PROCEDURE CreateDemoAccountSP (IN p_UserId INT, 
                                      IN p_AccountId VARCHAR(30),
                                      IN p_MethodUri VARCHAR(50),
                                      IN p_S256PayReq BINARY(32),
                                      IN p_S256BalReq BINARY(32))
  BEGIN
    CALL CreateAccountSP(p_UserId, p_AccountId, p_MethodUri);
    INSERT INTO CREDENTIALS(AccountId, S256PayReq, S256BalReq) 
        VALUES(p_AccountId, p_S256PayReq, p_S256BalReq);
  END
//

CREATE PROCEDURE CreateAccountTypeSP (IN p_MethodUri VARCHAR(50),
                                      IN p_CappedAt DECIMAL(8,2),
                                      IN p_Format VARCHAR(80),
                                      IN p_NextNumber INT(11))
  BEGIN
    INSERT INTO ACCOUNT_TYPES(MethodUri, CappedAt, Format, NextNumber) 
        VALUES(p_MethodUri, p_CappedAt, p_Format, p_NextNumber);
  END
//

CREATE PROCEDURE AuthenticatePayReqSP (OUT p_Error INT,
                                       IN p_AccountId VARCHAR(30),
                                       IN p_MethodUri VARCHAR(50),
                                       IN p_S256PayReq BINARY(32))
  BEGIN
	IF EXISTS (SELECT ACCOUNTS.Balance FROM ACCOUNTS INNER JOIN CREDENTIALS
	               ON ACCOUNTS.AccountId = CREDENTIALS.AccountId
	               WHERE CREDENTIALS.AccountId = p_AccountId AND
	               CREDENTIALS.MethodUri = p_MethodUri AND
	               CREDENTIALS.S256PayReq = p_S256PayReq) THEN
	  SET p_Error = 0;          -- Success => Update access info
      UPDATE USERS INNER JOIN ACCOUNTS ON USERS.UserId = ACCOUNTS.UserId
          SET LastAccess = CURRENT_TIMESTAMP, AccessCount = AccessCount + 1
          WHERE ACCOUNTS.AccountId = p_AccountId;	  
	ELSE                       -- Failed => Find reason
	  IF EXISTS (SELECT * FROM ACCOUNTS WHERE ACCOUNTS.AccountId = p_AccountId) THEN
        IF EXISTS (SELECT * FROM CREDENTIALS WHERE CREDENTIALS.MethodUri = p_MethodUri) THEN
	      SET p_Error = 3;       -- Key does not match account
        ELSE
          SET p_Error = 2;       -- Method does not match account type
        END IF;
	  ELSE
	    SET p_Error = 1;         -- No such account
	  END IF;
	END IF;
  END
//

CREATE PROCEDURE AuthenticateBalReqSP (OUT p_Balance DECIMAL(8,2),
                                       OUT p_Error INT,
                                       IN p_AccountId VARCHAR(30),
                                       IN p_S256BalReq BINARY(32))
  BEGIN
    DECLARE v_Balance DECIMAL(8,2);

	SELECT ACCOUNTS.Balance INTO v_Balance FROM ACCOUNTS INNER JOIN CREDENTIALS
        ON ACCOUNTS.AccountId = CREDENTIALS.AccountId
        WHERE CREDENTIALS.AccountId = p_AccountId AND
              CREDENTIALS.S256BalReq = p_S256BalReq;
    IF v_Balance IS NULL THEN    -- Failed => Find reason
	  IF EXISTS (SELECT * FROM ACCOUNTS WHERE ACCOUNTS.AccountId = p_AccountId) THEN
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

CREATE PROCEDURE WithDrawSP (OUT p_Error INT,
                             IN p_Amount DECIMAL(8,2),
                             IN p_AccountId VARCHAR(30))
  BEGIN
    DECLARE v_Balance DECIMAL(8,2);

	SET p_Error = 0;
	START TRANSACTION;
	SELECT ACCOUNTS.Balance INTO v_Balance FROM ACCOUNTS
	    WHERE ACCOUNTS.AccountId = p_AccountId
	    FOR UPDATE;
	IF v_Balance IS NULL THEN    -- Failed
      SET p_Error = 1;             -- No such account
	ELSE
	  IF p_Amount > v_Balance THEN
        SET p_Error = 4;           -- Out of funds
      ELSE                       -- Success => Withdraw the specified amount
        UPDATE ACCOUNTS SET Balance = Balance - p_Amount
            WHERE ACCOUNTS.AccountId = p_AccountId;
	  END IF;
	END IF;
	COMMIT;
  END
//

CREATE FUNCTION FRENCH_IBAN(v_AccountNumber INT(11)) RETURNS VARCHAR(30) DETERMINISTIC
  BEGIN
    set @chk = LPAD(CONVERT(97 - (2836843 + 3 * v_AccountNumber) % 97, CHAR), 2, '0');
    set @bban = CONCAT('3000211111', LPAD(CONVERT(v_AccountNumber, DECIMAL(11)), 11, '0'), @chk);
    set @key = LPAD(CONVERT(98 - (CONVERT(CONCAT(@bban, '152700'), DECIMAL(30)) % 97), CHAR), 2, '0');
    RETURN CONCAT('FR', @key, @bban);
  END
//

CREATE PROCEDURE CreateAccountNoSP (OUT p_AccountId VARCHAR(30),
                                    IN p_MethodUri VARCHAR(50))
  BEGIN
    DECLARE v_NextNumber INT(11);
    DECLARE v_Format VARCHAR(70);

	START TRANSACTION;
	SELECT ACCOUNT_TYPES.NextNumber, ACCOUNT_TYPES.Format
	    INTO v_NextNumber, v_Format FROM ACCOUNT_TYPES
	    WHERE ACCOUNT_TYPES.MethodUri = p_MethodUri
	    FOR UPDATE;
    UPDATE ACCOUNT_TYPES SET NextNumber = v_NextNumber + 1
	    WHERE ACCOUNT_TYPES.MethodUri = p_MethodUri;
	COMMIT;
    SET @format = v_Format;
    SET @nextNumber = v_NextNumber;
    SET @sql = CONCAT('SET @accountId = ', @format);
    PREPARE stmt FROM @sql;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
    SET p_AccountId = @accountId;
  END
//

CREATE PROCEDURE RestoreAccountsSP (IN p_Unconditionally BOOLEAN)
  BEGIN
    DECLARE v_Done BOOLEAN DEFAULT FALSE;
    DECLARE v_AccountId VARCHAR(30);
    DECLARE v_AccountId_cursor CURSOR FOR SELECT AccountId FROM ACCOUNTS;
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET v_Done = TRUE;

    OPEN v_AccountId_cursor;
    REPEAT
      FETCH v_AccountId_cursor INTO v_AccountId;
      IF NOT v_Done THEN
        BEGIN
          DECLARE v_UserId INT;
          DECLARE v_LastAccess TIMESTAMP;
          DECLARE v_Balance DECIMAL(8,2);
          SELECT USERS.UserId, USERS.LastAccess INTO v_UserId, v_LastAccess 
             FROM USERS INNER JOIN ACCOUNTS
             ON USERS.UserId = ACCOUNTS.UserId
             WHERE ACCOUNTS.AccountId = v_AccountId;
          IF v_LastAccess IS NOT NULL THEN
            IF p_Unconditially OR (v_LastAccess < (NOW() - INTERVAL 30 MINUTE)) THEN
              UPDATE USERS SET LastAccess = NULL, AccessCount = 0 WHERE UserId = v_UserID;
              UPDATE ACCOUNTS SET Balance = v_Balance
                  WHERE AccountId = v_AccountId;
            END IF;
          END IF;
        END;
      END IF;
    UNTIL v_Done END REPEAT;
    CLOSE v_AccountId_cursor;
  END
//

DELIMITER ;

CALL CreateAccountTypeSP("https://supercard.com",
                         2390.00,
                         "LPAD(CONVERT(@nextNumber, DECIMAL), 16, '0')",
                         10);

CALL CreateAccountTypeSP("https://bankdirect.net", 
                         5543.00,
                         "FRENCH_IBAN(@nextNumber)",
                         300000000);

CALL CreateAccountTypeSP("https://unusualcard.com", 
                         120.00,
                         "LPAD(CONVERT(@nextNumber, DECIMAL), 16, '0')",
                         78);

-- Demo data
CALL CreateUserSP("Luke Skywalker", @userid);
CALL CreateDemoAccountSP(@userid, "6875056745552109", 
                                  "https://supercard.com",
                                  x'b3b76a196ced26e7e5578346b25018c0e86d04e52e5786fdc2810a2a10bd104a',
                                  NULL);
CALL CreateDemoAccountSP(@userid, "8645-7800239403",
                                  "https://bankdirect.net",
                                  x'892225decf3038bdbe3a7bd91315930e9c5fc608dd71ab10d0fb21583ab8cadd',
                                  x'b3b76a196ced26e7e5578346b25018c0e86d04e52e5786fdc2810a2a10bd104a');
CALL CreateDemoAccountSP(@userid, "1111222233334444", 
                                  "https://unusualcard.com",
                                  x'19aed933edacc289d0d63fba788cf424612d346754d110863cd043b52abecd53',
                                  NULL);


CALL CreateUserSP("Kurtron", @userid);
CALL CreateDemoAccountSP(@userid, "6875056745552108", "https://supercard.com",   x'f3b76a196ced26e7e5578346b25018c0e86d04e52e5786fdc2810a2a10bd104a', NULL);
