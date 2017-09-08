package com.atguigu.crud.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.atguigu.crud.bean.Employee;
import com.atguigu.crud.bean.Msg;
import com.atguigu.crud.service.EmployeeService;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;

/**
 * 处理员工CRUD请求
 * 
 */
@Controller
public class EmployeeController {

	@Autowired
	EmployeeService employeeService;

	/**
	 * 单个批量二合一
	 * 批量删除：1-2-3
	 * 单个删除：1
	 * @param id
	 * @return
	 */
	@ResponseBody
	@RequestMapping(value="/emp/{ids}",method=RequestMethod.DELETE)
	public Msg deleteEmp(@PathVariable("ids") String ids){
		//批量删除
		if(ids.contains("-"))
		{
			String[] str_ids = ids.split("-");
			//组成id的集合
			List<Integer> del_ids = new ArrayList<Integer>();
			for (String string : str_ids) {
				del_ids.add(Integer.parseInt(string));
			}
			employeeService.deleteBatch(del_ids);
		}else{
			Integer id = Integer.parseInt(ids);
			employeeService.deleteEmp(id);
		}
		return Msg.success();		
	}
	
	
	/**
	 * 如果直接发送ajax=PUT形式的请求
	 * 问题：请求体中有数据，但是Employee对象封装不上
	 * 原因：
	 * Tomcat，
	 * 		将请求体中的数据，封装成一个map
	 * 		request.getParameter("empName") 就会从这个map中去找
	 * 		SpringMVC 封装POJO对象的时候，
	 * 				会把POJO中的每个属性的值，request.getParameter()拿到
	 * AJAX发送PUT请求引发的血案：
	 * 		PUT请求：请求体中的数据，request.getParemeter()拿不到
	 * 		Tomcat一看是PUT请求就不会封装请求体中的数据为map，只有POST形式请求的请求体中的数据才会封装成map
	 * 
	 * 解决方案：
	 * 我们要能支持直接发送PUT之类的请求还要封装请求体中的数据
	 * 配置上HttpPutFormContentFilter
	 * 作用：将请求体中的数据解析包装成一个map
	 * request被重新包装，request.getParameter被重写，就会从自己封装的map中取数据
	 * SpringMVC提供了解决方案
	 * 
	 * 员工更新方法
	 * @param employee
	 * @return
	 */
	@ResponseBody
	@RequestMapping(value="/emp/{empId}",method=RequestMethod.PUT)
	public Msg saveEmp(Employee employee){
		System.out.println("将要更新额员工数据："+employee.toString());
		employeeService.updateEmp(employee);
		return Msg.success();
	}
	
	/**
	 * 根据id查询
	 * @param id
	 * @return
	 */
	@ResponseBody
	@RequestMapping(value="/emp/{id}",method=RequestMethod.GET)
	public Msg getEmp(@PathVariable("id") Integer id){
		Employee employee = employeeService.getEmp(id);
		return Msg.success().add("emp",employee);
	}
	
	/**
	 * 检查用户名是否可用
	 * 1、支持JSR303校验
	 * 2、导入Hibernate-Validator包
	 * @param empName
	 * @return
	 */
	@ResponseBody
	@RequestMapping("/checkuser")
	public Msg checkUser(@RequestParam("empName") String empName){
		//先判断用户名是否是合法的表达式
		String regex = "(^[a-zA-Z0-9_-]{6,16}$)|(^[\u2E80-\u9FFF]{2,5})";
		if(!empName.matches(regex)){
			return Msg.fail().add("va_msg","用户名必须是6-16位英文和数字的组合或者2-5位中文");
		}
		boolean b = employeeService.checkUser(empName);
		if(b){
			return Msg.success();
		}else{
			return Msg.fail().add("va_msg","用户名不可用");
		}
	}
	
	@ResponseBody
	@RequestMapping(value="/emp",method=RequestMethod.POST)
	public Msg saveEmp(@Valid Employee employee,BindingResult result){
		if(result.hasErrors()){
			//校验失败，返回失败,在模态框中显示错误的信息
			Map<String,Object> map = new HashMap<String,Object>();
			List<FieldError> errors = result.getFieldErrors();
			for (FieldError fieldError : errors) {
				System.out.println("错误的字段"+fieldError.getField());
				System.out.println("错误信息："+fieldError.getDefaultMessage());
				map.put(fieldError.getField(), fieldError.getDefaultMessage());
			}
			return Msg.fail().add("errorField", map);
		}else{
			employeeService.saveEmp(employee);
		}
		return Msg.success();
	}
	
	/**
	 * ResponseBody 正常工作需要导入Jackson包，负责将返回的数据自动转化为json数据
	 * @param pn
	 * @param model
	 * @return
	 */
	@ResponseBody
	@RequestMapping(value="/emps")
	public Msg getEmpsWithJson(
			@RequestParam(value = "pn", defaultValue = "1") Integer pn,
			Model model) {
		// 这不是一个分页查询；
		// 引入PageHelper分页插件
		// 在查询之前只需要调用，传入页码，以及每页的大小
		PageHelper.startPage(pn, 5);
		// startPage后面紧跟的这个查询就是一个分页查询
		List<Employee> emps = employeeService.getAll();
		// 使用pageInfo包装查询后的结果，只需要将pageInfo交给页面就行了。
		// 封装了详细的分页信息,包括有我们查询出来的数据，传入连续显示的页数
		PageInfo page = new PageInfo(emps, 5);
		return Msg.success().add("pageInfo",page);
	}

	//@RequestMapping("/emps")
	public String getEmps(
			@RequestParam(value = "pn", defaultValue = "1") Integer pn,
			Model model) {
		// 这不是一个分页查询；
		// 引入PageHelper分页插件
		// 在查询之前只需要调用，传入页码，以及每页的大小
		PageHelper.startPage(pn, 5);
		// startPage后面紧跟的这个查询就是一个分页查询
		List<Employee> emps = employeeService.getAll();
		// 使用pageInfo包装查询后的结果，只需要将pageInfo交给页面就行了。
		// 封装了详细的分页信息,包括有我们查询出来的数据，传入连续显示的页数
		PageInfo page = new PageInfo(emps, 5);
		model.addAttribute("pageInfo", page);

		return "list";
	}

}
