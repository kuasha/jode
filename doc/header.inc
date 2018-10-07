<?php
if (! $extension) {
  $extension = "php";
}
$version="1.1";

function selflink($link) {
  global $extension;
  echo "<a href=\"./";
  if ($link != "index") {
    if (ereg("#", $link)) {
      $link = ereg_replace("#", ".$extension#", $link);
    } else {
      $link .= ".$extension";
    }
    echo "$link";
  }
  echo "\">";
}

function sflink($link) {
  echo "<a href=\"http://sourceforge.net/$link?group_id=3790\">";
}
?><!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<html>
<head>
<title>Java Optimize and Decompile Environment (JODE)</title>
<meta name="date" content="2001-05-29">
<meta name="description" content="JODE - Java Optimize and Decompile Environment.">
<meta name="author" content="Jochen Hoenicke">
<meta name="keywords" content="jode, java, decompiler, obfuscator, deobfuscator, reverse engineering, free, GPL">
<style type="text/css">
<!--
body { color:#000000; background-color: #FFFFFF; }
.nav { font-family: Helvetica, Arial, sans-serif; font-weight: bold;
       color:#000000; background-color: #EEEEF8; }
.footer { color:#FFFFFF; background-color: #737B9C; }
.boldlink { font-weight:bold; text-decoration: none; color:#FFFFFF; }
//-->
</style>
</head>

<body text="#000000" bgcolor="#FFFFFF">

<table cellpadding=4 cellspacing=1 width="100%"
  ><tr
    ><td align="left"
      ><img src="jode-logo.gif" alt="JODE" width=286 height=110
    ></td
    ><td align="right"
      >Powered by <a href="http://sourceforge.net"><img
      src="http://sourceforge.net/sflogo.php?group_id=3790&amp;type=1"
      border=0 width=88 height=31 alt="SourceForge"></a><br
      >Best viewed with <a
      href="http://www.anybrowser.org/campaign/"><img
      src="a-logo.gif" border=0 width=88 height=31 alt="Any
      Browser"></a><br
    ></td
  ></tr
></table>

<?php require("menu.inc"); ?>

