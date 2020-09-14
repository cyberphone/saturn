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
DROP DATABASE IF EXISTS MERCHANT;
CREATE DATABASE MERCHANT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
--
-- Create our single user
--
DROP USER IF EXISTS saturn_merchant@localhost;
CREATE USER saturn_merchant@localhost IDENTIFIED BY 'foo123';
--
-- Give this user access
--
GRANT ALL ON MERCHANT.* TO saturn_merchant@localhost;
GRANT SELECT ON mysql.proc TO saturn_merchant@localhost;
--
-- Create tables and stored procedures
--
-- #############################################################
-- # This is the Merchant side of a PoC database holding data  #
-- # for TBD                                                   #
-- #############################################################

USE MERCHANT;


/*=============================================*/
/*                   ORDERS                    */
/*=============================================*/

CREATE TABLE ORDERS (
    Id                    CHAR(16)  CHARACTER SET latin1  NOT NULL UNIQUE, -- Unique Order Id

    GasStation            BOOLEAN   NOT NULL,                            -- Special for this system        

    ReceiptPathData       CHAR(22)  CHARACTER SET latin1  NOT NULL,      -- Random path data for receipt URLs
    
    ReceiptStatus         INT       NOT NULL,                            -- 0: Pending, 1: Available
                                                                         -- see ReceiptDecoder.java for other values
    PRIMARY KEY (Id)
);


/*=============================================*/
/*                  ORDER_ID                   */
/*=============================================*/

CREATE TABLE ORDER_ID (
    IssueDate             CHAR(8)  NOT NULL,                             -- yyyymmdd
    
    Instance              INT      NOT NULL                              -- Instance for the actual day
);

INSERT INTO ORDER_ID(IssueDate, Instance) VALUES('00000000', 0);


/*=============================================*/
/*                  PAYMENTS                   */
/*=============================================*/

CREATE TABLE PAYMENTS (
    Id                    CHAR(16)  CHARACTER SET latin1  NOT NULL UNIQUE, -- Unique Order Id
    
    Amount                DECIMAL(8,2)  NOT NULL,                        -- Total

    Currency              CHAR(3)       NOT NULL,                        -- "EUR", "USD", "SEK",..

    PaymentMethodUrl      VARCHAR(50)   NOT NULL,                        -- like "https://bankdirect.org"

    ProviderAuthorityUrl  VARCHAR(100)  NOT NULL,                        -- Payer Bank
    
    ProviderTransactionId VARCHAR(50)   NOT NULL,                        -- Representation in Payer Bank
    
    AccountReference      VARCHAR(20)   NOT NULL,                        -- Shortened form

    FOREIGN KEY (Id) REFERENCES ORDERS(Id)
);

DELIMITER //


-- This particular implementation builds on creating the pending order IDs
-- before the transaction is performed.  There are pros and cons with all such
-- schemes. This one leave "holes" in the sequence for failed transactions.

-- Id format: {yyyymmdd} {nnnnnnnn} 
-- ReceiptPathData format: {Base64Url(random(byte[16]))

CREATE PROCEDURE CreateOrderIdSP (OUT p_Id CHAR(16),
                                  IN p_GasStation BOOLEAN,
                                  IN p_ReceiptPathData CHAR(22))
  BEGIN
    DECLARE v_IssueDate CHAR(8);
    DECLARE v_CurrentIssueDate CHAR(8);
   
    SET v_CurrentIssueDate = DATE_FORMAT(CURRENT_DATE(), '%Y%m%d');
    SELECT IssueDate INTO v_IssueDate FROM ORDER_ID LIMIT 1;
    IF v_IssueDate <> v_CurrentIssueDate THEN
      -- New day, new set of receipts starting from 1
      UPDATE ORDER_ID SET Instance = 0, IssueDate = v_CurrentIssueDate LIMIT 1;
    END IF;
    UPDATE ORDER_ID SET Instance = LAST_INSERT_ID(Instance + 1) LIMIT 1;
    SET p_Id = CONCAT(v_CurrentIssueDate, LPAD(CONVERT(LAST_INSERT_ID(), CHAR), 8, '0'));
    INSERT INTO ORDERS(Id,
                       GasStation,
                       ReceiptPathData, 
                       ReceiptStatus) 
        VALUES(p_Id,
               p_GasStation,
               p_ReceiptPathData, 
               0);
  END
//


CREATE PROCEDURE CreateReceiptSP (IN p_Id CHAR(16) CHARACTER SET latin1,
                                  IN p_Amount DECIMAL(8,2),
                                  IN p_Currency CHAR(3),
                                  IN p_PaymentMethodUrl VARCHAR(50),
                                  IN p_ProviderAuthorityUrl VARCHAR(100),
                                  IN p_ProviderTransactionId VARCHAR(50),
                                  IN p_AccountReference VARCHAR(30))
  BEGIN
    UPDATE ORDERS SET ReceiptStatus = 1 WHERE Id = p_Id;
    INSERT INTO PAYMENTS(Id, 
                         Amount, 
                         Currency,
                         PaymentMethodUrl,
                         ProviderAuthorityUrl,
                         ProviderTransactionId,
                         AccountReference)
        VALUES(p_Id,
               p_Amount, 
               p_Currency,
               p_PaymentMethodUrl,
               p_ProviderAuthorityUrl,
               p_ProviderTransactionId,
               p_AccountReference);
  END
//

