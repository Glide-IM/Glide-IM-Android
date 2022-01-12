package pro.glideim.sdk;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.functions.Function;
import pro.glideim.sdk.api.Response;
import pro.glideim.sdk.api.auth.AuthApi;
import pro.glideim.sdk.api.auth.AuthBean;
import pro.glideim.sdk.api.auth.LoginDto;
import pro.glideim.sdk.api.auth.RegisterDto;
import pro.glideim.sdk.api.group.CreateGroupBean;
import pro.glideim.sdk.api.group.CreateGroupDto;
import pro.glideim.sdk.api.group.GetGroupInfoDto;
import pro.glideim.sdk.api.group.GroupApi;
import pro.glideim.sdk.api.group.GroupInfoBean;
import pro.glideim.sdk.api.msg.AckOfflineMsgDto;
import pro.glideim.sdk.api.msg.MessageBean;
import pro.glideim.sdk.api.msg.MsgApi;
import pro.glideim.sdk.api.user.GetUserInfoDto;
import pro.glideim.sdk.api.user.UserApi;
import pro.glideim.sdk.api.user.UserInfoBean;
import pro.glideim.sdk.http.RetrofitManager;
import pro.glideim.sdk.utils.RxUtils;

public class GlideIM {

    public static final String TAG = "GlideIM";
    private static final int device = 1;
    private static GlideIM sInstance;
    public DataStorage dataStorage = new DefaultDataStoreImpl();
    private IMAccount account = new IMAccount(0);

    private GlideIM() {
    }

    public static GlideIM getInstance() {
        return sInstance;
    }

    public static void init(String baseUrlApi) {
        RetrofitManager.init(baseUrlApi);
        sInstance = new GlideIM();
    }

    public static Observable<String> authDefaultAccount() {
        long defaultUid = getDataStorage().getDefaultAccountUid();
        if (defaultUid == 0) {
            return Observable.error(new Exception("no default account"));
        }
        return authLoggedAccount(defaultUid);
    }

    public static Observable<String> authLoggedAccount(Long uid) {
        String token = getDataStorage().loadToken(uid);
        if (token.isEmpty()) {
            return Observable.error(new Exception("invalid token"));
        }
        IMAccount account = new IMAccount(uid);
        getInstance().account = account;
        return account.auth();
    }

    public static Observable<String> login(String account, String password, int device) {
        return AuthApi.API.login(new LoginDto(account, password, device))
                .map(RxUtils.bodyConverter())
                .flatMap((Function<AuthBean, ObservableSource<IMAccount>>) authBean -> {
                    getDataStorage().storeToken(authBean.getUid(), authBean.getToken());
                    IMAccount account1 = new IMAccount(authBean.getUid());
                    GlideIM.getInstance().account = account1;
                    return account1.initAccountAndConn().map(aBoolean -> account1);
                })
                .flatMap((Function<IMAccount, ObservableSource<String>>) IMAccount::auth);
    }

    public static Observable<Boolean> register(String account, String password) {
        return AuthApi.API.register(new RegisterDto(account, password))
                .map(RxUtils.bodyConverter())
                .map(o -> true);
    }

    public static Observable<List<MessageBean>> getOfflineMessage() {
        return MsgApi.API.getOfflineMsg()
                .map(RxUtils.bodyConverter())
                .doOnNext(messageBeans -> {
                    List<Long> mids = new ArrayList<>();
                    for (MessageBean b : messageBeans) {
                        mids.add(b.getMid());
                    }
                    MsgApi.API.ackOfflineMsg(new AckOfflineMsgDto(mids)).subscribe(new SilentObserver<>());
                });
    }

    public static Observable<GroupInfoBean> getGroupInfo(long gid) {
        GroupInfoBean groupInfoBean = getDataStorage().loadTempGroupInfo(gid);
        if (groupInfoBean != null) {
            return Observable.just(groupInfoBean);
        }
        return GroupApi.API.getGroupInfo(new GetGroupInfoDto(Collections.singletonList(gid)))
                .map(RxUtils.bodyConverter())
                .map(groupInfo -> {
                    GroupInfoBean g = groupInfo.get(0);
                    getDataStorage().storeTempGroupInfo(g);
                    return g;
                });
    }

    public static Observable<List<GroupInfoBean>> getGroupInfo(List<Long> gid) {

        final List<GroupInfoBean> temped = new ArrayList<>();
        List<Long> filtered = new ArrayList<>();
        for (Long id : gid) {
            GroupInfoBean temp = getDataStorage().loadTempGroupInfo(id);
            if (temp != null) {
                temped.add(temp);
            } else {
                filtered.add(id);
            }
        }
        if (filtered.isEmpty()) {
            return Observable.just(temped);
        }
        Observable<Response<List<GroupInfoBean>>> s = GroupApi.API.getGroupInfo(new GetGroupInfoDto(filtered));
        return s.map(RxUtils.bodyConverter()).map(r -> {
            for (GroupInfoBean u : r) {
                getDataStorage().storeTempGroupInfo(u);
            }
            temped.addAll(r);
            return r;
        });
    }

    public static Observable<UserInfoBean> getUserInfo(long uid) {
        UserInfoBean temp = getDataStorage().loadTempUserInfo(uid);
        if (temp != null) {
            return Observable.just(temp);
        }
        return UserApi.API.getUserInfo(new GetUserInfoDto(Collections.singletonList(uid)))
                .map(RxUtils.bodyConverter())
                .map(userInfoBeans -> {
                    UserInfoBean g = userInfoBeans.get(0);
                    getDataStorage().storeTempUserInfo(g);
                    return g;
                });
    }

    public static Observable<List<UserInfoBean>> getUserInfo(List<Long> uid) {
        final List<UserInfoBean> temped = new ArrayList<>();
        List<Long> filtered = new ArrayList<>();
        for (Long id : uid) {
            UserInfoBean temp = getDataStorage().loadTempUserInfo(id);
            if (temp != null) {
                temped.add(temp);
            } else {
                filtered.add(id);
            }
        }
        if (filtered.isEmpty()) {
            return Observable.just(temped);
        }
        Observable<Response<List<UserInfoBean>>> s = UserApi.API.getUserInfo(new GetUserInfoDto(filtered));
        return s.map(RxUtils.bodyConverter()).map(r -> {
            for (UserInfoBean u : r) {
                getDataStorage().storeTempUserInfo(u);
            }
            temped.addAll(r);
            return r;
        });
    }

    public static Observable<CreateGroupBean> createGroup(String name) {
        return GroupApi.API.createGroup(new CreateGroupDto(name))
                .map(RxUtils.bodyConverter());
    }

    public static IMAccount getAccount() {
        return getInstance().account;
    }

    public static DataStorage getDataStorage() {
        return getInstance().dataStorage;
    }

    public void setDataStorage(DataStorage dataStorage) {
        this.dataStorage = dataStorage;
    }

    public void setDevice(int device) {

    }
}
