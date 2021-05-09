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

import java.io.IOException;

import java.security.GeneralSecurityException;

import java.util.LinkedHashMap;

import org.webpki.tools.svg.SVGPathValues.SubCommand;

import org.webpki.xml.DOMAttributeReaderHelper;
import org.webpki.xml.DOMReaderHelper;
import org.webpki.xml.DOMWriterHelper;
import org.webpki.xml.XMLObjectWrapper;
import org.webpki.xml.XMLSchemaCache;

public class SVGEmbeddedText {
    
    public static class DecodedGlyph {
        SVGPathValues pathValues;
        double xAdvance;

        public SVGPathValues getSVGPathValues() {
            return pathValues;
        }

        public double getXAdvance() {
            return xAdvance;
        }
    }

    LinkedHashMap<Character,DecodedGlyph> glyphs;

    public static class GlyphReader extends XMLObjectWrapper {
        
        LinkedHashMap<Character,DecodedGlyph> glyphs = new LinkedHashMap<>();
        int index;
        String d;

        @Override
        public String element() {
            return "defs";
        }

        @Override
        protected void fromXML(DOMReaderHelper rd) throws IOException {
            DOMAttributeReaderHelper ah = rd.getAttributeHelper ();
            rd.getChild ();
            do {
                rd.getNext("glyph");
                DecodedGlyph decodedGlyphs = new DecodedGlyph();
                glyphs.put(ah.getString("unicode").charAt(0), decodedGlyphs);
                decodedGlyphs.xAdvance = Double.parseDouble(ah.getString("horiz-adv-x"));
                SVGPathValues pathValues = new SVGPathValues();
                d = ah.getString("d");
                index = 0;
                char last = 0;
                while (index < d.length()) {
                    char c = d.charAt(index);
                    if (!Character.isDigit(c) && c != '-') {
                        last = c;
                        index += 2;
                    }
                    switch (last) {
                    case 'm':
                        pathValues.moveRelative(x(), y());
                        break;
                    case 'M':    
                        pathValues.moveAbsolute(x(), y());
                        break;
                    case 'l':
                        pathValues.lineToRelative(x(), y());
                        break;
                    case 'L':    
                        pathValues.lineToAbsolute(x(), y());
                        break;
                    case 'c':
                        pathValues.cubicBezierRelative(x(), y(), x(), y(), x(), y());
                        break;
                    case 'C':    
                        pathValues.cubicBezierAbsolute(x(), y(), x(), y(), x(), y());
                        break;
                    case 'z':
                    case 'Z':
                        pathValues.endPath();
                        break;
                    default:
                        throw new IOException("GlyphReader path: " + index + "\n" + d);
                    }
                }
                decodedGlyphs.pathValues = pathValues;
                
            } while (rd.hasNext());
        }

        private double y() {
            int i = index;
            while (d.charAt(++index) != ' ')
                ;
            return -Double.parseDouble(d.substring(i, index++));
        }

        private double x() {
            int i = index;
            while (d.charAt(++index) != ',')
                ;
            return Double.parseDouble(d.substring(i, index++));
        }

        @Override
        protected boolean hasQualifiedElements() {
            // TODO Auto-generated method stub
            return true;
        }

        @Override
        protected void init() throws IOException {
            addSchema ("glypher.xsd");            
        }

        @Override
        public String namespace() {
             return "http://glypher";
        }

        @Override
        protected void toXML(DOMWriterHelper arg0) throws IOException {
            // TODO Auto-generated method stub
            
        }
    }

     public DecodedGlyph getDecodedGlyph(char c, double transform) {
        DecodedGlyph decodedGlyph = glyphs.get(c);
        DecodedGlyph newGlyph = new DecodedGlyph();
        newGlyph.pathValues = new SVGPathValues(decodedGlyph.pathValues);
        newGlyph.xAdvance = decodedGlyph.xAdvance / transform;
        for (SVGPathValues.SubCommand subCommand : decodedGlyph.pathValues.commands) {
            SVGPathValues.SubCommand newSubCommand = null;
            try {
                newSubCommand = (SubCommand) subCommand.copy();
            } catch (CloneNotSupportedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            newGlyph.pathValues.commands.add(newSubCommand);
            for (SVGPathValues.Coordinate coordinate : newSubCommand.coordinates) {
                coordinate.xValue /= transform;
                coordinate.yValue /= transform;
            }
        }
        newGlyph.pathValues.maxX /= transform;
        newGlyph.pathValues.maxY /= transform;
        newGlyph.pathValues.minX /= transform;
        newGlyph.pathValues.minY /= transform;
        return newGlyph;
    }
    
    public SVGEmbeddedText(Class<? extends SVGGlyphContainer> customGlyphs) {
        try {
            XMLSchemaCache xmlSchemaCache = new XMLSchemaCache();
            xmlSchemaCache.addWrapper(GlyphReader.class);
            SVGGlyphContainer glyphContainer = customGlyphs.newInstance ();
            glyphs = ((GlyphReader)xmlSchemaCache.parse(glyphContainer.init())).glyphs;
        } catch (InstantiationException e) {
            throw new IllegalArgumentException("Class " + customGlyphs.getName () + 
                                               " is not a valid xml wrapper (InstantiationException instantiating).");
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException("Class " + customGlyphs.getName () + 
                                               " is not a valid xml wrapper (IllegalAccessException instantiating).");
        } catch (IOException | GeneralSecurityException e) {
            throw new RuntimeException(e);
        }
    }
}
