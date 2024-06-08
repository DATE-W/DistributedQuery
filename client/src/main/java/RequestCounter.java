import lombok.Data;

@Data
public class RequestCounter {
    private int count;
    private int requestNum;
    private int result;
    RequestCounter(int requestNum) {
        this.requestNum = requestNum;
        this.result = 0;
    }

    /**
     * 将计数器设定为 request_num
     */
    public void resetCount() {
        this.count = this.requestNum;
    }

    /**
     * 计数减 1
     */
    public void countDown() {
        this.count--;
    }

    /**
     * 增加结果
     */
    public void addResult(int result) {
        this.result += result;
    }
}
