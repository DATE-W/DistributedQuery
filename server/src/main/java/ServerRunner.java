import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.List;

public class ServerRunner implements Runnable {
    private final Server m_server;
    private final int serverLabel;
    private Thread thread;

    /**
     *
     * @param mServer 需要run的server
     * @param serverLabel server序号
     */
    ServerRunner(Server mServer,int serverLabel,Thread thread){
        this.m_server = mServer;
        this.serverLabel = serverLabel;
        this.thread=thread;
    }

    @Override
    public void run() {
        try {
            m_server.makeMaps();
        } catch (ParserConfigurationException | IOException | SAXException e) {
            throw new RuntimeException(e);
        }

        while (true){
            if(m_server.startListen()){
                // 与客户端成功建立socket连接
                while(true){
                    try {
                        List<String> args = m_server.waitingForRequest();
                        System.out.println(m_server.getServerLabel()+"号服务端正在检索:"+args);
                        String result =null;
                        if(m_server.getSelectMethod()==1){
                            result= m_server.callShell(m_server.getShellDir(), m_server.getShellName(),args);
                        }
                        else{
                            result = m_server.searchMap(args);
                        }
                        System.out.println(m_server.getServerLabel()+"号服务端检索结果为:"+result);
                        m_server.replyRequest(result);
                    } catch (IOException | NullPointerException e) {
                        System.out.println(m_server.getServerLabel()+"号服务端与客户端断开连接！重新监听……");
                        break;
                    }
                }
            }
            else{
                // 若断联则log并重新监听
                System.out.println(m_server.getServerLabel()+"号服务端与客户端socket连接失败！重新监听……");
            }
        }

    }
}

