import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * 文件块分配器
 */
public class FileBlockDistributor {
    /**
     * 一致性hash
     */
    class ConsistentHash {
        // 存服务器hash值
        private TreeMap<Long, String> servers = new TreeMap<>();

        // 存储服务器列表
        private String[] serverList;

        // 计算hash
        public HashFunction hashFunction = Hashing.md5();

        ConsistentHash(String[] serverList) {
            this.serverList = serverList;
            for (String server : serverList) {
                this.addServer(server);
            }
        }

        /**
         * 向环中添加一个服务器
         * @param server 服务器label
         */
        public void addServer(String server) {
            long hash = hashFunction.hashString(server, StandardCharsets.UTF_8).asLong();
            servers.put(hash, server);
        }

        /**
         * 删除环里的一个服务器
         * @param server 服务器label
         */
        public void removeServer(String server) {
            long hash = hashFunction.hashString(server, StandardCharsets.UTF_8).asLong();
            servers.remove(hash);
        }

        /**
         * 根据文件快的hash找对应的服务器
         * @param key 文件块标签
         * @return 最近服务器label
         */
        public String getServer(String key) {
            long hash = hashFunction.hashString(key, StandardCharsets.UTF_8).asLong();
            if (!servers.containsKey(hash)) {
                // 如果没找到，就找最近的服务器
                SortedMap<Long, String> tailMap = servers.tailMap(hash);
                hash = tailMap.isEmpty() ? servers.firstKey() : tailMap.firstKey();
            }
            return servers.get(hash);
        }
    }

    private final ConsistentHash consistentHash;

    FileBlockDistributor(String[] serverList) {
        this.consistentHash = new ConsistentHash(serverList);
    }

    public void addServer(String server) {
        consistentHash.addServer(server);
    }

    public void removeServer(String server) {
        consistentHash.removeServer(server);
    }

    public void distribute(String fileBlock) {
        // 计算文件块的hash
        long hash = consistentHash.hashFunction.hashString(fileBlock, StandardCharsets.UTF_8).asLong();
        // 寻找服务器
        String server = consistentHash.getServer(fileBlock);
        // 设置冗余块
        List<String> servers = getReplicas(server, Constants.REPLICAS);

        for (String s : servers) {
            store(s, fileBlock);
        }
    }

    public List<String> getReplicas(String server, int count) {
        // 存储冗余服务器的列表
        List<String> replicas = new ArrayList<>();
        // 遍历服务器列表
        for (int i = 0; i < consistentHash.serverList.length; i++) {
            if (consistentHash.serverList[i].equals(server)) {
                for (int j = 1; j <= count; j++) {
                    replicas.add(consistentHash.serverList[(i+j) % consistentHash.serverList.length]);
                }
                break;
            }
        }
        return replicas;
    }

    public String getServer(String block_key) {
        return this.consistentHash.getServer(block_key);
    }

    private boolean store(String server, String fileBlock) {
        try {
            Socket socket = new Socket();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String fileName = fileBlock;
            // 发请求
            writer.write("put block\n");
            writer.flush();
            // 响应
            String msg = reader.readLine();
            if (!msg.equals("request_ok")) {
                System.out.println("块上传请求未被正确响应，传输终止");
                return false;
            }
            // 发送文件名
            writer.write(fileName+'\n');
            writer.flush();
            // 等待响应
            msg = reader.readLine();
            if(!msg.equals("name_ok")){
                System.out.println("块文件名未被正确响应，传输中止");
                return false;
            }

            // 从文件中读取
            int blockSize =1024*1024*768;
            String filePath = "/home/ubuntu/Distribution/XmlBlocks/";
            byte[] block;   // 缓冲区
            try {
                // 读取本地文件
                BufferedInputStream bufferedInputStream = new BufferedInputStream(new FileInputStream(filePath + "\\" + fileName));
                block = new byte[blockSize];
                int len = -1;
                while ((len = bufferedInputStream.read(block))!=-1) {
                }
            } catch (FileNotFoundException e) {
                System.out.println("找不到指定文件，请检查文件路径及文件名称");
                return false;
            } catch (IOException e) {
                System.out.println("本地文件读取失败！");
                return false;
            }

            // 准备文件传输
            BufferedOutputStream bufferedOutputStream = new BufferedOutputStream((socket.getOutputStream()));
            bufferedOutputStream.write(block);
            bufferedOutputStream.flush();

            msg = reader.readLine();
            if(!msg.equals("block_ok")){
                System.out.println("块文件上传失败，传输中止");
                return false;
            }
        } catch (IOException e) {
            //throw new RuntimeException(e);
            System.out.println(e);
            System.out.println("上传文件块时创建IO流失败");
            return false;
        }
        return true;
    }
}
