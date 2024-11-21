package lx;

import cn.hutool.core.io.IORuntimeException;
import cn.hutool.http.ContentType;
import cn.hutool.http.HttpException;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import lx.model.Zdm;
import lx.utils.StreamUtils;
import lx.utils.Utils;
import org.apache.commons.lang3.StringUtils;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static lx.utils.Const.WXPUSHER_URL;
import static lx.utils.Const.ZDM_URL;

public class ZdmCrawler {

    public static void main(String[] args) {
        String emailHost = System.getenv("emailHost"), emailAccount = System.getenv("emailAccount"),
                emailPassword = System.getenv("emailPassword"), spt = System.getenv("spt");
        int minVoted = Integer.parseInt(System.getenv("minVoted")), minComments = Integer.parseInt(System.getenv("minComments")),
                minPushSize = Integer.parseInt(System.getenv("MIN_PUSH_SIZE"));

        //git actions部署的服务器一般在海外,调整为东八区的时区
        TimeZone.setDefault(TimeZone.getTimeZone("GMT+8"));

        Set<Zdm> zdms = ZDM_URL.stream().flatMap(url -> {
                    List<Zdm> zdmPage = new ArrayList<>();
                    for (int i = 1; i <= 20; i++) {//爬取前20页数据
                        try {
                            String s = HttpUtil.get(url + i, 10000);
                            List<Zdm> zdmPart = JSONObject.parseArray(s, Zdm.class);
                            zdmPart.forEach(zdm -> {
                                //评论和点值数量的值后面会跟着'k','w'这种字符,将它们转换一下方便后面过滤和排序
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

        //已推送的优惠信息
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

        //黑词过滤
        HashSet<String> blackWords = Utils.readFile("./black_words.txt");
        blackWords.removeIf(StringUtils::isBlank);

        //白词过滤内容
        HashSet<String> whiteWords = Utils.readFile("./white_words.txt");
        whiteWords.removeIf(StringUtils::isBlank);

        if (whiteWords.isEmpty()) {
            //如果白词文件为空，则使用原来的黑词模式
            System.out.println("whiteWords is empty, running in blackWords mode.");
            zdms = new HashSet<>(StreamUtils.filter(zdms, z ->
                    StringUtils.isBlank(StreamUtils.findFirst(blackWords, w -> z.getTitle().contains(w))) //黑词过滤
                            && Integer.parseInt(z.getVoted()) > minVoted //值的数量
                            && Integer.parseInt(z.getComments()) > minComments //评论的数量
                            && !z.getPrice().contains("前") //不是前xxx名的耍猴抢购
                            && !pushedIds.contains(z.getArticleId()) //不是已经推送过的
            ));
        } else {
            //如果白词文件不为空，则启用新的白词模式，仅发送包含白名单中的商品优惠信息
            System.out.println("whiteWords is not empty, running in whiteWords mode.");
            for (String word : whiteWords) {
                System.out.println(word);
            }
            zdms = new HashSet<>(StreamUtils.filter(zdms, z ->
                    !StringUtils.isBlank(StreamUtils.findFirst(whiteWords, w -> z.getTitle().contains(w))) //白词过滤
                            && Integer.parseInt(z.getVoted()) > minVoted //值的数量
                            && Integer.parseInt(z.getComments()) > minComments //评论的数量
                            && !z.getPrice().contains("前") //不是前xxx名的耍猴抢购
                            && !pushedIds.contains(z.getArticleId()) //不是已经推送过的
            ));
        }

        zdms.forEach(z -> System.out.println(z.getArticleId() + " | " + z.getTitle()));

        //优惠信息单次推送的数量阈值,未达到阈值则暂存推送信息等待下次运行
        if (zdms.size() < minPushSize) {
            //记录暂未推送的优惠信息
            Utils.write("./unpushed.txt", false, StreamUtils.map(zdms, JSONObject::toJSONString));
            return;
        }

        //部分推送方式存在内容长度限制, 这里加了单次推送的条数限制, 超出则分批推送
        Lists.partition(new ArrayList<>(zdms), 100).forEach(part -> {
            //生成推送消息的正文内容(html格式)
            String text = Utils.buildMessage(new ArrayList<>(part));
            //通过邮箱推送
            pushToEmail(text, emailHost, emailAccount, emailPassword);
            //通过WxPusher推送
            pushToWx(text, spt);
            //记录已推送的优惠信息
            List<String> articleIds = StreamUtils.map(part, Zdm::getArticleId);
            Utils.write("./logs/" + LocalDate.now() + "/pushed.txt", true, articleIds);
        });
    }

    private static void pushToEmail(String text, String emailHost, String emailAccount, String emailPassword) {
        if (StringUtils.isBlank(emailHost) || StringUtils.isBlank(emailAccount) || StringUtils.isBlank(emailPassword)) {
            System.out.println("邮箱推送配置不完整,将尝试其他推送方式");
            return;
        }

        Properties props = new Properties();
        props.setProperty("mail.smtp.host", emailHost);
        props.setProperty("mail.smtp.port", "465");
        props.setProperty("mail.smtp.auth", "true");
        props.setProperty("mail.smtp.ssl.enable", "true");
        try {
            Session session = Session.getDefaultInstance(props, new Authenticator() {
                @Override
                public PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(emailAccount, emailPassword);
                }
            });

            MimeMessage message = new MimeMessage(session);
            message.setFrom(emailAccount);
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(emailAccount));
            message.setSubject("zdm优惠信息汇总" + DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").format(LocalDateTime.now()));
            message.setContent(text, "text/html;charset=UTF-8");
            Transport.send(message);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("邮件发送失败");
        }
    }

    private static void pushToWx(String text, String spt) {
        if (StringUtils.isBlank(spt)) {
            System.out.println("WxPusher推送配置不完整,将尝试其他推送方式");
            return;
        }
        HashMap<String, Object> body = new HashMap<>();
        //推送内容
        body.put("content", text);
        //消息摘要，显示在微信聊天页面或者模版消息卡片上，限制长度20(微信只能显示20)，可以不传，不传默认截取content前面的内容。
        body.put("summary", "zdm优惠信息汇总");
        //内容类型 1表示文字  2表示html 3表示markdown
        body.put("contentType", "2");
        body.put("spt", System.getenv("spt"));
        String response = HttpUtil.createPost(WXPUSHER_URL)
                .contentType(ContentType.JSON.getValue())
                .body(JSONObject.toJSONString(body))
                .execute().body();
        System.out.println("WxPusher response:" + response);
        JSONObject jsonObject = (JSONObject) JSONObject.parse(response);
        //状态码,非1000表示有异常
        String code = jsonObject.getString("code");
        if (!"1000".equals(code))
            throw new RuntimeException("WxPusher推送失败:" + jsonObject.getString("msg"));
    }
}
