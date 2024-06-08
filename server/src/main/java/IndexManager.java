import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.*;

public class IndexManager {
    /**
     * 从xml文件建立作者-论文数索引表
     * @param path xml文件路径
     * @return 作者-论文数索引表
     */
    MineMap<String,Integer> createIndex(String path) throws ParserConfigurationException, SAXException, IOException {
        // 使用 SAXParserFactory 创建 SAXParser
        SAXParserFactory factory = SAXParserFactory.newInstance();
        SAXParser parser = factory.newSAXParser();

        // 使用 SAXParser 解析 XML 文件
        AuthorPaperCountHandler handler = new AuthorPaperCountHandler();
        parser.parse(new File(path), handler);

        // 使用 AuthorPaperCountHandler 获取解析后的作者论文数量映射
        MineMap<String,Integer> res = new MineMap<>(handler.getAuthorPaperCount());
        return res;
    }

    /**
     * map序列化存储到指定路径
     * @param map 映射表
     * @param fileName 映射表名称（不含后缀）
     * @param path 存储路径
     */
    private void SerializeMap(MineMap<String,Integer> map,String fileName,String path){
        // 序列化
        try {
            // 创建序列化输出流
            FileOutputStream fos = new FileOutputStream(path+fileName+".ser");
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            // 序列化Map对象
            oos.writeObject(map);
            // 关闭输出流
            oos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 从文件中反序列化Map
     * @param fileName 文件名称
     * @param path 文件路径
     * @return 反序列化后的Map
     */
    private MineMap<String, Integer> DeserializeMap(String fileName,String path){
        try{
            // 创建序列化输入流
            FileInputStream fis = new FileInputStream(path+fileName+".ser");
            ObjectInputStream ois = new ObjectInputStream(fis);
            // 反序列化Map对象
            MineMap<String, Integer> map = (MineMap<String, Integer>) ois.readObject();
            // 关闭输入流
            ois.close();
            return map;
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void main(String[] args) throws ParserConfigurationException, IOException, SAXException {
        IndexManager manager = new IndexManager();
        // 获取所有元素
        MineMap<String, Integer> map = manager.createIndex("xmlBlocks\\output_0001.xml");
        // 序列化
        manager.SerializeMap(map,"author_paper_1","D:\\homework\\Distribution\\XmlPartition\\src\\main\\java\\index\\");
        // 反序列化
        MineMap<String, Integer> map1 = manager.DeserializeMap("author_paper_1","D:\\homework\\Distribution\\XmlPartition\\src\\main\\java\\index\\");
        int count=0;
        for(int year =1900;year<=2022;++year){
            count+=map1.get("Yuval Cassuto_"+year,0);
        }
        System.out.println(count);
        //System.out.println(map);
    }
}
