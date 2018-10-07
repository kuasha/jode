/* AssignOp Copyright (C) 1998-1999 Jochen Hoenicke.
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
 * $Id: AssignOp.java 1149 1999-08-19 15:16:59Z jochen $
 */


public class AssignOp {
    static short  static_short;
    static int    static_int;
    static double static_double;
    static String static_String;
    static long   static_long;

    short  obj_short;
    int    obj_int;
    long   obj_long;
    double obj_double;
    String obj_String;

    short[]  arr_short;
    int []   arr_int;
    long[]   arr_long;
    double[] arr_double;
    String[] arr_String;

    void assop() {
	short local_short = 0;
        int local_int = 0;
        long local_long = 0;
        double local_double = 1.0;
        String local_String = null;

        local_short -= 25 * local_int;
	static_short += 100 - local_int;
	obj_short /= 0.1;
	arr_short[local_int] >>= 25;

	local_long -= 15L;
	static_long <<= local_int;
	obj_long >>>= 3;
	arr_long[4+local_int] *= obj_long - static_long;

        local_int |= 25 | local_int;
        static_int <<= 3;
        obj_int *= 17 + obj_int;
        arr_int[local_int] /= (obj_int+=7);

        local_double /= 3.0;
        static_double *= obj_int;
        obj_double -= 25;
        arr_double[local_int] /= (local_double+=7.0);

        local_String += "Hallo";
        static_String += "Hallo";
        obj_String += "Hallo";
        arr_String[0] += local_double + static_String + "Hallo" + obj_int;
    }

    void prepost() {
        int local_int= -1;
        long local_long= 4;
        
        local_long = local_int++;
        obj_long = ++obj_int;
        arr_long[static_int] = static_long = (arr_long[--static_int] = (static_int--))+1;
    }

    void iinc() {
        int local_int = 0;
        local_int += 5;
        obj_int = (local_int -= 5);

        static_int = local_int++;
        obj_int = --local_int;
    }
}
