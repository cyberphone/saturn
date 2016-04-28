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
package org.webpki.saturn.bank;

import java.io.IOException;
import java.io.PrintWriter;

import java.math.BigDecimal;

import java.net.URL;

import java.security.GeneralSecurityException;

import java.util.HashMap;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.webpki.json.JSONDecoderCache;
import org.webpki.json.JSONObjectReader;
import org.webpki.json.JSONObjectWriter;
import org.webpki.json.JSONOutputFormats;
import org.webpki.json.JSONParser;

import org.webpki.net.HTTPSWrapper;

import org.webpki.util.ISODateTime;

import org.webpki.saturn.common.FinalizeCardpayResponse;
import org.webpki.saturn.common.FinalizeCreditResponse;
import org.webpki.saturn.common.FinalizeTransactionRequest;
import org.webpki.saturn.common.FinalizeTransactionResponse;
import org.webpki.saturn.common.ChallengeField;
import org.webpki.saturn.common.MerchantAccountEntry;
import org.webpki.saturn.common.Authority;
import org.webpki.saturn.common.BaseProperties;
import org.webpki.saturn.common.EncryptedData;
import org.webpki.saturn.common.FinalizeRequest;
import org.webpki.saturn.common.AccountDescriptor;
import org.webpki.saturn.common.ReserveOrBasicRequest;
import org.webpki.saturn.common.AuthorizationData;
import org.webpki.saturn.common.ReserveOrBasicResponse;
import org.webpki.saturn.common.Messages;
import org.webpki.saturn.common.PaymentRequest;
import org.webpki.saturn.common.ProtectedAccountData;
import org.webpki.saturn.common.TransactionRequest;
import org.webpki.saturn.common.TransactionResponse;
import org.webpki.saturn.common.UserAccountEntry;
import org.webpki.saturn.common.ProviderUserResponse;

import org.webpki.webutil.ServletUtil;

//////////////////////////////////////////////////////////////////////////
// This is the core Payment Provider (Bank) payment transaction servlet //
//////////////////////////////////////////////////////////////////////////

public class TransactionServlet extends HttpServlet implements BaseProperties {

    private static final long serialVersionUID = 1L;
    
    static Logger logger = Logger.getLogger(TransactionServlet.class.getCanonicalName());
    
    static final String RBA_PARM_MOTHER            = "mother";
    
    static HashMap<String,Integer> requestTypes = new HashMap<String,Integer>();
    
    static final int REQTYPE_PAYEE_INITIAL          = 0;
    static final int REQTYPE_TRANSACTION            = 1;
    static final int REQTYPE_PAYEE_FINALIZE_CREDIT  = 2;
    static final int REQTYPE_PAYEE_FINALIZE_CARDPAY = 3;
    static final int REQTYPE_FINALIZE_TRANSACTION   = 4;
    
    static {
        requestTypes.put(Messages.BASIC_CREDIT_REQUEST.toString(), REQTYPE_PAYEE_INITIAL);
        requestTypes.put(Messages.RESERVE_CREDIT_REQUEST.toString(), REQTYPE_PAYEE_INITIAL);
        requestTypes.put(Messages.RESERVE_CARDPAY_REQUEST.toString(), REQTYPE_PAYEE_INITIAL);

        requestTypes.put(Messages.FINALIZE_CREDIT_REQUEST.toString(), REQTYPE_PAYEE_FINALIZE_CREDIT);

        requestTypes.put(Messages.FINALIZE_CARDPAY_REQUEST.toString(), REQTYPE_PAYEE_FINALIZE_CARDPAY);

        requestTypes.put(Messages.TRANSACTION_REQUEST.toString(), REQTYPE_TRANSACTION);

        requestTypes.put(Messages.FINALIZE_TRANSACTION_REQUEST.toString(), REQTYPE_FINALIZE_TRANSACTION);
    }
    
    static final int TIMEOUT_FOR_REQUEST = 5000;

    static String portFilter(String url) throws IOException {
        // Our JBoss installation has some port mapping issues...
        if (BankService.serverPortMapping == null) {
            return url;
        }
        URL url2 = new URL(url);
        return new URL(url2.getProtocol(),
                       url2.getHost(),
                       BankService.serverPortMapping,
                       url2.getFile()).toExternalForm(); 
    }

    static JSONObjectReader fetchJSONData(HTTPSWrapper wrap, UrlHolder urlHolder) throws IOException {
        if (wrap.getResponseCode() != HttpServletResponse.SC_OK) {
            throw new IOException("HTTP error " + wrap.getResponseCode() + " " + wrap.getResponseMessage() + ": " +
                                  (wrap.getData() == null ? "No other information available" : wrap.getDataUTF8()));
        }
        // We expect JSON, yes
        if (!wrap.getRawContentType().equals(JSON_CONTENT_TYPE)) {
            throw new IOException("Content-Type must be \"" + JSON_CONTENT_TYPE + "\" , found: " + wrap.getRawContentType());
        }
        JSONObjectReader result = JSONParser.parse(wrap.getData());
        if (BankService.logging) {
            logger.info("Call to " + urlHolder.getUrl() + urlHolder.callerAddress +
                        "returned:\n" + result);
        }
        return result;
    }

    static JSONObjectReader postData(UrlHolder urlHolder, JSONObjectWriter request) throws IOException {
        if (BankService.logging) {
            logger.info("About to call " + urlHolder.getUrl() + urlHolder.callerAddress +
                        "with data:\n" + request);
        }
        HTTPSWrapper wrap = new HTTPSWrapper();
        wrap.setTimeout(TIMEOUT_FOR_REQUEST);
        wrap.setHeader("Content-Type", JSON_CONTENT_TYPE);
        wrap.setRequireSuccess(false);
        wrap.makePostRequest(portFilter(urlHolder.getUrl()), request.serializeJSONObject(JSONOutputFormats.NORMALIZED));
        return fetchJSONData(wrap, urlHolder);
    }

    static JSONObjectReader getData(UrlHolder urlHolder) throws IOException {
        if (BankService.logging) {
            logger.info("About to call " + urlHolder.getUrl() + urlHolder.callerAddress);
        }
        HTTPSWrapper wrap = new HTTPSWrapper();
        wrap.setTimeout(TIMEOUT_FOR_REQUEST);
        wrap.setRequireSuccess(false);
        wrap.makeGetRequest(portFilter(urlHolder.getUrl()));
        return fetchJSONData(wrap, urlHolder);
    }

    // The purpose of this class is to enable URL information in exceptions

    class UrlHolder {
        String remoteAddress;
        String contextPath;
        String callerAddress;
        
        UrlHolder(String remoteAddress, String contextPath) {
            this.remoteAddress = remoteAddress;
            this.contextPath = contextPath;
            callerAddress = " [Origin=" + remoteAddress + ", Context=" + contextPath + "] ";
        }

        private String url;

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }
    }
    
    static String getReferenceId() {
        return "#" + (BankService.referenceId++);
    }
    
    static Authority getAuthority(UrlHolder urlHolder) throws IOException {
        return new Authority(getData(urlHolder), urlHolder.getUrl());
    }

    JSONObjectWriter processTransactionRequest(JSONObjectReader request, UrlHolder urlHolder)
    throws IOException, GeneralSecurityException {

        // Decode transaction request message
        TransactionRequest transactionRequest = new TransactionRequest(request);
        
        // Verify that the transaction request is signed by a payment partner
        transactionRequest.getSignatureDecoder().verify(BankService.paymentRoot);

        // Get the embedded by the user and merchant attested payment request
        ReserveOrBasicRequest reserveOrBasicRequest = transactionRequest.getReserveOrBasicRequest();

        // Merchant provides the client's IP address which can be used for RBA
        String clientIpAddress = reserveOrBasicRequest.getClientIpAddress();

        // Decrypt the encrypted user authorization
        AuthorizationData authorizationData =
               reserveOrBasicRequest.getDecryptedAuthorizationData(BankService.decryptionKeys);

        // Verify that the there is a matching user account
        String accountId = authorizationData.getAccountDescriptor().getAccountId();
        String accountType = authorizationData.getAccountDescriptor().getAccountType();
        UserAccountEntry account = BankService.userAccountDb.get(accountId);
        if (account == null) {
            logger.severe("No such account ID: " + accountId);
            throw new IOException("No such user account ID");
        }
        if (!account.getType().equals(accountType)) {
            logger.severe("Wrong account type: " + accountType + " for account ID: " + accountId);
            throw new IOException("Wrong user account type");
        }
        if (!account.getPublicKey().equals(authorizationData.getPublicKey())) {
            logger.severe("Wrong public key for account ID: " + accountId);
            throw new IOException("Wrong user public key");
        }
        logger.info("Authorized AccountID=" + accountId + ", AccountType=" + accountType);

        // Get the embedded (counter-signed) payment request
        PaymentRequest paymentRequest = reserveOrBasicRequest.getPaymentRequest();

        ////////////////////////////////////////////////////////////////////////////
        // We got an authentic request.  Now we need to check available funds etc.//
        // Since we don't have a real bank this part is rather simplistic :-)     //
        ////////////////////////////////////////////////////////////////////////////

        // Sorry but you don't appear to have a million bucks :-)
        if (!reserveOrBasicRequest.getMessage().isCardPayment() &&
            paymentRequest.getAmount().compareTo(new BigDecimal("1000000.00")) >= 0) {
            return ProviderUserResponse.encode(BankService.bankCommonName,
                                               "<html>Your request for " + 
            paymentRequest.getCurrency().amountToDisplayString(paymentRequest.getAmount()) +
                                               " appears to be<br>slightly out of your current capabilities...</html>",
                                               null);
        }

        // RBA v0.001...
        if (!reserveOrBasicRequest.getMessage().isCardPayment() &&
            paymentRequest.getAmount().compareTo(new BigDecimal("100000.00")) >= 0 &&
            (authorizationData.getOptionalChallengeResults() == null ||
             !authorizationData.getOptionalChallengeResults()[0].getText().equals("garbo"))) {
            return ProviderUserResponse.encode(BankService.bankCommonName,
                                               "<html>This transaction requires additional information to<br> " +
                                               "be performed.  Please enter your <font color=\"blue\">mother's maiden<br>name</font> " +
                                               "and click the validate button.<br>&nbsp;<br>Since <i>this is a demo</i>, " +
                                               "answer <font color=\"red\">garbo</font>&nbsp; :-)",
                                               new ChallengeField[]{new ChallengeField(RBA_PARM_MOTHER,
                                                                               ChallengeField.TYPE.ALPHANUMERIC,
                                                                               20,
                                                                               null)});
        }

        // Separate credit-card and account2account payments
        AccountDescriptor payeeAccount = null;
        JSONObjectWriter encryptedCardData = null;
        if (reserveOrBasicRequest.getMessage().isCardPayment()) {

            // Lookup of payee's acquirer.  You would typically cache such information
            urlHolder.setUrl(reserveOrBasicRequest.getAcquirerAuthorityUrl());
            Authority acquirerAuthority = getAuthority(urlHolder);

            // Pure sample data...
            JSONObjectWriter protectedAccountData =
                ProtectedAccountData.encode(authorizationData.getAccountDescriptor(),
                                            "Luke Skywalker",
                                            ISODateTime.parseDateTime("2019-12-31T00:00:00Z").getTime(),
                                            "943");
            encryptedCardData = EncryptedData.encode(protectedAccountData,
                                                     acquirerAuthority.getDataEncryptionAlgorithm(),
                                                     acquirerAuthority.getPublicKey(),
                                                     acquirerAuthority.getKeyEncryptionAlgorithm());
        } else {
            // We simply take the first account in the list
            payeeAccount = transactionRequest.getPayeeAccountDescriptors()[0];
        }

        StringBuffer accountReference = new StringBuffer();
        int q = accountId.length() - 4;
        for (char c : accountId.toCharArray()) {
            accountReference.append((--q < 0) ? c : '*');
        }
        
        return TransactionResponse.encode(transactionRequest,
                                          accountReference.toString(),
                                          payeeAccount,
                                          encryptedCardData,
                                          getReferenceId(),
                                          BankService.bankKey);
    }

    JSONObjectWriter processReserveOrBasicRequest(JSONObjectReader request, UrlHolder urlHolder)
    throws IOException, GeneralSecurityException {
        // Read the by the user and merchant attested payment request
        ReserveOrBasicRequest attestedPaymentRequest = new ReserveOrBasicRequest(request);

        // The merchant is the only entity who can provide the client's IP address
        String clientIpAddress = attestedPaymentRequest.getClientIpAddress();

        // Client IP could be used for risk-based authentication
        logger.info("Client address: " + clientIpAddress);

        // Get the embedded (counter-signed) payment request
        PaymentRequest paymentRequest = attestedPaymentRequest.getPaymentRequest();

        // Verify that the merchant's signature belongs to a for us known merchant
        MerchantAccountEntry merchantAccountEntry = BankService.merchantAccountDb.get(paymentRequest.getPayee().getId());
        if (merchantAccountEntry == null) {
            throw new IOException("Unknown merchant: " + paymentRequest.getPayee().writeObject().toString());
        }
        if (!merchantAccountEntry.getPublicKey().equals(paymentRequest.getPublicKey())) {
            throw new IOException("Public key doesn't match merchant: " + paymentRequest.getPayee().writeObject().toString());
        }

        // Lookup of payer's bank.  You would typically cache such information
        urlHolder.setUrl(attestedPaymentRequest.getProviderAuthorityUrl());
        Authority providerAuthority = getAuthority(urlHolder);

        // We need to separate credit-card and account-2-account payments
        boolean acquirerBased = attestedPaymentRequest.getPayerAccountType().isAcquirerBased();
        logger.info("Kind of operation: " + (acquirerBased ? "credit-card" : "account-2-account"));

        ////////////////////////////////////////////////////////////////////////////
        // We got an authentic request.  Now we need to get an attestation by     //
        // the payer's bank that user is authentic and have the required funds... //
        ////////////////////////////////////////////////////////////////////////////

        // Should be external data but this is a demo you know...
        AccountDescriptor[] accounts = {new AccountDescriptor("http://ultragiro.fr", "35964640"),
                                        new AccountDescriptor("http://mybank.com", 
                                                              "J-399.962",
                                                              new String[]{"enterprise"})};
     
        urlHolder.setUrl(providerAuthority.getTransactionUrl());
        JSONObjectWriter transactionRequest = TransactionRequest.encode(attestedPaymentRequest,
                                                                        accounts,
                                                                        getReferenceId(),
                                                                        BankService.bankKey);
       
        // Decode response
        JSONObjectReader response = postData(urlHolder, transactionRequest);
        if (response.getString(JSONDecoderCache.QUALIFIER_JSON).equals(Messages.PROVIDER_USER_RESPONSE.toString())) {
            return new JSONObjectWriter(response);
        }

        // Success
        return ReserveOrBasicResponse.encode(new TransactionResponse(response),
                                             BankService.bankKey);
    }

    JSONObjectWriter processFinalizeCreditRequest(JSONObjectReader payeeRequest, UrlHolder urlHolder) throws IOException, GeneralSecurityException {

        // Decode the finalize credit request
        FinalizeRequest finalizeRequest = new FinalizeRequest(payeeRequest);

        // Lookup of payer's bank.  You would typically cache such information
        urlHolder.setUrl(finalizeRequest.getProviderAuthorityUrl());
        Authority providerAuthority = getAuthority(urlHolder);

        // This message is the one which finally actually lifts money
        urlHolder.setUrl(providerAuthority.getTransactionUrl());
        FinalizeTransactionResponse finalizeTransactionResponse = 
            new FinalizeTransactionResponse(postData(urlHolder,
                                                     FinalizeTransactionRequest.encode(finalizeRequest,
                                                                                       getReferenceId(),
                                                                                       BankService.bankKey)));

        // It appears that we succeeded
        return FinalizeCreditResponse.encode(finalizeTransactionResponse,
                                             getReferenceId(),
                                             BankService.bankKey);
    }

    JSONObjectWriter processFinalizeCardpayRequest(JSONObjectReader payeeRequest, UrlHolder urlHolder) throws IOException, GeneralSecurityException {

        // Decode the finalize cardpay request
        FinalizeRequest finalizeRequest = new FinalizeRequest(payeeRequest);
        
        logger.info("Card data: " + finalizeRequest.getProtectedAccountData(BankService.decryptionKeys));

        // Here we are supposed to talk to the card payment network....

        // It appears that we succeeded
        return FinalizeCardpayResponse.encode(finalizeRequest,
                                              getReferenceId(),
                                              BankService.bankKey);
    }

    JSONObjectWriter processFinalizeTransactionRequest(JSONObjectReader payeeRequest, UrlHolder urlHolder) throws IOException, GeneralSecurityException {

        // Decode the finalize transaction request
        FinalizeTransactionRequest finalizeTransactionRequest = new FinalizeTransactionRequest(payeeRequest);

        //////////////////////////////////////////////////////////////////////////////
        // Since we don't have a real bank we simply return success...              //
        //////////////////////////////////////////////////////////////////////////////
        return FinalizeTransactionResponse.encode(finalizeTransactionRequest,
                                                  getReferenceId(),
                                                  BankService.bankKey);
    }

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        UrlHolder urlHolder = null;
        try {
            urlHolder = new UrlHolder(request.getRemoteAddr(), request.getContextPath());
            String contentType = request.getContentType();
            if (!contentType.equals(JSON_CONTENT_TYPE)) {
                throw new IOException("Content-Type must be \"" + JSON_CONTENT_TYPE + "\" , found: " + contentType);
            }
            JSONObjectReader providerRequest = JSONParser.parse(ServletUtil.getData(request));
            if (BankService.logging) {
                logger.info("Call from" + urlHolder.callerAddress + "with data:\n" + providerRequest);
            }

            /////////////////////////////////////////////////////////////////////////////////////////
            // We rationalize here by using a single end-point for all requests                    //
            /////////////////////////////////////////////////////////////////////////////////////////
            Integer requestType = requestTypes.get(providerRequest.getString(JSONDecoderCache.QUALIFIER_JSON));
            if (requestType == null) {
                throw new IOException("Unexpected \"" + JSONDecoderCache.QUALIFIER_JSON + "\" :" + 
                                      providerRequest.getString(JSONDecoderCache.QUALIFIER_JSON));
            }

            JSONObjectWriter providerResponse = null; 
            switch (requestType) {
                case REQTYPE_PAYEE_INITIAL:
                    providerResponse = processReserveOrBasicRequest(providerRequest, urlHolder);
                    break;

                case REQTYPE_TRANSACTION:
                    providerResponse = processTransactionRequest(providerRequest, urlHolder);
                    break;

                case REQTYPE_PAYEE_FINALIZE_CREDIT:
                    providerResponse = processFinalizeCreditRequest(providerRequest, urlHolder);
                    break;
                    
                case REQTYPE_PAYEE_FINALIZE_CARDPAY:
                    providerResponse = processFinalizeCardpayRequest(providerRequest, urlHolder);
                    break;
 
                case REQTYPE_FINALIZE_TRANSACTION:
                    providerResponse = processFinalizeTransactionRequest(providerRequest, urlHolder);
                    break;
            }
            if (BankService.logging) {
                logger.info("Responded to caller"  + urlHolder.callerAddress + "with data:\n" + providerResponse);
            }

            /////////////////////////////////////////////////////////////////////////////////////////
            // Normal return                                                                       //
            /////////////////////////////////////////////////////////////////////////////////////////
            response.setContentType(JSON_CONTENT_TYPE);
            response.setHeader("Pragma", "No-Cache");
            response.setDateHeader("EXPIRES", 0);
            response.getOutputStream().write(providerResponse.serializeJSONObject(JSONOutputFormats.NORMALIZED));
            
        } catch (Exception e) {
            /////////////////////////////////////////////////////////////////////////////////////////
            // Hard error return. Note that we return a clear-text message in the response body.   //
            // Having specific error message syntax for hard errors only complicates things since  //
            // there will always be the dreadful "internal server error" to deal with as well as   //
            // general connectivity problems.                                                      //
            /////////////////////////////////////////////////////////////////////////////////////////
            String message = (urlHolder == null ? "" : "From" + urlHolder.callerAddress +
                              (urlHolder.getUrl() == null ? "" : "URL=" + urlHolder.getUrl()) + "\n") + e.getMessage();
            logger.log(Level.SEVERE, message, e);
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            PrintWriter writer = response.getWriter();
            writer.print(message);
            writer.flush();
        }
    }
}
