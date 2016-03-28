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
package org.webpki.w2nbproxy;

import java.io.IOException;

import org.webpki.json.JSONObjectReader;
import org.webpki.json.JSONParser;

import org.webpki.util.Base64URL;

public class ExtensionPositioning {
    
    public class TargetRectangle {
        
        public double left;
        public double top;
        public double width;
        public double height;

        TargetRectangle(double left, double top, double width, double height) {
            this.left = left;
            this.top = top;
            this.width = width;
            this.height = height;
        }
    }
    
    JSONObjectReader positioningArguments;
    
    public static enum HORIZONTAL_ALIGNMENT {Left, Right, Center};
    public static enum VERTICAL_ALIGNMENT   {Top, Bottom, Center};
    
    public static final String HORIZONTAL_ALIGNMENT_JSON = "horizontalAlignment";
    public static final String VERTICAL_ALIGNMENT_JSON   = "verticalAlignment";
    
    public static final String TARGET_RECTANGLE_JSON     = "targetRectangle";
    
    public static final String TARGET_LEFT_JSON          = "left";
    public static final String TARGET_TOP_JSON           = "top";
    public static final String TARGET_WIDTH_JSON         = "width";
    public static final String TARGET_HEIGHT_JSON        = "height";
    
    public HORIZONTAL_ALIGNMENT horizontalAlignment;
    public VERTICAL_ALIGNMENT verticalAlignment;
    
    public TargetRectangle targetRectangle;  // Optional (may be null)

    public ExtensionPositioning(String base64UrlEncodedArguments) throws IOException {
        positioningArguments = JSONParser.parse(Base64URL.decode(base64UrlEncodedArguments));
        if (positioningArguments.hasProperty(HORIZONTAL_ALIGNMENT_JSON)) {
            horizontalAlignment = HORIZONTAL_ALIGNMENT.valueOf(
                    positioningArguments.getString(HORIZONTAL_ALIGNMENT_JSON));
            verticalAlignment = VERTICAL_ALIGNMENT.valueOf(
                    positioningArguments.getString(VERTICAL_ALIGNMENT_JSON));
            if (positioningArguments.hasProperty(TARGET_RECTANGLE_JSON)) {
                JSONObjectReader values = positioningArguments.getObject(TARGET_RECTANGLE_JSON);
                targetRectangle = new TargetRectangle(values.getDouble(TARGET_LEFT_JSON),
                                                      values.getDouble(TARGET_TOP_JSON),
                                                      values.getDouble(TARGET_WIDTH_JSON),
                                                      values.getDouble(TARGET_HEIGHT_JSON));
            }
        }
        positioningArguments.checkForUnread();
    }
    
    public static String encode(HORIZONTAL_ALIGNMENT horizontalAlignment,
                                VERTICAL_ALIGNMENT verticalAlignment,
                                String optionalTargetElementId) {
        return "setExtensionPosition(\"" + horizontalAlignment.toString() +
                                  "\", \"" + verticalAlignment.toString() + "\"" +
                        (optionalTargetElementId == null ? "" : ", \"" + optionalTargetElementId + "\"") + ")";
    }
    
    public static final String SET_EXTENSION_POSITION_FUNCTION_TEXT =
            "function setExtensionPosition(hAlign, vAlign, optionalId) {\n" +
            "  var result = {" + HORIZONTAL_ALIGNMENT_JSON + ":hAlign, " +
                                   VERTICAL_ALIGNMENT_JSON + ":vAlign}\n" +
            "  if (optionalId) {\n" +
            "    var input = document.getElementById(optionalId).getBoundingClientRect();\n" +
            "    var rectangle = {};\n" +
            "    rectangle." + TARGET_LEFT_JSON + " = input." + TARGET_LEFT_JSON + ";\n" +
            "    rectangle." + TARGET_TOP_JSON + " = input." + TARGET_TOP_JSON + ";\n" +
            "    rectangle." + TARGET_WIDTH_JSON + " = input." + TARGET_WIDTH_JSON + ";\n" +
            "    rectangle." + TARGET_HEIGHT_JSON + " = input." + TARGET_HEIGHT_JSON + ";\n" +
            "    result." + TARGET_RECTANGLE_JSON + " = rectangle;\n" +
            "  }\n" +
            "  return result;\n" +
            "}\n";

    @Override
    public String toString() {
        return positioningArguments.toString();
    }
}
