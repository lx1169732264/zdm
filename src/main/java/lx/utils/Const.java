package lx.utils;

import java.util.List;

import com.google.common.collect.Lists;

public interface Const {

    //什么值得买排行榜数据接口地址
    List<String> ZDM_URL = Lists.newArrayList("https://faxian.smzdm.com/json_more?filter=h2s0t0f0c3&page=",
            "https://faxian.smzdm.com/json_more?filter=h3s0t0f0c3&page=");

    //WxPusher极简推送模式的链接
    String WXPUSHER_URL = "https://wxpusher.zjiecode.com/api/send/message/simple-push";

    //接口调用最大重试次数
    Integer MAX_RETRY = 3;

    List<String> USER_AGENTS = Lists.newArrayList(
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.4 Safari/605.1.15",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 17_4 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.4 Mobile/15E148 Safari/604.1",
            "Mozilla/5.0 (Linux; Android 14; SM-S918B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Mobile Safari/537.36",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:126.0) Gecko/20100101 Firefox/126.0"
    );
}
