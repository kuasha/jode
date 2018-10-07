/* TypeSignature Copyright (C) 1999-2002 Jochen Hoenicke.
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
 * $Id: TypeSignature.java 1411 2012-03-01 22:39:08Z hoenicke $
 */

package net.sf.jode.bytecode;

/**
 * This class contains some static methods to handle type signatures. <br>
 *
 * A type signature is a compact textual representation of a java
 * types.  It is described in the Java Virtual Machine Specification.
 * Primitive types have a one letter type signature.  Type signature
 * of classes contains the class name. Type signatures for arrays and
 * methods are recursively build from the type signatures of their
 * elements.  <br> Since java 5 there is a new class of type
 * signatures supporting generics.  These can be accessed with the
 * getSignature methods of ClassInfo, MethodInfo and FieldInfo.
 *
 * Here are a few examples:
 * <table><tr><th>type signature</th><th>Java type</th></tr>
 * <tr><td><code>Z</code></td><td><code>boolean</code></td></tr>
 * <tr><td><code>B</code></td><td><code>byte</code></td></tr>
 * <tr><td><code>S</code></td><td><code>short</code></td></tr>
 * <tr><td><code>C</code></td><td><code>char</code></td></tr>
 * <tr><td><code>I</code></td><td><code>int</code></td></tr>
 * <tr><td><code>F</code></td><td><code>float</code></td></tr>
 * <tr><td><code>J</code></td><td><code>long</code></td></tr>
 * <tr><td><code>D</code></td><td><code>double</code></td></tr>
 * <tr><td><code>Ljava/lang/Object;</code></td>
 *     <td><code>java.lang.Object</code></td></tr>
 * <tr><td><code>[[I</code></td><td><code>int[][]</code></td></tr>
 * <tr><td><code>(Ljava/lang/Object;I)V</code></td>
 *     <td>method with argument types <code>Object</code> and
 *         <code>int</code> and <code>void</code> return type.</td></tr>
 * <tr><td><code>()I</code></td>
 *     <td> method without arguments 
 *          and <code>int</code> return type.</td></tr>
 * <tr><td colspan="2"><code>&lt;E:Ljava/lang/Object;&gt;Ljava/lang/Object;Ljava/util/Collection&lt;TE;&gt;;</code></td>
 *     </tr><tr><td></td>
 *     <td> generic class over &lt;E extends Object&gt; extending
 *          Object and implementing Collections&lt;E&gt;</td></tr>
 * <tr><td colspan="2"><code>&lt;T:Ljava/lang/Object;&gt;([TT;)[TT;</code></td>
 *     </tr><tr><td></td>
 *     <td> generic method over &lt;T extends Object&gt; taking an
 *          array of T as parameters and returning an array of T.</td></tr>
 * </table>
 *
 *
 * @author Jochen Hoenicke
 */
public class TypeSignature {
    /**
     * This is a private method for generating the signature of a
     * given type.  
     */
    private static final StringBuffer appendSignature(StringBuffer sb,
						      Class javaType) {
	if (javaType.isPrimitive()) {
	    if (javaType == Boolean.TYPE)
		return sb.append('Z');
	    else if (javaType == Byte.TYPE)
		return sb.append('B');
	    else if (javaType == Character.TYPE)
		return sb.append('C');
	    else if (javaType == Short.TYPE)
		return sb.append('S');
	    else if (javaType == Integer.TYPE)
		return sb.append('I');
	    else if (javaType == Long.TYPE)
		return sb.append('J');
	    else if (javaType == Float.TYPE)
		return sb.append('F');
	    else if (javaType == Double.TYPE)
		return sb.append('D');
	    else if (javaType == Void.TYPE)
		return sb.append('V');
	    else
		throw new InternalError("Unknown primitive type: "+javaType);
	} else if (javaType.isArray()) {
	    return appendSignature(sb.append('['), 
				   javaType.getComponentType());
	} else {
	    return sb.append('L')
		.append(javaType.getName().replace('.','/')).append(';');
	}
    }

    /**
     * Generates the type signature of the given Class.
     * @param clazz a java.lang.Class, this may also be a primitive or
     * array type.
     * @return the type signature.
     */
    public static String getSignature(Class clazz) {
	return appendSignature(new StringBuffer(), clazz).toString();
    }
 
    /**
     * Generates a method signature.
     * @param paramT the java.lang.Class of the parameter types of the method.
     * @param returnT the java.lang.Class of the return type of the method.
     * @return the method type signature
     */
    public static String getSignature(Class paramT[], Class returnT) {
	StringBuffer sig = new StringBuffer("(");
	for (int i=0; i< paramT.length; i++)
	    appendSignature(sig, paramT[i]);
	return appendSignature(sig.append(')'), returnT).toString();
    }

    /**
     * Generates a Class object for a type signature.  This is the
     * inverse function of getSignature.
     * @param typeSig a single type signature
     * @return the Class object representing that type.
     */
    public static Class getClass(String typeSig) 
	throws ClassNotFoundException 
    {
        switch(typeSig.charAt(0)) {
        case 'Z':
            return Boolean.TYPE;
        case 'B':
            return Byte.TYPE;
        case 'C':
            return Character.TYPE;
        case 'S':
            return Short.TYPE;
        case 'I':
            return Integer.TYPE;
        case 'F':
            return Float.TYPE;
        case 'J':
            return Long.TYPE;
        case 'D':
            return Double.TYPE;
        case 'V':
            return Void.TYPE;
        case 'L':
	    typeSig = typeSig.substring(1, typeSig.length()-1)
		.replace('/','.');
	    /* fall through */
        case '[':
	    return Class.forName(typeSig);
        }
        throw new IllegalArgumentException(typeSig);
    }

    /**
     * Check if the given type is a two slot type.  The only two slot 
     * types are long and double.
     */
    private static boolean usingTwoSlots(char type) {
	return "JD".indexOf(type) >= 0;
    }

    /**
     * Returns the number of words, an object of the given simple type
     * signature takes.  For long and double this is two, for all other
     * types it is one.
     */
    public static int getTypeSize(String typeSig) {
	return usingTwoSlots(typeSig.charAt(0)) ? 2 : 1;
    }

    /**
     * Gets the element type of an array.  
     * @param typeSig type signature of the array.
     * @return type signature for the element type.
     * @exception IllegalArgumentException if typeSig is not an array
     * type signature.
     */
    public static String getElementType(String typeSig) {
	if (typeSig.charAt(0) != '[')
	    throw new IllegalArgumentException();
	return typeSig.substring(1);
    }

    /**
     * Gets the ClassInfo for a class type.
     * @param classpath the classpath in which the ClassInfo is searched.
     * @param typeSig type signature of the class.
     * @return the ClassInfo object for the class.
     * @exception IllegalArgumentException if typeSig is not an class
     * type signature.
     */
    public static ClassInfo getClassInfo(ClassPath classpath, String typeSig) {
	if (typeSig.charAt(0) != 'L')
	    throw new IllegalArgumentException();
	return classpath.getClassInfo
	    (typeSig.substring(1, typeSig.length()-1).replace('/', '.'));
    }

    /**
     * Skips the next entry of a method type signature
     * @param methodTypeSig type signature of the method.
     * @param position the index to the last entry.
     * @return the index to the next entry.
     */
    static int skipType(String methodTypeSig, int position) {
	char c = methodTypeSig.charAt(position++);
	while (c == '[')
	    c = methodTypeSig.charAt(position++);
	if (c == 'L')
	    return methodTypeSig.indexOf(';', position) + 1;
	return position;
    }
    
    /**
     * Gets the number of words the parameters for the given method
     * type signature takes.  This is the sum of getTypeSize() for
     * each parameter type.
     * @param methodTypeSig the method type signature.
     * @return the number of words the parameters take.
     */
    public static int getParameterSize(String methodTypeSig) {
	int nargs = 0;
	int i = 1;
	for (;;) {
	    char c = methodTypeSig.charAt(i);
	    if (c == ')')
		return nargs;
	    i = skipType(methodTypeSig, i);
	    if (usingTwoSlots(c))
		nargs += 2;
	    else 
		nargs++;
	}
    }

    /**
     * Gets the size of the return type of the given method in words.
     * This is zero for void return type, two for double or long return
     * type and one otherwise.
     * @param methodTypeSig the method type signature.
     * @return the size of the return type in words.
     */
    public static int getReturnSize(String methodTypeSig) {
	int length = methodTypeSig.length();
	if (methodTypeSig.charAt(length - 2) == ')') {
	    // This is a single character return type.
	    char returnType = methodTypeSig.charAt(length - 1);
	    return returnType == 'V' ? 0 
		: usingTwoSlots(returnType) ? 2 : 1;
	} else
	    // All multi character return types take one parameter
	    return 1;
    }

    /**
     * Gets the parameter type signatures of the given method signature.
     * @param methodTypeSig the method type signature.
     * @return an array containing all parameter types in correct order.
     */
    public static String[] getParameterTypes(String methodTypeSig) {
	int pos = 1;
	int count = 0;
	while (methodTypeSig.charAt(pos) != ')') {
	    pos = skipType(methodTypeSig, pos);
	    count++;
	}
	String[] params = new String[count];
	pos = 1;
	for (int i = 0; i < count; i++) {
	    int start = pos;
	    pos = skipType(methodTypeSig, pos);
	    params[i] = methodTypeSig.substring(start, pos);
	}
	return params;
    }

    /**
     * Gets the return type for a method signature
     * @param methodTypeSig the method signature.
     * @return the return type for a method signature, `V' for void methods.
     */
    public static String getReturnType(String methodTypeSig) {
	return methodTypeSig.substring(methodTypeSig.lastIndexOf(')')+1);
    }

    /**
     * Gets the default value an object of the given type has.  It is
     * null for objects and arrays, Integer(0) for boolean and short
     * integer types or Long(0L), Double(0.0), Float(0.0F) for long,
     * double and float.  This seems strange, but this way the type
     * returned is the same as for FieldInfo.getConstant().
     *
     * @param typeSig the type signature.
     * @return the default value.
     * @exception IllegalArgumentException if this is a method type signature.
     */
    public static Object getDefaultValue(String typeSig) {
	switch(typeSig.charAt(0)) {
	case 'Z':
	case 'B':
	case 'S':
	case 'C':
	case 'I':
	    return new Integer(0);
	case 'J':
	    return new Long(0L);
	case 'D':
	    return new Double(0.0);
	case 'F':
	    return new Float(0.0F);
	case 'L':
	case '[':
	    return null;
	default:
	    throw new IllegalArgumentException(typeSig);
	}
    }

    /**
     * Checks if there is a valid class name starting at index
     * in string typesig and ending with a semicolon.
     * @return the index at which the class name ends.
     * @exception IllegalArgumentException if there was an illegal character.
     * @exception StringIndexOutOfBoundsException if the typesig ended early.
     */
    private static int checkClassName(String clName, int i) 
	throws IllegalArgumentException, StringIndexOutOfBoundsException 
    {
	while (true) {
	    char c = clName.charAt(i++);
	    if (c == ';')
		return i;
	    if (c != '/' && !Character.isJavaIdentifierPart(c))
		throw new IllegalArgumentException("Illegal java class name: "
						   + clName);
	}
    }

    /**
     * Checks if there is a valid simple type signature starting at index
     * in string typesig.
     * @return the index at which the type signature ends.
     * @exception IllegalArgumentException if there was an illegal character.
     * @exception StringIndexOutOfBoundsException if the typesig ended early.
     */
    private static int checkTypeSig(String typesig, int index) {
	char c = typesig.charAt(index++);
	while (c == '[')
	    c = typesig.charAt(index++);
	if (c == 'L') {
	    index = checkClassName(typesig, index);
	} else {
	    if ("ZBSCIJFD".indexOf(c) == -1)
		throw new IllegalArgumentException("Type sig error: "+typesig);
	}
	return index;
    }

    /**
     * Checks whether a given type signature is a valid (not method)
     * type signature.  Throws an exception otherwise.
     * @param typesig the type signature.
     * @exception NullPointerException if typeSig is null.
     * @exception IllegalArgumentException if typeSig is not a valid
     * type signature or if it's a method type signature.
     */
    public static void checkTypeSig(String typesig) 
	throws IllegalArgumentException
    {
	try {
	    if (checkTypeSig(typesig, 0) != typesig.length())
		throw new IllegalArgumentException
		    ("Type sig too long: "+typesig);
	} catch (StringIndexOutOfBoundsException ex) {
	    throw new IllegalArgumentException
		("Incomplete type sig: "+typesig);
	}
    }

    /**
     * Checks whether a given type signature is a valid method
     * type signature.  Throws an exception otherwise.
     * @param typesig the type signature.
     * @exception NullPointerException if typeSig is null.
     * @exception IllegalArgumentException if typeSig is not a valid
     * method type signature.
     */
    public static void checkMethodTypeSig(String typesig) 
	throws IllegalArgumentException
    {
	try {
	    if (typesig.charAt(0) != '(')
		throw new IllegalArgumentException
		    ("No method signature: "+typesig);
	    int i = 1;
	    while (typesig.charAt(i) != ')')
		i = checkTypeSig(typesig, i);
	    // skip closing parenthesis.
	    i++;
	    if (typesig.charAt(i) == 'V')
		// accept void return type.
		i++;
	    else
		i = checkTypeSig(typesig, i);
	    if (i != typesig.length())
		throw new IllegalArgumentException
		    ("Type sig too long: "+typesig);
	} catch (StringIndexOutOfBoundsException ex) {
	    throw new IllegalArgumentException
		("Incomplete type sig: "+typesig);
	}
    }
}

