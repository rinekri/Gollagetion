package com.rinekri.collagetion;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.NavUtils;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.rinekri.model.InstagramPost;
import com.rinekri.model.InstagramPostsFactory;
import com.rinekri.net.NetworkConnector;
import com.rinekri.util.BitmapCollageWorker;
import com.rinekri.util.ShakeEventListener;

import java.util.ArrayList;

public class PublishFragment extends Fragment {
	public static final String EXTRA_IMAGES_IDS = "com.rinekri.images_ids";
	private static final String KEY_CURRENT_COMBINATION = "currentCombinations";
	private static final String KEY_CURRENT_IMAGES_IDS = "currentImagesIds";
	public static final String TAG = "PublishFragment";
	private static final String BITMAP_NAME = "PostCollage";

	private SensorManager mSensorManager;
	private Sensor mAccelerometer;
	private ShakeEventListener mSensorListener;

	private ImageButton mBackImageButton;
	private Button mPostButton;
	private RelativeLayout mCommonCollageLayout;
	private BitmapCollageWorker mBitmapWorker;

	private int[] mCombination;
	private String[] mGetCheckedImagesIDs;
	private ArrayList<Bitmap> mCheckedImagesBitmap;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mGetCheckedImagesIDs = (String[]) getActivity().getIntent().getSerializableExtra(EXTRA_IMAGES_IDS);
		Log.d(TAG, "Got IDs form second activity");
		for (String s: mGetCheckedImagesIDs) {
			Log.d(TAG, "ID: "+s);
		}

		if (savedInstanceState != null) {
			mGetCheckedImagesIDs = (String[]) savedInstanceState.getSerializable(KEY_CURRENT_IMAGES_IDS);
			mCombination = (int[]) savedInstanceState.getSerializable(KEY_CURRENT_COMBINATION);
//			Log.e(TAG, "Got IDs afrer restore this activity: " + mCombination.toString());

		} else {
			mCombination = InstagramPostsFactory.getFactory(getContext()).getFirstCombinationImages(mGetCheckedImagesIDs.length);
//			Log.e(TAG, "Got IDs from second activity: " + mCombination.toString());
		}

		Log.e(TAG, "Current combination:");
		for(int num: mCombination) {
			Log.d(TAG, "Number combination: "+num);
		}

		final NetworkConnector imageReturner = new NetworkConnector(getContext());
		mCheckedImagesBitmap = new ArrayList<Bitmap>();
		for(int i = 0; i < mCombination.length; i++) {
			String currentID = mGetCheckedImagesIDs[mCombination[i]];
			InstagramPost post = InstagramPostsFactory.getFactory(getContext()).getInstagramPost(currentID);
			Bitmap image = imageReturner.getBitmapFromURL(post.getPostImageURL());
			mCheckedImagesBitmap.add(image);
		}

		mSensorManager = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
		mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		mSensorListener = new ShakeEventListener();

		mSensorListener.setOnShakeListener(new ShakeEventListener.OnShakeListener() {

			@Override
			public void onShake(int count) {
				mCombination = InstagramPostsFactory.getFactory(getContext()).getCombinationImages(mGetCheckedImagesIDs.length);

				mCheckedImagesBitmap = new ArrayList<Bitmap>();
				for(int i = 0; i < mCombination.length; i++) {
					String currentID = mGetCheckedImagesIDs[mCombination[i]];
					InstagramPost post = InstagramPostsFactory.getFactory(getContext()).getInstagramPost(currentID);
					Bitmap image = imageReturner.getBitmapFromURL(post.getPostImageURL());
					mCheckedImagesBitmap.add(image);

					int childViewCounter = mCommonCollageLayout.getChildCount();

					for(int d = 0; d < childViewCounter; d++) {
						ImageView partCollage = (ImageView) mCommonCollageLayout.getChildAt(i);
						partCollage.setImageBitmap(mCheckedImagesBitmap.get(i));
					}
				}
			}
		});
	}

	@Override
	public void onResume() {
		super.onResume();
		mSensorManager.registerListener(mSensorListener, mAccelerometer, SensorManager.SENSOR_DELAY_UI);

	}

	@Override
	public void onPause() {
		mSensorManager.unregisterListener(mSensorListener);
		super.onPause();
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putSerializable(KEY_CURRENT_COMBINATION, mCombination);
		outState.putSerializable(KEY_CURRENT_IMAGES_IDS, mGetCheckedImagesIDs);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_publish, container, false);
		Toolbar toolbar = (Toolbar) v.findViewById(R.id.fragment_post_toolbar);
		toolbar.setTitle("");
		((PublishActivity) getActivity()).setSupportActionBar(toolbar);
		
		mCommonCollageLayout = (RelativeLayout) v.findViewById(R.id.common_collage);

		int childViewCounter = mCommonCollageLayout.getChildCount();

		for(int i = 0; i < childViewCounter; i++) {
			ImageView partCollage = (ImageView) mCommonCollageLayout.getChildAt(i);
			partCollage.setImageBitmap(mCheckedImagesBitmap.get(i));

		}

		mBackImageButton = (ImageButton) v.findViewById(R.id.back_image_button);
		mBackImageButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if (NavUtils.getParentActivityName(getActivity()) != null) {
					NavUtils.navigateUpFromSameTask(getActivity());
				}
			}
		});
		
		mPostButton = (Button) v.findViewById(R.id.post_button);
		mPostButton.setOnClickListener(new View.OnClickListener() {
			
			@TargetApi(Build.VERSION_CODES.HONEYCOMB)
			@Override
			public void onClick(View v) {
				Toast loadToast = Toast.makeText(getContext(), R.string.toast_load, Toast.LENGTH_SHORT);
				loadToast.show();
				
				mBitmapWorker = new BitmapCollageWorker(getContext(), BITMAP_NAME, BitmapCollageWorker.JPEG_FORMAT);	
				Bitmap collage = mBitmapWorker.createBitmap(mCommonCollageLayout);
				boolean saved = mBitmapWorker.saveBitmap(collage, 70);
				if (saved) {
					loadToast.cancel();
					String dialogTitle = getContext().getResources().getString(R.string.impicit_intent_title);
					startActivity(Intent.createChooser(mBitmapWorker.generateCollageIntent(), dialogTitle));
				}
			}
		});
		
		return v;
	}


	
}
