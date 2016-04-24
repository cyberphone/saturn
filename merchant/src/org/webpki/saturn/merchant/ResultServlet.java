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
package org.webpki.saturn.merchant;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.webpki.json.JSONArrayWriter;
import org.webpki.json.JSONDecoderCache;
import org.webpki.json.JSONObjectReader;
import org.webpki.json.JSONObjectWriter;
import org.webpki.json.JSONOutputFormats;
import org.webpki.json.JSONParser;
import org.webpki.net.HTTPSWrapper;
import org.webpki.util.ArrayUtil;
import org.webpki.webutil.ServletUtil;
import org.webpki.saturn.common.FinalizeCardpayResponse;
import org.webpki.saturn.common.Messages;
import org.webpki.saturn.common.Authority;
import org.webpki.saturn.common.BaseProperties;
import org.webpki.saturn.common.AuthorizationData;
import org.webpki.saturn.common.Expires;
import org.webpki.saturn.common.FinalizeCreditResponse;
import org.webpki.saturn.common.PaymentRequest;
import org.webpki.saturn.common.ReserveOrBasicResponse;
import org.webpki.saturn.common.ReserveOrBasicRequest;
import org.webpki.saturn.common.FinalizeRequest;
import org.webpki.saturn.common.PayerAuthorization;
import org.webpki.saturn.common.UserMessageResponse;

//////////////////////////////////////////////////////////////////////////
// This servlet does all Merchant backend payment transaction work      //
//////////////////////////////////////////////////////////////////////////

public class ResultServlet extends HttpServlet implements BaseProperties, MerchantProperties {

    private static final long serialVersionUID = 1L;
    
    static Logger logger = Logger.getLogger(ResultServlet.class.getCanonicalName());
    
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        HttpSession session = request.getSession(false);
        if (session == null) {
            ErrorServlet.sessionTimeout(response);
            return;
         }
        ResultData resultData = (ResultData) session.getAttribute(RESULT_DATA_SESSION_ATTR);
        if (resultData == null) {
            ErrorServlet.systemFail(response, "Missing result data");
            return;
        }
        HTML.resultPage(response,
                        UserPaymentServlet.getOption(session, DEBUG_MODE_SESSION_ATTR),
                        resultData);
    }
}
