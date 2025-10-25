package lx;

import java.net.HttpCookie;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TimeZone;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.io.IORuntimeException;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.http.ContentType;
import cn.hutool.http.Header;
import cn.hutool.http.HttpException;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpUtil;
import lx.mapper.ZdmMapper;
import lx.model.Zdm;
import lx.utils.StreamUtils;
import lx.utils.Utils;

import static lx.utils.Const.*;

public class ZdmCrawler {

    private static Collection<HttpCookie> cookies = new ArrayList<>();
    private static Date expiredDate = null;
    private static WebDriver driver = null;

    public static void main(String[] args) {
        //突然发现定环境变量名的时候一下子大写下划线,一下子小写驼峰. 考虑到之前已经有在用的用户了, 暂时不做修改
        Map<String, String> envMap = System.getenv();
        String emailHost = System.getenv("emailHost"), emailAccount = System.getenv("emailAccount"),
                emailPassword = System.getenv("emailPassword"), emailPort = envMap.getOrDefault("emailPort", "465"),
                spt = System.getenv("spt");
        int maxPageSize = Integer.parseInt(envMap.getOrDefault("maxPageSize", "10")),
                minVoted = Integer.parseInt(envMap.getOrDefault("minVoted", "0")),
                minComments = Integer.parseInt(envMap.getOrDefault("minComments", "0")),
                minPushSize = Integer.parseInt(envMap.getOrDefault("MIN_PUSH_SIZE", "0"));
        boolean detail = "true".equals(envMap.getOrDefault("detail", "false"));

        //获取待推送的优惠信息
        Collection<Zdm> zdms = obtainUnpushedArticles(maxPageSize);

        //根据各项规则执行过滤逻辑
        zdms = processFilter(zdms, minVoted, minComments, detail);
        System.out.println("过滤后剩余数据条数" + zdms.size());

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

        //从网页上获取的优惠信息
        Stream<Zdm> crawled = ZDM_URL.stream().flatMap(url -> {
            List<Zdm> zdmPage = new ArrayList<>();
            int interval = 1000;
            for (int i = 1; i <= maxPageSize; i++) {
                List<Zdm> zdmPart = processCrawl(url + i, MAX_RETRY);
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

                System.out.println("第" + i + "页数据获取成功, 当前页数据条数" + zdmPart.size());
                //翻页的间隔时间(毫秒),页数越多间隔时间越长,其实太靠后的信息基本上是不值的,建议maxPageSize配小一点
                ThreadUtil.sleep(ThreadLocalRandom.current().nextInt(interval, interval + 1000));
                interval += i * 50;
            }
            return zdmPage.stream();
        });

        return Stream.concat(crawled, unPush.stream())  //两个stream合并,一起参与排序和去重操作
                .sorted(Comparator.comparing(Zdm::getComments, Comparator.comparingInt(Integer::parseInt)).reversed())    //评论数量倒序,用LinkedHashSet保证有序
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static List<Zdm> processCrawl(String url, int retry) {
        /**
         * 什么值得买的cookie在未登录的状态下是由'__ckguid','x-waf-captcha-referer','w_tsfp'三段组成的
         * __ckguid是响应头的set-cookie里取下来的, x-waf-captcha-referer固定为空, w_tsfp是靠访问probe.js动态生成
         * 这里从selenium模拟浏览器行为自动获取cookie
         */
        try {
            HttpRequest request = HttpUtil.createGet(url)
                    .cookie(buildCookies())
                    .header(Header.USER_AGENT, Utils.ramdomUserAgent())
                    .header(Header.REFERER, url)
                    .header(Header.ACCEPT, "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7")
                    .header(Header.ACCEPT_LANGUAGE, "zh-CN,zh;q=0.9,en;q=0.8")
                    .header("X-Requested-With", "XMLHttpRequest")
                    .header(Header.CONNECTION, "keep-alive");

            String s = request.execute().body();
            return JSONObject.parseArray(s, Zdm.class);
        } catch (IORuntimeException | HttpException | JSONException | TimeoutException |
                 org.openqa.selenium.TimeoutException e) {
            //尝试重新获取cookie并重试接口, 重试次数耗尽则结束任务
            if (retry > 0) {
                //一般是接口调用太频繁跳验证码, 或者什么值得买的服务器重启宕机 等问题会引发重试, 重试的间隔时间会以分钟起步进行赋值
                int minutes = (MAX_RETRY - retry + 1);
                System.out.println("接口调用失败,等待" + minutes + "分钟后进行重试,剩余重试次数:" + retry);
                ThreadUtil.sleep((long) minutes * 60 * 1000);
                clearCookie();
                return processCrawl(url, retry - 1);
            }
            e.printStackTrace();
            throw new RuntimeException("接口调用失败,程序终止");
        }
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

    private static Collection<HttpCookie> buildCookies() throws TimeoutException {
        if (driver == null) {
            /**
             * GitActions运行时, 已通过工作流配置好了ChromeDriver的路径
             * 非GitActions运行时, 需要增加这段代码 System.setProperty("webdriver.chrome.driver", "xxxxxxx\\chromedriver.exe");
             */
            ChromeOptions options = new ChromeOptions();
            options.addArguments("--headless");          // 无头模式
            options.addArguments("--disable-gpu");      // 禁用 GPU 加速（Linux 必备）
            options.addArguments("--no-sandbox");       // 禁用沙盒（CI 环境必备）
            options.addArguments("--disable-dev-shm-usage"); // 避免 /dev/shm 不足
            options.addArguments("--disable-blink-features=AutomationControlled");
            options.addArguments("--disable-blink-features");
            options.addArguments("--disable-extensions");
            options.addArguments("--disable-plugins");
            options.addArguments("--disable-images");
            options.addArguments("--user-agent=" + Utils.ramdomUserAgent());
            // 排除自动化收集
            options.setExperimentalOption("excludeSwitches", new String[]{"enable-automation", "load-extension"});
            options.setExperimentalOption("useAutomationExtension", false);
            options.setPageLoadTimeout(Duration.ofSeconds(60));
            driver = new ChromeDriver(options);
            // 移除webdriver属性
            ((JavascriptExecutor) driver).executeScript(
                    "Object.defineProperty(navigator, 'webdriver', {get: () => undefined})");
        }

        //判断cookie即将过期时进行重新获取
        Date date = Date.from(Instant.now().minusSeconds(30));
        if (expiredDate != null && expiredDate.before(date)) {
            clearCookie();
            return buildCookies();
        }

        if (!CollectionUtil.isEmpty(cookies))
            return cookies;

        driver.get(ZDM_URL.get(0) + "1");
        //WebDriver#get方法是异步的, 这里需要等待页面加载完毕
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        wait.until(ExpectedConditions.jsReturnsValue("return document.readyState === 'complete'"));

        cookies = new ArrayList<>();
        cookies.add(new HttpCookie("x-waf-captcha-referer", ""));
        Cookie ckguid = driver.manage().getCookieNamed("__ckguid");
        if (ckguid != null)
            cookies.add(new HttpCookie("__ckguid", ckguid.getValue()));
        Cookie w_tsfp = driver.manage().getCookieNamed("w_tsfp");
        if (w_tsfp != null) {
            cookies.add(new HttpCookie("w_tsfp", w_tsfp.getValue()));
            expiredDate = w_tsfp.getExpiry();
        }
        return cookies;
    }

    private static void clearCookie() {
        cookies = null;
        driver.manage().deleteAllCookies();
        expiredDate = null;
    }

}
