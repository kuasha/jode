/* Expressions Copyright (C) 1999 Jochen Hoenicke.
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
 * $Id: Expressions.java 1149 1999-08-19 15:16:59Z jochen $
 */

public class Expressions {
    double  cd;
    float   cf;
    long    cl;
    int     ci;
    char    cc;
    short   cs;
    byte    cb;
    boolean cz;

    void postIncDecExpressions() {
	cd++;
	cf++;
	cl++;
	ci++;
	cs++;
	cb++;
	cc++;
	cd--;
	cf--;
	cl--;
	ci--;
	cs--;
	cb--;
	cc--;
	float f = 0.0F;
	double d = 0.0;
	long l = 0L;
	f++;
	f--;
	d++;
	d--;
	l++;
	l--;
    }
    

    void unary() {
	short s = 25;
	s = (short) ~s;
	boolean b = !true;
	s = b? s: cs;
	char c= 25;
	c = b ? c: cc;
    }

    void shift() {
	int i = 0;
	long l =0;
	l >>= i;
	l >>= i;
	i >>= i;
	l = l << l;
	l = l << i;
	l = i << l;
	l = i << i;
	i = (int) (l << l);
	i = (int) (l << i);
	i = i << l;
	i = i << i;
	cl >>= ci;
	ci <<= ci;
	cl = cl << cl;
	cl = cl << ci;
	cl = ci << cl;
	cl = ci << ci;
	ci = (int) (cl << cl);
	ci = (int) (cl << ci);
	ci = ci << cl;
	ci = ci << ci;
    }
}
