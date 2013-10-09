package com.mparticle.hello;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Vector;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;

import com.mparticle.MParticleAPI.EventType;
import com.mparticle.hello.musicplayer.MusicService;

public class WebServiceActivity extends BaseActivity {

	private static final String TAG = WebServiceActivity.class.getSimpleName();
	
	Button mStartStop;
	Button mNewUrl;
	WebView mWebView;
	WebViewClient mWebViewClient;
	ArrayList<String> mBeenThereBefore;
	
	Vector<ArrayList<String>> mHistoryStack;
	String mDefaultTopLevelUrl;
	boolean mRunning;
	boolean mbEnableCrawl = true;
	String mLastUrlLoaded;
	
	Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
				case 0: 
					if (mbEnableCrawl) {
						mWebView.loadUrl(msg.obj.toString());
					}
					break;
			}
		}
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_webservice);
		mStartStop = (Button)findViewById(R.id.btn_start_stop);
		mStartStop.setOnClickListener( new OnClickListener() {
			@Override
			public void onClick(View vw) {
				if ((mParticleAPI != null) && (smMParticleAPIEnabled != null) && smMParticleAPIEnabled) 
					mParticleAPI.logEvent("SDK Start/Stop Pressed", EventType.ACTION);
				mRunning = !mRunning;
				initializeMParticleAPI(); // make sure the api is initialized
				if (mRunning) {
					mStartStop.setText(R.string.btn_stop);
					smMParticleAPIEnabled = Boolean.valueOf(true);
				} else {
					mStartStop.setText(R.string.btn_start);
					smMParticleAPIEnabled = Boolean.valueOf(false);
				}
			}
		});
		mNewUrl = (Button)findViewById(R.id.btn_geturl);
		mNewUrl.setOnClickListener( new OnClickListener() {

			@Override
			public void onClick(View v) {
				if ((mParticleAPI != null) && (smMParticleAPIEnabled != null) && smMParticleAPIEnabled) 
					mParticleAPI.logEvent("Opening URL dialog", EventType.ACTION);
				showUrlDialog();
			}
			
		});
		mRunning = true;
		mStartStop.setText(R.string.btn_stop);
		
		if ((mParticleAPI != null) && (smMParticleAPIEnabled != null) && smMParticleAPIEnabled) 
        	mParticleAPI.logScreenView("WebServiceActivity");
		
		mWebView = (WebView)findViewById(R.id.webview);
		mWebView.setWebViewClient( mWebViewClient = new WebViewClient() {
			
			@Override
			public void onPageFinished(WebView view, final String url) {
				mLastUrlLoaded = url;
				Log.i(TAG, "Page loaded from: "+url);
				super.onPageFinished(view, url);
				if ((mParticleAPI != null) && (smMParticleAPIEnabled != null) && smMParticleAPIEnabled) 
					mParticleAPI.logEvent("Page loaded from "+url, EventType.ACTION);
				Thread t = new Thread() {
					@Override
					public void run() {
						if (!crawlThisPageAndGoDown(url)) {
							// unsuccessful finding any child links
							loadNextSibling();
						}
					}
				};
				t.start();
			}
			@Override
			public void onReceivedError(WebView view, int errorCode,
					String description, String failingUrl) {
				super.onReceivedError(view, errorCode, description, failingUrl);
				if ((mParticleAPI != null) && (smMParticleAPIEnabled != null) && smMParticleAPIEnabled) 
					mParticleAPI.logEvent("Error("+errorCode+" - "+description+") loading from "+failingUrl, EventType.ACTION);
				// pop the stack and continue
				loadNextParentSibling();
			}
		});
		
		mDefaultTopLevelUrl = getString(R.string.default_crawl_url);
		mBeenThereBefore = null;
		
		Message msg = handler.obtainMessage(0, mDefaultTopLevelUrl);
		handler.sendMessage(msg);
	}
	
	/** 
	 * Shows an alert dialog where the user can input a URL. After showing the dialog, if the user
	 * confirms, sends the appropriate intent to the {@link MusicService} to cause that URL to be
	 * played.
	 */
	void showUrlDialog() {
		mbEnableCrawl = false;
		
		AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this);
		alertBuilder.setTitle(getString(R.string.manual_entry));
		alertBuilder.setMessage(getString(R.string.enter_url));
		final EditText input = new EditText(this);
		alertBuilder.setView(input);

		input.setText(getString(R.string.default_crawl_url));

		alertBuilder.setPositiveButton(getString(R.string.choose_go), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dlg, int whichButton) {
				if ((mParticleAPI != null) && (smMParticleAPIEnabled != null) && smMParticleAPIEnabled) {
					mParticleAPI.logEvent("Load new URL Dialog Pressed", EventType.ACTION);
				}
				mDefaultTopLevelUrl = input.getText().toString();
				mHistoryStack.clear();
				mBeenThereBefore = null;
				mbEnableCrawl = true;
				Message msg = handler.obtainMessage(0, mDefaultTopLevelUrl);
				handler.sendMessage(msg);
			}
		});
		alertBuilder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dlg, int whichButton) {
				if ((mParticleAPI != null) && (smMParticleAPIEnabled != null) && smMParticleAPIEnabled) {
					mParticleAPI.logEvent("Cancel from URL Dialog Pressed", EventType.ACTION);
					mbEnableCrawl = true;
					// if cancel, pick up where left off
					Thread t = new Thread() {
						@Override
						public void run() {
							if (!crawlThisPageAndGoDown(mLastUrlLoaded)) {
								// unsuccessful finding any child links
								loadNextSibling();
							}
						}
					};
					t.start();
				}
			}
		});

		alertBuilder.show();
	}

	// page crawler
	private ArrayList<String> getListOfUrls(String newurl) {
		// this was just loaded into the WebView, so should be fast (cached)
	    String webpage = "";
        String inputLine = "";
        ArrayList<String> newList = null;
        try {
	        URL url = new URL(newurl);
	        BufferedReader in = new BufferedReader(
	        new InputStreamReader(url.openStream()));
	        while ((inputLine = in.readLine()) != null)
	        webpage += inputLine;
	        in.close();
	        
	        int end = webpage.indexOf("<body");
	        
	        while (true) {
		        int start = webpage.indexOf("http://", end);
		        if (start == -1) {
		        	return newList;
		        }
		        end = webpage.indexOf("\"", start);
		        int tmp = webpage.indexOf("'", start);
	            if(tmp < end && tmp != -1){
	                end = tmp;
	            }
		        String aUrl = webpage.substring(start, end);
		        if (newList == null) newList = new ArrayList<String>();
		        if (mBeenThereBefore == null) mBeenThereBefore = new ArrayList<String>();
		        if (!mBeenThereBefore.contains(aUrl)) {
		        	newList.add(aUrl);
		        	mBeenThereBefore.add(aUrl);
		        }
        	}
        } catch (Exception e) {
			if ((mParticleAPI != null) && (smMParticleAPIEnabled != null) && smMParticleAPIEnabled) {
				mParticleAPI.logEvent("Exception when trying to crawl: "+newurl, EventType.ACTION);
			}
        	
        }
		return newList;
	}
	
	// history stack
	private boolean crawlThisPageAndGoDown(String url) {
		// create a new arraylist of urls from links
		// add it to the history stack
		// pick 1st one and load it
		ArrayList<String> newUrls = getListOfUrls(url);
		if (mHistoryStack == null) mHistoryStack = new Vector<ArrayList<String>>();
		if ((newUrls != null) && (newUrls.size() > 0) ) {
			mHistoryStack.add(0, newUrls);
			removeAndLoad();
			return true;
		}
		return loadNextSibling(); // can't do it, continue at same level
	}

	private boolean loadNextSibling() {
		// get the next url from the arraylist
		// if none, then pop the stack one and continue there
		// if is another url, then load that
		if ((mHistoryStack == null) || (mHistoryStack.size() == 0)) {
			// start over at the top
			mBeenThereBefore = null;
			Message msg = handler.obtainMessage(0, mDefaultTopLevelUrl);
			handler.sendMessageDelayed(msg, 10);
			return true;
		}
		if ((mHistoryStack.get(0) == null) || (mHistoryStack.get(0).size() == 0)) {
			return loadNextParentSibling();
		}
		removeAndLoad();
		return true;
	}
	
	private boolean loadNextParentSibling() {
		// remove current level, pop to previous
		if ((mHistoryStack == null) || (mHistoryStack.size() == 0)) {
			// start over at the top
			mBeenThereBefore = null;
			Message msg = handler.obtainMessage(0, mDefaultTopLevelUrl);
			handler.sendMessageDelayed(msg, 10);
			return true;
		}
		mHistoryStack.remove(0);
		if ((mHistoryStack.size() == 0) || (mHistoryStack.get(0) == null) || (mHistoryStack.get(0).size() == 0)) {
			return loadNextParentSibling();
		}
		removeAndLoad();
		return true;
	}
	
	private void removeAndLoad() {
		String nextSibling = mHistoryStack.get(0).get(0);
		// remove the sibling from the list
		ArrayList<String> list = mHistoryStack.get(0);
		list.remove(0);
		mHistoryStack.remove(0);
		mHistoryStack.add(0, list);
		Message msg = handler.obtainMessage(0, nextSibling);
		handler.sendMessageDelayed(msg, 10);
	}

}
