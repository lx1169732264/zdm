package lx;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TimeZone;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.commons.lang3.StringUtils;

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;

import cn.hutool.core.io.IORuntimeException;
import cn.hutool.http.ContentType;
import cn.hutool.http.HttpException;
import cn.hutool.http.HttpUtil;
import lx.mapper.ZdmMapper;
import lx.model.Zdm;
import lx.utils.StreamUtils;
import lx.utils.Utils;

import static lx.utils.Const.WXPUSHER_URL;
import static lx.utils.Const.ZDM_URL;

public class ZdmCrawler {

    public static void main(String[] args) {
        //突然发现定环境变量名的时候一下子大写下划线,一下子小写驼峰. 考虑到之前已经有在用的用户了, 暂时不做修改了
        Map<String, String> envMap = System.getenv();
        String emailHost = System.getenv("emailHost"), emailAccount = System.getenv("emailAccount"),
                emailPassword = System.getenv("emailPassword"), emailPort = envMap.getOrDefault("emailPort", "465"),
                spt = System.getenv("spt");
        int maxPageSize = Integer.parseInt(envMap.getOrDefault("maxPageSize", "20")),
                minVoted = Integer.parseInt(envMap.getOrDefault("minVoted", "0")),
                minComments = Integer.parseInt(envMap.getOrDefault("minComments", "0")),
                minPushSize = Integer.parseInt(envMap.getOrDefault("MIN_PUSH_SIZE", "0"));
        boolean detail = "true".equals(envMap.getOrDefault("detail", "false"));

        //获取待推送的优惠信息
        Collection<Zdm> zdms = obtainUnpushedArticles(maxPageSize);

        //根据各项规则执行过滤逻辑
        zdms = processFilter(zdms, minVoted, minComments, detail);

        //在推送之前先入库数据,pushed字段默认为0(未推送)
        ZdmMapper.saveOrUpdateBatch(zdms);

        //未达到推送的数量阈值,则等待下次运行
        if (zdms.size() < minPushSize)
            return;

        //部分推送方式存在内容长度限制, 这里加了单次推送的条数限制, 超出则分批推送
        Lists.partition(new ArrayList<>(zdms), 100).forEach(part -> {
            //生成推送消息的正文内容(html格式)
            String text = Utils.buildMessage(part);
            //通过邮箱推送
            boolean pushToEmail = pushToEmail(text, emailHost, emailPort, emailAccount, emailPassword);
            //通过WxPusher推送
            boolean pushToWx = pushToWx(text, spt);
            if (!pushToEmail && !pushToWx)
                throw new RuntimeException("未匹配到推送方式,请检查配置");

            //推送完成后,pushed字段置为已推送
            part.forEach(o -> o.setPushed(true));
            ZdmMapper.saveOrUpdateBatch(part);
        });
    }

    private static Collection<Zdm> obtainUnpushedArticles(int maxPageSize) {
        //GitHub Actions部署的服务器一般在海外,调整为东八区的时区
        ZoneId zoneId = ZoneId.of("GMT+8");
        TimeZone.setDefault(TimeZone.getTimeZone(zoneId));

        //上次执行后未推送的优惠信息
        List<Zdm> unPush = ZdmMapper.unPush();

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();

        //从网页上获取的优惠信息
        Stream<Zdm> crawled = ZDM_URL.stream().flatMap(url -> {
            List<Zdm> zdmPage = new ArrayList<>();
            for (int i = 1; i <= maxPageSize; i++) {
                try {
                    /**
                     * 2025-05-08 什么值得买的这个接口似乎加了反爬虫机制,偶尔会返回一段js的验证码,导致JSONObject解析json时报错
                     * 分别尝试了cn.hutool.http.HttpUtil 和 java.net.http.HttpRequest两个接口调用工具,发现HttpUtil会出现上述问题,可能是这两种调用方式生成的请求头有所不同导致的?
                     * 总之不太清楚是触发了什么反爬虫的规则.有懂哥可以帮忙看看
                     */
                    HttpRequest httpRequest = HttpRequest.newBuilder().GET().uri(URI.create(url + i)).build();
                    String s = client.send(httpRequest, HttpResponse.BodyHandlers.ofString()).body();

                    List<Zdm> zdmPart = JSONObject.parseArray(s, Zdm.class);
                    zdmPart.forEach(zdm -> {
                        //评论和点值数量的值后面会跟着'k','w'这种字符,将它们转换一下方便后面过滤和排序
                        zdm.setComments(Utils.strNumberFormat(zdm.getComments()));
                        zdm.setVoted(Utils.strNumberFormat(zdm.getVoted()));

                        //转化为毫秒级时间戳
                        String timestampStr = zdm.getTimesort() + "000";
                        zdm.setArticle_time(Instant.ofEpochMilli(Long.parseLong(timestampStr))
                                .atZone(zoneId)
                                .toLocalDateTime().toString());
                    });
                    zdmPage.addAll(zdmPart);
                } catch (IORuntimeException | HttpException | IOException | InterruptedException e) {
                    //暂时的网络不通,会导致连接超时的异常,等待下次运行即可
                    System.out.println("pageNumber:" + i + ", connect to zdm server timeout:" + e.getMessage());
                }
            }
            return zdmPage.stream();
        });

        return Stream.concat(crawled, unPush.stream())  //两个stream合并,一起参与排序和去重操作
                .sorted(Comparator.comparing(Zdm::getComments, Comparator.comparingInt(Integer::parseInt)).reversed())    //评论数量倒序,用LinkedHashSet保证有序
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static List<Zdm> processFilter(Collection<Zdm> zdms, int minVoted, int minComments, boolean detail) {
        //黑词过滤
        HashSet<String> blackWords = Utils.readFile("./black_words.txt");
        blackWords.removeIf(StringUtils::isBlank);

        //白词过滤内容
        HashSet<String> whiteWords = Utils.readFile("./white_words.txt");
        whiteWords.removeIf(StringUtils::isBlank);

        if (whiteWords.isEmpty()) {
            //如果白词文件为空，则使用原来的黑词模式
            if (detail)
                System.out.println("whiteWords is empty, running in blackWords mode. blackWords list:\n" + String.join(",", blackWords));
            zdms = StreamUtils.filter(zdms, z -> StringUtils.isBlank(StreamUtils.findFirst(blackWords, w -> z.getTitle().contains(w))));
        } else {
            //如果白词文件不为空，则启用新的白词模式，仅发送包含白名单中的商品优惠信息
            if (detail)
                System.out.println("whiteWords is not empty, running in whiteWords mode. whiteWords list:\n" + String.join(",", whiteWords));
            zdms = StreamUtils.filter(zdms, z -> !StringUtils.isBlank(StreamUtils.findFirst(whiteWords, w -> z.getTitle().contains(w))));
        }

        //从数据库中取出已推送的优惠信息id
        Collection<String> pushedIds = ZdmMapper.pushedIds();

        //执行其他过滤规则
        List<Zdm> filtered = StreamUtils.filter(zdms, z ->
                Integer.parseInt(z.getVoted()) > minVoted //值的数量
                        && Integer.parseInt(z.getComments()) > minComments //评论的数量
                        && !z.getPrice().contains("前") //不是前xxx名的耍猴抢购
                        && !pushedIds.contains(z.getArticleId()) //不是已经推送过的
        );

        filtered.forEach(o -> o.setPushed(false));
        if (detail) {
            System.out.println("待推送的优惠信息:");
            filtered.forEach(z -> System.out.println(z.getArticleId() + " | " + z.getTitle()));
        }
        return filtered;
    }

    private static boolean pushToEmail(String text, String emailHost, String emailPort, String emailAccount, String emailPassword) {
        if (StringUtils.isBlank(emailHost) || StringUtils.isBlank(emailPort)
                || StringUtils.isBlank(emailAccount) || StringUtils.isBlank(emailPassword)) {
            System.out.println("邮箱推送配置不完整,将尝试其他推送方式");
            return false;
        }

        Properties props = new Properties();
        props.setProperty("mail.smtp.host", emailHost);
        props.setProperty("mail.smtp.port", emailPort);
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
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("邮件发送失败");
        }
    }

    private static boolean pushToWx(String text, String spt) {
        if (StringUtils.isBlank(spt)) {
            System.out.println("WxPusher推送配置不完整,将尝试其他推送方式");
            return false;
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
        return true;
    }
}
