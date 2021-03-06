package com.example.xyzreader.ui;

import android.app.LoaderManager;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ShareCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.format.DateUtils;
import android.transition.Slide;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;

public class ArticleDetailActivity extends AppCompatActivity
        implements LoaderManager.LoaderCallbacks<Cursor>, SimpleGestureFilter.SimpleGestureListener {

    private SimpleGestureFilter detector;

    private Cursor mCursor;
    private long mSelectedId;
    private String mTitle;

    ImageView mToolbarImage;
    LinearLayout mMetaLayout;

    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss");
    private SimpleDateFormat outputFormat = new SimpleDateFormat();
    private GregorianCalendar START_OF_EPOCH = new GregorianCalendar(2, 1, 1);

    public static final String ITEM_ID = "itemId";
    public static final String ITEM_NAME = "itemName";
    public static final String SLIDE = "slide";

    private CollapsingToolbarLayout mCollapsingToolbarLayout;
    private String mBodyText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        TextView titleView = (TextView) findViewById(R.id.article_title);
        mToolbarImage = (ImageView) findViewById(R.id.iv_article_image);

        mMetaLayout = (LinearLayout) findViewById(R.id.ll_meta_information);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        mCollapsingToolbarLayout = (CollapsingToolbarLayout) findViewById(R.id.collapsing_toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        if (savedInstanceState == null) {
            if (getIntent().getExtras() != null) {
                mSelectedId = getIntent().getLongExtra(ITEM_ID, 0);

                mTitle = getIntent().getStringExtra(ITEM_NAME);
                String defaultTitle = getString(R.string.app_name);

                mCollapsingToolbarLayout.setTitle(defaultTitle);
                titleView.setText(defaultTitle);

                if (mTitle != null) {
                    if (!mTitle.isEmpty()) {
                        mCollapsingToolbarLayout.setTitle(mTitle);
                        titleView.setText(mTitle);
                    }
                } else {
                    if (getIntent().getBooleanExtra(SLIDE, false)) {
                        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_right);
                    } else {
                        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_left);
                    }

                }
            }
        } else {
            mSelectedId = savedInstanceState.getLong(ITEM_ID, 0);
            mTitle = savedInstanceState.getString(ITEM_NAME);
        }

        getLoaderManager().initLoader(0, null, this);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            Slide slide = new Slide(Gravity.END);
            slide.setDuration(400);
            slide.addTarget(R.id.meta_bar);
            slide.setInterpolator(new DecelerateInterpolator());
            getWindow().setEnterTransition(slide);
        }

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(Intent.createChooser(ShareCompat.IntentBuilder.from(ArticleDetailActivity.this)
                        .setType("text/plain")
                        .setText("Some sample text")
                        .getIntent(), getString(R.string.action_share)));
            }
        });

        final TextView bodyView = (TextView) findViewById(R.id.article_body);
        final TextView loadingLayout = (TextView) findViewById(R.id.loading_text);
        bodyView.postDelayed(new Runnable() {
            @Override
            public void run() {
                loadingLayout.setVisibility(View.GONE);
                bodyView.setText(Html.fromHtml(mBodyText));
            }
        }, 650);

        detector = new SimpleGestureFilter(this, this);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putLong(ITEM_ID, mSelectedId);
        outState.putString(ITEM_NAME, mTitle);

        super.onSaveInstanceState(outState);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                break;
        }

        return true;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return ArticleLoader.newInstanceForItemId(this, mSelectedId);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        mCursor = cursor;
        if (mCursor != null && !mCursor.moveToFirst()) {
            mCursor.close();
            mCursor = null;
        }

        bindViews();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mCursor = null;
        bindViews();
    }

    private void bindViews() {
        TextView titleView = (TextView) findViewById(R.id.article_title);
        TextView bylineView = (TextView) findViewById(R.id.article_byline);

        if (mCursor != null) {
            String articleTitle = mCursor.getString(ArticleLoader.Query.TITLE);

            titleView.setText(articleTitle);
            mCollapsingToolbarLayout.setTitle(articleTitle);

            mBodyText = mCursor.getString(ArticleLoader.Query.BODY);

            Date publishedDate = parsePublishedDate();
            if (!publishedDate.before(START_OF_EPOCH.getTime())) {
                bylineView.setText(Html.fromHtml(
                        DateUtils.getRelativeTimeSpanString(
                                publishedDate.getTime(),
                                System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                                DateUtils.FORMAT_ABBREV_ALL).toString()
                                + " by <font color='#ffffff'>"
                                + mCursor.getString(ArticleLoader.Query.AUTHOR)
                                + "</font>"));

            } else {
                // If date is before 1902, just show the string
                bylineView.setText(Html.fromHtml(
                        outputFormat.format(publishedDate) + " by <font color='#ffffff'>"
                                + mCursor.getString(ArticleLoader.Query.AUTHOR)
                                + "</font>"));

            }

            ImageLoaderHelper.getInstance(this).getImageLoader()
                    .get(mCursor.getString(ArticleLoader.Query.PHOTO_URL), new ImageLoader.ImageListener() {
                        @Override
                        public void onResponse(ImageLoader.ImageContainer imageContainer, boolean b) {
                            Bitmap bitmap = imageContainer.getBitmap();
                            if (bitmap != null) {
                                mToolbarImage.setImageBitmap(bitmap);

                                Palette palette = Palette.from(bitmap).generate();
                                int mutedColor = palette.getDarkMutedColor(0xFF333333);
                                mMetaLayout.setBackgroundColor(mutedColor);
                            }
                        }

                        @Override
                        public void onErrorResponse(VolleyError volleyError) {

                        }
                    });
        }
    }

    private Date parsePublishedDate() {
        try {
            String date = mCursor.getString(ArticleLoader.Query.PUBLISHED_DATE);
            return dateFormat.parse(date);
        } catch (ParseException ex) {
            Log.e("error", ex.getMessage());
            Log.i("information", "passing today's date");
            return new Date();
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent me) {
        this.detector.onTouchEvent(me);
        return super.dispatchTouchEvent(me);
    }

    @Override
    public void onSwipe(int direction) {
        switch (direction) {
            case SimpleGestureFilter.SWIPE_RIGHT:
                changeActivity(false);
                break;
            case SimpleGestureFilter.SWIPE_LEFT:
                changeActivity(true);
                break;
        }
    }

    private void changeActivity(boolean next) {
        int nextIndex = ArticleListActivity.ids.indexOf(mSelectedId);
        boolean slide = false;

        if (next) {
            if (nextIndex == ArticleListActivity.ids.size() - 1) {
                nextIndex = -1;
            }
            nextIndex++;
            slide = true;
        } else {
            if (nextIndex == 0) {
                nextIndex = ArticleListActivity.ids.size();
            }
            nextIndex--;
        }

        Long changeId = ArticleListActivity.ids.get(nextIndex);

        Intent viewDetails = new Intent(ArticleDetailActivity.this, ArticleDetailActivity.class);
        viewDetails.putExtra(ArticleDetailActivity.ITEM_ID, changeId);
        viewDetails.putExtra(SLIDE, slide);
        startActivity(viewDetails);
        this.finish();
    }
}
