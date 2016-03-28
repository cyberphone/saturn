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
package org.webpki.saturn.acquirer;

import java.io.IOException;
import java.io.PrintWriter;

import java.math.BigDecimal;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.webpki.json.JSONObjectReader;
import org.webpki.json.JSONObjectWriter;
import org.webpki.json.JSONOutputFormats;
import org.webpki.json.JSONParser;

import org.webpki.saturn.common.BaseProperties;
import org.webpki.saturn.common.ErrorReturn;
import org.webpki.saturn.common.FinalizeResponse;
import org.webpki.saturn.common.ReserveOrDebitResponse;
import org.webpki.saturn.common.FinalizeRequest;
import org.webpki.saturn.common.PaymentRequest;

import org.webpki.webutil.ServletUtil;

////////////////////////////////////////////////////////////////////////////
// This is the core Acquirer (Card-Processor) payment transaction servlet //
////////////////////////////////////////////////////////////////////////////

public class TransactionServlet extends HttpServlet implements BaseProperties {

    private static final long serialVersionUID = 1L;
    
    static Logger logger = Logger.getLogger(TransactionServlet.class.getCanonicalName());
    
    static int referenceId = 194006;

    static String getReferenceId() {
        return "#" + (referenceId++);
    }

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        JSONObjectWriter acquirerResponse = null;
         try {
            String contentType = request.getContentType();
            if (!contentType.equals(JSON_CONTENT_TYPE)) {
                throw new IOException("Content-Type must be \"" + JSON_CONTENT_TYPE + "\" , found: " + contentType);
            }
            JSONObjectReader payeeRequest = JSONParser.parse(ServletUtil.getData(request));
            logger.info("Received:\n" + payeeRequest);

            // Decode the finalize request message
            FinalizeRequest payeeFinalizationRequest = new FinalizeRequest(payeeRequest);

            // Get the embedded authorization from the payer's payment provider (bank)
            ReserveOrDebitResponse embeddedResponse = payeeFinalizationRequest.getEmbeddedResponse();

            // Verify that the provider's signature belongs to a valid payment provider trust network
            embeddedResponse.getSignatureDecoder().verify(AcquirerService.paymentRoot);

            // Get the the account data we sent encrypted through the merchant 
            logger.info("Protected Account Data:\n" +
                embeddedResponse.getProtectedAccountData(AcquirerService.decryptionKeys));

            // The original request contains some required data like currency
            PaymentRequest paymentRequest = embeddedResponse.getPaymentRequest();

            // Verify that the merchant is one of our customers.  Simplistic "database": a single customer
//TODO
/*
            paymentRequest.getSignatureDecoder().verify(AcquirerService.merchantRoot);
            String merchantDn = paymentRequest.getSignatureDecoder().getCertificatePath()[0].getSubjectX500Principal().getName();
            if (!merchantDn.equals(AcquirerService.merchantDN)) {
                throw new IOException ("Unknown merchant: " + merchantDn);
            }
*/

            ////////////////////////////////////////////////////////////////////////////
            // We got an authentic request.  Now we need to check available funds etc.//
            // Since we don't have a real acquirer this part is rather simplistic :-) //
            ////////////////////////////////////////////////////////////////////////////

            // Sorry but you don't appear to have a million bucks :-)
            if (paymentRequest.getAmount().compareTo(new BigDecimal("1000000.00")) >= 0) {
                acquirerResponse = FinalizeResponse.encode(new ErrorReturn(ErrorReturn.ERRORS.INSUFFICIENT_FUNDS));
            } else {
                acquirerResponse = FinalizeResponse.encode(payeeFinalizationRequest,
                                                           getReferenceId(),
                                                           AcquirerService.acquirerKey);
            }
            logger.info("Returned to caller:\n" + acquirerResponse);

            /////////////////////////////////////////////////////////////////////////////////////////
            // Normal return                                                                       //
            /////////////////////////////////////////////////////////////////////////////////////////
            response.setContentType(JSON_CONTENT_TYPE);
            response.setHeader("Pragma", "No-Cache");
            response.setDateHeader("EXPIRES", 0);
            response.getOutputStream().write(acquirerResponse.serializeJSONObject(JSONOutputFormats.NORMALIZED));

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
