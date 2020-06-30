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
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.mcg.common.SpringContextHelper;
import com.mcg.common.sysenum.EletypeEnum;
import com.mcg.common.sysenum.LogOutTypeEnum;
import com.mcg.common.sysenum.LogTypeEnum;
import com.mcg.common.sysenum.MessageTypeEnum;
import com.mcg.entity.flow.sqlquery.FlowSqlQuery;
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

public class FlowSqlQueryStrategy implements ProcessStrategy {

	private static Logger logger = LoggerFactory.getLogger(FlowSqlQueryStrategy.class);
	
	@Override
	public void prepare(ArrayList<String> sequence, McgProduct mcgProduct, ExecuteStruct executeStruct) throws Exception {
	    FlowSqlQuery flowSqlQuery = (FlowSqlQuery)mcgProduct;
		executeStruct.getRunStatus().setExecuteId(flowSqlQuery.getId());
	}

	@Override
	public RunResult run(McgProduct mcgProduct, ExecuteStruct executeStruct) throws Exception {
		
	    FlowSqlQuery flowSqlQuery = (FlowSqlQuery)mcgProduct;
		JSON parentParam = DataConverter.getParentRunResult(flowSqlQuery.getId(), executeStruct);
		
        Message message = MessagePlugin.getMessage();
        message.getHeader().setMesType(MessageTypeEnum.FLOW);		
        FlowBody flowBody = new FlowBody();
        flowBody.setFlowId(flowSqlQuery.getFlowId());
        flowBody.setSubFlag(executeStruct.getSubFlag());
        flowBody.setOrderNum(flowSqlQuery.getOrderNum());
        flowBody.setLogOutType(LogOutTypeEnum.PARAM.getValue());
        flowBody.setEleType(EletypeEnum.SQLQUERY.getValue());
        flowBody.setEleTypeDesc(EletypeEnum.SQLQUERY.getName() + "--》" + flowSqlQuery.getSqlQueryProperty().getName());
        flowBody.setEleId(flowSqlQuery.getId());
        flowBody.setComment("参数");
        
        if(parentParam == null) {
        	flowBody.setContent("{}");
        } else {
        	flowBody.setContent(JSON.toJSONString(parentParam, true));
        }
        flowBody.setLogType(LogTypeEnum.INFO.getValue());
        flowBody.setLogTypeDesc(LogTypeEnum.INFO.getName());
        message.setBody(flowBody);
        MessagePlugin.push(flowSqlQuery.getMcgWebScoketCode(), executeStruct.getSession().getId(), message);		
		
        flowSqlQuery = DataConverter.flowOjbectRepalceGlobal(DataConverter.addFlowStartRunResult(parentParam, executeStruct), flowSqlQuery);		
		RunResult runResult = new RunResult();
		runResult.setElementId(flowSqlQuery.getId());
		
		FlowService flowService = SpringContextHelper.getSpringBean(FlowService.class);
        McgBizAdapter mcgBizAdapter = new FlowDataAdapterImpl(flowService.getMcgDataSourceById(flowSqlQuery.getSqlQueryCore().getDataSourceId()));
        
        Message sqlMessage = MessagePlugin.getMessage();
        sqlMessage.getHeader().setMesType(MessageTypeEnum.FLOW);		
        FlowBody sqlFlowBody = new FlowBody();
        sqlFlowBody.setFlowId(flowSqlQuery.getFlowId());
        sqlFlowBody.setSubFlag(executeStruct.getSubFlag());
        sqlFlowBody.setEleType(EletypeEnum.SQLQUERY.getValue());
        sqlFlowBody.setEleTypeDesc(EletypeEnum.SQLQUERY.getName() + "--》" + flowSqlQuery.getSqlQueryProperty().getName());
        sqlFlowBody.setEleId(flowSqlQuery.getId());
        sqlFlowBody.setComment("查询SQL语句");
        sqlFlowBody.setContent(flowSqlQuery.getSqlQueryCore().getSource());
        sqlFlowBody.setLogType(LogTypeEnum.INFO.getValue());
        sqlFlowBody.setLogTypeDesc(LogTypeEnum.INFO.getName());
        sqlMessage.setBody(sqlFlowBody);
        MessagePlugin.push(flowSqlQuery.getMcgWebScoketCode(), executeStruct.getSession().getId(), sqlMessage);
        
        List<Map<String, Object>> result = null;
        result = mcgBizAdapter.tableQuery(flowSqlQuery.getSqlQueryCore().getSource(), null);
     
        JSONObject runResultJson = (JSONObject)parentParam;
        runResultJson.put(flowSqlQuery.getSqlQueryProperty().getKey(), result);
		runResult.setJsonVar(JSON.toJSONString(runResultJson, SerializerFeature.WriteDateUseDateFormat));
		executeStruct.getRunStatus().setCode("success");
		
		logger.debug("SQL查询控件：{}，执行完毕！执行状态：{}", JSON.toJSONString(flowSqlQuery), JSON.toJSONString(executeStruct.getRunStatus()));
		return runResult;
	}
	
}