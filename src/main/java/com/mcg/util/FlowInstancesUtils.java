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

package com.mcg.util;

import java.util.concurrent.ConcurrentHashMap;

import com.mcg.entity.generate.ExecuteStruct;

public class FlowInstancesUtils {

	/**
	 * 正在执行的所有流程实例
	 * key：流程实例id 
	 * value：流程执行的 
	 */
	public static ConcurrentHashMap<String, ExecuteStruct> executeStructMap = new ConcurrentHashMap<String, ExecuteStruct>();
}
