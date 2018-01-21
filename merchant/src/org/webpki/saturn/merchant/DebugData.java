/*
 *  Copyright 2015-2018 WebPKI.org (http://webpki.org).
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
package org.webpki.saturn.merchant;

import java.io.Serializable;

import org.webpki.json.JSONObjectReader;

public class DebugData implements Serializable {

    private static final long serialVersionUID = 1L;
    
    public JSONObjectReader InvokeWallet;
    
    public JSONObjectReader walletResponse;

    public JSONObjectReader providerAuthority;

    public boolean basicCredit;
    
    public boolean softAuthorizationError;
    
    public JSONObjectReader payeeAuthority;
    
    public JSONObjectReader payeeProviderAuthority;

    public JSONObjectReader authorizationRequest;
    
    public JSONObjectReader authorizationResponse;

    public JSONObjectReader transactionRequest;
    
    public JSONObjectReader transactionResponse;

    // Native mode
    public JSONObjectReader reserveOrBasicRequest;
    
    public JSONObjectReader reserveOrBasicResponse;
    
    public boolean acquirerMode;
    
    public boolean hybridMode;
    
    public JSONObjectReader acquirerAuthority;
    
    public JSONObjectReader finalizeRequest;

    public JSONObjectReader finalizeResponse;

    public boolean softFinalizeError;

    public boolean gasStation;

    public JSONObjectReader refundRequest;

    public JSONObjectReader refundResponse;
}
