package com.teamame.smartproperties.api;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import com.teamame.smartproperties.api.core.SmartPropException;
import com.teamame.smartproperties.api.core.SmartPropException.ErrorCodes;
import com.teamame.smartproperties.api.core.SmartPropertiesClient;
import com.teamame.smartproperties.api.core.SmartPropertiesCompiler;
import com.teamame.smartproperties.api.core.SmartPropertiesCompiler.Block;
import com.teamame.smartproperties.api.core.SmartPropertiesCompiler.Value;
import com.teamame.smartproperties.api.core.SmartPropertiesCompiler.Variable;

public class SmartPropertiesApi {

	private static final Logger logger = LoggerFactory.getLogger(SmartPropertiesApi.class);

	private Map<String, Block> smartPropertiesCodeMap;
	private boolean smartPropertiesCodeMapLoaded;
    private String apiUrl;
    private String tenant;
    private String workspace;
    private String apiToken;

    public SmartPropertiesApi(String apiUrl, String tenant, String workspace, String apiToken){
		smartPropertiesCodeMap = new HashMap<>();
		smartPropertiesCodeMapLoaded = false;
        this.apiUrl = apiUrl;
        this.tenant = tenant;
        this.workspace = workspace;
        this.apiToken = apiToken;
    }

    public void initialize(){
		loadSmartPropertiesCodeMap();
    }

    public <T> T compile(String key, Map<String, Object> parameters, boolean skipDefault, Class<T> returnClass) throws SmartPropException {
        logger.info("Starting compile key: " + key);

        Block program = getSmartPropertiesCodeBlockByKey(key);
        return compile(program, parameters, skipDefault, returnClass);
    }

    public void compile(String key, Map<String, Object> parameters, boolean skipDefault) throws SmartPropException {
        logger.info("Starting compile key: " + key);

        Block program = getSmartPropertiesCodeBlockByKey(key);
        compile(program, parameters, skipDefault);
    }

    public void compile(Block program, Map<String, Object> parameters, boolean skipDefault) throws SmartPropException {    	
    	compile(program, parameters, skipDefault, Void.class);
    }

    public <T> T compile(Block program, Map<String, Object> parameters, boolean skipDefault, Class<T> returnClass) throws SmartPropException {
    	List<Variable> variables = passParametersToVariablesList(parameters);
    	T result = compile(program, variables, skipDefault, returnClass);
    	
    	variables.stream().forEach(v -> parameters.put(v.getName(), getObject(v)));
    	
    	return result;
    }

    public <T> T compile(Block program, List<Variable> variables, boolean skipDefault, Class<T> returnClass) throws SmartPropException {
        return returnValue(compile(program, variables, skipDefault), returnClass);
    }

    public Value compile(Block program, List<Variable> variables, boolean skipDefault) throws SmartPropException {
    	SmartPropertiesCompiler smartPropCompiler = new SmartPropertiesCompiler(this);
        
    	Value result = smartPropCompiler.compile(program, variables, skipDefault);
    	String console = smartPropCompiler.getConsole();
    	
    	if(!console.isEmpty()) {
	    	System.out.println("Console: ");
	    	System.out.println(smartPropCompiler.getConsole());
    	}
    	if(smartPropCompiler.wasDefaultReturned()) {
	    	System.out.println("Default Value was returned!");
    	}
    	
        return result;
    }

    public static List<Variable> passParametersToVariablesList(Map<String, Object> parameters) throws SmartPropException {
    	List<Variable> variables = new ArrayList<>();
    	
        for(Map.Entry<String, Object> entry : parameters.entrySet()){
            String key = entry.getKey();
            Object value = entry.getValue();

            Variable variable = new Variable(key);
            if(value instanceof String){
                variable.setValueString((String) value);
            }else if(value instanceof Integer){
                variable.setValueInt((Integer) value);
            }else if(value instanceof Float){
                variable.setValueFloat((Float) value);
            }else if(value instanceof Boolean){
                variable.setValueBoolean((Boolean) value);
            }else{
                throw new SmartPropException(ErrorCodes.ERROR_302, key);
            }
            variables.add(variable);
        }
        
        return variables;
    }

    @SuppressWarnings("unchecked")
    private <T> T returnValue(Value returnVar, Class<T> returnClass) throws SmartPropException {
        if (returnVar == null){
        	if(returnClass.equals(Void.class)) {
        		return null;
        	} else {
                throw new SmartPropException(ErrorCodes.ERROR_401);
        	}
        } else if (returnClass.equals(String.class) && returnVar.isValueString()) {
            return (T) returnVar.getValueString();
        } else if (returnClass.equals(Integer.class) && returnVar.isValueInt()) {
            return (T) returnVar.getValueInt();
        } else if (returnClass.equals(Float.class) && returnVar.isValueFloat()) {
            return (T) returnVar.getValueFloat();
        } else if (returnClass.equals(Float.class) && returnVar.isValueInt()) {
            return (T) ((Float)(float) returnVar.getValueInt());
        } else if (returnClass.equals(Boolean.class) && returnVar.isValueBoolean()){
            return (T) returnVar.getValueBoolean();
        } else if (returnClass.equals((Class<List<Object>>)(Object)List.class) && returnVar.isValueArray()){
            return (T) getListObject(returnVar.getValueArray());
        } else {
            throw new SmartPropException(ErrorCodes.ERROR_402, returnClass.getName());
        }
    }
    
    private Object getObject(Value v) {
    	return v.isValueString() ? v.getValueString() : 
			v.isValueInt() ? v.getValueInt() :
			v.isValueFloat() ? v.getValueFloat() : 
			v.isValueInt() ? v.getValueInt() : 
			v.isValueBoolean() ? v.getValueBoolean() : 
			v.isValueArray() ? getListObject(v.getValueArray()): 
			null;
    }
    
    private List<Object> getListObject(List<Value> l) {
    	List<Object> result = new ArrayList<Object>();
    	l.stream().forEach(v -> {
    		result.add(getObject(v));
    	});
    	
    	return result;
    }

	public void loadSmartPropertiesCodeMap(){
		SmartPropertiesClient.Response result = 
                SmartPropertiesClient.callSmartPropertiesCodesService(this.apiUrl + "/" + this.tenant + "/smart-property-code/" + this.workspace, this.apiToken, null);

		if(result.getResponseCode() == 200) {
			JSONArray smartPropertiesCode = new JSONArray(result.getResponseString());

			for(int i = 0; i < smartPropertiesCode.length(); i++){
				JSONObject smartPropertyCode = (JSONObject) smartPropertiesCode.get(i);
				System.out.println(" > key: " + smartPropertyCode.getString("key"));
				System.out.println(" > code: " + smartPropertyCode.getString("code"));
				getSmartPropertiesCodeMap().put(
                    smartPropertyCode.getString("key"), 
                    SmartPropertiesCompiler.parseBlock(new JSONObject(smartPropertyCode.getString("code"))));
			}
		}
		smartPropertiesCodeMapLoaded = true;
	}

	public boolean isSmartPropertiesCodeMapLoaded(){
		return this.smartPropertiesCodeMapLoaded;
	}

	public Block getSmartPropertiesCodeBlockByKey(String key){
		return getSmartPropertiesCodeMap().get(key);
	}

	public void resetSmartPropertiesCodeMap(){
		loadSmartPropertiesCodeMap();
	}

	public Map<String, Block> getSmartPropertiesCodeMap() {
		return smartPropertiesCodeMap;
	}

}
