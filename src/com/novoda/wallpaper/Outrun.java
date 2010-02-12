/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.novoda.wallpaper;

import java.util.Timer;
import java.util.TimerTask;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.os.Handler;
import android.service.wallpaper.WallpaperService;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;

public class Outrun extends WallpaperService {

    @Override
    public void onCreate() {
    	super.onCreate();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public Engine onCreateEngine() {
        return new OutRunEngine();
    }
    
    class OutRunEngine extends Engine {

		OutRunEngine() {
        	Resources res = getResources();
        	int id =0;
        	for (int i = 0; i< FRONT_RES; i++) {
        		id = res.getIdentifier("front_day00" + (i + 1), "drawable", "com.novoda.wallpaper");
        		mFrontPics[i] = BitmapFactory.decodeResource(res, id);
        	}
        	id=0;
        	for (int i = 0; i< LEFT_RES; i++) {
        		id = res.getIdentifier("left_day00" + (i + 1), "drawable", "com.novoda.wallpaper");
        		mLeftPics[i] = BitmapFactory.decodeResource(res, id);
        	}
        	id=0;
        	for (int i = 0; i< RIGHT_RES; i++) {
        		id = res.getIdentifier("right_day00" + (i + 1), "drawable", "com.novoda.wallpaper");
        		mRightPics[i] = BitmapFactory.decodeResource(res, id);
        	}
        }
        
        @Override
        public void onCreate(SurfaceHolder surfaceHolder) {
            super.onCreate(surfaceHolder);
            setTouchEventsEnabled(true);
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            mHandler.removeCallbacks(mDrawWallpaper);
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            mVisible = visible;
            if (visible) {
                drawFrame();
            } else {
                mHandler.removeCallbacks(mDrawWallpaper);
            }
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            super.onSurfaceChanged(holder, format, width, height);
            float w = mFrontPics[0].getWidth();
            float h = mFrontPics[0].getHeight();
            float s = width / (float)w;
            mMatrix.reset();
            mMatrix.setScale(s, s);
            
            mPosY = (height - (h * s)) / 2f;
            drawFrame();
        }

        @Override
        public void onSurfaceCreated(SurfaceHolder holder) {
            super.onSurfaceCreated(holder);
        }

        @Override
        public void onSurfaceDestroyed(SurfaceHolder holder) {
            super.onSurfaceDestroyed(holder);
            mVisible = false;
            mHandler.removeCallbacks(mDrawWallpaper);
        }

        @Override
        public void onOffsetsChanged(float xOffset, float yOffset, float xStep, float yStep, int xPixels, int yPixels) {
//        	Log.i(TAG, "XOffset["+xOffset+"] xStep["+xStep+"] xPixels["+xPixels+"]");
            drawFrame();
        }

        /*
         * Store the position of the touch event so we can use it for drawing later
         */
        @Override
        public void onTouchEvent(MotionEvent event) {
        	
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
            	mDragEventInProgress = true;
            	mDragEventStartX = event.getX();
            }
            
            if (event.getAction() == MotionEvent.ACTION_UP) {
            	boolean draggedLotsRight = (mDragEventStartX - event.getX()) >=160;
            	boolean draggedLotsLeft = (event.getX() - mDragEventStartX) >=160;
            	Log.v(TAG, "X:["+event.getX()+"+] - dragStart["+mDragEventStartX+"] =" + (event.getX() - mDragEventStartX));
            	
				if( (mDragEventStartX > 150) && draggedLotsRight ){
            		takingACorner =true;
            		currentDirection = DRIVING_RIGHT;
            		Log.d(TAG, "Driving animation started Right >");
            		new Timer().schedule(new TimerTask(){
						@Override
						public void run() {
							picIdx =0;
							takingACorner =false;
						}}, 1000);
            	}
            	
            	if( (mDragEventStartX < 150) && draggedLotsLeft ){
            		takingACorner =true;
            		currentDirection = DRIVING_LEFT;
            		Log.d(TAG, "Driving animation started < Left");
            		new Timer().schedule(new TimerTask(){
						@Override
						public void run() {
							picIdx =0;
		            		takingACorner =false;
						}}, 1000);
            	}
            	
            	mDragEventInProgress = false;
            	mDragEventStartX = 0;
            }
            super.onTouchEvent(event);
        }

        
        void drawFrame() {
            final SurfaceHolder holder = getSurfaceHolder();
            Canvas c = null;
            try {
                c = holder.lockCanvas();
                if (c != null) {
                	c.save();
                	drawCar(c);
                    c.restore();
                }
            } finally {
                if (c != null) holder.unlockCanvasAndPost(c);
            }

            // Reschedule the next redraw
            mHandler.removeCallbacks(mDrawWallpaper);
            if (mVisible) {
                mHandler.postDelayed(mDrawWallpaper, 1000 / 50);
            }
        }

        private void drawCar(Canvas c) {
    		
        	if(takingACorner){
        		if(currentDirection == DRIVING_RIGHT){
	        		drawAnim(c, mRightPics);        			
        		}else{
	        		drawAnim(c, mLeftPics);
        		}
    		}else{
	        	if(!mDragEventInProgress){
	        		drawAnim(c, mFrontPics);
	        	}else{
/*
 * Uncomment this to respond 
 * to all onscreen touch events 	        		
 *
 *	        		if(mDragEventStartX > 150){
 *	        			drawAnimRight(c, mLeftPics);
 *	        		}else{
 *						drawAnim(c, mLeftPics);
 *	        		}
 */	        	
	        	}
    		}
		}

        void drawAnim(Canvas c, Bitmap[] pics) {
        	c.drawBitmap(pics[picIdx], mMatrix, mPaint);
        	++picIdx;
        	if (picIdx == FRONT_RES) picIdx = 0;
        }

        private int picIdx = 0;
		private final Paint mPaint = new Paint();
		private float mTouchX = -1;
		private float mTouchY = -1;
		private int currentDirection = DRIVING_FORWARD;
		private static final int DRIVING_FORWARD = 5678;
		private static final int DRIVING_LEFT = 9876;
		private static final int DRIVING_RIGHT = 234;
		private boolean mDragEventInProgress = false;
		private float mDragEventStartX = 0;
		private boolean mVisible;
		private float mPosY;
		private boolean takingACorner = false;
		private Matrix mMatrix = new Matrix();
		private static final int FRONT_RES = 3;
		private static final int LEFT_RES = 5;
		private static final int RIGHT_RES = 5;
		private final Bitmap[] mFrontPics = new Bitmap[FRONT_RES];
		private final Bitmap[] mRightPics = new Bitmap[RIGHT_RES];
		private final Bitmap[] mLeftPics = new Bitmap[LEFT_RES];

		private final Runnable mDrawWallpaper = new Runnable() {
		    public void run() {
		        drawFrame();
		    }
		};
    }

	private final Handler mHandler = new Handler();
	private static final String TAG = Outrun.class.getSimpleName();
}
