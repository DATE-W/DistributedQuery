import jdk.dynalink.linker.LinkerServices;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class Client {
    private final int serverNum;
    private final int blockNum;
    private final Socket[] sockets;
    private final String[] serverList;
    private final int startPort;
    private Logger logger = Logger.getLogger("Client");
    private Client(int serverNum, int blockNum, int startPort) {
        try {
            FileHandler fileHandler = new FileHandler("log.txt");
            fileHandler.setFormatter(new SimpleFormatter());
            logger.addHandler(fileHandler);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        this.serverNum = serverNum;
        this.blockNum = blockNum;
        this.startPort = startPort;
        this.serverList = new String[this.serverNum];

        try {
            this.sockets = this.establishConnection(this.serverNum);
        } catch (IOException e) {
            System.out.println("Socket连接失败");
            throw new RuntimeException(e);
        }
    }

    static Client createClient(int serverNum, int blockNum) {
        return new Client(serverNum,blockNum,Constants.START_PORT);
    }

    /**
     * 建立socket连接
     * @param serverNum server数目
     * @return socket数组
     * @throws IOException
     */
    private Socket[] establishConnection(int serverNum) throws IOException {
        Socket [] sockets = new Socket[serverNum];
        for(int i=0;i<serverNum;++i){
            int port = this.startPort + i;
            sockets[i] = new Socket(InetAddress.getLocalHost().getHostAddress(),port);
            sockets[i].setSoTimeout(Constants.TIME_OUT);
            serverList[i]= String.valueOf(sockets[i].getPort()-this.startPort);
        }
        return sockets;
    }

    /**
     * 读取console输入的命令
     * @return 拆分后的List,若退出则返回null
     */
    private List<String> readConsole() throws IOException {
        BufferedReader console = new BufferedReader(new InputStreamReader(System.in));

        System.out.println("请输入所查询的作者名称(输入quit直接退出)");
        String s = console.readLine();
        if (s.equals("quit")) {
            return null;
        }

        List<String> list = new ArrayList<>();
        list.add(s);
        System.out.println("是否需要年份筛选(y/n):");
        while (true) {
            s = console.readLine();
            s = s.toLowerCase();
            if (s.equals("y")) {
                break;
            } else if (s.equals("n")) {
                return list;
            } else {
                System.out.println("请输入合法的值");
            }
        }

        System.out.println("请输入起止年份(若不限则输入'-'):");
        s = console.readLine();
        List<String> years = new ArrayList<>(List.of(s.split(" ")));
        list.addAll(years);
        return list;
    }

    private String list2String(List<String> argList) {
        StringBuilder args = new StringBuilder();
        for (int i = 0; i < args.length(); i++) {
            args.append(argList.get(i));
            if (i != args.length() - 1) {
                args.append("|");
            }
        }
        return args.toString();
    }

    private boolean dealRequest(List<String> argList) {
        RequestCounter counter = new RequestCounter(this.blockNum);
        counter.resetCount();

        // 采用多线程发送请求
        String args = this.list2String(argList);
        ExecutorService executorService = Executors.newFixedThreadPool(this.blockNum);
        long start_time = System.currentTimeMillis();

        for (int i = 0; i < this.blockNum; i++) {
            // 在参数最后增加查询的块号
            executorService.execute(new CommunicationManager(this.logger, args + "|" + i, counter, sockets, this.serverList, i));
        }
        // 启动有序关闭，不接受新任务，但会继续执行已提交的任务
        executorService.shutdown();

        try {
            // 等待所有任务完成，设置一个合理的超时时间
            if (!executorService.awaitTermination(1, TimeUnit.HOURS)) {
                // 如果在超时时间内未完成所有任务，则强制关闭
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            // 如果当前线程被中断，强制关闭线程池
            executorService.shutdownNow();
            throw new RuntimeException(e);
        }

        // 若运行到此处，则ok
        if (counter.getCount() == 0) {
            logger.info("--------------------------------------" +
                    "\n查询结果为:" + counter.getResult() +
                    "\n查询用时" + (System.currentTimeMillis() - (double) start_time) / 1000 + "s");
        }
        return true;
    }

    public static void main(String[] args) throws IOException {
        Client c = createClient(Constants.SERVER_NUM,Constants.BLOCK_NUM);

        while (true){
            List<String> arg_list = c.readConsole();
            if(arg_list == null) {
                break;
            }
            else{
                c.dealRequest(arg_list);
            }
        }
    }
}
