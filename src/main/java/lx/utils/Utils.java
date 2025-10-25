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
        s.append("<table border='1'>");
        s.append("<tr><th width='20%'>图</th><th width='45%'>标题</th><th width='15%'>价格</th><th width='10%'>赞/评</th><th width='10%'>平台</th></tr>");
        list.forEach(z -> s.append(z.toHtmlTr()));
        s.append("</table>");
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
