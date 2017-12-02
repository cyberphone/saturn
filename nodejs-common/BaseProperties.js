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

    // For the Web2Native Bridge wallet 
    WINDOW_JSON                      : 'window',
    HEIGHT_JSON                      : 'height',
    WIDTH_JSON                       : 'width',

    // For the Android/QR wallet
    ANDROID_CANCEL_URL_JSON          : 'androidCancelUrl',
    ANDROID_SUCCESS_URL_JSON         : 'androidSuccessUrl',
    ANDROID_TRANSACTION_URL_JSON     : 'androidTransactionUrl',

    HOME_PAGE_JSON                   : 'homePage',                   // Home page of entity
    COMMON_NAME_JSON                 : 'commonName',                 // Common name of providers to be used in UIs
    NAME_JSON                        : 'name',

    REQUESTER_JSON                   : 'requester',                  // ProviderUserResponse
    TEXT_JSON                        : 'text',                       // ProviderUserResponse
    USER_CHALLENGE_ITEMS_JSON        : 'userChallengeItems',         // ProviderUserResponse
    LABEL_JSON                       : 'label',                      // ProviderUserResponse
    LENGTH_JSON                      : 'length',                     // ProviderUserResponse
    ENCRYPTED_MESSAGE_JSON           : 'encryptedMessage',           // ProviderUserResponse

    KEY_JSON                         : 'key',                        // Wallet-originated encryption key

    USER_RESPONSE_ITEMS_JSON         : 'userResponseItems',          // Result of ProviderUserResponse 

    PAYMENT_NETWORKS_JSON            : 'paymentNetworks',
    PAYMENT_REQUEST_JSON             : 'paymentRequest',
    HTTP_VERSION_JSON                : 'httpVersion',                // For per partner being able to use HTTP/2 and further
    EXPIRES_JSON                     : 'expires',                    // Object expiration time
    RECEPIENT_URL_JSON               : 'recepientUrl',               // Where we are sending (target address)
    AUTHORITY_URL_JSON               : 'authorityUrl',               // Double use self in *Authority objects and initiator
    SERVICE_URL_JSON                 : 'serviceUrl',                 // Saturn core
    AMOUNT_JSON                      : 'amount',
    CURRENCY_JSON                    : 'currency',
    NON_DIRECT_PAYMENT_JSON          : 'nonDirectPayment',           // Deposit, automated gas station, booking
    TIME_STAMP_JSON                  : 'timeStamp',
    PAYMENT_METHOD_JSON              : 'paymentMethod',              // Payment method (URI)
    PAYMENT_METHODS_JSON             : 'paymentMethods',             // List of supported payment methods (URIs)
    PAYMENT_METHOD_SPECIFIC_JSON     : 'paymentMethodSpecific',      // Holding a payment method specific object
    PAYEE_ACCOUNTS_JSON              : 'payeeAccounts',              // Merchant DB element not used by the aquirer
    CLIENT_IP_ADDRESS_JSON           : 'clientIpAddress',            // Security data for the payment provider
    CLIENT_GEO_LOCATION_JSON         : 'clientGeoLocation',          // Optional security data that client devices may supply
    LOG_DATA_JSON                    : 'logData',                    // Insertion of external log data
    REFERENCE_ID_JSON                : 'referenceId',
    PAYEE_JSON                       : 'payee',                      // Payee object
    TEST_MODE_JSON                   : 'testMode',                   // Test mode = no real money involved
    ENCRYPTED_AUTHORIZATION_JSON     : 'encryptedAuthorization',     // Payer authorization data
    PROVIDER_AUTHORITY_URL_JSON      : 'providerAuthorityUrl',       // URL to payment provider
    EXTENSIONS_JSON                  : 'extensions',                 // Optional provider authority data
    ACCOUNT_ID_JSON                  : 'accountId',                  // Actual identifier
    ACCOUNT_REFERENCE_JSON           : 'accountReference',           // Account/Card number for payee (like ************5678)
    CARD_FORMAT_ACCOUNT_ID_JSON      : 'cardFormatAccountId',        // Display formatting like cards or not
    ENCRYPTED_ACCOUNT_DATA_JSON      : 'encryptedAccountData',       // Account data that (only) an acquirer needs
    REQUEST_HASH_JSON                : 'requestHash',
    DOMAIN_NAME_JSON                 : 'domainName',
    SIGNATURE_ALGORITHM_JSON         : 'signatureAlgorithm',
    SIGNATURE_PARAMETERS_JSON        : 'signatureParameters',
    SIGNATURE_PROFILES_JSON          : 'signatureProfiles',          // For "ProviderAuthority".  Accepts these
    KEY_ENCRYPTION_ALGORITHM_JSON    : 'keyEncryptionAlgorithm',     // For "ProviderAuthority" encryption key
    DATA_ENCRYPTION_ALGORITHM_JSON   : 'dataEncryptionAlgorithm',    //    -"-
    ENCRYPTION_PARAMETERS_JSON       : 'encryptionParameters',       //    -"-
    SOFTWARE_JSON                    : 'software',
    ID_JSON                          : 'id',
    TYPE_JSON                        : 'type',
    VERSION_JSON                     : 'version',

    SATURN_WEB_PAY_CONTEXT_URI       : 'http://webpki.org/saturn/v3',

    JSON_CONTENT_TYPE                : 'application/json'
};
    
module.exports = BaseProperties;
