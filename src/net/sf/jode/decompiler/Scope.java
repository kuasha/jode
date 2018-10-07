/* Scope Copyright (C) 1998-2002 Jochen Hoenicke.
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
 * $Id: Scope.java 1367 2002-05-29 12:06:47Z hoenicke $
 */

package net.sf.jode.decompiler;

/**
 * This interface describes a scope.  The basic scopes are: the package
 * scope, the class scope (one more for each inner class) and the method
 * scope.
 *
 * @author Jochen Hoenicke
 */
public interface Scope {
    public final int PACKAGENAME   = 0;
    public final int CLASSNAME     = 1;
    public final int METHODNAME    = 2;
    public final int FIELDNAME     = 3;
    public final int AMBIGUOUSNAME = 4;
    public final int LOCALNAME     = 5;

    public final int NOSUPERMETHODNAME = 12;
    public final int NOSUPERFIELDNAME  = 13;

    /**
     * Tells that we want to allow a classanalyzer as scope.
     */
    public final int CLASSSCOPE    = 1;
    /**
     * Tells that we want to allow a methodanalyzer as scope.
     */
    public final int METHODSCOPE   = 2;

    /**
     * Tells if this is the scope of the given object, which is of
     * scopeType.
     * @param object the object for which the scope
     * @param usageType either CLASSCOPE or METHODSCOPE
     * @return true if the given object is in this scope.
     */
    public boolean isScopeOf(Object object, int scopeType);
    public boolean conflicts(String name, int usageType);
}


