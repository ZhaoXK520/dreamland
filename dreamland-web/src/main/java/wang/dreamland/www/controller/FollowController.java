package wang.dreamland.www.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import wang.dreamland.www.common.PageHelper;
import wang.dreamland.www.entity.User;
import wang.dreamland.www.entity.UserContent;
import wang.dreamland.www.service.UserContentService;
import wang.dreamland.www.service.UserService;

import java.util.*;

@Controller
public class FollowController extends BaseController {

    @Autowired
    RedisTemplate redisTemplate;
    @Autowired
    UserContentService userContentService;

    @Autowired
    UserService userService;

    /**
     * 关注
     * @param id
     * @param followId
     * @return
     */
    @RequestMapping("/follow")
    @ResponseBody
    public Map<String ,Object> follow(@RequestParam(value = "id", required = false) Long id,
                                      @RequestParam(value = "followId", required = false) Long followId){
        User user = getCurrentUser();
        Map<String ,Object> map = new HashMap<String ,Object>();
        if (user==null){
            map.put("followSuccess",-1);
        }
        int addStatus = add(id.toString(),followId.toString());
        if(addStatus==0){
            map.put("followSuccess",0);
        }else {
            map.put("followSuccess",1);
        }
        return map;
    }

    /**
     *取关
     * @param id
     * @param followId
     * @return
     */
    @RequestMapping("/unfollow")
    @ResponseBody
    public Map<String ,Object> unfollow(@RequestParam(value = "id", required = false) Long id,
                                        @RequestParam(value = "followId", required = false) Long followId){
        Map<String ,Object> map = new HashMap<String ,Object>();
        User user = getCurrentUser();
        if (user==null){
            map.put("unfollowSuccess",-1);
        }
        int releaseStatus = release(id.toString(),followId.toString());
        if(releaseStatus==0){
            map.put("unfollowSuccess",0);
        }else {
            map.put("unfollowSuccess",1);
        }
        return map;
    }

    /**
     * 依据id查看用户关注列表
     * @param id
     * @param pageNum
     * @param pageSize
     * @return
     */
    @RequestMapping("/following_list")
    @ResponseBody
    public Map<String ,Object> followingList(@RequestParam(value = "id", required = false) Long id,
                                             @RequestParam(value = "pageNum", required = false) Integer pageNum,
                                             @RequestParam(value = "pageSize", required = false) Integer pageSize){
        Map<String ,Object> map = new HashMap<String ,Object>();
        Set followingSet = findFollwings(id.toString());
        if(followingSet.size()>0){
            PageHelper.Page<User> page = userService.findByIdSet(followingSet,pageNum,pageSize);
            map.put("followPage",page);
        }else{
            map.put("followPage",null);
        }
        return map;
    }

    /**
     * 依据id查看粉丝列表
     * @param id
     * @param pageNum
     * @param pageSize
     * @return
     */
    @RequestMapping("/fans_list")
    @ResponseBody
    public Map<String ,Object> fansList(@RequestParam(value = "id", required = false) Long id,
                                             @RequestParam(value = "pageNum", required = false) Integer pageNum,
                                             @RequestParam(value = "pageSize", required = false) Integer pageSize){
        Map<String ,Object> map = new HashMap<String ,Object>();
        Set fansSet = findFans(id.toString());
        if(fansSet.size()>0){
            PageHelper.Page<User> page = userService.findByIdSet(fansSet,pageNum,pageSize);
            map.put("fansPage",page);
        }else{
            map.put("fansPage",null);
        }
        return map;
    }

    /**
     * 分析用户关系
     * @param id
     * @param otherUserId
     * @return
     */
    @ResponseBody
    @RequestMapping("/relation")
    public Map<String ,Object> relation(@RequestParam(value = "id", required = false) Long id,
                                        @RequestParam(value = "otherUserId", required = false) Long otherUserId){
        Map<String ,Object> map = new HashMap<String ,Object>();
        map.put("relation",checkRelations(id.toString(),otherUserId.toString()));
        return map;
    }

    /**
     * 进入用户空间
     * @param model
     * @param id
     * @return
     */
    @RequestMapping("/space")
    public String userSpace(Model model, @RequestParam(value = "id",required = false) Long id){
        User user = getCurrentUser();
        model.addAttribute("user",user);
        User spaceUser = getUser(id);
        model.addAttribute("spaceUser",spaceUser);
        Integer relations = 0;
        if(user!=null){
            relations = checkRelations(user.getId().toString(),id.toString());
        }
        model.addAttribute("relations",relations);
        List<UserContent> categorys = userContentService.findCategoryByUid(id);
        model.addAttribute( "categorys",categorys );
        UserContent content = new UserContent();
        content.setPersonal("0");
        content.setuId(id);
        PageHelper.Page<UserContent> page =  findAll(content,null,  10); //分页
        model.addAttribute( "page",page );
        int blogsNum = getBlogsNum(id);
        model.addAttribute("blogsNum",blogsNum);
        //查询点赞热度
        Integer upvoteNum = userContentService.getUpvoteNum(id);
        if(upvoteNum == null){
            model.addAttribute("upvoteNum",0);
        }else{
            model.addAttribute("upvoteNum",upvoteNum);
        }

        Set followingSet = findFollwings(id.toString());
        if(followingSet.size()>0){
            PageHelper.Page<User> followingPage = userService.findByIdSet(followingSet,null,null);
            model.addAttribute("followingPage",followingPage);
        }else{
            model.addAttribute("followingPage",null);
        }
        Set fansSet = findFans(id.toString());
        if(fansSet.size()>0){
            PageHelper.Page<User> fansPage = userService.findByIdSet(fansSet,null,null);
            model.addAttribute("fansPage",fansPage);
        }else{
            model.addAttribute("fansPage",null);
        }
        return "personal/userInfo";
    }

    private static final String FOLLOWING = "FOLLOWING_";
    private static final String FANS = "FANS_";
    
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

    public int release(String userId, String followingId) {
        if (userId == null || followingId == null) {
            return -1;
        }
        int isFollow = 0;
        String followingKey = FOLLOWING + userId;
        String fansKey = FANS + followingId;
        if(redisTemplate.opsForZSet().rank(followingKey, followingId)!=null){ // 说明userId关注过followingId
            redisTemplate.opsForZSet().remove(followingKey,followingId);
            redisTemplate.opsForZSet().remove(fansKey, userId);
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

    public int getBlogsNum(Long uid){
        List<UserContent> list = userContentService.findByUserId( uid );
        return list.size();
    }
}
