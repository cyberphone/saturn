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

    // For the Web2Native Bridge wallet 
    String WINDOW_JSON                      = "window";
    String HEIGHT_JSON                      = "height";
    String WIDTH_JSON                       = "width";

    // For the Android/QR wallet
    String ANDROID_CANCEL_URL_JSON          = "androidCancelUrl";
    String ANDROID_SUCCESS_URL_JSON         = "androidSuccessUrl";
    String ANDROID_TRANSACTION_URL_JSON     = "androidTransactionUrl";

    String COMMON_NAME_JSON                 = "commonName";                 // Common name of providers to be used in UIs
    String NAME_JSON                        = "name";

    String REQUESTER_JSON                   = "requester";                  // ProviderUserResponse
    String TEXT_JSON                        = "text";                       // ProviderUserResponse
    String USER_CHALLENGE_ITEMS_JSON        = "userChallengeItems";         // ProviderUserResponse
    String LABEL_JSON                       = "label";                      // ProviderUserResponse
    String LENGTH_JSON                      = "length";                     // ProviderUserResponse
    String ENCRYPTED_MESSAGE_JSON           = "encryptedMessage";           // ProviderUserResponse

    String KEY_JSON                         = "key";                        // Wallet-originated encryption key

    String USER_RESPONSE_ITEMS_JSON         = "userResponseItems";          // Result of ProviderUserResponse 

    String PAYMENT_NETWORKS_JSON            = "paymentNetworks";
    String PAYMENT_REQUEST_JSON             = "paymentRequest";
    String HTTP_VERSION_JSON                = "httpVersion";                // For per partner being able to use HTTP/2 and further
    String EXPIRES_JSON                     = "expires";                    // Object expiration time
    String RECEPIENT_URL_JSON               = "recepientUrl";               // Where we are sending (target address)
    String AUTHORITY_URL_JSON               = "authorityUrl";               // Double use self in *Authority objects and initiator
    String SERVICE_URL_JSON                 = "serviceUrl";                 // Saturn core
    String AMOUNT_JSON                      = "amount";
    String CURRENCY_JSON                    = "currency";
    String NON_DIRECT_PAYMENT_JSON          = "nonDirectPayment";           // Deposit, automated gas station, booking
    String TIME_STAMP_JSON                  = "timeStamp";
    String TRANSACTION_ERROR_JSON           = "transactionError";
    String CLIENT_IP_ADDRESS_JSON           = "clientIpAddress";            // Security data for the payment provider
    String CLIENT_GEO_LOCATION_JSON         = "clientGeoLocation";          // Optional security data that client devices may supply
    String LOG_DATA_JSON                    = "logData";                    // Insertion of external log data
    String REFERENCE_ID_JSON                = "referenceId";
    String PAYEE_JSON                       = "payee";                      // Payee object
    String TEST_MODE_JSON                   = "testMode";                   // Test mode = no real money involved
    String ENCRYPTED_AUTHORIZATION_JSON     = "encryptedAuthorization";     // Encrypted Payer authorization data
    String PROVIDER_AUTHORITY_URL_JSON      = "providerAuthorityUrl";       // URL to payment provider authority object
    String HOSTING_PROVIDER_JSON            = "hostingProvider";            // Optional object in ProviderAuthority
    String HOME_PAGE_JSON                   = "homePage";                   // URL to the public Web of the entity
    String PAYMENT_METHOD_SPECIFIC_JSON     = "paymentMethodSpecific";      // Holding a payment method specific object
    String PAYMENT_METHODS_JSON             = "paymentMethods";             // List of accepted payment methods (URIs)
    String PAYMENT_METHOD_JSON              = "paymentMethod";              // Payment method (URI)
    String EXTENSIONS_JSON                  = "extensions";                 // Optional provider authority data
    String ACCOUNT_ID_JSON                  = "accountId";                  // Account identifier or PAN
    String ACCOUNT_REFERENCE_JSON           = "accountReference";           // Account/Card number for payee (like ************5678)
    String ACCOUNT_TYPE_JSON                = "accountType";                // Account/Card type in the form of a URI
    String CARD_FORMAT_ACCOUNT_ID_JSON      = "cardFormatAccountId";        // Display formatting like cards or not
    String ENCRYPTED_ACCOUNT_DATA_JSON      = "encryptedAccountData";       // Account data that (only) an acquirer needs
    String REQUEST_HASH_JSON                = "requestHash";
    String DOMAIN_NAME_JSON                 = "domainName";
    String SIGNATURE_ALGORITHM_JSON         = "signatureAlgorithm";
    String SIGNATURE_PROFILES_JSON          = "signatureProfiles";          // For "ProviderAuthority".  Accepts these
    String KEY_ENCRYPTION_ALGORITHM_JSON    = "keyEncryptionAlgorithm";     // For "ProviderAuthority" encryption key
    String DATA_ENCRYPTION_ALGORITHM_JSON   = "dataEncryptionAlgorithm";    //    -"-
    String ENCRYPTION_PARAMETERS_JSON       = "encryptionParameters";       //    -"-
    String SIGNATURE_PARAMETERS_JSON        = "signatureParameters";        // For "PayeeAuthority" signature key(s)
    String SOFTWARE_JSON                    = "software";
    String ID_JSON                          = "id";
    String TYPE_JSON                        = "type";
    String VERSION_JSON                     = "version";

    String SATURN_WEB_PAY_CONTEXT_URI       = "http://webpki.org/saturn/v3";

    String JSON_CONTENT_TYPE                = "application/json";
}
