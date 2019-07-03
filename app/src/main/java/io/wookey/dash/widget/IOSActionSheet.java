package io.wookey.dash.widget;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Build;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StyleRes;
import android.text.TextUtils;
import android.util.StateSet;
import android.util.TypedValue;
import android.view.*;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.TranslateAnimation;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import io.wookey.dash.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by xinle on 16/12/24.
 */

public class IOSActionSheet extends Dialog implements View.OnClickListener {
    public interface IActionSheetListener {
        void onActionSheetItemClick(IOSActionSheet actionSheet, int itemPosition, ItemModel itemModel);
    }

    private static final int TRANSLATE_DURATION = 100;  // 动画执行时长
    private static final String DEFAULT_CANCEL_TITLE = "取消";

    private Context mContext;

    private LinearLayout mView;

    private @StyleRes
    int mStyleId;
    private Attributes mAttrs;

    private CharSequence mTitleStr;
    private CharSequence mSubTitleStr;
    private CharSequence mCancelButtonTitle = DEFAULT_CANCEL_TITLE;
    private boolean mHaveCancelBtn = true;

    private List<ItemModel> mOtherButtonTitles;
    private IActionSheetListener mListener;

    private boolean mDismissed = true;
    private boolean mCancelableOnTouchOutside = true;

    public IOSActionSheet(@NonNull Activity activity) {
        this(activity, null);
    }

    public IOSActionSheet(@NonNull Activity activity, @StyleRes int mStyleId) {
        super(activity);
        this.mContext = activity;
        this.mStyleId = mStyleId;
        init();
    }

    private IOSActionSheet(@NonNull Activity activity, @Nullable Attributes attributes) {
        super(activity);
        this.mContext = activity;
        this.mAttrs = attributes;
        init();
    }

    private IOSActionSheet(@NonNull Builder builder) {
        this(builder.mActivity, builder.styleId);

        if (builder.mAttrs != null) {
            mAttrs = builder.mAttrs;
        }

        mTitleStr = builder.mTitleStr;
        mSubTitleStr = builder.mSubTitleStr;

        mOtherButtonTitles = builder.mOtherButtonTitles;
        mCancelButtonTitle = builder.mCancelTitleStr;
        mHaveCancelBtn = builder.mHaveCancelBtn;

        mListener = builder.mListener;
        mCancelableOnTouchOutside = builder.mCancelableOnTouchOutside;
        setCanceledOnTouchOutside(mCancelableOnTouchOutside);
    }

    private void init() {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setCanceledOnTouchOutside(mCancelableOnTouchOutside);

        if (mAttrs == null) {
            mAttrs = readAttribute();
        }

        LinearLayout linearLay = new LinearLayout(mContext);
        linearLay.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        linearLay.setOrientation(LinearLayout.VERTICAL);

        mView = linearLay;

        int type = TranslateAnimation.RELATIVE_TO_SELF;
        TranslateAnimation animation = new TranslateAnimation(type, 0, type, 0, type, 1, type, 0);
        animation.setDuration(TRANSLATE_DURATION);
        mView.startAnimation(animation);

    }

    @SuppressLint("ResourceType")
    private Attributes readAttribute() {
        Attributes attrs = new Attributes(mContext);
        if (mStyleId <= 0) {
            return mAttrs != null ? mAttrs : attrs;
        }

        TypedArray a = mContext.obtainStyledAttributes(mStyleId, R.styleable.IOSActionSheet);

        attrs.background = a.getColor(R.styleable.IOSActionSheet_ias_background, attrs.background);
        attrs.chooseBackground = a.getColor(R.styleable.IOSActionSheet_ias_chooseBackground, attrs.chooseBackground);

        attrs.titleTextColor = a.getColor(R.styleable.IOSActionSheet_ias_titleTextColor, attrs.titleTextColor);
        attrs.cancelButtonTextColor = a.getColor(R.styleable.IOSActionSheet_ias_cancelButtonTextColor, attrs.cancelButtonTextColor);
        attrs.otherButtonTextColor = a.getColor(R.styleable.IOSActionSheet_ias_otherButtonTextColor, attrs.otherButtonTextColor);
        attrs.warningButtonTextColor = a.getColor(R.styleable.IOSActionSheet_ias_warningButtonTextColor, attrs.warningButtonTextColor);
        attrs.checkButtonTextColor = a.getColor(R.styleable.IOSActionSheet_ias_checkButtonTextColor, attrs.checkButtonTextColor);
        attrs.checkCancelTextColor = a.getColor(R.styleable.IOSActionSheet_ias_checkCancelTextColor, attrs.checkCancelTextColor);

        attrs.titleTextSize = a.getDimensionPixelSize(R.styleable.IOSActionSheet_ias_titleTextSize, attrs.titleTextSize);
        attrs.subTitleTextSize = a.getDimensionPixelSize(R.styleable.IOSActionSheet_ias_subTitleTextSize, attrs.subTitleTextSize);
        attrs.cancelButtonTextSize = a.getDimensionPixelSize(R.styleable.IOSActionSheet_ias_cancelButtonTextSize, attrs.cancelButtonTextSize);
        attrs.otherButtonTextSize = a.getDimensionPixelSize(R.styleable.IOSActionSheet_ias_otherButtonTextSize, attrs.otherButtonTextSize);
        attrs.warningButtonTextSize = a.getDimensionPixelSize(R.styleable.IOSActionSheet_ias_warningButtonTextSize, attrs.warningButtonTextSize);

        attrs.lineHeight = (int) a.getDimension(R.styleable.IOSActionSheet_ias_lineHeight, attrs.lineHeight);
        attrs.cancelButtonMarginTop = (int) a.getDimension(R.styleable.IOSActionSheet_ias_cancelButtonMarginTop, attrs.cancelButtonMarginTop);
        attrs.radius = (int) a.getDimension(R.styleable.IOSActionSheet_ias_radius, attrs.radius);
        attrs.paddingLeft = (int) a.getDimension(R.styleable.IOSActionSheet_ias_paddingLeft, attrs.paddingLeft);
        attrs.paddingTop = (int) a.getDimension(R.styleable.IOSActionSheet_ias_paddingTop, attrs.paddingTop);
        attrs.paddingRight = (int) a.getDimension(R.styleable.IOSActionSheet_ias_paddingRight, attrs.paddingRight);
        attrs.paddingBottom = (int) a.getDimension(R.styleable.IOSActionSheet_ias_paddingBottom, attrs.paddingBottom);

        a.recycle();
        return attrs;
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        InputMethodManager imm = (InputMethodManager) mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm.isActive()) {
            View focusView = ((Activity) mContext).getCurrentFocus();
            if (focusView != null) {
                imm.hideSoftInputFromWindow(focusView.getWindowToken(), 0);
            }
        }
    }

    @Override
    public void show() {
        if (!mDismissed)
            return;

        createItems();

        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        setContentView(mView, params);

        if (getWindow() != null) {
            WindowManager.LayoutParams layoutParams = getWindow().getAttributes();
            layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
            layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
            layoutParams.gravity = Gravity.BOTTOM;
            getWindow().setAttributes(layoutParams);
            getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        mDismissed = false;
        super.show();
    }

    private void createItems() {
        mView.removeAllViews();
        mView.setPadding(mAttrs.paddingLeft, mAttrs.paddingTop, mAttrs.paddingRight, mAttrs.paddingBottom);

        DrawableSelector mDrawableSelector = new DrawableSelector(mAttrs.radius);

        int childCount = 0;
        if (!TextUtils.isEmpty(mTitleStr)) {
            childCount++;
        }

        if (mOtherButtonTitles != null) {
            childCount += mOtherButtonTitles.size();
        }

        if (!TextUtils.isEmpty(mCancelButtonTitle) && mHaveCancelBtn) {
            if (mOtherButtonTitles == null) {
                mOtherButtonTitles = new ArrayList<>();
            }
            mOtherButtonTitles.add(new ItemModel(mCancelButtonTitle, ItemModel.ITEM_TYPE_CANCEL));
        }

        if (!TextUtils.isEmpty(mTitleStr)) {
            LinearLayout titleLay = new LinearLayout(mContext);
            titleLay.setOrientation(LinearLayout.VERTICAL);
            titleLay.setMinimumHeight(mAttrs.lineHeight);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                titleLay.setBackground(mDrawableSelector.getTopBg(childCount, mView.getChildCount()));
            } else {
                titleLay.setBackgroundDrawable(mDrawableSelector.getTopBg(childCount, mView.getChildCount()));
            }

            TextView titleTextView = new TextView(mContext);
            titleTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, mAttrs.titleTextSize);
            titleTextView.setTextColor(mAttrs.titleTextColor);
            LinearLayout.LayoutParams params1 = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params1.weight = 1;
            params1.setMargins(dp2px(8), dp2px(4), dp2px(8), 0);
            titleTextView.setGravity(Gravity.CENTER);
            titleTextView.setLayoutParams(params1);
            titleTextView.setText(mTitleStr);
            titleLay.addView(titleTextView);

            if (!TextUtils.isEmpty(mSubTitleStr)) {
                TextView subTitleTextView = new TextView(mContext);
                subTitleTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, mAttrs.subTitleTextSize);
                subTitleTextView.setTextColor(mAttrs.titleTextColor);
                LinearLayout.LayoutParams params2 = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                params2.weight = 1;
                params2.setMargins(dp2px(8), dp2px(10), dp2px(8), dp2px(10));
                subTitleTextView.setGravity(Gravity.CENTER);
                subTitleTextView.setLayoutParams(params2);
                subTitleTextView.setText(mSubTitleStr);

                titleLay.addView(subTitleTextView);
            }

            mView.addView(titleLay);
        }

        int topChildCount = mView.getChildCount();
        if (topChildCount > 0) {
            mView.addView(createLineView());
        }

        if (mOtherButtonTitles != null && mOtherButtonTitles.size() > 0) {

            for (int i = 0; i < mOtherButtonTitles.size(); i++) {
                TextView button = new TextView(mContext);
//                button.setId(i);
                button.setGravity(Gravity.CENTER);
                button.setSingleLine(true);
                button.setPadding(dp2px(10), 0, dp2px(10), 0);
                button.setEllipsize(TextUtils.TruncateAt.MIDDLE);
                button.setOnClickListener(this);
                ItemModel itemModel = mOtherButtonTitles.get(i);

                button.setTag(itemModel);

                Drawable bg = ItemModel.ITEM_TYPE_CANCEL == itemModel.itemType ?
                        mDrawableSelector.createDrawable(mDrawableSelector.rDefault) :
                        mDrawableSelector.getTopBg(childCount, i + topChildCount);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    button.setBackground(bg);
                } else {
                    button.setBackgroundDrawable(bg);
                }

                LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, mAttrs.lineHeight);
                button.setText(itemModel.itemTitle);
                if (itemModel.itemType == ItemModel.ITEM_TYPE_DEFAULT) {
                    ColorStateList colorStateList = new ColorStateList(new int[][]{
                            {-android.R.attr.state_pressed},
                            {android.R.attr.state_pressed}},
                            new int[]{mAttrs.otherButtonTextColor, mAttrs.checkButtonTextColor});
                    button.setTextColor(colorStateList);
                    button.setTextSize(TypedValue.COMPLEX_UNIT_PX, mAttrs.otherButtonTextSize);
                } else if (itemModel.itemType == ItemModel.ITEM_TYPE_WARNING) {
                    ColorStateList colorStateList = new ColorStateList(new int[][]{
                            {-android.R.attr.state_pressed},
                            {android.R.attr.state_pressed}},
                            new int[]{mAttrs.warningButtonTextColor, mAttrs.checkButtonTextColor});
                    button.setTextColor(colorStateList);
                    button.setTextSize(TypedValue.COMPLEX_UNIT_PX, mAttrs.warningButtonTextSize);
                } else if (itemModel.itemType == ItemModel.ITEM_TYPE_CANCEL) {
                    ColorStateList colorStateList = new ColorStateList(new int[][]{
                            {-android.R.attr.state_pressed},
                            {android.R.attr.state_pressed}},
                            new int[]{mAttrs.cancelButtonTextColor, mAttrs.checkCancelTextColor});
                    button.setTextColor(colorStateList);
                    button.setTextSize(TypedValue.COMPLEX_UNIT_PX, mAttrs.cancelButtonTextSize);
                    layoutParams.setMargins(0, mAttrs.cancelButtonMarginTop, 0, 0);
                    button.getPaint().setFakeBoldText(true);
                }

                button.setLayoutParams(layoutParams);
                mView.addView(button);

                if (i != mOtherButtonTitles.size() - 1 - (TextUtils.isEmpty(mCancelButtonTitle) ? 0 : 1)
                        && itemModel.itemType != ItemModel.ITEM_TYPE_CANCEL) {
                    mView.addView(createLineView());
                }
            }
        }
    }

    private View createLineView() {
        View line = new View(mContext);
        line.setBackgroundColor(Color.parseColor("#E1E1E1"));
        line.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));
        return line;
    }

    @Override
    public void onClick(View v) {
        if (v.getTag() instanceof ItemModel) {
            ItemModel itemModel = (ItemModel) v.getTag();

            if (ItemModel.ITEM_TYPE_CANCEL == itemModel.itemType) {
                dismiss();
                return;
            }

            if (mListener != null) {
                mListener.onActionSheetItemClick(this, v.getId(), itemModel);
            }

            dismiss();
        }
    }

    @Override
    public void dismiss() {
        if (mDismissed) {
            return;
        }
        mDismissed = true;

        if (mView != null) {
            int type = TranslateAnimation.RELATIVE_TO_SELF;
            TranslateAnimation an = new TranslateAnimation(type, 0, type, 0, type, 0, type, 1);
            AlphaAnimation alpha = new AlphaAnimation(1, 0);

            AnimationSet animationSet = new AnimationSet(true);
            animationSet.addAnimation(an);
            animationSet.addAnimation(alpha);
            animationSet.setFillAfter(true);
            animationSet.setDuration(TRANSLATE_DURATION);

            animationSet.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    IOSActionSheet.super.dismiss();
                }

                @Override
                public void onAnimationRepeat(Animation animation) {
                }
            });

            mView.startAnimation(animationSet);
        }
    }

    public void setAttributesId(@StyleRes int mAttributesId) {
        this.mStyleId = mAttributesId;
        mAttrs = readAttribute();
    }

    public void setAttrs(Attributes attrs) {
        this.mAttrs = attrs;
    }

    public void setTitleStr(CharSequence mTitleStr) {
        this.mTitleStr = mTitleStr;
    }

    public void setSubTitleStr(CharSequence mSubTitleStr) {
        this.mSubTitleStr = mSubTitleStr;
    }

    public void setCancelButtonTitle(CharSequence mCancelButtonTitle) {
        this.mCancelButtonTitle = mCancelButtonTitle;
    }

    public void setHaveCancleBtn(boolean haveCancleBtn) {
        this.mHaveCancelBtn = haveCancleBtn;
    }

    public void setOtherButtonTitles(List<ItemModel> mOtherButtonTitles) {
        this.mOtherButtonTitles = mOtherButtonTitles;
    }

    public void setOtherButtonTitlesSimple(List<? extends CharSequence> mOtherButtonTitles) {
        if (mOtherButtonTitles == null) {
            this.mOtherButtonTitles = null;
        } else {
            this.mOtherButtonTitles = new ArrayList<>();
            for (CharSequence item : mOtherButtonTitles) {
                this.mOtherButtonTitles.add(new ItemModel(item));
            }
        }
    }

    public void otherButtonTitlesSimple(CharSequence... mOtherButtonTitles) {
        if (mOtherButtonTitles == null) {
            this.mOtherButtonTitles = null;
        } else {
            this.mOtherButtonTitles = new ArrayList<>();
            for (CharSequence item : mOtherButtonTitles) {
                this.mOtherButtonTitles.add(new ItemModel(item));
            }
        }
    }

    public void setItemClickListener(IActionSheetListener mListener) {
        this.mListener = mListener;
    }

    private class DrawableSelector {
        private float r;
        private final float r1 = 0f;

        private final float[] rMiddle;
        private final float[] rDefault;
        private final float[] rTop;
        private final float[] rBottom;

        public DrawableSelector(float cornerRadius) {
            r = cornerRadius;

            rMiddle = new float[]{r1, r1, r1, r1, r1, r1, r1, r1};
            rDefault = new float[]{r, r, r, r, r, r, r, r};
            rTop = new float[]{r, r, r, r, r1, r1, r1, r1};
            rBottom = new float[]{r1, r1, r1, r1, r, r, r, r};
        }

        private Drawable createDrawable(float[] r) {
            GradientDrawable drawable = new GradientDrawable();
            drawable.setShape(GradientDrawable.RECTANGLE);
            drawable.setColor(mAttrs.background);
            drawable.setCornerRadii(r);

            GradientDrawable checkDrawable = new GradientDrawable();
            checkDrawable.setShape(GradientDrawable.RECTANGLE);
            checkDrawable.setColor(mAttrs.chooseBackground);
            checkDrawable.setCornerRadii(r);

            StateListDrawable stateListDrawable = new StateListDrawable();
            stateListDrawable.addState(new int[]{android.R.attr.state_pressed}, checkDrawable);
            stateListDrawable.addState(StateSet.WILD_CARD, drawable);

            return drawable;
        }

        private Drawable getTopBg(int childCount, int childIndex) {
            if (childCount == 1) {
                return createDrawable(rDefault);
            } else {
                if (childIndex == 0) {
                    return createDrawable(rTop);
                } else if (childIndex == childCount - 1) {
                    return createDrawable(rBottom);
                } else {
                    return createDrawable(rMiddle);
                }
            }
        }
    }

    public static class ItemModel {

        public static final int ITEM_TYPE_DEFAULT = 0;
        public static final int ITEM_TYPE_WARNING = 1;
        public static final int ITEM_TYPE_CANCEL = 2;

        @IntDef({ITEM_TYPE_DEFAULT, ITEM_TYPE_WARNING, ITEM_TYPE_CANCEL})
        @interface ItemType {
        }

        private CharSequence itemTitle;
        @ItemType
        private int itemType = ITEM_TYPE_DEFAULT;

        public ItemModel(CharSequence itemTitle, @ItemType int itemType) {
            this.itemType = itemType;
            this.itemTitle = itemTitle;
        }

        public ItemModel(CharSequence itemTitle) {
            this.itemTitle = itemTitle;
        }

        public CharSequence getItemTitle() {
            return itemTitle;
        }

        public int getItemType() {
            return itemType;
        }
    }

    public static class Attributes {
        private Context mContext;

        public int background;            // 正常情况下的背景色
        public int chooseBackground;      // 选择状态下的背景色

        public int titleTextColor;             // 头部的文字的颜色
        public int cancelButtonTextColor;      // 取消按钮的颜色
        public int otherButtonTextColor;       // 其他按钮的颜色
        public int warningButtonTextColor;     // 警告按钮的颜色
        public int checkButtonTextColor;       // 选中状态下的文字的颜色
        public int checkCancelTextColor;       // 选中状态下的文字的颜色

        public int titleTextSize;            // 头部文字的大小
        public int subTitleTextSize;         // 二级头部文字的大小
        public int cancelButtonTextSize;     // 取消按钮的大小
        public int otherButtonTextSize;      // 其他按钮的大小
        public int warningButtonTextSize;    // 警告按钮的大小

        public int lineHeight;                // 每一行的高度
        public int cancelButtonMarginTop;     // 取消按钮其他按钮之间的间距
        public int radius;                    // 圆角的半径
        public int paddingLeft;                   // 周围的padding值
        public int paddingTop;                   // 周围的padding值
        public int paddingRight;                   // 周围的padding值
        public int paddingBottom;                   // 周围的padding值

        Attributes(Context context) {
            mContext = context;

            background = Color.parseColor("#FFFFFF");
            chooseBackground = Color.argb(214, 218, 218, 218);

            titleTextColor = Color.GRAY;
            cancelButtonTextColor = Color.parseColor("#007AFF");
            otherButtonTextColor = Color.parseColor("#393939");
            warningButtonTextColor = Color.RED;
            checkButtonTextColor = Color.parseColor("#393939");
            checkCancelTextColor = Color.parseColor("#007AFF");

            titleTextSize = sp2px(16);
            subTitleTextSize = sp2px(14);
            cancelButtonTextSize = sp2px(15);
            otherButtonTextSize = sp2px(15);
            warningButtonTextSize = sp2px(15);

            lineHeight = dp2px(55);
            cancelButtonMarginTop = dp2px(10);
            radius = dp2px(8);
            paddingLeft = dp2px(17);
            paddingTop = dp2px(10);
            paddingRight = dp2px(17);
            paddingBottom = dp2px(10);
        }

        private int dp2px(int dp) {
            return (int) (dp * mContext.getResources().getDisplayMetrics().density + 0.5f);
        }

        private int sp2px(float sp) {
            return (int) (sp * mContext.getResources().getDisplayMetrics().scaledDensity + 0.5f);
        }
    }

    private int dp2px(int dp) {
        return (int) (dp * mContext.getResources().getDisplayMetrics().density + 0.5f);
    }

    public static final class Builder {
        private Activity mActivity;

        private @StyleRes
        int styleId;
        private Attributes mAttrs;

        private CharSequence mTitleStr;
        private CharSequence mSubTitleStr;
        private CharSequence mCancelTitleStr = DEFAULT_CANCEL_TITLE;
        private boolean mHaveCancelBtn = true;

        private List<ItemModel> mOtherButtonTitles;

        private IActionSheetListener mListener;

        private boolean mCancelableOnTouchOutside = true;

        public Builder(Activity activity) {
            mActivity = activity;
        }

        /**
         * 设置属性的id(不能和setAttribute一起设置)
         *
         * @param styleId
         * @return
         */
        public Builder styleId(@StyleRes int styleId) {
            this.styleId = styleId;
            return this;
        }

        /**
         * 设置属性 (不能和setAttributesId一起设置)
         *
         * @param attributes
         * @return
         */
        public Builder attributes(Attributes attributes) {
            mAttrs = attributes;
            return this;
        }

        public Builder titleStr(CharSequence titleStr) {
            mTitleStr = titleStr;
            return this;
        }

        public Builder subTitleStr(CharSequence subTitleStr) {
            mSubTitleStr = subTitleStr;
            return this;
        }

        public Builder cancelTitle(CharSequence title) {
            mCancelTitleStr = title;
            return this;
        }

        public Builder otherButtonTitles(List<ItemModel> otherButtonTitles) {
            mOtherButtonTitles = otherButtonTitles;
            return this;
        }

        public Builder otherButtonTitlesSimple(List<? extends CharSequence> items) {
            if (items != null) {
                mOtherButtonTitles = new ArrayList<>();
                for (CharSequence item : items) {
                    mOtherButtonTitles.add(new ItemModel(item));
                }
            }

            return this;
        }

        public Builder otherButtonTitlesSimple(CharSequence... items) {
            if (items != null) {
                mOtherButtonTitles = new ArrayList<>();
                for (CharSequence item : items) {
                    mOtherButtonTitles.add(new ItemModel(item));
                }
            }
            return this;
        }

        public Builder itemClickListener(IActionSheetListener listener) {
            this.mListener = listener;
            return this;
        }

        public Builder cancelAbleOnTouchOutside(boolean cancelable) {
            mCancelableOnTouchOutside = cancelable;
            return this;
        }

        public Builder haveCancelBtn(boolean haveCancleBtn) {
            mHaveCancelBtn = haveCancleBtn;
            return this;
        }

        public void show() {
            IOSActionSheet actionSheet = new IOSActionSheet(this);
            actionSheet.show();
        }
    }
}
