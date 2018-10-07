/* MethodScopedClass Copyright (C) 1999 Jochen Hoenicke.
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
 * $Id: MethodScopedClass.java 1188 1999-11-04 00:21:51Z jochen $
 */

import java.util.Vector;

public class MethodScopedClass {
    int var = 3;
    
    public void test1() {
	final long longVar = 5;
	final int intVar = var;
	final double dblVar = 3;

	final float fooFloat = 5;
	final float barFloat = 7;
	
	class Hello {
	    int var = (int) longVar;

	    {
		System.err.println("all constructors");
	    }
	    
	    Hello(float f, String info) {
		    System.err.println("construct: "+info+" "+f);
	    }

	    Hello(float f) {
		this(f, "default");
	    }

	    public void hello() {
		System.err.println("HelloWorld: "+dblVar+" "+intVar);
	    }
	};

	/* This test checks if the variables longVar, intVar and dblVar
	 * can be detected correctly as inner values.  The first parameter
	 * of the Hello constructor can't be an outer value, because bar
	 * uses a different value for this.
	 */
	class foo {
	    foo() {
		new Hello(fooFloat);
	    }
	}

	class bar {
	    bar() {
		new Hello(barFloat, "bar");
	    }
	}

	new foo();
	new bar();
    }

//      public void test2() {
//  	final long longVar = 5;
//  	final int intVar = var;
//  	final double dblVar = 3;

//  	final float barFloat = 7;
	
//  	class Hello {
//  	    int var = (int) longVar;

//  	    {
//  		System.err.println("all constructors");
//  	    }
	    
//  	    Hello(int i, String info) {
//  		    System.err.println("construct: "+info);
//  	    }

//  	    Hello(int i) {
//  		this(i, "This can only be compiled correctly"
//  		     +" by a recent jikes");
//  	    }

//  	    public void hello() {
//  		System.err.println("HelloWorld: "+dblVar+" "+intVar);
//  	    }
//  	};

//  	/* Similar to the test above.  But this time foo is given barFloat
//  	 * as parameter.  
//  	 */
//  	class foo {
//  	    foo(int param) {
//  		new Hello(param);
//  	    }

//  	}

//  	class bar {
//  	    bar() {
//  		new Hello(barFloat, "bar");
//  	    }
//  	    test() {
//  		new foo(fooFloat);
//  	    }
//  	}

//  	new foo(barFloat);
//  	new bar();
//      }
}
