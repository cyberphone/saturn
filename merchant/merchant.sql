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
-- # This is the Merchantr side of a PoC database holding data #
-- # for TBD                                                   #
-- #############################################################

USE MERCHANT;

/*=============================================*/
/*                  RECEIPTS                   */
/*=============================================*/

CREATE TABLE RECEIPTS (
    SequenceId  VARCHAR(20)   NOT NULL,                                  -- Unique sequence Id
    
    Receipt     TEXT          NOT NULL,                                  -- The receipt itself in JSON format

    Created     TIMESTAMP     NOT NULL  DEFAULT CURRENT_TIMESTAMP,       -- Administrator data

    PRIMARY KEY (SequenceId)
);
