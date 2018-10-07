/* Options Copyright (C) 1998-2002 Jochen Hoenicke.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; see the file COPYING.LESSER.  If not, write to
 * the Free Software Foundation, 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 * $Id: Options.java 1411 2012-03-01 22:39:08Z hoenicke $
 */

package net.sf.jode.decompiler;
import net.sf.jode.bytecode.ClassInfo;
import java.io.IOException;

public class Options {
    public static final int OPTION_LVT       = 0x0001;
    public static final int OPTION_INNER     = 0x0002;
    public static final int OPTION_ANON      = 0x0004;
    public static final int OPTION_PUSH      = 0x0008;
    public static final int OPTION_PRETTY    = 0x0010;
    public static final int OPTION_DECRYPT   = 0x0020;
    public static final int OPTION_ONETIME   = 0x0040;
    public static final int OPTION_IMMEDIATE = 0x0080;
    public static final int OPTION_VERIFY    = 0x0100;
    public static final int OPTION_CONTRAFO  = 0x0200;

    public static int options = 
	OPTION_LVT | OPTION_INNER | OPTION_ANON | OPTION_PRETTY |
	OPTION_DECRYPT | OPTION_VERIFY | OPTION_CONTRAFO | OPTION_PUSH;

    public final static boolean doAnonymous() {
	return (options & OPTION_ANON) != 0;
    }

    public final static boolean doInner() {
	return (options & OPTION_INNER) != 0;
    }

    public static boolean skipClass(ClassInfo clazz) {
	if (!doInner() && !doAnonymous())
	    return false;
	try {
	    clazz.load(ClassInfo.OUTERCLASS);
	} catch (IOException ex) {
	    return false;
	}
	return (doInner() && clazz.getOuterClass() != null
		|| doAnonymous() && clazz.isMethodScoped());
    }
}
