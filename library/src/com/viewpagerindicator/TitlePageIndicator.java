/*
 * Copyright (C) 2011 Jake Wharton
 * Copyright (C) 2011 Patrik Akerfeldt
 * Copyright (C) 2011 Francisco Figueiredo Jr.
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
package com.viewpagerindicator;

import java.util.ArrayList;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewConfigurationCompat;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

/**
 * A TitlePageIndicator is a PageIndicator which displays the title of left view
 * (if exist), the title of the current select view (centered) and the title of
 * the right view (if exist). When the user scrolls the ViewPager then titles are
 * also scrolled.
 */
public class TitlePageIndicator extends View implements PageIndicator {
    /**
     * Percentage indicating what percentage of the screen width away from
     * center should the underline be fully faded. A value of 0.25 means that
     * halfway between the center of the screen and an edge.
     */
    private static final float SELECTION_FADE_PERCENTAGE = 0.25f;

    /**
     * Percentage indicating what percentage of the screen width away from
     * center should the selected text bold turn off. A value of 0.05 means
     * that 10% between the center and an edge.
     */
    private static final float BOLD_FADE_PERCENTAGE = 0.05f;

    /**
     * Interface for a callback when the center item has been clicked.
     */
    public static interface OnCenterItemClickListener {
        /**
         * Callback when the center item has been clicked.
         *
         * @param position Position of the current center item.
         */
        public void onCenterItemClick(int position);
    }

    public enum IndicatorStyle {
        None(0), Triangle(1), Underline(2);

        public final int value;

        private IndicatorStyle(int value) {
            this.value = value;
        }

        public static IndicatorStyle fromValue(int value) {
            for (IndicatorStyle style : IndicatorStyle.values()) {
                if (style.value == value) {
                    return style;
                }
            }
            return null;
        }
    }
    public enum AdjacentIndicatorStyle {
    	None(0), Fade(1), Arrows(2), Custom(4); 
    	public final int value;
    	
    	private AdjacentIndicatorStyle(int value) {
    		this.value = value;
    	}
    	
    	public static AdjacentIndicatorStyle fromValue(int value) {
    		for(AdjacentIndicatorStyle style : AdjacentIndicatorStyle.values()) {
    			if(style.value == value) return style;
    		}
    		return null;
    	}
    }
    

    private ViewPager mViewPager;
    private ViewPager.OnPageChangeListener mListener;
    private TitleProvider mTitleProvider;
    private int mCurrentPage;
    private int mCurrentOffset;
    private int mScrollState;
    private final Paint mPaintText = new Paint();
    private boolean mBoldText;
    private int mColorText;
    private int mColorSelected;
    private Path mPath;
    private final Paint mPaintFooterLine = new Paint();
    private IndicatorStyle mFooterIndicatorStyle;
    private AdjacentIndicatorStyle mAdjacentIndicatorStyle;
    private int mAdjacentIndicatorLeft;
    private int mAdjacentIndicatorRight;
    private float mAdjacentIndicatorWidth;
    private float mAdjacentIndicatorHeight;
    private boolean mCustomAdjacentIndicatorHeight = false;
    private boolean mCustomAdjacentIndicatorWidth = false;
    
    private final Paint mPaintFooterIndicator = new Paint();
    private float mFooterIndicatorHeight;
    private float mFooterIndicatorUnderlinePadding;
    private float mFooterPadding;
    private float mTitlePadding;
    private float mTopPadding;
    /** Left and right side padding for not active view titles. */
    private float mClipPadding;
    private float mFooterLineHeight;
    

    private static final int INVALID_POINTER = -1;

    private int mTouchSlop;
    private float mLastMotionX = -1;
    private int mActivePointerId = INVALID_POINTER;
    private boolean mIsDragging;

    private OnCenterItemClickListener mCenterItemClickListener;


    public TitlePageIndicator(Context context) {
        this(context, null);
    }

    public TitlePageIndicator(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.vpiTitlePageIndicatorStyle);
    }

    public TitlePageIndicator(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        
        //Load defaults from resources
        final Resources res = getResources();
        final int defaultFooterColor = res.getColor(R.color.default_title_indicator_footer_color);
        final float defaultFooterLineHeight = res.getDimension(R.dimen.default_title_indicator_footer_line_height);
        final int defaultFooterIndicatorStyle = res.getInteger(R.integer.default_title_indicator_footer_indicator_style);
        final float defaultFooterIndicatorHeight = res.getDimension(R.dimen.default_title_indicator_footer_indicator_height);
        final float defaultFooterIndicatorUnderlinePadding = res.getDimension(R.dimen.default_title_indicator_footer_indicator_underline_padding);
        final float defaultFooterPadding = res.getDimension(R.dimen.default_title_indicator_footer_padding);
        final int defaultSelectedColor = res.getColor(R.color.default_title_indicator_selected_color);
        final boolean defaultSelectedBold = res.getBoolean(R.bool.default_title_indicator_selected_bold);
        final int defaultTextColor = res.getColor(R.color.default_title_indicator_text_color);
        final float defaultTextSize = res.getDimension(R.dimen.default_title_indicator_text_size);
        final float defaultTitlePadding = res.getDimension(R.dimen.default_title_indicator_title_padding);
        final float defaultClipPadding = res.getDimension(R.dimen.default_title_indicator_clip_padding);
        final float defaultTopPadding = res.getDimension(R.dimen.default_title_indicator_top_padding);

        //Retrieve styles attributes
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.TitlePageIndicator, defStyle, R.style.Widget_TitlePageIndicator);

        //Retrieve the colors to be used for this view and apply them.
        mFooterLineHeight = a.getDimension(R.styleable.TitlePageIndicator_footerLineHeight, defaultFooterLineHeight);
        mFooterIndicatorStyle = IndicatorStyle.fromValue(a.getInteger(R.styleable.TitlePageIndicator_footerIndicatorStyle, defaultFooterIndicatorStyle));
        mFooterIndicatorHeight = a.getDimension(R.styleable.TitlePageIndicator_footerIndicatorHeight, defaultFooterIndicatorHeight);
        mFooterIndicatorUnderlinePadding = a.getDimension(R.styleable.TitlePageIndicator_footerIndicatorUnderlinePadding, defaultFooterIndicatorUnderlinePadding);
        mFooterPadding = a.getDimension(R.styleable.TitlePageIndicator_footerPadding, defaultFooterPadding);
        mTopPadding = a.getDimension(R.styleable.TitlePageIndicator_topPadding, defaultTopPadding);
        mTitlePadding = a.getDimension(R.styleable.TitlePageIndicator_titlePadding, defaultTitlePadding);
        mClipPadding = a.getDimension(R.styleable.TitlePageIndicator_clipPadding, defaultClipPadding);
        mColorSelected = a.getColor(R.styleable.TitlePageIndicator_selectedColor, defaultSelectedColor);
        mColorText = a.getColor(R.styleable.TitlePageIndicator_textColor, defaultTextColor);
        mBoldText = a.getBoolean(R.styleable.TitlePageIndicator_selectedBold, defaultSelectedBold);
        
        // TODO add adjacentIndicatorLeft & adjacentIndicatorRight
        mAdjacentIndicatorStyle = AdjacentIndicatorStyle.fromValue(a.getInteger(R.styleable.TitlePageIndicator_adjacentIndicatorStyle, 1));
        if(mAdjacentIndicatorStyle == AdjacentIndicatorStyle.Custom) {
	        mAdjacentIndicatorLeft = a.getResourceId(R.styleable.TitlePageIndicator_adjacentIndicatorLeft, R.drawable.vpi_adjacent_arrow_left);
	        mAdjacentIndicatorRight = a.getResourceId(R.styleable.TitlePageIndicator_adjacentIndicatorRight, R.drawable.vpi_adjacent_arrow_right);
        }
        else {
        	mAdjacentIndicatorLeft = R.drawable.vpi_adjacent_arrow_left;
        	mAdjacentIndicatorRight = R.drawable.vpi_adjacent_arrow_right;
        }
        
        final float textSize = a.getDimension(R.styleable.TitlePageIndicator_textSize, defaultTextSize);
        final int footerColor = a.getColor(R.styleable.TitlePageIndicator_footerColor, defaultFooterColor);
        mPaintText.setTextSize(textSize);
        mPaintText.setAntiAlias(true);
        mPaintFooterLine.setStyle(Paint.Style.FILL_AND_STROKE);
        mPaintFooterLine.setStrokeWidth(mFooterLineHeight);
        mPaintFooterLine.setColor(footerColor);
        mPaintFooterIndicator.setStyle(Paint.Style.FILL_AND_STROKE);
        mPaintFooterIndicator.setColor(footerColor);
        a.recycle();

        final ViewConfiguration configuration = ViewConfiguration.get(context);
        mTouchSlop = ViewConfigurationCompat.getScaledPagingTouchSlop(configuration);
    }



	public int getFooterColor() {
        return mPaintFooterLine.getColor();
    }

    public void setFooterColor(int footerColor) {
        mPaintFooterLine.setColor(footerColor);
        mPaintFooterIndicator.setColor(footerColor);
        invalidate();
    }

    public float getFooterLineHeight() {
        return mFooterLineHeight;
    }

    public void setFooterLineHeight(float footerLineHeight) {
        mFooterLineHeight = footerLineHeight;
        mPaintFooterLine.setStrokeWidth(mFooterLineHeight);
        invalidate();
    }

    public float getFooterIndicatorHeight() {
        return mFooterIndicatorHeight;
    }

    public void setFooterIndicatorHeight(float footerTriangleHeight) {
        mFooterIndicatorHeight = footerTriangleHeight;
        invalidate();
    }

    public float getFooterIndicatorPadding() {
        return mFooterPadding;
    }

    public void setFooterIndicatorPadding(float footerIndicatorPadding) {
        mFooterPadding = footerIndicatorPadding;
        invalidate();
    }

    public IndicatorStyle getFooterIndicatorStyle() {
        return mFooterIndicatorStyle;
    }

    public void setFooterIndicatorStyle(IndicatorStyle indicatorStyle) {
        mFooterIndicatorStyle = indicatorStyle;
        invalidate();
    }

    public int getSelectedColor() {
        return mColorSelected;
    }

    public void setSelectedColor(int selectedColor) {
        mColorSelected = selectedColor;
        invalidate();
    }

    public boolean isSelectedBold() {
        return mBoldText;
    }

    public void setSelectedBold(boolean selectedBold) {
        mBoldText = selectedBold;
        invalidate();
    }

    public int getTextColor() {
        return mColorText;
    }

    public void setTextColor(int textColor) {
        mPaintText.setColor(textColor);
        mColorText = textColor;
        invalidate();
    }

    public float getTextSize() {
        return mPaintText.getTextSize();
    }

    public void setTextSize(float textSize) {
        mPaintText.setTextSize(textSize);
        invalidate();
    }

    public float getTitlePadding() {
        return this.mTitlePadding;
    }

    public void setTitlePadding(float titlePadding) {
        mTitlePadding = titlePadding;
        invalidate();
    }

    public float getTopPadding() {
        return this.mTopPadding;
    }

    public void setTopPadding(float topPadding) {
        mTopPadding = topPadding;
        invalidate();
    }

    public float getClipPadding() {
        return this.mClipPadding;
    }

    public void setClipPadding(float clipPadding) {
        mClipPadding = clipPadding;
        invalidate();
    }

    public void setTypeface(Typeface typeface) {
        mPaintText.setTypeface(typeface);
        invalidate();
    }

    public Typeface getTypeface() {
        return mPaintText.getTypeface();
    }
    
    public void setAdjacentIndicatorStyle(AdjacentIndicatorStyle style) {
    	if(style != null) this.mAdjacentIndicatorStyle = style;
    }
    
    public AdjacentIndicatorStyle getAdjacentIndicatorStyle() {
    	return mAdjacentIndicatorStyle;
    }

    /**
     * Set a custom left Adjacent indicator 
     * AdjacentIndicatorStyle must be set to Custom
     * @param resId: Resource that points to drawable
     */
    public void setAdjacentIndicatorLeft(int resId) {
    	if(mAdjacentIndicatorStyle == AdjacentIndicatorStyle.Custom) 
    			this.mAdjacentIndicatorLeft = resId;
    }
    
    public int getAdjacentIndicatorLeft() {
    	return mAdjacentIndicatorLeft;
    }
    
    /**
     * Set a custom right Adjacent indicator 
     * AdjacentIndicatorStyle must be set to Custom
     * @param resId: Resource that points to drawable
     */
    public void setAdjacentIndicatorRight(int resId) {
    	if(mAdjacentIndicatorStyle == AdjacentIndicatorStyle.Custom)
    	this.mAdjacentIndicatorRight = resId;
    }
    
    public int getAdjacentIndicatorRight() {
    	return mAdjacentIndicatorRight;
    }
    
    /**
     * Set custom width for the Adjacent Indicators
     * Set to 0 or below to reset
     * @param width
     */
    public void setAdjacentIndicatorWidth(float width) {
    	if(width > 0) {
    		this.mAdjacentIndicatorWidth = width;
    		this.mCustomAdjacentIndicatorWidth = true;
    	}
    	
    }
    public float getAdjacentIndicatorWidth() {
    	return mAdjacentIndicatorWidth;
    }
    
    /**
     * Set custom height for the Adjacent Indicators
     * Set to 0 or below to reset
     * @param height
     */
    public void setAdjacentIndicatorHeight(float height) {
    	if(height > 0) {
	    	this.mAdjacentIndicatorHeight = height;
	    	this.mCustomAdjacentIndicatorHeight = true;
    	}
    }
    
    public float getAdjacentIndicatorHeight() {
    	return mAdjacentIndicatorHeight;
    }
    /*
     * (non-Javadoc)
     *
     * @see android.view.View#onDraw(android.graphics.Canvas)
     */
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (mViewPager == null) {
            return;
        }
        final int count = mViewPager.getAdapter().getCount();
        if (count == 0) {
            return;
        }

        //Calculate views bounds
        ArrayList<RectF> bounds = calculateAllBounds(mPaintText);

        //Make sure we're on a page that still exists
        if (mCurrentPage > bounds.size()) {
            setCurrentItem(bounds.size()-1);
        }

        final int countMinusOne = count - 1;
        final float halfWidth = getWidth() / 2f;
        final int left = getLeft();
        final float leftClip = left + mClipPadding;
        final int width = getWidth();
        final int height = getHeight();
        final int right = left + width;
        final float rightClip = right - mClipPadding;

        int page = mCurrentPage;
        float offsetPercent;
        if (mCurrentOffset <= halfWidth) {
            offsetPercent = 1.0f * mCurrentOffset / width;
        } else {
            page += 1;
            offsetPercent = 1.0f * (width - mCurrentOffset) / width;
        }
        final boolean currentSelected = (offsetPercent <= SELECTION_FADE_PERCENTAGE);
        final boolean currentBold = (offsetPercent <= BOLD_FADE_PERCENTAGE);
        final float selectedPercent = (SELECTION_FADE_PERCENTAGE - offsetPercent) / SELECTION_FADE_PERCENTAGE;

        //Verify if the current view must be clipped to the screen
        RectF curPageBound = bounds.get(mCurrentPage);
        float curPageWidth = curPageBound.right - curPageBound.left;
        if (curPageBound.left < leftClip) {
            //Try to clip to the screen (left side)
            clipViewOnTheLeft(curPageBound, curPageWidth, left);

        }
        if (curPageBound.right > rightClip) {
            //Try to clip to the screen (right side)
            clipViewOnTheRight(curPageBound, curPageWidth, right);
        }
        
        if(mAdjacentIndicatorHeight <= 0 || !mCustomAdjacentIndicatorHeight) mAdjacentIndicatorHeight = (getHeight()/3);  
        if(mAdjacentIndicatorWidth <= 0 || !mCustomAdjacentIndicatorWidth) mAdjacentIndicatorWidth = mAdjacentIndicatorHeight / 2;
          

        //Left views starting from the current position
        if (mCurrentPage > 0) {
            for (int i = mCurrentPage - 1; i >= 0; i--) {
                RectF bound = bounds.get(i);
                
                float w = bound.right - bound.left;
                
                // if AdjacentIndicatorStyle is arrows, increase width so the arrow fits.
                if(mAdjacentIndicatorStyle == AdjacentIndicatorStyle.Arrows || mAdjacentIndicatorStyle == AdjacentIndicatorStyle.Custom)
                {
                	w += (mTitlePadding * 2) + mAdjacentIndicatorWidth;
                }
                
                //Is left side is outside the screen
                if (bound.left < leftClip) {
                    
                    //Try to clip to the screen (left side)
                    clipViewOnTheLeft(bound, w, left);
                    
                    //Except if there's an intersection with the right view
                    RectF rightBound = bounds.get(i + 1);
                    //Intersection
                    if (bound.right + mTitlePadding > rightBound.left) {
                        bound.left = rightBound.left - w - mTitlePadding;
                        bound.right = bound.left + w;
                    }
                }
            }
        }
        //Right views starting from the current position
        if (mCurrentPage < countMinusOne) {
            for (int i = mCurrentPage + 1 ; i < count; i++) {
                RectF bound = bounds.get(i);
                
                float w = bound.right - bound.left;
             // if AdjacentIndicatorStyle is arrows, increase width so the arrow fits.
                if(mAdjacentIndicatorStyle == AdjacentIndicatorStyle.Arrows || mAdjacentIndicatorStyle == AdjacentIndicatorStyle.Custom)
                {
                	w += (mTitlePadding * 2) + mAdjacentIndicatorWidth;
                }
                
                //If right side is outside the screen
                if (bound.right > rightClip) {
                    
                    
                    //Try to clip to the screen (right side)
                    clipViewOnTheRight(bound, w, right);
                    //Except if there's an intersection with the left view
                    RectF leftBound = bounds.get(i - 1);
                    //Intersection
                    if (bound.left - mTitlePadding < leftBound.right) {
                        bound.left = leftBound.right + mTitlePadding;
                        bound.right = bound.left + w;
                    }
                }
            }
        }
        
    	
        //Now draw views
        for (int i = 0; i < count; i++) {
            //Get the title
            RectF bound = bounds.get(i);
            //Only if one side is visible
            if ((bound.left > left && bound.left < right) || (bound.right > left && bound.right < right)) {
                final boolean currentPage = (i == page);
                final boolean previousPage = (i == page -1);
                final boolean nextPage = (i == page + 1);
                
                //Only set bold if we are within bounds
                mPaintText.setFakeBoldText(currentPage && currentBold && mBoldText);

                //Draw text as unselected
                mPaintText.setColor(mColorText);
                
                if((mAdjacentIndicatorStyle == AdjacentIndicatorStyle.Arrows || mAdjacentIndicatorStyle == AdjacentIndicatorStyle.Custom )&& previousPage) {
                	canvas.drawText(mTitleProvider.getTitle(i), bound.left + mAdjacentIndicatorWidth  + (2*mTitlePadding), bound.bottom + mTopPadding, mPaintText);
                	
                	Drawable d = this.getResources().getDrawable(mAdjacentIndicatorLeft);
                	Bitmap arrowLeft = Bitmap.createScaledBitmap( ((BitmapDrawable)d).getBitmap(), Math.round(mAdjacentIndicatorWidth), Math.round(mAdjacentIndicatorHeight), true);
                	float drawableStart = (height - mAdjacentIndicatorHeight) / 2; 
            		canvas.drawBitmap(arrowLeft, bound.left +  mTitlePadding ,  drawableStart, null);
                }
                else if((mAdjacentIndicatorStyle == AdjacentIndicatorStyle.Arrows || mAdjacentIndicatorStyle == AdjacentIndicatorStyle.Custom )&& nextPage) {
                	canvas.drawText(mTitleProvider.getTitle(i), bound.left, bound.bottom + mTopPadding, mPaintText);
                	
                	Drawable d = this.getResources().getDrawable(mAdjacentIndicatorRight);
            		Bitmap arrowRight = Bitmap.createScaledBitmap( ((BitmapDrawable)d).getBitmap(), Math.round(mAdjacentIndicatorWidth), Math.round(mAdjacentIndicatorHeight), true);
            		float drawableStart = (height - mAdjacentIndicatorHeight) / 2;
            		canvas.drawBitmap(arrowRight, bound.right - (mAdjacentIndicatorWidth) - mTitlePadding, drawableStart  , null);
                }
                else canvas.drawText(mTitleProvider.getTitle(i), bound.left, bound.bottom + mTopPadding, mPaintText);
                
                
                //If we are within the selected bounds draw the selected text
                if (currentPage && currentSelected) {
                    mPaintText.setColor(mColorSelected);
                    mPaintText.setAlpha((int)((mColorSelected >>> 24) * selectedPercent));
                    canvas.drawText(mTitleProvider.getTitle(i), bound.left, bound.bottom + mTopPadding, mPaintText);
                }
            }
        }

        //Draw the footer line
        mPath = new Path();
        mPath.moveTo(0, height - mFooterLineHeight / 2f);
        mPath.lineTo(width, height - mFooterLineHeight / 2f);
        mPath.close();
        canvas.drawPath(mPath, mPaintFooterLine);

        switch (mFooterIndicatorStyle) {
            case Triangle:
                mPath = new Path();
                mPath.moveTo(halfWidth, height - mFooterLineHeight - mFooterIndicatorHeight);
                mPath.lineTo(halfWidth + mFooterIndicatorHeight, height - mFooterLineHeight);
                mPath.lineTo(halfWidth - mFooterIndicatorHeight, height - mFooterLineHeight);
                mPath.close();
                canvas.drawPath(mPath, mPaintFooterIndicator);
                break;

            case Underline:
                if (!currentSelected) {
                    break;
                }

                RectF underlineBounds = bounds.get(page);
                mPath = new Path();
                mPath.moveTo(underlineBounds.left  - mFooterIndicatorUnderlinePadding, height - mFooterLineHeight);
                mPath.lineTo(underlineBounds.right + mFooterIndicatorUnderlinePadding, height - mFooterLineHeight);
                mPath.lineTo(underlineBounds.right + mFooterIndicatorUnderlinePadding, height - mFooterLineHeight - mFooterIndicatorHeight);
                mPath.lineTo(underlineBounds.left  - mFooterIndicatorUnderlinePadding, height - mFooterLineHeight - mFooterIndicatorHeight);
                mPath.close();

                mPaintFooterIndicator.setAlpha((int)(0xFF * selectedPercent));
                canvas.drawPath(mPath, mPaintFooterIndicator);
                mPaintFooterIndicator.setAlpha(0xFF);
                break;
        }
        
      
        
    }

    public boolean onTouchEvent(android.view.MotionEvent ev) {
        if ((mViewPager == null) || (mViewPager.getAdapter().getCount() == 0)) {
            return false;
        }

        final int action = ev.getAction();

        switch (action & MotionEventCompat.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                mActivePointerId = MotionEventCompat.getPointerId(ev, 0);
                mLastMotionX = ev.getX();
                break;

            case MotionEvent.ACTION_MOVE: {
                final int activePointerIndex = MotionEventCompat.findPointerIndex(ev, mActivePointerId);
                final float x = MotionEventCompat.getX(ev, activePointerIndex);
                final float deltaX = x - mLastMotionX;

                if (!mIsDragging) {
                    if (Math.abs(deltaX) > mTouchSlop) {
                        mIsDragging = true;
                    }
                }

                if (mIsDragging) {
                    if (!mViewPager.isFakeDragging()) {
                        mViewPager.beginFakeDrag();
                    }

                    mLastMotionX = x;

                    mViewPager.fakeDragBy(deltaX);
                }

                break;
            }

            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                if (!mIsDragging) {
                    final int count = mViewPager.getAdapter().getCount();
                    final int width = getWidth();
                    final float halfWidth = width / 2f;
                    final float sixthWidth = width / 6f;
                    final float leftThird = halfWidth - sixthWidth;
                    final float rightThird = halfWidth + sixthWidth;
                    final float eventX = ev.getX();

                    if (eventX < leftThird) {
                        if (mCurrentPage > 0) {
                            mViewPager.setCurrentItem(mCurrentPage - 1);
                            return true;
                        }
                    } else if (eventX > rightThird) {
                        if (mCurrentPage < count - 1) {
                            mViewPager.setCurrentItem(mCurrentPage + 1);
                            return true;
                        }
                    } else {
                        //Middle third
                        if (mCenterItemClickListener != null) {
                            mCenterItemClickListener.onCenterItemClick(mCurrentPage);
                        }
                    }
                }

                mIsDragging = false;
                mActivePointerId = INVALID_POINTER;
                if (mViewPager.isFakeDragging()) mViewPager.endFakeDrag();
                break;

            case MotionEventCompat.ACTION_POINTER_DOWN: {
                final int index = MotionEventCompat.getActionIndex(ev);
                final float x = MotionEventCompat.getX(ev, index);
                mLastMotionX = x;
                mActivePointerId = MotionEventCompat.getPointerId(ev, index);
                break;
            }

            case MotionEventCompat.ACTION_POINTER_UP:
                final int pointerIndex = MotionEventCompat.getActionIndex(ev);
                final int pointerId = MotionEventCompat.getPointerId(ev, pointerIndex);
                if (pointerId == mActivePointerId) {
                    final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
                    mActivePointerId = MotionEventCompat.getPointerId(ev, newPointerIndex);
                }
                mLastMotionX = MotionEventCompat.getX(ev, MotionEventCompat.findPointerIndex(ev, mActivePointerId));
                break;
        }

        return true;
    };

    /**
     * Set bounds for the right textView including clip padding.
     *
     * @param curViewBound
     *            current bounds.
     * @param curViewWidth
     *            width of the view.
     */
    private void clipViewOnTheRight(RectF curViewBound, float curViewWidth, int right) {
    	
    		curViewBound.right = right - mClipPadding;
    		curViewBound.left = curViewBound.right - curViewWidth;
    }

    /**
     * Set bounds for the left textView including clip padding.
     *
     * @param curViewBound
     *            current bounds.
     * @param curViewWidth
     *            width of the view.
     */
    private void clipViewOnTheLeft(RectF curViewBound, float curViewWidth, int left) {
    	
    		curViewBound.left = left + mClipPadding;
    		curViewBound.right = mClipPadding + curViewWidth;
    }

    /**
     * Calculate views bounds and scroll them according to the current index
     *
     * @param paint
     * @param currentIndex
     * @return
     */
    private ArrayList<RectF> calculateAllBounds(Paint paint) {
        ArrayList<RectF> list = new ArrayList<RectF>();
        //For each views (If no values then add a fake one)
        final int count = mViewPager.getAdapter().getCount();
        final int width = getWidth();
        final int halfWidth = width / 2;
        
        
    	
        for (int i = 0; i < count; i++) {
        	
        	
            
            RectF bounds = calcBounds(i, paint);
            float w = (bounds.right - bounds.left);
            float h = (bounds.bottom - bounds.top);
            bounds.left = (halfWidth) - (w / 2) - mCurrentOffset + ((i - mCurrentPage) * width);
            bounds.right = bounds.left + w;
            bounds.top = 0;
            bounds.bottom = h;
            
            
            list.add(bounds);
        }

        return list;
    }

    /**
     * Calculate the bounds for a view's title
     *
     * @param index
     * @param paint
     * @return
     */
    private RectF calcBounds(int index, Paint paint) {
        //Calculate the text bounds
        RectF bounds = new RectF();
        bounds.right = paint.measureText(mTitleProvider.getTitle(index));
        bounds.bottom = paint.descent() - paint.ascent();
        return bounds;
    }

    @Override
    public void setViewPager(ViewPager view) {
        final PagerAdapter adapter = view.getAdapter();
        if (adapter == null) {
            throw new IllegalStateException("ViewPager does not have adapter instance.");
        }
        if (!(adapter instanceof TitleProvider)) {
            throw new IllegalStateException("ViewPager adapter must implement TitleProvider to be used with TitlePageIndicator.");
        }
        mViewPager = view;
        mViewPager.setOnPageChangeListener(this);
        mTitleProvider = (TitleProvider)adapter;
        invalidate();
    }

    @Override
    public void setViewPager(ViewPager view, int initialPosition) {
        setViewPager(view);
        setCurrentItem(initialPosition);
    }

    @Override
    public void notifyDataSetChanged() {
        invalidate();
    }

    /**
     * Set a callback listener for the center item click.
     *
     * @param listener Callback instance.
     */
    public void setOnCenterItemClickListener(OnCenterItemClickListener listener) {
        mCenterItemClickListener = listener;
    }

    @Override
    public void setCurrentItem(int item) {
        if (mViewPager == null) {
            throw new IllegalStateException("ViewPager has not been bound.");
        }
        mViewPager.setCurrentItem(item);
        mCurrentPage = item;
        invalidate();
    }

    @Override
    public void onPageScrollStateChanged(int state) {
        mScrollState = state;

        if (mListener != null) {
            mListener.onPageScrollStateChanged(state);
        }
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        mCurrentPage = position;
        mCurrentOffset = positionOffsetPixels;
        invalidate();

        if (mListener != null) {
            mListener.onPageScrolled(position, positionOffset, positionOffsetPixels);
        }
    }

    @Override
    public void onPageSelected(int position) {
        if (mScrollState == ViewPager.SCROLL_STATE_IDLE) {
            mCurrentPage = position;
            invalidate();
        }

        if (mListener != null) {
            mListener.onPageSelected(position);
        }
    }

    @Override
    public void setOnPageChangeListener(ViewPager.OnPageChangeListener listener) {
        mListener = listener;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        //Measure our width in whatever mode specified
        final int measuredWidth = MeasureSpec.getSize(widthMeasureSpec);

        //Determine our height
        float height = 0;
        final int heightSpecMode = MeasureSpec.getMode(heightMeasureSpec);
        if (heightSpecMode == MeasureSpec.EXACTLY) {
            //We were told how big to be
            height = MeasureSpec.getSize(heightMeasureSpec);
        } else {
            //Calculate the text bounds
            RectF bounds = new RectF();
            bounds.bottom = mPaintText.descent()-mPaintText.ascent();
            height = bounds.bottom - bounds.top + mFooterLineHeight + mFooterPadding + mTopPadding;
            if (mFooterIndicatorStyle != IndicatorStyle.None) {
                height += mFooterIndicatorHeight;
            }
        }
        final int measuredHeight = (int)height;

        setMeasuredDimension(measuredWidth, measuredHeight);
    }

    @Override
	public void onConfigurationChanged(Configuration newConfig){        
	    super.onConfigurationChanged(newConfig);
	    mCurrentPage = mViewPager.getCurrentItem();
	    this.invalidate();
    }
    @Override
    public void onRestoreInstanceState(Parcelable state) {
        SavedState savedState = (SavedState)state;
        super.onRestoreInstanceState(savedState.getSuperState());
        mCurrentPage = savedState.currentPage;
        requestLayout();
    }
    

    @Override
    public Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        SavedState savedState = new SavedState(superState);
        savedState.currentPage = mCurrentPage;
        return savedState;
    }

    static class SavedState extends BaseSavedState {
        int currentPage;

        public SavedState(Parcelable superState) {
            super(superState);
        }

        private SavedState(Parcel in) {
            super(in);
            currentPage = in.readInt();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeInt(currentPage);
        }

        public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState>() {
            @Override
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }
}
