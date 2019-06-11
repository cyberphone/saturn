-- SQL Script for MySQL 5.7
--
-- root priviledges are required!!!
--
-- Clear and create DB to begin with
--
DROP DATABASE IF EXISTS PAYER_BANK;
CREATE DATABASE PAYER_BANK CHARACTER SET utf8;
--
-- Create a user but remove any existing user first
DROP USER IF EXISTS PAYER_BANK@localhost;
--
CREATE USER PAYER_BANK@localhost IDENTIFIED BY 'foo123';
--
-- Let user access
--
GRANT ALL ON PAYER_BANK.* TO PAYER_BANK@localhost;
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
/*                 ACCOUNTS                    */
/*=============================================*/

CREATE TABLE ACCOUNTS
  (
    AccountId   VARCHAR(20)   NOT NULL  UNIQUE,                          -- Unique Account ID
    UserId      INT           NOT NULL,                                  -- Unique User ID (account holder)
    Created     TIMESTAMP     NOT NULL  DEFAULT CURRENT_TIMESTAMP,       -- Administrator data
    Balance     DECIMAL(8,2)  NOT NULL,                                  -- Disponible
    Currency    CHAR(3)       NOT NULL  DEFAULT "EUR",                   -- SEK, USD, EUR etc.
    PRIMARY KEY (AccountId),
    FOREIGN KEY (UserId) REFERENCES USERS(UserId) ON DELETE CASCADE
  );


/*=============================================*/
/*                PAYMENT_METHODS              */
/*=============================================*/

CREATE TABLE PAYMENT_METHODS
  (
    MethodUri   VARCHAR(50)   NOT NULL  UNIQUE,                          -- Unique Method URI
    PRIMARY KEY (MethodUri)
  );


/*=============================================*/
/*                DEFAULT_BALANCES             */
/*=============================================*/

CREATE TABLE DEFAULT_BALANCES
  (
    MethodUri   VARCHAR(50)   NOT NULL,                                  -- Unique Method URI
    Balance     DECIMAL(8,2)  NOT NULL,                                  -- Disponible
    FOREIGN KEY (MethodUri) REFERENCES PAYMENT_METHODS(MethodUri)
  );


/*=============================================*/
/*                CREDENTIALS                  */
/*=============================================*/

CREATE TABLE CREDENTIALS
  (
    AccountId   VARCHAR(20)   NOT NULL,                                  -- Unique User ID
    MethodUri   VARCHAR(50)   NOT NULL,                                  -- Unique Method URI
    Created     TIMESTAMP     NOT NULL  DEFAULT CURRENT_TIMESTAMP,       -- Administrator data
    S256PayReq  BINARY(32)    NOT NULL,                                  -- Payment request key hash 
    S256BalReq  BINARY(32)    NULL,                                      -- Optional: balance key hash 
    FOREIGN KEY (MethodUri) REFERENCES PAYMENT_METHODS(MethodUri),
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
                                  IN p_AccountId VARCHAR(20),
                                  IN p_Balance DECIMAL(8,2))
  BEGIN
    INSERT INTO ACCOUNTS(UserId, AccountId, Balance) VALUES(p_UserId, p_AccountId, p_Balance);
  END
//

CREATE PROCEDURE CreateDemoAccountSP (IN p_UserId INT, 
                                      IN p_AccountId VARCHAR(20),
                                      IN p_MethodUri VARCHAR(50),
                                      IN p_S256PayReq BINARY(32),
                                      IN p_S256BalReq BINARY(32))
  BEGIN
    DECLARE v_Balance DECIMAL(8,2);

	SELECT DEFAULT_BALANCES.Balance INTO v_Balance FROM DEFAULT_BALANCES WHERE MethodUri = p_methodUri;
    CALL CreateAccountSP(p_UserId, p_AccountId, v_Balance);
    INSERT INTO CREDENTIALS(AccountId, MethodUri, S256PayReq, S256BalReq) 
        VALUES(p_AccountId, p_MethodUri, p_S256PayReq, p_S256BalReq);
  END
//

CREATE PROCEDURE CreateMethodSP (IN p_MethodUri VARCHAR(50),
                                 IN p_Balance DECIMAL(8,2))
  BEGIN
    INSERT INTO PAYMENT_METHODS(MethodUri) VALUES(p_MethodUri);
    INSERT INTO DEFAULT_BALANCES(MethodUri, Balance) VALUES(p_MethodUri, p_Balance);
  END
//

CREATE PROCEDURE AuthenticatePayReqSP (OUT p_Error INT,
                                       IN p_AccountId VARCHAR(20),
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
	ELSE                         -- Failed => Find reason
	  IF EXISTS (SELECT * FROM ACCOUNTS WHERE ACCOUNTS.AccountId = p_AccountId) THEN
	    SET p_Error = 3;         -- No such key
        IF EXISTS (SELECT * FROM CREDENTIALS WHERE CREDENTIALS.MethodUri = p_MethodUri) THEN
          SET p_Error = 2;       -- No such method
        END IF;
	  ELSE
	    SET p_Error = 1;         -- No such account
	  END IF;
	END IF;
  END
//

CREATE PROCEDURE AuthenticateBalReqSP (OUT p_Balance DECIMAL(8,2),
                                       OUT p_Error INT,
                                       IN p_AccountId VARCHAR(20),
                                       IN p_S256BalReq BINARY(32))
  BEGIN
    DECLARE v_Balance DECIMAL(8,2);

	SELECT ACCOUNTS.Balance INTO v_Balance FROM ACCOUNTS INNER JOIN CREDENTIALS
        ON ACCOUNTS.AccountId = CREDENTIALS.AccountId
        WHERE CREDENTIALS.AccountId = p_AccountId AND
              CREDENTIALS.S256BalReq = p_S256BalReq;
    IF v_Balance IS NULL THEN    -- Failed => Find reason
	  IF EXISTS (SELECT * FROM ACCOUNTS WHERE ACCOUNTS.AccountId = p_AccountId) THEN
	    SET p_Error = 3;           -- No such key
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
                             IN p_AccountId VARCHAR(20))
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

DELIMITER ;

CALL CreateMethodSP("https://supercard.com",  2390.00);
CALL CreateMethodSP("https://bankdirect.net", 5543.00);
CALL CreateMethodSP("https://unusualcard.com", 120.00);

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
