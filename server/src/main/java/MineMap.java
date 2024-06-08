import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * discription : 封装Map实现可序列化
 * @author : Pikachudy
 * @date : 2022/12/9 23:48
 */
class MineMap<S, I extends Number> implements Serializable {
    private Map<Object, Integer> map;
    public MineMap() {
        this.map = new HashMap<>();
    }

    public MineMap(Map<Object, Integer> map) {
        this.map = map;
    }

    /**
     * 获取Map中某key对应键值,可设定默认值。若不设定默认值则返回null
     * @param key 键
     * @return 键值
     */
    public Integer get(String key) {
        if(map.containsKey(key)){
            return (Integer) map.get(key);
        }
        return null;
    }

    /**
     * 重载
     * 获取Map中某key对应键值,可设定默认值。若不设定默认值则返回null
     * @param key 键
     * @return 键值
     */
    public Integer get(String key,Integer defaultValue) {
        return map.getOrDefault(key,defaultValue);
    }

    /**
     * 添加键值对
     * @param key 键
     * @param value 值
     */
    public void put(String key,Integer value){
        map.put(key,value);
    }
}