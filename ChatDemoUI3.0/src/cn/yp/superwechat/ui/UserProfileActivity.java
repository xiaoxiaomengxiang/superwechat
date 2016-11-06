package cn.yp.superwechat.ui;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.hyphenate.EMValueCallBack;
import com.hyphenate.easeui.domain.EaseUser;
import com.hyphenate.easeui.domain.User;
import com.hyphenate.easeui.utils.EaseUserUtils;

import java.io.ByteArrayOutputStream;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import cn.yp.superwechat.R;
import cn.yp.superwechat.SuperWeChatHelper;
import cn.yp.superwechat.bean.Result;
import cn.yp.superwechat.data.NetDao;
import cn.yp.superwechat.data.OkHttpUtils;
import cn.yp.superwechat.utils.CommonUtils;
import cn.yp.superwechat.utils.L;
import cn.yp.superwechat.utils.MFGT;
import cn.yp.superwechat.utils.ResultUtils;

public class UserProfileActivity extends BaseActivity implements View.OnClickListener {
	private static final String TAG = UserProfileActivity.class.getSimpleName();

	private static final int REQUESTCODE_PICK = 1;
	private static final int REQUESTCODE_CUTTING = 2;
	@BindView(R.id.img_back)
	ImageView mImgBack;
	@BindView(R.id.txt_title)
	TextView mTxtTitle;
	@BindView(R.id.iv_userinfo_avatar)
	ImageView mIvUserinfoAvatar;
	@BindView(R.id.tv_userinfo_nick)
	TextView mTvUserinfoNick;
	@BindView(R.id.tv_userinfo_name)
	TextView mTvUserinfoName;
	private ProgressDialog dialog;
	private RelativeLayout rlNickName;

	User user = null;

	@Override
	protected void onCreate(Bundle arg0) {
		super.onCreate(arg0);
		setContentView(R.layout.em_activity_user_profile);
		ButterKnife.bind(this);
		initView();
		initListener();
		user = EaseUserUtils.getCurrentAppUserInfo();
	}

	@Override
	public void onCheckedChange(int checkedPosition, boolean byUser) {

	}

	private void initView() {
		mImgBack.setVisibility(View.VISIBLE);
		mTxtTitle.setVisibility(View.VISIBLE);
		mTxtTitle.setText(getString(R.string.title_user_profile));
	}

	private void initListener() {
		EaseUserUtils.setCurentAppUserAvatar(this,mIvUserinfoAvatar);
		EaseUserUtils.setCurentAppUserNick(mTvUserinfoNick);
		EaseUserUtils.setCurrentAppUserName(mTvUserinfoName);
	}

	public void asyncFetchUserInfo(String username) {
		SuperWeChatHelper.getInstance().getUserProfileManager().asyncGetUserInfo(username, new EMValueCallBack<EaseUser>() {

			@Override
			public void onSuccess(EaseUser user) {
				if (user != null) {
					SuperWeChatHelper.getInstance().saveContact(user);
					if (isFinishing()) {
						return;
					}
					mTvUserinfoNick.setText(user.getNick());
					if (!TextUtils.isEmpty(user.getAvatar())) {
						Glide.with(UserProfileActivity.this).load(user.getAvatar()).placeholder(R.drawable.default_hd_avatar).into(mIvUserinfoAvatar);
					} else {
						Glide.with(UserProfileActivity.this).load(R.drawable.default_hd_avatar).into(mIvUserinfoAvatar);
					}
				}
			}

			@Override
			public void onError(int error, String errorMsg) {
			}
		});
	}

//	Uri.Builder builder;
//	private void uploadHeadPhoto() {
////		Uri.Builder builder = new Uri.Builder(this);
////		builder.setTitle(R.string.dl_title_upload_photo);
//		builder.setItems(new String[]{getString(R.string.dl_msg_take_photo), getString(R.string.dl_msg_local_upload)},
//				new DialogInterface.OnClickListener() {
//
//					public void onClick(DialogInterface dialog, int which) {
//						dialog.dismiss();
//						switch (which) {
//							case 0:
//								Toast.makeText(UserProfileActivity.this, getString(R.string.toast_no_support),
//										Toast.LENGTH_SHORT).show();
//								break;
//							case 1:
//								Intent pickIntent = new Intent(Intent.ACTION_PICK, null);
//								pickIntent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
//								startActivityForResult(pickIntent, REQUESTCODE_PICK);
//								break;
//							default:
//								break;
//						}
//					}
//				});
////		builder.create().show();
//	}


	private void updateRemoteNick(final String nickName) {
		dialog = ProgressDialog.show(this, getString(R.string.dl_update_nick), getString(R.string.dl_waiting));
		new Thread(new Runnable() {

			@Override
			public void run() {
				boolean updatenick = SuperWeChatHelper.getInstance().getUserProfileManager().updateCurrentUserNickName(nickName);
				if (UserProfileActivity.this.isFinishing()) {
					return;
				}
				if (!updatenick) {
					runOnUiThread(new Runnable() {
						public void run() {
							Toast.makeText(UserProfileActivity.this, getString(R.string.toast_updatenick_fail), Toast.LENGTH_SHORT)
									.show();
							dialog.dismiss();
						}
					});
				} else {
					updateAppNick(nickName);
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							dialog.dismiss();
							Toast.makeText(UserProfileActivity.this, getString(R.string.toast_updatenick_success), Toast.LENGTH_SHORT)
									.show();
							mTvUserinfoNick.setText(nickName);
						}
					});
				}
			}
		}).start();
	}

	private void updateAppNick(String nickName) {
		NetDao.updateNick(this, user.getMUserName(), nickName, new OkHttpUtils.OnCompleteListener<String>() {
			@Override
			public void onSuccess(String s) {
				if(s!=null){
					Result result = ResultUtils.getResultFromJson(s, User.class);
					L.e(TAG,"result="+result);
					if(result!=null && result.isRetMsg()){
						User u = (User) result.getRetData();
						updateLocatUser(u);
					}else{
						Toast.makeText(UserProfileActivity.this, getString(R.string.toast_updatenick_fail), Toast.LENGTH_SHORT)
								.show();
						dialog.dismiss();
					}
				}else{
					Toast.makeText(UserProfileActivity.this, getString(R.string.toast_updatenick_fail), Toast.LENGTH_SHORT)
							.show();
					dialog.dismiss();
				}
			}

			@Override
			public void onError(String error) {
				L.e(TAG,"error="+error);
				Toast.makeText(UserProfileActivity.this, getString(R.string.toast_updatenick_fail), Toast.LENGTH_SHORT)
						.show();
				dialog.dismiss();
			}
		});
	}

	private void updateLocatUser(User u) {
		user = u;
		SuperWeChatHelper.getInstance().saveAppContact(u);
		EaseUserUtils.setCurentAppUserNick(mTvUserinfoNick);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
			case REQUESTCODE_PICK:
				if (data == null || data.getData() == null) {
					return;
				}
				startPhotoZoom(data.getData());
				break;
			case REQUESTCODE_CUTTING:
				if (data != null) {
					setPicToView(data);
				}
				break;
			default:
				break;
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	public void startPhotoZoom(Uri uri) {
		Intent intent = new Intent("com.android.camera.action.CROP");
		intent.setDataAndType(uri, "image/*");
		intent.putExtra("crop", true);
		intent.putExtra("aspectX", 1);
		intent.putExtra("aspectY", 1);
		intent.putExtra("outputX", 300);
		intent.putExtra("outputY", 300);
		intent.putExtra("return-data", true);
		intent.putExtra("noFaceDetection", true);
		startActivityForResult(intent, REQUESTCODE_CUTTING);
	}

	/**
	 * save the picture data
	 *
	 * @param picdata
	 */
	private void setPicToView(Intent picdata) {
		Bundle extras = picdata.getExtras();
		if (extras != null) {
			Bitmap photo = extras.getParcelable("data");
			Drawable drawable = new BitmapDrawable(getResources(), photo);
			mIvUserinfoAvatar.setImageDrawable(drawable);
			uploadUserAvatar(Bitmap2Bytes(photo));
		}

	}

	private void uploadUserAvatar(final byte[] data) {
		dialog = ProgressDialog.show(this, getString(R.string.dl_update_photo), getString(R.string.dl_waiting));
		new Thread(new Runnable() {

			@Override
			public void run() {
				final String avatarUrl = SuperWeChatHelper.getInstance().getUserProfileManager().uploadUserAvatar(data);
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						dialog.dismiss();
						if (avatarUrl != null) {
							Toast.makeText(UserProfileActivity.this, getString(R.string.toast_updatephoto_success),
									Toast.LENGTH_SHORT).show();
						} else {
							Toast.makeText(UserProfileActivity.this, getString(R.string.toast_updatephoto_fail),
									Toast.LENGTH_SHORT).show();
						}

					}
				});

			}
		}).start();

		dialog.show();
	}


	public byte[] Bitmap2Bytes(Bitmap bm) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		bm.compress(Bitmap.CompressFormat.PNG, 100, baos);
		return baos.toByteArray();
	}

	@OnClick({R.id.img_back, R.id.layout_userinfo_avatar, R.id.layout_userinfo_nick, R.id.layout_userinfo_name})
	public void onClick(View view) {
		switch (view.getId()) {
			case R.id.img_back:
				MFGT.finish(this);
				break;
			case R.id.layout_userinfo_avatar:
//				uploadHeadPhoto();
				break;
			case R.id.layout_userinfo_nick:
				final EditText editText = new EditText(this);
				editText.setText(user.getMUserNick());
//				new Uri.Builder(this).setTitle(R.string.setting_nickname).setIcon(android.R.drawable.ic_dialog_info).setView(editText)
//						.setPositiveButton(R.string.dl_ok, new DialogInterface.OnClickListener() {

//							@Override
//							public void onClick(DialogInterface dialog, int which) {
//								String nickString = editText.getText().toString().trim();
//								if (TextUtils.isEmpty(nickString)) {
//									Toast.makeText(UserProfileActivity.this, getString(R.string.toast_nick_not_isnull), Toast.LENGTH_SHORT).show();
//									return;
//								}
//								if(nickString.equals(user.getMUserNick())){
//									CommonUtils.showShortToast(getString(R.string.toast_nick_not_modify));
//									return;
//								}
//								updateRemoteNick(nickString);
//							}
//						}).setNegativeButton(R.string.dl_cancel, null).show();
				break;
			case R.id.layout_userinfo_name:
				CommonUtils.showShortToast(R.string.User_name_cannot_be_modify);
				break;
		}
	}
}
//
//	private static final int REQUESTCODE_PICK = 1;
//	private static final int REQUESTCODE_CUTTING = 2;
//	@BindView(R.id.img_back)
//	ImageView mImgBack;
//	@BindView(R.id.txt_title)
//	TextView mTxtTitle;
//	@BindView(R.id.iv_userinfo_avatar)
//	ImageView mIvUserinfoAvatar;
//	@BindView(R.id.tv_userinfo_nick)
//	TextView mTvUserinfoNick;
//	@BindView(R.id.tv_userinfo_name)
//	TextView mTvUserinfoName;
//	private ProgressDialog dialog;
//	private RelativeLayout rlNickName;
//	/**
//	 * ATTENTION: This was auto-generated to implement the App Indexing API.
//	 * See https://g.co/AppIndexing/AndroidStudio for more information.
//	 */
//	private GoogleApiClient client;
//
//
//	@Override
//	protected void onCreate(Bundle arg0) {
//		super.onCreate(arg0);
//		setContentView(R.layout.em_activity_user_profile);
//		ButterKnife.bind(this);
//		initView();
//		initListener();
//		// ATTENTION: This was auto-generated to implement the App Indexing API.
//		// See https://g.co/AppIndexing/AndroidStudio for more information.
//		client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
//	}
//
//	@Override
//	public void onCheckedChange(int checkedPosition, boolean byUser) {
//
//	}
//
//	private void initView() {
//		mImgBack.setVisibility(View.VISIBLE);
//		mTxtTitle.setVisibility(View.VISIBLE);
//		mTxtTitle.setText(getString(R.string.title_user_profile));
//	}
//
//	private void initListener() {
//		EaseUserUtils.setCurentAppUserAvatar(this, mIvUserinfoAvatar);
//		EaseUserUtils.setCurentAppUserNick(mTvUserinfoNick);
//		EaseUserUtils.setCurrentAppUserName(mTvUserinfoName);
//	}
//
//	public void asyncFetchUserInfo(String username) {
//		SuperWeChatHelper.getInstance().getUserProfileManager().asyncGetUserInfo(username, new EMValueCallBack<EaseUser>() {
//
//			@Override
//			public void onSuccess(EaseUser user) {
//				if (user != null) {
//					SuperWeChatHelper.getInstance().saveContact(user);
//					if (isFinishing()) {
//						return;
//					}
//					mTvUserinfoNick.setText(user.getNick());
//					if (!TextUtils.isEmpty(user.getAvatar())) {
//						Glide.with(UserProfileActivity.this).load(user.getAvatar()).placeholder(R.drawable.default_hd_avatar).into(mIvUserinfoAvatar);
//					} else {
//						Glide.with(UserProfileActivity.this).load(R.drawable.default_hd_avatar).into(mIvUserinfoAvatar);
//					}
//				}
//			}
//
//			@Override
//			public void onError(int error, String errorMsg) {
//			}
//		});
//	}
//
//
//	private void uploadHeadPhoto() {
//		Builder builder = new Builder(this);
//		builder.setTitle(R.string.dl_title_upload_photo);
//		builder.setItems(new String[]{getString(R.string.dl_msg_take_photo), getString(R.string.dl_msg_local_upload)},
//				new DialogInterface.OnClickListener() {
//
//					public void onClick(DialogInterface dialog, int which) {
//						dialog.dismiss();
//						switch (which) {
//							case 0:
//								Toast.makeText(UserProfileActivity.this, getString(R.string.toast_no_support),
//										Toast.LENGTH_SHORT).show();
//								break;
//							case 1:
//								Intent pickIntent = new Intent(Intent.ACTION_PICK, null);
//								pickIntent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
//								startActivityForResult(pickIntent, REQUESTCODE_PICK);
//								break;
//							default:
//								break;
//						}
//					}
//				});
//		builder.create().show();
//	}
//
//
//	private void updateRemoteNick(final String nickName) {
//		dialog = ProgressDialog.show(this, getString(R.string.dl_update_nick), getString(R.string.dl_waiting));
//		new Thread(new Runnable() {
//
//			@Override
//			public void run() {
//				boolean updatenick = SuperWeChatHelper.getInstance().getUserProfileManager().updateCurrentUserNickName(nickName);
//				if (UserProfileActivity.this.isFinishing()) {
//					return;
//				}
//				if (!updatenick) {
//					runOnUiThread(new Runnable() {
//						public void run() {
//							Toast.makeText(UserProfileActivity.this, getString(R.string.toast_updatenick_fail), Toast.LENGTH_SHORT)
//									.show();
//							dialog.dismiss();
//						}
//					});
//				} else {
//					runOnUiThread(new Runnable() {
//						@Override
//						public void run() {
//							dialog.dismiss();
//							Toast.makeText(UserProfileActivity.this, getString(R.string.toast_updatenick_success), Toast.LENGTH_SHORT)
//									.show();
//							mTvUserinfoNick.setText(nickName);
//						}
//					});
//				}
//			}
//		}).start();
//	}
//
//	@Override
//	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//		switch (requestCode) {
//			case REQUESTCODE_PICK:
//				if (data == null || data.getData() == null) {
//					return;
//				}
//				startPhotoZoom(data.getData());
//				break;
//			case REQUESTCODE_CUTTING:
//				if (data != null) {
//					setPicToView(data);
//				}
//				break;
//			default:
//				break;
//		}
//		super.onActivityResult(requestCode, resultCode, data);
//	}
//
//	public void startPhotoZoom(Uri uri) {
//		Intent intent = new Intent("com.android.camera.action.CROP");
//		intent.setDataAndType(uri, "image/*");
//		intent.putExtra("crop", true);
//		intent.putExtra("aspectX", 1);
//		intent.putExtra("aspectY", 1);
//		intent.putExtra("outputX", 300);
//		intent.putExtra("outputY", 300);
//		intent.putExtra("return-data", true);
//		intent.putExtra("noFaceDetection", true);
//		startActivityForResult(intent, REQUESTCODE_CUTTING);
//	}
//
//	/**
//	 * save the picture data
//	 *
//	 * @param picdata
//	 */
//	private void setPicToView(Intent picdata) {
//		Bundle extras = picdata.getExtras();
//		if (extras != null) {
//			Bitmap photo = extras.getParcelable("data");
//			Drawable drawable = new BitmapDrawable(getResources(), photo);
//			mIvUserinfoAvatar.setImageDrawable(drawable);
//			uploadUserAvatar(Bitmap2Bytes(photo));
//		}
//
//	}
//
//	private void uploadUserAvatar(final byte[] data) {
//		dialog = ProgressDialog.show(this, getString(R.string.dl_update_photo), getString(R.string.dl_waiting));
//		new Thread(new Runnable() {
//
//			@Override
//			public void run() {
//				final String avatarUrl = SuperWeChatHelper.getInstance().getUserProfileManager().uploadUserAvatar(data);
//				runOnUiThread(new Runnable() {
//					@Override
//					public void run() {
//						dialog.dismiss();
//						if (avatarUrl != null) {
//							Toast.makeText(UserProfileActivity.this, getString(R.string.toast_updatephoto_success),
//									Toast.LENGTH_SHORT).show();
//						} else {
//							Toast.makeText(UserProfileActivity.this, getString(R.string.toast_updatephoto_fail),
//									Toast.LENGTH_SHORT).show();
//						}
//
//					}
//				});
//
//			}
//		}).start();
//
//		dialog.show();
//	}
//
//
//	public byte[] Bitmap2Bytes(Bitmap bm) {
//		ByteArrayOutputStream baos = new ByteArrayOutputStream();
//		bm.compress(Bitmap.CompressFormat.PNG, 100, baos);
//		return baos.toByteArray();
//	}
//
//	@OnClick({R.id.img_back, R.id.layout_userinfo_avatar, R.id.layout_userinfo_nick, R.id.layout_userinfo_name})
//	public void onClick(View view) {
//		switch (view.getId()) {
//			case R.id.img_back:
//				MFGT.finish(this);
//				break;
//			case R.id.layout_userinfo_avatar:
//				uploadHeadPhoto();
//				break;
//			case R.id.layout_userinfo_nick:
//				final EditText editText = new EditText(this);
//				new Builder(this).setTitle(R.string.setting_nickname).setIcon(android.R.drawable.ic_dialog_info).setView(editText)
//						.setPositiveButton(R.string.dl_ok, new DialogInterface.OnClickListener() {
//
//							@Override
//							public void onClick(DialogInterface dialog, int which) {
//								String nickString = editText.getText().toString();
//								if (TextUtils.isEmpty(nickString)) {
//									Toast.makeText(UserProfileActivity.this, getString(R.string.toast_nick_not_isnull), Toast.LENGTH_SHORT).show();
//									return;
//								}
//								updateRemoteNick(nickString);
//							}
//						}).setNegativeButton(R.string.dl_cancel, null).show();
//				break;
//			case R.id.layout_userinfo_name:
//				CommonUtils.showShortToast(R.string.User_name_cannot_be_modify);
//				break;
//		}
//	}
//
//	/**
//	 * ATTENTION: This was auto-generated to implement the App Indexing API.
//	 * See https://g.co/AppIndexing/AndroidStudio for more information.
//	 */
//	public Action getIndexApiAction() {
//		Thing object = new Thing.Builder()
//				.setName("UserProfile Page") // TODO: Define a title for the content shown.
//				// TODO: Make sure this auto-generated URL is correct.
//				.setUrl(Uri.parse("http://[ENTER-YOUR-URL-HERE]"))
//				.build();
//		return new Action.Builder(Action.TYPE_VIEW)
//				.setObject(object)
//				.setActionStatus(Action.STATUS_TYPE_COMPLETED)
//				.build();
//	}
//
//	@Override
//	public void onStart() {
//		super.onStart();
//
//		// ATTENTION: This was auto-generated to implement the App Indexing API.
//		// See https://g.co/AppIndexing/AndroidStudio for more information.
//		client.connect();
//		AppIndex.AppIndexApi.start(client, getIndexApiAction());
//	}
//
//	@Override
//	public void onStop() {
//		super.onStop();
//
//		// ATTENTION: This was auto-generated to implement the App Indexing API.
//		// See https://g.co/AppIndexing/AndroidStudio for more information.
//		AppIndex.AppIndexApi.end(client, getIndexApiAction());
//		client.disconnect();
//	}
//}
////public class UserProfileActivity extends BaseActivity implements OnClickListener{
////
////	private static final int REQUESTCODE_PICK = 1;
////	private static final int REQUESTCODE_CUTTING = 2;
////	private ImageView headAvatar;
////	private ImageView headPhotoUpdate;
////	private ImageView iconRightArrow;
////	private TextView tvNickName;
////	private TextView tvUsername;
////	private ProgressDialog dialog;
////	private RelativeLayout rlNickName;
////
////
////
////	@Override
////	protected void onCreate(Bundle arg0) {
////		super.onCreate(arg0);
////		setContentView(R.layout.em_activity_user_profile);
////		initView();
////		initListener();
////	}
////
////	private void initView() {
////		headAvatar = (ImageView) findViewById(R.id.user_head_avatar);
////		headPhotoUpdate = (ImageView) findViewById(R.id.user_head_headphoto_update);
////		tvUsername = (TextView) findViewById(R.id.user_username);
////		tvNickName = (TextView) findViewById(R.id.user_nickname);
////		rlNickName = (RelativeLayout) findViewById(R.id.rl_nickname);
////		iconRightArrow = (ImageView) findViewById(R.id.ic_right_arrow);
////	}
////
////	private void initListener() {
////		Intent intent = getIntent();
////		String username = intent.getStringExtra("username");
////		boolean enableUpdate = intent.getBooleanExtra("setting", false);
////		if (enableUpdate) {
////			headPhotoUpdate.setVisibility(View.VISIBLE);
////			iconRightArrow.setVisibility(View.VISIBLE);
////			rlNickName.setOnClickListener(this);
////			headAvatar.setOnClickListener(this);
////		} else {
////			headPhotoUpdate.setVisibility(View.GONE);
////			iconRightArrow.setVisibility(View.INVISIBLE);
////		}
////		if(username != null){
////    		if (username.equals(EMClient.getInstance().getCurrentUser())) {
////    			tvUsername.setText(EMClient.getInstance().getCurrentUser());
////    			EaseUserUtils.setUserNick(username, tvNickName);
////                EaseUserUtils.setUserAvatar(this, username, headAvatar);
////    		} else {
////    			tvUsername.setText(username);
////    			EaseUserUtils.setUserNick(username, tvNickName);
////    			EaseUserUtils.setUserAvatar(this, username, headAvatar);
////    			asyncFetchUserInfo(username);
////    		}
////		}
////	}
////
////	@Override
////	public void onClick(View v) {
////		switch (v.getId()) {
////		case R.id.user_head_avatar:
////			uploadHeadPhoto();
////			break;
////		case R.id.rl_nickname:
////			final EditText editText = new EditText(this);
////			new AlertDialog.Builder(this).setTitle(R.string.setting_nickname).setIcon(android.R.drawable.ic_dialog_info).setView(editText)
////					.setPositiveButton(R.string.dl_ok, new DialogInterface.OnClickListener() {
////
////						@Override
////						public void onClick(DialogInterface dialog, int which) {
////							String nickString = editText.getText().toString();
////							if (TextUtils.isEmpty(nickString)) {
////								Toast.makeText(UserProfileActivity.this, getString(R.string.toast_nick_not_isnull), Toast.LENGTH_SHORT).show();
////								return;
////							}
////							updateRemoteNick(nickString);
////						}
////					}).setNegativeButton(R.string.dl_cancel, null).show();
////			break;
////		default:
////			break;
////		}
////
////	}
////
////	public void asyncFetchUserInfo(String username){
////		SuperWeChatHelper.getInstance().getUserProfileManager().asyncGetUserInfo(username, new EMValueCallBack<EaseUser>() {
////
////			@Override
////			public void onSuccess(EaseUser user) {
////				if (user != null) {
////				    SuperWeChatHelper.getInstance().saveContact(user);
////				    if(isFinishing()){
////				        return;
////				    }
////					tvNickName.setText(user.getNick());
////					if(!TextUtils.isEmpty(user.getAvatar())){
////						 Glide.with(UserProfileActivity.this).load(user.getAvatar()).placeholder(R.drawable.em_default_avatar).into(headAvatar);
////					}else{
////					    Glide.with(UserProfileActivity.this).load(R.drawable.em_default_avatar).into(headAvatar);
////					}
////				}
////			}
////
////			@Override
////			public void onError(int error, String errorMsg) {
////			}
////		});
////	}
////
////
////
////	private void uploadHeadPhoto() {
////		AlertDialog.Builder builder = new Builder(this);
////		builder.setTitle(R.string.dl_title_upload_photo);
////		builder.setItems(new String[] { getString(R.string.dl_msg_take_photo), getString(R.string.dl_msg_local_upload) },
////				new DialogInterface.OnClickListener() {
////
////					public void onClick(DialogInterface dialog, int which) {
////						dialog.dismiss();
////						switch (which) {
////						case 0:
////							Toast.makeText(UserProfileActivity.this, getString(R.string.toast_no_support),
////									Toast.LENGTH_SHORT).show();
////							break;
////						case 1:
////							Intent pickIntent = new Intent(Intent.ACTION_PICK,null);
////							pickIntent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
////							startActivityForResult(pickIntent, REQUESTCODE_PICK);
////							break;
////						default:
////							break;
////						}
////					}
////				});
////		builder.create().show();
////	}
////
////
////
////	private void updateRemoteNick(final String nickName) {
////		dialog = ProgressDialog.show(this, getString(R.string.dl_update_nick), getString(R.string.dl_waiting));
////		new Thread(new Runnable() {
////
////			@Override
////			public void run() {
////				boolean updatenick = SuperWeChatHelper.getInstance().getUserProfileManager().updateCurrentUserNickName(nickName);
////				if (UserProfileActivity.this.isFinishing()) {
////					return;
////				}
////				if (!updatenick) {
////					runOnUiThread(new Runnable() {
////						public void run() {
////							Toast.makeText(UserProfileActivity.this, getString(R.string.toast_updatenick_fail), Toast.LENGTH_SHORT)
////									.show();
////							dialog.dismiss();
////						}
////					});
////				} else {
////					runOnUiThread(new Runnable() {
////						@Override
////						public void run() {
////							dialog.dismiss();
////							Toast.makeText(UserProfileActivity.this, getString(R.string.toast_updatenick_success), Toast.LENGTH_SHORT)
////									.show();
////							tvNickName.setText(nickName);
////						}
////					});
////				}
////			}
////		}).start();
////	}
////
////	@Override
////	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
////		switch (requestCode) {
////		case REQUESTCODE_PICK:
////			if (data == null || data.getData() == null) {
////				return;
////			}
////			startPhotoZoom(data.getData());
////			break;
////		case REQUESTCODE_CUTTING:
////			if (data != null) {
////				setPicToView(data);
////			}
////			break;
////		default:
////			break;
////		}
////		super.onActivityResult(requestCode, resultCode, data);
////	}
////
////	public void startPhotoZoom(Uri uri) {
////		Intent intent = new Intent("com.android.camera.action.CROP");
////		intent.setDataAndType(uri, "image/*");
////		intent.putExtra("crop", true);
////		intent.putExtra("aspectX", 1);
////		intent.putExtra("aspectY", 1);
////		intent.putExtra("outputX", 300);
////		intent.putExtra("outputY", 300);
////		intent.putExtra("return-data", true);
////		intent.putExtra("noFaceDetection", true);
////		startActivityForResult(intent, REQUESTCODE_CUTTING);
////	}
////
////	/**
////	 * save the picture data
////	 *
////	 * @param picdata
////	 */
////	private void setPicToView(Intent picdata) {
////		Bundle extras = picdata.getExtras();
////		if (extras != null) {
////			Bitmap photo = extras.getParcelable("data");
////			Drawable drawable = new BitmapDrawable(getResources(), photo);
////			headAvatar.setImageDrawable(drawable);
////			uploadUserAvatar(Bitmap2Bytes(photo));
////		}
////
////	}
////
////	private void uploadUserAvatar(final byte[] data) {
////		dialog = ProgressDialog.show(this, getString(R.string.dl_update_photo), getString(R.string.dl_waiting));
////		new Thread(new Runnable() {
////
////			@Override
////			public void run() {
////				final String avatarUrl = SuperWeChatHelper.getInstance().getUserProfileManager().uploadUserAvatar(data);
////				runOnUiThread(new Runnable() {
////					@Override
////					public void run() {
////						dialog.dismiss();
////						if (avatarUrl != null) {
////							Toast.makeText(UserProfileActivity.this, getString(R.string.toast_updatephoto_success),
////									Toast.LENGTH_SHORT).show();
////						} else {
////							Toast.makeText(UserProfileActivity.this, getString(R.string.toast_updatephoto_fail),
////									Toast.LENGTH_SHORT).show();
////						}
////
////					}
////				});
////
////			}
////		}).start();
////
////		dialog.show();
////	}
////
////
////	public byte[] Bitmap2Bytes(Bitmap bm){
////		ByteArrayOutputStream baos = new ByteArrayOutputStream();
////		bm.compress(Bitmap.CompressFormat.PNG, 100, baos);
////		return baos.toByteArray();
////	}
////}
