/* ArrayTest Copyright (C) 1999 Jochen Hoenicke.
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
 * $Id: ArrayTest.java 1149 1999-08-19 15:16:59Z jochen $
 */

import java.io.*;
import java.lang.reflect.*;

public class ArrayTest {
    Serializable s;
    Serializable[] u;
    Cloneable c;

    public void test() {
	int[] i = {4,3,2};
	i = new int[] {1, 2, 3};
	int[] j = new int[] {4,5,6};

	int[][] k = {i,j};

	u = k;
	s = i;
	c = i;
    }

    public void typetest() {
	int[] arr = null;
	s = arr;
	c = arr;
	arr[0] = 3;
	arr = arr != null ? arr : new int[4];
    }

    public static void main(String[] param) {
	int[] arr = new int[4];
	Class cls = arr.getClass();
	System.err.println("int[].getClass() is: "+cls);
	System.err.println("int[].getClass().getSuperclass() is: "
			   + cls.getSuperclass());
	Class[] ifaces = cls.getInterfaces();
	System.err.print("int[].getClass().getInterfaces() are: ");
	for (int i = 0; i < ifaces.length; i++) {
	    if (i > 0)
		System.err.print(", ");
	    System.err.print(ifaces[i]);
	}
	System.err.println();

	Field[] fields = cls.getDeclaredFields();
	System.err.print("int[].getClass().getDeclaredFields() are: ");
	for (int i = 0; i < fields.length; i++) {
	    if (i > 0)
		System.err.print(", ");
	    System.err.print(fields[i]);
	}
	System.err.println();

	Method[] methods = cls.getDeclaredMethods();
	System.err.print("int[].getClass().getDeclaredMethods() are: ");
	for (int i = 0; i < methods.length; i++) {
	    if (i > 0)
		System.err.print(", ");
	    System.err.print(methods[i]);
	}
	System.err.println();

	Object o = arr;
	System.err.println("arr instanceof Serializable: "+
			   (o instanceof Serializable));
	System.err.println("arr instanceof Externalizable: "+
			   (o instanceof Externalizable));
	System.err.println("arr instanceof Cloneable: "+
			   (o instanceof Cloneable));
// 	System.err.println("arr instanceof Comparable: "+
// 			   (o instanceof Comparable));
    }
}

