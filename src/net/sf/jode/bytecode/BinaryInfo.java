/* BinaryInfo Copyright (C) 1998-2002 Jochen Hoenicke.
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
 * $Id: BinaryInfo.java 1390 2005-07-25 17:14:53Z hoenicke $
 */

package net.sf.jode.bytecode;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import net.sf.jode.util.SimpleMap;

///#def COLLECTIONS java.util
import java.util.Map;
import java.util.Collections;
import java.util.Iterator;
///#enddef


/**
 * <p>Represents a container for user specified attributes.</p>
 *
 * <p>Java bytecode is extensible: Classes, Methods and Fields may
 * have any number of attributes.  Every attribute has a name and some
 * unformatted data.</p>
 *
 * <p>There are some predefined attributes, even the Code of a Method
 * is an attribute.  These predefined attributes are all handled by
 * this package as appropriate.  These methods are only useful for non
 * standard attributes.</p>
 *
 * <p>You can provide new attributes by overriding the protected
 * methods of this class.  This makes it possible to use constant pool
 * entries in the attributes.</p>
 *
 * <p>Another possibility is to add the attributes with the public
 * method.  This way you don't need to extend the classes, but you
 * can't use a constant pool for the contents of the attributes.  One
 * possible application of this are installation classes.  These
 * classes have a special attribute containing a zip archive of the
 * files that should be installed.  There are other possible uses,
 * e.g.  putting native machine code for some architectures into the
 * class.</p>
 *
 * @author Jochen Hoenicke 
 */
public class BinaryInfo {
    /**
     * The bit mask representing public modifier.
     */
    public static int ACC_PUBLIC     = 0x0001;
    /**
     * The bit mask representing private modifier.
     */
    public static int ACC_PRIVATE    = 0x0002;
    /**
     * The bit mask representing protected modifier.
     */
    public static int ACC_PROTECTED  = 0x0004;
    /**
     * The bit mask representing static modifier.
     */
    public static int ACC_STATIC     = 0x0008;
    /**
     * The bit mask representing final modifier.
     */
    public static int ACC_FINAL      = 0x0010;
    /**
     * The bit mask representing the ACC_SUPER modifier for classes.
     * This is a special modifier that only has historic meaning.  Every
     * class should have this set.
     */
    public static int ACC_SUPER      = 0x0020;
    /**
     * The bit mask representing volatile modifier for fields.
     */
    public static int ACC_VOLATILE   = 0x0040;
    /**
     * The bit mask representing synthetic bridge method.  This is
     * used when a non-generic method overrides a generic method of 
     * super class/interface.
     */
    public static int ACC_BRIDGE     = 0x0040;
    /**
     * The bit mask representing transient fields.
     */
    public static int ACC_TRANSIENT  = 0x0080;
    /**
     * The bit mask representing varargs methods.
     */
    public static int ACC_VARARGS    = 0x0080;
    /**
     * The bit mask representing enumeration fields.
     */
    public static int ACC_ENUM       = 0x0100;
    /**
     * The bit mask representing native methods.
     */
    public static int ACC_NATIVE     = 0x0100;
    /**
     * The bit mask representing interfaces.
     */
    public static int ACC_INTERFACE  = 0x0200;
    /**
     * The bit mask representing abstract modifier.
     */
    public static int ACC_ABSTRACT   = 0x0400;
    /**
     * The bit mask representing annotation classes.
     */
    public static int ACC_ANNOTATION = 0x0800;
    /**
     * The bit mask representing strictfp modifier.
     */
    public static int ACC_STRICT     = 0x0800;
    /**
     * The bit mask representing synthetic fields/methods and classes.
     */
    public static int ACC_SYNTHETIC  = 0x1000;

    private Map unknownAttributes = null;

    void skipAttributes(DataInputStream input) throws IOException {
        int count = input.readUnsignedShort();
        for (int i=0; i< count; i++) {
            input.readUnsignedShort();  // the name index
            long length = input.readInt();
	    while (length > 0) {
		long skipped = input.skip(length);
		if (skipped == 0)
		    throw new EOFException("Can't skip. EOF?");
		length -= skipped;
	    }
        }
    }

    /**
     * Reads in an attributes of this class.  Overwrite this method if
     * you want to handle your own attributes.  If you don't know how
     * to handle an attribute call this method for the super class.
     * @param name the attribute name.
     * @param length the length of the attribute.
     * @param constantPool the constant pool of the class.
     * @param input a data input stream where you can read the attribute
     * from.  It will protect you to read more over the attribute boundary.
     * @param howMuch the constant that was given to the {@link
     * ClassInfo#load} function when loading this class.
     */
    protected void readAttribute(String name, int length,
				 ConstantPool constantPool,
				 DataInputStream input, 
				 int howMuch) throws IOException {
	byte[] data = new byte[length];
	input.readFully(data);
	if (howMuch >= ClassInfo.ALL) {
	    if (unknownAttributes == null)
		unknownAttributes = new SimpleMap();
	    unknownAttributes.put(name, data);
	}
    }

    static class ConstrainedInputStream extends FilterInputStream {
	int length;

	public ConstrainedInputStream(int attrLength, InputStream input) {
	    super(input);
	    length = attrLength;
	}

	public int read() throws IOException {
	    if (length > 0) {
		int data = super.read();
		length--;
		return data;
	    }
	    throw new EOFException();
	}

	public int read(byte[] b, int off, int len) throws IOException {
	    if (length < len) {
		len = length;
	    }
	    if (len == 0)
		return -1;
	    int count = super.read(b, off, len);
	    length -= count;
	    return count;
	}

	public int read(byte[] b) throws IOException {
	    return read(b, 0, b.length);
	}

	public long skip(long count) throws IOException {
	    if (length < count) {
		count = length;
	    }
	    count = super.skip(count);
	    length -= (int) count;
	    return count;
	}

	public void skipRemaining() throws IOException {
	    while (length > 0) {
		int skipped = (int) skip(length);
		if (skipped == 0)
		    throw new EOFException();
		length -= skipped;
	    }
	}
    }

    void readAttributes(ConstantPool constantPool,
			DataInputStream input, 
			int howMuch) throws IOException {
	int count = input.readUnsignedShort();
	unknownAttributes = null;
	for (int i=0; i< count; i++) {
	    String attrName = 
		constantPool.getUTF8(input.readUnsignedShort());
	    final int attrLength = input.readInt();
	    ConstrainedInputStream constrInput = 
		    new ConstrainedInputStream(attrLength, input);
	    readAttribute(attrName, attrLength, 
			  constantPool, new DataInputStream(constrInput),
			  howMuch);
	    constrInput.skipRemaining();
	}
    }

    /**
     * Drops information from this info.  Override this to drop your
     * own info and don't forget to call the method of the super class.
     * @param keep the constant representing how much information we
     * should keep (see {@link ClassInfo#load}).
     */
    protected void drop(int keep) {
	if (keep < ClassInfo.ALL)
	    unknownAttributes = null;
    }

    /**
     * Returns the number of attributes of this class.  Overwrite this
     * method if you want to add your own attributes by providing a
     * writeAttributes method.  You should call this method for the
     * super class and add the number of your own attributes to the
     * returned value.
     * @return the number of attributes of this class.
     */
    protected int getAttributeCount() {
	return unknownAttributes != null ? unknownAttributes.size() : 0;
    }

    /**
     * Prepare writing your attributes.  Overwrite this method if you
     * want to add your own attributes, which need constants on the
     * class pool.  Add the necessary constants to the constant pool
     * and call this method for the super class.
     * @param gcp The growable constant pool.
     */
    protected void prepareAttributes(GrowableConstantPool gcp) {
	if (unknownAttributes == null)
	    return;
	Iterator i = unknownAttributes.keySet().iterator();
	while (i.hasNext())
	    gcp.putUTF8((String) i.next());
    }

    /**
     * <p>Writes the attributes to the output stream.
     * Overwrite this method if you want to add your own attributes.
     * All constants you need from the growable constant pool must
     * have been previously registered by the {@link #prepareAttributes}
     * method.  This method must not add new constants to the pool</p>
     *
     * First call the method of the super class.  Afterwrites write
     * each of your own attributes including the attribute header
     * (name and length entry).
     *
     * @param constantPool The growable constant pool, which is not 
     * growable anymore (see above).
     * @param output the data output stream.  You must write exactly
     * as many bytes to it as you have told with the {@link
     * #getAttributeSize} method.
     */
    protected void writeAttributes
	(GrowableConstantPool constantPool, 
	 DataOutputStream output) throws IOException {
	int count = getAttributeCount();
	output.writeShort(count);
	if (unknownAttributes != null) {
	    Iterator i = unknownAttributes.entrySet().iterator();
	    while (i.hasNext()) {
		Map.Entry e = (Map.Entry) i.next();
		String name = (String) e.getKey();
		byte[] data = (byte[]) e.getValue();
		output.writeShort(constantPool.putUTF8(name));
		output.writeInt(data.length);
		output.write(data);
	    }
	}
    }

    /**
     * Gets the total length of all attributes in this binary info.
     * Overwrite this method if you want to add your own attributes 
     * and add the size of your attributes to the value returned by
     * the super class.<br>
     *
     * Currently you only need to write this if you extend
     * BasicBlocks.
     *
     * @return the total length of all attributes, including their
     * headers and the "number of attributes" field.
     */
    protected int getAttributeSize() {
	int size = 2; /* attribute count */
	if (unknownAttributes != null) {
	    Iterator i = unknownAttributes.values().iterator();
	    while (i.hasNext())
		size += 2 + 4 + ((byte[]) i.next()).length;
	}
	return size;
    }
    
    /**
     * Finds a non standard attribute with the given name.  You don't
     * have access to the constant pool.  If you need the pool don't
     * use this method but extend this class and override
     * readAttribute method.
     * @param name the name of the attribute.
     * @return the contents of the attribute, null if not found.
     * @see #readAttribute
     */
    public byte[] findAttribute(String name) {
	if (unknownAttributes != null)
	    return (byte[]) unknownAttributes.get(name);
	return null;
    }

    /**
     * Gets all non standard attributes.
     * @return an iterator for all attributes.  The values returned by
     * the next() method of the iterator are of Map.Entry type.  The
     * key of the entry is the name of the attribute, while the values
     * are the byte[] contents.
     * @see #findAttribute
     */
    public Iterator getAttributes() {
	if (unknownAttributes != null)
	    return unknownAttributes.entrySet().iterator();
	return Collections.EMPTY_SET.iterator();
    }

    /**
     * Adds a new non standard attribute or replaces an old one with
     * the same name.  If it already exists, it will be overwritten.
     * Note that there's now way to correlate the contents with a
     * constant pool.  If you need that extend this class and override
     * the methods {@link #getAttributeCount}, {@link
     * #prepareAttributes}, {@link #writeAttributes}, and {@link
     * #getAttributeSize}.
     * @param name the name of the attribute.
     * @param contents the new contens.
     */
    public void addAttribute(String name, byte[] contents) {
	if (unknownAttributes == null)
	    unknownAttributes = new SimpleMap();
	unknownAttributes.put(name, contents);
    }

    /**
     * Removes a non standard attributes.
     * @param name the name of the attribute.
     * @return the old contents of the attribute.
     */
    public byte[] removeAttribute(String name) {
	if (unknownAttributes != null)
	    return (byte[]) unknownAttributes.remove(name);
	return null;
    }

    /**
     * Removes all non standard attributes.
     */
    public void removeAllAttributes() {
	unknownAttributes = null;
    }
}
