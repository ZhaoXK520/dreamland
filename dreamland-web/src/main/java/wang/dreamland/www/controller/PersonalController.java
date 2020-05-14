package wang.dreamland.www.controller;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.encoding.Md5PasswordEncoder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import wang.dreamland.www.common.Constants;
import wang.dreamland.www.common.DateUtils;
import wang.dreamland.www.common.MD5Util;
import wang.dreamland.www.common.PageHelper;
import wang.dreamland.www.entity.OpenUser;
import wang.dreamland.www.entity.User;
import wang.dreamland.www.entity.UserContent;
import wang.dreamland.www.entity.UserInfo;
import wang.dreamland.www.service.*;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by 12903 on 2018/5/19.
 */
@Controller
public class PersonalController extends BaseController{
    private final static Logger log = Logger.getLogger(PersonalController.class);
    @Autowired
    private CommentService commentService;
    @Autowired
    private UpvoteService upvoteService;
    @Autowired
    private UserContentService userContentService;
    @Autowired
    private UserInfoService userInfoService;
    @Autowired
    private UserService userService;
    @Autowired
    private OpenUserService openUserService;

    @Autowired
    private SolrService solrService;
    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 初始化个人主页数据
     * @param model
     * @param id
     * @param pageNum
     * @param pageSize
     * @return
     */
    @RequestMapping("/list")
    public String findList(Model model, @RequestParam(value = "id",required = false) String id,
                           @RequestParam(value = "manage",required = false) String manage ,
                           @RequestParam(value = "pageNum",required = false) Integer pageNum,
                           @RequestParam(value = "pageSize",required = false) Integer pageSize) {
        User user = getCurrentUser();
        UserContent content = new UserContent();
        UserContent uc = new UserContent();
        UserContent uc2 = new UserContent();
        if(user!=null){
            model.addAttribute( "user",user );
            content.setuId( user.getId() );
            uc.setuId(user.getId());
        }else{
            return "../login";
        }
        log.info("初始化个人主页信息");

        if(StringUtils.isNotBlank(manage)){
            model.addAttribute("manage",manage);
        }

        //查询梦分类
        List<UserContent> categorys = userContentService.findCategoryByUid(user.getId());
        model.addAttribute( "categorys",categorys );
        //发布的梦 不含私密梦
        content.setPersonal("0");
        pageSize = 10; //默认每页显示10条数据
        PageHelper.Page<UserContent> page =  findAll(content,pageNum,  pageSize); //分页

        model.addAttribute( "page",page );

        //查询私密梦
        uc.setPersonal("1");
        PageHelper.Page<UserContent> page2 =  findAll(uc,pageNum,  pageSize);
        model.addAttribute( "page2",page2 );

        //查询热梦
        pageSize = 15; //默认每页显示15条数据
        UserContent uct = new UserContent();
        uct.setPersonal("0");
        PageHelper.Page<UserContent> hotPage =  findAllByUpvote(uct,pageNum,  pageSize);
        model.addAttribute( "hotPage",hotPage );

        //查询博客总量,包括私密文章
        int blogsNum = getBlogsNum(user.getId());
        model.addAttribute("blogsNum",blogsNum);
        //查询点赞热度
        Integer upvoteNum = userContentService.getUpvoteNum(user.getId());
        if(upvoteNum == null){
            model.addAttribute("upvoteNum",0);
        }else{
            model.addAttribute("upvoteNum",upvoteNum);
        }

        Set followingSet = findFollwings(user.getId().toString());
        if(followingSet.size()>0){
            PageHelper.Page<User> followingPage = userService.findByIdSet(followingSet,null,null);
            model.addAttribute("followingPage",followingPage);
        }else{
            model.addAttribute("followingPage",null);
        }
        Set fansSet = findFans(user.getId().toString());
        if(fansSet.size()>0){
            PageHelper.Page<User> fansPage = userService.findByIdSet(fansSet,null,null);
            model.addAttribute("fansPage",fansPage);
        }else{
            model.addAttribute("fansPage",null);
        }
        return "personal/personal";
    }

    /**
     * 根据分类名称查询所有文章
     * @param model
     * @param category
     * @return
     */
    @RequestMapping("/findByCategory")
    @ResponseBody
    public Map<String,Object> findByCategory(Model model, @RequestParam(value = "id",required = false) Long id,
                                             @RequestParam(value = "category",required = false) String category,@RequestParam(value = "pageNum",required = false) Integer pageNum ,
                                             @RequestParam(value = "pageSize",required = false) Integer pageSize) {

        Map map = new HashMap<String,Object>(  );
        if(id==null){
            User user = getCurrentUser();
            if(user==null) {
                map.put("pageCate","fail");
                return map;
            }
            id = user.getId();
        }
        pageSize = 10; //默认每页显示10条数据
        PageHelper.Page<UserContent> pageCate = userContentService.findByCategory(category,id,pageNum,pageSize);
        map.put("pageCate",pageCate);
        return map;
    }


    /**
     * 根据用户id查询私密梦
     * @param model
     * @param pageNum
     * @param pageSize
     * @return
     */
    @RequestMapping("/findPersonal")
    @ResponseBody
    public Map<String,Object> findPersonal(Model model,
                                           @RequestParam(value = "pageNum",required = false) Integer pageNum ,
                                           @RequestParam(value = "pageSize",required = false) Integer pageSize) {

        Map map = new HashMap<String,Object>(  );
        User user = getCurrentUser();
        if(user==null) {
            map.put("page2","fail");
            return map;
        }
        pageSize = 10; //默认每页显示10条数据
        PageHelper.Page<UserContent> page = userContentService.findPersonal(user.getId(),pageNum,pageSize);
        map.put("page2",page);
        return map;
    }

    /**
     * 查询出所有文章并分页 根据点赞数倒序排列
     * @param model
     * @param pageNum
     * @param pageSize
     * @return
     */
    @RequestMapping("/findAllHotContents")
    @ResponseBody
    public Map<String,Object> findAllHotContents(Model model, @RequestParam(value = "pageNum",required = false) Integer pageNum , @RequestParam(value = "pageSize",required = false) Integer pageSize) {

        Map map = new HashMap<String,Object>(  );
        User user = getCurrentUser();
        if(user==null) {
            map.put("hotPage","fail");
            return map;
        }
        pageSize = 15; //默认每页显示15条数据
        UserContent uct = new UserContent();
        uct.setPersonal("0");
        PageHelper.Page<UserContent> hotPage =  findAllByUpvote(uct,pageNum,  pageSize);
        map.put("hotPage",hotPage);
        return map;
    }

    /**
     * 根据文章id删除文章
     * @param model
     * @param cid
     * @return
     */
    @RequestMapping("/deleteContent")
    public String deleteContent(Model model, @RequestParam(value = "cid",required = false) Long cid) {

        User user = getCurrentUser();
        if(user==null) {
            return "../login";
        }
        commentService.deleteByContentId(cid);
        upvoteService.deleteByContentId(cid);
        userContentService.deleteById(cid);
        solrService.deleteById(cid);
        return "redirect:/list?manage=manage";
    }

    /**
     * 进入个人资料修改页面
     * @param model
     * @return
     */
    @RequestMapping("/profile")
    public String profile(Model model, @RequestParam(value = "email",required = false) String email,
                          @RequestParam(value = "password",required = false) String password,
                          @RequestParam(value = "phone",required = false) String phone) {
        User user = getCurrentUser();
        if(StringUtils.isBlank(user.getPassword()) && StringUtils.isBlank(password)){
            return "redirect:/list";
        }

        if(StringUtils.isNotBlank(email)){
            user.setEmail(email);
            user.setPassword(new Md5PasswordEncoder().encodePassword(password,email));
            user.setPhone(phone);
            userService.update(user);
        }
//        List<OpenUser> openUsers = openUserService.findByUid(user.getId());
//        setAttribute(openUsers,model);
        UserInfo userInfo =   userInfoService.findByUid(user.getId());
        model.addAttribute("user",user);
        model.addAttribute("userInfo",userInfo);

        return "personal/profile";
    }

    /**
     * 保存个人头像
     * @param model
     * @param url
     * @return
     */
    @RequestMapping("/saveImage")
    @ResponseBody
    public  Map<String,Object>  saveImage(Model model,@RequestParam(value = "url",required = false) String url) {
        Map map = new HashMap<String,Object>(  );
        User user = getCurrentUser();
        user.setImgUrl(url);
        userService.update(user);
        map.put("msg","success");
        return map;
    }

    /**
     * 保存用户信息
     * @param model
     * @param name
     * @param nickName
     * @param sex
     * @param address
     * @param birthday
     * @return
     */
    @RequestMapping("/saveUserInfo")
    public String saveUserInfo(Model model, @RequestParam(value = "name",required = false) String name ,
                               @RequestParam(value = "nick_name",required = false) String nickName,
                               @RequestParam(value = "sex",required = false) String sex,
                               @RequestParam(value = "address",required = false) String address,
                               @RequestParam(value = "birthday",required = false) String birthday){
        User user = getCurrentUser();
        if(user==null){
            return "../login";
        }
        UserInfo userInfo = userInfoService.findByUid(user.getId());
        boolean flag = false;
        if(userInfo == null){
            userInfo = new UserInfo();
        }else {
            flag = true;
        }
        userInfo.setName(name);
        userInfo.setAddress(address);
        userInfo.setSex(sex);
        Date bir =  DateUtils.StringToDate(birthday,"yyyy-MM-dd");
        userInfo.setBirthday(bir);
        userInfo.setuId(user.getId());
        if(!flag){
            userInfoService.add(userInfo);
        }else {
            userInfoService.update(userInfo);
        }

        user.setNickName(nickName);
        userService.update(user);

        model.addAttribute("user",user);
        model.addAttribute("userInfo",userInfo);
        return "personal/profile";
    }

    /**
     * 进入修改密码页面
     * @param model
     * @return
     */
    @RequestMapping("/repassword")
    public String repassword(Model model) {
        User user = getCurrentUser();
        if(user!=null) {
            model.addAttribute("user",user);
            return "personal/repassword";
        }
        return "../login";
    }

    /**
     * 修改密码
     * @param model
     * @param oldPassword
     * @param password
     * @return
     */
    @RequestMapping("/updatePassword")
    public String updatePassword(Model model, @RequestParam(value = "old_password",required = false) String oldPassword,
                                 @RequestParam(value = "password",required = false) String password){

        User user = getCurrentUser();
        if(user!=null) {
                oldPassword = MD5Util.encodeToHex(Constants.SALT + oldPassword);
                if (user.getPassword().equals(oldPassword)) {
                    password = MD5Util.encodeToHex(Constants.SALT + password);
                    user.setPassword(password);
                    userService.update(user);
                    model.addAttribute("message", "success");
                } else {
                    model.addAttribute("message", "fail");
                }
        }
        model.addAttribute("user",user);
        return "personal/passwordSuccess";
    }

    /**
     * 解除qq绑定
     * @param model
     * @return
     */
    @RequestMapping("/remove_qq")
    public String removeQQ(Model model){
        User user = getCurrentUser();
        if(user == null){
            return "../login";
        }
        openUserService.deleteByUidAndType(user.getId(),Constants.OPEN_TYPE_QQ);
        List<OpenUser> openUsers = openUserService.findByUid(user.getId());
        setAttribute(openUsers,model);
        UserInfo userInfo =   userInfoService.findByUid(user.getId());
        model.addAttribute("user",user);
        model.addAttribute("userInfo",userInfo);
        return "personal/profile";
    }

    public void setAttribute(List<OpenUser> openUsers,Model model){
        if(openUsers!=null && openUsers.size()>0){
            for(OpenUser openUser:openUsers){
                if(Constants.OPEN_TYPE_QQ.equals(openUser.getOpenType())){
                    model.addAttribute("qq",openUser.getOpenType());
                }else if(Constants.OPEN_TYPE_WEIBO.equals(openUser.getOpenType())){
                    model.addAttribute("weibo",openUser.getOpenType());
                }else if(Constants.OPEN_TYPE_WEIXIN.equals(openUser.getOpenType())){
                    model.addAttribute("weixin",openUser.getOpenType());
                }
            }
        }
    }

    public int getBlogsNum(Long uid){
        List<UserContent> list = userContentService.findByUserId( uid );
        return list.size();
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
}
