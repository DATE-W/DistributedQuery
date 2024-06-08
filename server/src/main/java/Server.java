import lombok.Data;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

@Data
public class Server {
    private final String shellDir;
    private final String shellName;
    private final String xmlDir;
    private final int serverLabel;
    private final int port;
    private final int selectMethod;
    private List<MineMap<String, Integer>> maps;
    private Socket accept;

    Server(int serverLabel, int selectMethod) {
        this.serverLabel = serverLabel;
        this.port = serverLabel + 1100;
        this.shellDir = "/home/ubuntu/Distribution/DistributedQuery/script/src/main/shell";
        this.shellName = "select.sh";
        // 监听块的名称
        this.xmlDir = "/home/ubuntu/Distribution/ServerFile0"+serverLabel+"/output_000";
        this.selectMethod = selectMethod;
    }

    /**
     * 建立索引
     */
    public void makeMaps() throws ParserConfigurationException, IOException, SAXException {
        IndexManager manager = new IndexManager();
        this.maps = new ArrayList<>();
        for (int i = 0; i < Constants.BLOCK_NUM; i++) {
            maps.add(null);
        }
        if(this.selectMethod==2){
            System.out.println("Server"+this.serverLabel+"正在为每一文件块分别构建索引……");
            int count = 0;
            for(int i=(this.serverLabel+Constants.BLOCK_NUM-Constants.REPLICAS)%Constants.BLOCK_NUM;count<Constants.REPLICAS;i=((i+1)%Constants.BLOCK_NUM)) {
                maps.set(i,manager.createIndex("/home/ubuntu/Distribution/ServerFile0" + serverLabel + "/output_000" + i+".xml"));
                count++;
            }
            maps.set(serverLabel,manager.createIndex("/home/ubuntu/Distribution/ServerFile0" + serverLabel + "/output_000" + serverLabel+".xml"));
            System.out.println("Server"+this.serverLabel+"索引建立完毕");
        }
    }

    /**
     * 监听相应端口，若端口被占用则抛出异常并输出
     * @return 若已建立连接返回true,添加监听成功返回false
     */
    public boolean startListen(){
        try{
            ServerSocket serverSocket = new ServerSocket(this.port);
            System.out.println(this.serverLabel+"号服务正在监听"+this.port+"端口……");
            this.accept = serverSocket.accept();
            System.out.println(port+"端口"+"已建立连接！");
            return true;
        } catch (IOException e) {
            System.out.println("端口"+this.port+"已被占用！");
            return false;
        }
    }

    /**
     * 接收socket流消息——查找参数
     * @return 返回消息中的--参数组成的列表
     */
    public List<String> waitingForRequest() throws IOException {
        System.out.println(this.port+":正在等待");
        BufferedReader reader = new BufferedReader(new InputStreamReader(accept.getInputStream()));
        // 获取请求
        String request = reader.readLine();
        return new ArrayList<>(List.of(request.split("\\|")));
    }

    /**
     * 返回检索结果至服务端
     * @param result 检索结果
     */
    public void replyRequest(String result) throws IOException {
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(accept.getOutputStream()));
        writer.write(result+'\n');
        writer.flush();
    }

    /**
     * 通过查找本地索引来查询
     * @param args 参数 - 最后一个为查询的文件块序列号
     * @return 查询结果
     */
    public String searchMap(List<String> args) {
        // 取出块对应的序列号
        String blockIndex = args.get(args.size() - 1);
        int maxYear = 2024;
        int minYear = 1900;
        if (args.size() == 4) {
            // name l_year r_year block_index
            if("-".equals(args.get(1))){
                //只有上限值
                maxYear = Integer.parseInt(args.get(2));
            }
            else if("-".equals(args.get(2))){
                //只有下限值
                minYear = Integer.parseInt(args.get(1));
            }
            else{
                minYear = Integer.parseInt(args.get(1));
                maxYear = Integer.parseInt(args.get(2));
            }
        }
        int count=0;
        for(int year=minYear+1;year<maxYear;year++){
            count+=this.maps.get(Integer.parseInt(blockIndex)).get(args.get(0)+"_"+year,0);
        }
        if(args.size()==2){
            // 计算不含有年份的标签内的author 例如<www>
            count+=this.maps.get(Integer.parseInt(blockIndex)).get(args.get(0)+"_null",0);
        }
        return String.valueOf(count);
    }

    /**
     * 调用脚本查询文件
     * @param shellDir shell所在路径
     * @param shellName shell名称
     * @param args shell参数 - 最后一个为查询的文件块序列号
     * @return shell返回值
     * @throws IOException 捕获shell输出失败
     */
    public String callShell(String shellDir, String shellName, List<String> args) throws IOException {
        // 取出块序列号
        String blockIndex = args.get(args.size()-1);
        args.remove(args.size() - 1);

        List<String> command = new ArrayList<>();
        command.add("./" + shellName);
        command.addAll(args);
        command.add(this.xmlDir+blockIndex+".xml");
        System.out.println(command);
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.directory(new File(shellDir));

        // 调用脚本
        int runningStatus = 0;
        Process p = null;
        try {
            p = processBuilder.start();
            try {
                runningStatus = p.waitFor();
            } catch (InterruptedException e) {
                System.out.println("shell run failed!\n" + e);
            }
        } catch (IOException e) {
            System.out.println("shell run failed\n"+e);
        }
        if (runningStatus != 0) {
            System.out.println("call shell failed.Error code:"+runningStatus);
        }

        // 返回值
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(p.getInputStream()));
        String line = null;
        while ((line = bufferedReader.readLine()) != null) {
            return line;
        }
        return null;
    }

    public static void main(String []args) throws IOException {
        int select_method = 0;
        BufferedReader console = new BufferedReader(new InputStreamReader(System.in));
        while (true){
            System.out.println("请选择查询方式\n1-脚本查询\n2-使用本地索引文件:");
            String s = console.readLine();
            if("1".equals(s)||"2".equals(s)){
                select_method = Integer.parseInt(s);
                break;
            }
            else{
                System.out.println("输入非法！");
            }
        }

        Thread[] thread_list = new Thread[Constants.SERVER_NUM];
        for(int i = 0; i< Constants.SERVER_NUM; ++i){
            thread_list[i] = new Thread(new ServerRunner(new Server(i,select_method),i,Thread.currentThread()));
            thread_list[i].start();
        }
        while(true){
            console = new BufferedReader(new InputStreamReader(System.in));
            int stop_server = Integer.parseInt(console.readLine());
            if(stop_server>=0&&stop_server<= Constants.SERVER_NUM) {
                thread_list[stop_server].stop();
                System.out.println("server_"+stop_server+"已停止");
            }
        }
    }
}
