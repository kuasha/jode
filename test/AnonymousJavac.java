/* AnonymousJavac Copyright (C) 1999 Jochen Hoenicke.
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
 * $Id: AnonymousJavac.java 1224 2000-01-30 16:49:58Z jochen $
 */


import java.util.Vector;

public class AnonymousJavac {
    class Inner {
	int var = 3;

	public void test() {
	    final long longVar = 5;
	    final double dblVar = 3;
	    class Hello {
		int var = (int) longVar;

		{
		    System.err.println("all constructors");
		}

		Hello() {
		    System.err.println("construct");
		}
		Hello(String info) {
		    System.err.println("construct: "+info);
		}

		private void hello() {
		    this.hashCode();
		    Inner.this.hashCode();
		    Inner.this.var = var;
		    AnonymousJavac.this.hashCode();
		    System.err.println("HelloWorld: "+dblVar);
		}
	    };
	    final Hello hi = new Hello();
	    final Hello ho = new Hello("ho");
	    final Object o = new Object() {
		int blah = 5;
		Hello hii = hi;
		Hello hoo = new Hello("hoo");

		{
		    System.err.println("Anonymous Constructor speaking");
		}

		public String toString() {
		    this.hii.hello();
		    hi.hello();
		    return Integer.toHexString(AnonymousJavac.this.hashCode()
					       +blah);
		}

		{
		    System.err.println("Anonymous Constructor continues");
		}

	    };
	    Object p = new Object() {
		public String toString() {
		    return o.toString();
		}
	    };

	    Inner blub1 = new Inner("Inner param") {
		Hello hii = hi;

		public void test() {
		    System.err.println("overwritten: "+hii+hi);
		}
	    };

	    Inner blub2 = new AnonymousJavac().new Inner("Inner param") {
		Hello hii = hi;

		public void test() {
		    System.err.println("overwritten: "+hii);
		    AnonymousJavac.this.hashCode();
		}
	    };

	    class Hi extends Inner {
		public Hi() {
		    super("Hi World");
		}
	    }

	    Vector v = new Vector(hi.var, var) {
		Hello hii = hi;
		public Object clone() {
		    return super.clone();
		}
	    };

	    Hi hu = new Hi();
		
	}
	Inner (String str) {
	}
    }


	public void test() {
	    class Hello {
		int var = 4;

		Hello() {
		    System.err.println("construct");
		}
		Hello(String info) {
		    System.err.println("construct: "+info);
		}

		public void hello() {
		    this.hashCode();
		    AnonymousJavac.this.hashCode();
		    System.err.println("HelloWorld");
		}
	    };
	    final Hello hi = new Hello();
	    final Hello ho = new Hello("ho");
	    final Object o = new Object() {
		int blah = 5;
		Hello hii = hi;

		public String toString() {
		    this.hii.hello();
		    hi.hello();
		    return Integer.toHexString(AnonymousJavac.this.hashCode()
					       +blah);
		}
	    };
	    Object p = new Object() {
		public String toString() {
		    return o.toString();
		}
	    };

	    Inner blub1 = new Inner("Inner param") {
		public void test() {
		    System.err.println("overwritten");
		}
	    };

	    Inner blub2 = new AnonymousJavac().new Inner("Inner param") {
		public void test() {
		    System.err.println("overwritten");
		}
	    };

	    class Hi extends Inner {
		public Hi() {
		    super("Hi World");
		}
	    }

	    Vector v = new Vector(hi.var, new Inner("Hi").var) {
		public Object clone() {
		    return super.clone();
		}
	    };

	    Hi hu = new Hi();
	}
}
