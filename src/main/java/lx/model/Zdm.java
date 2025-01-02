package lx.model;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.util.Objects;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "ZDM")
public class Zdm implements Crawlable {

    //主键用值得买接口提供的id
    @Id
    @JSONField(name = "article_id")
    String articleId;

    //标题
    @JSONField(name = "article_title")
    String title;

    //优惠信息正文链接
    @JSONField(name = "article_url")
    String url;

    //优惠信息商品图片缩略图
    @JSONField(name = "article_pic_url")
    String picUrl;

    //价格
    @JSONField(name = "article_price")
    String price;

    //点值的数量
    @JSONField(name = "article_rating")
    String voted;

    //评论的数量
    @JSONField(name = "article_comment")
    String comments;

    //商城(京东/天猫/拼多多...)
    @JSONField(name = "article_mall")
    String articleMall;

    //优惠信息发布时间(由接口返回的时间戳格式, 因为不方便阅读,这个字段不持久化进数据库)
    @Transient
    @JSONField(name = "timesort")
    String timesort;

    //优惠信息发布时间(yyyy-MM-ddTHH:mm:ss)
    String article_time;

    //是否已推送
    Boolean pushed;

    @Override
    public String toHtmlTr() {
        return "<tr>" +
                "<td><img src='" + picUrl + "'/></td>" +
                "<td>" + "<a target='_blank' href='" + url + "'>" + title + "</a></td>" +
                "<td>" + price + "</td>" +
                "<td>" + voted + "/" + comments + "</td>" +
                "<td>" + articleMall + "</td>" +
                "</tr>";
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Zdm) {
            return articleId.equals(((Zdm) obj).articleId);
        }
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(articleId);
    }
}
