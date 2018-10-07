/* TriadicExpr Copyright (C) 1998-1999 Jochen Hoenicke.
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
 * $Id: TriadicExpr.java 1149 1999-08-19 15:16:59Z jochen $
 */


public class TriadicExpr {
    int a,b,c,d,e,f,g,h,i,j;
    boolean bool;

    void test() {
        if( (a< b ? bool : (a<b)))
            a=b;
        else
            b=a;

        if ((a<b?b<a:i<j) ? (c<d ? e<f : g < h) : (j<i ? h<g:f<e))
            a=(b<a ? g : h);
        else
            b=a;
    }
}


