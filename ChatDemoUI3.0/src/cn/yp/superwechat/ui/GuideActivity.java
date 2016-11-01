package cn.yp.superwechat.ui;

import android.os.Bundle;
import android.view.View;

import butterknife.ButterKnife;
import butterknife.OnClick;
import cn.yp.superwechat.R;
import cn.yp.superwechat.utils.MFGT;


public class GuideActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_guide);
        ButterKnife.bind(this);
        super.onCreate(savedInstanceState);
    }

    @OnClick({R.id.but_register, R.id.but_login})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.but_register:
                MFGT.gotoRegister(this);
                break;
            case R.id.but_login:
                MFGT.gotoLogin(this);
                break;
        }
    }
}
