package com.teamame.smartproperties.api.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import org.json.JSONArray;
import org.json.JSONObject;

import com.teamame.smartproperties.api.SmartPropertiesApi;
import com.teamame.smartproperties.api.core.SmartPropException.ErrorCodes;

public class SmartPropertiesCompiler {

	private SmartPropertiesApi smartPropertiesApi;
	private StringBuilder console;
	private boolean defaultReturned;

	public SmartPropertiesCompiler(SmartPropertiesApi smartPropertiesApi){
		this.smartPropertiesApi = smartPropertiesApi;		
		defaultReturned = false;
		console = new StringBuilder();
	}
	
	public Value compile(Block program, List<Variable> variables, boolean skipDefault) throws SmartPropException {
        boolean existsDefaultValue = false;
        
        if(program.getDefaultValue() != null && program.getDefaultValue().isInitialized() && !skipDefault){
            existsDefaultValue = true;
        }
        if(program.getType() == null){
            if(existsDefaultValue) {
                defaultReturned = true;
                return program.getDefaultValue();
            } else {
                return null;
            }
        }
        
		Map<String, Variable> variablesMap = new HashMap<>();
		Variable returnVar = new Variable("");

		variables.stream().forEach(variable -> {
			variablesMap.put(variable.name, variable);
		});
		
		try {
			compileBlock(program, variablesMap, returnVar);
		} catch(SmartPropException e) {
			if(existsDefaultValue) {
				defaultReturned = true;
				return program.getDefaultValue();
			} else {
				throw e;
			}
		}
		
		variablesMap.values().stream().forEach(varMap -> {
			String name = varMap.getName();
			boolean varFound = false;
			
			for(Variable variable: variables) {
				if(variable.getName().equals(name)) {
					variable.setValue(varMap);
					varFound = true;
					break;
				}
			}
			
			if(!varFound) {
				variables.add(varMap);
			}
		});
		
		if(returnVar.isInitialized()) {
			return returnVar;
		} else if(existsDefaultValue) {
            defaultReturned = true;
            return program.getDefaultValue();
        } else {
            return null;
        }
	}

	private void compileBlock(Block block, Map<String, Variable> variables, Variable returnVar) throws SmartPropException {		
		if(block.getCondition() != null) {
			Value value = evaluateExpression(block.getCondition(), variables, true);
			if(value.isValueBoolean()){
				block.setConditionResult(value.getValueBoolean());
				if(!block.getConditionResult()) {
					return;
				}
			}else{
				throw new SmartPropException(ErrorCodes.ERROR_201);
			}
		}

		Queue<Instruction> q = new LinkedList<>();
		q.addAll(Arrays.asList(block.getInstructions()));
		
		Boolean lastIfCondition = null;
		while(!q.isEmpty()) {
			Instruction instruction = q.remove();
			if (instruction instanceof Assignment) {
				Assignment assignment = (Assignment) instruction;
				Variable variable = assignment.getVariable();
				Node expression = assignment.getExpression();

				Value value = evaluateExpression(expression, variables, false);
				variable.setValue(value);
				variables.put(variable.getName(), variable);
				lastIfCondition = null;
			} else if (instruction instanceof Block) {
				Block blockChild = (Block) instruction;
				
				if(lastIfCondition != null && lastIfCondition && blockChild.getName().contains("else")) {
					continue;
				}
				
				compileBlock(blockChild, variables, returnVar);

				if (returnVar.isInitialized()) {
					return;
				}
				
				lastIfCondition = null;
				if(blockChild.getName().contains("if")) {
					lastIfCondition = blockChild.getConditionResult();
				}
				
				if(blockChild.getName().equals("while") && blockChild.getConditionResult()) {
					((LinkedList<Instruction>)q).addFirst(blockChild);
				}				
			} else if(instruction instanceof Funct) {
				Funct funct = (Funct) instruction;

				lastIfCondition = null;
				evaluateFunction(funct, variables);
			}
		}				
		if (block.getReturnExpression() != null) {
			Value value = evaluateExpression(block.getReturnExpression(), variables, false);
			returnVar.setValue(value);
		}
	}

	private Node evaluateExpression(Node node, Map<String, Variable> variables, boolean isBooleanOperator) throws SmartPropException {
		if(!hasChildren(node)){
			if(node.isVariable()){
				if(variables.containsKey(node.getVariable())){
					Value varValue = variables.get(node.getVariable());
					
					if(!isBooleanOperator || varValue.isValueBoolean()){
						node.setValue(varValue);
					}else{
						node.setValueBoolean(true);
					}
				}else{
					if(!isBooleanOperator){
						throw new SmartPropException(ErrorCodes.ERROR_101, node.getVariable());
					}else{
						node.setValueBoolean(false);
					}
				}
			}else if(node.isFunction()){
				Funct funct = node.getFunct();
				Value value = evaluateFunction(funct, variables);
				
				if(value.isInitialized()){
					node.setValue(value);
				}else{
					throw new SmartPropException(ErrorCodes.ERROR_504, funct.getName());
				}
			}
		}else{
			operateNodeChildren(node, variables);
		}
		return node;
	}
	
	private Value evaluateFunction(Funct funct, Map<String, Variable> variables) throws SmartPropException {
		String name = funct.getName();
		List<Value> paramValues = new ArrayList<Value>();
		
		for(Node expression : funct.parameters) {
			Value value = evaluateExpression(expression, variables, false);
			paramValues.add(value);
		}
		
		return executeFunction(name, paramValues, variables);
	}
	
	private Value executeFunction(String name, List<Value> paramValues, Map<String, Variable> variables) throws SmartPropException {
		switch(name){
			case "call": return executeCallFunction(paramValues, variables);
			case "sqrt": return executeSquareRootFunction(paramValues);
			case "array": return executeArrayFunction(paramValues);
			case "get": return executeGetFunction(paramValues);
			case "length": return executeLengthFunction(paramValues);
			case "push": return executePushFunction(paramValues);
			case "pop": return executePopFunction(paramValues);
			case "unshift": return executeUnshiftFunction(paramValues);
			case "shift": return executeShiftFunction(paramValues);
			case "pushall": return executePushAllFunction(paramValues);
			case "print": return executePrintFunction(paramValues);
			default: throw new SmartPropException(ErrorCodes.ERROR_502, name);
		}
	}
	
	private Value executeCallFunction(List<Value> paramValues, Map<String, Variable> variables) throws SmartPropException {
		int funcParamsLength = 1;
		
		if(paramValues.size() != funcParamsLength){
			throw new SmartPropException(ErrorCodes.ERROR_503, funcParamsLength, paramValues.size());
		} else {
			Value param0 = paramValues.get(0);
			
			if(!param0.isValueString()) {
				throw new SmartPropException(ErrorCodes.ERROR_501, 1, "string", param0.getType());
			} else {
				String paramKey = param0.getValueString();
				Block resultBlock = smartPropertiesApi.getSmartPropertiesCodeMap().get(paramKey);
				Variable returnVar = new Variable("");
				
				compileBlock(resultBlock, variables, returnVar);
				return returnVar;
			}
		}
	}

	private Value executeSquareRootFunction(List<Value> paramValues) throws SmartPropException {
		int funcParamsLength = 1;
		
		if(paramValues.size() != funcParamsLength){
			throw new SmartPropException(ErrorCodes.ERROR_503, funcParamsLength, paramValues.size());
		} else {
			Value param0 = paramValues.get(0);
			
			if(!param0.isValueFloat() && !param0.isValueInt()){
				throw new SmartPropException(ErrorCodes.ERROR_501, 1, "int/float", param0.getType());
			} else {
				float value = param0.isValueFloat() ? param0.getValueFloat() : param0.getValueInt();
				Value squareValue = new Value();
				
				squareValue.setValueFloat((float)Math.sqrt(value));
				return squareValue;
			}
		}
	}

	private Value executeArrayFunction(List<Value> paramValues) {
		Value valueArray = new Value();
		valueArray.setValueArray(paramValues);
		
		return valueArray;
	}

	private Value executeGetFunction(List<Value> paramValues) throws SmartPropException {
		int funcParamsLength = 2;
		
		if(paramValues.size() != funcParamsLength){
			throw new SmartPropException(ErrorCodes.ERROR_503, funcParamsLength, paramValues.size());
		}else{
			Value param0Array = paramValues.get(0);
			Value param1Index = paramValues.get(1);
			
			if(!param0Array.isValueArray()){
				throw new SmartPropException(ErrorCodes.ERROR_501, 1, "array", param0Array.getType());
			}else if(!param1Index.isValueInt()){
				throw new SmartPropException(ErrorCodes.ERROR_501, 2, "int", param1Index.getType());
			}else{
				int index = param1Index.getValueInt();
				int arraySize = param0Array.getValueArray().size();
				
				if(index < 0 || index >= arraySize){
					throw new SmartPropException(ErrorCodes.ERROR_505, arraySize - 1, index);
				}else{
					Value gettedValue = new Value();
					gettedValue.setValue(param0Array.getValueArray().get(index));
					
					return gettedValue;
				}
			}
		}
	}
	
	private Value executeLengthFunction(List<Value> paramValues) throws SmartPropException {
		int funcParamsLength = 1;
		
		if(paramValues.size() != funcParamsLength){
			throw new SmartPropException(ErrorCodes.ERROR_503, funcParamsLength, paramValues.size());
		}else{
			Value param0Array = paramValues.get(0);
			
			if(!param0Array.isValueArray()){
				throw new SmartPropException(ErrorCodes.ERROR_501, 1, "array", param0Array.getType());
			}else{
				Value lengthValue = new Value();
				lengthValue.setValueInt(param0Array.getValueArray().size());
				
				return lengthValue;
			}
		}
	}
	
	private Value executePushFunction(List<Value> paramValues) throws SmartPropException {
		int funcParamsLength = 2;
		
		if(paramValues.size() != funcParamsLength){
			throw new SmartPropException(ErrorCodes.ERROR_503, funcParamsLength, paramValues.size());
		}else{
			Value param0Array = paramValues.get(0);
			Value param1Value = paramValues.get(1);
			
			if(!param0Array.isValueArray()){
				throw new SmartPropException(ErrorCodes.ERROR_501, 1, "array", param0Array.getType());
			}else{
				param0Array.getValueArray().add(param1Value);
				return new Value();
			}
		}
	}
	
	private Value executePopFunction(List<Value> paramValues) throws SmartPropException {
		int funcParamsLength = 1;
		
		if(paramValues.size() != funcParamsLength){
			throw new SmartPropException(ErrorCodes.ERROR_503, funcParamsLength, paramValues.size());
		}else{
			Value param0Array = paramValues.get(0);
			
			if(!param0Array.isValueArray()){
				throw new SmartPropException(ErrorCodes.ERROR_501, 1, "array", param0Array.getType());
			}else{
				List<Value> valueArray = param0Array.getValueArray();
				
				if(valueArray.size() > 0){
					return valueArray.remove(valueArray.size() - 1);
				}else{
					return new Value();
				}
			}
		}
	}
	
	private Value executeUnshiftFunction(List<Value> paramValues) throws SmartPropException {
		int funcParamsLength = 2;
		
		if(paramValues.size() != funcParamsLength){
			throw new SmartPropException(ErrorCodes.ERROR_503, funcParamsLength, paramValues.size());
		}else{
			Value param0Array = paramValues.get(0);
			Value param1Value = paramValues.get(1);
			
			if(!param0Array.isValueArray()){
				throw new SmartPropException(ErrorCodes.ERROR_501, 1, "array", param0Array.getType());
			}else{
				param0Array.getValueArray().add(0, param1Value);
				return new Value();
			}
		}
	}
	
	private Value executeShiftFunction(List<Value> paramValues) throws SmartPropException {
		int funcParamsLength = 1;
		
		if(paramValues.size() != funcParamsLength){
			throw new SmartPropException(ErrorCodes.ERROR_503, funcParamsLength, paramValues.size());
		}else{
			Value param0Array = paramValues.get(0);
			
			if(!param0Array.isValueArray()){
				throw new SmartPropException(ErrorCodes.ERROR_501, 1, "array", param0Array.getType());
			}else{
				List<Value> valueArray = param0Array.getValueArray();
				
				if(valueArray.size() > 0){
					return valueArray.remove(0);
				}else{
					return new Value();
				}
			}
		}
	}
	
	private Value executePushAllFunction(List<Value> paramValues) throws SmartPropException {
		int funcParamsLength = 2;

		if(paramValues.size() != funcParamsLength){
			throw new SmartPropException(ErrorCodes.ERROR_503, funcParamsLength, paramValues.size());
		}else{
			Value param0Array = paramValues.get(0);
			Value param1Array = paramValues.get(1);
			
			if(!param0Array.isValueArray()){
				throw new SmartPropException(ErrorCodes.ERROR_501, 1, "array", param0Array.getType());
			}else if(!param1Array.isValueArray()){
				throw new SmartPropException(ErrorCodes.ERROR_501, 2, "array", param1Array.getType());
			}else{
				param0Array.getValueArray().addAll(param1Array.getValueArray());
				return new Value();
			}
		}
	}

	private Value executePrintFunction(List<Value> paramValues) throws SmartPropException {
		int funcParamsMaxLength = 1;
		
		if(paramValues.size() > funcParamsMaxLength){
			throw new SmartPropException(ErrorCodes.ERROR_503, funcParamsMaxLength, paramValues.size());
		}else{
			if(paramValues.size() == 1){
				this.console.append(paramValues.get(0).toString());
				
			}
			this.console.append("\n");
			return new Value();
		}
	}
	
	private void operateNodeChildren(Node node, Map<String, Variable> variables) throws SmartPropException {
		String operator = node.getOperator();

		Node nodeRight = node.getRight();
		Node nodeLeft = node.getLeft();

		Node nodeChildA = evaluateExpression(nodeRight, variables, isBooleanOperator(operator));

		// evaluate the right result for conditional to not evaluate left
		if(this.isBooleanOperator(operator) && nodeChildA.isValueBoolean()){
			if(operator.equals("&") && !nodeChildA.getValueBoolean()){
				node.setValueBoolean(false);
				return;
			}else if(operator.equals("|") && nodeChildA.getValueBoolean()){
				node.setValueBoolean(true);
				return;
			}
		}

		Node nodeChildB = evaluateExpression(nodeLeft, variables, isBooleanOperator(operator));

		if(nodeChildA.isValueString() && nodeChildB.isValueString() && (isStringOperator(operator) || isCompareOperator(operator))){
			if(isCompareOperator(operator)) {
				node.setValueBoolean(compareStrings(operator, nodeChildA.getValueString(), nodeChildB.getValueString()));
			}else{
				node.setValueString(nodeChildA.getValueString() + nodeChildB.getValueString());
			}
		} else if(((nodeChildA.isValueString() && !nodeChildB.isValueString()) || (!nodeChildA.isValueString() && nodeChildB.isValueString()))
				&& isStringOperator(operator)) {
			node.setValueString(nodeChildA.toString() + nodeChildB.toString());
		}else if(nodeChildA.isValueInt() && nodeChildB.isValueInt() && (isNumberOperator(operator) || isCompareOperator(operator))){
			if(isCompareOperator(operator)) {
				node.setValueBoolean(compareInt(operator, nodeChildA.getValueInt(), nodeChildB.getValueInt()));
			}else {
				node.setValueInt(operateInt(operator, nodeChildA.getValueInt(), nodeChildB.getValueInt()));
			}
		}else if(nodeChildA.isValueFloat() && nodeChildB.isValueFloat() && (isNumberOperator(operator) || isCompareOperator(operator))){
			if(isCompareOperator(operator)) {
				node.setValueBoolean(compareFloat(operator, nodeChildA.getValueFloat(), nodeChildB.getValueFloat()));
			}else {
				node.setValueFloat(operateFloat(operator, nodeChildA.getValueFloat(), nodeChildB.getValueFloat()));
			}
		}else if(nodeChildA.isValueInt() && nodeChildB.isValueFloat() && (isNumberOperator(operator) || isCompareOperator(operator))){
			if(isCompareOperator(operator)) {
				node.setValueBoolean(compareFloat(operator, (float)nodeChildA.getValueInt(), nodeChildB.getValueFloat()));
			}else {
				node.setValueFloat(operateFloat(operator, (float)nodeChildA.getValueInt(), nodeChildB.getValueFloat()));
			}
		}else if(nodeChildA.isValueFloat() && nodeChildB.isValueInt() && (isNumberOperator(operator) || isCompareOperator(operator))){
			if(isCompareOperator(operator)) {
				node.setValueBoolean(compareFloat(operator, nodeChildA.getValueFloat(), (float)nodeChildB.getValueInt()));
			}else {
				node.setValueFloat(operateFloat(operator, nodeChildA.getValueFloat(), (float)nodeChildB.getValueInt()));
			}
		}else if(nodeChildA.isValueBoolean() && nodeChildB.isValueBoolean() && (isBooleanOperator(operator) || isEqualsOperator(operator))){
			node.setValueBoolean(operateBoolean(operator, nodeChildA.getValueBoolean(), nodeChildB.getValueBoolean()));
		}else{
			throw new SmartPropException(ErrorCodes.ERROR_301);
		}
	}

	private boolean isBooleanOperator(String operator) {
		return "&|!".contains(operator.toUpperCase());
	}

	private boolean isNumberOperator(String operator) {
		return "+-*/".contains(operator);
	}

	private boolean isStringOperator(String operator) {
		return "+".equals(operator);
	}

	private boolean isCompareOperator(String operator) {
		return "<=>=".contains(operator);
	}

	private boolean isEqualsOperator(String operator) {
		return "=".equals(operator);
	}

	private boolean compareStrings(String operator, String a, String b){
		switch (operator){
			case "=" : return a.equals(b);
			case "<" : return a.compareTo(b) < 0;
			case ">" : return a.compareTo(b) > 0;
			case "<=" : return a.compareTo(b) <= 0;
			case ">=" : return a.compareTo(b) >= 0;
		}
		return false;
	}

	private Integer operateInt(String operator, Integer a, Integer b){
		switch (operator){
			case "+" : return a + b;
			case "-" : return a - b;
			case "*" : return a * b;
			case "/" : return a / b;
		}
		return 0;
	}

	private boolean compareInt(String operator, Integer a, Integer b){
		switch (operator){
			case "=" : return a == b;
			case "<" : return a < b;
			case ">" : return a > b;
			case "<=" : return a <= b;
			case ">=" : return a >= b;
		}
		return false;
	}

	private Float operateFloat(String operator, Float a, Float b){
		switch (operator){
			case "+" : return a + b;
			case "-" : return a - b;
			case "*" : return a * b;
			case "/" : return a / b;
		}
		return 0.0f;
	}

	private boolean compareFloat(String operator, Float a, Float b){
		switch (operator){
			case "=" : return a == b;
			case "<" : return a < b;
			case ">" : return a > b;
			case "<=" : return a <= b;
			case ">=" : return a >= b;
		}
		return false;
	}

	private Boolean operateBoolean(String operator, Boolean a, Boolean b){
		switch (operator.toUpperCase()){
			case "&"  : return a && b;
			case "|"  : return a || b;
			case "!"  : return !b;
			case "="  : return a == b;
		}
		return false;
	}

	private boolean hasChildren(Node node){
		return node.getLeft() != null && node.getRight() != null;
	}
	
	public String getConsole(){
		return console.toString();
	}
	
	public boolean wasDefaultReturned() {
		return defaultReturned;
	}

	public static Block parseBlock(JSONObject blockJson){
		Block block = new Block();

		if(blockJson.has("name")) block.setName(blockJson.getString("name"));
		if(blockJson.has("type")) block.setType(blockJson.getString("type"));
		if(blockJson.has("condition")) block.setCondition(parseExpression(blockJson.getJSONObject("condition")));
		if(blockJson.has("return")) block.setReturnExpression(parseExpression(blockJson.getJSONObject("return")));
		if(blockJson.has("defaultValue")) block.setDefaultValue(parseValue(blockJson.getJSONObject("defaultValue")));;

		if(blockJson.has("instructions")) {
			JSONArray instructionsArray = blockJson.getJSONArray("instructions");
			Instruction[] instructions = new Instruction[instructionsArray.length()];
	
			for(int i=0; i < instructionsArray.length(); i++){
				JSONObject instructionJson = (JSONObject) instructionsArray.get(i);
				String type = instructionJson.getString("type");
	
				if("block".equals(type)){
					instructions[i] = parseBlock(instructionJson);
				} else if("assignment".equals(type)){
					instructions[i] = parseAssignment(instructionJson);
				}else if("funct".equals(type)){
					instructions[i] = parseFunct(instructionJson);
				}
			}
			block.setInstructions(instructions);
		}
		
		return block;
	}

	public static Assignment parseAssignment(JSONObject assignmentJson) {
		JSONObject variableJson = assignmentJson.getJSONObject("variable");
		JSONObject expressionJson = assignmentJson.getJSONObject("expression");
		Variable variable = new Variable(variableJson.getString("name"));
		Assignment assignment = new Assignment(variable);

		assignment.setType(assignmentJson.getString("type"));
		assignment.setExpression(parseExpression(expressionJson));
		return assignment;
	}

	public static Funct parseFunct(JSONObject functJson) {
		JSONArray parametersArray = functJson.getJSONArray("parameters");
		Funct funct = new Funct(functJson.getString("name"));
		Node[] nodes = new Node[parametersArray.length()];

		funct.setType(functJson.getString("type"));
		for(int i = 0; i < parametersArray.length(); i++){
			JSONObject parameterJson = (JSONObject)parametersArray.get(i);
			nodes[i] = parseExpression(parameterJson);
		}
		funct.setParameters(nodes);

		return funct;
	}

	public static Node parseExpression(JSONObject nodeJson){
		Node node = new Node();

		parseValue(node, nodeJson);
		if(nodeJson.has("left")) node.setLeft(parseExpression(nodeJson.getJSONObject("left")));
		if(nodeJson.has("right")) node.setRight(parseExpression(nodeJson.getJSONObject("right")));
		if(nodeJson.has("funct")) node.setFunct(parseFunct(nodeJson.getJSONObject("funct")));

		return node;
	}
	
	public static Value parseValue(JSONObject valueJson){
		Value value = new Value();
		parseValue(value, valueJson);
		return value;
	}
	
	private static void parseValue(Value value, JSONObject valueJson) {
		if(valueJson.has("variable")) value.setVariable(valueJson.getString("variable"));
		if(valueJson.has("operator")) value.setOperator(valueJson.getString("operator"));
		if(valueJson.has("valueString")) value.setValueString(valueJson.getString("valueString"));
		if(valueJson.has("valueInt")) value.setValueInt(valueJson.getInt("valueInt"));
		if(valueJson.has("valueFloat")) value.setValueFloat(valueJson.getFloat("valueFloat"));
		if(valueJson.has("valueBoolean")) value.setValueBoolean(valueJson.getBoolean("valueBoolean"));
	}

	public static class Instruction {

		private String type;

		public String getType() {
			return type;
		}

		public void setType(String type) {
			this.type = type;
		}

	}

	public static class Block extends Instruction {

		private String name;
		private Node condition;
		private Boolean conditionResult;
		private Instruction[] instructions;
		private Node returnExpression;
		private Value defaultValue;

		public Block(){
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Node getCondition() {
			return condition;
		}

		public void setCondition(Node condition) {
			this.condition = condition;
		}

		public Instruction[] getInstructions() {
			return instructions;
		}

		public void setInstructions(Instruction[] instructions) {
			this.instructions = instructions;
		}

		public Node getReturnExpression() {
			return returnExpression;
		}

		public void setReturnExpression(Node returnExpression) {
			this.returnExpression = returnExpression;
		}

		public Boolean getConditionResult() {
			return conditionResult;
		}

		public void setConditionResult(Boolean conditionResult) {
			this.conditionResult = conditionResult;
		}

		public Value getDefaultValue() {
			return defaultValue;
		}

		public void setDefaultValue(Value defaultValue) {
			this.defaultValue = defaultValue;
		}

	}

	public static class Assignment extends Instruction {

		private Variable variable;
		private Node expression;

		public Assignment(Variable variable){
			this.variable = variable;
		}

		public Variable getVariable() {
			return variable;
		}

		public void setVariable(Variable variable) {
			this.variable = variable;
		}

		public Node getExpression() {
			return expression;
		}

		public void setExpression(Node expression) {
			this.expression = expression;
		}
	}
	
	public static class Funct extends Instruction {
		
		private String name;
		private Node[] parameters;

		public Funct(String name){
			this.name = name;
		}
		
		public String getName() {
			return name;
		}
		
		public void setName(String name) {
			this.name = name;
		}
		
		public Node[] getParameters() {
			return parameters;
		}
		
		public void setParameters(Node[] parameters) {
			this.parameters = parameters;
		}
		
	}

	public static class Value {

		private String variable;
		private String operator;
		private String valueString;
		private Integer valueInt;
		private Float valueFloat;
		private Boolean valueBoolean;
		private List<Value> valueArray;

		private boolean boolInitialized;
		private boolean boolVariable;
		private boolean boolOperator;
		private boolean boolValueString;
		private boolean boolValueInt;
		private boolean boolValueFloat;
		private boolean boolValueBoolean;
		private boolean boolValueArray;

		public void setValue(Value value){
			if(value.isValueString()){
				setValueString(value.getValueString());
			}else if(value.isValueInt()){
				setValueInt(value.getValueInt());
			}else if(value.isValueFloat()){
				setValueFloat(value.getValueFloat());
			}else if(value.isValueBoolean()){
				setValueBoolean(value.getValueBoolean());
			}else if(value.isValueArray()){
				this.setValueArray(value.getValueArray());
			}
		}

		public String getVariable() {
			return variable;
		}

		public void setVariable(String variable) {
			this.variable = variable;
			this.boolVariable = true;
		}

		public String getOperator() {
			return operator;
		}

		public void setOperator(String operator) {
			this.operator = operator;
			this.boolOperator = true;
		}

		public String getValueString() {
			return valueString;
		}

		public void setValueString(String valueString) {
			this.valueString = valueString;
			this.boolInitialized = true;
			this.boolValueString = true;

			valueInt = null;
			valueFloat = null;
			valueBoolean = null;
			valueArray = null;

			boolValueInt = false;
			boolValueFloat = false;
			boolValueBoolean = false;
			boolValueArray = false;
		}

		public Integer getValueInt() {
			return valueInt;
		}

		public void setValueInt(Integer valueInt) {
			this.valueInt = valueInt;
			this.boolInitialized = true;
			this.boolValueInt = true;

			valueString = null;
			valueFloat = null;
			valueBoolean = null;
			valueArray = null;

			boolValueString = false;
			boolValueFloat = false;
			boolValueBoolean = false;
			boolValueArray = false;
		}

		public Float getValueFloat() {
			return valueFloat;
		}

		public void setValueFloat(Float valueFloat) {
			this.valueFloat = valueFloat;
			this.boolInitialized = true;
			this.boolValueFloat = true;

			valueString = null;
			valueInt = null;
			valueBoolean = null;
			valueArray = null;

			boolValueString = false;
			boolValueInt = false;
			boolValueBoolean = false;
			boolValueArray = false;
		}

		public Boolean getValueBoolean() {
			return valueBoolean;
		}

		public void setValueBoolean(Boolean valueBoolean) {
			this.valueBoolean = valueBoolean;
			this.boolInitialized = true;
			this.boolValueBoolean = true;

			valueString = null;
			valueInt = null;
			valueFloat = null;
			valueArray = null;

			boolValueString = false;
			boolValueInt = false;
			boolValueFloat = false;
			boolValueArray = false;
		}

		public List<Value> getValueArray() {
			return valueArray;
		}
		
		public void setValueArray(List<Value> valueArray) {
			this.valueArray = valueArray;
			boolInitialized = true;
			boolValueArray = true;
			
			valueString = null;
			valueInt = null;
			valueFloat = null;
			valueBoolean = null;
			
			boolValueString = false;
			boolValueInt = false;
			boolValueFloat = false;
			boolValueBoolean = false;
		}

		public boolean isInitialized() {
			return boolInitialized;
		}

		public boolean isVariable() {
			return boolVariable;
		}

		public boolean isOperator() {
			return boolOperator;
		}

		public boolean isValueString() {
			return boolValueString;
		}

		public boolean isValueInt() {
			return boolValueInt;
		}

		public boolean isValueFloat() {
			return boolValueFloat;
		}

		public boolean isValueBoolean() {
			return boolValueBoolean;
		}

		public boolean isValueArray() {
			return this.boolValueArray;
		}

		@Override
		public String toString(){
			if (boolValueBoolean){
				return valueBoolean.toString();
			} else if (boolValueInt){
				return valueInt.toString();
			} else if (boolValueFloat) {
				return valueFloat.toString();
			} else if (boolValueString) {
				return valueString;
			} else if (boolValueArray) {
				return Arrays.toString(valueArray.toArray());
			}
			return "";
		}

		public String getType(){
			if (boolValueBoolean){
				return "boolean";
			} else if (boolValueInt){
				return "int";
			} else if (boolValueFloat) {
				return "float";
			} else if (boolValueString) {
				return "string";
			} else if (boolValueArray) {
				return "array";
			}
			return "";
		}

	}

	public static class Node extends Value {

		private Node left;
		private Node right;
		private Funct funct;

		public Node getLeft() {
			return left;
		}

		public void setLeft(Node left) {
			this.left = left;
		}

		public Node getRight() {
			return right;
		}

		public void setRight(Node right) {
			this.right = right;
		}

		@Override
		public String toString(){
			return super.toString();
		}

		public Funct getFunct() {
			return funct;
		}

		public void setFunct(Funct funct) {
			this.funct = funct;
		}

		public boolean isFunction(){
			return this.funct != null;
		}

	}

	public static class Variable extends Value {

		private String name;

		public Variable(String name){
			super();
			this.name = name;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

	}

}
