/*
 *  Copyright 2015-2020 WebPKI.org (http://webpki.org).
 *
 *  Licensed under the Apache License, Version 2.0 (the 'License'),
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an 'AS IS' BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
 
'use strict';

const BaseProperties = {

  ISSUER_SIGNATURE_JSON           : 'issuerSignature',            // Used for Authority Object signatures

  COMMON_NAME_JSON                : 'commonName',                 // Common name of providers to be used in UIs

  REQUESTER_JSON                  : 'requester',                  // ProviderUserResponse
  TEXT_JSON                       : 'text',                       // ProviderUserResponse
  USER_CHALLENGE_ITEMS_JSON       : 'userChallengeItems',         // ProviderUserResponse
  NAME_JSON                       : 'name',                       // ProviderUserResponse & more
  VALUE_JSON                      : 'value',                      // ProviderUserResponse
  TYPE_JSON                       : 'type',                       // ProviderUserResponse
  LABEL_JSON                      : 'label',                      // ProviderUserResponse
  ENCRYPTED_MESSAGE_JSON          : 'encryptedMessage',           // ProviderUserResponse

  ENCRYPTION_KEY_JSON             : 'encryptionKey',              // Wallet-originated encryption key
    
  USER_RESPONSE_ITEMS_JSON        : 'userResponseItems',          // Result of ProviderUserResponse 

  NO_MATCHING_METHODS_URL_JSON    : 'noMatchingMethodsUrl',       // Optional tip by Payee to Payer in case nothing matches
  PAYMENT_REQUEST_JSON            : 'paymentRequest',
  HTTP_VERSIONS_JSON              : 'httpVersions',               // List of understood HTTP versions
  EXPIRES_JSON                    : 'expires',                    // Object expiration time
  RECIPIENT_URL_JSON              : 'recipientUrl',               // Where we are sending (target address)
  SERVICE_URL_JSON                : 'serviceUrl',                 // Saturn core
  HOSTING_URL_JSON                : 'hostingUrl',                 // For hosting entities
  AMOUNT_JSON                     : 'amount',                     // Money
  CURRENCY_JSON                   : 'currency',                   // In this format
  NON_DIRECT_PAYMENT_JSON         : 'nonDirectPayment',           // Deposit, automated gas station, booking
  TIME_STAMP_JSON                 : 'timeStamp',                  // Everywhere
  TRANSACTION_ERROR_JSON          : 'transactionError',
  CLIENT_IP_ADDRESS_JSON          : 'clientIpAddress',            // Security data for the payment provider
  CLIENT_GEO_LOCATION_JSON        : 'clientGeoLocation',          // Optional security data that client devices may supply
  LOG_DATA_JSON                   : 'logData',                    // Insertion of external log data
  REFERENCE_ID_JSON               : 'referenceId',                // Unique reference in a message
  PAYEE_JSON                      : 'payee',                      // Payee object used in PaymentRequest
  LOCAL_PAYEE_ID_JSON             : 'localPayeeId',               // Provider's local ID of Payee
  TEST_MODE_JSON                  : 'testMode',                   // Test mode = no real money involved
  ENCRYPTED_AUTHORIZATION_JSON    : 'encryptedAuthorization',     // Encrypted Payer authorization data
  PROVIDER_AUTHORITY_URL_JSON     : 'providerAuthorityUrl',       // URL to payment provider authority object
  PAYEE_AUTHORITY_URL_JSON        : 'payeeAuthorityUrl',          // URL to peyee authority object
  HOSTING_PROVIDERS_JSON          : 'hostingProviders',           // Optional array in ProviderAuthority
  HOME_PAGE_JSON                  : 'homePage',                   // URL to the public Web of the entity
  SUPPORTED_PAYMENT_METHODS_JSON  : 'supportedPaymentMethods',    // List of accepted payment methods (URLs)
  PAYMENT_METHOD_JSON             : 'paymentMethod',              // Payment method (URL)
  AUTHORIZATION_SIGNATURE_JSON    : 'authorizationSignature',     // User and bank authorization signature
  USER_AUTHORIZATION_METHOD_JSON  : 'userAuthorizationMethod',    // PIN, Fingerprint, etc.
  REQUEST_SIGNATURE_JSON          : 'requestSignature',           // Payee signature
  SUB_TYPE_JSON                   : 'subType',                    // For non-direct payments
  FIXED_JSON                      : 'fixed',                      //    -"-
  INTERVAL_JSON                   : 'interval',                   //    -"-
  INSTALLMENTS_JSON               : 'installments',               //    -"-
  EXTENSIONS_JSON                 : 'extensions',                 // Optional provider authority data
  ACCOUNT_ID_JSON                 : 'accountId',                  // Account identifier or PAN
  CREDENTIAL_ID_JSON              : 'credentialId',               // Each virtual card has a unique ID
  ACCOUNT_REFERENCE_JSON          : 'accountReference',           // Account/Card number for payee (like ************5678)
  ENCRYPTED_ACCOUNT_DATA_JSON     : 'encryptedAccountData',       // Account data that (only) an acquirer needs
  PAYEE_RECEIVE_ACCOUNT_JSON      : 'payeeReceiveAccount',        // Holding payee account data
  PAYEE_SOURCE_ACCOUNT_JSON       : 'payeeSourceAccount',         // For refunds
  REQUEST_HASH_JSON               : 'requestHash',                // Wallet authorization
  PAYEE_HOST_JSON                 : 'payeeHost',                  // As given to the wallet
  ACCOUNT_VERIFIER_JSON           : 'accountVerifier',            // Option for "PayeeAuthority"
  PLATFORM_JSON                   : 'platform',                   // Wallet (client) platform
  VENDOR_JSON                     : 'vendor',                     // Hardware
  HASHED_PAYEE_ACCOUNTS_JSON      : 'hashedPayeeAccounts',        //    -"-
  NONCE_JSON                      : 'nonce',                      // For usage in methods together with the option above
  SIGNATURE_ALGORITHM_JSON        : 'signatureAlgorithm',
  SIGNATURE_PROFILES_JSON         : 'signatureProfiles',          // For "ProviderAuthority".  Accepts these
  KEY_ENCRYPTION_ALGORITHM_JSON   : 'keyEncryptionAlgorithm',     // For "ProviderAuthority" encryption key
  DATA_ENCRYPTION_ALGORITHM_JSON  : 'dataEncryptionAlgorithm',    //    -"-
  ENCRYPTION_PARAMETERS_JSON      : 'encryptionParameters',       //    -"-
  REQUEST_HASH_ALGORITHM_JSON     : 'requestHashAlgorithm',       // Used by virtual cards
  SIGNATURE_PARAMETERS_JSON       : 'signatureParameters',        // For "PayeeAuthority" signature key(s)
  SOFTWARE_JSON                   : 'software',
  VERSION_JSON                    : 'version',

  SATURN_WEB_PAY_CONTEXT_URI      : 'https://webpki.github.io/saturn/v3',

                                                                  // Optional for QR schemes terminating locally
  SATURN_LOCAL_SUCCESS_URI        : 'https://webpki.github.io/saturn/v3/uris#local-success', 

  JSON_CONTENT_TYPE               : 'application/json'
};
  
module.exports = BaseProperties;
