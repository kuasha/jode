
(jde-set-project-name "jode")
(jde-set-variables 
 '(jde-run-option-properties nil)
 '(jde-run-option-stack-size (quote ((128 . "kilobytes") (400 . "kilobytes"))))
 '(jde-gen-buffer-templates (quote (("Class" . jde-gen-class) ("Console" . jde-gen-console) ("Swing App" . jde-gen-jfc-app))))
 '(jde-compile-option-command-line-args "")
 '(jde-gen-action-listener-template (quote ("'& (P \"Component name: \")" "\".addActionListener(new ActionListener() {\" 'n>" "\"public void actionPerformed(ActionEvent e) {\" 'n>" "\"}});\" 'n>")))
 '(jde-compile-option-depend nil)
 '(jde-compile-option-optimize nil)
 '(jde-run-option-verify (quote (nil t)))
 '(jde-gen-inner-class-template (quote ("'& \"class \" (P \"Class name: \" class)" "(P \"Superclass: \" super t)" "(let ((parent (jde-gen-lookup-named 'super)))" "(if (not (string= parent \"\"))" "(concat \" extends \" parent))) \" {\" 'n>" "\"public \" (s class) \"() {\" 'n> \"}\" 'n> \"}\" 'n>")))
 '(jde-run-read-vm-args nil)
 '(jde-entering-java-buffer-hooks (quote (jde-reload-project-file)))
 '(jde-run-applet-viewer "appletviewer")
 '(jde-compile-option-debug t t)
 '(jde-project-file-name "prj.el")
 '(jde-run-option-verbose (quote (nil nil nil)))
 '(jde-run-application-class "")
 '(jde-db-option-vm-args nil)
 '(jde-run-option-heap-size (quote ((1 . "megabytes") (16 . "megabytes"))))
 '(jde-db-read-vm-args nil)
 '(jde-db-option-heap-profile (quote (nil "./java.hprof" 5 20 "Allocation objects")))
 '(jde-db-mode-hook nil)
 '(jde-run-option-garbage-collection (quote (t t)))
 '(jde-compile-option-vm-args nil)
 '(jde-run-applet-doc "index.html")
 '(jde-db-option-java-profile (quote (nil . "./java.prof")))
 '(jde-gen-get-set-var-template (quote ("'n>" "(P \"Variable type: \" type) \" \"" "(P \"Variable name: \" name) \";\" 'n> 'n>" "\"/**\" 'n>" "\"* Get the value of \" (s name) \".\" 'n>" "\"* @return Value of \" (s name) \".\" 'n>" "\"*/\" 'n>" "\"public \" (s type) \" get\" (jde-gen-init-cap (jde-gen-lookup-named 'name))" "\"() {return \" (s name) \";}\" 'n> 'n>" "\"/**\" 'n>" "\"* Set the value of \" (s name) \".\" 'n>" "\"* @param v  Value to assign to \" (s name) \".\" 'n>" "\"*/\" 'n>" "\"public void set\" (jde-gen-init-cap (jde-gen-lookup-named 'name))" "\"(\" (s type) \"  v) {this.\" (s name) \" = v;}\" 'n>")))
 '(jde-db-option-verify (quote (nil t)))
 '(jde-run-mode-hook nil)
 '(jde-db-option-classpath nil)
 '(jde-compile-option-deprecation nil)
 '(jde-db-startup-commands nil)
 '(jde-gen-boilerplate-function (quote jde-gen-create-buffer-boilerplate))
 '(jde-compile-option-nodebug nil)
 '(jde-compile-option-classpath nil)
 '(jde-build-use-make nil)
 '(jde-quote-classpath t)
 '(jde-gen-to-string-method-template (quote ("'&" "\"public String toString() {\" 'n>" "\"return super.toString();\" 'n>" "\"}\" 'n>")))
 '(jde-run-read-app-args nil)
 '(jde-db-source-directories (quote ("d:/jdk1.2/src/")))
 '(jde-db-option-properties nil)
 '(jde-db-option-stack-size (quote ((128 . "kilobytes") (400 . "kilobytes"))))
 '(jde-db-set-initial-breakpoint t)
 '(jde-run-option-application-args (quote ("-v" "--debug=analyze,inout" "--classpath=/home/jochen/output/share/jode-1.0.90.jar" "jode.Decompiler")) t)
 '(jde-gen-mouse-listener-template (quote ("'& (P \"Component name: \")" "\".addMouseListener(new MouseAdapter() {\" 'n>" "\"public void mouseClicked(MouseEvent e) {}\" 'n>" "\"public void mouseEntered(MouseEvent e) {}\" 'n>" "\"public void mouseExited(MouseEvent e) {}\" 'n>" "\"public void mousePressed(MouseEvent e) {}\" 'n>" "\"public void mouseReleased(MouseEvent e) {}});\" 'n>")))
 '(jde-gen-console-buffer-template (quote ("(funcall jde-gen-boilerplate-function) 'n" "\"/**\" 'n" "\" * \"" "(file-name-nondirectory buffer-file-name) 'n" "\" *\" 'n" "\" *\" 'n" "\" * Created: \" (current-time-string) 'n" "\" *\" 'n" "\" * @author \" (user-full-name) 'n" "\" * @version\" 'n" "\" */\" 'n>" "'n>" "\"public class \"" "(file-name-sans-extension (file-name-nondirectory buffer-file-name))" "\" {\" 'n> 'n>" "\"public \"" "(file-name-sans-extension (file-name-nondirectory buffer-file-name))" "\"() {\" 'n>" "'n>" "\"}\" 'n>" "'n>" "\"public static void main(String[] args) {\" 'n>" "'p 'n>" "\"}\" 'n> 'n>" "\"} // \"" "(file-name-sans-extension (file-name-nondirectory buffer-file-name))" "'n>")))
 '(jde-compile-option-directory "/home/jochen/java/unstable/build" t)
 '(jde-run-option-vm-args nil)
 '(jde-make-program "make")
 '(jde-use-font-lock t)
 '(jde-db-option-garbage-collection (quote (t t)))
 '(jde-gen-class-buffer-template (quote ("(funcall jde-gen-boilerplate-function)" "\"package jode;\" 'n" "'n" "\"/**\" 'n" "\" * \" 'n" "\" * @author \" (user-full-name) 'n" "\" */\" 'n" "\"public class \" (file-name-sans-extension (file-name-nondirectory buffer-file-name))" "\" \" (jde-gen-get-super-class) \" {\" 'n" "> 'n" "> \"public \" (file-name-sans-extension (file-name-nondirectory buffer-file-name)) \"() {\" 'n" "> 'p 'n" "> \"}\" 'n" "> 'n" "> \"}\" 'n")))
 '(jde-compiler "jikes +E")
 '(jde-jdk-doc-url "file:/usr/doc/packages/jdk115/docs/index.html")
 '(jde-db-debugger (quote ("jdb" . "Executable")))
 '(jde-compile-option-optimize-interclass nil)
 '(jde-run-option-classpath nil)
 '(jde-key-bindings (quote (("" . jde-compile) ("" . jde-run) ("" . jde-db) ("" . jde-build) ("" . jde-run-menu-run-applet) ("" . jde-browse-jdk-doc) ("" . jde-save-project) ("" . jde-gen-println))))
 '(jde-gen-mouse-motion-listener-template (quote ("'& (P \"Component name: \")" "\".addMouseMotionListener(new MouseMotionAdapter() {\" 'n>" "\"public void mouseDragged(MouseEvent e) {}\" 'n>" "\"public void mouseMoved(MouseEvent e) {}});\" 'n>")))
 '(jde-db-marker-regexp "^Breakpoint hit: .*(\\([^$]*\\).*:\\([0-9]*\\))")
 '(jde-run-working-directory "")
 '(jde-gen-window-listener-template (quote ("'& (P \"Window name: \")" "\".addWindowListener(new WindowAdapter() {\" 'n>" "\"public void windowActivated(WindowEvent e) {}\" 'n>" "\"public void windowClosed(WindowEvent e) {}\" 'n>" "\"public void windowClosing(WindowEvent e) {System.exit(0);}\" 'n>" "\"public void windowDeactivated(WindowEvent e) {}\" 'n>" "\"public void windowDeiconified(WindowEvent e) {}\" 'n>" "\"public void windowIconified(WindowEvent e) {}\" 'n>" "\"public void windowOpened(WindowEvent e) {}});\" 'n>")))
 '(jde-global-classpath (quote ("/usr/local/swing-1.1/swing.jar" "/usr/local/1.1collections/lib/collections.jar" "/home/jochen/java/jars/getopt.zip" "/home/jochen/java/unstable/jode" "/home/jochen/java/unstable/build" "/usr/lib/java/lib/classes.zip")) t)
 '(jde-enable-abbrev-mode nil)
 '(jde-gen-println (quote ("'&" "\"System.out.println(\" (P \"Print out: \") \");\" 'n>")))
 '(jde-run-option-heap-profile (quote (nil "./java.hprof" 5 20 "Allocation objects")))
 '(jde-db-read-app-args nil)
 '(jde-db-option-verbose (quote (nil nil nil)))
 '(jde-run-java-vm "java")
 '(jde-read-compile-args nil)
 '(jde-run-option-java-profile (quote (nil . "./java.prof")))
 '(jde-compile-option-encoding nil)
 '(jde-run-java-vm-w "javaw")
 '(jde-compile-option-nowarn nil)
 '(jde-gen-jfc-app-buffer-template (quote ("(funcall jde-gen-boilerplate-function) 'n" "\"import java.awt.*;\" 'n" "\"import java.awt.event.*;\" 'n" "\"import com.sun.java.swing.*;\" 'n 'n" "\"/**\" 'n" "\" * \"" "(file-name-nondirectory buffer-file-name) 'n" "\" *\" 'n" "\" *\" 'n" "\" * Created: \" (current-time-string) 'n" "\" *\" 'n" "\" * @author \" (user-full-name) 'n" "\" * @version\" 'n" "\" */\" 'n>" "'n>" "\"public class \"" "(file-name-sans-extension (file-name-nondirectory buffer-file-name))" "\" extends JFrame {\" 'n> 'n>" "\"public \"" "(file-name-sans-extension (file-name-nondirectory buffer-file-name))" "\"() {\" 'n>" "\"super(\\\"\" (P \"Enter app title: \") \"\\\");\" 'n>" "\"setSize(600, 400);\" 'n>" "\"addWindowListener(new WindowAdapter() {\" 'n>" "\"public void windowClosing(WindowEvent e) {System.exit(0);}\" 'n>" "\"public void windowOpened(WindowEvent e) {}});\" 'n>" "\"}\" 'n>" "'n>" "\"public static void main(String[] args) {\" 'n>" "'n>" "(file-name-sans-extension (file-name-nondirectory buffer-file-name))" "\" f = new \"" "(file-name-sans-extension (file-name-nondirectory buffer-file-name))" "\"();\" 'n>" "\"f.show();\" 'n>" "'p 'n>" "\"}\" 'n> 'n>" "\"} // \"" "(file-name-sans-extension (file-name-nondirectory buffer-file-name))" "'n>")))
 '(jde-db-option-application-args nil)
 '(jde-gen-buffer-boilerplate (quote ("/* " (file-name-nondirectory buffer-file-name) " Copyright (C) 1997-1998 Jochen Hoenicke." (quote n) " *" (quote n) " * This program is free software; you can redistribute it and/or modify" (quote n) " * it under the terms of the GNU General Public License as published by" (quote n) " * the Free Software Foundation; either version 2, or (at your option)" (quote n) " * any later version." (quote n) " *" (quote n) " * This program is distributed in the hope that it will be useful," (quote n) " * but WITHOUT ANY WARRANTY; without even the implied warranty of" (quote n) " * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the" (quote n) " * GNU General Public License for more details." (quote n) " *" (quote n) " * You should have received a copy of the GNU General Public License" (quote n) " * along with this program; see the file COPYING.  If not, write to" (quote n) " * the Free Software Foundation, 675 Mass Ave, Cambridge, MA 02139, USA." (quote n) " *" (quote n) " * $" "Id$" (quote n) " */" (quote n))))
 '(jde-db-option-heap-size (quote ((1 . "megabytes") (16 . "megabytes"))))
 '(jde-compile-option-verbose nil)
 '(jde-mode-abbreviations (quote (("ab" . "abstract") ("bo" . "boolean") ("br" . "break") ("by" . "byte") ("byv" . "byvalue") ("cas" . "cast") ("ca" . "catch") ("ch" . "char") ("cl" . "class") ("co" . "const") ("con" . "continue") ("de" . "default") ("dou" . "double") ("el" . "else") ("ex" . "extends") ("fa" . "false") ("fi" . "final") ("fin" . "finally") ("fl" . "float") ("fo" . "for") ("fu" . "future") ("ge" . "generic") ("go" . "goto") ("impl" . "implements") ("impo" . "import") ("ins" . "instanceof") ("in" . "int") ("inte" . "interface") ("lo" . "long") ("na" . "native") ("ne" . "new") ("nu" . "null") ("pa" . "package") ("pri" . "private") ("pro" . "protected") ("pu" . "public") ("re" . "return") ("sh" . "short") ("st" . "static") ("su" . "super") ("sw" . "switch") ("sy" . "synchronized") ("th" . "this") ("thr" . "throw") ("throw" . "throws") ("tra" . "transient") ("tr" . "true") ("vo" . "void") ("vol" . "volatile") ("wh" . "while"))))
 '(jde-make-args "")
 '(jde-gen-code-templates (quote (("Get Set Pair" . jde-gen-get-set) ("toString method" . jde-gen-to-string-method) ("Action Listener" . jde-gen-action-listener) ("Window Listener" . jde-gen-window-listener) ("Mouse Listener" . jde-gen-mouse-listener) ("Mouse Motion Listener" . jde-gen-mouse-motion-listener) ("Inner Class" . jde-gen-inner-class) ("println" . jde-gen-println)))))
