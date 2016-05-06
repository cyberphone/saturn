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

import java.util.Iterator;
import java.util.LinkedHashMap;

import java.util.logging.Logger;

public class QRSessions {

    static class SessionInProgress {
        long expiry_time;
        String id;
        Synchronizer synchronizer;
    }

    private static final Logger logger = Logger.getLogger (QRSessions.class.getCanonicalName());

    private static LinkedHashMap<String,SessionInProgress> currentSessions = new LinkedHashMap<String,SessionInProgress> ();

    private static Looper looper;

    private static int sessionId;

    static final long CYCLE_TIME  = 60000L;
    static final long COMET_WAIT  = 30000L;
    static final long MAX_SESSION = 300000L;

    private static class Looper extends Thread {
        public void run() {
            while (true) {
                try {
                    sleep(CYCLE_TIME);
                    synchronized (QRSessions.class) {
                        if (currentSessions.isEmpty()) {
                            logger.info("Timeout thread died");
                            looper = null;
                            break;
                        }
                        long current_time = System.currentTimeMillis();
                        Iterator<SessionInProgress> list = currentSessions
                                .values().iterator();
                        while (list.hasNext()) {
                            SessionInProgress sessionInProgress = list.next();
                            if (current_time > sessionInProgress.expiry_time) {
                                logger.info("Removed due to timeout, QR Session ID=" + sessionInProgress.id);
                                synchronized (sessionInProgress.synchronizer) {
                                    sessionInProgress.synchronizer.notify();
                                }
                                list.remove();
                            }
                        }
                    }
                } catch (InterruptedException e) {
                    break;
                }
            }
        }
    }

    static synchronized String createSession() throws IOException {
        SessionInProgress sessionInProgress = new SessionInProgress();
        sessionInProgress.id = String.valueOf(++sessionId);
        sessionInProgress.expiry_time = System.currentTimeMillis() + MAX_SESSION;
        sessionInProgress.synchronizer = new Synchronizer();
        currentSessions.put(sessionInProgress.id, sessionInProgress);
        logger.info("Created QR Session ID=" + sessionInProgress.id);
        if (looper == null) {
            logger.info("Timeout thread started");
            (looper = new Looper()).start();
        }
        return sessionInProgress.id;
    }

    static synchronized void removeSession(String id) {
        currentSessions.remove(id);
    }

    static Synchronizer getSynchronizer(String id) {
        SessionInProgress auth = currentSessions.get(id);
        if (auth == null) {
            return null;
        }
        return auth.synchronizer;
    }
}
