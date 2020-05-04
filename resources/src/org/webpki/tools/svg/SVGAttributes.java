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

public enum SVGAttributes {
    X              ("x"),
    Y              ("y"),
    X1             ("x1"),
    X2             ("x2"),
    Y1             ("y1"),
    Y2             ("y2"),
    RX             ("rx"),
    RY             ("ry"),
    DY             ("dy"),
    CX             ("cx"),
    CY             ("cy"),
    R              ("r"),
    D              ("d"),
    STROKE_LINECAP ("stroke-linecap"),
    STROKE_WIDTH   ("stroke-width"),
    STROKE_DASHES  ("stroke-dasharray"),
    STROKE_OPACITY ("stroke-opacity"),
    WIDTH          ("width"),
    HEIGHT         ("height"),
    POINTS         ("points"),
    FILTER         ("filter"),
    FILL_COLOR     ("fill"),
    FILL_OPACITY   ("fill-opacity"),
    STROKE_COLOR   ("stroke"),
    FONT_FAMILY    ("font-family"),
    FONT_SIZE      ("font-size"),
    FONT_WEIGHT    ("font-weight"),
    LETTER_SPACING ("letter-spacing"),
    TRANSFORM      ("transform"),
    TEXT_ANCHOR    ("text-anchor");

    String svgNotation;
    
    SVGAttributes(String svgNotation) {
        this.svgNotation = svgNotation;
    }
    
    @Override
    public String toString() {
        return svgNotation;
    }
}

