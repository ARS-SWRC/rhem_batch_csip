package service_tests.rhem;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
import csip.test.ServiceTest2;
import java.io.File;
import java.util.Properties;
import org.codehaus.jettison.json.JSONArray;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

/**
 *
 * @author od
 */
public class STest {

//    @Test
//    public void stest() throws Exception {
//        String name = "test/" + new File(getClass().getCanonicalName().replace('.', '/')).getParent();
//        ServiceTest.Results r = ServiceTest.run(name);
//        System.out.println(name + ":\n" + r);
//        Assert.assertTrue(r.getTotal() == r.getSucceeded());
//    }
    
    @Rule
    public TestName name = new TestName();

    private void run() throws Exception {
        String testFolder = new File(getClass().getCanonicalName().replace('.', '/')).getParent();
        JSONArray r = ServiceTest2.run(new Properties(), "test/" + testFolder + "/" + name.getMethodName() + ".json");
        org.junit.Assert.assertTrue(r.getJSONObject(0).getInt("successful") == 1);
    }
    
    @Test public void rhem01_m_rhem_runmodel_1_0__default() throws Exception { run();}
    
    @Test public void rhem02_m_rhem_getclimatestations_1_0__default() throws Exception { run();}
    @Test public void rhem02_m_rhem_getclimatestations_1_0__test1() throws Exception { run();}
    @Test public void rhem02_m_rhem_getclimatestations_1_0__test2() throws Exception { run();}
    @Test public void rhem02_m_rhem_getclimatestations_1_0__test3() throws Exception { run();}
    @Test public void rhem02_m_rhem_getclimatestations_1_0__test4() throws Exception { run();}
    @Test public void rhem02_m_rhem_getclimatestations_1_0__test5() throws Exception { run();}
    @Test public void rhem02_m_rhem_getclimatestations_1_0__test6() throws Exception { run();}
    
    @Test public void rhem03_m_rhem_compEsd_1_0__default() throws Exception { run();}
    @Test public void rhem03_m_rhem_compEsd_1_0__test1() throws Exception { run();}
    @Test public void rhem03_m_rhem_compEsd_1_0__test2() throws Exception { run();}
    @Test public void rhem03_m_rhem_compEsd_1_0__test3() throws Exception { run();}
    @Test public void rhem03_m_rhem_compEsd_1_0__test4() throws Exception { run();}
    @Test public void rhem03_m_rhem_compEsd_1_0__test5() throws Exception { run();}
    @Test public void rhem03_m_rhem_compEsd_1_0__test6() throws Exception { run();}
    @Test public void rhem03_m_rhem_compEsd_1_0__test7() throws Exception { run();}
    @Test public void rhem03_m_rhem_compEsd_1_0__test8() throws Exception { run();}
    @Test public void rhem03_m_rhem_compEsd_1_0__test9() throws Exception { run();}
    
    @Test public void rhem04_m_rhem_surfacetextureclasses_1_0__default() throws Exception { run();}
    
    @Test public void rhem05_m_rhem_slopeShapes_1_0__default() throws Exception { run();}
}
