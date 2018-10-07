/* ClassOpTest Copyright (C) 1999 Jochen Hoenicke.
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
 * $Id: ClassOpTest.java 1149 1999-08-19 15:16:59Z jochen $
 */


public class ClassOpTest {
    static void test1() {
	Class c1 = String.class;
	Class c2 = Object.class;
	if (ClassOpTest.class == null);
	c1.getClass();
    }

    void test2() {
	Class c2 = Object.class;
	Class c3 = ClassOpTest.class;
	Class c4 = int[].class;
	Class c5 = Object[][].class;
	Class c6 = int.class;
    }
}
