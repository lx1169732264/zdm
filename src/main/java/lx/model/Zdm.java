package lx.model;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.util.Objects;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Zdm implements Crawlable {

    @JSONField(name = "article_id")
    String articleId;

    @JSONField(name = "article_title")
    String title;

    @JSONField(name = "article_url")
    String url;
    @JSONField(name = "article_pic_url")
    String picUrl;

    @JSONField(name = "article_price")
    String price;
    @JSONField(name = "article_rating")
    String voted;

    @JSONField(name = "article_comment")
    String comments;
    @JSONField(name = "article_mall")
    String articleMall;

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
    public Integer obtainSortOrder() {
        return Integer.valueOf(comments);
    }

    public Zdm() {
        pushed = false;
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
