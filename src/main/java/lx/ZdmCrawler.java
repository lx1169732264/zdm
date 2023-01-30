package lx;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
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

import cn.hutool.http.HttpUtil;
import lx.model.Zdm;
import lx.utils.StreamUtils;
import lx.utils.Utils;

import static lx.utils.Const.ZDM_URL;

public class ZdmCrawler {

    public static void main(String[] args) {
        Set<Zdm> zdms = ZDM_URL.stream().map(url -> {
            List<Zdm> zdmPage = new ArrayList<>();
            for (int i = 1; i <= 20; i++) {
                String s = HttpUtil.get(url + i, 10000);
                List<Zdm> zdmPart = JSONObject.parseArray(s, Zdm.class);
                zdmPart.forEach(zdm -> {
                    if (zdm.getComments().endsWith("k")) {
                        String comments = zdm.getComments().substring(0, zdm.getComments().length() - 2);
                        zdm.setComments(new BigDecimal(comments).multiply(new BigDecimal(1000)).toString());
                    }
                    if (zdm.getVoted().endsWith("k")) {
                        String voted = zdm.getVoted().substring(0, zdm.getComments().length() - 2);
                        zdm.setVoted(new BigDecimal(voted).multiply(new BigDecimal(1000)).toString());
                    }
                });
                zdmPage.addAll(zdmPart);
            }
            return zdmPage;
        }).flatMap(Collection::stream).sorted(Comparator.comparing(Zdm::getComments).reversed()).collect(Collectors.toCollection(LinkedHashSet::new));

        HashSet<String> unPushed = Utils.readFile("./unpushed.txt");
        zdms.addAll(StreamUtils.map(unPushed, o -> JSONObject.parseObject(o, Zdm.class)));

        HashSet<String> blackWords = Utils.readFile("./black_words.txt");
        blackWords.remove("");
        HashSet<String> pushedIds = Utils.readFile("./pushed.txt");

        zdms = new HashSet<>(StreamUtils.filter(zdms, z ->
                StringUtils.isBlank(StreamUtils.findFirst(blackWords, w -> z.getTitle().contains(w))) //不包含黑词
                        && Integer.parseInt(z.getVoted()) > Integer.parseInt(System.getenv("minVoted")) //值的数量
                        && Integer.parseInt(z.getComments()) > Integer.parseInt(System.getenv("minComments")) //评论的数量
                        && !z.getPrice().contains("前") //不是前xxx名的耍猴抢购
                        && !pushedIds.contains(z.getArticleId()) //不是已经推送过的
        ));
        zdms.forEach(z -> System.out.println(z.getArticleId() + " | " + z.getTitle()));

        if (zdms.size() > Integer.parseInt(System.getenv("MIN_PUSH_SIZE"))) {
            send(Utils.buildMessage(new ArrayList<>(zdms)));
            Utils.write("./pushed.txt", true, StreamUtils.map(zdms, Zdm::getArticleId));
        } else {
            Utils.write("./unpushed.txt", false, StreamUtils.map(zdms, JSONObject::toJSONString));
        }
    }

    public static void send(String text) {
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
        }
    }

}
