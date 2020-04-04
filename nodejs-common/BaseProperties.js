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

// JSON properties used in the Web2Native Bridge PoC

const BaseProperties = {

    ATTESTATION_SIGNATURE_JSON      : 'attestationSignature',       // Used for Authority Object signatures

    COMMON_NAME_JSON                : 'commonName',                 // Common name of providers to be used in UIs
    NAME_JSON                       : 'name',

    REQUESTER_JSON                  : 'requester',                  // ProviderUserResponse
    TEXT_JSON                       : 'text',                       // ProviderUserResponse
    USER_CHALLENGE_ITEMS_JSON       : 'userChallengeItems',         // ProviderUserResponse
    LABEL_JSON                      : 'label',                      // ProviderUserResponse
    LENGTH_JSON                     : 'length',                     // ProviderUserResponse
    ENCRYPTED_MESSAGE_JSON          : 'encryptedMessage',           // ProviderUserResponse

    ENCRYPTION_KEY_JSON                        : 'key',                        // Wallet-originated encryption key

    USER_RESPONSE_ITEMS_JSON        : 'userResponseItems',          // Result of ProviderUserResponse 

    NO_MATCHING_METHODS_URL_JSON    : 'noMatchingMethodsUrl',       // Optional tip by Payee to Payer in case nothing matches
    PAYMENT_REQUEST_JSON            : 'paymentRequest',
    HTTP_VERSION_JSON               : 'httpVersion',                // For per partner being able to use HTTP/2 and further
    EXPIRES_JSON                    : 'expires',                    // Object expiration time
    RECEPIENT_URL_JSON              : 'recepientUrl',               // Where we are sending (target address)
    AUTHORITY_URL_JSON              : 'authorityUrl',               // Double use self in *Authority objects and initiator
    SERVICE_URL_JSON                : 'serviceUrl',                 // Saturn core
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
    HOSTING_PROVIDER_JSON           : 'hostingProvider',            // Optional object in ProviderAuthority
    HOME_PAGE_JSON                  : 'homePage',                   // URL to the public Web of the entity
    SUPPORTED_PAYMENT_METHODS_JSON            : 'paymentMethods',             // List of accepted payment methods (URLs)
    PAYMENT_METHOD_JSON             : 'paymentMethod',              // Payment method (URL)
    AUTHORIZATION_SIGNATURE_JSON    : 'authorizationSignature',     // User and bank authorization signature
    REQUEST_SIGNATURE_JSON          : 'requestSignature',           // Payee signature
    EXTENSIONS_JSON                 : 'extensions',                 // Optional provider authority data
    ACCOUNT_ID_JSON                 : 'accountId',                  // Account identifier or PAN
    CREDENTIAL_ID_JSON              : 'credentialId',               // Each virtual card has a unique ID
    ACCOUNT_REFERENCE_JSON          : 'accountReference',           // Account/Card number for payee (like ************5678)
    ENCRYPTED_ACCOUNT_DATA_JSON     : 'encryptedAccountData',       // Account data that (only) an acquirer needs
    PAYEE_RECEIVE_ACCOUNT_JSON      : 'payeeReceiveAccount',        // Holding payee account data
    PAYEE_SOURCE_ACCOUNT_JSON       : 'payeeSourceAccount',         // For refunds
    REQUEST_HASH_JSON               : 'requestHash',                // Wallet authorization
    PAYEE_HOST_JSON                : 'domainName',
    ACCOUNT_VERIFIER_JSON           : 'accountVerifier',            // Option for "PayeeAuthority"
    HASHED_PAYEE_ACCOUNTS_JSON      : 'hashedPayeeAccounts',        //    -"-
    NONCE_JSON                      : 'nonce',                      // For usage in methods together with the option above
    SIGNATURE_ALGORITHM_JSON        : 'signatureAlgorithm',
    SIGNATURE_PROFILES_JSON         : 'signatureProfiles',          // For "ProviderAuthority".  Accepts these
    KEY_ENCRYPTION_ALGORITHM_JSON   : 'keyEncryptionAlgorithm',     // For "ProviderAuthority" encryption key
    DATA_ENCRYPTION_ALGORITHM_JSON  : 'dataEncryptionAlgorithm',    //    -"-
    ENCRYPTION_PARAMETERS_JSON      : 'encryptionParameters',       //    -"-
    SIGNATURE_PARAMETERS_JSON       : 'signatureParameters',        // For "PayeeAuthority" signature key(s)
    SOFTWARE_JSON                   : 'software',
    ID_JSON                         : 'id',
    TYPE_JSON                       : 'type',
    VERSION_JSON                    : 'version',

    SATURN_WEB_PAY_CONTEXT_URI      : 'https://webpki.github.io/saturn/v3',

                                                                     // Optional for QR schemes terminating locally
    SATURN_LOCAL_SUCCESS_URI        : 'https://webpki.github.io/saturn/v3/uris#local-success', 

    JSON_CONTENT_TYPE               : 'application/json'
};
    
module.exports = BaseProperties;
