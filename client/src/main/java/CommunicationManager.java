import lombok.Data;

import java.io.*;
import java.net.Socket;
import java.util.List;
import java.util.logging.Logger;

/**
 * 客户端-服务端通信管理器——便于多线程
 */
@Data
public class CommunicationManager implements Runnable {
    private FileBlockDistributor distributor;
    private final RequestCounter counter;
    private Socket[] sockets;
    private String[] serverList;
    private String requestMsg;
    private int blockIndex;
    private Logger logger;
    @Override
    public void run() {
        // 取出对应的Socket
        Socket socket = this.sockets[Integer.parseInt(this.distributor.getServer(String.valueOf(blockIndex)))];
        String socketLabel = String.valueOf(socket.getPort() - Constants.START_PORT);
        // 取出冗余服务器对应label列表便于查找冗余服务器上的文件块,便于正式服务器异常时的t处理
        List<String> replicasServerLabel = this.distributor.getReplicas(this.distributor.getServer(String.valueOf(blockIndex)), Constants.REPLICAS);
        // 尝试次数，便于出错遍历
        int tryCount = 0;
        while (true) {
            try {
                // lock
                synchronized (socket) {
                    this.sendMessage(socket, this.requestMsg);
                    logger.info("等待" + socketLabel + "服务端回应...");
                    String result = this.waitingForRes(socket);

                    // 运行到此说明无异常
                    logger.info(socketLabel + "返回结果为：" + result);
                    // 修改counter
                    synchronized (this.counter) {
                        this.counter.addResult(Integer.parseInt(result));
                        this.counter.countDown();
                    }
                    break;
                }
            } catch (IOException e) {
                // 发生异常，则无法跳出循环
                if (tryCount < Constants.REPLICAS) {
                    socket = this.sockets[Integer.parseInt(replicasServerLabel.get(tryCount++))];
                    socketLabel = String.valueOf(socket.getPort()-Constants.START_PORT);
                    logger.warning("等待超时，尝试从"+socketLabel+"查询第"+(tryCount+1)+"个冗余块……");
                }
                else{
                    logger.severe("已有超过"+Constants.REPLICAS+"个服务端宕机，查询失败");
                    // 修改counter
                    synchronized (this.counter){
                        this.counter.setCount(-1);
                    }
                    break;
                }
            }
        }
    }

    /**
     *
     * @param counter 计数器
     * @param sockets socket表——便于遇到异常时处理
     * @param blockIndex 当前尝试搜索的文件块号
     */
    CommunicationManager(Logger logger,String requestMsg,RequestCounter counter,Socket[] sockets,String[] server_list,int blockIndex){
        this.requestMsg = requestMsg;
        this.counter = counter;
        this.sockets = sockets;
        this.distributor = new FileBlockDistributor(server_list);
        this.blockIndex = blockIndex;
        this.logger = logger;
    }

    /**
     * 发送信息至server
     * 若连接断开则抛出异常
     * @param socket 尝试连接的服务端的socket
     * @param msg 发送信息
     * @throws IOException socket连接断开
     */
    void sendMessage(Socket socket,String msg) throws IOException {
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        writer.write(msg+'\n');
        writer.flush();
    }

    /**
     * 等待接收消息
     * 若连接断开则抛出异常
     * @param socket 尝试连接的服务端的socket
     * @return 接收到的查询结果
     * @throws IOException socket连接断开
     */
    String waitingForRes(Socket socket) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        return reader.readLine();
    }
}
