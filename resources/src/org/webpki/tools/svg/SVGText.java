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
package org.webpki.tools.svg;

public class SVGText extends SVGObject {

    public enum FONT_WEIGHTS {
        NORMAL ("normal"),
        BOLD   ("bold"),
        W500   ("500");
        
        String value;
        
        FONT_WEIGHTS(String value) {
            this.value = value;
        }
    }
    
    class SVGTspan extends SVGObject {

        String subString;

        @Override
        String getTag() {
            return "tspan";
        }

        @Override
        double getMaxX() {
             return getAttribute(SVGAttributes.X) == null ? 0 : updateMargin(SVGDocument.marginX,SVGAttributes.X);
        }

        @Override
        double getMaxY() {
            return getAttribute(SVGAttributes.Y) == null ? 0 : updateMargin(SVGDocument.marginY,SVGAttributes.Y);
        }

        @Override
        boolean hasBody() {
             return true;
        }
        
        @Override
        String getBody() {
            return subString;
        }
    }
    
    public enum TEXT_ANCHOR {START, MIDDLE, END};
    
    String text;
    
    String multiLineSpacing = "1.2em";
    
    public SVGText(SVGValue x,
                   SVGValue y,
                   String fontFamily,
                   double fontSize,
                   TEXT_ANCHOR optionalTextAnchor,
                   String text) {
        addDouble(SVGAttributes.X, x);
        addDouble(SVGAttributes.Y, y);
        addString(SVGAttributes.FONT_FAMILY, new SVGStringValue(fontFamily));
        addDouble(SVGAttributes.FONT_SIZE, new SVGDoubleValue(fontSize));
        if (optionalTextAnchor != null) {
            addString(SVGAttributes.TEXT_ANCHOR, new SVGStringValue(optionalTextAnchor.toString().toLowerCase()));
        }
        if (text.indexOf('\n') > 0) {
            boolean next = false;
            while (text.length() > 0) {
                String subString = text;
                int i = text.indexOf('\n');
                if (i>= 0) {
                    subString = text.substring(0, i);
                    text = text.substring(i + 1);
                } else {
                    text = "";
                }
                SVGTspan tspan = new SVGTspan();
                tspan.subString = subString.length() == 0 ? "&#160;" : subString;
                afterDependencyElements.add(tspan);
                if (next) {
                    tspan.addDouble(SVGAttributes.X, x);
                    tspan.addString(SVGAttributes.DY, new SVGStringValue(multiLineSpacing));
                }
                next = true;
            }
        } else {
            this.text = text;
        }
    }
    
    public SVGText setFontColor(String fontColor) {
        addString(SVGAttributes.FILL_COLOR, new SVGStringValue(fontColor));
        return this;
    }

    public SVGText setLetterSpacing(double letterSpacing) {
        addDouble(SVGAttributes.LETTER_SPACING, new SVGDoubleValue(letterSpacing));
        return this;
    }

    public SVGText setFontWeight(FONT_WEIGHTS fontWeight) {
        addString(SVGAttributes.FONT_WEIGHT, new SVGStringValue(fontWeight.value));
        return this;
    }
    public SVGText setMultiLineSpacing(String multiLineSpacing) {
        this.multiLineSpacing = multiLineSpacing;
        return this;
    }

    @Override
    String getTag() {
        return "text";
    }

    @Override
    boolean hasBody() {
        return true;
    }
    
    @Override
    String getBody() {
        return text;
    }
 
    
    public SVGText setDy(String dy) {
        addString(SVGAttributes.DY, new SVGStringValue(dy));
        return this;
    }

    @Override
    double getMaxX() {
         return updateMargin(SVGDocument.marginX,SVGAttributes.X);
    }

    @Override
    double getMaxY() {
        return updateMargin(SVGDocument.marginY,SVGAttributes.Y);
    }

    public SVGText setLink(String url, String toolTip, Boolean replace) {
        _setLink(url, toolTip, replace);
        return this;
    }

    public SVGText endLink() {
        _endLink();
        return this;
    }
}
