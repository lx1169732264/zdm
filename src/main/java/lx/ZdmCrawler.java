package lx;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.commons.lang3.StringUtils;

import com.alibaba.fastjson.JSONObject;

import cn.hutool.core.io.IORuntimeException;
import cn.hutool.http.HttpException;
import cn.hutool.http.HttpUtil;
import lx.model.Zdm;
import lx.utils.StreamUtils;
import lx.utils.Utils;

import static lx.utils.Const.ZDM_URL;

public class ZdmCrawler {

    public static void main(String[] args) {
        Set<Zdm> zdms = ZDM_URL.stream().flatMap(url -> {
                    List<Zdm> zdmPage = new ArrayList<>();
                    for (int i = 1; i <= 300; i++) {//爬取前20页数据
                        try {
                            String s = HttpUtil.get(url + i, 10000);
                            List<Zdm> zdmPart = JSONObject.parseArray(s, Zdm.class);
                            zdmPart.forEach(zdm -> {
                                //将评论和点值数量的值后面会跟着'k','w'这种字符,将它们转换一下方便后面过滤和排序
                                zdm.setComments(Utils.strNumberFormat(zdm.getComments()));
                                zdm.setVoted(Utils.strNumberFormat(zdm.getVoted()));
                            });
                            zdmPage.addAll(zdmPart);
                        } catch (IORuntimeException | HttpException e) {
                            //暂时的网络不通,会导致连接超时的异常,等待下次运行即可
                            System.out.println("pageNumber:" + i + ", connect to zdm server timeout:" + e.getMessage());
                        }
                    }
                    return zdmPage.stream();
                }).sorted(Comparator.comparing(Zdm::getComments).reversed())    //评论数量倒序,用LinkedHashSet保证有序
                .collect(Collectors.toCollection(LinkedHashSet::new));//ZDM_URL这里是按多个时间段纬度的排行榜进行爬取的,会存在相同优惠信息被重复爬取的情况,Zdm类重写了equals(),利用Set去重

        //unpushed.txt记录了上次执行后,未推送的优惠信息
        HashSet<String> unPushed = Utils.readFile("./unpushed.txt");
        zdms.addAll(StreamUtils.map(unPushed, o -> JSONObject.parseObject(o, Zdm.class)));
System.out.println("zdms:"+zdms.size());
        //黑词过滤
        HashSet<String> blackWords = Utils.readFile("./black_words.txt");
        blackWords.removeIf(StringUtils::isBlank);
        //白詞
        HashSet<String> whiteWords = Utils.readFile("./white_words.txt");
        whiteWords.removeIf(StringUtils::isBlank);

        //已推送的优惠信息id
        Set<String> pushedIds;
        try {
            new File("./logs/").mkdirs();
            pushedIds = Files.walk(Paths.get("./logs/"), 2)
                    .filter(p -> !Files.isDirectory(p))
                    .filter(p -> LocalDate.parse(p.getParent().getFileName().toString()).isAfter(LocalDate.now().minusMonths(1))) //统计一个月内的数据,这也意味着相同的优惠信息,如果一个月后再次登上排行榜,则会被重复推送.不过这种场景比较少,在排行榜上的一般是比较新的内容
                    .map(Path::toFile)
                    .flatMap(f -> Utils.readFile(f.getPath()).stream()).collect(Collectors.toSet());
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("读取logs目录失败");
        }

        zdms = new HashSet<>(StreamUtils.filter(zdms, z ->
                StringUtils.isBlank(StreamUtils.findFirst(blackWords, w -> z.getTitle().contains(w))) //黑词过滤
                        && Integer.parseInt(z.getVoted()) > getEnvValue("minVoted",2) //值的数量
                        && Integer.parseInt(z.getComments()) > getEnvValue("minComments",1) //评论的数量
                        && !z.getPrice().contains("前") //不是前xxx名的耍猴抢购
                        && !pushedIds.contains(z.getArticleId()) //不是已经推送过的
                        && isTextConvertibleToDoubleAndLessThan50(z.getPrice().split("元")[0].trim())
                &&StringUtils.isNotBlank(StreamUtils.findFirst(whiteWords, w -> (z.getTitle().contains(w.split(",")[0].trim())
                        &&Double.parseDouble(z.getPrice().split("元")[0].trim()) <= Double.parseDouble(w.split(",")[1].trim()))))
        ));
        zdms.forEach(z -> System.out.println(z.getArticleId() + " | " + z.getTitle()));
        System.out.println("zdms:"+zdms.size());
        if (zdms.size() > getEnvValue("MIN_PUSH_SIZE",1)) {
            sendEmail(Utils.buildMessage(new ArrayList<>(zdms)));
            Utils.write("./logs/" + LocalDate.now() + "/pushed.txt", true, StreamUtils.map(zdms, Zdm::getArticleId));
        } else {
            Utils.write("./unpushed.txt", false, StreamUtils.map(zdms, JSONObject::toJSONString));
        }
    }
    private static boolean isTextConvertibleToDoubleAndLessThan50(String text) {
        try {
            double doubleValue = Double.parseDouble(text);
            return doubleValue < 50;
        } catch (NumberFormatException e) {
            // 处理转换异常，例如日志记录或其他逻辑
            System.err.println("Error converting text to double: " + e.getMessage());
            return false; // 或者根据需求进行其他处理
        }
    }
    private static int getEnvValue(String envValue, int defaultValue){
        if(System.getenv(envValue)==null){
            return defaultValue;
        }
        return Integer.parseInt(System.getenv(envValue));
    }
    public static void sendEmail(String text) {
        Properties props = new Properties();
        props.setProperty("mail.smtp.host", System.getenv("emailHost"));
        props.setProperty("mail.smtp.auth", "true");
        try {
            Session session = Session.getDefaultInstance(props, new Authenticator() {
                @Override
                public PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(System.getenv("emailAccount"), System.getenv("emailPassword"));
                }
            });

            MimeMessage message = new MimeMessage(session);
            message.setFrom(System.getenv("emailAccount"));
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(System.getenv("emailAccount")));
            message.setSubject(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").format(LocalDateTime.now()));
            message.setContent(text, "text/html;charset=UTF-8");
            Transport.send(message);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("邮件发送失败");
        }
    }

}
