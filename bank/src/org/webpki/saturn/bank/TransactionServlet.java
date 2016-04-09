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

import org.webpki.saturn.common.CertificatePathCompare;
import org.webpki.saturn.common.PayerAccountTypes;
import org.webpki.saturn.common.Authority;
import org.webpki.saturn.common.BaseProperties;
import org.webpki.saturn.common.EncryptedData;
import org.webpki.saturn.common.ErrorReturn;
import org.webpki.saturn.common.FinalizeRequest;
import org.webpki.saturn.common.FinalizeResponse;
import org.webpki.saturn.common.AccountDescriptor;
import org.webpki.saturn.common.ReserveOrBasicRequest;
import org.webpki.saturn.common.AuthorizationData;
import org.webpki.saturn.common.ReserveOrBasicResponse;
import org.webpki.saturn.common.Messages;
import org.webpki.saturn.common.PaymentRequest;
import org.webpki.saturn.common.ProtectedAccountData;

import org.webpki.webutil.ServletUtil;

//////////////////////////////////////////////////////////////////////////
// This is the core Payment Provider (Bank) payment transaction servlet //
//////////////////////////////////////////////////////////////////////////

public class TransactionServlet extends HttpServlet implements BaseProperties {

    private static final long serialVersionUID = 1L;
    
    static Logger logger = Logger.getLogger(TransactionServlet.class.getCanonicalName());
    
    static final int TIMEOUT_FOR_REQUEST = 5000;

    static int referenceId = 164006;

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

    static String getReferenceId() {
        return "#" + (referenceId++);
    }

    JSONObjectWriter processReserveOrBasicRequest(JSONObjectReader payeeRequest)
    throws IOException, GeneralSecurityException {
        // Read the by the user and merchant attested payment request
       ReserveOrBasicRequest attestedPaymentRequest = new ReserveOrBasicRequest(payeeRequest);

       // Decrypt the encrypted user authorization
       AuthorizationData authorizationData =
               attestedPaymentRequest.getDecryptedAuthorizationData(BankService.decryptionKeys);

       // The merchant is the only entity who can provide the client's IP address
       String clientIpAddress = attestedPaymentRequest.getClientIpAddress();

       // Client IP could be used for risk-based authentication, here it is only logged
       logger.info("Client address: " + clientIpAddress);

       // Verify that the there is an account matching the received public key
       String accountId = authorizationData.getAccountDescriptor().getAccountId();
       String accountType = authorizationData.getAccountDescriptor().getAccountType();
       UserAccountEntry account = BankService.userAccountDb.get(accountId);
       if (account == null) {
           logger.info("No such account ID: " + accountId);
           throw new IOException("No such user account ID");
       }
       if (!account.getType().equals(accountType)) {
           logger.info("Wrong account type: " + accountType + " for account ID: " + accountId);
           throw new IOException("Wrong user account type");
       }
       if (!account.getPublicKey().equals(authorizationData.getPublicKey())) {
           logger.info("Wrong public key for account ID: " + accountId);
           throw new IOException("Wrong user public key");
       }

       // Get the embedded (counter-signed) payment request
       PaymentRequest paymentRequest = attestedPaymentRequest.getPaymentRequest();

       // Verify that the merchant's signature belongs to a for us known merchant
       // To simply things we only recognize a single merchant...
       if (!paymentRequest.getPublicKey().equals(BankService.merchantKey)) {
           throw new IOException("Unknown merchant");
       }

       // We need to separate credit-card and account-2-account payments
       boolean acquirerBased = PayerAccountTypes.fromTypeUri(authorizationData.getAccountDescriptor().getAccountType()).isAcquirerBased();
       logger.info("Kind of operation: " + (acquirerBased ? "credit-card" : "account-2-account"));

       ////////////////////////////////////////////////////////////////////////////
       // We got an authentic request.  Now we need to check available funds etc.//
       // Since we don't have a real bank this part is rather simplistic :-)     //
       ////////////////////////////////////////////////////////////////////////////

       // Sorry but you don't appear to have a million bucks :-)
       if (!acquirerBased && paymentRequest.getAmount().compareTo(new BigDecimal("1000000.00")) >= 0) {
           return ReserveOrBasicResponse.encode(attestedPaymentRequest.getMessage().isBasicCredit(),
                                                new ErrorReturn(ErrorReturn.ERRORS.INSUFFICIENT_FUNDS));
       }

       // Separate credit-card and account2account payments
       AccountDescriptor payeeAccount = null;
       JSONObjectWriter encryptedCardData = null;
       if (acquirerBased) {
           String authorityUrl = attestedPaymentRequest.getAcquirerAuthorityUrl();
           HTTPSWrapper wrap = new HTTPSWrapper();
           wrap.setTimeout(TIMEOUT_FOR_REQUEST);
           wrap.setRequireSuccess(true);
           wrap.makeGetRequest(portFilter(authorityUrl));
           if (!wrap.getContentType().equals(JSON_CONTENT_TYPE)) {
               throw new IOException("Content-Type must be \"" + JSON_CONTENT_TYPE + "\" , found: " + wrap.getContentType());
           }
           Authority authority = new Authority(JSONParser.parse(wrap.getData()), authorityUrl);

           // Pure sample data...
           JSONObjectWriter protectedAccountData =
                ProtectedAccountData.encode(authorizationData.getAccountDescriptor(),
                                            "Luke Skywalker",
                                            ISODateTime.parseDateTime("2019-12-31T00:00:00Z").getTime(),
                                            "943");
           encryptedCardData = EncryptedData.encode(protectedAccountData,
                                                    authority.getDataEncryptionAlgorithm(),
                                                    authority.getPublicKey(),
                                                    authority.getKeyEncryptionAlgorithm());
        } else {
            // We simply take the first account in the list
            payeeAccount = attestedPaymentRequest.getPayeeAccountDescriptors()[0];
        }

       return ReserveOrBasicResponse.encode(attestedPaymentRequest,
                                            paymentRequest,
                                            authorizationData.getAccountDescriptor(),
                                            encryptedCardData,
                                            payeeAccount,
                                            getReferenceId(),
                                            BankService.bankKey);
    }

    JSONObjectWriter processFinalizeRequest(JSONObjectReader payeeRequest) throws IOException, GeneralSecurityException {

        // Decode the finalize request message which the one which lifts money
        FinalizeRequest payeeFinalizationRequest = new FinalizeRequest(payeeRequest);

        // Get the embedded authorization presumably made by ourselves :-)
        ReserveOrBasicResponse embeddedResponse = payeeFinalizationRequest.getEmbeddedResponse();

        // Verify that the provider's signature really belongs to us
        CertificatePathCompare.compareCertificatePaths(embeddedResponse.getSignatureDecoder().getCertificatePath(),
                                                       BankService.bankCertificatePath);

        //////////////////////////////////////////////////////////////////////////////
        // Since we don't have a real bank we simply return success...              //
        //////////////////////////////////////////////////////////////////////////////
        return FinalizeResponse.encode(payeeFinalizationRequest,
                                       getReferenceId(),
                                       BankService.bankKey);
    }
        
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        try {
            String contentType = request.getContentType();
            if (!contentType.equals(JSON_CONTENT_TYPE)) {
                throw new IOException("Content-Type must be \"" + JSON_CONTENT_TYPE + "\" , found: " + contentType);
            }
            JSONObjectReader payeeRequest = JSONParser.parse(ServletUtil.getData(request));
            logger.info("Received:\n" + payeeRequest);

            /////////////////////////////////////////////////////////////////////////////////////////
            // We rationalize here by using a single end-point for both reserve/debit and finalize //
            /////////////////////////////////////////////////////////////////////////////////////////
            JSONObjectWriter providerResponse = 
                payeeRequest.getString(JSONDecoderCache.QUALIFIER_JSON).equals(Messages.FINALIZE_CREDIT_REQUEST.toString()) ?
                    processFinalizeRequest(payeeRequest) : processReserveOrBasicRequest(payeeRequest);
            logger.info("Returned to caller:\n" + providerResponse);

            /////////////////////////////////////////////////////////////////////////////////////////
            // Normal return                                                                       //
            /////////////////////////////////////////////////////////////////////////////////////////
            response.setContentType(BankService.jsonMediaType);
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
            logger.log(Level.SEVERE, e.getMessage(), e);
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            PrintWriter writer = response.getWriter();
            writer.print(e.getMessage());
            writer.flush();
        }
    }
}
