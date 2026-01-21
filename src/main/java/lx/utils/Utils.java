package lx.utils;

import lx.model.Crawlable;

import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import com.alibaba.fastjson.JSON;
import org.apache.commons.lang3.RandomUtils;

import static lx.utils.Const.USER_AGENTS;

public class Utils {

    public static String buildMessage(List<? extends Crawlable> list) {
        List<Map<String, String>> items = new ArrayList<>();
        for (Crawlable item : list) {
            Map<String, String> view = new HashMap<>();
            view.put("title", item.getTitle());
            view.put("url", item.getUrl());
            view.put("picUrl", item.getPicUrl());
            view.put("price", item.getPrice());
            view.put("voted", item.getVoted());
            view.put("comments", item.getComments());
            view.put("articleMall", item.getArticleMall());
            items.add(view);
        }
        String itemsJson = JSON.toJSONString(items).replace("</", "<\\/");
        StringBuilder s = new StringBuilder();
        s.append("<!DOCTYPE html><html lang='zh-CN'><head><meta charset='UTF-8'><meta name='viewport' content='width=device-width, initial-scale=1.0'>");
        s.append("<link rel='stylesheet' href='https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2.15.0/cdn/themes/light.css'>");
        s.append("<style>");
        s.append("body { font-family: var(--sl-font-sans); background-color: var(--sl-color-neutral-50); margin: 0; padding: 12px; color: var(--sl-color-neutral-900); }");
        s.append("table { width: 100%; max-width: 1100px; margin: 0 auto; border-collapse: separate; border-spacing: 0; background: var(--sl-color-neutral-0); box-shadow: var(--sl-shadow-medium); border-radius: var(--sl-border-radius-large); overflow: hidden; }");
        s.append("th { background-color: var(--sl-color-neutral-100); color: var(--sl-color-neutral-600); font-weight: 600; padding: 12px 16px; text-align: left; font-size: 14px; border-bottom: 1px solid var(--sl-color-neutral-200); }");
        s.append("td { padding: 12px 16px; border-bottom: 1px solid var(--sl-color-neutral-200); vertical-align: middle; font-size: 14px; }");
        s.append("img { display: block; width: 110px; height: 110px; object-fit: cover; border-radius: var(--sl-border-radius-medium); border: 1px solid var(--sl-color-neutral-200); }");
        s.append("a { color: var(--sl-color-primary-700); text-decoration: none; font-weight: 600; line-height: 1.4; display: block; }");
        s.append("a:hover { color: var(--sl-color-primary-600); }");
        s.append("td:nth-child(3) { color: var(--sl-color-danger-600); font-weight: 700; font-family: var(--sl-font-sans); }");
        s.append("td:nth-child(4) { color: var(--sl-color-neutral-600); font-size: 13px; }");
        s.append("td:nth-child(5) { color: var(--sl-color-neutral-600); font-size: 13px; }");
        s.append("@media (max-width: 600px) {");
        s.append("  thead { display: none; }");
        s.append("  tr { display: block; margin-bottom: 12px; background: var(--sl-color-neutral-0); border-radius: var(--sl-border-radius-large); padding: 12px; position: relative; box-shadow: var(--sl-shadow-medium); border: 1px solid var(--sl-color-neutral-200); }");
        s.append("  td { display: block; border: none; padding: 2px 0; text-align: left; }");
        s.append("  td:nth-child(1) { position: absolute; left: 12px; top: 12px; width: 90px; padding: 0; }");
        s.append("  td:nth-child(1) img { width: 90px; height: 90px; }");
        s.append("  td:nth-child(2), td:nth-child(3), td:nth-child(4), td:nth-child(5) { margin-left: 105px; min-height: 20px; }");
        s.append("  td:nth-child(2) { margin-bottom: 6px; }");
        s.append("  td:nth-child(2) a { font-size: 15px; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }");
        s.append("  td:nth-child(3) { color: var(--sl-color-danger-600); font-size: 16px; margin-bottom: 6px; }");
        s.append("  td:nth-child(4) { display: inline-block; width: auto; margin-right: 10px; font-size: 12px; color: var(--sl-color-neutral-600); }");
        s.append("  td:nth-child(5) { display: inline-block; width: auto; margin-left: 0; font-size: 12px; background: var(--sl-color-neutral-100); padding: 2px 6px; border-radius: var(--sl-border-radius-small); color: var(--sl-color-neutral-700); }");
        s.append("}");
        s.append("</style></head><body class='sl-theme-light'>");
        s.append("<div id='app' style='display:none'>");
        s.append("<table>");
        s.append("<thead><tr><th width='15%'>图</th><th width='40%'>标题</th><th width='15%'>价格</th><th width='15%'>赞/评</th><th width='15%'>平台</th></tr></thead>");
        s.append("<tbody>");
        s.append("<tr v-for='item in items' :key='item.url'>");
        s.append("<td><img :src='item.picUrl'/></td>");
        s.append("<td><a target='_blank' :href='item.url'>{{ item.title }}</a></td>");
        s.append("<td>{{ item.price }}</td>");
        s.append("<td>{{ item.voted }}/{{ item.comments }}</td>");
        s.append("<td>{{ item.articleMall }}</td>");
        s.append("</tr>");
        s.append("</tbody>");
        s.append("</table>");
        s.append("</div>");
        s.append("<div id='fallback'>");
        s.append("<table>");
        s.append("<thead><tr><th width='15%'>图</th><th width='40%'>标题</th><th width='15%'>价格</th><th width='15%'>赞/评</th><th width='15%'>平台</th></tr></thead>");
        s.append("<tbody>");
        list.forEach(z -> s.append(z.toHtmlTr()));
        s.append("</tbody>");
        s.append("</table>");
        s.append("</div>");
        s.append("<script src='https://unpkg.com/vue@3/dist/vue.global.prod.js'></script>");
        s.append("<script>const { createApp } = Vue;createApp({ data() { return { items: " + itemsJson + " }; } }).mount('#app');const fallback=document.getElementById('fallback');if(fallback){fallback.style.display='none';}const appEl=document.getElementById('app');if(appEl){appEl.style.display='block';}</script>");
        s.append("</body></html>");
        return s.toString();
    }

    public static HashSet<String> readFile(String path) {
        HashSet<String> res = new HashSet<>();
        try (FileInputStream in = new FileInputStream(path);
             InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8);
             BufferedReader bufferedReader = new BufferedReader(reader)) {
            String lineTxt;
            while ((lineTxt = bufferedReader.readLine()) != null) {
                res.add(lineTxt);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new IllegalArgumentException(path + "文件读取失败");
        }
        return res;
    }

    /**
     * 按行写入
     *
     * @param path
     * @param list
     */
    public static void write(String path, boolean append, List<String> list) {
        File file = new File(path);
        file.getParentFile().mkdirs();
        try (FileWriter fr = new FileWriter(file, append);
             BufferedWriter bw = new BufferedWriter(fr)) {
            for (String s : list) {
                bw.write(s);
                bw.newLine();
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new IllegalArgumentException(path + "文件写入失败");
        }
    }

    public static String strNumberFormat(String number) {
        if (number.endsWith("k")) {
            String v = number.substring(0, number.length() - 2);
            return new BigDecimal(v).multiply(new BigDecimal(1000)).toString();
        }
        if (number.endsWith("w")) {
            String v = number.substring(0, number.length() - 2);
            return new BigDecimal(v).multiply(new BigDecimal(10000)).toString();
        }
        return number;
    }

    public static String ramdomUserAgent() {
        return USER_AGENTS.get(RandomUtils.nextInt(0, USER_AGENTS.size()));
    }
}
