package au.com.sensis.android.demo;

/*
 * Copyright (C) 2008 The Android Open Source Project
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
 * 
 * Modifications by: Alessandro Crugnola and David Weiss
 * 
 * Notes:
 * 
 * From: http://blog.sephiroth.it/2011/03/29/widget-slidingdrawer-top-to-bottom/
 * and http://stackoverflow.com/questions/9088826/slider-in-android-app/9287885#9287885
 * Original API: http://developer.android.com/reference/android/widget/SlidingDrawer.html
 * 
 * You may want to override the back button so that it closes the open drawer.  
 * That can be done like this:
 *   @Override
 *   public void onBackPressed() {
 *     if(mDrawer.isShown()) && mDrawer.isOpened())
 *       mDrawer.animateClose();
 *     else
 *       super.onBackPressed();
 *   }
 * 
 * You may want to hide the handle on open.  In that case you could set
 * mHandle.setVisibility(View.GONE); in the onDrawerOpenListener.  Don't forget to
 * make it visible again in the onDrawerCloseListener.
 * 
 * Here is an idea to make 
 * Insert this code into a method onMeasure after line "int heightSpecSize = MeasureSpec.getSize( eightMeasureSpec );"
 * heightSpecSize *= 0.75; // percentage of height
 * 
 * Another possibility is com.sileria.android.SlidingTray from http://aniqroid.sileria.com/doc/api/
 * but it has some limitations.
 *
 */


import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SoundEffectConstants;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;


public class CustomSlidingDrawer extends ViewGroup {
	
	public static final int				ORIENTATION_RTL		= 0;
	public static final int				ORIENTATION_BTT		= 1;
	public static final int				ORIENTATION_LTR		= 2;
	public static final int				ORIENTATION_TTB		= 3;
	
	private static final int			TAP_THRESHOLD				= 6;
	private static final float			MAXIMUM_TAP_VELOCITY		= 100.0f;
	private static final float			MAXIMUM_MINOR_VELOCITY		= 150.0f;
	private static final float			MAXIMUM_MAJOR_VELOCITY		= 200.0f;
	private static final float			MAXIMUM_ACCELERATION		= 2000.0f;
	private static final int			VELOCITY_UNITS				= 1000;
	private static final int			MSG_ANIMATE					= 1000;
	private static final int			ANIMATION_FRAME_DURATION	= 1000 / 60;
	
	private static final int			EXPANDED_FULL_OPEN			= -10001;
	private static final int			COLLAPSED_FULL_CLOSED		= -10002;
	
	private final int					mHandleId;
	private final int					mContentId;
	
	private View						mHandle;
	private View						mContent;
	
	private final Rect					mFrame							= new Rect();
	private final Rect					mInvalidate						= new Rect();
	private boolean						mTracking;
	private boolean						mLocked;
	
	private VelocityTracker				mVelocityTracker;
	
	private boolean						mInvert;
	private boolean						mVertical;
	private boolean						mExpanded;
	private int							mBottomOffset;
	private int							mTopOffset;
	private int							mHandleHeight;
	private int							mHandleWidth;
	
	private OnDrawerOpenListener		mOnDrawerOpenListener;
	private OnDrawerCloseListener		mOnDrawerCloseListener;
	private OnDrawerScrollListener		mOnDrawerScrollListener;
	
	private final Handler				mHandler							= new SlidingHandler();
	private float						mAnimatedAcceleration;
	private float						mAnimatedVelocity;
	private float						mAnimationPosition;
	private long						mAnimationLastTime;
	private long						mCurrentAnimationTime;
	private int							mTouchDelta;
	private boolean						mAnimating;
	private boolean						mAllowSingleTap;
	private boolean						mAnimateOnClick;
	
	private final int					mTapThreshold;
	private final int					mMaximumTapVelocity;
	// Custom: these vectors are in opposite direction for inverted operation.
	private int							mMaximumMinorVelocity;
	private int							mMaximumMajorVelocity;
	private int							mMaximumAcceleration;
	private final int					mVelocityUnits;
	
	/**
	 * Callback invoked when the drawer is opened.
	 */
	public static interface OnDrawerOpenListener {
		/**
		 * Invoked when the drawer becomes fully open.
		 */
		public void onDrawerOpened();
	}
	
	/**
	 * Callback invoked when the drawer is closed.
	 */
	public static interface OnDrawerCloseListener {
		/**
		 * Invoked when the drawer becomes fully closed.
		 */
		public void onDrawerClosed();
	}
	
	/**
	 * Callback invoked when the drawer is scrolled.
	 */
	public static interface OnDrawerScrollListener {
		/**
		 * Invoked when the user starts dragging/flinging the drawer's handle.
		 */
		public void onScrollStarted();
		
		/**
		 * Invoked when the user stops dragging/flinging the drawer's handle.
		 */
		public void onScrollEnded();
	}
	
	/**
	 * Creates a new SlidingDrawer from a specified set of attributes defined in
	 * XML.
	 * 
	 * @param context
	 *           The application's environment.
	 * @param attrs
	 *           The attributes defined in XML.
	 */
	public CustomSlidingDrawer( Context context, AttributeSet attrs ) {
		this( context, attrs, 0 );
		System.out.println("CustomSlidingDrawer.ctor-2");
	}
	
	/**
	 * Creates a new SlidingDrawer from a specified set of attributes defined in
	 * XML.
	 * 
	 * @param context
	 *           The application's environment.
	 * @param attrs
	 *           The attributes defined in XML.
	 * @param defStyle
	 *           The style to apply to this widget.
	 */
	public CustomSlidingDrawer( Context context, AttributeSet attrs, int defStyle ) {
		super( context, attrs, defStyle );

		TypedArray a = context.obtainStyledAttributes( attrs, R.styleable.CustomSlidingDrawer, defStyle, 0 );
		
		int orientation = a.getInt( R.styleable.CustomSlidingDrawer_direction, ORIENTATION_BTT );
		mVertical = ( orientation == ORIENTATION_BTT || orientation == ORIENTATION_TTB );
		mBottomOffset = (int)a.getDimension( R.styleable.CustomSlidingDrawer_bottomOffset, 0.0f );
		mTopOffset = (int)a.getDimension( R.styleable.CustomSlidingDrawer_topOffset, 0.0f );
		mAllowSingleTap = a.getBoolean( R.styleable.CustomSlidingDrawer_allowSingleTap, true );
		mAnimateOnClick = a.getBoolean( R.styleable.CustomSlidingDrawer_animateOnClick, true );
		// Custom: top-to-bottom and left-to-right are "inverted operation".
		mInvert = ( orientation == ORIENTATION_TTB || orientation == ORIENTATION_LTR );
		
		int handleId = a.getResourceId( R.styleable.CustomSlidingDrawer_handle, 0 );
		if ( handleId == 0 ) { 
			throw new IllegalArgumentException( "The handle attribute is required and must refer to a valid child." ); 
		}
		
		int contentId = a.getResourceId( R.styleable.CustomSlidingDrawer_content, 0 );
		if ( contentId == 0 ) { 
			throw new IllegalArgumentException( "The content attribute is required and must refer to a valid child." ); 
		}
		
		if ( handleId == contentId ) { 
			throw new IllegalArgumentException( "The content and handle attributes must refer to different children." ); 
		}
		mHandleId = handleId;
		mContentId = contentId;
		
		final float density = getResources().getDisplayMetrics().density;
		mTapThreshold = (int)( TAP_THRESHOLD * density + 0.5f );
		mMaximumTapVelocity = (int)( MAXIMUM_TAP_VELOCITY * density + 0.5f );
		mMaximumMinorVelocity = (int)( MAXIMUM_MINOR_VELOCITY * density + 0.5f );
		mMaximumMajorVelocity = (int)( MAXIMUM_MAJOR_VELOCITY * density + 0.5f );
		mMaximumAcceleration = (int)( MAXIMUM_ACCELERATION * density + 0.5f );
		mVelocityUnits = (int)( VELOCITY_UNITS * density + 0.5f );
		
		if( mInvert ) {
			// Custom: invert these vectors.
			mMaximumAcceleration = -mMaximumAcceleration;
			mMaximumMajorVelocity = -mMaximumMajorVelocity;
			mMaximumMinorVelocity = -mMaximumMinorVelocity;
		}
		
		a.recycle();
		setAlwaysDrawnWithCacheEnabled( false );
	}
	
	@Override
	protected void onFinishInflate() {
		mHandle = findViewById( mHandleId );
		if ( mHandle == null ) { 
			throw new IllegalArgumentException( "The handle attribute is must refer to an" + " existing child." ); 
		}
		mHandle.setOnClickListener( new DrawerToggler() );
		mContent = findViewById( mContentId );
		if ( mContent == null ) { 
			throw new IllegalArgumentException( "The content attribute is must refer to an existing child." ); 
		}
		mContent.setVisibility( View.GONE );
	}
	
	//@Override
	// Not used.
	protected void onMeasureOriginal( int widthMeasureSpec, int heightMeasureSpec ) {
		int widthSpecMode = MeasureSpec.getMode( widthMeasureSpec );
		int widthSpecSize = MeasureSpec.getSize( widthMeasureSpec );
		
		int heightSpecMode = MeasureSpec.getMode( heightMeasureSpec );
		int heightSpecSize = MeasureSpec.getSize( heightMeasureSpec );
		
		if ( widthSpecMode == MeasureSpec.UNSPECIFIED || heightSpecMode == MeasureSpec.UNSPECIFIED ) { 
			throw new RuntimeException("SlidingDrawer cannot have UNSPECIFIED dimensions"); 
		}
		
		final View handle = mHandle;
		measureChild( handle, widthMeasureSpec, heightMeasureSpec );
		
		if ( mVertical ) {
			int height = heightSpecSize - handle.getMeasuredHeight() - mTopOffset;
			mContent.measure( 
					MeasureSpec.makeMeasureSpec( widthSpecSize, MeasureSpec.EXACTLY ), 
					MeasureSpec.makeMeasureSpec( height, MeasureSpec.EXACTLY ) );
		} else {
			int width = widthSpecSize - handle.getMeasuredWidth() - mTopOffset;
			mContent.measure( 
					MeasureSpec.makeMeasureSpec( width, MeasureSpec.EXACTLY ), 
					MeasureSpec.makeMeasureSpec( heightSpecSize, MeasureSpec.EXACTLY ) );
		}
		
		setMeasuredDimension( widthSpecSize, heightSpecSize );
	}
	
	/**
	 * The code below fixes a problem with android.widget.SlidingDrawer where the contents
	 * layout_width and layout_height attributes are ignored and instead assumed as fill_parent.
	 * With this fix, the 'android:layout_height="wrap_content"' spec. is correctly honoured.
	 * Ref: http://stackoverflow.com/a/4265553/1402287
	 */
	@Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthSpecMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSpecSize =  MeasureSpec.getSize(widthMeasureSpec);

        int heightSpecMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSpecSize =  MeasureSpec.getSize(heightMeasureSpec);

        if (widthSpecMode == MeasureSpec.UNSPECIFIED || heightSpecMode == MeasureSpec.UNSPECIFIED) {
            throw new RuntimeException("SlidingDrawer cannot have UNSPECIFIED dimensions");
        }

        final View handle = getHandle();
        final View content = getContent();
        measureChild(handle, widthMeasureSpec, heightMeasureSpec);

        if (mVertical) {
            int height = heightSpecSize - handle.getMeasuredHeight() - mTopOffset;
            content.measure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(height, heightSpecMode));
            heightSpecSize = handle.getMeasuredHeight() + mTopOffset + content.getMeasuredHeight();
            widthSpecSize = content.getMeasuredWidth();
            if (handle.getMeasuredWidth() > widthSpecSize) widthSpecSize = handle.getMeasuredWidth();
        }
        else {
            int width = widthSpecSize - handle.getMeasuredWidth() - mTopOffset;
            getContent().measure(MeasureSpec.makeMeasureSpec(width, widthSpecMode), heightMeasureSpec);
            widthSpecSize = handle.getMeasuredWidth() + mTopOffset + content.getMeasuredWidth();
            heightSpecSize = content.getMeasuredHeight();
            if (handle.getMeasuredHeight() > heightSpecSize) heightSpecSize = handle.getMeasuredHeight();
        }

        setMeasuredDimension(widthSpecSize, heightSpecSize);
    }
	
	@Override
	protected void dispatchDraw( Canvas canvas ) {
		final long drawingTime = getDrawingTime();
		final View handle = mHandle;
		final boolean isVertical = mVertical;
		
		drawChild( canvas, handle, drawingTime );
		
		if ( mTracking || mAnimating ) {
			final Bitmap cache = mContent.getDrawingCache();
			// Custom: Adjust draw co-ords for inverted operation.
			if ( cache != null ) {
				if ( isVertical ) {
					canvas.drawBitmap( cache, 0, !mInvert ? handle.getBottom() : (handle.getTop() - (getBottom() - getTop()) + mHandleHeight), null );
				} else {
					canvas.drawBitmap( cache, !mInvert ? handle.getRight() : (handle.getLeft() - cache.getWidth()) , 0, null );
				}
			} else {
				canvas.save();
				float nonVerticalDx = handle.getLeft() - mTopOffset - (mInvert ? mContent.getMeasuredWidth() : 0);
				float nonVerticalDy = handle.getTop() - mTopOffset - (mInvert ? mContent.getMeasuredHeight() : 0);
				canvas.translate( isVertical ? 0 : nonVerticalDx, isVertical ? nonVerticalDy : 0 );
				
				drawChild( canvas, mContent, drawingTime );
				canvas.restore();
			}
			invalidate();
		} else if ( mExpanded ) {
			drawChild( canvas, mContent, drawingTime );
		}
	}
	
	public static final String	LOG_TAG	= "Sliding";
	
	/**
	 * @param changed - This is a new size or position for this view
	 * @param l  Left position, relative to parent
	 * @param t  Top position, relative to parent
	 * @param r  Right position, relative to parent
	 * @param b  Bottom position, relative to parent  
	 */
	@Override
	protected void onLayout( boolean changed, int l, int t, int r, int b ) {
		if ( mTracking ) { 
			return; 
		}
		
		final int width = r - l;
		final int height = b - t;
		
		final View handle = mHandle;
		final int handleWidth = handle.getMeasuredWidth();
		final int handleHeight = handle.getMeasuredHeight();
		int handleLeft;
		int handleTop;
		
		final View content = mContent;
		int contentLayoutLeft   = 0;
		int contentLayoutTop    = 0;
		int contentLayoutRight  = content.getMeasuredWidth();
		int contentLayoutBottom = content.getMeasuredHeight();
		
		if ( mVertical ) {
			// Custom: For "inverted mode", add the handle height to the layout top coord for the content.
			int contentTopAdjust = mInvert ? 0 : handleHeight;
			
			contentLayoutTop 	+= mTopOffset + contentTopAdjust;
			contentLayoutBottom += mTopOffset + contentTopAdjust;
			
			int expandedHandleTop = mInvert ? (height - handleHeight - mBottomOffset) : mTopOffset;
			int unExpandedHandleTop = mInvert ? mTopOffset : (height - handleHeight + mBottomOffset);

			handleTop = mExpanded ? expandedHandleTop : unExpandedHandleTop;
			handleLeft = ( width - handleWidth ) / 2;
		} else {
			// Custom: For "inverted mode", add the handle width to the layout left coord for the content.
			int contentLeftAdjust = mInvert ? 0 : handleWidth;
			
			contentLayoutLeft  += mTopOffset + contentLeftAdjust;
			contentLayoutRight += mTopOffset + contentLeftAdjust;
			
			int expandedHandleLeft = mInvert ? (width - handleWidth - mBottomOffset) : mTopOffset;
			int unExpandedHandleLeft = mInvert ? mTopOffset : (width - handleWidth + mBottomOffset);
			
			handleLeft = mExpanded ? expandedHandleLeft : unExpandedHandleLeft;
			handleTop = ( height - handleHeight ) / 2;
		}
		content.layout( contentLayoutLeft, contentLayoutTop, contentLayoutRight, contentLayoutBottom );
		handle.layout( handleLeft, handleTop, handleLeft + handleWidth, handleTop + handleHeight );
		mHandleHeight = handle.getHeight();
		mHandleWidth = handle.getWidth();
	}
	
	@Override
	public boolean onInterceptTouchEvent( MotionEvent event ) {
		// TODO remove
		//SearchActivity.dumpEvent(event, "SlidingDrawer.onInterceptTouchEvent");
		if ( mLocked ) { 
			return false; 
		}
		
		final int action = event.getAction();
		
		float x = event.getX();
		float y = event.getY();
		
		final Rect frame = mFrame;
		final View handle = mHandle;
		
		handle.getHitRect( frame );
		if ( !mTracking && !frame.contains( (int)x, (int)y ) ) {
			return false; 
		}
		
		if ( action == MotionEvent.ACTION_DOWN ) {
			mTracking = true;
			
			handle.setPressed( true );
			// Must be called before prepareTracking()
			prepareContent();
			
			// Must be called after prepareContent()
			if ( mOnDrawerScrollListener != null ) {
				mOnDrawerScrollListener.onScrollStarted();
			}
			
			if ( mVertical ) {
				final int top = mHandle.getTop();
				mTouchDelta = (int)y - top;
				prepareTracking( top );
			} else {
				final int left = mHandle.getLeft();
				mTouchDelta = (int)x - left;
				prepareTracking( left );
			}
			mVelocityTracker.addMovement( event );
		}
		
		return true;
	}
	
	/**
	 * Custom: Given a velocity, return it, limiting to the mMaximumMinorVelocity.
	 * Allow for a different sense of the test, depending on the inverted mode of operation.
	 * Inspired by Maciej Ciemeaga.
	 * @param velocity
	 * @return the limited velocity
	 */
	private float limitToMaximumMinorVelocity(float velocity) {
		if (!mInvert) {
			if (velocity > mMaximumMinorVelocity) {
				velocity = mMaximumMinorVelocity;
            }						
		} else {
			if (velocity < mMaximumMinorVelocity) {
				velocity = mMaximumMinorVelocity;
            }						
		}
		return velocity;
	}
	
	@Override
	public boolean onTouchEvent( MotionEvent event ) {
		// TODO remove
		//SearchActivity.dumpEvent(event, "SlidingDrawer.onTouchEvent");
		if ( mLocked ) { 
			return true; 
		}
		if ( mTracking ) {
			mVelocityTracker.addMovement( event );
			final int action = event.getAction();
			switch ( action ) {
				case MotionEvent.ACTION_MOVE:
					moveHandle( (int)( mVertical ? event.getY() : event.getX() ) - mTouchDelta );
					break;
				case MotionEvent.ACTION_UP:
				case MotionEvent.ACTION_CANCEL: {
					final VelocityTracker velocityTracker = mVelocityTracker;
					velocityTracker.computeCurrentVelocity( mVelocityUnits );
					
					float yVelocity = velocityTracker.getYVelocity();
					float xVelocity = velocityTracker.getXVelocity();
					boolean negative;
					
					final boolean vertical = mVertical;
					if ( vertical ) {
						negative = yVelocity < 0;
						if ( xVelocity < 0 ) {
							xVelocity = -xVelocity;
						}
						// Custom: invert mode dependent velocity limit.
						xVelocity = limitToMaximumMinorVelocity(xVelocity);
					} else {
						negative = xVelocity < 0;
						if ( yVelocity < 0 ) {
							yVelocity = -yVelocity;
						}
						// Custom: invert mode dependent velocity limit.
						yVelocity = limitToMaximumMinorVelocity(yVelocity);
					}
					
					float velocity = (float)Math.hypot( xVelocity, yVelocity );
					if ( negative ) {
						velocity = -velocity;
					}
					
					final int handleTop = mHandle.getTop();
					final int handleLeft = mHandle.getLeft();
					final int handleBottom = mHandle.getBottom();
					final int handleRight = mHandle.getRight();
					
					if ( Math.abs( velocity ) < mMaximumTapVelocity ) {
						// Custom: conditions for open/close/fling are complicated. 
						// Let's explode some of that original code to make it clearer.
						boolean c1;
						boolean c2;
						boolean c3;
						boolean c4;
						
						if( mInvert ) {
							c1 = ( mExpanded && (getBottom() - handleBottom ) < mTapThreshold + mBottomOffset );
							c2 = ( !mExpanded && handleTop < mTopOffset + mHandleHeight - mTapThreshold );
							c3 = ( mExpanded && (getRight() - handleRight ) < mTapThreshold + mBottomOffset );
							// Custom: 'Pista' (on comment at http://blog.sephiroth.it/2011/03/29/widget-slidingdrawer-top-to-bottom/) 
							// claims that 'There is a bug that causes the "setOnClickListener on mHandle view doesn't works" problem'.
							// His fix it so change ' ... mHandleWidth + mTapThreshold' to ' ... mHandleWidth - mTapThreshold'
							// which I've done. Hope it is correct.
							c4 = ( !mExpanded && handleLeft > mTopOffset + mHandleWidth - mTapThreshold );
						} else {
							c1 = ( mExpanded && handleTop < mTapThreshold + mTopOffset );
							c2 = ( !mExpanded && handleTop > mBottomOffset + getBottom() - getTop() - mHandleHeight - mTapThreshold );
							c3 = ( mExpanded && handleLeft < mTapThreshold + mTopOffset );
							c4 = ( !mExpanded && handleLeft > mBottomOffset + getRight() - getLeft() - mHandleWidth - mTapThreshold );
						}
						
						if ( vertical ? c1 || c2 : c3 || c4 ) {
							if ( mAllowSingleTap ) {
								playSoundEffect( SoundEffectConstants.CLICK );
								
								if ( mExpanded ) {
									animateClose( vertical ? handleTop : handleLeft );
								} else {
									animateOpen( vertical ? handleTop : handleLeft );
								}
							} else {
								performFling( vertical ? handleTop : handleLeft, velocity, false );
							}
						} else {
							performFling( vertical ? handleTop : handleLeft, velocity, false );
						}
					} else {
						performFling( vertical ? handleTop : handleLeft, velocity, false );
					}
				}
				break;
			}
		}
		
		return mTracking || mAnimating || super.onTouchEvent( event );
	}
	
	private void animateClose( int position ) {
		prepareTracking( position );
		performFling( position, mMaximumAcceleration, true );
	}
	
	private void animateOpen( int position ) {
		prepareTracking( position );
		performFling( position, -mMaximumAcceleration, true );
	}
	
	private void performFling( int position, float velocity, boolean always ) {
		mAnimationPosition = position;
		mAnimatedVelocity = velocity;
		
		boolean c1;
		boolean c2;
		boolean c3;
		
		if ( mExpanded ) {
			int bottom = mVertical ? getBottom() : getRight();
			int handleHeight = mVertical ? mHandleHeight : mHandleWidth;
			
			Log.d( LOG_TAG, "position: " + position + ", velocity: " + velocity + ", mMaximumMajorVelocity: " + mMaximumMajorVelocity );
			c1 = mInvert ? velocity < mMaximumMajorVelocity : velocity > mMaximumMajorVelocity;
			c2 = mInvert ? ( bottom - (position + handleHeight) ) + mBottomOffset > handleHeight : position > mTopOffset + ( mVertical ? mHandleHeight : mHandleWidth );
			c3 = mInvert ? velocity < -mMaximumMajorVelocity : velocity > -mMaximumMajorVelocity;
			Log.d( LOG_TAG, "EXPANDED. c1: " + c1 + ", c2: " + c2 + ", c3: " + c3 );
			if ( always || ( c1 || ( c2 && c3 ) ) ) {
				// Custom: We are expanded, So animate to CLOSE!
				mAnimatedAcceleration = mMaximumAcceleration;
				if( mInvert ) {
					if ( velocity > 0 ) {
						mAnimatedVelocity = 0;
					}
				} else {
					if ( velocity < 0 ) {
						mAnimatedVelocity = 0;
					}
				}
			} else {
				// We are expanded, but they didn't move sufficiently to cause
				// us to retract. Animate back to the expanded position. so animate BACK to expanded!
				mAnimatedAcceleration = -mMaximumAcceleration;
				
				if( mInvert ) {
					if ( velocity < 0 ) {
						mAnimatedVelocity = 0;
					}
				} else {
					if ( velocity > 0 ) {
						mAnimatedVelocity = 0;
					}
				}
			}
		} else {
			
			// WE'RE COLLAPSED
			
			c1 = mInvert ? velocity < mMaximumMajorVelocity : velocity > mMaximumMajorVelocity;
			c2 = mInvert ? ( position < ( mVertical ? getHeight() : getWidth() ) / 2 ) : ( position > ( mVertical ? getHeight() : getWidth() ) / 2 );
			c3 = mInvert ? velocity < -mMaximumMajorVelocity : velocity > -mMaximumMajorVelocity;
			
			Log.d( LOG_TAG, "COLLAPSED. position: " + position + ", velocity: " + velocity + ", mMaximumMajorVelocity: " + mMaximumMajorVelocity );
			Log.d( LOG_TAG, "COLLAPSED. always: " + always + ", c1: " + c1 + ", c2: " + c2 + ", c3: " + c3 );
			
			if ( !always && ( c1 || ( c2 && c3 ) ) ) {
				mAnimatedAcceleration = mMaximumAcceleration;
				
				if( mInvert ) {
					if ( velocity > 0 ) {
						mAnimatedVelocity = 0;
					}
				} else {
					if ( velocity < 0 ) {
						mAnimatedVelocity = 0;
					}
				}
			} else {
				mAnimatedAcceleration = -mMaximumAcceleration;
				
				if( mInvert ) {
					if ( velocity < 0 ) {
						mAnimatedVelocity = 0;
					}
				} else {
					if ( velocity > 0 ) {
						mAnimatedVelocity = 0;
					}
				}
			}
		}
		
		long now = SystemClock.uptimeMillis();
		mAnimationLastTime = now;
		mCurrentAnimationTime = now + ANIMATION_FRAME_DURATION;
		mAnimating = true;
		mHandler.removeMessages( MSG_ANIMATE );
		mHandler.sendMessageAtTime( mHandler.obtainMessage( MSG_ANIMATE ), mCurrentAnimationTime );
		stopTracking();
	}
	
	private void prepareTracking( int position ) {
		mTracking = true;
		mVelocityTracker = VelocityTracker.obtain();
		boolean opening = !mExpanded;
		
		if ( opening ) {
			mAnimatedAcceleration = mMaximumAcceleration;
			mAnimatedVelocity = mMaximumMajorVelocity;
			if( mInvert )
				mAnimationPosition = mTopOffset;
			else
				mAnimationPosition = mBottomOffset + ( mVertical ? getHeight() - mHandleHeight : getWidth() - mHandleWidth );
			moveHandle( (int)mAnimationPosition );
			mAnimating = true;
			mHandler.removeMessages( MSG_ANIMATE );
			long now = SystemClock.uptimeMillis();
			mAnimationLastTime = now;
			mCurrentAnimationTime = now + ANIMATION_FRAME_DURATION;
			mAnimating = true;
		} else {
			if ( mAnimating ) {
				mAnimating = false;
				mHandler.removeMessages( MSG_ANIMATE );
			}
			moveHandle( position );
		}
	}
	
	private void moveHandle( int position ) {
		final View handle = mHandle;
		
		if ( mVertical ) {
			if ( position == EXPANDED_FULL_OPEN ) {
				if( mInvert )
					handle.offsetTopAndBottom( mBottomOffset + getBottom() - getTop() - mHandleHeight );
				else
					handle.offsetTopAndBottom( mTopOffset - handle.getTop() );
				invalidate();
			} else if ( position == COLLAPSED_FULL_CLOSED ) {
				if( mInvert ) {
					handle.offsetTopAndBottom( mTopOffset - handle.getTop() );
				} else {
					handle.offsetTopAndBottom( mBottomOffset + getBottom() - getTop() - mHandleHeight - handle.getTop() );
				}
				invalidate();
			} else {
				final int top = handle.getTop();
				int deltaY = position - top;
				if ( position < mTopOffset ) {
					deltaY = mTopOffset - top;
				} else if ( deltaY > mBottomOffset + getBottom() - getTop() - mHandleHeight - top ) {
					deltaY = mBottomOffset + getBottom() - getTop() - mHandleHeight - top;
				}
				
				handle.offsetTopAndBottom( deltaY );
				
				final Rect frame = mFrame;
				final Rect region = mInvalidate;
				
				handle.getHitRect( frame );
				region.set( frame );
				
				region.union( frame.left, frame.top - deltaY, frame.right, frame.bottom - deltaY );
				region.union( 0, frame.bottom - deltaY, getWidth(), frame.bottom - deltaY + mContent.getHeight() );
				
				invalidate( region );
			}
		} else {
			if ( position == EXPANDED_FULL_OPEN ) {
				if( mInvert )
					handle.offsetLeftAndRight( mBottomOffset + getRight() - getLeft() - mHandleWidth );
				else
					handle.offsetLeftAndRight( mTopOffset - handle.getLeft() );
				invalidate();
			} else if ( position == COLLAPSED_FULL_CLOSED ) {
				if( mInvert )
					handle.offsetLeftAndRight( mTopOffset - handle.getLeft() );
				else
					handle.offsetLeftAndRight( mBottomOffset + getRight() - getLeft() - mHandleWidth - handle.getLeft() );
				invalidate();
			} else {
				final int left = handle.getLeft();
				int deltaX = position - left;
				if ( position < mTopOffset ) {
					deltaX = mTopOffset - left;
				} else if ( deltaX > mBottomOffset + getRight() - getLeft() - mHandleWidth - left ) {
					deltaX = mBottomOffset + getRight() - getLeft() - mHandleWidth - left;
				}
				handle.offsetLeftAndRight( deltaX );
				
				final Rect frame = mFrame;
				final Rect region = mInvalidate;
				
				handle.getHitRect( frame );
				region.set( frame );
				
				region.union( frame.left - deltaX, frame.top, frame.right - deltaX, frame.bottom );
				region.union( frame.right - deltaX, 0, frame.right - deltaX + mContent.getWidth(), getHeight() );
				
				invalidate( region );
			}
		}
	}
	
	private void prepareContent() {
		if ( mAnimating ) { 
			return; 
		}
		
		// Something changed in the content, we need to honor the layout request
		// before creating the cached bitmap
		final View content = mContent;
		if ( content.isLayoutRequested() ) {
			
			if ( mVertical ) {
				final int handleHeight = mHandleHeight;
				int height = getBottom() - getTop() - handleHeight - mTopOffset;
				content.measure( MeasureSpec.makeMeasureSpec( getRight() - getLeft(), MeasureSpec.EXACTLY ), MeasureSpec.makeMeasureSpec( height, MeasureSpec.EXACTLY ) );
				
				if ( mInvert ) 
					content.layout( 0, mTopOffset, content.getMeasuredWidth(), mTopOffset + content.getMeasuredHeight() );
				else 
					content.layout( 0, mTopOffset + handleHeight, content.getMeasuredWidth(), mTopOffset + handleHeight + content.getMeasuredHeight() );
			
			} else {
				
				final int handleWidth = mHandle.getWidth();
				int width = getRight() - getLeft() - handleWidth - mTopOffset;
				content.measure( MeasureSpec.makeMeasureSpec( width, MeasureSpec.EXACTLY ), MeasureSpec.makeMeasureSpec( getBottom() - getTop(), MeasureSpec.EXACTLY ) );
				
				if( mInvert )
					content.layout( mTopOffset, 0, mTopOffset + content.getMeasuredWidth(), content.getMeasuredHeight() );
				else
					content.layout( handleWidth + mTopOffset, 0, mTopOffset + handleWidth + content.getMeasuredWidth(), content.getMeasuredHeight() );
			}
		}
		// Try only once... we should really loop but it's not a big deal
		// if the draw was cancelled, it will only be temporary anyway
		content.getViewTreeObserver().dispatchOnPreDraw();
		content.buildDrawingCache();
		
		content.setVisibility( View.GONE );
	}
	
	private void stopTracking() {
		mHandle.setPressed( false );
		mTracking = false;
		
		if ( mOnDrawerScrollListener != null ) {
			mOnDrawerScrollListener.onScrollEnded();
		}
		
		if ( mVelocityTracker != null ) {
			mVelocityTracker.recycle();
			mVelocityTracker = null;
		}
	}
	
	private void doAnimation() {
		if ( mAnimating ) {
			incrementAnimation();
			
			if( mInvert ) {
				if ( mAnimationPosition < mTopOffset ) {
					mAnimating = false;
					closeDrawer();
				} else if ( mAnimationPosition >= mTopOffset + ( mVertical ? getHeight() : getWidth() ) - 1 ) {
					mAnimating = false;
					openDrawer();
				} else {
					moveHandle( (int)mAnimationPosition );
					mCurrentAnimationTime += ANIMATION_FRAME_DURATION;
					mHandler.sendMessageAtTime( mHandler.obtainMessage( MSG_ANIMATE ), mCurrentAnimationTime );
				}				
			} else {
				if ( mAnimationPosition >= mBottomOffset + ( mVertical ? getHeight() : getWidth() ) - 1 ) {
					mAnimating = false;
					closeDrawer();
				} else if ( mAnimationPosition < mTopOffset ) {
					mAnimating = false;
					openDrawer();
				} else {
					moveHandle( (int)mAnimationPosition );
					mCurrentAnimationTime += ANIMATION_FRAME_DURATION;
					mHandler.sendMessageAtTime( mHandler.obtainMessage( MSG_ANIMATE ), mCurrentAnimationTime );
				}
			}
		}
	}
	
	private void incrementAnimation() {
		long now = SystemClock.uptimeMillis();
		float t = ( now - mAnimationLastTime ) / 1000.0f; // ms -> s
		final float position = mAnimationPosition;
		final float v = mAnimatedVelocity; // px/s
		final float a = mAnimatedAcceleration; // px/s/s
		mAnimationPosition = position + ( v * t ) + ( 0.5f * a * t * t ); // px
		mAnimatedVelocity = v + ( a * t ); // px/s
		mAnimationLastTime = now; // ms
	}
	
	/**
	 * Toggles the drawer between open and close. Takes effect immediately.
	 */
	public void toggle() {
		if ( !mExpanded ) {
			openDrawer();
		} else {
			closeDrawer();
		}
		invalidate();
		requestLayout();
	}
	
	/**
	 * Toggles the drawer between open and close with an animation.
	 */
	public void animateToggle()	{
		if ( !mExpanded ) {
			animateOpen();
		} else {
			animateClose();
		}
	}
	
	/**
	 * Opens the drawer immediately.
	 */
	public void open() {
		openDrawer();
		invalidate();
		requestLayout();
		
		sendAccessibilityEvent( AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED );
	}
	
	/**
	 * Closes the drawer immediately.
	 */
	public void close() {
		closeDrawer();
		invalidate();
		requestLayout();
	}
	
	/**
	 * Closes the drawer with an animation.
	 */
	public void animateClose() {
		prepareContent();
		final OnDrawerScrollListener scrollListener = mOnDrawerScrollListener;
		if ( scrollListener != null ) {
			scrollListener.onScrollStarted();
		}
		animateClose( mVertical ? mHandle.getTop() : mHandle.getLeft() );
		
		if ( scrollListener != null ) {
			scrollListener.onScrollEnded();
		}
	}
	
	/**
	 * Opens the drawer with an animation.
	 */
	public void animateOpen() {
		prepareContent();
		final OnDrawerScrollListener scrollListener = mOnDrawerScrollListener;
		if ( scrollListener != null ) {
			scrollListener.onScrollStarted();
		}
		animateOpen( mVertical ? mHandle.getTop() : mHandle.getLeft() );
		
		sendAccessibilityEvent( AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED );
		
		if ( scrollListener != null ) {
			scrollListener.onScrollEnded();
		}
	}
	
	private void closeDrawer() {
		moveHandle( COLLAPSED_FULL_CLOSED );
		mContent.setVisibility( View.GONE );
		mContent.destroyDrawingCache();
		
		if ( !mExpanded ) { 
			return; 
		}
		
		mExpanded = false;
//		System.out.println("closeDrawer mLocked was=" + isLocked());
//		//unlock();
//		System.out.println("closeDrawer mLocked now=" + isLocked());
		if ( mOnDrawerCloseListener != null ) {
			mOnDrawerCloseListener.onDrawerClosed();
		}
	}
	
	private void openDrawer() {
		moveHandle( EXPANDED_FULL_OPEN );
		mContent.setVisibility( View.VISIBLE );
		
		if ( mExpanded ) { 
			return; 
		}
		
		mExpanded = true;
		// TODO remove
//		System.out.println("openDrawer mLocked was=" + isLocked());
//		//lock();
//		System.out.println("openDrawer mLocked now=" + isLocked());
		if ( mOnDrawerOpenListener != null ) {
			mOnDrawerOpenListener.onDrawerOpened();
		}
	}
	
	/**
	 * Sets the listener that receives a notification when the drawer becomes
	 * open.
	 * 
	 * @param onDrawerOpenListener
	 *           The listener to be notified when the drawer is opened.
	 */
	public void setOnDrawerOpenListener( OnDrawerOpenListener onDrawerOpenListener ) {
		mOnDrawerOpenListener = onDrawerOpenListener;
	}
	
	/**
	 * Sets the listener that receives a notification when the drawer becomes
	 * close.
	 * 
	 * @param onDrawerCloseListener
	 *           The listener to be notified when the drawer is closed.
	 */
	public void setOnDrawerCloseListener( OnDrawerCloseListener onDrawerCloseListener ) {
		mOnDrawerCloseListener = onDrawerCloseListener;
	}
	
	/**
	 * Sets the listener that receives a notification when the drawer starts or
	 * ends a scroll. A fling is considered as a scroll. A fling will also
	 * trigger a drawer opened or drawer closed event.
	 * 
	 * @param onDrawerScrollListener
	 *           The listener to be notified when scrolling starts or stops.
	 */
	public void setOnDrawerScrollListener( OnDrawerScrollListener onDrawerScrollListener ) {
		mOnDrawerScrollListener = onDrawerScrollListener;
	}
	
	/**
	 * Returns the handle of the drawer.
	 * 
	 * @return The View reprenseting the handle of the drawer, identified by the
	 *         "handle" id in XML.
	 */
	public View getHandle() {
		return mHandle;
	}
	
	/**
	 * Returns the content of the drawer.
	 * 
	 * @return The View reprenseting the content of the drawer, identified by the
	 *         "content" id in XML.
	 */
	public View getContent() {
		return mContent;
	}
	
	/**
	 * Unlocks the SlidingDrawer so that touch events are processed.
	 * 
	 * @see #lock()
	 */
	public void unlock() {
		mLocked = false;
	}
	
	/**
	 * Locks the SlidingDrawer so that touch events are ignores.
	 * 
	 * @see #unlock()
	 */
	public void lock() {
		mLocked = true;
	}
	
	public boolean isLocked() {
		return mLocked;
	}
	
	/**
	 * Indicates whether the drawer is currently fully opened.
	 * 
	 * @return True if the drawer is opened, false otherwise.
	 */
	public boolean isOpened() {
		return mExpanded;
	}
	
	/**
	 * Indicates whether the drawer is scrolling or flinging.
	 * 
	 * @return True if the drawer is scroller or flinging, false otherwise.
	 */
	public boolean isMoving() {
		return mTracking || mAnimating;
	}
	
	private class DrawerToggler implements OnClickListener {
		
		public void onClick( View v ) {
			if ( mLocked ) { 
				return; 
			}
			// mAllowSingleTap isn't relevant here; you're *always*
			// allowed to open/close the drawer by clicking with the
			// trackball.
			
			if ( mAnimateOnClick ) {
				animateToggle();
			} else {
				toggle();
			}
		}
	}
	
	private class SlidingHandler extends Handler {
		
		public void handleMessage( Message m ) {
			switch ( m.what ) {
				case MSG_ANIMATE:
					doAnimation();
					break;
			}
		}
	}
}
