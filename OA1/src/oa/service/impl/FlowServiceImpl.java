package oa.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.jbpm.api.ProcessEngine;
import org.jbpm.api.ProcessInstance;
import org.jbpm.api.ProcessInstanceQuery;
import org.jbpm.api.TaskQuery;
import org.jbpm.api.task.Task;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import oa.dao.IApplicationDao;
import oa.dao.IApproveInfoDao;
import oa.domain.Application;
import oa.domain.ApproveInfo;
import oa.domain.TaskView;
import oa.domain.User;
import oa.service.IFlowService;


/**
 * 流转方法
 * @author 永远喜欢亚莉莎
 *
 */
@Service
@Transactional
public class FlowServiceImpl implements IFlowService{
	
@Resource
private IApplicationDao applicationDao;
@Resource
private ProcessEngine  processEngine;
@Resource
private IApproveInfoDao approveInfoDao;
@Override
public void submit(Application app) {
	// 保存一个申请记录
	applicationDao.save(app);
	
	// 启动一个流程实例
	Map<String,Application>map=new HashMap<String, Application>();
	map.put("application", app);
	ProcessInstance pi=processEngine.getExecutionService().startProcessInstanceByKey(app.getTemplate().getProcessDefinitionKey(),map);
	//办理提交申请的业务
 TaskQuery query=processEngine.getTaskService().createTaskQuery();
 query.processInstanceId(pi.getId());//获取当前流程实例下唯一的一个任务
  Task task=query.uniqueResult();
  
  String taskId=task.getId();
  processEngine.getTaskService().completeTask(taskId);
}
/**
 * 查询我的任务
 */
@Override
public List<TaskView> findTaskList(User currentUser) {
	// 根据用户登录名查询对应的任务列表
	List<Task> taskList =processEngine.getTaskService().findPersonalTasks(currentUser.getLoginName());
	List<TaskView>list=new ArrayList<TaskView>();
	//从流程变量中获取对应的一个申请实体
	for(Task task : taskList){
		Application application = (Application) processEngine.getTaskService().getVariable(task.getId(), "application");
		TaskView tv = new TaskView(application,task);
		list.add(tv);
	}
	return list;
}
/**
 * 审批处理
 */
@Override
public void approve(ApproveInfo ai, String taskId) {
	// TODO Auto-generated method stub
	Task task=processEngine.getTaskService().getTask(taskId);//根据任务id查询任务
     String  executionId=task.getExecutionId();//获得Id

       //保存一个审批实体
     
     approveInfoDao.save(ai);
     //办理任务
     processEngine.getTaskService().completeTask(taskId);
     ProcessInstanceQuery query = processEngine.getExecutionService().createProcessInstanceQuery();
		query.processInstanceId(executionId);//添加过滤条件
		ProcessInstance pi = query.uniqueResult();
  	if(ai.getApproval()){
	//审批通过
	if(pi== null){
		//当前办理任务是最后一个任务
		//申请状态该为"已通过"
		ai.getApplication().setStatus(Application.STATUS_APPROVED);
	}
	
}else{
	//申请状态该为"未通过"
	ai.getApplication().setStatus(Application.STATUS_UNAPPROVED);
	//审批不通过
	if(pi != null){
		//流程还没有结束，手动结束当前流程实例
		processEngine.getExecutionService().endProcessInstance(executionId, ProcessInstance.STATE_ENDED);

}
}

}

}
