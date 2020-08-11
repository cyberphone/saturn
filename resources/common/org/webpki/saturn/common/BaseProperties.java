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
package org.webpki.saturn.common;

public interface BaseProperties {

    String ISSUER_SIGNATURE_JSON            = "issuerSignature";            // Used for Authority Object signatures

    String COMMON_NAME_JSON                 = "commonName";                 // Common name of providers to be used in UIs

    String REQUESTER_JSON                   = "requester";                  // ProviderUserResponse
    String TEXT_JSON                        = "text";                       // ProviderUserResponse
    String USER_CHALLENGE_ITEMS_JSON        = "userChallengeItems";         // ProviderUserResponse
    String NAME_JSON                        = "name";                       // ProviderUserResponse & more
    String VALUE_JSON                       = "value";                      // ProviderUserResponse
    String TYPE_JSON                        = "type";                       // ProviderUserResponse
    String LABEL_JSON                       = "label";                      // ProviderUserResponse
    String ENCRYPTED_MESSAGE_JSON           = "encryptedMessage";           // ProviderUserResponse

    String ENCRYPTION_KEY_JSON              = "encryptionKey";              // Wallet-originated encryption key
    
    String USER_RESPONSE_ITEMS_JSON         = "userResponseItems";          // Result of ProviderUserResponse 

    String NO_MATCHING_METHODS_URL_JSON     = "noMatchingMethodsUrl";       // Optional tip by Payee to Payer in case nothing matches
    String PAYMENT_REQUEST_JSON             = "paymentRequest";
    String HTTP_VERSION_JSON                = "httpVersion";                // For per partner being able to use HTTP/2 and further
    String EXPIRES_JSON                     = "expires";                    // Object expiration time
    String RECIPIENT_URL_JSON               = "recipientUrl";               // Where we are sending (target address)
    String SERVICE_URL_JSON                 = "serviceUrl";                 // Saturn core
    String AMOUNT_JSON                      = "amount";                     // Money
    String CURRENCY_JSON                    = "currency";                   // In this format
    String NON_DIRECT_PAYMENT_JSON          = "nonDirectPayment";           // Deposit, automated gas station, booking
    String TIME_STAMP_JSON                  = "timeStamp";                  // Everywhere
    String TRANSACTION_ERROR_JSON           = "transactionError";
    String CLIENT_IP_ADDRESS_JSON           = "clientIpAddress";            // Security data for the payment provider
    String CLIENT_GEO_LOCATION_JSON         = "clientGeoLocation";          // Optional security data that client devices may supply
    String LOG_DATA_JSON                    = "logData";                    // Insertion of external log data
    String REFERENCE_ID_JSON                = "referenceId";                // Unique reference in a message
    String PAYEE_JSON                       = "payee";                      // Payee object used in PaymentRequest
    String LOCAL_PAYEE_ID_JSON              = "localPayeeId";               // Provider's local ID of Payee
    String TEST_MODE_JSON                   = "testMode";                   // Test mode = no real money involved
    String ENCRYPTED_AUTHORIZATION_JSON     = "encryptedAuthorization";     // Encrypted Payer authorization data
    String PROVIDER_AUTHORITY_URL_JSON      = "providerAuthorityUrl";       // URL to payment provider authority object
    String PAYEE_AUTHORITY_URL_JSON         = "payeeAuthorityUrl";          // URL to peyee authority object
    String HOSTING_PROVIDER_JSON            = "hostingProvider";            // Optional object in ProviderAuthority
    String HOME_PAGE_JSON                   = "homePage";                   // URL to the public Web of the entity
    String SUPPORTED_PAYMENT_METHODS_JSON   = "supportedPaymentMethods";    // List of accepted payment methods (URLs)
    String PAYMENT_METHOD_JSON              = "paymentMethod";              // Payment method (URL)
    String AUTHORIZATION_SIGNATURE_JSON     = "authorizationSignature";     // User and bank authorization signature
    String USER_AUTHORIZATION_METHOD_JSON   = "userAuthorizationMethod";    // PIN, Fingerprint, etc.
    String REQUEST_SIGNATURE_JSON           = "requestSignature";           // Payee signature
    String SUB_TYPE_JSON                    = "subType";                    // For non-direct payments
    String FIXED_JSON                       = "fixed";                      //    -"-
    String INTERVAL_JSON                    = "interval";                   //    -"-
    String INSTALLMENTS_JSON                = "installments";               //    -"-
    String EXTENSIONS_JSON                  = "extensions";                 // Optional provider authority data
    String ACCOUNT_ID_JSON                  = "accountId";                  // Account identifier or PAN
    String CREDENTIAL_ID_JSON               = "credentialId";               // Each virtual card has a unique ID
    String ACCOUNT_REFERENCE_JSON           = "accountReference";           // Account/Card number for payee (like ************5678)
    String ENCRYPTED_ACCOUNT_DATA_JSON      = "encryptedAccountData";       // Account data that (only) an acquirer needs
    String PAYEE_RECEIVE_ACCOUNT_JSON       = "payeeReceiveAccount";        // Holding payee account data
    String PAYEE_SOURCE_ACCOUNT_JSON        = "payeeSourceAccount";         // For refunds
    String REQUEST_HASH_JSON                = "requestHash";                // Wallet authorization
    String PAYEE_HOST_JSON                  = "payeeHost";                  // As given to the wallet
    String ACCOUNT_VERIFIER_JSON            = "accountVerifier";            // Option for "PayeeAuthority"
    String PLATFORM_JSON                    = "platform";                   // Wallet (client) platform
    String VENDOR_JSON                      = "vendor";                     // Hardware
    String HASHED_PAYEE_ACCOUNTS_JSON       = "hashedPayeeAccounts";        //    -"-
    String NONCE_JSON                       = "nonce";                      // For usage in methods together with the option above
    String SIGNATURE_ALGORITHM_JSON         = "signatureAlgorithm";
    String SIGNATURE_PROFILES_JSON          = "signatureProfiles";          // For "ProviderAuthority".  Accepts these
    String KEY_ENCRYPTION_ALGORITHM_JSON    = "keyEncryptionAlgorithm";     // For "ProviderAuthority" encryption key
    String DATA_ENCRYPTION_ALGORITHM_JSON   = "dataEncryptionAlgorithm";    //    -"-
    String ENCRYPTION_PARAMETERS_JSON       = "encryptionParameters";       //    -"-
    String REQUEST_HASH_ALGORITHM_JSON      = "requestHashAlgorithm";       // Used by virtual cards
    String SIGNATURE_PARAMETERS_JSON        = "signatureParameters";        // For "PayeeAuthority" signature key(s)
    String SOFTWARE_JSON                    = "software";
    String VERSION_JSON                     = "version";

    String SATURN_WEB_PAY_CONTEXT_URI       = "https://webpki.github.io/saturn/v3";

                                                                            // Optional for QR schemes terminating locally
    String SATURN_LOCAL_SUCCESS_URI         = "https://webpki.github.io/saturn/v3/uris#local-success"; 

    String JSON_CONTENT_TYPE                = "application/json";
}
