package lx.model;

public interface Crawlable {

    String getTitle();

    String getUrl();

    String getPicUrl();

    String getPrice();

    String getVoted();

    String getComments();

    String getArticleMall();

    /**
     * 优惠信息在推送时转化为html表格
     */
    String toHtmlTr();

}
