package lx.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

import lx.model.Crawlable;

public class Utils {

    public static String buildMessage(List<? extends Crawlable> zdms, String th) {
        zdms.sort(Comparator.comparingInt(Crawlable::obtainSortOrder).reversed());

        StringBuilder s = new StringBuilder();
        s.append("<table border=\"1\">");
        s.append("<tr><th>图</th><th>标题</th><th>价格</th><th>赞/评</th></tr>");
        zdms.forEach(z -> s.append(z.toHtmlTr()));
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
    public static void write(String path, List<String> list) {
        BufferedWriter bw = null;
        FileWriter fr = null;
        try {
            //将写入转化为流的形式
            fr = new FileWriter(path);
            bw = new BufferedWriter(fr);
            //一次写一行
            for (String s : list) {
                bw.write(s);
                bw.newLine();  //换行用
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (bw != null) {
                    bw.close();
                }
                if (fr != null) {
                    fr.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    //清空txt文件
    public static void clearFile(String path) {
        File file = new File(path);
        try {
            if (!file.exists()) {
                file.createNewFile();
            }
            FileWriter fileWriter = new FileWriter(file);
            fileWriter.write("");  //写入空
            fileWriter.flush();
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
