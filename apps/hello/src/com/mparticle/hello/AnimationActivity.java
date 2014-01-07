package com.mparticle.hello;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.content.Context;
import android.content.Intent;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.FrameLayout;

import com.mparticle.MParticleAPI.EventType;

public class AnimationActivity extends BaseActivity {

	Button mStartStop;
	Button mSend10;
	Button mSend100;
	Button mSend500;
	Button mNext;

	boolean mRunning;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_animation);
		mStartStop = (Button)findViewById(R.id.btn_start_stop);
		mStartStop.setOnClickListener( new OnClickListener() {
			@Override
			public void onClick(View vw) {
				if ((mParticleAPI != null) && (smMParticleAPIEnabled != null) && smMParticleAPIEnabled) 
					mParticleAPI.logEvent("SDK Start/Stop Pressed", EventType.UserContent);
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
		mSend10 = (Button)findViewById(R.id.btn_send10);
		mSend10.setOnClickListener( new OnClickListener() {
			@Override
			public void onClick(View vw) {
				if ((mParticleAPI != null) && (smMParticleAPIEnabled != null) && smMParticleAPIEnabled) 
					mParticleAPI.logEvent("Send10 Pressed", EventType.UserContent);
				sendLog(10);
			}
		});
		mSend100 = (Button)findViewById(R.id.btn_send100);
		mSend100.setOnClickListener( new OnClickListener() {
			@Override
			public void onClick(View vw) {
				if ((mParticleAPI != null) && (smMParticleAPIEnabled != null) && smMParticleAPIEnabled) 
					mParticleAPI.logEvent("Send100 Pressed", EventType.UserContent);
				sendLog(100);
			}
		});
		mSend500 = (Button)findViewById(R.id.btn_send500);
		mSend500.setOnClickListener( new OnClickListener() {
			@Override
			public void onClick(View vw) {
				if ((mParticleAPI != null) && (smMParticleAPIEnabled != null) && smMParticleAPIEnabled) 
					mParticleAPI.logEvent("Send500 Pressed", EventType.UserContent);
				sendLog(500);
			}
		});
		mNext = (Button)findViewById(R.id.btn_next);
		mNext.setOnClickListener( new OnClickListener() {
			@Override
			public void onClick(View vw) {
				if ((mParticleAPI != null) && (smMParticleAPIEnabled != null) && smMParticleAPIEnabled) 
					mParticleAPI.logEvent("Next Button Pressed", EventType.Navigation);
				Intent intent = new Intent(AnimationActivity.this, PerformanceActivity.class);
				startActivity(intent);
			}
		});
		mRunning = true;
		mStartStop.setText(R.string.btn_stop);

		// set opengl surface
		initSurface();
		
//		if ((mParticleAPI != null) && (smMParticleAPIEnabled != null) && smMParticleAPIEnabled) 
//        	mParticleAPI.logScreenView("AnimationActivity");
	}

	private void sendLog(int n) {
		if ((mParticleAPI != null) && (smMParticleAPIEnabled != null) && smMParticleAPIEnabled) {
			for (int i=0; i<n; i++) {
				mParticleAPI.logEvent("AutoLog"+(i+1)+"Of"+n, EventType.UserContent);
			}
		}
	}

	private void initSurface() {
		if (mGLSurfaceView == null) {
			mGLSurfaceView = new TouchSurfaceView(this);
			FrameLayout container = (FrameLayout)findViewById(R.id.gl_container);
			container.addView(mGLSurfaceView);
		}
		mGLSurfaceView.requestFocus();
		mGLSurfaceView.setFocusableInTouchMode(true);
	}

	@Override
	protected void onResume() {
		// Ideally a game should implement onResume() and onPause()
		// to take appropriate action when the activity looses focus
		super.onResume();
		mGLSurfaceView.onResume();
	}

	@Override
	protected void onPause() {
		// Ideally a game should implement onResume() and onPause()
		// to take appropriate action when the activity looses focus
		super.onPause();
		mGLSurfaceView.onPause();
	}

	private GLSurfaceView mGLSurfaceView;
}

/**
 * Implement a simple rotation control.
 *
 */
class TouchSurfaceView extends GLSurfaceView {

	static final int NUM_ANIMATING_CUBES = 100;
	AnimationActivity mContext;
	
	public TouchSurfaceView(Context context) {
		super(context);
		mContext = (AnimationActivity)context;
		mRenderer = new CubeRenderer(false);
		setRenderer(mRenderer);
//		setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
	}

	@Override public boolean onTrackballEvent(MotionEvent e) {
		mRenderer.mCube.mAngleX += e.getX() * TRACKBALL_SCALE_FACTOR;
		mRenderer.mCube.mAngleY += e.getY() * TRACKBALL_SCALE_FACTOR;
		requestRender();
		return true;
	}

	@Override public boolean onTouchEvent(MotionEvent e) {
		float x = e.getX();
		float y = e.getY();
		switch (e.getAction()) {
		case MotionEvent.ACTION_MOVE:
			float dx = x - mPreviousX;
			float dy = y - mPreviousY;
			mRenderer.mCube.mAngleX += dx * TOUCH_SCALE_FACTOR;
			mRenderer.mCube.mAngleY += dy * TOUCH_SCALE_FACTOR;
			requestRender();
			if ((mContext.mParticleAPI != null) && (mContext.smMParticleAPIEnabled != null) && mContext.smMParticleAPIEnabled) {
				mContext.mParticleAPI.logEvent("Moving("+x+","+y+")", EventType.UserContent);
			}
		}
		mPreviousX = x;
		mPreviousY = y;
		return true;
	}

	/**
	 * Render a cube.
	 */
	private class CubeRenderer implements GLSurfaceView.Renderer {
		public Cube mCube;
		private Cube[] mAnimatingCubes;
		
	    public CubeRenderer(boolean useTranslucentBackground) {
	        mTranslucentBackground = useTranslucentBackground;
			mCube = new Cube(0.0f, 0.0f, 0.0f, 0.0f, -4.0f, 1.0f, 0.0f, 0.0f);
			mAnimatingCubes = new Cube[NUM_ANIMATING_CUBES];
			for (int i=0; i<mAnimatingCubes.length; i++) {
				mAnimatingCubes[i] = new Cube((float)Math.random(),(float)Math.random(),10.0f*(0.5f-(float)Math.random()),10.0f*(0.5f-(float)Math.random()),-10.0f+7.0f*(float)Math.random(),(float)Math.random(), 0.5f-(float)Math.random(), 0.5f-(float)Math.random());
			}
		}

		public void onDrawFrame(GL10 gl) {

			gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);

			for (int i=-1; i<mAnimatingCubes.length; i++) {
				Cube c = (i>=0)?mAnimatingCubes[i]:mCube;
				
				gl.glMatrixMode(GL10.GL_MODELVIEW);
				gl.glLoadIdentity();
				gl.glTranslatef(c.mTransX, c.mTransY, c.mTransZ); 
				gl.glRotatef(c.mAngleX, 0, 1, 0);
				gl.glRotatef(c.mAngleY, 1, 0, 0);
	
				gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
				gl.glEnableClientState(GL10.GL_COLOR_ARRAY);
	
				c.draw(gl);
				
				c.mAngleX += c.mRateX;
				c.mAngleY += c.mRateY;
			}
		}

		public void onSurfaceChanged(GL10 gl, int width, int height) {
			gl.glViewport(0, 0, width, height);

			/*
			 * Set our projection matrix. This doesn't have to be done
			 * each time we draw, but usually a new projection needs to
			 * be set when the viewport is resized.
			 */

			float ratio = (float) width / height;
			gl.glMatrixMode(GL10.GL_PROJECTION);
			gl.glLoadIdentity();
			gl.glFrustumf(-ratio, ratio, -1, 1, 1, 10);
		}

		public void onSurfaceCreated(GL10 gl, EGLConfig config) {
			/*
			 * By default, OpenGL enables features that improve quality
			 * but reduce performance. One might want to tweak that
			 * especially on software renderer.
			 */
			gl.glDisable(GL10.GL_DITHER);

			/*
			 * Some one-time OpenGL initialization can be made here
			 * probably based on features of this particular context
			 */
			gl.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT,
					GL10.GL_FASTEST);


			gl.glClearColor(1,1,1,1);
			gl.glEnable(GL10.GL_CULL_FACE);
			gl.glShadeModel(GL10.GL_SMOOTH);
			gl.glEnable(GL10.GL_DEPTH_TEST);
		}
	}

	private final float TOUCH_SCALE_FACTOR = 180.0f / 320;
	private final float TRACKBALL_SCALE_FACTOR = 36.0f;
	private CubeRenderer mRenderer;
	private float mPreviousX;
	private float mPreviousY;
	private boolean mTranslucentBackground;
	
	/**
	 * A vertex shaded cube.
	 */
	class Cube
	{
		public float mAngleX;
		public float mAngleY;
		public float mTransX;
		public float mTransY;
		public float mTransZ;
		public float mRateX;
		public float mRateY;
		public float mScale;
		
	    private IntBuffer   mVertexBuffer;
	    private IntBuffer   mColorBuffer;
	    private ByteBuffer  mIndexBuffer;
		
	    public Cube(float angX, float angY, float transX, float transY, float transZ, float scale, float ratex, float ratey)
	    {
	    	mAngleX = angX;
	    	mAngleY = angY;
	    	mTransX = transX;
	    	mTransY = transY;
	    	mTransZ = transZ;
	    	mScale = scale;
	    	mRateX = ratex;
	    	mRateY = ratey;
	    	
	    	int one = 0x10000;
	        int oneX = (int)(0x10000 * scale);
	        int oneY = (int)(0x10000 * scale);
	        int oneZ = (int)(0x10000 * scale);

	        int vertices[] = {
	                -oneX, -oneY, -oneZ,
	                oneX, -oneY, -oneZ,
	                oneX,  oneY, -oneZ,
	                -oneX,  oneY, -oneZ,
	                -oneX, -oneY,  oneZ,
	                oneX, -oneY,  oneZ,
	                oneX,  oneY,  oneZ,
	                -oneX,  oneY,  oneZ,
	        };

	        int colors[] = {
	                0,    0,    0,  one,
	                one,    0,    0,  one,
	                one,  one,    0,  one,
	                0,  one,    0,  one,
	                0,    0,  one,  one,
	                one,    0,  one,  one,
	                one,  one,  one,  one,
	                0,  one,  one,  one,
	        };

	        byte indices[] = {
	                0, 4, 5,    0, 5, 1,
	                1, 5, 6,    1, 6, 2,
	                2, 6, 7,    2, 7, 3,
	                3, 7, 4,    3, 4, 0,
	                4, 7, 6,    4, 6, 5,
	                3, 0, 1,    3, 1, 2
	        };

	        // Buffers to be passed to gl*Pointer() functions
	        // must be direct, i.e., they must be placed on the
	        // native heap where the garbage collector cannot
	        // move them.
	        //
	        // Buffers with multi-byte datatypes (e.g., short, int, float)
	        // must have their byte order set to native order

	        ByteBuffer vbb = ByteBuffer.allocateDirect(vertices.length*4);
	        vbb.order(ByteOrder.nativeOrder());
	        mVertexBuffer = vbb.asIntBuffer();
	        mVertexBuffer.put(vertices);
	        mVertexBuffer.position(0);

	        ByteBuffer cbb = ByteBuffer.allocateDirect(colors.length*4);
	        cbb.order(ByteOrder.nativeOrder());
	        mColorBuffer = cbb.asIntBuffer();
	        mColorBuffer.put(colors);
	        mColorBuffer.position(0);

	        mIndexBuffer = ByteBuffer.allocateDirect(indices.length);
	        mIndexBuffer.put(indices);
	        mIndexBuffer.position(0);
	    }

	    public void draw(GL10 gl)
	    {
	        gl.glFrontFace(gl.GL_CW);
	        gl.glVertexPointer(3, gl.GL_FIXED, 0, mVertexBuffer);
	        gl.glColorPointer(4, gl.GL_FIXED, 0, mColorBuffer);
	        gl.glDrawElements(gl.GL_TRIANGLES, 36, gl.GL_UNSIGNED_BYTE, mIndexBuffer);
	    }

	}
}

