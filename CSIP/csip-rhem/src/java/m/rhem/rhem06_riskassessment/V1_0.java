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
package m.rhem.rhem06_riskassessment;

import csip.Executable;
import csip.ModelDataService;
import csip.ServiceException;
import csip.annotations.Polling;
import csip.annotations.Resource;
import static csip.annotations.ResourceType.OUTPUT;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.Path;
import csip.annotations.Description;
import csip.annotations.Name;
import rhem.utils.LinearInterpolate;
import static rhem.utils.DBResources.RHEM_RA_EXE;
import rhem.utils.DBResources;

/**
 * RHEM-06: Risk Assessment
 *
 * @author rumpal
 * @version 1.0
 */
@Name("RHEM-06: Risk Assessment")
@Description("This service runs risk assessment with a maximum of five detailed output files.")
@Path("m/rhem/riskassessment/1.0")
@Polling(first = 10000, next = 2000)
@Resource(file = "*.out *.run", type = OUTPUT)
@Resource(from = DBResources.class)
public class V1_0 extends ModelDataService {

    private String baseLineScenarioFileName;
    private List<String> raScenarioFilenameList = new ArrayList<>();
    private ArrayList<String> scenarioNames = new ArrayList<>();

    @Override
    public void preProcess() throws ServiceException {
        int numberOfFiles = getIntParam("number_of_files");
        if (numberOfFiles > 5) {
            throw new ServiceException("The risk assessment can be performed with a maximum of five scenarios.");
        }
        for (int i = 1; i < numberOfFiles; i++) {
            raScenarioFilenameList.add(getStringParam("scenario_" + i + "_filename"));
        }
        baseLineScenarioFileName = getStringParam("base_line_scenario_file");
    }

    @Override
    public void doProcess() throws ServiceException {
        writeOutFilesToWorkspace();
        generateRARunFile();
        try {
            runRiskAssessment();
        } catch (IOException ex) {
            throw new ServiceException(ex);
        }

        double[][] outRAarray = readRiskAssessmentOutFile();
        ArrayList<ArrayList> interpolatedResultsArray = calculateReturnPeriods(outRAarray);
        writeInterpolatedResultsArray(interpolatedResultsArray);
    }

    private void writeOutFilesToWorkspace() throws ServiceException {
        getFileInput(baseLineScenarioFileName);
        for (String filename : raScenarioFilenameList) {
            getFileInput(filename);
        }
    }

    private void generateRARunFile() throws ServiceException {
        try (PrintWriter writer = new PrintWriter(new File(getWorkspaceDir(), "risk_assessment.run"))) {
            writer.println(baseLineScenarioFileName);
            for (String fileName : raScenarioFilenameList) {
                writer.println(fileName);
            }
            writer.close();
        } catch (FileNotFoundException ex) {
            throw new ServiceException(ex);
        }
    }

    private void runRiskAssessment() throws ServiceException, IOException {
        Executable rh = getResourceExe(RHEM_RA_EXE);
        rh.setArguments("-b", getWorkspaceFile("risk_assessment.run").toPath());
        int run = rh.exec();
        if (run != 0) {
            throw new ServiceException("Problem in running risk assessment.");
        }
    }

    private double[][] readRiskAssessmentOutFile() throws ServiceException {
        double outputRAArray[][] = new double[12][raScenarioFilenameList.size() + 2];
        int count = -1;
        boolean check = false;
        try {
            FileReader fileReader = new FileReader(getWorkspaceFile("risk_assessment.OUT"));
            try (BufferedReader bufferedReader
                    = new BufferedReader(fileReader)) {
                String line;

                while ((line = bufferedReader.readLine()) != null && !check) {
                    if (line.contains("FREQUENCY ANALYSIS")) {
                        check = true;
                        for (int i = 0; i < 3; i++) {
                            line = bufferedReader.readLine();
                            if (i == 2) {
                                String[] test = line.trim().split("\\s+");
                                for (int j = 1; j < test.length; j++) {
                                    scenarioNames.add(test[j]);
                                }
                            }
                        }
                    }
                }

                do {
                    count++;
                    String[] test = line.trim().split("\\s+");
                    for (int i = 0; i < test.length; i++) {
                        outputRAArray[count][i] = Double.parseDouble(test[i].trim());
                    }
                } while ((line = bufferedReader.readLine()) != null && check);

            }
        } catch (IOException exception) {
            throw new ServiceException(exception);
        }
        return outputRAArray;
    }

    public ArrayList<ArrayList> calculateReturnPeriods(double outputRAArray[][]) {
        /* Sample outputRAArray
        double[][] outputRAArray = {
            {2, 0.15, 0.28, 1.09, 1.45},
            {5, 0.3, 0.54, 2.03, 2.75},
            {10, 0.4, 0.72, 2.75, 3.76},
            {20, 0.53, 0.96, 3.65, 4.88},
            {30, 0.58, 1.05, 3.9, 5.27},
            {40, 0.59, 1.07, 3.91, 5.33},
            {50, 0.63, 1.14, 4.22, 5.72},
            {60, 0.64, 1.16, 4.34, 5.83},
            {70, 0.66, 1.18, 4.37, 5.91},
            {80, 0.68, 1.22, 4.49, 6.1},
            {90, 0.7, 1.26, 4.67, 6.32},
            {100, 0.71, 1.3, 4.84, 6.52}};
         */
        ArrayList<ArrayList> interpolatedResultsArray = new ArrayList();
        outputRAArray = roundValues(outputRAArray);
        double[][] trasposedMatrix = trasposeMatrix(outputRAArray);

        for (int x = 0; x < trasposedMatrix[0].length; x++) {
            double currentRP = trasposedMatrix[0][x];
            double currentSoilLoss = trasposedMatrix[1][x];
            double maxBaselineSoilLoss = trasposedMatrix[1][x];
            ArrayList<Double> rpPeriodArray = new ArrayList<>();
            rpPeriodArray.add(currentSoilLoss);
            rpPeriodArray.add(currentRP);

            for (int i = 2; i < trasposedMatrix.length; i++) {
                ArrayList<Double> alt_scenario = new ArrayList<>();
                for (int k = 0; k < trasposedMatrix[i].length; k++) {
                    if (trasposedMatrix[i][k] <= maxBaselineSoilLoss) {
                        alt_scenario.add(trasposedMatrix[i][k]);
                    }
                }
                // default the altenative scenario interpolated value to 1
                double altScenarioInterp = 1;
                if (!alt_scenario.isEmpty()) {
                    altScenarioInterp = LinearInterpolate.interpolate(maxBaselineSoilLoss, trasposedMatrix[i], trasposedMatrix[0]);
                    altScenarioInterp = Math.round(altScenarioInterp * 1000.0) / 1000.0;

                    // set the return period to 100 if the interpolated value is greater than 100
                    if (altScenarioInterp > 100 || Double.isNaN(altScenarioInterp)) {
                        altScenarioInterp = 100;
                    }
                }
                // round the interpolated year to the nearest 10th place
                altScenarioInterp = Math.round(altScenarioInterp * 10) / 10.0;
                rpPeriodArray.add(altScenarioInterp);
            }
            interpolatedResultsArray.add(rpPeriodArray);
        }
        return interpolatedResultsArray;
    }

    public static double[][] roundValues(double[][] matrix) {
        double[][] outputMatrix = new double[matrix.length][matrix[0].length];
        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < matrix[i].length; j++) {
                outputMatrix[i][j] = Math.round(matrix[i][j] * 1000.0) / 1000.0;
            }
        }
        return outputMatrix;
    }

    public static double[][] trasposeMatrix(double[][] matrix) {
        int x = matrix.length;
        int y = matrix[0].length;

        double[][] trasposedMatrix = new double[y][x];

        for (int i = 0; i < y; i++) {
            for (int j = 0; j < x; j++) {
                trasposedMatrix[i][j] = matrix[j][i];
            }
        }
        return trasposedMatrix;
    }

    public void writeInterpolatedResultsArray(ArrayList<ArrayList> interpolatedResultsArray) throws ServiceException {

        try (PrintWriter writer = new PrintWriter(new File(getWorkspaceDir(), "frequencyAnalysisReturnPeriodTable.out"));) {
            writer.print(String.format("%-20s %-20s", "BASELINE SCENARIO", "RETURN PERIOD"));
            for (String name : scenarioNames) {
                writer.print(String.format("%-20s", name + "(years)"));
            }
            writer.println();

            for (int i = 0; i < interpolatedResultsArray.size(); i++) {
                for (int j = 0; j < interpolatedResultsArray.get(i).size(); j++) {
                    writer.print(String.format("%-20s", interpolatedResultsArray.get(i).get(j).toString()));
                }
                writer.println();
            }
            writer.close();
        } catch (FileNotFoundException ex) {
            throw new ServiceException(ex);
        }

    }

}
