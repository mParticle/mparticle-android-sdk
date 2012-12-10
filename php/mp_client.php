<html>
<head>
<title>mParticle Browser Integration</title>
<style>
ul li a { line-height: 2em; }
</style>
</head>
<body>
<h1>
<img style="float:right;" src="mparticle.png">
mParticle Browser Integration</h1>
<p>This page demonstrates how the mParticle SDK can be integrated with the user's mobile browser to share cookies and integrate with the mParticle server</p>
<p></p>

<h2>Pixel Beacon</h2>
<p>This large image would be replaced by a single invisible pixel served by mparticle.com servers and embedded from a partner web site. The image source URL takes a single parameter "mp_token" which it sets as a cookie. For now, this is all on a single domain but it would work cross-domain.</p>
<ul>
<li>GREEN - mParticle has received the cookie</li>
<li>YELLOW - The token and cookie do not match. The cookie is being set.</li>
<li>RED - No cookie has been received</li>
</ul>
<?php
$pixel_path = "/mp/mp.php?time=". time() . ( isset($_GET['mp_token']) ? "&mp_token=".$_GET['mp_token'] : "");
?>
<a href="mp_client.php"><img src="<?php echo $pixel_path; ?>" style="border: 1px solid black;" width="100%"></a>
<br/>
<i>Image source: <?php echo $pixel_path; ?></i>

<h2>Status Page Links</h2>
<ul>
<li><a href="mp_client.php">Load page with no params</a></li>
<li><a href="mp_client.php?mp_token=abc">Load page with mp_token=abc</a></li>
<li><a href="mp_client.php?mp_token=abcde">Load page with mp_token=abcde</a></li>
<li><a href="mp_client.php?mp_token=">Load page with mp_token=</a> (clear the cookie)</li>
</ul>

<h2>Launch App Directly</h2>
<ul>
<li><a href="mparticledemo://testUri?test_arg=test_val_1">Custom URI scheme:</a> mparticledemo://testUri?test_arg=test_val_1</li>
<li><a href="http://demo.mparticle.com/testreferrer/abc/123?def=ghi">Http URI (android only):</a> http://demo.mparticle.com/testreferrer/abc/123?def=ghi</li>
</ul>

<h2>Set Cookie and Launch App on redirect</h2>
<ul>
<li><a href="mp.php?mp_token=abc&mp_location=mparticledemo://testUri?test_arg=test_val_1">Custom URI scheme:</a> mparticledemo://testUri?test_arg=test_val_1</li>
<li><a href="mp.php?mp_token=abc&mp_location=<?php echo urlencode('http://demo.mparticle.com/testreferrer/abc/123?def=ghi'); ?>">Http URI (android only):</a> http://demo.mparticle.com/testreferrer/abc/123?def=ghi</li>
</ul>

</body>
</html>
