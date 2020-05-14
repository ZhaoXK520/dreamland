import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;

import java.util.HashSet;
import java.util.Set;

@ContextConfiguration(locations = {"classpath:applicationContext-redis.xml","classpath:spring-mybatis.xml","classpath:applicationContext-activemq.xml","classpath:applicationContext-solr.xml"})
public class TestRedis extends AbstractJUnit4SpringContextTests {
    @Autowired// redis数据库操作模板
    private RedisTemplate<String, String> redisTemplate;

    private static final String FOLLOWING = "FOLLOWING_";
    private static final String FANS = "FANS_";
    private static final String COMMON_KEY = "COMMON_FOLLOWING";

    public int add(String userId, String followingId) {
        if (userId == null || followingId == null) {
            return -1;
        }
        int isFollow = 0; // 0 = 取消关注 1 = 关注
        String followingKey = FOLLOWING + userId;
        String fansKey = FANS + followingId;
        if(redisTemplate.opsForZSet().rank(followingKey, followingId)==null){ // 说明userId没有关注过followingId
            redisTemplate.opsForZSet().add(followingKey,followingId,System.currentTimeMillis());
            redisTemplate.opsForZSet().add(fansKey, userId, System.currentTimeMillis());
            isFollow = 1;
        }
        return isFollow;
    }

    public int Release(String userId, String followingId) {
        if (userId == null || followingId == null) {
            return -1;
        }
        int isFollow = 0;
        String followingKey = FOLLOWING + userId;
        String fansKey = FANS + followingId;
        if(redisTemplate.opsForZSet().rank(followingKey, followingId)!=null){ // 说明userId没有关注过followingId
            redisTemplate.opsForZSet().remove(followingKey,followingId);
            redisTemplate.opsForZSet().remove(fansKey, fansKey);
            isFollow = 1;
        }
        return isFollow;
    }

    // 验证两个用户之间的关系
    // 0=没关系  1=自己 2=userId关注了otherUserId 3= otherUserId是userId的粉丝 4=互相关注
    public int checkRelations (String userId, String otherUserId) {

        if (userId == null || otherUserId == null) {
            return 0;
        }

        if (userId.equals(otherUserId)) {
            return 1;
        }
        String followingKey = FOLLOWING + userId;
        int relation = 0;
        if (redisTemplate.opsForZSet().rank(followingKey, otherUserId) != null) { // userId是否关注otherUserId
            relation = 2;
        }
        String fansKey = FANS + userId;
        if (redisTemplate.opsForZSet().rank(fansKey, userId) != null) {// userId粉丝列表中是否有otherUserId
            relation = 3;
        }
        if ((redisTemplate.opsForZSet().rank(followingKey, otherUserId) != null)
                && redisTemplate.opsForZSet().rank(fansKey, userId) != null) {
            relation = 4;
        }
        return relation;
    }

    // 获取用户所有关注的人的id
    public Set<String> findFollwings(String userId) {
        return findSet(FOLLOWING + userId);
    }

    // 获取用户所有的粉丝
    public Set<String> findFans(String userId) {
        return findSet(FANS + userId);
    }

    // 根据key获取set
    private Set<String> findSet(String key) {
        if (key == null) {
            return new HashSet<>();
        }
        Set<String> result = redisTemplate.opsForZSet().reverseRange(key,0,-1); // 按照score从大到小排序
        return result;
    }


    @Test
    public void testadd(){
        add("11","22");
        add("11","12");
        add("11","2");
        add("11","21");
    }
    @Test
    public void testFind(){
        Set s1 = findFollwings("11");
        Set s2 = findFans("12");
        System.out.println("111");
    }

}
