<?php
$menu = 
     array("<B>Home</B>"  , "selflink", "index",
           "Project page" , "sflink", "project/",
           "Applet"       , "selflink", "applet",
           "Download"     , "selflink", "download",
           "FAQ"          , "selflink", "faq",
           "Feedback"     , "selflink", "feedback",
	   "Documentation", "selflink", "usage",
           "License"      , "selflink", "license",
           "History"      , "selflink", "history",
           "Links"        , "selflink", "links",
           "Blue Sky"     , "selflink", "bluesky");
?>

<table cellspacing=0 cellpadding=3 border=0 bgcolor="#EEEEF8" class="nav">
<tr><td class="nav">
<?php
reset($menu);
$self = ereg_replace("^.*/", "", $PHP_SELF);
while (list($dummy, $name) = each($menu)) {
  list($dummy, $type) = each($menu);
  list($dummy, $link) = each($menu);
  $name = ereg_replace(" ", "&nbsp;", $name);
  if ($type == "selflink") {
    if ($self == "$link.$extension") {
      echo "$name";
    } else {
      selflink($link);
      echo "$name</a>";
    }
  } else if ($type == "sflink") {
    sflink($link);
    echo "$name</a>";
  } else if ($type == "-") {
    echo "<br>\n";
    continue;
  } else if ($type == "link") {
    echo "<a href=\"$link\">$name</a>";
  }
  if (current($menu)) {
    echo " |\n";
  }
}
?>
</td></tr>
</table><br>
