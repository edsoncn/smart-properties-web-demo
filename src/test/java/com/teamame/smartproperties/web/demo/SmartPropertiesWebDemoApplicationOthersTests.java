package com.teamame.smartproperties.web.demo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.teamame.smartproperties.api.SmartPropertiesApi;
import com.teamame.smartproperties.api.core.SmartPropException;

@Deprecated
public class SmartPropertiesWebDemoApplicationOthersTests {
    
    private SmartPropertiesApi smartPropApi = new SmartPropertiesApi("http://localhost:3001/api/v2", "ripleype", "prod", "667259e779f8908ce1899fe86cf68d29");

    @Before
    public void init() {
        smartPropApi.initialize();
    }

    @Deprecated
    @Test
    @Ignore
    public void callMath() throws SmartPropException {
    	String key = "test.math01";
        Map<String, Object> params = new HashMap<>();
        Assert.assertEquals(27.0f, smartPropApi.<Float>compile(key, params, false, Float.class), 0.0f);
        
        key = "test.math02.pitagoras";
        params.put("a", 3);
        params.put("b", 4.0f);
        Assert.assertEquals(5.0f, smartPropApi.<Float>compile(key, params, false, Float.class), 0.0f);
    }

    @Deprecated
    @Test
    @Ignore
    public void callBoolean() throws SmartPropException {
    	String key = "test.boolean01";
        Map<String, Object> params = new HashMap<>();        
        Assert.assertEquals(true, smartPropApi.<Boolean>compile(key, params, false, Boolean.class));
    }

    @SuppressWarnings("unchecked")
    @Deprecated
    @Test
    @Ignore
    public void callTestArray01() throws SmartPropException {
    	String key = "test.array01";
        Map<String, Object> params = new HashMap<>();
        List<Object> expectedResult = new ArrayList<Object>();
        expectedResult.add(2);
        expectedResult.add(3);
        expectedResult.add(4);
        expectedResult.add(5);
        expectedResult.add(6);
        expectedResult.add(1);
        expectedResult.add(7);
        expectedResult.add(2);
        expectedResult.add(5);
        
        Assert.assertEquals(expectedResult, smartPropApi.<List<Object>>compile(key, params, false, (Class<List<Object>>)(Object)List.class));
    }

    @SuppressWarnings("unchecked")
    @Deprecated
    @Test
    @Ignore
    public void callCubik() throws SmartPropException {
    	String key = "test.cubik";
        Map<String, Object> params = new HashMap<>();
        
        List<Object> cubik11 = new ArrayList<Object>();
        cubik11.add(1);
        cubik11.add(2);
        cubik11.add(3);
        List<Object> cubik12 = new ArrayList<Object>();
        cubik12.add(10);
        cubik12.add(11);
        cubik12.add(12);
        List<Object> cubik13 = new ArrayList<Object>();
        cubik13.add(19);
        cubik13.add(20);
        cubik13.add(21);
        List<Object> cubik21 = new ArrayList<Object>();
        cubik21.add(4);
        cubik21.add(5);
        cubik21.add(6);
        List<Object> cubik22 = new ArrayList<Object>();
        cubik22.add(13);
        cubik22.add(14);
        cubik22.add(15);
        List<Object> cubik23 = new ArrayList<Object>();
        cubik23.add(22);
        cubik23.add(23);
        cubik23.add(24);
        List<Object> cubik31 = new ArrayList<Object>();
        cubik31.add(7);
        cubik31.add(8);
        cubik31.add(9);
        List<Object> cubik32 = new ArrayList<Object>();
        cubik32.add(16);
        cubik32.add(17);
        cubik32.add(18);
        List<Object> cubik33 = new ArrayList<Object>();
        cubik33.add(25);
        cubik33.add(26);
        cubik33.add(27);
        
        List<Object> cubik1 = new ArrayList<Object>();
        cubik1.add(cubik11);
        cubik1.add(cubik12);
        cubik1.add(cubik13);

        List<Object> cubik2 = new ArrayList<Object>();
        cubik2.add(cubik21);
        cubik2.add(cubik22);
        cubik2.add(cubik23);

        List<Object> cubik3 = new ArrayList<Object>();
        cubik3.add(cubik31);
        cubik3.add(cubik32);
        cubik3.add(cubik33);

        List<Object> cubik = new ArrayList<Object>();
        cubik.add(cubik1);
        cubik.add(cubik2);
        cubik.add(cubik3);
        
        Assert.assertEquals(cubik, smartPropApi.<List<Object>>compile(key, params, false, (Class<List<Object>>)(Object)List.class));
    }

    @Deprecated
    @Test
    @Ignore
    public void callBoolean02() throws SmartPropException {
    	String key = "test.boolean02";
        Map<String, Object> params = new HashMap<>();
        
        params.put("b", Boolean.TRUE);
        Assert.assertEquals(0f, smartPropApi.<Integer>compile(key, params, false, Integer.class), 0f);

        params.put("b", Boolean.FALSE);
        Assert.assertEquals(5f, smartPropApi.<Integer>compile(key, params, false, Integer.class), 0f);

        params.put("c", 1);
        Assert.assertEquals(0f, smartPropApi.<Integer>compile(key, params, false, Integer.class), 0f);

        params.put("c", 0);
        Assert.assertEquals(-1f, smartPropApi.<Integer>compile(key, params, false, Integer.class), 0f);
        
        params.put("a", 3);
        Assert.assertEquals(3f, smartPropApi.<Integer>compile(key, params, false, Integer.class), 0f);
    }

    @Deprecated
    @Test
    @Ignore
    public void callDefaultTest01() throws SmartPropException {
    	String key = "test.default01";
        Map<String, Object> params = new HashMap<>();
        
        Assert.assertEquals("ok", smartPropApi.<String>compile(key, params, false, String.class));
    }

    @Deprecated
    @Test
    @Ignore
    public void callDefaultTest02() throws SmartPropException {
    	String key = "test.default02";
        Map<String, Object> params = new HashMap<>();
        
        Assert.assertEquals("ok", smartPropApi.<String>compile(key, params, false, String.class));
    }

}
