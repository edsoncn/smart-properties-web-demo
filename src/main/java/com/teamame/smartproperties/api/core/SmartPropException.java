package com.teamame.smartproperties.api.core;

import java.text.MessageFormat;

public class SmartPropException extends Exception{

	private static final long serialVersionUID = -3440977392872020546L;

    public enum ErrorCodes {
        ERROR_001("Compile Error: {0}"),
        ERROR_101("Compile Error Parameter {0} not found"),
        ERROR_201("Compile Error The condition isn't a boolean expression"),
        ERROR_301("Compile Error Inconsistent types in expression"),
        ERROR_302("Compile Error Parameter type incorrect: {0}"),
        ERROR_401("Compile Error Properties value wasn't returned in program"),
        ERROR_402("Compile Error Properties class {0} doesn't match with returned type of program"),
        ERROR_501("Compile Error Function parameter {0} has wrong type. Expect {1}, received {2}"),
        ERROR_502("Compile Error Function {0} not found"),
        ERROR_503("Compile Error Function wrong parameter size. Expect {0}, received {1}"),
        ERROR_504("Compile Error Function missing return value. Function name: {0}"),
        ERROR_505("Compile Error Function array index is out of. Expect index range [0...{0}], received {1}");

        private String message;

        ErrorCodes(String message){
            this.message = message;
        }

        public String getMessage() {
            return message;
        }

    }

	public SmartPropException(ErrorCodes errorCode, Object... args){
        super(args != null && args.length > 0 ? MessageFormat.format(errorCode.getMessage(), args) : errorCode.getMessage());
    }


}
