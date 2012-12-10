<?php
// This generates a gif with the background color set 
// based on the value of an "mp_cookie" cookie and some 
// info about the params and current state
// It also sets or clears the cookie based on the "mp_token" param

$mp_cookie = $_COOKIE['mp_cookie'];
$mp_token = $_GET['mp_token'];
$agent = $_SERVER['HTTP_USER_AGENT'];

$WIDTH = 400;
$HEIGHT = 100;

if (isset($_GET['mp_token'])) {
  if (strlen($mp_token) == 0 ) {
    setcookie('mp_cookie', 'clear', time() - 3600);
  } else {
    setcookie('mp_cookie', $mp_token, 60 * 60 * 24 * 60 + time());
  }
}
if (isset($_GET['mp_location'])) {
  if(preg_match('/AppleWebKit/i',$agent)){
    echo "<script>window.location='" . $_GET['mp_location'] . "';</script>";
  } else {
    header( 'Location: ' . $_GET['mp_location'] ) ;
  }
} else {
  $im = imagecreatetruecolor($WIDTH, $HEIGHT);
  if (isset($_COOKIE['mp_cookie'])) {
    if ($mp_cookie==$mp_token || !isset($_GET['mp_token'])) {
      // GREEN - cookie set and matches
      imagefilledrectangle($im, 0, 0, $WIDTH-1 , $HEIGHT-1 , 0x1BA608);
    } else {
      // YELLOW - cookie being reset or cleared
      imagefilledrectangle($im, 0, 0, $WIDTH-1 , $HEIGHT-1 , 0xDEDE18);
    }
  } else {
    // RED - no cookie set
    imagefilledrectangle($im, 0, 0, $WIDTH-1 , $HEIGHT-1 , 0xFF0000);
  }
  imagestring($im, 3, 20, 15, 'mp_cookie: ' . $mp_cookie, 0xFFFFFF);
  imagestring($im, 3, 20, 45, 'mp_token: ' . $mp_token, 0xFFFFFF);
  imagestring($im, 3, 20, 75, 'time: ' . date('Y-m-d H:i:s'), 0xFFFFFF);
  header('Content-Type: image/gif');
  imagegif($im);
  imagedestroy($im);
}
?>
