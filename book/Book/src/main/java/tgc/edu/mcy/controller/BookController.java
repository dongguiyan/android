package tgc.edu.mcy.controller;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import tgc.edu.mcy.custom.DataGridParam;
import tgc.edu.mcy.custom.EasyuiResult;
import tgc.edu.mcy.custom.JournalUtil;
import tgc.edu.mcy.entity.Book;
import tgc.edu.mcy.entity.Kind;
import tgc.edu.mcy.entity.Records;
import tgc.edu.mcy.entity.SysRole;
import tgc.edu.mcy.entity.SysUser;
import tgc.edu.mcy.form.BookForm;
import tgc.edu.mcy.security.UserUtils;
import tgc.edu.mcy.service.BookService;
import tgc.edu.mcy.service.KindService;
import tgc.edu.mcy.service.RecordsService;
import tgc.edu.mcy.service.SysUserService;

@Controller
@RequestMapping(value="/book")
public class BookController {
	@Autowired
	private BookService bookDAO;
	@Autowired
	private KindService kindDAO;
	@Autowired
	private UserUtils userUtils;
	@Autowired
	private JournalUtil journalUtil;
	@Autowired
	private RecordsService recordsDAO;
	@Autowired
	private SysUserService sysUserDAO;
	
	@RequestMapping(value="/main")
	public String main() {
		return "book/main";
	}
	
	@RequestMapping(value="/edit")
	public String edit(Integer id, ModelMap map) {
		List<Kind> kind = kindDAO.findAll();
		Book model = new Book();
		if(id != null) {
			model = bookDAO.findById(id);
			map.put("type", model.getKind().getType());
		}
		map.put("kind", kind);
		map.put("model", model);
		return "book/edit";
	}
	
	@RequestMapping(value="/apply")
	public String apply() {
		return "book/apply";
	}
	
	@RequestMapping(value="/message")
	public String message(Integer id, ModelMap map) {
		Book book = bookDAO.findById(id);
		map.put("book", book);
		return "book/message";
	}
	
	/**
	 * 还书
	 * */
	@RequestMapping(value="/huanshu")
	@ResponseBody
	public Object huanshu(Integer id) {
		Records records = recordsDAO.findById(id);
		Book book = bookDAO.findById(records.getBook().getId());
		records.setState("已还");
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");//设置日期格式
		String data = df.format(new Date());
		records.setReturnTime(data);
		recordsDAO.save(records);
		
		book.setNumber(book.getNumber()+1);
		book.setLoanNumber(book.getLoanNumber()-1);
		bookDAO.save(book);
		
		return new EasyuiResult("操作成功");
	}
	
	/**
	 * 借书
	 * @throws ParseException 
	 * */
	@RequestMapping(value="/borrow")
	@ResponseBody
	public synchronized Object borrow(Integer id) throws ParseException {
		SysUser user = userUtils.getUser();
		user.setNumber(user.getNumber()+1);		
		sysUserDAO.save(user);
		
		List<SysRole> roles = user.getRoles();
		String str = null;
		for (SysRole sysRole : roles) {
			str = sysRole.getName();
		}
		System.out.println(str+"====");
		
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");//设置日期格式
		String data = df.format(new Date());
		Date utilDate = df.parse(data);
		Records records = new Records();
		records.setUser(user);
		records.setBook(bookDAO.findById(id));
		if(str.equals("图书管理员")) {
			records.setState("未还");
			records.setReamark("图书管理员");			
		}else if(str.equals("老师")  || str.equals("学生")){
			records.setState("待审核");
			records.setReamark("用户");	
		}
		records.setStartTime(data);
		Date newDate = stepMonth(utilDate, 2);
		records.setEndTime(df.format(newDate));
		recordsDAO.save(records);
		
		Book book = bookDAO.findById(id);
		Integer number = book.getNumber()-1;
		Integer number1 = book.getLoanNumber()+1;
		book.setNumber(number);
		book.setLoanNumber(number1);
		bookDAO.save(book);
		return new EasyuiResult("借书成功");
	}
	
	//在原有时间是加几个月
	public static Date stepMonth(Date sourceDate, int month) {
        Calendar c = Calendar.getInstance();
        c.setTime(sourceDate);
        c.add(Calendar.MONTH, month);
        return c.getTime();
    }
	
	/**
	 * 添加图书
	 * */
	@RequestMapping(value="/save")
	@ResponseBody
	public Object save(BookForm form, MultipartFile file) {
		try {
			bookDAO.save(form, file);
			if(form.getId() == null) {
				SysUser user = userUtils.getUser();
				journalUtil.save(user.getUsername(), "添加图书", "图书管理员");				
			}
			return new EasyuiResult("数据保存成功");
		} catch (Exception e) {
			return new EasyuiResult(false, "数据保存失败");
		}
	}
	
	/**
	 * 申请同意
	 * */
	@RequestMapping(value="/consent")
	@ResponseBody
	public Object consent(Integer id) {
		Records records = recordsDAO.findById(id);
		records.setState("未还");
		recordsDAO.save(records);
		return new EasyuiResult("操作成功");
	}
	
	/**
	 * 显示所有图书
	 * */
	@RequestMapping(value="/list")
	@ResponseBody
	public Object list(DataGridParam param, String name, String press, String kind) {
		Pageable pageable = param.buildPageable();
		Specification<Book> specification = buildSpec(param, name, press, kind);
		Page<Book> page = bookDAO.findAll(specification, pageable);
		HashMap<String , Object> result = param.getPageResult(page);
		return result;
	}

	private Specification<Book> buildSpec(DataGridParam param, String name, String press, String kind) {
		Specification<Book> specification=new Specification<Book>() {

			private static final long serialVersionUID = 1L;

			@Override
			public Predicate toPredicate(Root<Book> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
				HashSet<Predicate> rules=new HashSet<>();
				if(StringUtils.hasText(name)) {
					rules.add(cb.like(root.get("name"), "%"+name+"%"));
				}
				if(StringUtils.hasText(press)) {
					rules.add(cb.like(root.get("press"), "%"+press+"%"));
				}
				if(StringUtils.hasText(kind)) {
					rules.add(cb.like(root.get("kind").get("type"), "%"+kind+"%"));
				}
				return cb.and(rules.toArray(new Predicate[rules.size()]));
			}
		};
		return specification;
	}
	
	/**
	 * 删除图书
	 * */
	@RequestMapping(value="/delete")
	@ResponseBody
	public Object delete(Integer id) {
		SysUser user = userUtils.getUser();
		journalUtil.save(user.getUsername(), "删除图书", "图书管理员");
		
		Book book = bookDAO.findById(id);
		Kind kind = kindDAO.findById(book.getKind().getId());
		kind.setNumber(kind.getNumber()-book.getNumber());
		kindDAO.save(kind);
		
		bookDAO.deleteById(id);
		return new EasyuiResult("删除成功");
	}
}
