package cn.yp.superwechat.utils;

import android.widget.Toast;

import cn.yp.superwechat.I;
import cn.yp.superwechat.R;
import cn.yp.superwechat.SuperWeChatApplication;

/**
 * Created by Winston on 2016/11/1.
 */

public class CommonUtils {
    public static void showLongToast(String msg){
        Toast.makeText(SuperWeChatApplication.applicationContext,msg,Toast.LENGTH_LONG).show();
    }
    public static void showShortToast(String msg){
        Toast.makeText(SuperWeChatApplication.applicationContext,msg,Toast.LENGTH_SHORT).show();
    }
    public static void showLongToast(int rId){
        showLongToast(SuperWeChatApplication.applicationContext.getString(rId));
    }
    public static void showShortToast(int rId){
        showShortToast(SuperWeChatApplication.applicationContext.getString(rId));
    }

    public static void showMsgShortToast(int msgId){
        if(msgId>0) {
            showShortToast(SuperWeChatApplication.getInstance().getResources()
                    .getIdentifier(I.MSG_PREFIX_MSG+msgId,"string",
                            SuperWeChatApplication.getInstance().getPackageName()));
        }else{
            showShortToast(R.string.msg_1);
        }
    }
}
