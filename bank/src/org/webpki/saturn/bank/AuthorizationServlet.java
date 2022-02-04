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
package org.webpki.saturn.bank;

import io.interbanking.IBRequest;
import io.interbanking.IBResponse;

import java.io.IOException;

import java.math.BigDecimal;

import java.text.SimpleDateFormat;

import java.sql.Connection;

import java.util.Locale;

import org.webpki.json.JSONObjectReader;
import org.webpki.json.JSONObjectWriter;

import org.webpki.saturn.common.PayeeCoreProperties;
import org.webpki.saturn.common.TransactionTypes;
import org.webpki.saturn.common.UrlHolder;
import org.webpki.saturn.common.UserAuthorizationMethods;
import org.webpki.saturn.common.AuthorizationRequestDecoder;
import org.webpki.saturn.common.AuthorizationResponseEncoder;
import org.webpki.saturn.common.NonDirectPaymentDecoder;
import org.webpki.saturn.common.UserChallengeItem;
import org.webpki.saturn.common.PayeeAuthorityDecoder;
import org.webpki.saturn.common.AccountDataDecoder;
import org.webpki.saturn.common.AccountDataEncoder;
import org.webpki.saturn.common.AuthorizationDataDecoder;
import org.webpki.saturn.common.PaymentRequestDecoder;
import org.webpki.saturn.common.ProviderAuthorityDecoder;
import org.webpki.saturn.common.UserResponseItem;

import org.webpki.util.ISODateTime;

/////////////////////////////////////////////////////////////////////////////////
// This is the Saturn core Payment Provider (Bank) authorization servlet       //
/////////////////////////////////////////////////////////////////////////////////

public class AuthorizationServlet extends ProcessingBaseServlet {
  
    private static final long serialVersionUID = 1L;
    
    @Override
    JSONObjectWriter processCall(UrlHolder urlHolder,
                                 JSONObjectReader providerRequest,
                                 Connection connection) throws Exception {
 
        // Decode authorization request message
        AuthorizationRequestDecoder authorizationRequest = new AuthorizationRequestDecoder(providerRequest);

// TODO for idempotent operation and replay protection
//        byte[] authorizationHash = authorizationRequest.getHashOfAuthorization(HashAlgorithms.SHA256);

        // Check that we actually were the intended party
        if (!BankService.serviceUrl.equals(authorizationRequest.getRecipientUrl())) {
            throw new IOException("Unexpected \"" + RECIPIENT_URL_JSON + "\" : " + authorizationRequest.getRecipientUrl());
        }

        // Verify that we understand the backend payment method
        AccountDataDecoder payeeReceiveAccount =
                authorizationRequest.getPayeeReceiveAccount(BankService.knownPayeeMethods);

        // Fetch the payment request object
        PaymentRequestDecoder paymentRequest = authorizationRequest.getPaymentRequest();
        NonDirectPaymentDecoder nonDirectPayment = paymentRequest.getNonDirectPayment();
        boolean cardPayment = authorizationRequest.getPaymentMethod().isCardPayment();
        
        // Get the providers. Note that caching could play tricks on you!
        PayeeAuthorityDecoder payeeAuthority;
        ProviderAuthorityDecoder providerAuthority;
        boolean nonCached = false;
        while (true) {
            // Lookup of Payee
            urlHolder.setNonCachedMode(nonCached);
            payeeAuthority = 
                BankService.externalCalls.getPayeeAuthority(urlHolder,
                                                            authorizationRequest.getPayeeAuthorityUrl());

            // Lookup of Payee's Provider
            urlHolder.setNonCachedMode(nonCached);
            providerAuthority =
                BankService.externalCalls
                        .getProviderAuthority(urlHolder,
                                              payeeAuthority.getProviderAuthorityUrl());

            // Now verify that the Payee is vouched for by a proper entity
            if (providerAuthority.checkPayeeKey(payeeAuthority)) {
                break;
            }

            // No match, should we give up?
            if (nonCached) {
                throw new IOException("Payee attestation key mismatch");
            }
            
            // Edge case?  Yes, but it could happen during key update
            // Note: in case of a key update you MUST update the authority object server
            // *before* using the key for message signing.
            // Note: old Payee keys MUST be published as well, otherwise messages may
            // be rejected.  There is no such need for Providers since they use certificates
            // which permits key updates without breaking things. Possible root CA updates
            // MUST be communicated in advance to all parties before taking it in active use.
            nonCached = !nonCached;
        }

        // Verify that the authority objects were signed by a genuine payment partner
        providerAuthority.getSignatureDecoder().verify(cardPayment ? 
                                          BankService.acquirerRoot : BankService.paymentRoot);

        // Verify Payee signature key.  It may be one generation back as well
        PayeeCoreProperties payeeCoreProperties = payeeAuthority.getPayeeCoreProperties();
        payeeCoreProperties.verify(authorizationRequest.getSignatureDecoder());

        // Optionally verify the claimed Payee account
        payeeCoreProperties.verifyAccount(payeeReceiveAccount);

        // Decrypt and validate the encrypted Payer authorization
        AuthorizationDataDecoder authorizationData = authorizationRequest
                .getDecryptedAuthorizationData(BankService.decryptionKeys,
                                               BankService.AUTHORIZATION_SIGNATURE_POLICY);
        if (BankService.logging) {
            logger.info("Decrypted user authorization:\n" + 
                        authorizationData.getAuthorizationObject());
        }

        // Verify that the there is a matching Payer account
        String accountId = authorizationData.getAccountId();
        String userName = DataBaseOperations.authenticateAuthorization(
                                            authorizationData.getCredentialId(),
                                            accountId, 
                                            authorizationRequest.getPaymentMethod(),
                                            authorizationData.getPublicKey(), 
                                            connection);

        // We don't accept requests that are old or ahead of time
        long diff = System.currentTimeMillis() - authorizationData.getTimeStamp().getTimeInMillis();
        if (diff > (MAX_CLIENT_CLOCK_SKEW + MAX_CLIENT_AUTH_AGE) || diff < -MAX_CLIENT_CLOCK_SKEW) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z", Locale.US);
            sdf.setTimeZone(authorizationData.getTimeStamp().getTimeZone());
            BankService.rejectedTransactions++;
            return createProviderUserResponse("Either your request is older than " + 
                                                (MAX_CLIENT_AUTH_AGE / 60000) +
                                                " minutes, or your device clock is incorrect.<p>Timestamp=" +
                                                "<span style='white-space:nowrap'>" + 
                                                sdf.format(authorizationData.getTimeStamp().getTime()) +
                                                "</span>.</p>",
                                              null,
                                              authorizationData);
        }
            
        ////////////////////////////////////////////////////////////////////////////
        // We got an authentic request.  Now we need to check available funds etc.//
        ////////////////////////////////////////////////////////////////////////////
        BigDecimal amount = paymentRequest.getAmount();

        // First we apply RBA v0.001...
        // Merchant provides the client's IP address which also could be used for RBA
        String clientIpAddress = authorizationRequest.getClientIpAddress();
        UserResponseItem userResponseItem;
        if (amount.compareTo(DEMO_RBA_LIMIT) >= 0 &&
            (((userResponseItem = authorizationData.getUserResponseItems().get(RBA_PARM_MOTHER)) == null) ||
            (!userResponseItem.getValue().equals(MOTHER_NAME)))) {
            BankService.rejectedTransactions++;
            boolean specialTest = amount.compareTo(DEMO_RBA_LIMIT_CT) == 0;
            return createProviderUserResponse("Transaction requests exceeding " +
                                                amountInHtml(paymentRequest, DEMO_RBA_LIMIT) +
                                                " require additional user authentication to " +
                                                "be performed. Please enter your " +
                                                "<span style='color:blue'>mother's maiden name</span>." +
                                                "<br>&nbsp;<br>Since <i>this is a demo</i>, " +
                                                "answer <span style='color:red'>" + 
                                                MOTHER_NAME + 
                                                "</span>&nbsp;&#x1f642;",
              new UserChallengeItem[]{new UserChallengeItem(RBA_PARM_MOTHER,
                                                            specialTest ?
                                             UserChallengeItem.TYPE.ALPHANUMERIC
                                                                        : 
                                             UserChallengeItem.TYPE.ALPHANUMERIC_SECRET,
                                                             specialTest ? 
                                                 "Mother's maiden name" : null)},
                                              authorizationData);
        }

        // A very special test just for showing more what RBA can do...
        if (authorizationData.getUserAuthorizationMethod() != UserAuthorizationMethods.PIN &&
            amount.compareTo(DEMO_FINGER_PRINT_TEST) == 0) {
            BankService.rejectedTransactions++;
            return createProviderUserResponse("Demo alert &#x1f642 For the transaction amount " +
                                                amountInHtml(paymentRequest, DEMO_FINGER_PRINT_TEST) +
                                                " you need to use a PIN code.",
                                              null,
                                              authorizationData);
            }        

        TransactionTypes transactionType = 
                !cardPayment && nonDirectPayment == null ? 
                           TransactionTypes.INSTANT : TransactionTypes.RESERVE;
        int transactionId;
        boolean testMode = authorizationRequest.getTestMode();
        String optionalLogData = null;
        if (testMode) {
            // In test mode we only authenticate using the "real" solution, the rest is "fake".
            transactionId = BankService.testReferenceId++;
        } else {
            // Here we actually update things...
            transactionId = 
                    DataBaseOperations.externalWithDraw(amount,
                                                        accountId,
                                                        transactionType,
                                                        payeeReceiveAccount.getAccountId(),
                                                        paymentRequest.getPayeeCommonName(),
                                                        paymentRequest.getReferenceId(),
                                                        null,
                                                        false,
                                                        connection);
            if (transactionId == 0) {
                return createProviderUserResponse("Your request for " + 
                                                  amountInHtml(paymentRequest, amount) +
                    " appears to be slightly out of your current capabilities..." +
                    "<br>&nbsp;<br>Since <i>this is a demo</i> your account will be restored " +
                    "in <span style='color:red'>30 minutes</span>&nbsp;&#x1f642;",
                                                  null,
                                                  authorizationData);
            }
            if (!cardPayment && nonDirectPayment == null) {
                //#################################################
                //# Payment backend networking take place here... #
                //#################################################
                //
                // If Payer and Payee are in the same bank networking is not needed
                // and is replaced by a local database account adjust operations.
                //
                // Note that if backend networking for some reason fails, the transaction
                // must be reversed.
                //
                // If successful one could imagine updating the transaction record with
                // a reference to that part as well.
                //
                // Note that card and nonDirectPayments payments only reserve an amount locally.
                try {
                    IBResponse ibResponse = 
                        IBRequest.perform(BankService.payeeInterbankUrl,
                                          IBRequest.Operations.CREDIT_TRANSFER,
                                          accountId, 
                                          null,
                                          amount,
                                          paymentRequest.getCurrency().toString(),
                                          paymentRequest.getPayeeCommonName(),
                                          paymentRequest.getReferenceId(),
                                          payeeReceiveAccount.getAccountId(),
                                          testMode, 
                                          BankService.bankKey);
                    optionalLogData = ibResponse.getOurReference();
                } catch (Exception e) {
                    // Since the external operation failed we must restore the account
                    // with the amount involved.
                    DataBaseOperations.nullifyTransaction(transactionId, connection);
                    throw e;
                }
            }
        }

        // Pure sample data...
        // Separate credit-card and account2account payments
        AccountDataEncoder accountData = cardPayment ?
            new com.supercard.SupercardAccountDataEncoder(
                    accountId, 
                    userName,
                    ISODateTime.parseDateTime("2022-12-31T00:00:00Z", ISODateTime.COMPLETE))
                                                     :
            new org.payments.sepa.SEPAAccountDataEncoder(accountId);

        logger.info((testMode ? "TEST ONLY: ": "") +
                    "Authorized Amount=" + amount.toString() + 
                    ", Transaction Type=" + transactionType.toString() + 
                    ", Transaction ID=" + transactionId + 
                    ", Account ID=" + accountId + 
                    ", Payment Method=" + authorizationData.getPaymentMethodUrl() + 
                    ", Client IP=" + clientIpAddress +
                    ", Receive Account=" + payeeReceiveAccount.logLine());

        // We did it!
        BankService.successfulTransactions++;
        return AuthorizationResponseEncoder.encode(authorizationRequest,
                                                   providerAuthority.getEncryptionParameters()[0],
                                                   accountData,
                                                   accountData.getPartialAccountIdentifier(accountId),
                                                   formatReferenceId(transactionId),
                                                   optionalLogData,
                                                   BankService.bankKey);
    }
}
