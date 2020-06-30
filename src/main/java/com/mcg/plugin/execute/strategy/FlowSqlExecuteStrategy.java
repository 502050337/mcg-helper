/*
 * @Copyright (c) 2018 缪聪(mcg-helper@qq.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");  
 * you may not use this file except in compliance with the License.  
 * You may obtain a copy of the License at  
 *     
 *     http://www.apache.org/licenses/LICENSE-2.0  
 *     
 * Unless required by applicable law or agreed to in writing, software  
 * distributed under the License is distributed on an "AS IS" BASIS,  
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  
 * See the License for the specific language governing permissions and  
 * limitations under the License.
 */

package com.mcg.plugin.execute.strategy;

import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.mcg.common.SpringContextHelper;
import com.mcg.common.sysenum.EletypeEnum;
import com.mcg.common.sysenum.LogOutTypeEnum;
import com.mcg.common.sysenum.LogTypeEnum;
import com.mcg.common.sysenum.MessageTypeEnum;
import com.mcg.entity.flow.sqlexecute.FlowSqlExecute;
import com.mcg.entity.generate.ExecuteStruct;
import com.mcg.entity.generate.RunResult;
import com.mcg.entity.message.FlowBody;
import com.mcg.entity.message.Message;
import com.mcg.plugin.build.McgProduct;
import com.mcg.plugin.dbconn.FlowDataAdapterImpl;
import com.mcg.plugin.dbconn.McgBizAdapter;
import com.mcg.plugin.execute.ProcessStrategy;
import com.mcg.plugin.websocket.MessagePlugin;
import com.mcg.service.FlowService;
import com.mcg.util.DataConverter;

public class FlowSqlExecuteStrategy implements ProcessStrategy {

	private static Logger logger = LoggerFactory.getLogger(FlowSqlExecuteStrategy.class);
	
	@Override
	public void prepare(ArrayList<String> sequence, McgProduct mcgProduct, ExecuteStruct executeStruct) throws Exception {
	    FlowSqlExecute flowSqlExecute = (FlowSqlExecute)mcgProduct;
		executeStruct.getRunStatus().setExecuteId(flowSqlExecute.getId());
	}

	@Override
	public RunResult run(McgProduct mcgProduct, ExecuteStruct executeStruct) throws Exception {
		
	    FlowSqlExecute flowSqlExecute = (FlowSqlExecute)mcgProduct;
		JSON parentParam = DataConverter.getParentRunResult(flowSqlExecute.getId(), executeStruct);
		
        Message message = MessagePlugin.getMessage();
        message.getHeader().setMesType(MessageTypeEnum.FLOW);		
        FlowBody flowBody = new FlowBody();
        flowBody.setFlowId(flowSqlExecute.getFlowId());
        flowBody.setSubFlag(executeStruct.getSubFlag());
        flowBody.setOrderNum(flowSqlExecute.getOrderNum());
        flowBody.setLogOutType(LogOutTypeEnum.PARAM.getValue());
        flowBody.setEleType(EletypeEnum.SQLEXECUTE.getValue());
        flowBody.setEleTypeDesc(EletypeEnum.SQLEXECUTE.getName() + "--》" + flowSqlExecute.getSqlExecuteProperty().getName());
        flowBody.setEleId(flowSqlExecute.getId());
        flowBody.setComment("参数");
        
        if(parentParam == null) {
        	flowBody.setContent("{}");
        } else {
        	flowBody.setContent(JSON.toJSONString(parentParam, true));
        }
        flowBody.setLogType(LogTypeEnum.INFO.getValue());
        flowBody.setLogTypeDesc(LogTypeEnum.INFO.getName());
        message.setBody(flowBody);
        MessagePlugin.push(flowSqlExecute.getMcgWebScoketCode(), executeStruct.getSession().getId(), message);		
		
        
        
        flowSqlExecute = DataConverter.flowOjbectRepalceGlobal(DataConverter.addFlowStartRunResult(parentParam, executeStruct), flowSqlExecute);		
		RunResult runResult = new RunResult();
		runResult.setElementId(flowSqlExecute.getId());
		
		FlowService flowService = SpringContextHelper.getSpringBean(FlowService.class);
        McgBizAdapter mcgBizAdapter = new FlowDataAdapterImpl(flowService.getMcgDataSourceById(flowSqlExecute.getSqlExecuteCore().getDataSourceId()));
        
        Message sqlMessage = MessagePlugin.getMessage();
        sqlMessage.getHeader().setMesType(MessageTypeEnum.FLOW);		
        FlowBody sqlFlowBody = new FlowBody();
        sqlFlowBody.setFlowId(flowSqlExecute.getFlowId());
        sqlFlowBody.setSubFlag(executeStruct.getSubFlag());
        sqlFlowBody.setEleType(EletypeEnum.SQLEXECUTE.getValue());
        sqlFlowBody.setEleTypeDesc(EletypeEnum.SQLEXECUTE.getName() + "--》" + flowSqlExecute.getSqlExecuteProperty().getName());
        sqlFlowBody.setEleId(flowSqlExecute.getId());
        sqlFlowBody.setComment("执行SQL语句");
        sqlFlowBody.setContent(flowSqlExecute.getSqlExecuteCore().getSource());
        sqlFlowBody.setLogType(LogTypeEnum.INFO.getValue());
        sqlFlowBody.setLogTypeDesc(LogTypeEnum.INFO.getName());
        sqlMessage.setBody(sqlFlowBody);
        MessagePlugin.push(flowSqlExecute.getMcgWebScoketCode(), executeStruct.getSession().getId(), sqlMessage);
        
        int rows = mcgBizAdapter.executeUpdate(flowSqlExecute.getSqlExecuteCore().getSource(), null);
     
        JSONObject runResultJson = (JSONObject)parentParam;
        runResult.setJsonVar(JSON.toJSONString(runResultJson, true));
        runResult.setSourceCode("成功执行，影响行数【" + rows  + "】行");
		executeStruct.getRunStatus().setCode("success");
		
		logger.debug("SQL执行控件：{}，执行完毕！执行状态：{}", JSON.toJSONString(flowSqlExecute), JSON.toJSONString(executeStruct.getRunStatus()));
		return runResult;
	}
	
}