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
package org.webpki.saturn.common;

public interface CardImageData {
    
    public double STANDARD_WIDTH          = 300;
    public double STANDARD_HEIGHT         = 180;
    public String STANDARD_NAME           = "Your Name";
    public String STANDARD_ACCOUNT        = "ACCOUNT NUMBER";
    public double STANDARD_TEXT_LEFT      = 30;
    public double STANDARD_TEXT_Y_OFFSET  = 22;
    public int STANDARD_NAME_FONT_SIZE    = 20;
    public int STANDARD_ACCOUNT_FONT_SIZE = 14;
    
    static StringBuilder viewableFormat(String rawCardImage, String htmlWidth) {
        return new StringBuilder("<svg style='width:")
            .append(htmlWidth)
            .append("' viewBox='0 0 320 200' xmlns='http://www.w3.org/2000/svg'>" +
                  "<defs>" +
                    "<clipPath id='cardClip'>" +
                      "<rect x='0' y='0' width='300' height='180' rx='15'/>" +
                    "</clipPath>" +
                    "<filter id='dropShaddow'>" +
                      "<feGaussianBlur stdDeviation='2.4'/>" +
                    "</filter>" +
                    "<linearGradient x1='0' y1='0' x2='1' y2='1' id='innerCardBorder'>" +
                      "<stop offset='0' stop-opacity='0.6' stop-color='#e8e8e8'/>" +
                      "<stop offset='0.48' stop-opacity='0.6' stop-color='#e8e8e8'/>" +
                      "<stop offset='0.52' stop-opacity='0.6' stop-color='#b0b0b0'/>" +
                      "<stop offset='1' stop-opacity='0.6' stop-color='#b0b0b0'/>" +
                    "</linearGradient>" +
                    "<linearGradient x1='0' y1='0' x2='1' y2='1' id='outerCardBorder'>" +
                      "<stop offset='0' stop-color='#b0b0b0'/>" +
                      "<stop offset='0.48' stop-color='#b0b0b0'/>" +
                      "<stop offset='0.52' stop-color='#808080'/>" +
                      "<stop offset='1' stop-color='#808080'/>" +
                    "</linearGradient>" +
                  "</defs>" +
                  "<rect x='12' y='12' width='302' height='182' rx='16' " +
                      "fill='#c0c0c0' filter='url(#dropShaddow)'/>" +
                  "<svg x='10' y='10' clip-path='url(#cardClip)'")
                    // Rewriting the original svg element
            .append(rawCardImage.substring(rawCardImage.indexOf('>')))
            .append(
                  "<rect x='11' y='11' width='298' height='178' rx='14.7' " +
                      "fill='none' stroke='url(#innerCardBorder)' stroke-width='2.7'/>" +
                  "<rect x='9.5' y='9.5' width='301' height='181' rx='16' " +
                      "fill='none' stroke='url(#outerCardBorder)'/>" +
                "</svg>");
    }
}
