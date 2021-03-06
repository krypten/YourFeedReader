package com.udacity.feedreader.ui;

import android.app.Activity;
import android.app.ActivityOptions;
import android.app.LoaderManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.format.DateUtils;
import android.transition.TransitionInflater;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.udacity.feedreader.R;
import com.udacity.feedreader.data.ArticleLoader;
import com.udacity.feedreader.data.ItemsContract;
import com.udacity.feedreader.data.UpdaterService;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * An activity representing a list of Articles. This activity has different presentations for
 * handset and tablet-size devices. On handsets, the activity presents a list of items, which when
 * touched, lead to a {@link ArticleDetailActivity} representing item details. On tablets, the
 * activity presents a grid of items as cards.
 */
public class ArticleListActivity extends AppCompatActivity implements
	LoaderManager.LoaderCallbacks<Cursor> {

	private static final String TAG = ArticleListActivity.class.toString();
	private Toolbar mToolbar;
	private SwipeRefreshLayout mSwipeRefreshLayout;
	private RecyclerView mRecyclerView;

	private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss");
	// Use default locale format
	private SimpleDateFormat outputFormat = new SimpleDateFormat();
	// Most time functions can only handle 1902 - 2037
	private GregorianCalendar START_OF_EPOCH = new GregorianCalendar(2, 1, 1);

	@RequiresApi(api = Build.VERSION_CODES.M)
	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_article_list);
		getWindow().setSharedElementEnterTransition(TransitionInflater.from(this)
			.inflateTransition(R.transition.image_transform));

		mToolbar = (Toolbar) findViewById(R.id.toolbar);
		final CollapsingToolbarLayout toolbarLayout = (CollapsingToolbarLayout) findViewById(R.id.toolbar_container);
		toolbarLayout.setTitle(getResources().getString(R.string.app_title));
		toolbarLayout.setCollapsedTitleTextColor(Color.WHITE);
		toolbarLayout.setExpandedTitleColor(Color.TRANSPARENT);

		mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh_layout);
		mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);

		getLoaderManager().initLoader(0, null, this);

		if (savedInstanceState == null) {
			refresh();
		}
	}

	private void refresh() {
		startService(new Intent(this, UpdaterService.class));
	}

	@Override
	protected void onStart() {
		super.onStart();
		registerReceiver(mRefreshingReceiver,
			new IntentFilter(UpdaterService.BROADCAST_ACTION_STATE_CHANGE));
	}

	@Override
	protected void onStop() {
		super.onStop();
		unregisterReceiver(mRefreshingReceiver);
	}

	private boolean mIsRefreshing = false;

	private BroadcastReceiver mRefreshingReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (UpdaterService.BROADCAST_ACTION_STATE_CHANGE.equals(intent.getAction())) {
				mIsRefreshing = intent.getBooleanExtra(UpdaterService.EXTRA_REFRESHING, false);
				updateRefreshingUI();
			}
		}
	};

	private void updateRefreshingUI() {
		mSwipeRefreshLayout.setRefreshing(mIsRefreshing);
	}

	@Override
	public Loader<Cursor> onCreateLoader(final int i, final Bundle bundle) {
		return ArticleLoader.newAllArticlesInstance(this);
	}

	@Override
	public void onLoadFinished(final Loader<Cursor> cursorLoader, final Cursor cursor) {
		final Adapter adapter = new Adapter(cursor, this);
		adapter.setHasStableIds(true);
		mRecyclerView.setAdapter(adapter);
		final int columnCount = getResources().getInteger(R.integer.list_column_count);
		final StaggeredGridLayoutManager sglm =
			new StaggeredGridLayoutManager(columnCount, StaggeredGridLayoutManager.VERTICAL);
		mRecyclerView.setLayoutManager(sglm);
	}

	@Override
	public void onLoaderReset(final Loader<Cursor> loader) {
		mRecyclerView.setAdapter(null);
	}

	private class Adapter extends RecyclerView.Adapter<ViewHolder> {
		private Activity mActivity;
		private Cursor mCursor;

		public Adapter(final Cursor cursor, final Activity activity) {
			mCursor = cursor;
			mActivity = activity;
		}

		@Override
		public long getItemId(final int position) {
			mCursor.moveToPosition(position);
			return mCursor.getLong(ArticleLoader.Query._ID);
		}

		@Override
		public ViewHolder onCreateViewHolder(final ViewGroup parent, final int viewType) {
			View view = getLayoutInflater().inflate(R.layout.list_item_article, parent, false);
			final ViewHolder vh = new ViewHolder(view);
			view.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					final DynamicHeightNetworkImageView imageView = (DynamicHeightNetworkImageView) view.findViewById(R.id.thumbnail);
					final Bundle bundle = ActivityOptions
						.makeSceneTransitionAnimation(
							mActivity,
							imageView,
							imageView.getTransitionName())
						.toBundle();
					startActivity(new Intent(Intent.ACTION_VIEW,
						ItemsContract.Items.buildItemUri(getItemId(vh.getAdapterPosition()))), bundle);
				}
			});
			return vh;
		}

		private Date parsePublishedDate() {
			try {
				final String date = mCursor.getString(ArticleLoader.Query.PUBLISHED_DATE);
				return dateFormat.parse(date);
			} catch (ParseException ex) {
				Log.e(TAG, ex.getMessage());
				Log.i(TAG, "passing today's date");
				return new Date();
			}
		}

		@Override
		public void onBindViewHolder(ViewHolder holder, int position) {
			mCursor.moveToPosition(position);
			holder.titleView.setText(mCursor.getString(ArticleLoader.Query.TITLE));
			final Date publishedDate = parsePublishedDate();
			if (!publishedDate.before(START_OF_EPOCH.getTime())) {
				holder.subtitleView.setText(Html.fromHtml(
					DateUtils.getRelativeTimeSpanString(
						publishedDate.getTime(),
						System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
						DateUtils.FORMAT_ABBREV_ALL).toString()
						+ "<br/>" + " by "
						+ mCursor.getString(ArticleLoader.Query.AUTHOR)));
			} else {
				holder.subtitleView.setText(Html.fromHtml(
					outputFormat.format(publishedDate)
						+ "<br/>" + " by "
						+ mCursor.getString(ArticleLoader.Query.AUTHOR)));
			}
			Glide.with(mActivity)
				.load(mCursor.getString(ArticleLoader.Query.THUMB_URL))
				.centerCrop()
				.into(holder.thumbnailView);
			holder.thumbnailView.setAspectRatio(mCursor.getFloat(ArticleLoader.Query.ASPECT_RATIO));
		}

		@Override
		public int getItemCount() {
			return mCursor.getCount();
		}
	}

	public static class ViewHolder extends RecyclerView.ViewHolder {
		public DynamicHeightNetworkImageView thumbnailView;
		public TextView titleView;
		public TextView subtitleView;

		public ViewHolder(final View view) {
			super(view);
			thumbnailView = (DynamicHeightNetworkImageView) view.findViewById(R.id.thumbnail);
			titleView = (TextView) view.findViewById(R.id.article_title);
			subtitleView = (TextView) view.findViewById(R.id.article_subtitle);
		}
	}
}
