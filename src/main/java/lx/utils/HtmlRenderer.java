package lx.utils;

import lx.model.Crawlable;
import java.util.List;

/**
 * 负责HTML消息的构建与渲染
 * @author Smallway
 * @date 2026-01-21
 */
public class HtmlRenderer {

    /**
     * 构建精美的HTML邮件消息
     * 采用现代极简卡片式设计，移除Vue和第三方CSS库依赖
     * @param list 爬取的数据列表
     * @return 完整的HTML字符串
     */
    public static String render(List<? extends Crawlable> list) {
        StringBuilder s = new StringBuilder();
        s.append("<!DOCTYPE html><html lang='zh-CN'><head>");
        s.append("<meta charset='UTF-8'><meta name='viewport' content='width=device-width, initial-scale=1.0'>");
        s.append("<title>值得买精选</title>");
        s.append("<style>");
        // Reset & Base
        s.append("body { margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif; background-color: #f5f7fa; color: #333; -webkit-font-smoothing: antialiased; }");
        s.append("a { text-decoration: none; color: inherit; }");
        
        // Container
        s.append(".container { max-width: 960px; margin: 0 auto; padding: 20px; }");
        s.append(".header { text-align: center; margin-bottom: 24px; padding-bottom: 12px; border-bottom: 2px solid #e1e4e8; }");
        s.append(".header h1 { margin: 0; font-size: 24px; color: #2c3e50; font-weight: 700; letter-spacing: 1px; }");
        s.append(".footer { text-align: center; margin-top: 32px; font-size: 12px; color: #95a5a6; width: 100%; }");
        
        // Grid Layout
        s.append(".grid { display: flex; flex-wrap: wrap; gap: 16px; }");
        
        // Card
        s.append(".card { background: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 12px rgba(0,0,0,0.03); border: 1px solid rgba(0,0,0,0.03); display: flex; transition: transform 0.2s; box-sizing: border-box; }");
        s.append(".card { flex: 1 1 calc(50% - 16px); min-width: 300px; }"); // Responsive Grid
        s.append(".card:hover { transform: translateY(-2px); box-shadow: 0 8px 16px rgba(0,0,0,0.06); }");
        
        // Image Section
        s.append(".card-img { width: 140px; height: 140px; flex-shrink: 0; background: #f8f9fa; position: relative; }");
        s.append(".card-img img { width: 100%; height: 100%; object-fit: cover; }");
        
        // Content Section
        s.append(".card-content { flex: 1; padding: 16px; display: flex; flex-direction: column; justify-content: space-between; min-width: 0; }");
        
        // Title
        s.append(".card-title { font-size: 16px; font-weight: 600; line-height: 1.5; color: #2d3748; margin-bottom: 8px; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; height: 48px; }");
        s.append(".card-title:hover { color: #e74c3c; }");
        
        // Price
        s.append(".card-price { font-size: 18px; font-weight: 700; color: #e74c3c; margin-bottom: 8px; }");
        
        // Meta Info
        s.append(".card-meta { display: flex; align-items: center; justify-content: space-between; font-size: 12px; color: #718096; }");
        s.append(".meta-left { display: flex; gap: 8px; align-items: center; }");
        s.append(".tag-mall { background: #edf2f7; color: #4a5568; padding: 2px 8px; border-radius: 4px; font-weight: 500; }");
        s.append(".meta-right { color: #a0aec0; }");

        // Mobile Responsive
        s.append("@media (max-width: 600px) {");
        s.append("  .container { padding: 12px; }");
        s.append("  .card { flex-basis: 100%; }"); // Force full width on mobile
        s.append("  .card-img { width: 110px; height: 110px; }");
        s.append("  .card-content { padding: 12px; }");
        s.append("  .card-title { font-size: 15px; height: auto; -webkit-line-clamp: 3; }");
        s.append("  .card-price { font-size: 16px; }");
        s.append("}");
        
        s.append("</style></head><body>");
        
        s.append("<div class='container'>");
        
        s.append("<div class='header'><h1>精选好价</h1></div>");
        s.append("<div class='grid'>");
        
        for (Crawlable item : list) {
            s.append("<div class='card'>");
            
            // Image Link
            s.append("<a href='").append(item.getUrl()).append("' target='_blank' class='card-img'>");
            if (item.getPicUrl() != null && !item.getPicUrl().isEmpty()) {
                s.append("<img src='").append(item.getPicUrl()).append("' alt='商品图片'>");
            }
            s.append("</a>");
            
            // Content
            s.append("<div class='card-content'>");
            
            // Title
            s.append("<a href='").append(item.getUrl()).append("' target='_blank' class='card-title'>");
            s.append(item.getTitle());
            s.append("</a>");
            
            // Price
            s.append("<div class='card-price'>").append(item.getPrice()).append("</div>");
            
            // Meta
            s.append("<div class='card-meta'>");
            s.append("<div class='meta-left'>");
            if (item.getArticleMall() != null && !item.getArticleMall().isEmpty()) {
                s.append("<span class='tag-mall'>").append(item.getArticleMall()).append("</span>");
            }
            s.append("</div>");
            s.append("<div class='meta-right'>");
            s.append("<span>").append(item.getVoted()).append("赞 · ").append(item.getComments()).append("评</span>");
            s.append("</div>");
            s.append("</div>"); // end card-meta
            
            s.append("</div>"); // end card-content
            s.append("</div>"); // end card
        }
        
        s.append("</div>"); // end grid
        s.append("<div class='footer'>Designed by ZDM Crawler</div>");
        s.append("</div>"); // end container
        
        s.append("</body></html>");
        return s.toString();
    }

    /**
     * 构建简洁的HTML消息（适用于WxPusher等有长度限制的渠道）
     * 去除Vue、CSS框架，仅保留基础表格结构
     * @param list 爬取的数据列表
     * @return 简洁的HTML字符串
     */
    public static String renderSimple(List<? extends Crawlable> list) {
        StringBuilder s = new StringBuilder();
        s.append("<!DOCTYPE html><html><body>");
        s.append("<table border='0' cellspacing='0' cellpadding='4' width='100%' style='font-size:13px;font-family:sans-serif;'>");
        
        for (Crawlable item : list) {
            s.append("<tr style='border-bottom:1px solid #eee;'>");
            
            // 图片列
            s.append("<td width='80' valign='top'>");
            if (item.getPicUrl() != null && !item.getPicUrl().isEmpty()) {
                s.append("<img src='").append(item.getPicUrl()).append("' width='70' height='70' style='border-radius:4px;'/>");
            }
            s.append("</td>");
            
            // 内容列
            s.append("<td valign='top'>");
            s.append("<div><a href='").append(item.getUrl()).append("' style='text-decoration:none;color:#333;font-weight:bold;'>").append(item.getTitle()).append("</a></div>");
            s.append("<div style='margin-top:4px;'><span style='color:#ff5722;font-weight:bold;'>").append(item.getPrice()).append("</span>");
            s.append(" <span style='color:#999;font-size:12px;margin-left:8px;'>").append(item.getVoted()).append("赞/").append(item.getComments()).append("评</span></div>");
            s.append("<div style='color:#999;font-size:12px;margin-top:2px;'>").append(item.getArticleMall()).append("</div>");
            s.append("</td>");
            
            s.append("</tr>");
        }
        
        s.append("</table>");
        s.append("</body></html>");
        return s.toString();
    }
}
