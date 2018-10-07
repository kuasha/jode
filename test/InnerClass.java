/* InnerClass Copyright (C) 1998-1999 Jochen Hoenicke.
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
 * $Id: InnerClass.java 1149 1999-08-19 15:16:59Z jochen $
 */


public class InnerClass 
{
    private int x;

    class Parent
    {
	int a = 1;
	protected int x;

	void ambiguous() {
	    System.err.println("Parent");
	}
    }

    class Inner 
    {
        int a = 4;
        private int b = x;

        class InnerInner extends Parent
	{

            public int getB() {
                return Inner.this.getB();
            }

            public int getStaticB(InnerInner innerinner) {
		createInner(InnerClass.this);
                return innerinner.getB();
            }

            public InnerInner(int c) {
                this.x = c;
		InnerClass.this.x = b;
                this.a = b;
		Inner.this.a = c;
		this.ambiguous();
		Inner.this.ambiguous();
		InnerClass.this.ambiguous();
            }
        }

	void ambiguous() {
	    System.err.println("Inner");
	}

        private int getB() {
            return b;
        }
        
        public InnerInner createInnerInner(int a) {
            return new InnerInner(a);
        }
    }

    class Extended 
	extends Inner.InnerInner
    {

        Extended(Inner inner) {
            inner.super(3);
	    this.ambiguous();
	    InnerClass.this.ambiguous();
        }
    }

    void ambiguous() {
	System.err.println("InnerClass");
    }

    private static Inner createInner(InnerClass outer) {
	return outer.new Inner();
    }

    InnerClass() {
        new Inner().createInnerInner(10).getB();
	createInner(this).new InnerInner(42);
    }
}
