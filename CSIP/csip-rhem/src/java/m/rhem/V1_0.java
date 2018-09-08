/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package m.rhem;

import csip.*;
import csip.annotations.Polling;
import csip.annotations.Resource;
import static csip.annotations.ResourceType.*;
import javax.ws.rs.*;
import csip.annotations.Description;
import csip.annotations.Name;

/**
 * WEPP This implements the CSIP WEPS service.
 *
 * @author od, mh
 */
@Name("rhem")
@Description("RHEM Service")
@Path("m/rhem/1.0")
@Polling(first = 5000, next = 2000)

@Resource(file = "/bin/win-x86/rhem_v2.3.exe", wine = true, id = "rhem", type = EXECUTABLE)
@Resource(file = "*.sum  *.out *stdout.txt *stderr.txt", type = OUTPUT)
public class V1_0 extends ModelDataService {

    /**
     *
     * @return null if all good, an error message otherwise.
     * @throws Exception
     */
    @Override
    protected void doProcess() throws Exception {
        Executable weps = getResourceExe("rhem");
        weps.setArguments("-b", "scenarion_run_3678.run");
        int r = weps.exec();
//        return r == 0 ? EXEC_OK : EXEC_FAILED;
    }
}
