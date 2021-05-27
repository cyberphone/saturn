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
    String VALUE_JSON                       = "value";                      // ProviderUserResponse & Barcode
    String TYPE_JSON                        = "type";                       // ProviderUserResponse & Barcode
    String LABEL_JSON                       = "label";                      // ProviderUserResponse
    String ENCRYPTED_MESSAGE_JSON           = "encryptedMessage";           // ProviderUserResponse

    String ENCRYPTION_KEY_JSON              = "encryptionKey";              // Wallet-originated encryption key
    
    String USER_RESPONSE_ITEMS_JSON         = "userResponseItems";          // Result of ProviderUserResponse 

    String NO_MATCHING_METHODS_URL_JSON     = "noMatchingMethodsUrl";       // Optional tip by Payee to Payer in case nothing matches
    String PAYMENT_REQUEST_JSON             = "paymentRequest";
    String HTTP_VERSIONS_JSON               = "httpVersions";               // List of understood HTTP versions
    String EXPIRES_JSON                     = "expires";                    // Object expiration time
    String RECIPIENT_URL_JSON               = "recipientUrl";               // Where we are sending (target address)
    String SERVICE_URL_JSON                 = "serviceUrl";                 // Saturn core
    String HOSTING_URL_JSON                 = "hostingUrl";                 // For hosting entities
    String RECEIPT_URL_JSON                 = "receiptUrl";                 // For fetching optional receipts
    String LOGOTYPE_URL_JSON                = "logotypeUrl";                // For "authority" objects
    String AMOUNT_JSON                      = "amount";                     // Money
    String CURRENCY_JSON                    = "currency";                   // In this format
    String NON_DIRECT_PAYMENT_JSON          = "nonDirectPayment";           // Deposit, automated gas station, booking
    String TIME_STAMP_JSON                  = "timeStamp";                  // Everywhere
    String TRANSACTION_ERROR_JSON           = "transactionError";
    String CLIENT_IP_ADDRESS_JSON           = "clientIpAddress";            // Security data for the payment provider
    String CLIENT_GEO_LOCATION_JSON         = "clientGeoLocation";          // Optional security data that client devices may supply
    String LOG_DATA_JSON                    = "logData";                    // Insertion of external log data
    String REFERENCE_ID_JSON                = "referenceId";                // Unique reference in a message
    String LOCAL_PAYEE_ID_JSON              = "localPayeeId";               // Provider's local ID of Payee
    String TEST_MODE_JSON                   = "testMode";                   // Test mode = no real money involved
    String ENCRYPTED_AUTHORIZATION_JSON     = "encryptedAuthorization";     // Encrypted Payer authorization data
    String PROVIDER_AUTHORITY_URL_JSON      = "providerAuthorityUrl";       // URL to payment provider authority object
    String PAYEE_AUTHORITY_URL_JSON         = "payeeAuthorityUrl";          // URL to peyee authority object
    String HOSTING_PROVIDERS_JSON           = "hostingProviders";           // Optional array in ProviderAuthority
    String HOME_PAGE_JSON                   = "homePage";                   // URL to the public Web of the entity
    String SUPPORTED_PAYMENT_METHODS_JSON   = "supportedPaymentMethods";    // List of accepted payment methods (URLs)
    String PAYMENT_METHOD_JSON              = "paymentMethod";              // Payment method (URL)
    String AUTHORIZATION_SIGNATURE_JSON     = "authorizationSignature";     // User and bank authorization signature
    String STATUS_JSON                      = "status";                     // See Receipts
    String PHYSICAL_ADDRESS_JSON            = "physicalAddress";            //    -"-
    String PHONE_NUMBER_JSON                = "phoneNumber";                //    -"-
    String EMAIL_ADDRESS_JSON               = "emailAddress";               //    -"-
    String PAYMENT_METHOD_NAME_JSON         = "paymentMethodName";          //    -"-
    String PAYEE_REQUEST_ID_JSON            = "payeeRequestId";             //    -"-
    String PAYER_PROVIDER_DATA_JSON         = "payerProviderData";          //    -"-
    String TAX_JSON                         = "tax";                        //    -"- [2] Top and/or Line item level (O)
    String PERCENTAGE_JSON                  = "percentage";                 //    -"- [2] must always be used with TAX_JSON
    String SUBTOTAL_JSON                    = "subtotal";                   //    -"- Line item (O), Top level (O)
    String SHIPPING_JSON                    = "shipping";                   //    -"- Top level (O)   
    String FREE_TEXT_JSON                   = "freeText";                   //    -"- Text (O)
    String BARCODE_JSON                     = "barcode";                    //    -"- (O)
    String DISCOUNT_JSON                    = "discount";                   //    -"-           [2]
    String LINE_ITEMS_JSON                  = "lineItems";                  //    -"- Header (M)
    String SKU_JSON                         = "sku";                        //    -"- Line item (O)
    String UNIT_JSON                        = "unit";                       //    -"- Line item (O)  Example: Litre
    String PRICE_JSON                       = "price";                      //    -"- Line item (O)  Note: per unit
    String QUANTITY_JSON                    = "quantity";                   //    -"- Line item (M)
    String DESCRIPTION_JSON                 = "description";                //    -"- Line item (M)
    String RECEIPT_SIGNATURE_JSON           = "receiptSignature";           //    -"- (M)
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
    String HASHED_PAYEE_ACCOUNTS_JSON       = "hashedPayeeAccounts";        //
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
