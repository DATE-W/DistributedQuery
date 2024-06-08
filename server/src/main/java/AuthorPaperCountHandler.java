import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * discription : 用于解析 XML文件,构建Map的 handler
 */
class AuthorPaperCountHandler extends DefaultHandler {
    private Map<Object, Integer> authorPaperCount;
    private List<String> currentAuthor;
    private String currentYear;
    private boolean isAuthor;
    private boolean isYear;

    public AuthorPaperCountHandler() {
        this.authorPaperCount = new HashMap<Object, Integer>();
        this.currentAuthor = new ArrayList<>();
        this.currentYear = null;
        this.isAuthor = false;
        this.isYear = false;
    }

    public Map<Object, Integer> getAuthorPaperCount() {
        return authorPaperCount;
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        // 在开始元素时记录当前元素是否未作者或年份
        if ("author".equals(qName)) {
            isAuthor = true;
        } else if ("year".equals(qName)) {
            isYear = true;
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        // 在characters方法中，若为作者或者年份，则记录下来作者和发表年份
        if (isAuthor) {
            currentAuthor.add(new String(ch, start, length));
            isAuthor = false;
        } else if (isYear) {
            currentYear = new String(ch, start, length);
            isYear = false;
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        // 在结束元素时更新作者论文数量映射
        if (qName.equals("article") || qName.equals("inproceedings")
                || qName.equals("proceedings") || qName.equals("book")
                || qName.equals("incollection") || qName.equals("phdthesis")
                || qName.equals("mastersthesis") || qName.equals("www")
                || qName.equals("person") || qName.equals("data")) {
            if (!currentAuthor.isEmpty() && currentYear != null) {
                // 构建索引
                for (String author : currentAuthor) {
                    String index = author + "_" + currentYear;
                    int count = authorPaperCount.getOrDefault(index, 0);
                    authorPaperCount.put(index, count + 1);
                }
            } else if (!currentAuthor.isEmpty()) {
                // 主要是为了记录<www>标签中的作者
                // 构建索引
                for (String author : currentAuthor) {
                    String index = author + "_null";
                    int count = authorPaperCount.getOrDefault(index, 0);
                    authorPaperCount.put(index, count + 1);
                }
            }
            currentAuthor.clear();
            currentYear = null;
        }
    }
}