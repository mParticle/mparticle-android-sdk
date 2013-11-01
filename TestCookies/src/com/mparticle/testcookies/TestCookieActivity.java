package com.mparticle.testcookies;

import java.lang.reflect.Method;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;

import com.mparticle.MParticleAPI;
import com.mparticle.MParticleAPI.EventType;
import com.mparticle.MParticleJSInterface;

public class TestCookieActivity extends Activity {

	WebView mWebView;
	WebViewClient mWebViewClient;
    protected MParticleAPI mParticleAPI;
    CookieManager mCookieMgr;
    MParticleJSInterface mJSInterface;
    
    static final String PARTICLE_APP_KEY = "1615d52f0d2ba44fb1125051c61ea87d";
    static final String PARTICLE_APP_SECRET = "Ay8AtMxEjUQB07xSwA270rWMX_CLGSP6l0zFJikZ71ccRZXmZp1PHeDvzgAGJOnF";
    static final String PARTICLE_BASE_URL = "api-qa.mparticle.com"; //"sdk.dev.aws.mparticle.com";
    
    static final String TEST_URL = "https://api-qa.mparticle.com/TestCookieHelper.aspx?action=write&cookie=myCookie&domain=.mparticle.com&value=myCookieValue&expiration=1383000000000";
    static final String kMParticleWebViewSdkUrl = "mp-sdk://";
	
	Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
				case 0: 
					if (msg.obj != null) {
						String newurl = msg.obj.toString();
						
						mWebView.loadUrl(newurl);
					}
					break;
				case 1:
					// did the cookie get stored?
					if (mCookieMgr.hasCookies()) {
						((TextView)findViewById(R.id.text)).setText("View now has cookie: "+mCookieMgr.getCookie(".mparticle.com"));
					} else {
						((TextView)findViewById(R.id.text)).setText("No cookies were stored");
					}
					Message newmsg = obtainMessage(2);
					sendMessageDelayed(newmsg, 5000L);
					break;
				case 2:
					// load a page to test the javascript
					mWebView.loadUrl("file:///android_asset/Test.html");
					break;		
			}
		}
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_test_cookie);
        initializeMParticleAPI();
		
		mWebView = (WebView)findViewById(R.id.webview);
		WebSettings webSettings = mWebView.getSettings();
		webSettings.setJavaScriptEnabled(true);
		
		mCookieMgr = CookieManager.getInstance();
		if (mCookieMgr.hasCookies()) {
			String cookie = mCookieMgr.getCookie(".mparticle.com");
			if (cookie != null) {
				((TextView)findViewById(R.id.text)).setText("View already had cookie: "+cookie);
				mCookieMgr.removeAllCookie();
			}
		}
		mJSInterface = new MParticleJSInterface(this, mParticleAPI);
		mWebView.addJavascriptInterface(mJSInterface, "mParticleAndroid"); 
			
		mWebView.setWebViewClient( mWebViewClient = new WebViewClient() {
			
			@Override
			public void onPageFinished(WebView view, final String url) {
				super.onPageFinished(view, url);
				if (mParticleAPI != null) 
					mParticleAPI.logEvent("Page loaded from "+url, EventType.UserContent);
				Message msg = handler.obtainMessage(1);
				handler.sendMessageDelayed(msg, 1000L);
			}
			@Override
			public void onReceivedError(WebView view, int errorCode,
					String description, String failingUrl) {
				super.onReceivedError(view, errorCode, description, failingUrl);
				if (mParticleAPI != null) 
					mParticleAPI.logEvent("Error("+errorCode+" - "+description+") loading from "+failingUrl, EventType.UserContent);
			}

		});
		
		Message msg = handler.obtainMessage(0, TEST_URL);
		handler.sendMessage(msg);
	}
		
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.test_cookie, menu);
		return true;
	}

    
    @Override
    protected void onStart() {
    	super.onStart();
    	if (mParticleAPI != null) {
    		mParticleAPI.newSession();	// explicitly start a new session in onStart
    		mParticleAPI.startActivity();
    	}
    }
    
    @Override
    protected void onStop() {
    	super.onStop();
    	if (mParticleAPI != null) {
    		mParticleAPI.stopActivity();
    	}
    }
    
    protected void initializeMParticleAPI() {
    	boolean hasPropFile = false;
    	// is there a config file?
    	try {
    		getResources().getAssets().open("mparticle.properties").close();
        	// first time
        	mParticleAPI = MParticleAPI.getInstance(this);
        	hasPropFile = true;
    	} catch(Exception e) {
    		// if failed, then use explicit getinstance
        	// first time
        	mParticleAPI = MParticleAPI.getInstance(this, PARTICLE_APP_KEY, PARTICLE_APP_SECRET);
    	}
    	try {
    		getResources().getAssets().open("mparticle.properties").close();
        	mParticleAPI = MParticleAPI.getInstance(this);
        	hasPropFile = true;
    	} catch(Exception e) {
    		// if failed, then use explicit getinstance
        	mParticleAPI = MParticleAPI.getInstance(this, PARTICLE_APP_KEY, PARTICLE_APP_SECRET);
    	}
	    if (mParticleAPI != null) {
	    	try {
	    		// force the debug endpoint
	        	Method privateStringMethod = mParticleAPI.getClass().getDeclaredMethod("setServiceHost", String.class);
	        	privateStringMethod.setAccessible(true);
	        	privateStringMethod.invoke(mParticleAPI, PARTICLE_BASE_URL);
//		    	Method privateBooleanMethod = mParticleAPI.getClass().getDeclaredMethod("setSecureTransport", boolean.class);
//		    	privateBooleanMethod.setAccessible(true);
//		    	privateBooleanMethod.invoke(mParticleAPI, false);
	    	} catch( Exception e ) {
	    		e.printStackTrace();
	    		mParticleAPI = null;
	    	}
	    }
	}

}
