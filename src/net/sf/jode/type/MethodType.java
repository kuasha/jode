/* MethodType Copyright (C) 1998-2002 Jochen Hoenicke.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; see the file COPYING.LESSER.  If not, write to
 * the Free Software Foundation, 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 * $Id: MethodType.java 1380 2004-06-01 08:46:10Z hoenicke $
 */

package net.sf.jode.type;
import net.sf.jode.bytecode.ClassPath;

/** 
 * This type represents an method type.
 *
 * @author Jochen Hoenicke 
 */
public class MethodType extends Type {
    final String signature;
    final Type[] parameterTypes;
    final Type returnType;
    final ClassPath cp;

    public MethodType(ClassPath cp, String signature) {
	super(TC_METHOD);
	this.cp = cp;
        this.signature = signature;
        int index = 1, types = 0;
        while (signature.charAt(index) != ')') {
            types++;
            while (signature.charAt(index) == '[')
                index++;
            if (signature.charAt(index) == 'L')
                index = signature.indexOf(';', index);
            index++;
        }
        parameterTypes = new Type[types];

        index = 1;
        types = 0;
        while (signature.charAt(index) != ')') {
            int lastindex = index;
            while (signature.charAt(index) == '[')
                index++;
            if (signature.charAt(index) == 'L')
                index = signature.indexOf(';', index);
            index++;
            parameterTypes[types++] 
                = Type.tType(cp, signature.substring(lastindex,index));
        }
        returnType = Type.tType(cp, signature.substring(index+1));
    }

    public final int stackSize() {
	int size = returnType.stackSize();
	for (int i=0; i<parameterTypes.length; i++)
	    size -= parameterTypes[i].stackSize();
	return size;
    }

    public ClassPath getClassPath() {
	return cp;
    }

    public Type[] getParameterTypes() {
        return parameterTypes;
    }

    public Class[] getParameterClasses() throws ClassNotFoundException {
	Class[] paramClasses = new Class[parameterTypes.length];
	for (int i = paramClasses.length; --i >= 0; )
	    paramClasses[i] = parameterTypes[i].getTypeClass();
        return paramClasses;
    }

    public Type getReturnType() {
        return returnType;
    }

    public Class getReturnClass() throws ClassNotFoundException {
	return returnType.getTypeClass();
    }

    public String getTypeSignature() {
        return signature;
    }

    public String toString() {
        return signature;
    }
}
