package wang.dreamland.www.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import wang.dreamland.www.common.PageHelper;
import wang.dreamland.www.entity.User;
import wang.dreamland.www.entity.UserContent;
import wang.dreamland.www.service.UserContentService;

import java.util.HashMap;
import java.util.Map;

@Controller
public class AdminController extends BaseController{
    @Autowired
    private UserContentService userContentService;

    @RequestMapping("/admin")
    public String admin(Model model,@RequestParam(value = "pageNum",required = false) Integer pageNum ,
                        @RequestParam(value = "pageSize",required = false) Integer pageSize){
        User user = getCurrentUser();
        if(user==null){
            return "../login";
        }
        model.addAttribute("user",user);
        UserContent content = new UserContent();
        content.setAudit(0);
        PageHelper.Page page1  = userContentService.findAll(content,pageNum,pageSize);
        model.addAttribute("page1",page1);
        content.setAudit(-1);
        PageHelper.Page page2  = userContentService.findAll(content,pageNum,pageSize);
        model.addAttribute("page2",page2);
        return "personal/admin";
    }

    @RequestMapping("failPass")
    @ResponseBody
    public Map failPass(@RequestParam(value = "cid",required = false) Long cid){
        Map map = new HashMap();
        UserContent content = null;
        if(cid!=null) {
            content = userContentService.findById(cid,0);
        }
        if(content!=null){
            content.setAudit(-1);
        }
        if(content==null||userContentService.updateById(content)==0){
            map.put("ok",0);
        }else {
            map.put("ok",1);
        }
        return map;
    }

    @RequestMapping("successPass")
    @ResponseBody
    public Map successPass(@RequestParam(value = "cid",required = false) Long cid){
        Map map = new HashMap();
        UserContent content = null;
        if(cid!=null) {
            content = userContentService.findById(cid,0);
        }
        if(content!=null){
            content.setAudit(1);
        }
        if(content==null||userContentService.updateById(content)==0){
            map.put("ok",0);
        }else {
            map.put("ok",1);
        }
        return map;
    }
}
