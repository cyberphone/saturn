/*
 *  Copyright 2015-2020 WebPKI.org (http://webpki.org).
 *
 *  Licensed under the Apache License, Version 2.0 (the \"License\");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an \"AS IS\" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.webpki.saturn.svg.diagrams;

import org.webpki.tools.svg.SVGDocument;
import org.webpki.tools.svg.SVGPathValues;
import org.webpki.tools.svg.SVGSubDocument;
import org.webpki.tools.svg.SVGDoubleValue;
import org.webpki.tools.svg.SVGPath;
import org.webpki.tools.svg.SVGRect;

public class Bank extends SVGDocument {

    public Bank() {
        super(5, 5);
    }

    public static final int BANK_HEIGHT = 70;
    public static final int BANK_WIDTH = 100;
    
    static final int ROOF_WIDTH = 90;
    
    static final int PILLAR_WIDTH_3 = 13;
    
    static final int PILLAR_WIDTH_4 = 11;

    static final int STAIRS_HEIGHT = 6;
    
    static final int OUTER_PILLAR_GAP = 7;
    
    static final int ROOF_HEIGHT = 15;
    
    static final int TOP_BAR_HEIGHT = 10;
    
    static final double STROKE_WIDTH = 1;
    
    static final String FILL_COLOR = "#f2f2f2";
    
    static final String ROOF_COLOR = "#e0e0e0";

    static final String STROKE_COLOR = "#404040";
    
    static final String SHADDOW_COLOR = "#a0a0a0";
    
    static final String SHADDOW_FILTER = "url(#bankShaddow)";

    static String getBankPillarDefs() { 
        return size < 0.5 ?
            "<linearGradient x2=\"1\" x1=\"0\" id=\"bankPillar\">\n" +
            "<stop stop-color=\"#707070\" offset=\"0\"/>\n" +
            "<stop stop-color=\"#ffffff\" offset=\"0.5\"/>\n" +
            "<stop stop-color=\"#707070\" offset=\"1\"/>\n" +
            "</linearGradient>\n"
                :
            "<linearGradient x2=\"1\" x1=\"0\" id=\"bankPillar\">\n" +
            "<stop stop-color=\"#606060\" offset=\"0\"/>\n" +
            "<stop stop-color=\"#ffffff\" offset=\"0.4\"/>\n" +
            "<stop stop-color=\"#ffffff\" offset=\"0.6\"/>\n" +
            "<stop stop-color=\"#606060\" offset=\"1\"/>\n" +
            "</linearGradient>\n";
    }
    
    public static String getBankDefs() {
        return
            getBankPillarDefs() +
            "<filter width=\"200%\" height=\"200%\" x=\"-50%\" y=\"-50%\" id=\"bankShaddow\">\n" +
            "<feGaussianBlur stdDeviation=\"" +
            (2 * size) +
            "\"/>\n" +
            "</filter>\n";
    }
    
    static final double X_SHADDOW = 3.5;
    
    static final double Y_SHADDOW = 3.5;
    
    static double size;

    @Override
    public String getFilters() {
        return 
        "<defs>\n" +
         getBankDefs() +
        "</defs>\n";

    }

    public static class SubBank extends SVGSubDocument {
    
        double x, y;
        boolean fourPillars;
        double strokeWeight;
        String strokeColor = STROKE_COLOR;
        int numberOfPillars;
        double pillarWidth;
        double roofLR;
        double roofLow;
        double pillarX;
        double pillarXincrement;
        double pillarHeight;
        double inset;
        boolean shaddow;
   
        public SubBank(double x, double y, double size, boolean fourPillars) {
            this.x = x;
            this.y = y;
            Bank.size = size;
            this.fourPillars = fourPillars;
            this.strokeWeight = STROKE_WIDTH * size;
        }
    
        public SubBank setStrokeWeight(double strokeWeight) {
            this.strokeWeight = strokeWeight;
            return this;
        }

        public SubBank setStrokeColor(String strokeColor) {
            this.strokeColor = strokeColor;
            return this;
        }
        
        public SubBank setShaddow(boolean shaddow) {
            this.shaddow = shaddow;
            return this;
        }

        void createMainBackground() {
            SVGPathValues background = new SVGPathValues()
                .moveAbsolute(-roofLR - strokeWeight / 2, roofLow - strokeWeight / 2)
                .lineToAbsolute(0, -BANK_HEIGHT * size * 0.5 - strokeWeight / 2)
                .lineToAbsolute(roofLR + strokeWeight / 2, roofLow - strokeWeight / 2)
                .lineToAbsolute(roofLR + strokeWeight / 2, roofLow + TOP_BAR_HEIGHT * size + strokeWeight)
                .lineToAbsolute(roofLR - OUTER_PILLAR_GAP * size, roofLow + TOP_BAR_HEIGHT * size + strokeWeight)
                .lineToAbsolute(roofLR - OUTER_PILLAR_GAP * size, roofLow + TOP_BAR_HEIGHT * size + strokeWeight + pillarHeight)
                .lineToAbsolute((BANK_WIDTH / 2) * size - inset + strokeWeight / 2, roofLow + TOP_BAR_HEIGHT * size + strokeWeight + pillarHeight)
                .lineToAbsolute((BANK_WIDTH / 2) * size - inset + strokeWeight / 2, (BANK_HEIGHT / 2 - STAIRS_HEIGHT) * size - strokeWeight / 2)
                .lineToAbsolute((BANK_WIDTH / 2) * size + strokeWeight / 2, (BANK_HEIGHT / 2 - STAIRS_HEIGHT) * size - strokeWeight / 2)
                .lineToAbsolute((BANK_WIDTH / 2) * size + strokeWeight / 2, (BANK_HEIGHT / 2) * size + strokeWeight / 2)
                .lineToAbsolute(-(BANK_WIDTH / 2) * size - strokeWeight / 2, (BANK_HEIGHT / 2) * size + strokeWeight / 2)
                .lineToAbsolute(-(BANK_WIDTH / 2) * size - strokeWeight / 2,  (BANK_HEIGHT / 2 - STAIRS_HEIGHT) * size - strokeWeight / 2)
                .lineToAbsolute(-(BANK_WIDTH / 2) * size + inset - strokeWeight / 2,(BANK_HEIGHT / 2 - STAIRS_HEIGHT) * size - strokeWeight / 2)
                .lineToAbsolute(-(BANK_WIDTH / 2) * size + inset - strokeWeight / 2, roofLow + TOP_BAR_HEIGHT * size + strokeWeight + pillarHeight)
                .lineToAbsolute(-roofLR + OUTER_PILLAR_GAP * size, roofLow + TOP_BAR_HEIGHT * size + strokeWeight + pillarHeight)
                .lineToAbsolute(-roofLR + OUTER_PILLAR_GAP * size, roofLow + TOP_BAR_HEIGHT * size + strokeWeight)
                .lineToAbsolute(-roofLR - strokeWeight / 2, roofLow + TOP_BAR_HEIGHT * size + strokeWeight);
            add(new SVGPath(
                    new SVGDoubleValue(x + X_SHADDOW * size),
                    new SVGDoubleValue(y + Y_SHADDOW * size),
                    background,
                    null,
                    null,
                    SHADDOW_COLOR).setFilter(SHADDOW_FILTER));
        }

        @Override
        public void generate() {
            numberOfPillars = fourPillars ? 4 : 3;
            pillarWidth = (fourPillars ? PILLAR_WIDTH_4 : PILLAR_WIDTH_3) * size;
            roofLR = (ROOF_WIDTH * size) / 2;
            roofLow = (ROOF_HEIGHT - BANK_HEIGHT / 2) * size;
            pillarX = x - roofLR + OUTER_PILLAR_GAP * size;
            pillarXincrement = (roofLR + roofLR - OUTER_PILLAR_GAP * 2 * size - pillarWidth) / (numberOfPillars - 1);
            pillarHeight = (BANK_HEIGHT - STAIRS_HEIGHT * 2 - ROOF_HEIGHT - TOP_BAR_HEIGHT) * size - strokeWeight * 1.5;
            inset = ((BANK_WIDTH - ROOF_WIDTH) / 2) * size;
            if (shaddow) {
                createMainBackground();
            }
            add(new SVGRect(
                    new SVGDoubleValue(pillarX),
                    new SVGDoubleValue(y + roofLow + strokeWeight + TOP_BAR_HEIGHT * size),
                    new SVGDoubleValue(roofLR + roofLR - OUTER_PILLAR_GAP * 2 * size),
                    new SVGDoubleValue(pillarHeight),
                    null,
                    null,
                    "#ffffff"));
            double shaddowX = -roofLR + OUTER_PILLAR_GAP * size;
            for (int q = 0; q < numberOfPillars; q++) {
                if (shaddow && q < numberOfPillars - 1) {
                    SVGPathValues background = new SVGPathValues()
                        .moveAbsolute(shaddowX, roofLow)
                        .lineToAbsolute(shaddowX + pillarXincrement, roofLow)
                        .lineToAbsolute(shaddowX + pillarXincrement, roofLow + TOP_BAR_HEIGHT * size + strokeWeight)
                        .lineToAbsolute(shaddowX + pillarWidth, roofLow + TOP_BAR_HEIGHT * size + strokeWeight)
                        .lineToAbsolute(shaddowX + pillarWidth, roofLow + TOP_BAR_HEIGHT * size + strokeWeight + pillarHeight)
                        .lineToAbsolute(shaddowX, roofLow + TOP_BAR_HEIGHT * size + strokeWeight + pillarHeight);
                    add(new SVGPath(
                            new SVGDoubleValue(x + X_SHADDOW * size),
                            new SVGDoubleValue(y + Y_SHADDOW * size),
                            background,
                            null,
                            null,
                            SHADDOW_COLOR).setFilter(SHADDOW_FILTER));
                    shaddowX += pillarXincrement;
                }
                add(new SVGRect(
                        new SVGDoubleValue(pillarX),
                        new SVGDoubleValue(y + roofLow + strokeWeight + TOP_BAR_HEIGHT * size),
                        new SVGDoubleValue(pillarWidth),
                        new SVGDoubleValue(pillarHeight),
                        null,
                        null,
                        "url(#bankPillar)"));
                pillarX += pillarXincrement;
            }
            add(new SVGPath(
                    new SVGDoubleValue(x),
                    new SVGDoubleValue(y),
                    new SVGPathValues().moveAbsolute(-roofLR, roofLow)
                                       .lineToAbsolute(0, -BANK_HEIGHT * size * 0.5)
                                       .lineToAbsolute(roofLR, roofLow),
                    strokeWeight,
                    strokeColor,
                    ROOF_COLOR).setRoundLineCap());
            add(new SVGRect(
                    new SVGDoubleValue(x - roofLR),
                    new SVGDoubleValue(y + roofLow + strokeWeight / 2),
                    new SVGDoubleValue(roofLR + roofLR),
                    new SVGDoubleValue(TOP_BAR_HEIGHT * size),
                    strokeWeight,
                    strokeColor,
                    FILL_COLOR));
            add(new SVGRect(
                    new SVGDoubleValue(x - (BANK_WIDTH / 2) * size),
                    new SVGDoubleValue(y + (BANK_HEIGHT / 2 - STAIRS_HEIGHT) * size),
                    new SVGDoubleValue(BANK_WIDTH * size),
                    new SVGDoubleValue(STAIRS_HEIGHT * size),
                    strokeWeight,
                    strokeColor,
                    FILL_COLOR));
            add(new SVGRect(
                    new SVGDoubleValue(x - (BANK_WIDTH / 2) * size + inset),
                    new SVGDoubleValue(y + (BANK_HEIGHT / 2 - STAIRS_HEIGHT * 2) * size),
                    new SVGDoubleValue(BANK_WIDTH * size - inset * 2),
                    new SVGDoubleValue(STAIRS_HEIGHT * size),
                    strokeWeight,
                    strokeColor,
                    FILL_COLOR));
        }
    }
    
    @Override
    public void generate() {
       new SubBank(BANK_WIDTH /2, BANK_HEIGHT / 2, 1, true).setShaddow(true).generate(this);        
    }
}
