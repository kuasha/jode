/* LocalTypes Copyright (C) 1999 Jochen Hoenicke.
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
 * $Id: LocalTypes.java 1149 1999-08-19 15:16:59Z jochen $
 */


class A {
}

interface I1 {
}

interface I2 {
}

interface I12 extends I1, I2 {
}

class B extends A implements I1 {
}

class C extends B implements I2, I12 {
}

class D extends A implements I12 {
}

class E extends A implements I2 {
}

public class LocalTypes {
    A a;
    B b;
    C c;
    D d;
    E e;

    I1 i1;
    I2 i2;
    I12 i12;

    boolean g_bo;
    byte g_by;
    short g_sh;
    int g_in;

    int z;
    int[]ain;

    public void arithTest() {
        int a=1,b=2;
        boolean xb = true,yb = false;
	int     xi =    1,yi =     0;
        int c=0;
        arithTest();
        if ((xb & yb) || (xi & yi) != 0) {
            c = 5;
            arithTest();
            xb &= yb;
            xi &= yi;
            arithTest();
            xb = xb | yb;
            xi = xi | yi;
            arithTest();
            xb ^= yb;
            xi ^= yi;
            arithTest();
            xb = xb && yb;
            xi = (xi != 0) && (yi != 0) ? 1 : 0;
            arithTest();
            b <<= a;
            b <<= c;
        }
	xi++;
        a&=b;
    }
    
    public void intTypeTest() {
        boolean b = false;
        boolean abo[] = null;
        byte aby[] = null;
        byte by;
        int in;
        short sh;
        b = g_bo;
        in = g_sh;
        sh = (short)g_in;
        by = (byte)sh;
        sh = by;
        in = by;
        abo[0] = g_bo;
        abo[1] = false;
        abo[2] = true;
        aby[0] = g_by;
        aby[1] = 0;
        aby[2] = 1;
    }

    native void arrFunc(B[] b);
    
    /**
     * This is an example where it is really hard to know, which type
     * each local has.  
     */
    void DifficultType () {
        B myB = c;
	C myC = c;
        I2 myI2 = c;
        I12 myI12 = c;
	boolean bool = true;
        B[] aB = new C[3];
        arrFunc(new C[3]);

	while (bool) {
            if (bool) {
                i1 = myB;
                i2 = myC;
                i1 = aB[0];
            } else if (bool) {
                i1 = myI12;
                i2 = myI2;
            } else {
                i1 = myC;
                i2 = myI12;
            }
	    myB = b;
            if (bool)
                myI2 = i12;
            else {
                myI12 = d;
                myI2 = e;
            }
	}
    }

    /**
     * This is an example where it is really hard to know, which type
     * each local has.  
     */
    void DifficultArrayType () {
        boolean bool = true;
        B[] aB = new C[3];
        arrFunc(new C[3]);
        C[][][][][] x = new C[4][3][4][][];
        int[][][][][] y = new int[1][2][][][];

	while (bool) {
            if (bool) {
                i1 = aB[0];
                aB[0] = b;
            }
	}
    }

    public void castTests() {
	System.err.println(null instanceof int[]);
	System.err.println(((Object)new int[4]) instanceof byte[]);
	System.err.println(((Object)new byte[4]) instanceof int[]);
	System.err.println(((Object)new int[4]) instanceof LocalTypes);
	System.err.println(((Object)new int[4]) instanceof int[][]);
	System.err.println(new Object[4] instanceof int[][]);
	System.err.println(new int[5][4] instanceof java.io.Serializable[]);
	System.err.println(((Object)this) instanceof RuntimeException);
	System.err.println(((RuntimeException)(Object)this).getMessage());
	((java.io.PrintStream)null).println("Hallo");
	((LocalTypes) null).a = ((LocalTypes) null).b;
    }
}

