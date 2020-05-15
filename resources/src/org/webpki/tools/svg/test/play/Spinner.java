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
package org.webpki.tools.svg.test.play;

import org.webpki.tools.svg.SVGAttributes;
import org.webpki.tools.svg.SVGBeginGlobal;
import org.webpki.tools.svg.SVGDocument;
import org.webpki.tools.svg.SVGDoubleValue;
import org.webpki.tools.svg.SVGEndGlobal;
import org.webpki.tools.svg.SVGLine;
import org.webpki.tools.svg.SVGStringValue;

public class Spinner extends SVGDocument {
    public Spinner() {
        super(LINE_WIDTH /2 , LINE_WIDTH / 2);
    }

    final static double RADIUS        = 50;
    final static double LINE_WIDTH    = 10;
    final static double LINE_LENGTH   = 25;
    final static int    SEGMENTS      = 12;
    final static String CORE_COLOR    = "black";

    @Override
    public boolean useViewBox() {
        return true;
    }

    @Override
    public void generate() {
        add(new SVGBeginGlobal()
                .addString(SVGAttributes.STROKE_COLOR, new SVGStringValue("yellow"))
                .addString(SVGAttributes.STROKE_WIDTH, new SVGDoubleValue(LINE_WIDTH))
                .addString(SVGAttributes.STROKE_LINECAP, new SVGStringValue("round")));
        double opacity = 1;
        for (int i = 0; i < SEGMENTS; i++) {
            double angle = ((Math.PI * 2) / SEGMENTS) * i;
            add(new SVGLine(new SVGDoubleValue(Math.cos(angle) * (RADIUS - LINE_WIDTH / 2) + RADIUS - LINE_WIDTH / 2), 
                            new SVGDoubleValue(Math.sin(angle) * (RADIUS - LINE_WIDTH / 2) + RADIUS - LINE_WIDTH / 2), 
                            new SVGDoubleValue(Math.cos(angle) * (RADIUS - LINE_LENGTH) + RADIUS - LINE_WIDTH / 2), 
                            new SVGDoubleValue(Math.sin(angle) * (RADIUS -LINE_LENGTH) + RADIUS - LINE_WIDTH / 2), 
                            null, 
                            null).addString(SVGAttributes.STROKE_OPACITY, new SVGDoubleValue(opacity)));
 //                           null));
            opacity -= 0.7 / SEGMENTS;

        }
        add(new SVGEndGlobal());
    }
}
