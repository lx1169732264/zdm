package lx.utils;

import java.util.List;

import com.google.common.collect.Lists;

public class Const {
    public static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/108.0.0.0 Safari/537.36";

    public static final String ZDM_TH = "<tr><th>标题</th><th>价格</th><th>赞/评</th></tr>";
    public static final List<String> ZDM_URL = Lists.newArrayList("https://faxian.smzdm.com/json_more?filter=h2s0t0f0c5&page=",
            "https://faxian.smzdm.com/json_more?filter=h3s0t0f0c5&page=");

}
