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
package m.rhem.rhem01_runmodel;

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
import java.util.logging.Level;
import javax.ws.rs.Path;
import m.rhem.model.AoA;
import m.rhem.model.Parameter;
import m.rhem.model.RhemModel;
import csip.annotations.Description;
import csip.annotations.Name;
import rhem.utils.DBQueries;
import static rhem.utils.DBResources.RHEM_EXE;
import rhem.utils.DBResources;

/**
 * RHEM-01:Run RHEM Model
 *
 * @version 1.0
 * @author Rumpal Sidhu
 */
@Name("RHEM-01:Run RHEM Model")
@Description("Run RHEM Model utilizing parameters including climate station, "
        + "surface soil texture class, slope percent and length, and vegetative "
        + "cover characteristics (bunchgrass, forbs/annual, shrub, and sodgrass "
        + "foliar cover; plant basal cover; rock cover; litter cover; and "
        + "cryptogam cover.")
@Path("m/rhem/runrhem/1.0")
@Polling(first = 10000, next = 2000)
@Resource(from = DBResources.class)

public class V1_0 extends ModelDataService {

    private AoA aoa;
    private String parameterFileName;
    private String stormFileName;
    private String summaryFileName;
    private String runFileName;
    private String detailedOutputFileName;
    private Parameter parameter;

    String cligen_db = Config.getString("rhem.cligen_db", "file:/Users/rumpal/Documents/Work/csipDocuments/RHEM/cligen");

    @Override
    public void preProcess() throws ServiceException {
        int aoaId = getIntParam("AoAID", 0);
        int rhemSiteId = getIntParam("rhem_site_id", 0);
        String scenarioName = getStringParam("scenarioname");
        String scenarioDescription = getStringParam("scenariodescription");
        int unit = getIntParam("units", 0);
        String stateId = getStringParam("stateid");
        String climatestationId = getStringParam("climatestationid");
        String soilTexture = getStringParam("soiltexture");
        String slopeShape = getStringParam("slopeshape");
        double slopeSteepness = getDoubleParam("slopesteepness", 0.0);
        double bunchGgrassCanopyCover = getDoubleParam("bunchgrasscanopycover", 0.0);
        double forbsCanopyCover = getDoubleParam("forbscanopycover", 0.0);
        double shrubsCanopyCover = getDoubleParam("shrubscanopycover", 0.0);
        double sodGrassCanopyCover = getDoubleParam("sodgrasscanopycover", 0.0);
        double basalCover = getDoubleParam("basalcover", 0.0);
        double rockCover = getDoubleParam("rockcover", 0.0);
        double litterCover = getDoubleParam("littercover", 0.0);
        double cryptogamsCover = getDoubleParam("cryptogamscover", 0.0);

        //Validations
        //The values allowed for the field unit in the request are 1 and 2. 1 is for metric units and 2 is for English units
        if (unit != 1 && unit != 2) {
            LOG.log(Level.SEVERE, "RHEM-01: Invalid unit");
            throw new ServiceException("Unit should be 1-metric or 2 - English. Digit " + unit + " is not valid.");
        }
        double slopeLength;
        if (unit == 1) {
            slopeLength = 50;
        } else {
            slopeLength = 164.04;
        }

        aoa = new AoA(aoaId, rhemSiteId, scenarioName,
                scenarioDescription, unit, stateId, climatestationId,
                soilTexture, slopeLength, slopeShape, slopeSteepness,
                bunchGgrassCanopyCover, forbsCanopyCover, shrubsCanopyCover,
                sodGrassCanopyCover, basalCover, rockCover, litterCover,
                cryptogamsCover, 25);
    }

    @Override
    public void doProcess() throws ServiceException {
        SimpleDateFormat sdf = new SimpleDateFormat("MMMM dd, YYYY, hh:mm a");
        Date now = new Date();
        String today = sdf.format(now);

        String fileName = aoa.getScenarioName();
        if (fileName.length() > 15) {
            fileName = fileName.substring(0, 15);
        }
        parameterFileName = "scenario_input_" + fileName + ".par";
        stormFileName = "storm_input_" + fileName + ".pre";
        summaryFileName = "scenario_output_summary_" + fileName + ".sum";
        detailedOutputFileName = "scenario_output_summary_" + fileName + ".out";
        runFileName = fileName + ".run";

        parameter = new Parameter(aoa);

        try (Connection connection = getResourceJDBC(DBResources.CRDB);) {
            parameter.computeParameters(connection);
        } catch (ServiceException | SQLException se) {
            throw new ServiceException(se);
        }

        RhemModel rhemModel = new RhemModel(aoa.getStateId(), aoa.getClimateStationId(),
                aoa.getScenarioName(), today, getWorkspaceDir(), parameterFileName,
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
            try (ResultSet rs = statement.executeQuery(DBQueries.RHEM01Query02(aoa.getClimateStationId()))) {
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
        Executable rh = getResourceExe(RHEM_EXE);
        rh.setArguments("-b", getWorkspaceFile(runFileName).toPath());
        int run = rh.exec();
        if (run != 0) {
            throw new ServiceException("Problem in running the model.");
        }
    }

    @Override
    public void postProcess() {
        putResult("AoAID", aoa.getAoaId(), "Area of Analysis Identifier");
        putResult("rhem_site_id", aoa.getRhemSiteId(), "RHEM Evaluation Site Identifier");
        putResult("CLEN", parameter.getClen());
        putResult("UNITS", "metric");
        putResult("DIAMS", parameter.getDiams());
        putResult("DENSITY", parameter.getDensity());
        putResult("CHEZY", parameter.getChezy());
        putResult("RCHEZY", parameter.getRchezy());
        putResult("SL", parameter.getSl());
        putResult("SX", parameter.getSx());
        putResult("KSS", parameter.getKss());
        putResult("KE", parameter.getKe());
        putResult("G", parameter.getG());
        putResult("DIST", parameter.getDist());
        putResult("POR", parameter.getPor());
        putResult("FRACT", parameter.getFract());
        putResult(new File(getWorkspaceDir(), parameterFileName), "Parameter input file");
        putResult(new File(getWorkspaceDir(), stormFileName), "Storm input file");
        putResult(new File(getWorkspaceDir(), summaryFileName), "Summary file");
        putResult(new File(getWorkspaceDir(), detailedOutputFileName), "Detailed");
    }
}
