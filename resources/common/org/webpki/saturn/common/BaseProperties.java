/*
 *  Copyright 2006-2016 WebPKI.org (http://webpki.org).
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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

    String EMBEDDED_JSON                    = "@embedded";

     // For the Web2Native Bridge wallet 
    String WINDOW_JSON                      = "window";
    String HEIGHT_JSON                      = "height";
    String WIDTH_JSON                       = "width";

     // For the Android/QR wallet
    String ANDROID_CANCEL_URL_JSON          = "androidCancelUrl";
    String ANDROID_SUCCESS_URL_JSON         = "androidSuccessUrl";
    String ANDROID_TRANSACTION_URL_JSON     = "androidExtendedServiceUrl";

    String COMMON_NAME_JSON                 = "commonName";                 // Common name of providers to be used in UIs
    String NAME_JSON                        = "name";

    String TEXT_JSON                        = "text";                       // ProviderUserResponse
    String CHALLENGE_FIELDS_JSON            = "challengeFields";            // ProviderUserResponse
    String LABEL_JSON                       = "label";                      // ProviderUserResponse
    String LENGTH_JSON                      = "length";                     // ProviderUserResponse
    String ENCRYPTED_MESSAGE_JSON           = "encryptedMessage";           // ProviderUserResponse

    String KEY_JSON                         = "key";                        // Wallet-originated encryption key

    String RESPONSE_TO_CHALLENGE_JSON       = "responseToChallenge";        // Result of ProviderUserResponse 

    String PAYMENT_NETWORKS_JSON            = "paymentNetworks";
    String PAYMENT_REQUEST_JSON             = "paymentRequest";
    String HTTP_VERSION_JSON                = "httpVersion";                // For per partner being able to use HTTP/2 and further
    String ACQUIRER_AUTHORITY_URL_JSON      = "acquirerAuthorityUrl";       // For CreditCard payments
    String PAYEE_ACCOUNT_JSON               = "payeeAccount";               // Selected payee Account2Account
    String EXPIRES_JSON                     = "expires";                    // Object expiration time
    String AUTHORITY_URL_JSON               = "authorityUrl";               // Double use self in *Authority objects and initiator
    String SERVICE_URL_JSON                 = "serviceUrl";                 // Saturn "lite" (standard)
    String EXTENDED_SERVICE_URL_JSON        = "extendedServiceUrl";         // Native Saturn payment system
    String AMOUNT_JSON                      = "amount";
    String CURRENCY_JSON                    = "currency";
    String NON_DIRECT_PAYMENT_JSON          = "nonDirectPayment";           // Deposit, automated gas station, booking
    String TIME_STAMP_JSON                  = "timeStamp";
    String TRANSACTION_ID_JSON              = "transactionId";
    String CLIENT_IP_ADDRESS_JSON           = "clientIpAddress";            // Security data for the payment provider
    String CLIENT_GEO_LOCATION_JSON         = "clientGeoLocation";          // Optional security data that client devices may supply
    String REFERENCE_ID_JSON                = "referenceId";
    String PAYEE_JSON                       = "payee";                      // Payee object
    String ENCRYPTED_AUTHORIZATION_JSON     = "encryptedAuthorization";     // Payer authorization data
    String PROVIDER_AUTHORITY_URL_JSON      = "providerAuthorityUrl";       // URL to payment provider
    String PROVIDER_ACCOUNT_TYPES_JSON      = "providerAccountTypes";       // List of Account2Account types "understood" by provider
    String ACCEPTED_ACCOUNT_TYPES_JSON      = "acceptedAccountTypes";       // List of ACCOUNT_TYPE_JSON
    String ACCOUNT_JSON                     = "account";                    // Payer Account2Account or Card
    String ACCOUNT_REFERENCE_JSON           = "accountReference";           // Account/Card number for payee (like ************5678)
    String ACCOUNT_TYPE_JSON                = "accountType";                // Account/Card type in the form of a URI
    String ACCOUNT_HOLDER_JSON              = "accountHolder";              // Card holder
    String ACCOUNT_SECURITY_CODE_JSON       = "accountSecurityCode";        // CCV
    String CARD_FORMAT_ACCOUNT_ID_JSON      = "cardFormatAccountId";        // Display formatting like cards or not
    String ENCRYPTED_ACCOUNT_DATA_JSON      = "encryptedAccountData";       // Account data that (only) an acquirer needs
    String REQUEST_HASH_JSON                = "requestHash";
    String DOMAIN_NAME_JSON                 = "domainName";
    String SIGNATURE_ALGORITHM_JSON         = "signatureAlgorithm";
    String KEY_ENCRYPTION_ALGORITHM_JSON    = "keyEncryptionAlgorithm";     // For "ProviderAuthority" encryption key
    String DATA_ENCRYPTION_ALGORITHM_JSON   = "dataEncryptionAlgorithm";    //    -"-
    String ENCRYPTION_PARAMETERS_JSON       = "encryptionParameters";       //    -"-
    String SOFTWARE_JSON                    = "software";
    String ID_JSON                          = "id";
    String TYPE_JSON                        = "type";
    String FIELD1_JSON                      = "field1";                     // Used for "additional" account data 
    String FIELD2_JSON                      = "field2";
    String FIELD3_JSON                      = "field3";
    String VERSION_JSON                     = "version";
    
    String SATURN_WEB_PAY_CONTEXT_URI       = "http://xmlns.webpki.org/webpay/v2";

    String JSON_CONTENT_TYPE                = "application/json";
}
