/* TryCatch Copyright (C) 1998-1999 Jochen Hoenicke.
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
 * $Id: TryCatch.java 1414 2012-03-02 11:20:15Z hoenicke $
 */


/**
 * This tests everything that has to do with a ExceptionHandler, that
 * is try, catch, finally and synchronized.
 */
class TryCatch {
    void verysimple() {
	try {
            foo();
        } catch (RuntimeException ex) {
            ex.printStackTrace();
	}
    }
    int simple() {
        TryCatch i = null;
        try {
            foo();
        } catch (RuntimeException ex) {
            ex.printStackTrace();
        } catch (Exception ex) {
            return 0;
        } finally {
            simple();
        }
        synchronized(this) {
            System.gc();
            if (i != null)
                return -1;
        }
        synchronized(i) {
            foo();
        }
        return 1;
    }

    int localsInCatch() {
        int a = 0;
        try {
            a = 1;
            foo();
            a = 2;
            simple();
	    a = (a=4) / (a=0);
            a = 3;
            localsInCatch();
            a = 4;
            return 5;
        } catch (Exception ex) {
            return a;
        }
    }

    int finallyBreaks() {
        try {
            simple();
            throw new Exception();
        } catch (Exception ex) {
            return 3;
        } finally {
            simple();
            return 3;
        }
    }

    int whileInTry() {
        int a=1;
        try {
            while (true) {
                a= 4;
                whileInTry();
            }
        } finally {
            synchronized(this) {
                while (true) {
                    foo();
                    if (a == 5)
                        break;
                    finallyBreaks();
                }
            } 
            return a;
        }
    }

    void foo() {
        TryCatch local = null;
        while (true) {
            foo();
            synchronized (this) {
                System.gc();
                try {
                    System.err.println();
                } catch (RuntimeException ex) {
                    ex.printStackTrace();
                    for (int i=0; i<4; i++)
                        foo();
                    break;
                } finally {
                    System.out.println();
                    for (int i=0; i<4; i++)
                        foo();
                    System.out.println();
                }
            }
        }
        synchronized (local) {
            local.foo();
        }
        if (simple() == 0) {
            synchronized (this) {
                try {
                    System.err.println();
                } finally {
                    Thread.dumpStack();
                    return;
                }
            }
        }
        System.out.println();
    }

    void oneEntryFinally() {
        try {
            throw new RuntimeException("ex");
        } finally {
            System.err.println("hallo");
        }
    }

    void finallyMayBreak() {
        while(simple() > 3) {
            try {
                System.err.println();
            } finally {
                if (simple() < 2)
                    break;
                else if (simple() < 3)
                    foo();
		else
		    return;
            }
            System.out.println();
        }
        System.err.println();
    }
}
