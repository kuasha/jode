/* InnerCompat Copyright (C) 1999 Jochen Hoenicke.
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
 * $Id: InnerCompat.java 1149 1999-08-19 15:16:59Z jochen $
 */


public class InnerCompat {
    int x;

    private class privateNeedThis {
	void a() { x = 5; }
    }
    protected class protectedNeedThis {
	void a() { x = 5; }
    }
    class packageNeedThis {
	void a() { x = 5; }
    }
    public class publicNeedThis {
	void a() { x = 5; }
    }
    private class privateNeedNotThis {
	int x;
	void a() { x = 5; }
    }
    protected class protectedNeedNotThis {
	int x;
	void a() { x = 5; }
    }
    class packageNeedNotThis {
	int x;
	void a() { x = 5; }
    }
    public class publicNeedNotThis {
	int x;
	void a() { x = 5; }
    }

    private static class privateStatic {
	int x;
	void a() { x = 5; }
    }
    protected static class protectedStatic {
	int x;
	void a() { x = 5; }
    }
    static class packageStatic {
	int x;
	void a() { x = 5; }
    }
    public static class publicStatic {
	int x;
	void a() { x = 5; }
    }
}
