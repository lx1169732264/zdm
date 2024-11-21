package lx.utils;

import com.google.common.collect.Lists;

import java.util.List;

public interface Const {

    List<String> ZDM_URL = Lists.newArrayList("https://faxian.smzdm.com/json_more?filter=h2s0t0f0c3&page=",
            "https://faxian.smzdm.com/json_more?filter=h3s0t0f0c3&page=");

    //WxPusher极简推送模式的链接
    String WXPUSHER_URL = "https://wxpusher.zjiecode.com/api/send/message/simple-push";

}
