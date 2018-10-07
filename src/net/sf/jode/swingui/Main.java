/* Main Copyright (C) 1999-2002 Jochen Hoenicke.
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
 * $Id: Main.java 1411 2012-03-01 22:39:08Z hoenicke $
 */

package net.sf.jode.swingui;
import net.sf.jode.GlobalOptions;
import net.sf.jode.bytecode.ClassPath;
import net.sf.jode.decompiler.Decompiler;
import net.sf.jode.decompiler.ProgressListener;

///#def JAVAX_SWING javax.swing
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.*;
///#enddef

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.StringTokenizer;
import java.util.ResourceBundle;

public class Main 
    implements ActionListener, Runnable, TreeSelectionListener {
    Decompiler decompiler;
    JFrame frame;
    JTree classTree;
    JPanel statusLine;
    PackagesTreeModel packModel;
    HierarchyTreeModel hierModel;
    JTextArea  sourcecodeArea, errorArea;
    Thread decompileThread;
    String currentClassPath, lastClassName;
    ClassPathDialog classPathDialog;

    JProgressBar progressBar;
    private JMenuItem saveMenuItem;

    boolean hierarchyTree;

    public static ResourceBundle bundle;

    public Main(String[] classpath) {
        decompiler = new Decompiler();
	frame = new JFrame(GlobalOptions.copyright);
	classPathDialog = new ClassPathDialog(frame, classpath);
	decompiler.setClassPath(classPathDialog.getClassPath());
	classPathDialog.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    ClassPath classPath = classPathDialog.getClassPath();
		    decompiler.setClassPath(classPath);
		    if (classTree != null)
			classTree.clearSelection();
		    if (packModel != null)
			packModel.rebuild();
		    if (hierModel != null && hierarchyTree) {
			hierModel.rebuild();
		    } else {
			hierModel = null;
		    }
		}
	    });

	fillContentPane(frame.getContentPane());
	addMenu(frame);
	frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
	frame.addWindowListener(new WindowAdapter() {
	    public void windowClosing(WindowEvent e) {
		System.exit(0);
	    }
	});
	frame.pack();
	this.center();
    }
    
     public void center() {
        //center window
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension frameSize = frame.getSize();

        if (frameSize.height > screenSize.height) {
            frameSize.height = screenSize.height;
        }

        if (frameSize.width > screenSize.width) {
            frameSize.width = screenSize.width;
        }

        frame.setLocation((screenSize.width - frameSize.width) / 2,
            (screenSize.height - frameSize.height) / 2);
    }

    public void show() {
	frame.setVisible(true);
    }

    public void fillContentPane(Container contentPane) {
	statusLine = new JPanel();
	hierarchyTree = false;
	packModel = new PackagesTreeModel(this);
	hierModel = null;
	Font monospaced = new Font("monospaced", Font.PLAIN, 12);
	classTree = new JTree(packModel);
	classTree.setRootVisible(false);
	DefaultTreeSelectionModel selModel = new DefaultTreeSelectionModel();
	selModel.setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
	classTree.setSelectionModel(selModel);
	classTree.addTreeSelectionListener(this);
        JScrollPane spClassTree = new JScrollPane(classTree);
	sourcecodeArea = new JTextArea(20, 80);
	sourcecodeArea.setEditable(false);
	sourcecodeArea.setFont(monospaced);
	JScrollPane spText = new JScrollPane(sourcecodeArea);
	errorArea = new JTextArea(3, 80);
	errorArea.setEditable(false);
	errorArea.setFont(monospaced);
	JScrollPane spError = new JScrollPane(errorArea);

	JSplitPane rightPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
					      spText, spError);
	JSplitPane allPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
					    spClassTree, rightPane);
	contentPane.setLayout(new BorderLayout());
	contentPane.add(allPane, BorderLayout.CENTER);
	contentPane.add(statusLine, BorderLayout.SOUTH);
	progressBar = new JProgressBar();
	statusLine.add(progressBar);
	rightPane.setDividerLocation(300);
	rightPane.setDividerSize(4);
	allPane.setDividerLocation(200);
	allPane.setDividerSize(4);
	decompiler.setErr(new PrintWriter
	    (new BufferedWriter(new AreaWriter(errorArea)), true));
    }

    public synchronized void valueChanged(TreeSelectionEvent e) {
	if (decompileThread != null)
	    return;
	TreePath path = e.getNewLeadSelectionPath();
	if (path == null)
	    return;
	Object node = path.getLastPathComponent();
	if (node != null) {
	    if (hierarchyTree && hierModel.isValidClass(node))
		lastClassName = hierModel.getFullName(node);
	    else if (!hierarchyTree && packModel.isValidClass(node))
		lastClassName = packModel.getFullName(node);
	    else
		return;
	    
	    startDecompiler();
	}
    }

    public void actionPerformed(ActionEvent e) {
	if (e.getSource() == classTree)
	    startDecompiler();
    }

    public synchronized void startDecompiler() {
	if (decompileThread == null) {
	    decompileThread = new Thread(this);
	    decompileThread.setPriority(Thread.MIN_PRIORITY);
	    
	    progressBar.setMinimum(0);
	    progressBar.setMaximum(1000);
	    progressBar.setString(bundle.getString("main.decompiling"));
	    progressBar.setStringPainted(true);
	    decompileThread.start();
	    this.saveMenuItem.setEnabled(true);
	}
    }

    public class AreaWriter extends Writer {
	boolean initialized = false;
	boolean lastCR = false;
	private JTextArea area;

	public AreaWriter(JTextArea a) {
	    area = a;
	}

	public void write(char[] b, int off, int len) throws IOException {
	    /* Note that setText and append are thread safe! */
	    if (!initialized) {
		area.setText("");
		initialized = true;
	    }
	    String str = new String(b, off, len);
	    StringBuffer sb = new StringBuffer(len);
	    while (str != null && str.length() > 0) {
		if (lastCR && str.charAt(0) == '\n')
		    str = str.substring(1);
		int crIndex = str.indexOf('\r');
		if (crIndex >= 0) {
		    sb.append(str.substring(0, crIndex));
		    sb.append("\n");
		    str = str.substring(crIndex+1);
		    lastCR = true;
		} else {
		    sb.append(str);
		    str = null;
		}
	    }
	    area.append(sb.toString());
	}

	public void flush() {
	}

	public void close() {
	}
    }

    public void run() {
	errorArea.setText("");
	Writer writer = new BufferedWriter
	    (new AreaWriter(sourcecodeArea), 1024);

	ProgressListener progListener = new ProgressListener()
	    {
		public void updateProgress(final double progress, 
					   final String detail) {
		    SwingUtilities.invokeLater(new Runnable() 
			{
			    public void run() {
				progressBar.setValue((int)(1000 * progress));
				progressBar.setString(detail);
			    }
			});
		}
	    };
	try {
	    decompiler.decompile(lastClassName, writer, progListener);
	} catch (Throwable t) {
	    try {
		writer.write(bundle.getString("main.exception"));
		PrintWriter pw = new PrintWriter(writer);
		t.printStackTrace(pw);
		pw.flush();
	    } catch (IOException ex) {
		/* Shouldn't happen, complain to stderr */
		ex.printStackTrace();
	    }
	} finally {
	    try {
		writer.close();
	    } catch (IOException ex) {
		/* ignore */
	    }
	    synchronized(this) {
		decompileThread = null;
	    }
	}
	SwingUtilities.invokeLater(new Runnable()
	    {
		public void run() {
		    progressBar.setValue(0);
		    progressBar.setString("");
		}
	    });
    }

    public void addMenu(JFrame frame) {
	JMenuBar bar = new JMenuBar();
	JMenu menu;
	JMenuItem item;
	menu = new JMenu(bundle.getString("menu.file"));
	menu.setMnemonic('f');
	
	this.saveMenuItem = new JMenuItem(bundle.getString("menu.file.save"));
	this.saveMenuItem.setMnemonic('s');
	this.saveMenuItem.setEnabled(false);
	this.saveMenuItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ev) {
                    JFileChooser chooser = new JFileChooser();

                    try {
                        if (JFileChooser.APPROVE_OPTION != chooser.showSaveDialog(
                                    new Container())) {
                            return; //cancelled
                        }
                    } catch (Exception he) {
                        return;
                    }

                    //save file
                    File saveFile = new File(chooser.getSelectedFile()
                                                    .getAbsoluteFile().toString());

                    FileOutputStream out; // declare a file output object
                    PrintStream p; // declare a print stream object

                    try {
                        // Create a new file output stream
                        out = new FileOutputStream(saveFile);

                        // Connect print stream to the output stream
                        p = new PrintStream(out);

                        p.print(sourcecodeArea.getText());
                        p.close();
                    } catch (Exception e) {
                        errorArea.setText(bundle.getString("menu.file.save.ex"));
                    }
                }
            });
        menu.add(this.saveMenuItem);

        menu.add(new JSeparator());
	
	item = new JMenuItem(bundle.getString("menu.file.gc"));
	item.setMnemonic('c');
	item.addActionListener(new ActionListener() {
	    public void actionPerformed(ActionEvent ev) {
		System.gc();
		System.runFinalization();
	    }
	});
	menu.add(item);
	item = new JMenuItem(bundle.getString("menu.file.exit"));
	item.setMnemonic('x');
	item.addActionListener(new ActionListener() {
	    public void actionPerformed(ActionEvent ev) {
		System.exit(0);
	    }
	});
	menu.add(item);
	bar.add(menu);
	menu = new JMenu(bundle.getString("menu.opt"));
	menu.setMnemonic('o');
	final JCheckBoxMenuItem hierItem
	    = new JCheckBoxMenuItem(bundle.getString("menu.opt.hier"),
				    hierarchyTree);
	hierItem.setMnemonic('h');
	hierItem.addActionListener(new ActionListener() {
	    public void actionPerformed(ActionEvent ev) {
		hierarchyTree = hierItem.isSelected();
		if (hierarchyTree && hierModel == null) {
		    hierModel = new HierarchyTreeModel(Main.this, progressBar);
		    reselect();
		}
		classTree.setModel(hierarchyTree
				   ? (TreeModel) hierModel : packModel);
		if (lastClassName != null) {
		    TreePath lastPath = (hierarchyTree
					 ? hierModel.getPath(lastClassName)
					 : packModel.getPath(lastClassName));
		    classTree.setSelectionPath(lastPath);
		    classTree.scrollPathToVisible(lastPath);
		}
	    }
	});
	menu.add(hierItem);
	menu.add(new JSeparator());
	item = new JMenuItem(bundle.getString("menu.opt.cp"));
	item.setMnemonic('c');
	item.addActionListener(new ActionListener() {
	    public void actionPerformed(ActionEvent ev) {
		classPathDialog.showDialog();
	    }
	});
	menu.add(item);
	bar.add(menu);
	frame.setJMenuBar(bar);
    }

    public ClassPath getClassPath() {
	return classPathDialog.getClassPath();
    }

    public void treeNodesChanged(TreeModelEvent e) {
	reselect();
    }

    public void treeNodesInserted(TreeModelEvent e) {
	reselect();
    }

    public void treeNodesRemoved(TreeModelEvent e) {
	reselect();
    }

    public void treeStructureChanged(TreeModelEvent e) {
	reselect();
    }

    public void reselect() {
	if (lastClassName != null) {
	    TreePath lastPath = (hierarchyTree
				 ? hierModel.getPath(lastClassName)
				 : packModel.getPath(lastClassName));
	    if (lastPath != null) {
		classTree.setSelectionPath(lastPath);
		classTree.scrollPathToVisible(lastPath);
	    }
	}
    }

    public static void usage() {
	PrintWriter err = GlobalOptions.err;
	int numUsage = Integer.parseInt(bundle.getString("usage.count"));
	for (int i=0; i < numUsage ; i++)
	    err.println(bundle.getString("usage."+i));
    }
    
    public static void main(String[] params) {
	bundle = ResourceBundle.getBundle("net.sf.jode.swingui.Resources");
	String cp = System.getProperty("java.class.path", "");
	cp = cp.replace(File.pathSeparatorChar, 
			Decompiler.altPathSeparatorChar);
	String bootClassPath = System.getProperty("sun.boot.class.path");
	if (bootClassPath != null)
	    cp += Decompiler.altPathSeparatorChar
		+ bootClassPath.replace(File.pathSeparatorChar, 
					Decompiler.altPathSeparatorChar);
	int i;
	for (i = 0; i < params.length; i++) {
	    if (params[i].equals("--classpath")
		|| params[i].equals("--cp")
		|| params[i].equals("-c"))
		cp = params[++i];
	    else if (params[i].equals("--debug")
		     || params[i].equals("--D")) {
		String arg = params[++i];
		GlobalOptions.setDebugging(arg);
	    } else if (params[i].startsWith("-")) {
		if (!params[i].equals("--help")
		    && !params[i].equals("-h"))
		    System.err.println("Unknown option: "+params[i]);
		usage();
		return;
	    } else
		cp = params[i];
	}
	StringTokenizer st = new StringTokenizer
	    (cp, ""+Decompiler.altPathSeparatorChar);
	String[] splitcp = new String[st.countTokens()];
	for (i = 0; i< splitcp.length; i++)
	    splitcp[i] = st.nextToken();
	Main win = new Main(splitcp);
	win.show();
    }
}
