/*
 * $Id$
 *
 * This file is part of the Cloud Services Integration Platform (CSIP),
 * a Model-as-a-Service framework, API, and application suite.
 *
 * 2012-2017, OMSLab, Colorado State University.
 *
 * OMSLab licenses this file to you under the MIT license.
 * See the LICENSE file in the project root for more information.
 */
package m.rhem.rhem07_editparfile;

import csip.Config;
import csip.Executable;
import csip.ModelDataService;
import csip.ServiceException;
import csip.annotations.Polling;
import csip.annotations.Resource;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.ws.rs.Path;
import m.rhem.model.Parameter;
import m.rhem.model.RhemModel;
import csip.annotations.Description;
import csip.annotations.Name;
import rhem.utils.DBQueries;
import static rhem.utils.DBResources.RHEM_EXE;
import rhem.utils.DBResources;

/**
 * RHEM-07: Run model with edited parameter file
 *
 * @version 1.0
 * @author Rumpal Sidhu
 */
@Name("RHEM-07: Run model with edited parameter file")
@Description("Run the RHEM model with parameter file as input.")
@Path("m/rhem/editparfile/1.0")
@Polling(first = 5000, next = 2000)
@Resource(from = DBResources.class)

public class V1_0 extends ModelDataService {

    private String cligen_db = Config.getString("rhem.cligen_db", "file:/Users/rumpal/Documents/Work/csipDocuments/RHEM/cligen");

    private String parameterFileName;
    private String stormFileName;
    private String summaryFileName;
    private String runFileName;
    private String detailedOutputFileName;
    private Parameter parameter;

    private int aoaId;
    private int rhemSiteId;
    private String scenarioName;
    private String stateId;
    private String climatestationId;

    @Override
    public void preProcess() throws ServiceException {
        aoaId = getIntParam("AoAID", 0);
        rhemSiteId = getIntParam("rhem_site_id", 0);
        scenarioName = getStringParam("scenarioname");
        stateId = getStringParam("stateid");
        climatestationId = getStringParam("climatestationid");

        String diams = getStringParam("DIAMS");
        String density = getStringParam("DENSITY");

        String len = getStringParam("LEN");
        String chezy = getStringParam("CHEZY");
        String rchezy = getStringParam("RCHEZY");
        String sl = getStringParam("SL");
        String sx = getStringParam("SX");
        String cv = getStringParam("CV");
        String sat = getStringParam("SAT");
        String kss = getStringParam("KSS");
        String komega = getStringParam("KOMEGA");
        String kcm = getStringParam("KCM");
        String ke = getStringParam("KE");
        String adf = getStringParam("ADF");
        String alf = getStringParam("ALF");
        String bare = getStringParam("BARE");

        String g = getStringParam("G");
        String dist = getStringParam("DIST");
        String por = getStringParam("POR");
        String fract = getStringParam("FRACT");

        parameter = new Parameter(diams, density, len, chezy, rchezy,
                sl, sx, cv, sat, kss, komega, kcm, ke, adf, alf, bare, g,
                dist, por, fract);
    }

    @Override
    public void doProcess() throws ServiceException {

        SimpleDateFormat sdf = new SimpleDateFormat("MMMM dd, YYYY, hh:mm a");
        Date now = new Date();
        String today = sdf.format(now);

        String fileName = scenarioName;
        if (scenarioName.length() > 15) {
            fileName = scenarioName.substring(0, 15);
        }
        parameterFileName = "scenario_input_" + fileName + ".par";
        stormFileName = "storm_input_" + fileName + ".pre";
        summaryFileName = "scenario_output_summary_" + fileName + ".sum";
        detailedOutputFileName = "scenario_output_summary_" + fileName + ".out";
        runFileName = fileName + ".run";

        RhemModel rhemModel = new RhemModel(stateId, climatestationId,
                scenarioName, today, getWorkspaceDir(), parameterFileName,
                stormFileName, runFileName, summaryFileName);

        rhemModel.generateParamFile(parameter);
        rhemModel.generateStormFile(cligen_db, Double.parseDouble(parameter.getKe()));
        rhemModel.generateRunFile();
        try {
            runModel();
        } catch (IOException ex) {
            throw new ServiceException(ex);
        }

        //If the run is successful then edit the summary file.
        double avgYearlyPrecip = 0;
        try (Connection connection = getResourceJDBC(DBResources.CRDB);
                Statement statement = connection.createStatement();) {
            try (ResultSet rs = statement.executeQuery(DBQueries.RHEM07Query01(climatestationId))) {
                while (rs.next()) {
                    avgYearlyPrecip = rs.getDouble("avg_yearly_precip_mm");
                }
            }
        } catch (SQLException e) {
            throw new ServiceException(e);
        }
        rhemModel.appendToSumFile(avgYearlyPrecip);
    }

    private void runModel() throws ServiceException, IOException {
        int run = -1;
        Executable rh = getResourceExe(RHEM_EXE);
        rh.setArguments("-b", getWorkspaceFile(runFileName).toPath());
        run = rh.exec();
        if (run != 0) {
            throw new ServiceException("Problem in running the model.");
        }
    }

    @Override
    public void postProcess() {
        putResult("AoAID", aoaId, "Area of Analysis Identifier");
        putResult("rhem_site_id", rhemSiteId, "RHEM Evaluation Site Identifier");
        putResult("CLEN", parameter.getClen());
        putResult("UNITS", "metric");
        putResult("DIAMS", parameter.getDiams());
        putResult("DENSITY", parameter.getDensity());
        putResult("LEN", parameter.getLen());
        putResult("CHEZY", parameter.getChezy());
        putResult("RCHEZY", parameter.getRchezy());
        putResult("SL", parameter.getSl());
        putResult("SX", parameter.getSx());
        putResult("CV", parameter.getCv());
        putResult("SAT", parameter.getSat());
        putResult("KSS", parameter.getKss());
        putResult("KOMEGA", parameter.getKomega());
        putResult("KCM", parameter.getKcm());
        putResult("KE", parameter.getKe());
        putResult("G", parameter.getG());
        putResult("DIST", parameter.getDist());
        putResult("POR", parameter.getPor());
        putResult("ADF", parameter.getAdf());
        putResult("ALF", parameter.getAlf());
        putResult("BARE", parameter.getBare());
        putResult("FRACT", parameter.getFract());
        putResult(new File(getWorkspaceDir(), parameterFileName), "Parameter input file");
        putResult(new File(getWorkspaceDir(), stormFileName), "Storm input file");
        putResult(new File(getWorkspaceDir(), summaryFileName), "Summary file");
        putResult(new File(getWorkspaceDir(), detailedOutputFileName), "Detailed");
    }
}
