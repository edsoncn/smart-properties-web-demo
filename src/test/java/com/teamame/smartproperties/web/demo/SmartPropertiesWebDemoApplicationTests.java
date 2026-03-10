package com.teamame.smartproperties.web.demo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.teamame.smartproperties.api.SmartPropertiesApi;
import com.teamame.smartproperties.api.core.SmartPropException;

public class SmartPropertiesWebDemoApplicationTests {

    private SmartPropertiesApi smartPropApi = new SmartPropertiesApi("http://localhost:3001/api/v2", "ripleype", "prod", "0e0370c308cf0919d66a29034b3303f4");

    @Before
    public void init() {
        smartPropApi.initialize();
    }

    @Test
    public void analyzeHeaderMessageTest() throws SmartPropException {
        String key = "user.header.message";
        Map<String, Object> params = new HashMap<>();

        params.put("name", "Monica");
        params.put("dayBirth", 28);
        params.put("monthBirth", 12);
        params.put("day", 28);
        params.put("month", 12);
        Assert.assertEquals("Welcome Monica. Happy birthday! Have a nice day!", smartPropApi.<String>compile(key, params, false, String.class));
        
        params.put("dayBirth", 31);
        params.put("monthBirth", 1);
        Assert.assertEquals("Welcome Monica. Have a nice day!", smartPropApi.<String>compile(key, params, false, String.class));

        params.put("day", 24);
        params.put("month", 12);
        Assert.assertEquals("Welcome Monica. Merry Christmas!", smartPropApi.<String>compile(key, params, false, String.class));

        params.put("day", 25);
        params.put("month", 12);
        Assert.assertEquals("Welcome Monica. Merry Christmas!", smartPropApi.<String>compile(key, params, false, String.class));

        params.put("day", 28);
        params.put("month", 7);
        Assert.assertEquals("Welcome Monica. Happy Independence Day!", smartPropApi.<String>compile(key, params, false, String.class));
    }

    @Test
    public void analyzeMenuOptionsTest() throws SmartPropException {
        String key = "user.menu.options";
        Map<String, Object> params = new HashMap<>();

        params.put("rol", "guest");
        params.put("hasHistoricalOrders", false);
        params.put("creditClient ", false);
        Assert.assertEquals("login,register", smartPropApi.<String>compile(key, params, false, String.class));

        params.put("rol", "representative");
        Assert.assertEquals("account,represent,address,history,password", smartPropApi.<String>compile(key, params, false, String.class));

        params.put("rol", "registered");
        params.put("hasHistoricalOrders", true);
        params.put("creditClient", true);
        Assert.assertEquals("account,address,history,tracking,password,credit", smartPropApi.<String>compile(key, params, false, String.class));

        params.put("creditClient", false);
        Assert.assertEquals("account,address,history,tracking,password", smartPropApi.<String>compile(key, params, false, String.class));

        params.put("hasHistoricalOrders", false);
        Assert.assertEquals("account,address,password", smartPropApi.<String>compile(key, params, false, String.class));
    }

    @SuppressWarnings("unchecked")
	@Test
    public void analyzePaymentMethods() throws SmartPropException {
        String key = "order.payment.methods";
        Map<String, Object> params = new HashMap<>();

        params.put("rol", "guest");
        params.put("creditClient", false);
        
        params.put("totalAmount", 900);
        List<Object> expectedResult = new ArrayList<Object>();
        expectedResult.add("creditCard");
        expectedResult.add("paypal");
        expectedResult.add("billMeLater");
        Assert.assertEquals(expectedResult, smartPropApi.<List<Object>>compile(key, params, false, (Class<List<Object>>)(Object)List.class));
        params.remove("paymentMethods");
        
        params.put("rol", "representative");
        params.put("totalAmount", 1200);
        List<Object> expectedResult2 = new ArrayList<Object>();
        expectedResult2.add("payWithLink");        
        Assert.assertEquals(expectedResult2, smartPropApi.<List<Object>>compile(key, params, false, (Class<List<Object>>)(Object)List.class));
        params.remove("paymentMethods");
        
        params.put("totalAmount", 600);
        List<Object> expectedResult3 = new ArrayList<Object>();
        expectedResult3.add("payWithLink");
        expectedResult3.add("billMeLater");
        Assert.assertEquals(expectedResult3, smartPropApi.<List<Object>>compile(key, params, false, (Class<List<Object>>)(Object)List.class));
        params.remove("paymentMethods");
        
        params.put("rol", "registered");
        params.put("creditClient", true);
        params.put("totalAmount", 999);
        List<Object> expectedResult4 = new ArrayList<Object>();
        expectedResult4.add("creditCard");
        expectedResult4.add("paypal");
        expectedResult4.add("smartShopCredit");
        expectedResult4.add("billMeLater");
        Assert.assertEquals(expectedResult4, smartPropApi.<List<Object>>compile(key, params, false, (Class<List<Object>>)(Object)List.class));
        params.remove("paymentMethods");
    
    }

    @Test
    public void analyzeCouponCode() throws SmartPropException {
        String key = "order.completed.discount";
        Map<String, Object> params = new HashMap<>();

        params.put("paymentMethod", "billMeLater");
        params.put("lastCouponWinDays", 20);
        params.put("totalAmount", 900);
        Assert.assertEquals("", smartPropApi.<String>compile(key, params, false, String.class));

        params.put("paymentMethod", "paypal");
        Assert.assertEquals("", smartPropApi.<String>compile(key, params, false, String.class));

        params.put("lastCouponWinDays", 40);
        Assert.assertEquals("", smartPropApi.<String>compile(key, params, false, String.class));

        params.put("totalAmount", 1100);
        Assert.assertEquals("discount5", smartPropApi.<String>compile(key, params, false, String.class));

        params.put("totalAmount", 1600);
        Assert.assertEquals("discount10", smartPropApi.<String>compile(key, params, false, String.class));

        params.put("totalAmount", 2000);
        Assert.assertEquals("discount15", smartPropApi.<String>compile(key, params, false, String.class));

        params.put("paymentMethod", "smartShopCredit");
        params.put("totalAmount", 1000);
        Assert.assertEquals("discount10", smartPropApi.<String>compile(key, params, false, String.class));

        params.put("totalAmount", 1500);
        Assert.assertEquals("discount15", smartPropApi.<String>compile(key, params, false, String.class));

        params.put("totalAmount", 2100);
        Assert.assertEquals("discount20", smartPropApi.<String>compile(key, params, false, String.class));
    }

    @Test
    public void analyzeOrderCreditTest() throws SmartPropException {
        String key = "order.credit.earned";
        Map<String, Object> params = new HashMap<>();

        params.put("paymentMethod", "paypal");
        params.put("totalAmount", 475.40f);
        Assert.assertEquals(0.0f, smartPropApi.<Float>compile(key, params, false, Float.class), 0.0f);

        params.put("paymentMethod", "smartShopCredit");
        Assert.assertEquals((475.40f / (1 + 0.12f))  * (1 - 0.19f) / 3.33f, smartPropApi.<Float>compile(key, params, false, Float.class), 0.0f);
    }

    @Test
    public void analyzeTestWhileFibonacci() throws SmartPropException {
        String key = "fibonacci";
        Map<String, Object> params = new HashMap<>();
        Assert.assertEquals("0 1 1 2 3 5 8 13 21 34 ", smartPropApi.<String>compile(key, params, false, String.class));
    }
  
}
