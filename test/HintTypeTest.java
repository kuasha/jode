/* HintTypeTest Copyright (C) 1999 Jochen Hoenicke.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; see the file COPYING.  If not, write to
 * the Free Software Foundation, 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 * $Id: HintTypeTest.java 1149 1999-08-19 15:16:59Z jochen $
 */


/**
 * The primitive types can give some headaches.  You almost never can say
 * if a local variable is of type int, char, short etc. <p>
 *
 * Most times this doesn't matter this much, but with int and character's
 * this can get ugly. <p>
 *
 * The solution is to give every variable a hint, which type it probably is.
 * The hint reset, when the type is not possible.  For integer types we try
 * to set it to the smallest explicitly assigned type. <p>
 *
 * Some operators will propagate this hint.<p>
 */
public class HintTypeTest {

    public void charLocal() {
	String s= "Hallo";
	for (byte i=0; i< s.length(); i++) {
	    char c = s.charAt(i);
	    if (c == 'H')
		// The widening to int doesn't occur in byte code, but
		// is necessary.  This is really difficult.
		System.err.println("H is "+(int)c);
	    else
		System.err.println(""+c+" is "+(int)c);
	}
    }
}


