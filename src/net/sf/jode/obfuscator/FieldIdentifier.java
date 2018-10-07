/* FieldIdentifier Copyright (C) 1999-2002 Jochen Hoenicke.
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
 * $Id: FieldIdentifier.java 1411 2012-03-01 22:39:08Z hoenicke $
 */

package net.sf.jode.obfuscator;
import net.sf.jode.bytecode.*;
///#def COLLECTIONS java.util
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.HashSet;
///#enddef


public class FieldIdentifier extends Identifier{
    FieldInfo info;
    ClassIdentifier clazz;
    String name;
    String type;
    /**
     * This field tells if the value is not constant.  It is initially
     * set to false, and if a write to that field is found, it is set
     * to true.
     */
    private boolean notConstant;
    private Object constant;

    /**
     * The FieldChangeListener that should be notified if a 
     * write to this field is found.
     */
    private Collection fieldListeners;

    public FieldIdentifier(ClassIdentifier clazz, FieldInfo info) {
	super(info.getName());
	this.name = info.getName();
	this.type = info.getType();
	this.info = info;
	this.clazz = clazz;
	this.constant = info.getConstant();
    }

    public void setSingleReachable() {
	super.setSingleReachable();
	Main.getClassBundle().analyzeIdentifier(this);
    }
    
    public void setSinglePreserved() {
	super.setSinglePreserved();
	setNotConstant();
    }
    
    public void analyze() {
	String type = getType();
	int index = type.indexOf('L');
	if (index != -1) {
	    int end = type.indexOf(';', index);
	    Main.getClassBundle().reachableClass
		(type.substring(index+1, end).replace('/', '.'));
	}
    }

    public Identifier getParent() {
	return clazz;
    }

    public String getFullName() {
	return clazz.getFullName() + "." + getName() + "." + getType();
    }

    public String getFullAlias() {
	return clazz.getFullAlias() + "." + getAlias() + "."
	    + Main.getClassBundle().getTypeAlias(getType());
    }

    public String getName() {
	return name;
    }

    public String getType() {
	return type;
    }
    
    public int getModifiers() {
	return info.getModifiers();
    }

    public Iterator getChilds() {
	return Collections.EMPTY_LIST.iterator();
    }

    public boolean isNotConstant() {
	return notConstant;
    }
    
    public Object getConstant() {
	return constant;
    }
    
    public void addFieldListener(Identifier ident) {
	if (ident == null)
	    throw new NullPointerException();
	if (fieldListeners == null)
	    fieldListeners = new HashSet();
	if (!fieldListeners.contains(ident))
	    fieldListeners.add(ident);
    }

    public void removeFieldListener(Identifier ident) {
	if (fieldListeners != null)
	    fieldListeners.remove(ident);
    }

    public void setNotConstant() {
	if (notConstant)
	    return;

	notConstant = true;
	if (fieldListeners == null)
	    return;

	for (Iterator i = fieldListeners.iterator(); i.hasNext(); )
	    Main.getClassBundle().analyzeIdentifier((Identifier) i.next());
	fieldListeners = null;
    }

    public String toString() {
	return "FieldIdentifier "+getFullName();
    }

    public boolean conflicting(String newAlias) {
	return clazz.fieldConflicts(this, newAlias);
    }

    public void doTransformations() {
	info.setName(getAlias());
	info.setType(Main.getClassBundle().getTypeAlias(type));
    }
}
