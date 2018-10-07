/* LocalVariableInfo Copyright (C) 1999-2002 Jochen Hoenicke.
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
 * $Id: LocalVariableInfo.java 1367 2002-05-29 12:06:47Z hoenicke $
 */

package net.sf.jode.bytecode;
import net.sf.jode.util.UnifyHash;
///#def COLLECTIONS java.util
import java.util.Iterator;
///#enddef

/**
 * A simple class containing the info of the LocalVariableTable.  This
 * info is stored decentral: every load, store or iinc instruction contains
 * the info for its local.  When writing code it will automatically be
 * collected again. <br>
 *
 * You can't modify a LocalVariableInfo, for this reason they can and
 * will be shared.<br>
 *
 * This information consists of name, type signature and slot number.
 * There is no public constructor; use the static getInfo() methods
 * instead.
 */
public final class LocalVariableInfo {
    private String name, type;
    private int slot;
    private static LocalVariableInfo anonymous[];
    static {
	grow(5);
    }
    private static final UnifyHash unifier = new UnifyHash();

    private LocalVariableInfo(int slot) {
	this.slot = slot;
    }

    private LocalVariableInfo(int slot, String name, String type) {
	this.slot = slot;
	this.name = name;
	this.type = type;
    }

    private static void grow(int upper) {
	LocalVariableInfo[] newAnon = new LocalVariableInfo[upper];
	int start = 0;
	if (anonymous != null) {
	    start = anonymous.length;
	    System.arraycopy(anonymous, 0, newAnon, 0, start);
	}
	anonymous = newAnon;
	for (int i=start; i< upper; i++)
	    anonymous[i] = new LocalVariableInfo(i);
    }

    /**
     * Creates a new local variable info, with no name or type.
     * @param slot the slot number.
     */
    public static LocalVariableInfo getInfo(int slot) {
	if (slot >= anonymous.length)
	    grow(Math.max(slot + 1, anonymous.length * 2));
	return anonymous[slot];
    }

    /**
     * Creates a new local variable info, with given name and type.
     * @param slot the slot number.
     * @param name the name of the local.
     * @param type the type signature of the local.
     */
    public static LocalVariableInfo getInfo(int slot, 
					    String name, String type) {
	if (name == null && type == null)
	    return getInfo(slot);
	int hash = slot ^ name.hashCode() ^ type.hashCode();
	Iterator iter = unifier.iterateHashCode(hash);
	while (iter.hasNext()) {
	    LocalVariableInfo lvi = (LocalVariableInfo) iter.next();
	    if (lvi.slot == slot
		&& lvi.name.equals(name)
		&& lvi.type.equals(type))
		return lvi;
	}
	LocalVariableInfo lvi = new LocalVariableInfo(slot, name, type);
	unifier.put(hash, lvi);
	return lvi;
    }
    
    /**
     * Gets the slot number.
     */
    public int getSlot() {
	return slot;
    }

    /**
     * Gets the name.
     */
    public String getName() {
	return name;
    }
    
    /**
     * Gets the type signature.
     * @see TypeSignature
     */
    public String getType() {
	return type;
    }

    /**
     * Gets a string representation for debugging purposes.
     */
    public String toString() {
	String result = ""+slot;
	if (name != null)
	    result += " ["+name+","+type+"]";
	return result;
    }
}
