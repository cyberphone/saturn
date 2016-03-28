/*
 *  Copyright 2006-2016 WebPKI.org (http://webpki.org).
 *
 *  Licensed under the Apache License, Version 2.0 (the 'License'),
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an 'AS IS' BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
 
'use strict';

// JSON properties used in the Web2Native Bridge PoC

const BaseProperties = {

    WINDOW_JSON                       : 'window',
    HEIGHT_JSON                       : 'height',
    WIDTH_JSON                        : 'width',
    COMMON_NAME_JSON                  : 'commonName',
    NAME_JSON                         : 'name',
    PAYMENT_REQUEST_JSON              : 'paymentRequest',
    ACQUIRER_AUTHORITY_URL_JSON       : 'acquirerAuthorityUrl',  // For CreditCard payments
    PAYEE_ACCOUNTS_JSON               : 'payeeAccounts',         // For Account2Account payments
    PAYEE_ACCOUNT_JSON                : 'payeeAccount',          // Selected payee Account2Account
    PAYER_ACCOUNT_JSON                : 'payerAccount',          // Selected payer Account2Account or Card
    EXPIRES_JSON                      : 'expires',               // Object expiration time
    AUTHORITY_URL_JSON                : 'authorityUrl',
    TRANSACTION_URL_JSON              : 'transactionUrl',
    PROVIDER_AUTHORIZATION_JSON       : 'providerAuthorization',
    AMOUNT_JSON                       : 'amount',
    DESCRIPTION_JSON                  : 'description',
    CURRENCY_JSON                     : 'currency',
    ERROR_CODE_JSON                   : 'errorCode',
    TIME_STAMP_JSON                   : 'timeStamp',
    TRANSACTION_ID_JSON               : 'transactionId',
    CLIENT_IP_ADDRESS_JSON            : 'clientIpAddress',       // Security data for the payment provider
    CLIENT_GEO_LOCATION_JSON          : 'clientGeoLocation',     // Optional security data that client devices may supply
    REFERENCE_ID_JSON                 : 'referenceId',
    PAYEE_JSON                        : 'payee',                 // Common name of payee to be used in UIs
    AUTHORIZATION_DATA_JSON           : 'authorizationData',     // Payer authorization data
    PROVIDER_AUTHORITY_URL_JSON       : 'providerAuthorityUrl',  // URL to payment provider
    ACCEPTED_ACCOUNT_TYPES_JSON       : 'acceptedAccountTypes',  // List of ACCOUNT_TYPE_JSON
    ACCOUNT_TYPE_JSON                 : 'accountType',           // Account/Card type in the form of a URI
    ACCOUNT_HOLDER_JSON               : 'accountHolder',         // Card holder
    ACCOUNT_SECURITY_CODE_JSON        : 'accountSecurityCode',   // CCV
    CARD_FORMAT_ACCOUNT_ID_JSON       : 'cardFormatAccountId',   // Display formatting like cards or not
    ACCOUNT_REFERENCE_JSON            : 'accountReference',      // Account/Card number for payee (like ************5678)
    PROTECTED_ACCOUNT_DATA_JSON       : 'protectedAccountData',  // Account data that (only) an acquirer needs
    REQUEST_HASH_JSON                 : 'requestHash',
    VALUE_JSON                        : 'value',
    DOMAIN_NAME_JSON                  : 'domainName',
    ENCRYPTED_DATA_JSON               : 'encryptedData',
    ENCRYPTED_KEY_JSON                : 'encryptedKey',
    STATIC_KEY_JSON                   : 'staticKey',
    EPHEMERAL_KEY_JSON                : 'ephemeralKey',
    SIGNATURE_ALGORITHM_JSON          : 'signatureAlgorithm',
    KEY_ENCRYPTION_ALGORITHM_JSON     : 'keyEncryptionAlgorithm',     // For acquirer encryption key
    DATA_ENCRYPTION_ALGORITHM_JSON    : 'dataEncryptionAlgorithm',    //    -'-
    ENCRYPTION_PARAMETERS_JSON        : 'encryptionParameters',       //    -'-
    ALGORITHM_JSON                    : 'algorithm',
    IV_JSON                           : 'iv',                    // For symmetric encryption
    TAG_JSON                          : 'tag',                   // For symmetric encryption
    CIPHER_TEXT_JSON                  : 'cipherText',
    SOFTWARE_JSON                     : 'software',
    ID_JSON                           : 'id',
    TYPE_JSON                         : 'type',
    FIELD1_JSON                       : 'field1',                //Used for 'additional' account data 
    FIELD2_JSON                       : 'field2',
    FIELD3_JSON                       : 'field3',
    VERSION_JSON                      : 'version',
    
    W2NB_WEB_PAY_CONTEXT_URI          : 'http://xmlns.webpki.org/webpay/v1',

    JSON_CONTENT_TYPE                 : 'application/json'

};
    
module.exports = BaseProperties;
