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
import com.mcg.common.sysenum.EletypeEnum;
import com.mcg.common.sysenum.LogOutTypeEnum;
import com.mcg.common.sysenum.LogTypeEnum;
import com.mcg.common.sysenum.MessageTypeEnum;
import com.mcg.entity.flow.json.FlowJson;
import com.mcg.entity.generate.ExecuteStruct;
import com.mcg.entity.generate.RunResult;
import com.mcg.entity.message.FlowBody;
import com.mcg.entity.message.Message;
import com.mcg.plugin.build.McgProduct;
import com.mcg.plugin.execute.ProcessStrategy;
import com.mcg.plugin.websocket.MessagePlugin;
import com.mcg.util.DataConverter;

public class FlowJsonStrategy implements ProcessStrategy {

	private static Logger logger = LoggerFactory.getLogger(FlowJsonStrategy.class);
	
	@Override
	public void prepare(ArrayList<String> sequence, McgProduct mcgProduct, ExecuteStruct executeStruct) throws Exception {
		FlowJson flowJson = (FlowJson)mcgProduct;
		executeStruct.getRunStatus().setExecuteId(flowJson.getId());
	}

	@Override
	public RunResult run(McgProduct mcgProduct, ExecuteStruct executeStruct) throws Exception {
		
		FlowJson flowJson = (FlowJson)mcgProduct;
		JSON parentParam = DataConverter.getParentRunResult(flowJson.getId(), executeStruct);
        Message message = MessagePlugin.getMessage();
        message.getHeader().setMesType(MessageTypeEnum.FLOW);		
        FlowBody flowBody = new FlowBody();
        flowBody.setSubFlag(executeStruct.getSubFlag());
        flowBody.setFlowId(flowJson.getFlowId());
        flowBody.setOrderNum(flowJson.getOrderNum());
        flowBody.setLogOutType(LogOutTypeEnum.PARAM.getValue());
        flowBody.setEleType(EletypeEnum.JSON.getValue());
        flowBody.setEleTypeDesc(EletypeEnum.JSON.getName() + "--》" + flowJson.getJsonProperty().getName());
        flowBody.setEleId(flowJson.getId());
        flowBody.setComment("参数");
        if(parentParam == null) {
        	flowBody.setContent("{}");
        } else {
        	flowBody.setContent(JSON.toJSONString(parentParam, true));
        }
        flowBody.setLogType(LogTypeEnum.INFO.getValue());
        flowBody.setLogTypeDesc(LogTypeEnum.INFO.getName());
        message.setBody(flowBody);
        MessagePlugin.push(flowJson.getMcgWebScoketCode(), executeStruct.getSession().getId(), message);		
		
		flowJson = DataConverter.flowOjbectRepalceGlobal(DataConverter.addFlowStartRunResult(parentParam, executeStruct), flowJson);		
		RunResult runResult = new RunResult();
		runResult.setElementId(flowJson.getId());
		
		JSONObject runResultJson = (JSONObject)parentParam;
		runResultJson.put(flowJson.getJsonProperty().getKey(), JSON.parse(flowJson.getJsonCore().getSource()));
		runResult.setJsonVar(JSON.toJSONString(runResultJson, true));
		executeStruct.getRunStatus().setCode("success");

		logger.debug("JSON控件：{}，执行完毕！执行状态：{}", JSON.toJSONString(flowJson), JSON.toJSONString(executeStruct.getRunStatus()));
		return runResult;
	}
	
}
