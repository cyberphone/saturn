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

    EMBEDDED_JSON                    : '@embedded';

    // For the Web2Native Bridge wallet 
    WINDOW_JSON                      : 'window';
    HEIGHT_JSON                      : 'height';
    WIDTH_JSON                       : 'width';

    // For the Android/QR wallet
    ANDROID_CANCEL_URL_JSON          : 'androidCancelUrl';
    ANDROID_SUCCESS_URL_JSON         : 'androidSuccessUrl';
    ANDROID_TRANSACTION_URL_JSON     : 'androidTransactionUrl';

    COMMON_NAME_JSON                 : 'commonName';                 // Common name of providers to be used in UIs
    NAME_JSON                        : 'name';

    TEXT_JSON                        : 'text';                       // ProviderUserResponse
    CHALLENGE_FIELDS_JSON            : 'challengeFields';            // ProviderUserResponse
    LABEL_JSON                       : 'label';                      // ProviderUserResponse
    LENGTH_JSON                      : 'length';                     // ProviderUserResponse
    ENCRYPTED_MESSAGE_JSON           : 'encryptedMessage';           // ProviderUserResponse

    CHALLENGE_RESULTS_JSON           : 'challengeResults';           // Result of ProviderUserResponse 

    PAYMENT_NETWORKS_JSON            : 'paymentNetworks';
    PAYMENT_REQUEST_JSON             : 'paymentRequest';
    HTTP_VERSION_JSON                : 'httpVersion';                // For per partner being able to use HTTP/2 and further
    INITIATOR_AUTHORITY_URL_JSON     : 'initiatorAuthorityUrl';      // Payee bank authority URL
    ACQUIRER_AUTHORITY_URL_JSON      : 'acquirerAuthorityUrl';       // For CreditCard payments
    PAYEE_ACCOUNTS_JSON              : 'payeeAccounts';              // For Account2Account payments
    PAYEE_ACCOUNT_JSON               : 'payeeAccount';               // Selected payee Account2Account
    EXPIRES_JSON                     : 'expires';                    // Object expiration time
    AUTHORITY_URL_JSON               : 'authorityUrl';
    TRANSACTION_URL_JSON             : 'transactionUrl';
    AMOUNT_JSON                      : 'amount';
    CURRENCY_JSON                    : 'currency';
    TIME_STAMP_JSON                  : 'timeStamp';
    TRANSACTION_ID_JSON              : 'transactionId';
    CLIENT_IP_ADDRESS_JSON           : 'clientIpAddress';            // Security data for the payment provider
    CLIENT_GEO_LOCATION_JSON         : 'clientGeoLocation';          // Optional security data that client devices may supply
    REFERENCE_ID_JSON                : 'referenceId';
    PAYEE_JSON                       : 'payee';                      // Payee object
    ENCRYPTED_AUTHORIZATION_JSON     : 'encryptedAuthorization';     // Payer authorization data
    PROVIDER_AUTHORITY_URL_JSON      : 'providerAuthorityUrl';       // URL to payment provider
    ACCEPTED_ACCOUNT_TYPES_JSON      : 'acceptedAccountTypes';       // List of ACCOUNT_TYPE_JSON
    ACCOUNT_JSON                     : 'account';                    // Payer Account2Account or Card
    ACCOUNT_REFERENCE_JSON           : 'accountReference';           // Account/Card number for payee (like ************5678)
    ACCOUNT_TYPE_JSON                : 'accountType';                // Account/Card type in the form of a URI
    ACCOUNT_HOLDER_JSON              : 'accountHolder';              // Card holder
    ACCOUNT_SECURITY_CODE_JSON       : 'accountSecurityCode';        // CCV
    CARD_FORMAT_ACCOUNT_ID_JSON      : 'cardFormatAccountId';        // Display formatting like cards or not
    ENCRYPTED_ACCOUNT_DATA_JSON      : 'encryptedAccountData';       // Account data that (only) an acquirer needs
    REQUEST_HASH_JSON                : 'requestHash';
    DOMAIN_NAME_JSON                 : 'domainName';
    SIGNATURE_ALGORITHM_JSON         : 'signatureAlgorithm';
    KEY_ENCRYPTION_ALGORITHM_JSON    : 'keyEncryptionAlgorithm';     // For "Authority" encryption key
    DATA_ENCRYPTION_ALGORITHM_JSON   : 'dataEncryptionAlgorithm';    //    -"-
    ENCRYPTION_PARAMETERS_JSON       : 'encryptionParameters';       //    -"-
    SOFTWARE_JSON                    : 'software';
    ID_JSON                          : 'id';
    TYPE_JSON                        : 'type';
    FIELD1_JSON                      : 'field1';                     // Used for "additional" account data 
    FIELD2_JSON                      : 'field2';
    FIELD3_JSON                      : 'field3';
    VERSION_JSON                     : 'version';
   
    SATURN_WEB_PAY_CONTEXT_URI       : 'http://xmlns.webpki.org/webpay/v2';

    JSON_CONTENT_TYPE                : 'application/json';

};
    
module.exports = BaseProperties;
