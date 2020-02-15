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

package org.webpki.saturn.resources;

import org.webpki.util.ArrayUtil;

/**
 * Convert XML, HTML or SVG resources into strings
 *
 */
public class Escaping {
    
    static boolean java = true;
    static boolean minify80 = false;
    static StringBuilder result = new StringBuilder();
    
    static void bad() {
        System.out.println("inputfile outputfile [-java|-js] {-minify80}");
        System.exit(3);
    }
    
    public static void main(String[] args) throws Exception {
        if (args.length < 3 || args.length > 4) {
            bad();
        }
        if (!args[2].equals("-java")) {
            if (args[2].equals("-js")) {
                java = false;
            } else {
                bad();
            }
        }
        if (args.length == 4) {
            if (args[3].equals("-minify80")) {
                minify80 = true;
            } else {
                bad();
            }
        }
        boolean quoting = false;
        int lineLength = 0;
        for (char c : new String(ArrayUtil.readFile(args[0]), "utf-8").toCharArray()) {
            if (c == '"') {
                if (java) {
                    lineLength++;
                    result.append('\\');
                }
                quoting = !quoting;
            } else if (c == '\'' && !java) {
                lineLength += 2;
                result.append("\\'");
                continue;
            }
            if (minify80) {
                if (c == '\r' || c == '\n') {
                    continue;
                }
                if (c == ' ' || c == '\t') {
                    c = ' ';
                    if (result.length() > 0) {
                        char previous = result.charAt(result.length() - 1);
                        if (previous  == ' ' || previous  == '>') {
                            continue;
                        }
                    }
                }
                result.append(c);
                if (++lineLength >= 80) {
                    if (java) {
                        
                    } else {
                        result.append("\\\n");
                    }
                    lineLength = 0;
                }
            } else {
                result.append(c);
            }
        }
        ArrayUtil.writeFile(args[1], result.toString().getBytes("utf-8"));
    }
}
