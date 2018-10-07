/* NameSwapper Copyright (C) 1999-2002 Jochen Hoenicke.
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
 * $Id: NameSwapper.java 1412 2012-03-01 22:52:08Z hoenicke $
 */

package net.sf.jode.obfuscator.modules;
import net.sf.jode.obfuscator.*;

///#def COLLECTIONS java.util
import java.util.Collection;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
///#enddef
///#def COLLECTIONEXTRA java.lang
import java.lang.UnsupportedOperationException;
///#enddef


public class NameSwapper implements Renamer {
    private Random rand;
    private Set packs, clazzes, methods, fields, locals;

    public NameSwapper(boolean swapAll, long seed) {
	if (swapAll) {
	    packs = clazzes = methods = fields = locals = new HashSet();
	} else {
	    packs = new HashSet();
	    clazzes = new HashSet();
	    methods = new HashSet();
	    fields = new HashSet();
	    locals = new HashSet();
	}
    }

    public NameSwapper(boolean swapAll) {
	this(swapAll, System.currentTimeMillis());
    }

    private class NameGenerator implements Iterator {
	Collection pool;
	
	NameGenerator(Collection c) {
	    pool = c;
	}

	public boolean hasNext() {
	    return true;
	}

	public Object next() {
	    int pos = rand.nextInt(pool.size());
	    Iterator i = pool.iterator();
	    while (pos > 0)
		i.next();
	    return i.next();
	}

	public void remove() {
	    throw new UnsupportedOperationException();
	}
    }

    public final Collection getCollection(Identifier ident) {
	if (ident instanceof PackageIdentifier)
	    return packs;
	else if (ident instanceof ClassIdentifier)
	    return clazzes;
	else if (ident instanceof MethodIdentifier)
	    return methods;
	else if (ident instanceof FieldIdentifier)
	    return fields;
	else if (ident instanceof LocalIdentifier)
	    return locals;
	else
	    throw new IllegalArgumentException(ident.getClass().getName());
    }

    public final void addIdentifierName(Identifier ident) {
	getCollection(ident).add(ident.getName());
    }

    public Iterator generateNames(Identifier ident) {
	return new NameGenerator(getCollection(ident));
    }
}


