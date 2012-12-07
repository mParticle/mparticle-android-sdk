<?php
// This generates a 300x100 gif with the background set 
// based on the value of an "mp_cookie" cookie
// It also sets or clears the cookie based on the "mp_token" param

$mp_cookie = $_COOKIE['mp_cookie'];
$mp_token = $_GET['mp_token'];

if (isset($_GET['mp_token'])) {
  if (strlen($mp_token) == 0 ) {
    setcookie('mp_cookie', 'clear', time() - 3600);
  } else {
    setcookie('mp_cookie', $mp_token, 60 * 60 * 24 * 60 + time());
  }
}
if (isset($_GET['mp_location'])) {
  header( 'Location: ' . $_GET['mp_location'] ) ;
} else {
  $im = imagecreatetruecolor(300, 100);
  if (isset($_COOKIE['mp_cookie'])) {
    if ($mp_cookie==$mp_token || !isset($_GET['mp_token'])) {
      // GREEN - cookie set and matches
      imagefilledrectangle($im, 0, 0, 299, 99, 0x1BA608);
    } else {
      // YELLOW - cookie being reset or cleared
      imagefilledrectangle($im, 0, 0, 299, 99, 0xE8E841);
    }
  } else {
    // RED - no cookie set
    imagefilledrectangle($im, 0, 0, 299, 99, 0xFF0000);
  }
  imagestring($im, 3, 20, 15, 'mp_cookie: ' . $mp_cookie, 0xFFFFFF);
  imagestring($im, 3, 20, 45, 'mp_token: ' . $mp_token, 0xFFFFFF);
  imagestring($im, 3, 20, 75, 'time: ' . date('Y-m-d H:i:s'), 0xFFFFFF);
  header('Content-Type: image/gif');
  imagegif($im);
  imagedestroy($im);
}

?>
