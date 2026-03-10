package com.teamame.smartproperties.web.demo.controller;

import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import com.teamame.smartproperties.api.SmartPropertiesApi;
import com.teamame.smartproperties.api.core.SmartPropException;

@CrossOrigin
@Controller
public class WebAppController {

	private static Map<String, Object> VARIABLES = new HashMap<>();
    private SmartPropertiesApi smartPropApi;
    private String appMode;

    public static float SHIPPING_COST = 10.01f;
    public static Map<String, String> PAYMENT_METHODS = new HashMap<>();
    public static Map<String, String> STATUS = new HashMap<>();

    static {
    	// Variables Map
    	VARIABLES.put("name", "Marissa");
    	VARIABLES.put("day", 28);
    	VARIABLES.put("month", 7);
    	VARIABLES.put("dayBirth", 28);
    	VARIABLES.put("monthBirth", 7);
    	
    	VARIABLES.put("rol", "guest");
    	VARIABLES.put("hasHistoricalOrders", false);
    	VARIABLES.put("creditClient", false);

    	VARIABLES.put("orderNumber", 152002);
    	VARIABLES.put("status", "M");
    	VARIABLES.put("totalAmount", 900);
    	VARIABLES.put("paymentMethod", "creditCard");
    	VARIABLES.put("lastCouponWinDays", 20);
    	
    	// Payment Method Map
        PAYMENT_METHODS.put("smartShopCredit", "Smart Shop Credit");
        PAYMENT_METHODS.put("creditCard", "Credit Card");
        PAYMENT_METHODS.put("paypal", "PayPal");
        PAYMENT_METHODS.put("payWithLink", "Pay By Link");
        PAYMENT_METHODS.put("billMeLater", "Bill me Later");
        
        STATUS.put("C", "Completed");
        STATUS.put("M", "Pending");
        STATUS.put("X", "Canceled");              
    }

    @Autowired
    public WebAppController(Environment environment, SmartPropertiesApi smartPropApi){
        appMode = environment.getProperty("app-mode");
        this.smartPropApi = smartPropApi;
    }

    @RequestMapping("/")
    public String index(Model model) throws SmartPropException {
        System.out.println("Index page accessed. Compiling smart properties...");

        // Header Message
        Map<String, Object> params = getMapVariables("name", "day", "month", "dayBirth", "monthBirth");
        model.addAttribute("message", smartPropApi.<String>compile("user.header.message", params, false, String.class));

        // Menu Options
        Map<String, Object> params2 = getMapVariables("rol", "hasHistoricalOrders", "creditClient");
        model.addAttribute("options", smartPropApi.<String>compile("user.menu.options", params2, false, String.class));

        String key = "fibonacci";
        Map<String, Object> params3 = getMapVariables();
        model.addAttribute("fibonacci", smartPropApi.<String>compile(key, params3, false, String.class));

        for (Entry<String,Object> entrySet : VARIABLES.entrySet()) {
            model.addAttribute(entrySet.getKey(), entrySet.getValue());
        }
        model.addAttribute("mode", appMode);

        return "index";
    }

    @RequestMapping("/my-account")
    public String account(Model model) throws SmartPropException {
        index(model);
        return "account";
    }

    @SuppressWarnings("unchecked")
	@RequestMapping("/checkout")
    public String checkout(Model model
    ) throws SmartPropException {
        index(model);

        int totalAmount = (int)VARIABLES.get("totalAmount");
        
        String key = "order.payment.methods";
        Map<String, Object> params = getMapVariables("rol", "creditClient", "totalAmount");
        model.addAttribute("paymentMethods", smartPropApi.<List<Object>>compile(key, params, false, (Class<List<Object>>)(Object)List.class));

        model.addAttribute("totalProduct", totalAmount - SHIPPING_COST);
        model.addAttribute("shippingCost", SHIPPING_COST);
        model.addAttribute("totalAmount", totalAmount);
        model.addAttribute("paymentMethodMap", PAYMENT_METHODS);

        return "checkout";
    }

    @RequestMapping("/order-completed")
    public String orderCompleted(Model model) throws SmartPropException {
        index(model);
        
        System.out.println("Order Completed page accessed. Compiling smart properties...");

        // Header Message
        int totalAmount = (int)VARIABLES.get("totalAmount");
        String paymentMethod = (String)VARIABLES.get("paymentMethod");
        
        String key = "order.completed.discount";
        Map<String, Object> params = getMapVariables("paymentMethod", "lastCouponWinDays", "totalAmount");
        String discount = smartPropApi.<String>compile(key, params, false, String.class);

        key = "order.completed.message";
        Map<String, Object> params2 = getMapVariables("name", "orderNumber", "paymentMethod", "status");
        smartPropApi.compile(key, params2, false, String.class);
        String orderTitle = (String)params2.get("title");
        String orderSubtitle = (String)params2.get("subtitle");

        System.out.println(" >> discount: " + discount);        
        if(!"".equals(discount)){
            model.addAttribute("discountPercentage", Integer.parseInt(discount.replace("discount", "")));
        }

        model.addAttribute("totalProduct", totalAmount - SHIPPING_COST);
        model.addAttribute("shippingCost", SHIPPING_COST);
        model.addAttribute("totalAmount", totalAmount);
        model.addAttribute("orderTitle", orderTitle);
        model.addAttribute("orderSubtitle", orderSubtitle);
        model.addAttribute("statusName", STATUS.get(VARIABLES.get("status")));

        Calendar c = Calendar.getInstance();
        c.add(Calendar.DATE, 2);
        model.addAttribute("shippingDate", c.getTime());
        model.addAttribute("paymentMethod", paymentMethod);
        model.addAttribute("paymentMethodMap", PAYMENT_METHODS);

        key = "order.credit.earned";
        float earnedCredits = smartPropApi.<Float>compile(key, params, false, Float.class);
        if(earnedCredits > 0.0f) {
            Double roundEarnedCredits = Math.floor(earnedCredits);
            model.addAttribute("earnedCredits", roundEarnedCredits.intValue());
        }

        return "order-completed";
    }

    @RequestMapping("/smart-properties/reset")
    public ResponseEntity<String> index(){
        smartPropApi.resetSmartPropertiesCodeMap();
        return ResponseEntity.ok("Done");
    }

    @CrossOrigin(origins = "http://localhost:3000")
    @RequestMapping("/variables")
    public ResponseEntity<String> variables(Model model,
            // Header Message
            @RequestBody Map<String, Object> variables){
        for(Map.Entry<String, Object> entry : variables.entrySet()){
            if(Arrays.asList("day", "month", "dayBirth", "monthBirth", "totalAmount", "lastCouponWinDays", "orderNumber").contains(entry.getKey())){
                WebAppController.VARIABLES.put(entry.getKey(), Integer.valueOf((String)entry.getValue()));
            } else if(Arrays.asList("hasHistoricalOrders", "creditClient").contains(entry.getKey())){
                WebAppController.VARIABLES.put(entry.getKey(), entry.getValue().equals("true"));
            } else {
                WebAppController.VARIABLES.put(entry.getKey(), entry.getValue());
            }
        } 
        return ResponseEntity.ok("Done");
    }
    
    private static Map<String, Object> getMapVariables(String... names){
    	Map<String, Object> map = new HashMap<>();
    	
    	// System.out.println("Variables:");
    	for(String name: names) {
    		// System.out.println(" - " + name + ": " + VARIABLES.get(name) + " (" + VARIABLES.get(name).getClass().getSimpleName() + ")");
    		map.put(name, VARIABLES.get(name));
    	}
    	
    	return map;
    }
    
}
