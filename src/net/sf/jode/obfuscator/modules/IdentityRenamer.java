/* IdentityRenamer Copyright (C) 1999-2002 Jochen Hoenicke.
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
 * $Id: IdentityRenamer.java 1367 2002-05-29 12:06:47Z hoenicke $
 */

package net.sf.jode.obfuscator.modules;
import net.sf.jode.obfuscator.Renamer;
import net.sf.jode.obfuscator.Identifier;
///#def COLLECTIONS java.util
import java.util.Iterator;
///#enddef
///#def COLLECTIONEXTRA java.lang
import java.lang.UnsupportedOperationException;
///#enddef

public class IdentityRenamer implements Renamer {
    public Iterator generateNames(Identifier ident) {
	final String base = ident.getName();
	return new Iterator() {
	    int last = 0;
	    
	    public boolean hasNext() {
		return true;
	    }
	    
	    public Object next() {
		return (last++ == 0 ? base : base + last);
	    }
	    
	    public void remove() {
		throw new UnsupportedOperationException();
	    }
	};
    }
}

