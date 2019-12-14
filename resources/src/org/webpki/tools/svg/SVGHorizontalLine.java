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

public class SVGHorizontalLine extends SVGLine {
    
    public static class Arrow {
        double length;
        double height;
        double gutter;
        
        public Arrow(double length, double height, double gutter) {
            this.length = length;
            this.height = height;
            this.gutter = gutter;
        }
    }
    
    public static class Rect extends SVGRect {

        Double alignX;
        Double alignY;
        
        public Rect(Double alignX,
                    Double alignY,
                    SVGValue width,
                    SVGValue height,
                    Double strokeWidth,
                    String strokeColor,
                    String fillColor) {
            super(new SVGDoubleValue(0),
                  new SVGDoubleValue(0),
                  width,
                  height,
                  strokeWidth,
                  strokeColor,
                  fillColor);
            this.alignX = alignX;
            this.alignY = alignY;
        }

        @Override
        public Rect setRadiusY(double value) {
            super.setRadiusY(value);
            return this;
        }

        @Override
        public Rect setRadiusX(double value) {
            super.setRadiusX(value);
            return this;
        }
    }
    
    public static class Text extends SVGText {

        Double alignX;
        Double alignY;

        public Text(Double alignX,
                    Double alignY,
                    String fontFamily,
                    double fontSize,
                    String text) {
            super(new SVGDoubleValue(0),
                  new SVGDoubleValue(0),
                  fontFamily,
                  fontSize,
                  TEXT_ANCHOR.MIDDLE,
                  text);
            this.alignX = alignX;
            this.alignY = alignY;
        }

        @Override
        public Text setFontColor(String fontColor) {
            super.setFontColor(fontColor);
            return this;
        }

        @Override
        public Text setLink(String url, String toolTip, Boolean replace) {
            super.setLink(url, toolTip, replace);
            return this;
        }
    }

    double leftGutter;

    double rightGutter;
    
    public SVGHorizontalLine(SVGValue x,
                             SVGValue y,
                             SVGValue length,
                             double strokeWidth,
                             String strokeColor) {
        super(x, y, new SVGAddDouble(x, length), y, strokeWidth, strokeColor);
    }
    
    public SVGHorizontalLine(SVGVerticalLine vertLine1,
                             SVGVerticalLine vertLine2,
                             SVGValue y,
                             double strokeWidth,
                             String strokeColor) {
        this(new SVGAddOffset(vertLine1.getAttribute(SVGAttributes.X1),
                              vertLine1.getAttribute(SVGAttributes.STROKE_WIDTH).getDouble() / 2),
             y,
             new SVGAddOffset(new SVGSubtractDouble(vertLine2.getAttribute(SVGAttributes.X1),
                                                    vertLine1.getAttribute(SVGAttributes.X1)),
                          - (vertLine1.getAttribute(SVGAttributes.STROKE_WIDTH).getDouble() / 2  +
                             vertLine2.getAttribute(SVGAttributes.STROKE_WIDTH).getDouble() / 2)),
             strokeWidth,
             strokeColor);
    }
    
    public SVGHorizontalLine setLeftGutter(double gutter) {
        getAttributes().put(SVGAttributes.X1,
                        new SVGAddOffset(getAttribute(SVGAttributes.X1), leftGutter = gutter));
        return this;
    }

    public SVGHorizontalLine setRightGutter(double gutter) {
        getAttributes().put(SVGAttributes.X2,
                            new SVGAddOffset(getAttribute(SVGAttributes.X2), rightGutter = -gutter));
        return this;
    }

    public SVGHorizontalLine setLeftArrow (Arrow arrow) {
        SVGValue x = new SVGAddOffset(getAttribute(SVGAttributes.X1), arrow.gutter);
        SVGValue y = new SVGAddOffset(getAttribute(SVGAttributes.Y1), -arrow.height / 2);
        setLeftGutter(arrow.gutter + arrow.length / 2);
        afterDependencyElements.add(new SVGPolygon(x,
                                                   y,
                                                   new double[]{0, arrow.height / 2,
                                                                arrow.length, 0,
                                                                arrow.length, arrow.height},
                                                   null,
                                                   null,
                                                   getAttribute(SVGAttributes.STROKE_COLOR).getStringRepresentation()));
        return this;
    }

    public SVGHorizontalLine setRightArrow (Arrow arrow) {
        SVGValue x = new SVGAddOffset(getAttribute(SVGAttributes.X2), -arrow.gutter - arrow.length);
        SVGValue y = new SVGAddOffset(getAttribute(SVGAttributes.Y2), -arrow.height / 2);
        setRightGutter(arrow.gutter + arrow.length / 2);
        afterDependencyElements.add(new SVGPolygon(x,
                                                   y,
                                                   new double[]{0, 0,
                                                                0, arrow.height,
                                                                arrow.length, arrow.height / 2},
                                                   null,
                                                   null,
                                                   getAttribute(SVGAttributes.STROKE_COLOR).getStringRepresentation()));
        return this;
    }
    
    public SVGHorizontalLine setRect(Rect rect) {
        double strokeWidth = rect.getAttribute(SVGAttributes.STROKE_WIDTH) == null ?
                                   0 : rect.getAttribute(SVGAttributes.STROKE_WIDTH).getDouble();
        double xOffset = 0;
        SVGValue x1 = new SVGAddOffset(getAttribute(SVGAttributes.X1), - leftGutter);
        SVGValue x2 = new SVGAddOffset(getAttribute(SVGAttributes.X2), + rightGutter);
        if (rect.alignX == null) {
            rect.getAttributes().put(SVGAttributes.X, new SVGCenter(x1, x2, rect.getAttribute(SVGAttributes.WIDTH)));
        }
        if (rect.alignY == null) {
            rect.getAttributes().put(SVGAttributes.Y,
                    new SVGAddOffset(getAttribute(SVGAttributes.Y1), -(rect.getAttribute(SVGAttributes.HEIGHT).getDouble() / 2)));
        }
        
 //       rect.getAttributes().put(SVGAttributes.Y,
 //               new SVGAddOffset(getAttribute(SVGAttributes.Y1),
 //                                (strokeWidth / 2)));
        afterDependencyElements.add(rect);
        return this;
    }

    public SVGHorizontalLine setText(Text text) {
        SVGValue x1 = new SVGAddOffset(getAttribute(SVGAttributes.X1), - leftGutter);
        SVGValue x2 = new SVGAddOffset(getAttribute(SVGAttributes.X2), + rightGutter);

        SVGValue x = new SVGCenter(x1, x2, 0);
        if (text.alignX == null) {
        } else if (text.alignX > 0) {
            x = new SVGAddOffset(x1, text.alignX);
            text.getAttributes().put(SVGAttributes.TEXT_ANCHOR, new SVGStringValue(SVGText.TEXT_ANCHOR.START.toString().toLowerCase()));
        } else {
            x = new SVGAddOffset(x2, text.alignX);
            text.getAttributes().put(SVGAttributes.TEXT_ANCHOR, new SVGStringValue(SVGText.TEXT_ANCHOR.END.toString().toLowerCase()));
        }
        text.getAttributes().put(SVGAttributes.X, x);

        SVGValue y = getAttribute(SVGAttributes.Y1);
        if (text.alignY == null) {
            text.setDy("0.35em");
        } else {
            y = new SVGAddOffset(y, -text.alignY);
        }
        text.getAttributes().put(SVGAttributes.Y, y);
        
 //       rect.getAttributes().put(SVGAttributes.Y,
 //               new SVGAddOffset(getAttribute(SVGAttributes.Y1),
 //                                (strokeWidth / 2)));
        afterDependencyElements.add(text);
        return this;
    }
}
