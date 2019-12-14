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
import java.sql.CallableStatement;

import java.util.Locale;

import org.webpki.json.JSONObjectReader;
import org.webpki.json.JSONObjectWriter;

import org.webpki.crypto.HashAlgorithms;

import org.webpki.saturn.common.PayeeCoreProperties;
import org.webpki.saturn.common.UrlHolder;
import org.webpki.saturn.common.AuthorizationRequest;
import org.webpki.saturn.common.AuthorizationResponse;
import org.webpki.saturn.common.UserChallengeItem;
import org.webpki.saturn.common.PayeeAuthority;
import org.webpki.saturn.common.AuthorizationData;
import org.webpki.saturn.common.PaymentRequest;
import org.webpki.saturn.common.ProviderAuthority;
import org.webpki.saturn.common.NonDirectPayments;
import org.webpki.saturn.common.Messages;
import org.webpki.saturn.common.UserResponseItem;

import org.webpki.util.ArrayUtil;
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
        AuthorizationRequest authorizationRequest = new AuthorizationRequest(providerRequest);

        // Check that we actually were the intended party
        if (!BankService.serviceUrl.equals(authorizationRequest.getRecepientUrl())) {
            throw new IOException("Unexpected \"" + RECEPIENT_URL_JSON + "\" : " + authorizationRequest.getRecepientUrl());
        }

        // Verify that we understand the payment method
        AuthorizationRequest.PaymentBackendMethodDecoder paymentMethodSpecific =
            authorizationRequest.getPaymentBackendMethodSpecific(BankService.knownPayeeMethods);

        // Fetch the payment request object
        PaymentRequest paymentRequest = authorizationRequest.getPaymentRequest();
        NonDirectPayments nonDirectPayment = paymentRequest.getNonDirectPayment();
        boolean cardPayment = authorizationRequest.getPaymentMethod().isCardPayment();
        
        // Get the providers. Note that caching could play tricks on you!
        PayeeAuthority payeeAuthority;
        ProviderAuthority providerAuthority;
        boolean nonCached = false;
        while (true) {
            // Lookup of Payee
            urlHolder.setNonCachedMode(nonCached);
            payeeAuthority = 
                BankService.externalCalls.getPayeeAuthority(urlHolder,
                                                            authorizationRequest.getAuthorityUrl());

            // Lookup of Payee's Provider
            urlHolder.setNonCachedMode(nonCached);
            providerAuthority =
                BankService.externalCalls.getProviderAuthority(urlHolder,
                                                               payeeAuthority.getProviderAuthorityUrl());

            // Now verify that they are issued by the same entity
            if (payeeAuthority.getAttestationKey().equals(
                    providerAuthority.getHostingProvider() == null ?
                // Direct attestation of Payee
                providerAuthority.getSignatureDecoder().getCertificatePath()[0].getPublicKey()
                                                                   :
                // Indirect attestation of Payee through a designated Hosting provider
                providerAuthority.getHostingProvider().getPublicKey())) {
                break;
            }

            // No match, should we give up?
            if (nonCached) {
                throw new IOException("Payee attestation key mismatch");
            }
            
            // Edge case?  Yes, but it could happen
            nonCached = !nonCached;
        }

        // Verify that the authority objects were signed by a genuine payment partner
        providerAuthority.getSignatureDecoder().verify(cardPayment ? BankService.acquirerRoot : BankService.paymentRoot);

        // Verify Payee signature key.  It may be one generation back as well
        PayeeCoreProperties payeeCoreProperties = payeeAuthority.getPayeeCoreProperties();
        payeeCoreProperties.verify(authorizationRequest.getSignatureDecoder());

        // Optionally verify the claimed Payee account
        byte[] accountHash = paymentMethodSpecific.getAccountHash();
        if (payeeCoreProperties.getAccountHashes() == null) {
            if (accountHash != null) {
                throw new IOException("Missing \"" + ACCOUNT_VERIFIER_JSON + 
                                      "\" in \"" + Messages.PAYEE_AUTHORITY.toString() + "\"");
            }
        } else {
            if (accountHash == null) {
                throw new IOException("Missing verifiable payee account");
            }
            boolean notFound = true;
            for (byte[] hash : payeeCoreProperties.getAccountHashes()) {
                if (ArrayUtil.compare(accountHash, hash)) {
                    notFound = false;
                    break;
                }
            }
            if (notFound) {
                throw new IOException("Payee account does not match \"" + ACCOUNT_VERIFIER_JSON + 
                                      "\" in \"" + Messages.PAYEE_AUTHORITY.toString() + "\"");
            }
        }

        // Decrypt and validate the encrypted Payer authorization
        AuthorizationData authorizationData = authorizationRequest.getDecryptedAuthorizationData(BankService.decryptionKeys);

        // Verify that the there is a matching Payer account
        String accountId = authorizationData.getAccountId();
        String authorizedPaymentMethod = authorizationData.getPaymentMethod();
/*
        CREATE PROCEDURE AuthenticatePayReqSP (OUT p_Error INT,
                                               OUT p_Name VARCHAR(50),
                                               IN p_CredentialId VARCHAR(30),
                                               IN p_MethodUri VARCHAR(50),
                                               IN p_S256PayReq BINARY(32))
*/
        String userName;
        try (CallableStatement stmt = 
                connection.prepareCall("{call AuthenticatePayReqSP(?, ?, ?, ?, ?)}");) {
            stmt.registerOutParameter(1, java.sql.Types.INTEGER);
            stmt.registerOutParameter(2, java.sql.Types.VARCHAR);
            stmt.setString(3, accountId);
            stmt.setString(4, authorizedPaymentMethod);
            stmt.setBytes(5, HashAlgorithms.SHA256.digest(authorizationData.getPublicKey().getEncoded()));
            stmt.execute();
            int result = stmt.getInt(1);            
            if (result != 0) {
                if (result == 1) {
                    logger.severe("No such account ID: " + accountId);
                    throw new NormalException("No such user account ID");
                }
                if (result == 2) {
                    logger.severe("Wrong payment method: " + authorizedPaymentMethod + " for account ID: " + accountId);
                    throw new NormalException("Wrong payment method");
                }
                logger.severe("Wrong public key for account ID: " + accountId);
                throw new NormalException("Wrong user public key");
            } else {
                userName = stmt.getString(2);
            }
        }

        // We don't accept requests that are old or ahead of time
        long diff = System.currentTimeMillis() - authorizationData.getTimeStamp().getTimeInMillis();
        if (diff > (MAX_CLIENT_CLOCK_SKEW + MAX_CLIENT_AUTH_AGE) || diff < -MAX_CLIENT_CLOCK_SKEW) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z", Locale.US);
            sdf.setTimeZone(authorizationData.getTimeStamp().getTimeZone());
            BankService.rejectedTransactions++;
            return createProviderUserResponse("Either your request is older than " + 
                                                (MAX_CLIENT_AUTH_AGE / 60000) +
                                                " minutes, or your device clock is incorrect.<p>Timestamp=" +
                                                "<span style=\"white-space:nowrap\">" + 
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
            (!userResponseItem.getText().equals(MOTHER_NAME)))) {
            BankService.rejectedTransactions++;
            boolean specialTest = amount.compareTo(DEMO_RBA_LIMIT_CT) == 0;
            return createProviderUserResponse("Transaction requests exceeding " +
                                                amountInHtml(paymentRequest, DEMO_RBA_LIMIT) +
                                                " require additional user authentication to " +
                                                "be performed. Please enter your " +
                                                "<span style=\"color:blue\">mother's maiden name</span>." +
                                                "<br>&nbsp;<br>Since <i>this is a demo</i>, " +
                                                "answer <span style=\"color:red\">" + 
                                                MOTHER_NAME + 
                                                "</span>&nbsp;&#x1f642;",
              new UserChallengeItem[]{new UserChallengeItem(RBA_PARM_MOTHER,
                                                            specialTest ?
                                             UserChallengeItem.TYPE.ALPHANUMERIC
                                                                        : 
                                             UserChallengeItem.TYPE.ALPHANUMERIC_SECRET,
                                                            20,
                                                            specialTest ? 
                                                 "Mother's maiden name" : null)},
              authorizationData);
        }

        TransactionTypes transactionType = 
                !cardPayment && nonDirectPayment == null ? 
                           TransactionTypes.DIRECT_DEBIT : TransactionTypes.RESERVE;
        int transactionId;
        boolean testMode = authorizationRequest.getTestMode();
        String optionalLogData = null;
        if (testMode) {
            // In test mode we only authenticate using the "real" solution, the rest is "fake".
            transactionId = BankService.testReferenceId++;
        } else {
            // Here we actually update things...
            WithDrawFromAccount wdfa = new WithDrawFromAccount(amount,
                                                               accountId,
                                                               transactionType,
                                                               paymentMethodSpecific.getPayeeAccount(),
                                                               paymentRequest.getPayeeCommonName(),
                                                               paymentRequest.getReferenceId(),
                                                               null,
                                                               false,
                                                               connection);
            if (wdfa.getResult() == 0) {
                transactionId = wdfa.getTransactionId();
            } else {
                return createProviderUserResponse("Your request for " + 
                                                  amountInHtml(paymentRequest, amount) +
                        " appears to be slightly out of your current capabilities..." +
                        "<br>&nbsp;<br>Since <i>this is a demo</i> your account will be restored " +
                        "in <span style=\"color:red\">30 minutes</span>&nbsp;&#x1f642;",
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
                                          paymentMethodSpecific.getPayeeAccount(),
                                          testMode, 
                                          BankService.bankKey);
                    optionalLogData = ibResponse.getOurReference();
                } catch (Exception e) {
                    // Since the external operation failed we must restore the account
                    // with the amount involved.
                    new NullifyTransaction(transactionId, connection);
                    throw e;
                }
            }
        }

        // Pure sample data...
        // Separate credit-card and account2account payments
        AuthorizationResponse.AccountDataEncoder accountData = cardPayment ?
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
                    ", Payment Method=" + authorizedPaymentMethod + 
                    ", Client IP=" + clientIpAddress +
                    ", Method Specific=" + paymentMethodSpecific.logLine());

        // We did it!
        BankService.successfulTransactions++;
        return AuthorizationResponse.encode(authorizationRequest,
                                            providerAuthority.getEncryptionParameters()[0],
                                            accountData,
                                            formatReferenceId(transactionId),
                                            optionalLogData,
                                            BankService.bankKey);
    }
}
