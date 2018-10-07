/* ClassPathDialog Copyright (C) 2000-2002 Jochen Hoenicke.
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
 * $Id: ClassPathDialog.java 1411 2012-03-01 22:39:08Z hoenicke $
 */

package net.sf.jode.swingui;
import net.sf.jode.bytecode.ClassPath;

///#def JAVAX_SWING javax.swing
import javax.swing.*;
import javax.swing.filechooser.*;
import javax.swing.event.*;
///#enddef

import java.awt.Dimension;
import java.awt.BorderLayout;
import java.awt.event.*;
import java.awt.AWTEventMulticaster;
import java.io.File;

public class ClassPathDialog {
    JDialog dialog;

    JTextField editField;
    boolean editFieldChanged = false;
    JList pathList;
    DefaultListModel pathListModel;

    ClassPath currentClassPath;
    ActionListener actionListener = null;

    public ClassPathDialog(JFrame frame, String[] classPath) {
	dialog = new JDialog(frame, Main.bundle.getString("cpdialog.title"), 
			     false);
	JPanel panel = new JPanel();
	panel.setLayout(new BorderLayout());
	panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));
	panel.add(BorderLayout.NORTH, createEditPane());
	panel.add(BorderLayout.CENTER, createListPane());
	panel.add(BorderLayout.SOUTH, createOkayCancelPane());

	dialog.getContentPane().add(BorderLayout.CENTER, panel);
	dialog.pack();
	dialog.addWindowListener(new WindowAdapter() {
		public void windowClosing() {
		    dialog.setVisible(false);
		}
	    });

	for (int i = 0; i < classPath.length; i++)
	    pathListModel.addElement(classPath[i]);
	createNewClassPath();
    }

    public void showDialog() {
	dialog.setVisible(true);
    }

    public ClassPath getClassPath() {
	return currentClassPath;
    }

    public void addActionListener(ActionListener l) {
	actionListener = AWTEventMulticaster.add(actionListener, l);
    }
    public void removeActionListener(ActionListener l) {
	actionListener = AWTEventMulticaster.remove(actionListener, l);
    }

    ClassPath reflectClassPath = new ClassPath("reflection:");
    private void createNewClassPath() {
	String[] paths = new String[pathListModel.getSize()];
	pathListModel.copyInto(paths);
	currentClassPath = new ClassPath(paths, reflectClassPath);
	if (actionListener != null)
	    actionListener.actionPerformed
		(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, null));
    }

    void add() {
	String entry = editField.getText();
	int index = pathListModel.getSize();
	if (pathList.isSelectionEmpty())
	    pathListModel.addElement(entry);
	else {
	    index = pathList.getLeadSelectionIndex() + 1;
	    pathListModel.add(index - 1,
			      editField.getText());
	}
	pathList.setSelectedIndex(index);
	editFieldChanged = false;
    }

    static class JarFileFilter extends FileFilter {
	public boolean accept(File f) {
	    if (f.isDirectory())
		return true;
	    String name = f.getName();
	    int dot = name.lastIndexOf('.');
	    if (dot >= 0) {
		String ext = name.substring(dot+1);
		if (ext.equals("jar") || ext.equals("zip"))
		    return true;
	    }
	    return false;
	}

	public String getDescription() {
	    return Main.bundle.getString("browse.filter.description");
	}
    }

    class BrowseListener implements ActionListener {
	public void actionPerformed(ActionEvent e) {
	    String fileName = editField.getText();
	    if (fileName.length() == 0)
		fileName = null;
	    JFileChooser fileChooser = new JFileChooser(fileName);
	    fileChooser.setFileSelectionMode
		(JFileChooser.FILES_AND_DIRECTORIES);
	    fileChooser.setDialogType(JFileChooser.CUSTOM_DIALOG);
	    fileChooser.setDialogTitle(Main.bundle.getString("browse.title"));
	    fileChooser.setFileFilter(new JarFileFilter());
	    fileChooser.setApproveButtonText
		(Main.bundle.getString("button.select"));
	    fileChooser.setApproveButtonMnemonic('s');
	    if (fileChooser.showDialog(dialog, null)
		== JFileChooser.APPROVE_OPTION) {
		editField.setText(fileChooser.getSelectedFile().getPath());
		add();
	    }
	}
    }

    private JPanel createEditPane() {
	editField = new JTextField();

	JButton browseButton = new JButton
	    (Main.bundle.getString("button.browse"));
	browseButton.setMnemonic('b');
	browseButton.addActionListener(new BrowseListener());

	JButton addButton = new JButton
	    (Main.bundle.getString("button.add"));
	addButton.setMnemonic('d');
	JButton removeButton = new JButton
	    (Main.bundle.getString("button.remove"));
	removeButton.setMnemonic('r');

	ActionListener addListener = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    add();
		}
	    };
	addButton.addActionListener(addListener);
	editField.addActionListener(addListener);
	editField.getDocument().addDocumentListener(new DocumentListener() {
		public void changedUpdate(DocumentEvent e) {
		    editFieldChanged = true;
		}
		public void insertUpdate(DocumentEvent e) {
		    editFieldChanged = true;
		}
		public void removeUpdate(DocumentEvent e) {
		    editFieldChanged = (editField.getText().length() > 0);
		}
	    });

	removeButton.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    if (!pathList.isSelectionEmpty()) {
			int index = pathList.getLeadSelectionIndex();
			pathListModel.remove(index);
			if (index < pathListModel.getSize())
			    pathList.setSelectedIndex(index);
		    }
		}
	    });

	JPanel editPane = new JPanel();
	editPane.setLayout(new BorderLayout());
	editPane.add(BorderLayout.EAST, browseButton);
	editPane.add(BorderLayout.SOUTH, 
		     createButtonPane(new JButton[] { addButton, 
						      removeButton }));
	editPane.add(BorderLayout.CENTER, editField);
	return editPane;
    }

    private JComponent createListPane() {
	pathListModel = new DefaultListModel();
	pathList = new JList(pathListModel);
	pathList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JScrollPane listScroller = new JScrollPane(pathList);
        listScroller.setMinimumSize(new Dimension(250, 80));
        listScroller.setAlignmentX(JScrollPane.LEFT_ALIGNMENT);

	pathList.addListSelectionListener(new ListSelectionListener() {
		public void valueChanged(ListSelectionEvent e) {
		    if (!pathList.isSelectionEmpty()
			&& !editFieldChanged) {
			editField.setText((String) 
					  pathList.getSelectedValue());
			editFieldChanged = false;
		    }
		}
	    });
	return listScroller;
    }

    private JPanel createOkayCancelPane() {
	JButton okayButton = new JButton
	    (Main.bundle.getString("button.okay"));
	okayButton.setMnemonic('o');
	okayButton.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    createNewClassPath();
		    dialog.setVisible(false);
		}
	    });
	JButton applyButton = new JButton
	    (Main.bundle.getString("button.apply"));
	applyButton.setMnemonic('a');
	applyButton.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    createNewClassPath();
		}
	    });
	JButton cancelButton = new JButton
	    (Main.bundle.getString("button.cancel"));
	cancelButton.setMnemonic('c');
	cancelButton.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    dialog.setVisible(false);
		}
	    });
	return createButtonPane
	    (new JButton[] { okayButton, applyButton, cancelButton });
    }

    private JPanel createButtonPane(JButton[] buttons) {
	JPanel buttonPane = new JPanel();
	buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.X_AXIS));
	buttonPane.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
	buttonPane.add(Box.createHorizontalGlue());
	buttonPane.add(buttons[0]);
	for (int i=1; i < buttons.length; i++) {
	    buttonPane.add(Box.createRigidArea(new Dimension(10, 0)));
	    buttonPane.add(buttons[i]);
	}
	buttonPane.add(Box.createHorizontalGlue());
	return buttonPane;
    }
}
