/* StringQuoter Copyright (C) 1999-2002 Jochen Hoenicke.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; see the file COPYING.LESSER.  If not, write to
 * the Free Software Foundation, 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 * $Id: StringQuoter.java 1367 2002-05-29 12:06:47Z hoenicke $
 */

package net.sf.jode.util;
/**
 * This is a simple class to quote a string or a char.  It puts it in
 * quotes (" resp. ') and prints special chars with the same syntax as
 * strings and chars in java source codes.  
 */
public class StringQuoter {
    /**
     * This is the static method, that quotes a string.
     */
    public static String quote(String str) {
        StringBuffer result = new StringBuffer("\"");
        for (int i=0; i< str.length(); i++) {
            char c;
            switch (c = str.charAt(i)) {
            case '\0':
                result.append("\\0");
                break;
            case '\t':
                result.append("\\t");
                break;
            case '\n':
                result.append("\\n");
                break;
            case '\r':
                result.append("\\r");
                break;
            case '\\':
                result.append("\\\\");
                break;
            case '\"':
                result.append("\\\"");
                break;
            default:
                if (c < 32) {
                    String oct = Integer.toOctalString(c);
                    result.append("\\000".substring(0, 4-oct.length()))
                        .append(oct);
                } else if (c >= 32 && c < 127)
                    result.append(str.charAt(i));
                else {
                    String hex = Integer.toHexString(c);
                    result.append("\\u0000".substring(0, 6-hex.length()))
                        .append(hex);
                }
            }
        }
        return result.append("\"").toString();
    }

    /**
     * This is the static method, that quotes a char.
     */
    public static String quote(char c) {
	switch (c) {
	case '\0':
	    return "\'\\0\'";
	case '\t':
                return "\'\\t\'";
	case '\n':
	    return "\'\\n\'";
	case '\r':
	    return "\'\\r\'";
	case '\\':
	    return "\'\\\\\'";
	case '\"':
	    return "\'\\\"\'";
	case '\'':
	    return "\'\\\'\'";
            }
	if (c < 32) {
	    String oct = Integer.toOctalString(c);
	    return "\'\\000".substring(0, 5-oct.length())+oct+"\'";
	}
	if (c >= 32 && c < 127)
	    return "\'"+c+"\'";
	else {
	    String hex = Integer.toHexString(c);
	    return "\'\\u0000".substring(0, 7-hex.length())+hex+"\'";
	}
    }
}
