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
package org.webpki.saturn.keyprovider;

import java.io.IOException;

import java.util.logging.Logger;

import javax.servlet.ServletException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.webpki.saturn.common.NonDirectPayments;

public class WalletUiTestServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    
    static Logger logger = Logger.getLogger(WalletUiTestServlet.class.getName ());
    
    static final String W3C_MODE = "w3c";     

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        if (request.getParameter(W3C_MODE) == null) {
            guiMode(request, response);
        } else {
            w3cMode(request, response);
        }
    }

    static void printHtml(HttpServletResponse response, 
                          String javascript, 
                          String bodyscript, 
                          String box) throws IOException, ServletException {
        StringBuilder s = new StringBuilder(
            "<!DOCTYPE html>"+
            "<html><head>" +
            "<meta charset=\"utf-8\">" +
            "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">" +
            "<link rel=\"icon\" href=\"saturn.png\" sizes=\"192x192\">"+
            "<link rel=\"stylesheet\" type=\"text/css\" href=\"style.css\">" +
            "<title>Wallet UI Testing</title>");

        if (javascript != null) {
            s.append("<script type=\"text/javascript\">").append(javascript).append("</script>");
        }
        s.append("</head><body");
        if (bodyscript != null) {
            if (bodyscript.charAt(0) != '>') {
                s.append(' ');
            }
            s.append(bodyscript);
        }
        s.append('>')
         .append(box)
         .append("</table></body></html>");
        KeyProviderInitServlet.output(response, s.toString());
    }

    private void w3cMode(HttpServletRequest request, HttpServletResponse response)  throws IOException, ServletException {
        // TODO Auto-generated method stub
        
    }

    private void guiMode(HttpServletRequest request, HttpServletResponse response)  throws IOException, ServletException {
        // TODO Auto-generated method stub
        
    }
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
    }
}
