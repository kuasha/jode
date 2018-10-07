/* ClassPath Copyright (C) 1998-2002 Jochen Hoenicke.
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
 * $Id: ClassPath.java 1411 2012-03-01 22:39:08Z hoenicke $
 */

package net.sf.jode.bytecode;

import java.io.ByteArrayInputStream;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import java.util.Enumeration;
import java.util.NoSuchElementException;
import java.util.Hashtable;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

///#def COLLECTIONS java.util
import java.util.Iterator;
///#enddef

import net.sf.jode.GlobalOptions;
import net.sf.jode.util.UnifyHash;

/**
 * A path in which class files are searched for.  
 *
 * Class files can be loaded from several different locations, these
 * locations can be:
 * <ul>
 * <li> A local directory.</li>
 * <li> A local jar or zip file </li>
 * <li> A URL (unified resource location), pointing to a directory </li>
 * <li> A URL pointing to a jar or zip file. </li>
 * <li> A Jar URL (see {@link java.net.JarURLConnection}), useful if
 * the jar file is not packed correctly.</li>
 * <li> The reflection URL <code>reflection:/</code>.  This is a
 * special location, which fills the ClassInfo with the information
 * from the java reflection API.  Obviously it can't load any files
 * nor the full bytecode. It only loads declarations of classes.  If a
 * security manager is present, it can only load public
 * declarations. </li>
 * </ul>
 *
 * We use standard java means to find a class file: package correspond
 * to directories and the class file must have the <code>.class</code>
 * extension.  For example if the class path points to
 * <code>/home/java</code>, the class <code>java.lang.Object</code> is
 * loaded from <code>/home/java/java/lang/Object.class</code>.  Of course
 * you can write your own {@link Location}s that break this rule.
 *
 * A class path can have another ClassPath as fallback.  If
 * ClassInfo.loadInfo is called and the class isn't found the fallback
 * ClassPath is searched instead.  This repeats until there is no
 * further fallback.  The fallback is not used when listing classes or
 * files.
 *
 * The main method for creating classes is {@link #getClassInfo}.  The
 * other available methods are useful to find other files in the
 * ClassPath and to get a listing of all available files and classes.
 *
 * A ClassPath handles some <code>IOException</code>s and
 * <code>SecurityException</code>s skipping the path that produced
 * them.
 * 
 * @author Jochen Hoenicke
 * @version 1.1 
 */
public class ClassPath  {

    /**
     * We need a different pathSeparatorChar, since ':' (used for UNIX
     * systems) is also used as protocol separator in URLs. <br>
     *
     * We currently allow both pathSeparatorChar and
     * altPathSeparatorChar and decide if it is a protocol separator
     * by context.  This doesn't always work, so use
     * <code>altPathSeparator</code>, or better yet the
     * ClassPath(String[]) or ClassPath(Location[]) constructors.
     */
    public static final char altPathSeparatorChar = ',';

    /**
     * A location is a single component of the ClassPath.  It provides
     * methods to find files, list all files and reading them. <br>
     *
     * Files and directories are always separated by "/" in this class,
     * even under Windows where the default is a "\".  This behaviour
     * is consistent with that of {@link ZipFile}. <br>
     *
     * You can extend this class to provide your own custom locations.
     */
    public static class Location {
	/**
	 * Tells whether there exists a file or directory with
	 * the given name at this location. <br>
	 * The default implementation returns false.
	 * @param file the name of the file, directories are always
	 * separated by "/".
	 * @return true if a file exists at this location.
	 */
	protected boolean exists(String file) {
	    return false;
	}

	/**
	 * Tells whether there exists a directory (or package) with
	 * the given name at this location. <br>
	 * The default implementation returns false.
	 * @param file the name of the directory, subdirectories are always
	 * separated by "/".
	 * @return true if a file exists at this location.
	 */
	protected boolean isDirectory(String file) {
	    return false;
	}

	/**
	 * Returns an input stream that reads the given file.  It is only
	 * called for files for which exists returns true. <br>
	 * The default implementation returns null.
	 * @param file the name of the file, subdirectories are always
	 * separated by "/".
	 * @return an input stream for the given file, or null if file
	 * was not found.
	 * @exception IOException if an io exception occured while opening
	 * the file.
	 */
	protected InputStream getFile(String file) throws IOException {
	    return null;
	}

	/**
	 * Lists the files and subdirectory in a directory.  This is
	 * only called for directories for which isDirectory returns
	 * true.  <br>
	 *
	 * The objects returned by the nextElement()
	 * method of the Enumeration should be of type String and
	 * contain the file resp directory name without any parent
	 * directory names. <br>
	 *
	 * Note that this method is also used by
	 * {@link ClassPath#listClassesAndPackages}. <br>
	 *
	 * The default implementation returns null, which is equivalent
	 * to an empty enumeration.
	 *
	 * @param directory the name of the directory, subdirectories
	 * are always separated by "/".
	 * @return an enumeration, listing the file names. It may
	 * return null instead of an empty enumeration.
	 */
	protected Enumeration listFiles(String directory) {
	    return null;
	}

	/**
	 * Loads a class from this location and fills it with the given
	 * information. <br>
	 * The default implementation will get the corresponding ".class"
	 * file via getFile() and fill the information from the stream.
	 * So normally there is no need to override this method.
	 * <br>
	 *	 
	 * If you want to build classes on the fly, for example if you
	 * wrote a parser for java files and want to build class files
	 * from them, you can override this method.
	 *
	 * @param clazz the dot separated full qualified class name.
	 * @param howMuch the amount of information to load
	 * @return true, if loading the class was successful, false 
	 * if it was not found.
	 * @exception ClassFormatException if class format is illegal
	 * @exception IOException if an io exception occured while reading
	 * the class.
	 * @see ClassInfo#read
	 */
	protected boolean loadClass(ClassInfo clazz, int howMuch) 
	    throws IOException, ClassFormatException 
	{
	    String file = clazz.getName().replace('.', '/') + ".class";
	    if (!exists(file))
		return false;
	    DataInputStream input = new DataInputStream
		(new BufferedInputStream(getFile(file)));
	    clazz.read(input, howMuch);
	    return true;
	}
    }

    private static class ReflectionLocation extends Location {
	protected boolean loadClass(ClassInfo classinfo, int howMuch) 
	    throws IOException, ClassFormatException 
	{
	    if (howMuch > ClassInfo.DECLARATIONS)
		return false;

	    Class clazz = null;
	    try {
		clazz = Class.forName(classinfo.getName());
	    } catch (ClassNotFoundException ex) {
		return false;
	    } catch (NoClassDefFoundError ex) {
		return false;
	    }
	    try {
		classinfo.loadFromReflection(clazz, howMuch);
		return true;
	    } catch (SecurityException ex) {
		return false;
	    }
	}

	public String toString() {
	    return "reflection:";
	}
    }

    private static class LocalLocation extends Location {
	private File dir;

	protected LocalLocation(File path) {
	    dir = path;
	}

	protected boolean exists(String filename) {
	    if (java.io.File.separatorChar != '/')
		filename = filename
		    .replace('/', java.io.File.separatorChar);
	    try {
		return new File(dir, filename).exists();
	    } catch (SecurityException ex) {
		return false;
	    }
	}

	protected boolean isDirectory(String filename) {
	    if (java.io.File.separatorChar != '/')
		filename = filename
		    .replace('/', java.io.File.separatorChar);
	    return new File(dir, filename).isDirectory();
	}

	protected InputStream getFile(String filename) throws IOException {
	    if (java.io.File.separatorChar != '/')
		filename = filename
		    .replace('/', java.io.File.separatorChar);
	    File f = new File(dir, filename);
	    return new FileInputStream(f);
	}

	protected Enumeration listFiles(String directory) {
	    if (File.separatorChar != '/')
		directory = directory
		    .replace('/', File.separatorChar);
	    File f = new File(dir, directory);
	    final String[] files = f.list();
	    if (files == null)
		return null;

	    if (!directory.endsWith(File.separator))
		directory += File.separator;
	    return new Enumeration() {
		int i = 0;
		public boolean hasMoreElements() {
		    return i < files.length;
		}
		public Object nextElement() {
		    try {
			return files[i++];
		    } catch (ArrayIndexOutOfBoundsException ex) {
			return new NoSuchElementException();
		    }
		}
	    };
	}

	public String toString() {
	    return dir.getName();
	}
    }

    private static class ZipLocation extends Location {
	private Hashtable entries = new Hashtable();
	private ZipFile file;
	private byte[] contents;
	private String prefix;

	private void addEntry(ZipEntry ze) {
	    String name = ze.getName();
	    if (prefix != null) {
		if (!name.startsWith(prefix))
		    return;
		name = name.substring(prefix.length());
	    }
	    
	    if (ze.isDirectory()
		/* || !name.endsWith(".class")*/)
		return;

	    do {
		String dir = "";
		int pathsep = name.lastIndexOf("/");
		if (pathsep != -1) {
		    dir = name.substring(0, pathsep);
		    name = name.substring(pathsep+1);
		}
		
		Vector dirContent = (Vector) entries.get(dir);
		if (dirContent != null) {
		    dirContent.addElement(name);
		    return;
		}
		    
		dirContent = new Vector();
		dirContent.addElement(name);
		entries.put(dir, dirContent);
		name = dir;
	    } while (name.length() > 0);
	}

	ZipLocation(ZipFile zipfile, String prefix) {
	    this.file = zipfile;
	    this.prefix = prefix;

	    Enumeration zipEnum = file.entries();
	    entries = new Hashtable();
	    while (zipEnum.hasMoreElements()) {
		addEntry((ZipEntry) zipEnum.nextElement());
	    }
	}

	ZipLocation(byte[] zipcontents, String prefix) 
	    throws IOException
	{
	    this.contents = zipcontents;
	    this.prefix = prefix;

	    // fill entries into hash table
	    ZipInputStream zis = new ZipInputStream
		(new ByteArrayInputStream(zipcontents));
	    entries = new Hashtable();
	    ZipEntry ze;
	    while ((ze = zis.getNextEntry()) != null) {
		addEntry(ze);
		zis.closeEntry();
	    }
	    zis.close();
	}

	protected boolean exists(String filename) {
	    if (entries.containsKey(filename))
		return true;

	    String dir = "";
	    String name = filename;
	    int index = filename.lastIndexOf('/');
	    if (index >= 0) {
		dir = filename.substring(0, index);
		name = filename.substring(index+1);
	    }
	    Vector directory = (Vector)entries.get(dir);
	    if (directory != null && directory.contains(name))
		return true;
	    return false;
	}

	protected boolean isDirectory(String filename) {
	    return entries.containsKey(filename);
	}

	protected InputStream getFile(String filename) throws IOException {
	    String fullname = prefix != null ? prefix + filename : filename;
	    if (contents != null) {
		ZipInputStream zis = new ZipInputStream
		    (new ByteArrayInputStream(contents));
		ZipEntry ze;
		while ((ze = zis.getNextEntry()) != null) {
		    if (ze.getName().equals(fullname)) {
///#ifdef JDK11
///			// The skip method in jdk1.1.7 ZipInputStream
///			// is buggy.  We return a wrapper that fixes
///			// this.
///			return new FilterInputStream(zis) {
///			    private byte[] tmpbuf = new byte[512];
///			    public long skip(long n) throws IOException {
///				long skipped = 0;
///				while (n > 0) {
///				    int count = read(tmpbuf, 0, 
///						     (int)Math.min(n, 512L));
///				    if (count == -1)
///					return skipped;
///				    skipped += count;
///				    n -= count;
///				}
///				return skipped;
///			    }
///			};
///#else
			return zis;
///#endif
		    }
		    zis.closeEntry();
		}
		zis.close();
	    } else {
                ZipEntry ze = file.getEntry(fullname);
                if (ze != null)
                    return file.getInputStream(ze);
	    }
	    return null;
	}

	protected Enumeration listFiles(String directory) {
	    Vector direntries = (Vector) entries.get(directory);
	    if (direntries != null)
		return direntries.elements();
	    return null;
	}

	public String toString() {
	    return file.getName();
	}
    }

    private static class URLLocation extends Location {
	private URL base;

	public URLLocation(URL base) {
	    this.base = base;
	}

	protected boolean exists(String filename) {
	    try {
		URL url = new URL(base, filename);
		URLConnection conn = url.openConnection();
		conn.connect();
		conn.getInputStream().close();
		return true;
	    } catch (IOException ex) {
		return false;
	    }
	}

	protected InputStream getFile(String filename) throws IOException {
	    try {
		URL url = new URL(base, filename);
		URLConnection conn = url.openConnection();
		conn.setAllowUserInteraction(true);
		return conn.getInputStream();
	    } catch (IOException ex) {
		return null;
	    }
	}

	protected boolean loadClass(ClassInfo clazz, int howMuch) 
	    throws IOException, ClassFormatException 
	{
	    /* We override this method to avoid the costs of the
	     * exists call, and to optimize howMuch.
	     */
	    String file = clazz.getName().replace('.', '/') + ".class";
	    InputStream is = getFile(file);
	    if (is == null)
		return false;

	    DataInputStream input = new DataInputStream
		(new BufferedInputStream(is));

	    /* Reading an URL may be expensive.  Therefore we ignore
	     * howMuch and read everything to avoid reading it again.
	     */
	    clazz.read(input, ClassInfo.ALL);
	    return true;
	}

	public String toString() {
	    return base.toString();
	}
    }

    private Location[] paths;
    private UnifyHash classes = new UnifyHash();
    
    ClassPath fallback = null;

    /**
     * Creates a new class path for the given path.  See the class
     * description for more information, which kind of paths are
     * supported. When a class or a file is not found in the class
     * path the fallback is used.
     * @param paths An array of paths.
     * @param fallback The fallback classpath.
     */
    public ClassPath(String[] paths, ClassPath fallback) {
	this.fallback = fallback;
	initPath(paths);
    }

    /**
     * Creates a new class path for the given path.  See the class
     * description for more information, which kind of paths are
     * supported.
     * @param paths An array of paths.
     */
    public ClassPath(String[] paths) {
	initPath(paths);
    }

    /**
     * Creates a new class path for the given path.  When a class
     * or a file is not found in the class path the fallback is used.
     * @param locs An array of locations.
     * @param fallback The fallback classpath.
     */
    public ClassPath(Location[] locs, ClassPath fallback) {
	this.fallback = fallback;
	paths = locs;
    }

    /**
     * Creates a new class path for the given path.  
     * @param locs An array of locations.
     */
    public ClassPath(Location[] locs) {
	paths = locs;
    }

    /**
     * Creates a new class path for the given path.  See the class
     * description for more information, which kind of paths are
     * supported.
     * @param path One or more paths.  They should be separated by the
     * altPathSeparatorChar or pathSeparatorChar, but the latter is
     * deprecated since it may give problems for UNIX machines.  
     * @see #ClassPath(String[] paths)
     */
    public ClassPath(String path, ClassPath fallback) {
	this.fallback = fallback;
	initPath(tokenizeClassPath(path));
    }

    /**
     * Creates a new class path for the given path.  See the class
     * description for more information, which kind of paths are
     * supported.
     * @param path One or more paths.  They should be separated by the
     * altPathSeparatorChar or pathSeparatorChar, but the latter is
     * deprecated since it may give problems for UNIX machines.  
     * @see #ClassPath(String[] paths)
     */
    public ClassPath(String path) {
	initPath(tokenizeClassPath(path));
    }

    /**
     * Creates a location for a given path component.  See the
     * class comment which path components are supported.
     * @param path the path component.
     * @return a location corresponding to the class.
     * @exception NullPointerException if path is null.
     * @exception IOException if an io exception occured while accessing the
     * path component.
     * @exception SecurityException if a security exception occured
     * while accessing the path component.
     */
    public static Location createLocation(String path) 
	throws IOException, SecurityException
    {
	String zipPrefix = null;
	// The special reflection URL
	if (path.startsWith("reflection:"))
	    return new ReflectionLocation();
	
	// We handle jar URL's ourself, this makes them work even with
	// java 1.1
	if (path.startsWith("jar:")) {
	    int index = 0;
	    do {
		index = path.indexOf('!', index);
	    } while (index != -1 && index != path.length()-1
		     && path.charAt(index+1) != '/');
	    
	    if (index == -1 || index == path.length() - 1)
		throw new MalformedURLException(path);
	    zipPrefix = path.substring(index+2);
	    if (!zipPrefix.endsWith("/"))
		zipPrefix += "/";
	    path = path.substring(4, index);
	}
	
	int index = path.indexOf(':');
	/* Grrr, we need to distinguish c:\foo from URLs. */
	if (index > 1) {
	    // This looks like an URL.
	    URL base = new URL(path);
	    URLConnection connection = base.openConnection();
	    if (zipPrefix != null
		|| path.endsWith(".zip") || path.endsWith(".jar")
		|| connection.getContentType().endsWith("/zip")) {
		// This is a zip file.  Read it into memory.
		byte[] contents = readURLZip(connection);
		return new ZipLocation(contents, zipPrefix);
	    } else
		return new URLLocation(base);
	} else {
	    File dir = new File(path);
	    if (zipPrefix != null || !dir.isDirectory()) {
		return new ZipLocation(new ZipFile(dir), zipPrefix);
	    } else
		return new LocalLocation(dir);
	}
    }

    private static String[] tokenizeClassPath(String path) {
	// Calculate a good approximation (rounded upwards) of the tokens
	// in this path.
	int length = 1;
	for (int index=path.indexOf(File.pathSeparatorChar); 
	     index != -1; length++)
	    index = path.indexOf(File.pathSeparatorChar, index+1);
	if (File.pathSeparatorChar != altPathSeparatorChar) {
	    for (int index=path.indexOf(altPathSeparatorChar); 
		 index != -1; length++)
		index = path.indexOf(altPathSeparatorChar, index+1);
	}


	String[] tokens = new String[length];
	int i = 0;
        for (int ptr=0; ptr < path.length(); ptr++, i++) {
	    int next = ptr;
	    while (next < path.length()
		   && path.charAt(next) != File.pathSeparatorChar
		   && path.charAt(next) != altPathSeparatorChar)
		next++;

	    int index = ptr;
	colon_separator:
	    while (next > ptr
		   && next < path.length()
		   && path.charAt(next) == ':') {
		// Check if this is a URL instead of a pathSeparator
		// Since this is a while loop it allows nested urls like
		// jar:ftp://ftp.foo.org/pub/foo.jar!/
		
		while (index < next) {
		    char c = path.charAt(index);
		    // According to RFC 1738 letters, digits, '+', '-'
		    // and '.' are allowed SCHEMA characters.  We
		    // disallow '.' because it is a good marker that
		    // the user has specified a filename instead of a
		    // URL.
		    if ((c < 'A' || c > 'Z')
			&& (c < 'a' || c > 'z')
			&& (c < '0' || c > '9')
			&& "+-".indexOf(c) == -1) {
			break colon_separator;
		    }
		    index++;
		}
		next++;
		index++;
		while (next < path.length()
		       && path.charAt(next) != File.pathSeparatorChar
		       && path.charAt(next) != altPathSeparatorChar)
		    next++;
	    }
	    tokens[i] = path.substring(ptr, next);
	    ptr = next;
	}
	return tokens;
    }

    private static byte[] readURLZip(URLConnection conn) throws IOException {
	int length = conn.getContentLength();
	if (length <= 0)
	    // Give a approximation if length is unknown
	    length = 10240;
	else
	    // Increase the length by one, so we hopefully don't need
	    // to grow the array later (we need one byte overshot to
	    // know when the end is reached).
	    length++;

	byte[] contents = new byte[length];

	InputStream is = conn.getInputStream();
	int pos = 0;
	for (;;) {
	    // This is ugly, is.available() may return zero even
	    // if there are more bytes.
	    int avail = Math.max(is.available(), 1);
	    if (pos + avail > contents.length) {
		// grow the byte array.
		byte[] newarr = new byte 
		    [Math.max(2*contents.length, pos + avail)];
		System.arraycopy(contents, 0, newarr, 0, pos);
		contents = newarr;
	    }
	    int count = is.read(contents, pos, contents.length-pos);
	    if (count == -1)
		break;
	    pos += count;
	}
	if (pos < contents.length) {
	    // shrink the byte array again.
	    byte[] newarr = new byte[pos];
	    System.arraycopy(contents, 0, newarr, 0, pos);
	    contents = newarr;
	}
	return contents;
    }

    private void initPath(String[] tokens) {
	int length = tokens.length;
	paths = new Location[length];

        for (int i = 0; i < length; i++) {
	    if (tokens[i] == null)
		continue;
	    try {
		paths[i] = createLocation(tokens[i]);
	    } catch (MalformedURLException ex) {
		GlobalOptions.err.println
		    ("Warning: Malformed URL "+ tokens[i] + ".");
	    } catch (IOException ex) {
		GlobalOptions.err.println
		    ("Warning: IO exception while accessing "
		     +tokens[i]+".");
	    } catch (SecurityException ex) {
		GlobalOptions.err.println
		    ("Warning: Security exception while accessing "
		     +tokens[i]+".");
	    }
	}
    }


    /** 
     * Creates a new class info for a class residing in this search
     * path.  This doesn't load the class immediately, this is done by
     * ClassInfo.loadInfo.  It is no error if class doesn't exists. <br>
     *
     * ClassInfos are guaranteed to be unique, i.e. if you have two
     * ClsssInfo objects loaded from the same class path with the same
     * classname they will always be identical.  The only exception is
     * if you use setName() or getClassInfoFromStream() and explicitly
     * overwrite a previous class.<br>
     *
     * @param classname the dot-separated full qualified name of the class.  
     *        For inner classes you must use the bytecode name with $,
     *        e.g. <code>java.util.Map$Entry</code>.
     * @exception IllegalArgumentException if class name isn't valid.
     */
    public ClassInfo getClassInfo(String classname) 
    {
	checkClassName(classname);
	int hash = classname.hashCode();
	Iterator iter = classes.iterateHashCode(hash);
	while (iter.hasNext()) {
	    ClassInfo clazz = (ClassInfo) iter.next();
	    if (clazz.getName().equals(classname))
		return clazz;
	}
	ClassInfo clazz = new ClassInfo(classname, this);
	classes.put(hash, clazz);
        return clazz;
    }

    /** 
     * Creates a new class info from an input stream containing the
     * bytecode.  This method is useful if you don't know the class
     * name or if you have the class in an unusual location.  The
     * class is fully loaded ({@link ClassInfo#ALL}) when you use this
     * method. <br>
     *
     * If a class with the same name was already created with
     * getClassInfo() it will effectively be removed from the class
     * path, although references to it may still exists elsewhere.
     *
     * @param stream the input stream containing the bytecode.
     * @return the ClassInfo representing this class.
     * @exception IOException if an io exception occurs.
     * @exception ClassFormatException if bytecode isn't valid.
     */
    public ClassInfo getClassInfoFromStream(InputStream stream) 
	throws IOException, ClassFormatException
    {
	ClassInfo classInfo = new ClassInfo(null, this);
	classInfo.read(new DataInputStream(new BufferedInputStream(stream)),
		   ClassInfo.ALL);
	String classname = classInfo.getName();
	/* Remove the classinfo with the same name from this path if
	 * it exists.
	 */
	Iterator iter = classes.iterateHashCode(classname.hashCode());
	while (iter.hasNext()) {
	    ClassInfo clazz = (ClassInfo) iter.next();
	    if (clazz.getName().equals(classname)) {
		iter.remove();
		break;
	    }
	}
	classes.put(classname.hashCode(), classInfo);
	return classInfo;
    }

    /**
     * Updates the classes unify hash for a class renaming.  This
     * should be only called by {@link ClassInfo#setName}.
     */
    void renameClassInfo(ClassInfo classInfo, String classname) {
	classes.remove(classInfo.getName().hashCode(), classInfo);
	/* Now remove any class already loaded with that name, just
	 * in case we're overwriting one.
	 */
	Iterator iter = classes.iterateHashCode(classname.hashCode());
	while (iter.hasNext()) {
	    ClassInfo clazz = (ClassInfo) iter.next();
	    if (clazz.getName().equals(classname)) {
		iter.remove();
		break;
	    }
	}
	classes.put(classname.hashCode(), classInfo);
    }

    /**
     * Checks, if a class with the given name exists somewhere in this
     * path.
     * @param classname the class name.
     * @exception IllegalArgumentException if class name isn't valid.
     */
    public boolean existsClass(String classname) {
	checkClassName(classname);
	return existsFile(classname.replace('.', '/') + ".class");
    }

    /**
     * Checks, if a file with the given name exists somewhere in this
     * path.
     * @param filename the file name.
     * @see #existsClass
     */
    public boolean existsFile(String filename) {
        for (int i=0; i<paths.length; i++) {
	    if (paths[i] != null && paths[i].exists(filename))
		return true;
	}
	return false;
    }

    private void checkClassName(String name) {
	if (name == null
	    || name.indexOf(';') != -1
	    || name.indexOf('[') != -1
	    || name.indexOf('/') != -1)
	    throw new IllegalArgumentException("Illegal class name: "+name);
    }

    /**
     * Searches for a file in the class path.
     * @param filename the filename. The path components should be separated
     * by "/".
     * @return An InputStream for the file.
     */
    public InputStream getFile(String filename) throws IOException {
        for (int i=0; i < paths.length; i++) {
	    if (paths[i] != null && paths[i].exists(filename))
		return paths[i].getFile(filename);
	}
	if (fallback != null)
	    return fallback.getFile(filename);
	throw new FileNotFoundException(filename);
    }

    /**
     * Searches for a filename in the class path and tells if it is a
     * directory.
     * @param filename the filename. The path components should be separated
     * by "/".
     * @return true, if filename exists and is a directory, false otherwise.
     */
    public boolean isDirectory(String filename) {
        for (int i=0; i < paths.length; i++) {
	    if (paths[i] != null && paths[i].exists(filename))
		return paths[i].isDirectory(filename);
	}
	return false;
    }

    /**
     * Searches for a filename in the class path and tells if it is a
     * package.  This is the same as isDirectory, but takes dot separated
     * names.
     * @param fqn the full qualified name. The components should be dot
     * separated.
     * @return true, if filename exists and is a package, false otherwise.
     * @see #isDirectory
     */
    public boolean isPackage(String fqn) {
	return isDirectory(fqn.replace('.', '/'));
    }

    /**
     * Get a list of all files in a given directory.
     * @param dirName the directory name. The path components must
     * be separated by "/".
     * @return An enumeration with all files/directories in the given
     * directory.  If dirName doesn't denote a directory it returns null.
     */
    public Enumeration listFiles(final String dirName) {
	return new Enumeration() {
	    int i = 0;
	    Enumeration enumeration;

	    public boolean hasMoreElements() {
		while (true) {
		    while (enumeration == null && i < paths.length) {
			if (paths[i] != null && paths[i].exists(dirName)
			    && paths[i].isDirectory(dirName))
			    enumeration = paths[i].listFiles(dirName);
			i++;
		    }

		    if (enumeration == null)
			return false;
		    if (enumeration.hasMoreElements())
			return true;
		    enumeration = null;
		}
	    }

	    public Object nextElement() {
		if (!hasMoreElements())
		    return new NoSuchElementException();
		return enumeration.nextElement();
	    }
	};
    }

    /**
     * Get a list of all classes and packages in the given package.
     * @param packageName a dot-separated package name.
     * @return An enumeration with all class/subpackages in the given
     * package.  If package doesn't denote a package it returns null.
     */
    public Enumeration listClassesAndPackages(String packageName) {
	String dir = packageName.replace('.','/');
        final Enumeration enumeration = listFiles(dir);
	final String prefix = dir.length() > 0 ? dir + "/" : dir;
        return new Enumeration() {
	    String next = getNext();
	    
	    private String getNext() {
		while (enumeration.hasMoreElements()) {
		    String name = (String) enumeration.nextElement();
		    if (name.indexOf('.') == -1
			&& isDirectory(prefix + name))
			// This is a package
			return name;
		    if (name.endsWith(".class"))
			// This is a class
			return name.substring(0, name.length()-6);
		}
		return null;
	    }

            public boolean hasMoreElements() {
                return next != null;
            }
            public Object nextElement() {
		if (next == null)
		    throw new NoSuchElementException();
		String result = next;
		next = getNext();
		return result;
            }
        };
    }

    /**
     * Loads the contents of a class.  This is only called by ClassInfo.
     */
    boolean loadClass(ClassInfo clazz, int howMuch) 
	throws IOException, ClassFormatException
    {
	for (int i = 0; i < paths.length; i++) {
	    if (paths[i] != null && paths[i].loadClass(clazz, howMuch))
		return true;
	}
	if (fallback != null)
	    return fallback.loadClass(clazz, howMuch);
	return false;
    }

    /**
     * Returns a string representation of this classpath.
     * @return a string useful for debugging purposes.
     */
    public String toString() {
	StringBuffer sb = new StringBuffer("ClassPath[");
	for (int i = 0; i < paths.length; i++) {
	    if (paths[i] != null)
		sb.append(paths[i]).append(',');
	}
	sb.append("fallback=").append(fallback).append(']');
	return sb.toString();
    }
}
