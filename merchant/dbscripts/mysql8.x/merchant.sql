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

-- SQL Script for MySQL 8.x
--
-- root privileges are required!!!
--
-- Clear and create DB to begin with
--
DROP DATABASE IF EXISTS MERCHANT;
CREATE DATABASE MERCHANT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE MERCHANT;
--
-- Create our single user
--
DROP USER IF EXISTS saturn_merchant@localhost;
CREATE USER saturn_merchant@localhost IDENTIFIED BY 'foo123';
--
-- Give this user access
--
GRANT ALL ON MERCHANT.* TO saturn_merchant@localhost;
CREATE DEFINER = root@localhost SQL SECURITY DEFINER
  VIEW v_routines AS SELECT * FROM information_schema.routines;
GRANT SELECT ON v_routines TO saturn_merchant@localhost;
--
-- Create tables and stored procedures
--
-- #############################################################
-- # This is the Merchant side of a PoC database holding data  #
-- # for creating Saturn receipts                              #
-- #############################################################


/*=============================================*/
/*                   ORDERS                    */
/*=============================================*/

CREATE TABLE ORDERS (
    Id                    CHAR(16)  CHARACTER SET latin1  NOT NULL UNIQUE, -- Unique Order Id

    ReceiptPathData       CHAR(22)  CHARACTER SET latin1  NOT NULL,        -- Random path data for receipt URLs
    
    Status                VARCHAR(10)                     NOT NULL,        -- see ReceiptDecoder.java for values
    
    PRIMARY KEY (Id)
);


/*=============================================*/
/*                  ORDER_ID                   */
/*=============================================*/

CREATE TABLE ORDER_ID (
    IssueDate             CHAR(8)  NOT NULL,                               -- yyyymmdd
    
    Instance              INT      NOT NULL                                -- Instance for the actual day
);

INSERT INTO ORDER_ID(IssueDate, Instance) VALUES('00000000', 0);


/*=============================================*/
/*                  RECEIPTS                   */
/*=============================================*/

CREATE TABLE RECEIPTS (
    Id                    CHAR(16)  CHARACTER SET latin1  NOT NULL UNIQUE, -- Unique Order Id
    
    JsonData              BLOB      NOT NULL,                              -- Serialized receipt in JSON

    FOREIGN KEY (Id) REFERENCES ORDERS(Id)
);

DELIMITER //


-- This particular implementation builds on creating the pending order IDs
-- before the transaction is performed.  There are pros and cons with all such
-- schemes. This one leave "holes" in the sequence for failed transactions.

-- Id format: {yyyymmdd} {nnnnnnnn} 
-- ReceiptPathData format: {Base64Url(random(byte[16]))

CREATE PROCEDURE CreateOrderIdSP (OUT p_Id CHAR(16),
                                  IN p_ReceiptPathData CHAR(22))
  BEGIN
    DECLARE v_IssueDate CHAR(8);
    DECLARE v_CurrentIssueDate CHAR(8);
    DECLARE v_Instance INT;
   
    SET v_CurrentIssueDate = DATE_FORMAT(CURRENT_DATE(), '%Y%m%d');
    START TRANSACTION;
    -- Lock ORDER_ID for updates
    SELECT IssueDate, Instance INTO v_IssueDate, v_Instance FROM 
       ORDER_ID LIMIT 1 FOR UPDATE;
    IF v_IssueDate <> v_CurrentIssueDate THEN
      -- New day, new set of receipts starting from 1
      SET v_instance = 0;
      UPDATE ORDER_ID SET IssueDate = v_CurrentIssueDate LIMIT 1;
    END IF;
    SET v_Instance = v_Instance + 1;
    UPDATE ORDER_ID SET Instance = v_Instance LIMIT 1;
    -- Finally, we got a useful order id
    COMMIT;
    SET p_Id = CONCAT(v_CurrentIssueDate, LPAD(CONVERT(v_Instance, CHAR), 8, '0'));
    INSERT INTO ORDERS(Id,
                       ReceiptPathData, 
                       Status) 
        VALUES(p_Id,
               p_ReceiptPathData, 
               'PENDING');
  END
//


CREATE PROCEDURE SaveTransactionSP (IN p_Id CHAR(16) CHARACTER SET latin1,
                                    IN p_JsonData BLOB)
  BEGIN
    UPDATE ORDERS SET Status = 'AVAILABLE' WHERE Id = p_Id;
    INSERT INTO RECEIPTS(Id, JsonData) VALUES(p_Id, p_JsonData);
  END
//

