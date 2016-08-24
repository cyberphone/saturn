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
package org.webpki.saturn.svg.diagrams;

import org.webpki.tools.svg.SVGAnchor;
import org.webpki.tools.svg.SVGAttributes;
import org.webpki.tools.svg.SVGCircle;
import org.webpki.tools.svg.SVGDocument;
import org.webpki.tools.svg.SVGDoubleValue;
import org.webpki.tools.svg.SVGEllipse;
import org.webpki.tools.svg.SVGEndGlobal;
import org.webpki.tools.svg.SVGPath;
import org.webpki.tools.svg.SVGPathValues;
import org.webpki.tools.svg.SVGRect;
import org.webpki.tools.svg.SVGShaderTemplate;
import org.webpki.tools.svg.SVGText;
import org.webpki.tools.svg.SVGTransform;
import org.webpki.tools.svg.SVGValue;
import org.webpki.tools.svg.SVGText.TEXT_ANCHOR;

public class SaturnLogo extends SVGDocument {
    public SaturnLogo() {
        super(MARGIN_X, MARGIN_Y);
    }

    final static String RING_COLOR        = "#dd5454";
    final static String S_COLOR           = "#ffffff";
    final static String ATURN_COLOR       = "#808080";
    final static String CIRCLE_COLOR      = "#6e9e3f";

    final static double WIDTH             = 450;
    final static double HEIGHT            = 200;
    
    final static double MARGIN_X          = 10;
    final static double MARGIN_Y          = 10;
    final static double RING_WIDTH        = 200;
    final static double RING_HEIGHT       = 66;
    final static double END_RING_SPAN     = 20;
    final static double SATURN_WIDTH      = 120;
    final static double FONT_SIZE         = 116;
    final static double INNER_RING_SPAN   = 7;
    final static double ANGLE             = -45;
    
    double x;
    double y;
    
    @Override
    public String getFilters() {
        return "<defs>\n" +
               "<filter id=\"actorsBlur\" x=\"-25%\" y=\"-25%\" width=\"150%\" height=\"150%\">\n" +
               "<feGaussianBlur stdDeviation=\"3\"/>\n" +
               "</filter>\n" +
               "</defs>\n";
    }

    @Override
    public boolean useViewBox() {
        return true;
    }

    void addGuide() {
        add(new SVGTransform("rotate(" + ANGLE + " " + (x + RING_WIDTH / 2 + MARGIN_X) + " " + (y + RING_HEIGHT / 2 + MARGIN_Y) + ")"));
        add(new SVGEllipse(new SVGDoubleValue(x),
                           new SVGDoubleValue(y), 
                           new SVGDoubleValue(RING_WIDTH),
                           new SVGDoubleValue(RING_HEIGHT),
                           1.0,
                           "black",
                           null));
    
        add(new SVGEllipse(new SVGDoubleValue(END_RING_SPAN + x),
                           new SVGDoubleValue(INNER_RING_SPAN + y), 
                           new SVGDoubleValue(RING_WIDTH - END_RING_SPAN - END_RING_SPAN),
                           new SVGDoubleValue(RING_HEIGHT - INNER_RING_SPAN - INNER_RING_SPAN),
                           1.0,
                           "black",
                           null));
    
        add(new SVGEndGlobal());
    }

    @Override
    public void generate() {
        x = 20;
        y = 50;
/*
        add(new SVGRect(new SVGDoubleValue(0), new SVGDoubleValue(0),
                new SVGDoubleValue(WIDTH), new SVGDoubleValue(HEIGHT),
            1.0,
            "#A0A0A0",
            null));
*/

        add(new SVGTransform("rotate(" + ANGLE + " " + (x + RING_WIDTH / 2 + MARGIN_X) + " " + (y + RING_HEIGHT / 2 + MARGIN_Y) + ")"));
        add(new SVGEllipse(new SVGDoubleValue(x),
                           new SVGDoubleValue(y), 
                           new SVGDoubleValue(RING_WIDTH),
                           new SVGDoubleValue(RING_HEIGHT),
                           null,
                           null,
                           RING_COLOR));

        add(new SVGEllipse(new SVGDoubleValue(END_RING_SPAN + x),
                           new SVGDoubleValue(INNER_RING_SPAN + y), 
                           new SVGDoubleValue(RING_WIDTH - END_RING_SPAN - END_RING_SPAN),
                           new SVGDoubleValue(RING_HEIGHT - INNER_RING_SPAN - INNER_RING_SPAN),
                           null,
                           null,
                           "#ffffff"));

        add(new SVGEndGlobal());
        add(new SVGCircle(new SVGDoubleValue(RING_WIDTH / 2 - SATURN_WIDTH / 2 + x),
                new SVGDoubleValue((RING_HEIGHT /2) - SATURN_WIDTH / 2 + y), 
                new SVGDoubleValue(SATURN_WIDTH),
                null,
                null,
                CIRCLE_COLOR));
        add(new SVGText(new SVGDoubleValue(RING_WIDTH / 2 + x),
                        new SVGDoubleValue((RING_HEIGHT / 2) + FONT_SIZE / 2.8 + y),
                        "sans-serif",
                        FONT_SIZE,
                        TEXT_ANCHOR.MIDDLE,
                        "S").setFontColor(S_COLOR));

        add(new SVGText(new SVGDoubleValue(RING_WIDTH / 2 + SATURN_WIDTH * 4 / 7 + x),
                        new SVGDoubleValue((RING_HEIGHT / 2) + FONT_SIZE / 2.8 + y),
                        "sans-serif",
                        FONT_SIZE,
                        TEXT_ANCHOR.START,
                        "aturn").setFontColor(ATURN_COLOR));


//        addGuide();
        SVGPath svgPath = new SVGPath(new SVGDoubleValue(x + 71.7),
                                      new SVGDoubleValue(y + 86.4),
                                      new SVGPathValues().moveAbsolute(0, 0)
                                                         .cubicBezierRelative(0,0,0,0, 10.5, 4.5)
                                                         .cubicBezierRelative(/*15,-10*/ 0,0,46,-32, 75.4, -75.6)
                                                         .cubicBezierRelative(0,0,0,0, -4.1, -11)
                                                         .cubicBezierRelative(0,0,-36,60, -81, 80.5)
                                                         .endPath(),
/*
                                      0.5,
                                      "#0000FF",
*/
                                      null, null,
                                      RING_COLOR);
 //       svgPath.setFillOpacity(0.5);
        add(svgPath);

    }
}
