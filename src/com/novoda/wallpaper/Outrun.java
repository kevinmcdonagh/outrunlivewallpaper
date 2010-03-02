package com.novoda.wallpaper;

import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import android.app.WallpaperManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Handler;
import android.service.wallpaper.WallpaperService;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;

public class Outrun extends WallpaperService {

	public static final int TIME_PERIOD_DAY = 84521;
	public static final int TIME_PERIOD_SUNSET = 878651;
	public static final int TIME_PERIOD_NIGHT = 35664;
	
	private final Handler mHandler = new Handler();
	
	WallpaperManager wallpaperMgr;
	
	@Override
    public void onCreate() {
    	super.onCreate();
    	wallpaperMgr = WallpaperManager.getInstance(this);
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

    	/*
    	 * All IDs of resources needed for the animations
    	 * are stored to cycle through in drawAnim().
    	 * A picIndx is stored and iterated through.
    	 * 
    	 */
		OutRunEngine() {
        	Resources res = getResources();
        	currSceneOfDay = Utils.currentPeriodOfDay();
        	
        	for (int i = 0; i< TOTAL_FRONT_RES; i++) {
        		mFrontPicIds[i] = res.getIdentifier("car_front_day" + String.format("%03d", i), "drawable", "com.novoda.wallpaper");
        	}
        	for (int i = 0; i< TOTAL_LEFT_RES; i++) {
        		mLeftPicIds[i] = res.getIdentifier("car_left_day" + String.format("%03d", i), "drawable", "com.novoda.wallpaper");
        	}
        	for (int i = 0; i< TOTAL_RIGHT_RES; i++) {
        		mRightPicIds[i] = res.getIdentifier("car_right_day" + String.format("%03d", i), "drawable", "com.novoda.wallpaper");
        	}
        	for (int i = 0; i< TOTAL_DAY_RES; i++) {
        		mDayPicIds[i] = res.getIdentifier("horiz_day" + String.format("%03d", i), "drawable", "com.novoda.wallpaper");
        	}
        	for (int i = 0; i< TOTAL_SUNSET_RES; i++) {
        		mSunsetPicIds[i] = res.getIdentifier("horiz_sunset" + String.format("%03d", i), "drawable", "com.novoda.wallpaper");
        	}
        	for (int i = 0; i< TOTAL_NIGHT_RES; i++) {
        		mNightPicIds[i] = res.getIdentifier("horiz_night" + String.format("%03d", i), "drawable", "com.novoda.wallpaper");
        	}
        }
        
        @Override
        public void onCreate(SurfaceHolder surfaceHolder) {
            super.onCreate(surfaceHolder);
            setTouchEventsEnabled(true);
            
            currBGIdx = getNewBgInxForPeriod(currSceneOfDay);
        }

		@Override
        public void onDestroy() {
            super.onDestroy();
            mHandler.removeCallbacks(mDrawWallpaper);
        }

        /*
         * Scene Background Timings
         * -------------------------
         * Sunrise: 5am-9am 
         * Day: 9am-5pm
         * Sunset: 5pm-7pm
         * Night: 7pm-5am
         * 
         * Checks if it's time to change the scenery
         * Bounds checking is incase there are different
         * amount of BG resources for the period of day.
         * 
         */
        @Override
        public void onVisibilityChanged(boolean visible) {
            mVisible = visible;
            
            if (visible) {
            	currSceneOfDay = Utils.currentPeriodOfDay();
            	currBGIdx = getNewBgInxForPeriod(currSceneOfDay);
            	drawFullFrame();
            } else {
                mHandler.removeCallbacks(mDrawWallpaper);
            }
        }

        @Override
        public void onSurfaceDestroyed(SurfaceHolder holder) {
            super.onSurfaceDestroyed(holder);
            mVisible = false;
            mHandler.removeCallbacks(mDrawWallpaper);
        }

        /*
         * Touch events check if a user has dragged their finger
         * over the halfway vertical of the screen.
         * 	
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
            		currAnimDirection = DRIVING_RIGHT;
            		Log.d(TAG, "Driving animation started Right >");
            		new Timer().schedule(new TimerTask(){
						@Override
						public void run() {
							picIdx =0;
							currAnimDirection = DRIVING_FORWARD;
						}}, 1000);
            	}
            	
            	if( (mDragEventStartX < 150) && draggedLotsLeft ){
            		currAnimDirection = DRIVING_LEFT;
            		Log.d(TAG, "Driving animation started < Left");
            		new Timer().schedule(new TimerTask(){
						@Override
						public void run() {
							picIdx =0;
							currAnimDirection = DRIVING_FORWARD;
						}}, 1000);
            	}
            	
            	mDragEventInProgress = false;
            	mDragEventStartX = 0;
            }
            super.onTouchEvent(event);
        }

        private int getNewBgInxForPeriod(int currPeriodOfDay) {
			Random rand = new Random();
			switch(currPeriodOfDay){
		    	case TIME_PERIOD_DAY:
		    		return rand.nextInt(TOTAL_DAY_RES);
		    	case TIME_PERIOD_SUNSET:
		    		return rand.nextInt(TOTAL_SUNSET_RES);
		    	case TIME_PERIOD_NIGHT:
		    		return rand.nextInt(TOTAL_NIGHT_RES);
			}
			
			return 0;
		}

        /*
         * Invalidates full canvas. 
         */
		void drawFullFrame() {
			final SurfaceHolder holder = getSurfaceHolder();
            Canvas c = null;
            try {
                c = holder.lockCanvas();
                if (c != null) {
                	c.save();
               		drawHorizon(c);
               		drawBottomScreenPadding(c);                		
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

        /*
         * Invalidates car animation 
         */
		void drawCarAndRoad() {
			final SurfaceHolder holder = getSurfaceHolder();
			Canvas c = null;
			try {
				c = holder.lockCanvas(new Rect(0, 549, 480, 700));
				if (c != null) {
					c.save();
               		drawHorizon(c);
               		drawBottomScreenPadding(c);                		
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
        	if(currAnimDirection != DRIVING_FORWARD){
        		if(currAnimDirection == DRIVING_RIGHT){
	        		drawAnim(c, mRightPicIds, TOTAL_RIGHT_RES, 549);        			
        		}else{
	        		drawAnim(c, mLeftPicIds, TOTAL_LEFT_RES, 549);
        		}
    		}else{
	        	if(!mDragEventInProgress){
	        		drawAnim(c, mFrontPicIds, TOTAL_FRONT_RES, 549);
	        	}
    		}
		}
        
        private void drawHorizon(Canvas c){
        	int resId=0, bgId=0;
        	switch(currSceneOfDay){
        	case TIME_PERIOD_DAY:
        		resId=mDayPicIds[currBGIdx];
        		bgId=mDayColourBGIds[currBGIdx];
        		break;
        	case TIME_PERIOD_SUNSET:
        		resId=mSunsetPicIds[currBGIdx];
        		bgId=mSunsetColourBGIds[currBGIdx];
        		break;
        	case TIME_PERIOD_NIGHT:
        		resId=mNightPicIds[currBGIdx];
        		bgId=mNightColourBGIds[currBGIdx];
        		break;
        	}
        	
        	Paint mBackgroundPaint = new Paint();
        	mBackgroundPaint.setColor(getResources().getColor(bgId));
        	mBackgroundPaint.setStyle(Paint.Style.FILL);
        	Rect mBackgroundRect = new Rect();
        	mBackgroundRect.set(0, 0, 480, 325);
        	c.drawRect(mBackgroundRect, mBackgroundPaint);
        	
        	c.drawBitmap(BitmapFactory.decodeResource(getResources(), resId), 0, 325, null);
        }
        
        private void drawBottomScreenPadding(Canvas c){
        	Paint mBackgroundPaint = new Paint();
        	mBackgroundPaint.setStyle(Paint.Style.FILL);
        	Rect mBackgroundRect = new Rect();
        	mBackgroundPaint.setColor(getResources().getColor(R.color.ROAD));
        	mBackgroundRect.set(0, 685, 480, 800);
        	c.drawRect(mBackgroundRect, mBackgroundPaint);
        }
        
        /*
         * Generic animation renderer
         * Rolls over on bounds of array.
         */
        void drawAnim(Canvas c, int[] pics, int totalRes, int topMargin) {
        	Bitmap decodeResource = BitmapFactory.decodeResource(getResources(), pics[picIdx++]);
			c.drawBitmap(decodeResource, 0, topMargin, null);
        	if (picIdx == totalRes) picIdx = 0;
        }
        
        private final Runnable mDrawWallpaper = new Runnable() {
        	public void run() {
        			drawCarAndRoad();
        	}
        };

        //Only one animation plays at a time and this represents the index.
        private int picIdx = 0;

        //Signal animation change needed
        private int currAnimDirection = DRIVING_FORWARD;
        private int currSceneOfDay = TIME_PERIOD_SUNSET;

        private float mDragEventStartX = 0;
        private boolean mDragEventInProgress = false;
        
    	private int currBGIdx = 0;

    	/*
    	 * When the wallpaper goes out of view
    	 * stop animations 
    	 */
        private boolean mVisible;
        
        //Resource bounds checking helpers
        private static final int TOTAL_FRONT_RES = 2;
        private static final int TOTAL_LEFT_RES = 5;
        private static final int TOTAL_RIGHT_RES = 5;
        private static final int TOTAL_DAY_RES = 12;
        private static final int TOTAL_NIGHT_RES = 8;
        private static final int TOTAL_SUNSET_RES = 9;

		private static final int DRIVING_FORWARD = 5678;
		private static final int DRIVING_LEFT = 9876;
		private static final int DRIVING_RIGHT = 234;
		
		private final int[] mFrontPicIds = new int[TOTAL_FRONT_RES];
		private final int[] mRightPicIds = new int[TOTAL_RIGHT_RES];
		private final int[] mLeftPicIds = new int[TOTAL_LEFT_RES];
		private final int[] mSunsetPicIds = new int[TOTAL_SUNSET_RES];
		private final int[] mNightPicIds = new int[TOTAL_NIGHT_RES];
		private final int[] mDayPicIds = new int[TOTAL_DAY_RES];
		
		private Integer[] mDayColourBGIds = {
				R.color.bg_day000, R.color.bg_day001,
				R.color.bg_day002, R.color.bg_day003,
				R.color.bg_day004, R.color.bg_day005,
				R.color.bg_day006, R.color.bg_day007,
				R.color.bg_day008, R.color.bg_day009,
				R.color.bg_day010, R.color.bg_day011
				};
		private final int[] mNightColourBGIds = {
				R.color.bg_night000, R.color.bg_night001,
				R.color.bg_night002, R.color.bg_night003,
				R.color.bg_night004, R.color.bg_night005,
				R.color.bg_night006, R.color.bg_night007
		};
		private final int[] mSunsetColourBGIds = {
				R.color.bg_sunset000, R.color.bg_sunset001,
				R.color.bg_sunset002, R.color.bg_sunset003,
				R.color.bg_sunset004, R.color.bg_sunset005,
				R.color.bg_sunset006, R.color.bg_sunset007,
				R.color.bg_sunset008
		};

		private static final String TAG = "OutRun";
    }
}
