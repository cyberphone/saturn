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
package org.webpki.saturn.svg.diagrams;

import org.webpki.tools.svg.SVGDocument;
import org.webpki.tools.svg.SVGDoubleValue;
import org.webpki.tools.svg.SVGEmbeddedText;
import org.webpki.tools.svg.SVGHorizontalLine;
import org.webpki.tools.svg.SVGPath;

public class WebPKI extends SVGDocument {
    public WebPKI() {
        super(10, 10);
    }

    @Override
    public void generate() {
        String webPKIString = "WebPKI";

        SVGEmbeddedText et = new SVGEmbeddedText(org.webpki.saturn.svg.diagrams.WebPKIGlyphs.class);
        
        double x = 10;
        double y = 42;
        double transformation = 40;
        double gutter = -3;
        String color = "rgb(0,102,255)";
        
        for (char c : webPKIString.toCharArray()) {
            
            SVGEmbeddedText.DecodedGlyph dg = et.getDecodedGlyph(c, transformation);

            add(new SVGPath(new SVGDoubleValue(x), new SVGDoubleValue(y),
                    dg.getSVGPathValues(),
                    null,
                    null,
                    color));
            if (c == 'b') {
                color = "rgb(197,0,11)";
            }
            x += dg.getXAdvance() + gutter;
            gutter = 4;
        }
        
        add(new SVGHorizontalLine(new SVGDoubleValue(0), new SVGDoubleValue(y + 12),
                new SVGDoubleValue(195),
                1.2,
                "#000000"));

        et = new SVGEmbeddedText(org.webpki.saturn.svg.diagrams.ORGGlyphs.class);
        String orgString = ".ORG";
        x = 10;
        y = 76;
        transformation = 120;
        gutter = 8;
         
        for (char c : orgString.toCharArray()) {
            
            SVGEmbeddedText.DecodedGlyph dg = et.getDecodedGlyph(c, transformation);

            add(new SVGPath(new SVGDoubleValue(x), new SVGDoubleValue(y),
                    dg.getSVGPathValues(),
                    null,
                    null,
                    "#000000"));
            x += dg.getXAdvance() + gutter;
            if (c == '.') {
                transformation = 100;
                gutter = 58;
            }
        }
    }
 }
