
package cn.yp.superwechat.ui;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.hyphenate.EMCallBack;
import com.hyphenate.chat.EMClient;
import com.hyphenate.easeui.utils.EaseCommonUtils;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import cn.yp.superwechat.R;
import cn.yp.superwechat.SuperWeChatApplication;
import cn.yp.superwechat.SuperWeChatHelper;
import cn.yp.superwechat.data.NetDao;
import cn.yp.superwechat.data.OkHttpUtils;
import cn.yp.superwechat.db.SuperWeChatDBManager;
import cn.yp.superwechat.utils.L;
import cn.yp.superwechat.utils.MD5;
import cn.yp.superwechat.utils.MFGT;

/**
 * Login screen
 * 
 */
public class LoginActivity extends BaseActivity {
	private static final String TAG = "LoginActivity";
	public static final int REQUEST_CODE_SETNICK = 1;
	@BindView(R.id.img_back)
	ImageView mImgBack;
	@BindView(R.id.txt_title)
	TextView mTxtTitle;
	@BindView(R.id.et_username)
	EditText mEtUsername;
	@BindView(R.id.et_password)
	EditText mEtPassword;

	private boolean progressShow;
	private boolean autoLogin = false;

	String currentUsername;
	String currentPassword;
	ProgressDialog pd;
	LoginActivity mContext;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// enter the main activity if already logged in
		if (SuperWeChatHelper.getInstance().isLoggedIn()) {
			autoLogin = true;
			startActivity(new Intent(LoginActivity.this, MainActivity.class));

			return;
		}
		setContentView(R.layout.em_activity_login);
		ButterKnife.bind(this);

		setListener();
		initView();
		mContext = this;

	}

	private void initView() {
		if (SuperWeChatHelper.getInstance().getCurrentUsernName() != null) {
			mEtUsername.setText(SuperWeChatHelper.getInstance().getCurrentUsernName());
		}
		mImgBack.setVisibility(View.VISIBLE);
		mTxtTitle.setVisibility(View.VISIBLE);
		mTxtTitle.setText(R.string.login);
	}

	private void setListener() {
		// if user changed, clear the password
		mEtUsername.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				mEtPassword.setText(null);
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {

			}

			@Override
			public void afterTextChanged(Editable s) {

			}
		});
	}

	/**
	 * login
	 */
	public void login() {
		if (!EaseCommonUtils.isNetWorkConnected(this)) {
			Toast.makeText(this, R.string.network_isnot_available, Toast.LENGTH_SHORT).show();
			return;
		}
		currentUsername = mEtUsername.getText().toString().trim();
		currentPassword = mEtPassword.getText().toString().trim();

		if (TextUtils.isEmpty(currentUsername)) {
			Toast.makeText(this, R.string.User_name_cannot_be_empty, Toast.LENGTH_SHORT).show();
			return;
		}
		if (TextUtils.isEmpty(currentPassword)) {
			Toast.makeText(this, R.string.Password_cannot_be_empty, Toast.LENGTH_SHORT).show();
			return;
		}

		progressShow = true;
		pd = new ProgressDialog(LoginActivity.this);
		pd.setCanceledOnTouchOutside(false);
		pd.setOnCancelListener(new OnCancelListener() {

			@Override
			public void onCancel(DialogInterface dialog) {
				Log.d(TAG, "EMClient.getInstance().onCancel");
				progressShow = false;
			}
		});
		pd.setMessage(getString(R.string.Is_landing));
		pd.show();

		loginEMServer();
	}

	private void loginEMServer() {
		// After logout，the DemoDB may still be accessed due to async callback, so the DemoDB will be re-opened again.
		// close it before login to make sure DemoDB not overlap
		SuperWeChatDBManager.getInstance().closeDB();

		// reset current user name before login
		SuperWeChatHelper.getInstance().setCurrentUserName(currentUsername);

		final long start = System.currentTimeMillis();
		// call login method
		Log.d(TAG, "EMClient.getInstance().login");
		EMClient.getInstance().login(currentUsername, MD5.getMessageDigest(currentPassword), new EMCallBack() {

			@Override
			public void onSuccess() {
				Log.d(TAG, "login: onSuccess");

				loginAppServer();
			}

			@Override
			public void onProgress(int progress, String status) {
				Log.d(TAG, "login: onProgress");
			}

			@Override
			public void onError(final int code, final String message) {
				Log.d(TAG, "login: onError: " + code);
				if (!progressShow) {
					return;
				}
				runOnUiThread(new Runnable() {
					public void run() {
						pd.dismiss();
						Toast.makeText(getApplicationContext(), getString(R.string.Login_failed) + message,
								Toast.LENGTH_SHORT).show();
					}
				});
			}
		});
	}

	private void loginAppServer() {
		NetDao.login(mContext, currentUsername, currentPassword, new OkHttpUtils.OnCompleteListener<String>() {
			@Override
			public void onSuccess(String s) {
				L.e(TAG,"s="+s);

				loginSuccess();
			}

			@Override
			public void onError(String error) {
				pd.dismiss();
				L.e(TAG,"onError="+error);
			}
		});
	}

	private void loginSuccess() {
		// ** manually load all local groups and conversation
		EMClient.getInstance().groupManager().loadAllGroups();
		EMClient.getInstance().chatManager().loadAllConversations();

		// update current user's display name for APNs
		boolean updatenick = EMClient.getInstance().updateCurrentUserNick(
				SuperWeChatApplication.currentUserNick.trim());
		if (!updatenick) {
			Log.e("LoginActivity", "update current user nick fail");
		}

		if (!LoginActivity.this.isFinishing() && pd.isShowing()) {
			pd.dismiss();
		}
		// get user's info (this should be get from App's server or 3rd party service)
		SuperWeChatHelper.getInstance().getUserProfileManager().asyncGetCurrentUserInfo();

		Intent intent = new Intent(LoginActivity.this,
				MainActivity.class);
		startActivity(intent);

		finish();
	}


	@Override
	protected void onResume() {
		super.onResume();
		if (autoLogin) {
			return;
		}
	}

	@OnClick({R.id.lr_title, R.id.button2, R.id.btn_register})
	public void onClick(View view) {
		switch (view.getId()) {
			case R.id.lr_title:
				MFGT.finish(this);
				break;
			case R.id.button2:
				login();
				break;
			case R.id.btn_register:
				MFGT.gotoRegister(this);
				break;
		}
	}
}