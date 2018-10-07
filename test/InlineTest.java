/* InlineTest Copyright (C) 1999 Jochen Hoenicke.
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
 * $Id: InlineTest.java 1149 1999-08-19 15:16:59Z jochen $
 */


/**
 * Check if inlines are allowed over package borders.  You should put it
 * in a different package to check if inlining works over package borders.
 *
 * @author Jochen Hoenicke
 * @see jode.test.OptimizeTest
 */
public class InlineTest {

    public static final int 
	difficultSideInline(jode.test.OptimizeTest ot, int a) {
	return ot.g++ + a;
    }
}
