package tgc.edu.mcy.controller;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import tgc.edu.mcy.custom.JournalUtil;
import tgc.edu.mcy.entity.Book;
import tgc.edu.mcy.entity.SysAdmin;
import tgc.edu.mcy.entity.SysRole;
import tgc.edu.mcy.entity.SysUser;
import tgc.edu.mcy.security.UserUtils;
import tgc.edu.mcy.service.BookService;
import tgc.edu.mcy.service.RoleService;
import tgc.edu.mcy.service.SysAdminService;
import tgc.edu.mcy.service.SysUserService;

@Controller
public class LoginController {
	@Autowired
	private SysUserService userDAO;
	@Autowired
	private RoleService roleDAO;
	@Autowired
	private SysAdminService sysAdminDAO;
	@Autowired
	private UserUtils userUtils;
	@Autowired
	private JournalUtil journalUtil;
	@Autowired
	private BookService bookDAO;
	
	@RequestMapping(value="/login",method=RequestMethod.GET)
	public String index(ModelMap map) {
		List<SysUser> list = userDAO.findAll();
		if(list.size() == 0) {
			test();
		}
		List<Book> findAll = bookDAO.findByNumberAfter(0);
		map.put("book", findAll);
		return "index";
	}
	
	/**
	 * 登录
	 * */
	@RequestMapping(value="login1")
	public String login() {
		return "login";
	}
	
	@RequestMapping(value="/update")
	public String update() {
		return "update";
	}
	
	@RequestMapping(value="/update_user")
	public String update_user() {
		return "update_user";
	}
	
	@RequestMapping(value="/download")
	@ResponseBody
	public Object download(Integer id, HttpServletRequest request, HttpServletResponse response) {
		bookDAO.download(id, request, response);
		return null;
	}
	
	@RequestMapping(value="/xg")
	public String xg(String pwd) {
		SysUser user = userUtils.getUser();
		BCryptPasswordEncoder encoder=new BCryptPasswordEncoder();
		user.setPassword(encoder.encode(pwd));
		userDAO.save(user);
		return "redirect:logout";
	}
	
	/**
	 * 判断用户用是否已存在
	 * */
	@RequestMapping(value="/username")
	@ResponseBody
	public Boolean username(String username) {
		SysUser user = userDAO.findByUsername(username);
		if(user == null) {
			return false;
		}else {
			return true;
		}
	}
	
	/**
	 * 判断原密码是否正确
	 * */
	@RequestMapping(value="/pwd")
	@ResponseBody
	public Boolean pwd(String password) {
		SysUser user = userUtils.getUser();
		BCryptPasswordEncoder encoder=new BCryptPasswordEncoder();
		boolean f = encoder.matches(password,user.getPassword());
		return f;
	}
	
	/**
	 *  退出
	 * */
	@RequestMapping(value="/logout1")
	public String logout1() {
		SysUser user = userUtils.getUser();
		List<SysRole> roles = user.getRoles();
		String name = null;
		for (SysRole s : roles) {
			if(s.getCode().equals("ROLE_SYSTEM")) {
				name = "系统管理员";
			}else if(s.getCode().equals("ROLE_ADMIN")) {
				name = "图书管理员";
			}else if(s.getCode().equals("ROLE_USER") || s.getCode().equals("ROLE_TEACHER")) {
				name = "用户";
			}
		}
		journalUtil.save(user.getUsername(), "退出", name);
		return "redirect:logout";
	}
	
	/**
	 * 图书管理员日志
	 * */
	@RequestMapping(value="/admin")
	public String admin() {
		SysUser user = userUtils.getUser();
		journalUtil.save(user.getUsername(), "登录", "图书管理员");
		return "redirect:main";
	}
	
	/**
	 * 系统管理员日志
	 * */
	@RequestMapping(value="/system")
	public String system() {
		SysUser user = userUtils.getUser();
		journalUtil.save(user.getUsername(), "登录", "系统管理员");
		return "redirect:main";
	}
	
	/**
	 * 用户日志
	 * */
	@RequestMapping(value="user")
	public String user() {
		SysUser user = userUtils.getUser();
		journalUtil.save(user.getUsername(), "登录", "用户");
		return "redirect:welCome";
	}
	
	/**
	 * 系统管理员，图书管理登录
	 * */
	@RequestMapping(value="/main")
	public String main() {
		return "main";
	}
	
	/**
	 * 用户
	 * */
	@RequestMapping(value="/welCome")
	public String welCome(ModelMap map) {
		List<Book> findAll = bookDAO.findByNumberAfter(0);
		map.put("book", findAll);
		return "welCome";
	}
	
	@RequestMapping(value="/keyword")
	public String keyword(String str, ModelMap map) {
		List<Book> book = bookDAO.findByNumberAfterAndNameLikeOrIsbnLikeOrPressLikeOrAuthorLikeOrKindTypeLike(0, "%"+str+"%", "%"+str+"%", "%"+str+"%", "%"+str+"%", "%"+str+"%");
		map.put("book", book);
		return "book";
	}
	
	
	/**
	 *   初始数据库中没有数据，先添加数据
	 */
	private void test() {
		BCryptPasswordEncoder encoder=new BCryptPasswordEncoder();   //密码加密类
		SysRole role = new SysRole();		
		role.setName("系统管理员");
		role.setCode("ROLE_SYSTEM");
		roleDAO.save(role);
		SysRole role2 = new SysRole();
		role2.setName("图书管理员");
		role2.setCode("ROLE_ADMIN");
		roleDAO.save(role2);
		SysRole role3 = new SysRole();
		role3.setName("学生");
		role3.setCode("ROLE_USER");
		roleDAO.save(role3);
		SysRole role4 = new SysRole();
		role4.setName("老师");
		role4.setCode("ROLE_TEACHER");
		roleDAO.save(role4);
		
		SysUser user = new SysUser();
		user.setUsername("system");
		user.setPassword(encoder.encode("system")); 
		user.setName("系统管理员");
		user.getRoles().add(role);
		userDAO.save(user);
		
		SysAdmin sysAdmin = new SysAdmin();
		sysAdmin.setUsername("admin");
		sysAdmin.setNumber(0);
		sysAdmin.setName("图书管理员");
		sysAdmin.setPassword(encoder.encode("admin"));
		sysAdmin.getRoles().add(role2);
		sysAdminDAO.save(sysAdmin);
		
	}
}
