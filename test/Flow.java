/* Flow Copyright (C) 1999 Jochen Hoenicke.
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
 * $Id: Flow.java 1149 1999-08-19 15:16:59Z jochen $
 */


public abstract class Flow {
    int g;
    int[] ain;

    void skip() {
        for (int i=0; i<3; i++) {
            if (g > 5) {
                for (int j=0; j<5; j++) {
                    g++;
                }
            }
            i--;
        }
    }

    /* This tests the while in a switch problem.
     */
    public void switchWhileTest() {
        int dir = g;
        int x = 0;
        int y = 0;
        boolean done = false;
        g = 5;
        switch (dir) {
        case 1:
            while (!done) {
                done = true;
		if (g > 7) 
		    g = g - 4;
                x = g;
                y = g;
                if (x > 7)
                    x = x - 4;
		for (int i=0; i<4; i++) {
		    for (int j=0; j<5; j++) {
                        if (ain[j] == x + i && ain[j] == y)
                            done = false;
                    }
		}
            }
	    for (int i=0; i<5; i++) {
                ain[g] = x + i;
                ain[g] = y;
                g += 1;
            }
            break;
        case 2:
            while (!done) {
                done = true;
                x = g;
                y = g;
                if (y > 7)
                    y = y - 4;
		for (int i=0; i<4; i++) {
		    for (int j=0; j<4; j++) {
                        if (ain[j] == x && ain[j] == y + i)
                            done = false;
                    }
                }
            }
	    for (int i = 0; i<4; i++) {
                ain[g] = x;
                ain[g] = y + i;
                g += 1;
            }
            break;
	case 3: // Same code as case 2 slightly optimized
	big:
            for (;;) {
                x = g;
                y = g;
                if (y > 7)
                    y = y - 4;
		for (int i=0; i<4; i++) {
		    for (int j=0; j<4; j++) {
                        if (ain[j] == x && ain[j] == y + i)
                            continue big;
                    }
                }
		break;
            }
	    for (int i = 0; i<4; i++) {
                ain[g] = x;
                ain[g] = y + i;
                g += 1;
            }
            break;
        }
    }

    /**
     * This was an example where our flow analysis didn't find an
     * elegant solution.  The reason is, that we try to make 
     * while(true)-loops as small as possible (you can't see the real
     * end of the loop, if it is breaked there like here).
     *
     * Look at the assembler code and you know why my Decompiler had
     * problems with this.  But the decompiler did produce compilable
     * code which produces the same assembler code.  
     *
     * The solution was, to make switches as big as possible, the whole
     * analyze methods were overworked.
     */
    void WhileTrueSwitch() {
        int i = 1;
        while (true) {
            switch (i) {
            case 0:
                return;
            case 1:
                i = 5;
                continue;
            case 2:
                i = 6;
                continue;
            case 3:
                throw new RuntimeException();
            default:
                i = 7;
		return;
            }
        }
    }

    abstract int test();

    /**
     * This tests shorts and empty ifs.  Especially the no op ifs can
     * be optimized to very unusual code.
     */
    public void shortIf() {
	while(g != 7) {
	    if (g == 5)
		return;
	    else if (g != 4)
		break;
	    else if (g == 2)
		shortIf();
	    else
		return;

	    if (g!= 7)
		shortIf();
	    else {
		shortIf();
		return;
	    }

	    if (g != 1)
		break;
	    else if (g == 3)
		shortIf();
	    else
		break;

	    // javac optimizes this instruction to 
	    //   test();
	    // jikes reproduces this statement as one would expect
	    if (g + 5 == test()) {
	    }

	    // javac -O  optimizes this to the following weired statements
	    //   PUSH g;
	    //   PUSH test();
	    //   POP2;
	    // This cannot be decompiled correctly, since the == is lost.
	    if (g == test())
		continue;
	}
	while(g == 3) {
	    // javac:
	    //   PUSH test() == 4 || test() == 3 && test() == 2;
	    //   POP;
	    if (test() == 4 || test() == 3 && test() == 2);
	    // javac -O:
	    //   if (test() != 4 && test() == 3) {
	    //     PUSH test()+test() - test();
	    //     PUSH g-4;
	    //     POP2;
	    //   }
	    if (test() == 4 || test() == 3 && test() == 2)
		continue;
	}
	while (g==2) {
	    // javac:
	    //   test();
	    //   test();
	    //   test();
	    if ((long) (test() + test() - test()) == (long)(g-4));
	    // javac -O:
	    //   PUSH (long)(test() + test() - test()) <=> (long)(g-4)
	    //   POP;
	    if ((long) (test() + test() - test()) == (long)(g-4))
		continue;
	}
	System.err.println("Hallo");
    }
}
