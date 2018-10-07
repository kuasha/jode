/* IfCombine Copyright (C) 1998-1999 Jochen Hoenicke.
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
 * $Id: IfCombine.java 1149 1999-08-19 15:16:59Z jochen $
 */


public class IfCombine {
    boolean a,b,c;
    int i,j,k;

    public void foo() {
        if ( a && (b || c) && (i<k)) {
            if (a != b)
                i = 1;
            else
                i = 2;
        }
    }
}
