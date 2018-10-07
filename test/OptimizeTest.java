/* OptimizeTest Copyright (C) 1999 Jochen Hoenicke.
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
 * $Id: OptimizeTest.java 1149 1999-08-19 15:16:59Z jochen $
 */


public class OptimizeTest {

    public final int getInlined(String str, int i) {
	return str.charAt(i);
    }

    public final int getInlined(String str, int i, OptimizeTest t) {
	return str.charAt(i) + t.getInlined(str, i) + i;
    }


    public final int complexInline(String str, int i) {
	System.err.println("");
	return str.charAt(i)+str.charAt(-i);
    }

    public int notInlined(String str, int i, OptimizeTest t) {
	return str.charAt(i) + t.getInlined(str, i);
    }
    
    public final void longInline(String str, int i) {
	str.replace('a','b');
	System.err.println(str.concat(String.valueOf(str.charAt(i))));
    }

    public int g;
   
    /**
     * This is a really brutal test.  It shows that side effects can
     * make the handling of inlined methods really, really difficult.
     */ 
    public final int sideInline(int a) {
	return g++ + a;
    }

    public void main(String[] param) {
	OptimizeTest ot = new OptimizeTest();

	System.err.println(ot.getInlined("abcde".replace('a','b'), param.length));
	System.err.println(ot.getInlined("Hallo", ot.notInlined(param[1], 10 - ot.getInlined(param[0], 0, new OptimizeTest()), ot)));
	System.err.println(ot.complexInline("ollah", param.length));
	System.err.println("result: "+(g++ + sideInline(g) + g++) + "g: "+g);
	longInline("Hallo", 3);
	System.err.println("result:"+ 
			   (g++ + InlineTest
			    .difficultSideInline(this, g) 
			    + g++) + "g: "+g);
	// This was a check which methods are inlined. The result:
	// Only methods in the same package or in sub packages.
// 	System.err.println("result:"+ 
// 			   (g++ + inline.InlineTest
// 			    .difficultSideInline(this, g) 
// 			    + g++) + "g: "+g);
// 	System.err.println("result:"+ 
// 			   (g++ + jode.InlineTest
// 			    .difficultSideInline(this, g) 
// 			    + g++) + "g: "+g);
    }
}
