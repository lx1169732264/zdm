package lx.utils;

import lx.model.Crawlable;

import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.lang3.RandomUtils;

import static lx.utils.Const.USER_AGENTS;

public class Utils {

    public static String buildMessage(List<? extends Crawlable> list) {
        StringBuilder s = new StringBuilder();
        s.append("<!DOCTYPE html><html lang='zh-CN'><head><meta charset='UTF-8'><meta name='viewport' content='width=device-width, initial-scale=1.0'>");
        s.append("<style>");
        s.append("body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif; background-color: #f7f8fa; margin: 0; padding: 10px; color: #333; }");
        s.append("table { width: 100%; max-width: 1100px; margin: 0 auto; border-collapse: collapse; background: #fff; box-shadow: 0 1px 3px rgba(0,0,0,0.05); border-radius: 8px; overflow: hidden; }");
        s.append("th { background-color: #f8f9fa; color: #666; font-weight: 600; padding: 12px 15px; text-align: left; font-size: 14px; border-bottom: 1px solid #eee; }");
        s.append("td { padding: 12px 15px; border-bottom: 1px solid #f0f0f0; vertical-align: middle; font-size: 14px; }");
        s.append("img { display: block; width: 110px; height: 110px; object-fit: cover; border-radius: 4px; border: 1px solid #eee; }");
        s.append("a { color: #333; text-decoration: none; font-weight: 500; line-height: 1.4; display: block; }");
        s.append("a:hover { color: #e4393c; }");
        s.append("td:nth-child(3) { color: #e4393c; font-weight: bold; font-family: Arial, sans-serif; }"); // 价格
        s.append("td:nth-child(4) { color: #999; font-size: 13px; }"); // 赞/评
        s.append("td:nth-child(5) { color: #666; font-size: 13px; }"); // 平台
        s.append("@media (max-width: 600px) {");
        s.append("  thead { display: none; }");
        s.append("  tr { display: block; margin-bottom: 12px; background: #fff; border-radius: 8px; padding: 12px; position: relative; box-shadow: 0 2px 6px rgba(0,0,0,0.05); border: 1px solid #eee; }");
        s.append("  td { display: block; border: none; padding: 2px 0; text-align: left; }");
        s.append("  td:nth-child(1) { position: absolute; left: 12px; top: 12px; width: 90px; padding: 0; }");
        s.append("  td:nth-child(1) img { width: 90px; height: 90px; }");
        s.append("  td:nth-child(2), td:nth-child(3), td:nth-child(4), td:nth-child(5) { margin-left: 105px; min-height: 20px; }");
        s.append("  td:nth-child(2) { margin-bottom: 6px; }");
        s.append("  td:nth-child(2) a { font-size: 15px; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }");
        s.append("  td:nth-child(3) { color: #ff4400; font-size: 16px; margin-bottom: 6px; }");
        s.append("  td:nth-child(4) { display: inline-block; width: auto; margin-right: 10px; font-size: 12px; color: #999; }");
        s.append("  td:nth-child(5) { display: inline-block; width: auto; margin-left: 0; font-size: 12px; background: #f5f5f5; padding: 2px 6px; border-radius: 3px; color: #666; }");
        s.append("}");
        s.append("</style></head><body>");
        s.append("<table>");
        s.append("<thead><tr><th width='15%'>图</th><th width='40%'>标题</th><th width='15%'>价格</th><th width='15%'>赞/评</th><th width='15%'>平台</th></tr></thead>");
        s.append("<tbody>");
        list.forEach(z -> s.append(z.toHtmlTr()));
        s.append("</tbody>");
        s.append("</table>");
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
