package wang.dreamland.www.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tk.mybatis.mapper.entity.Example;
import wang.dreamland.www.common.PageHelper;
import wang.dreamland.www.dao.UserMapper;
import wang.dreamland.www.entity.User;
import wang.dreamland.www.service.UserService;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Created by wly on 2017/12/15.
 */
@Service
public class UserServiceImpl implements UserService {
    @Autowired
    private UserMapper userMapper;

    public int regist(User user) {
       return userMapper.insert(user);
    }

    public User login(String name, String password) {
        User user = new User();
        user.setEmail( name );
        user.setPassword( password );
        return userMapper.selectOne( user );
        //return userMapper.findUserByNameAndPwd( name,password );
    }

    public User findByEmail(String email) {
        User user = new User();
        user.setEmail( email );
        return userMapper.selectOne( user );
       // return userMapper.findByEmail(email);
    }

    @Override
    public User findByPhone(String phone) {
        User user = new User();
        user.setPhone(phone);
        return userMapper.selectOne(user);
    }

    @Override
    public User findById(Long id) {
        User user = new User();
        user.setId(id);
        return userMapper.selectOne(user);
    }

    public User findByEmailActive(String email) {
        User user = new User();
        user.setEmail( email );
        return userMapper.selectOne( user );
        // return userMapper.findByEmail(email);
    }

    public User findById(String id) {
        User user = new User();
        Long uid = Long.parseLong( id );
        user.setId( uid );
        return userMapper.selectOne( user );
    }

    public User findById(long id) {
        User user = new User();
        user.setId( id );
        return userMapper.selectOne( user );
    }

    public void deleteByEmail(String email) {
        User user = new User();
        user.setEmail( email );
        userMapper.delete( user );
    }

    public void deleteByEmailAndFalse(String email) {
        User user = new User();
        user.setEmail( email );
        userMapper.delete( user );
    }

    public void update(User user) {
        userMapper.updateByPrimaryKeySelective( user );
    }

    @Override
    public PageHelper.Page<User> findByIdSet(Set set, Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);//开始分页
        Example example = new Example(User.class);
        Example.Criteria criteria = example.createCriteria();
        criteria.andIn("id",new ArrayList<Object>(set));
        List<User> list = userMapper.selectByExample(example);
        PageHelper.Page page = PageHelper.endPage();
        return page;
    }
}
