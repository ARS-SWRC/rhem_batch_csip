package m.rhem.model;

import csip.ServiceException;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLConnection;
import org.apache.commons.io.FileUtils;

/**
 *
 * @author rumpal
 */
public class RhemModel {

    protected String stateId;
    protected String climatestationId;
    protected String scenarioName;
    protected String today;

    protected File workSpaceDir;

    protected String parameterFileName;
    protected String stormFileName;
    protected String runFileName;
    protected String summaryFileName;


    public RhemModel(String stateId, String climatestationId, String scenarioName,
            String today, File workSpaceDir, String parameterFileName,
            String stormFileName, String runFileName, String summaryFileName) {
        this.stateId = stateId;
        this.climatestationId = climatestationId;
        this.scenarioName = scenarioName;
        this.today = today;
        this.workSpaceDir = workSpaceDir;
        this.parameterFileName = parameterFileName;
        this.stormFileName = stormFileName;
        this.runFileName = runFileName;
        this.summaryFileName = summaryFileName;
    }


    public void generateParamFile(Parameter param) throws ServiceException {

        try (PrintWriter writer = new PrintWriter(new File(workSpaceDir, parameterFileName));) {
            writer.println("! Parameter file for scenario: " + scenarioName);
            writer.println("! Date built: " + today + " (Version 2.3)");
            writer.println("! Parameter units: DIAMS(mm), DENSITY(g/cc),TEMP(deg C)");
            writer.println("BEGIN GLOBAL");
            writer.println(String.format("        %-8s=   %s", "CLEN", param.clen));
            writer.println(String.format("        %-8s=   %s", "UNITS", "metric"));
            writer.println(String.format("        %-8s=   %s", "DIAMS", param.diams));
            writer.println(String.format("        %-8s=   %s", "DENSITY", param.density));
            writer.println(String.format("        %-8s=   %s", "TEMP", 40));
            writer.println(String.format("        %-8s=   %s", "NELE", 1));
            writer.println("END GLOBAL");
            writer.println("BEGIN PLANE");
            writer.println(String.format("        %-8s=   %s", "ID", 1));
            writer.println(String.format("        %-8s=   %s", "LEN", param.len));
            writer.println(String.format("        %-8s=   %s", "WIDTH", 1.0));
            writer.println(String.format("        %-8s=   %s", "CHEZY", param.chezy));
            writer.println(String.format("        %-8s=   %s", "RCHEZY", param.rchezy));
            writer.println(String.format("        %-8s=   %s", "SL", param.sl));
            writer.println(String.format("        %-8s=   %s", "SX", param.sx));
            writer.println(String.format("        %-8s=   %s", "CV", param.cv));
            writer.println(String.format("        %-8s=   %s", "SAT", param.sat));
            writer.println(String.format("        %-8s=   %s", "PR", 1));
            writer.println(String.format("        %-8s=   %s", "KSS", param.kss));
            writer.println(String.format("        %-8s=   %s", "KOMEGA", param.komega));
            writer.println(String.format("        %-8s=   %s", "KCM", param.kcm));
            writer.println(String.format("        %-8s=   %s", "CA", 1.0));
            writer.println(String.format("        %-8s=   %s", "IN", 0.0));
            writer.println(String.format("        %-8s=   %s", "KE", param.ke));
            writer.println(String.format("        %-8s=   %s", "G", param.g));
            writer.println(String.format("        %-8s=   %s", "DIST", param.dist));
            writer.println(String.format("        %-8s=   %s", "POR", param.por));
            writer.println(String.format("        %-8s=   %s", "ROCK", 0.00));
            writer.println(String.format("        %-8s=   %s", "SMAX", 1.0));
            writer.println(String.format("        %-8s=   %s", "ADF", param.adf));
            writer.println(String.format("        %-8s=   %s", "ALF", param.alf));
            writer.println(String.format("        %-8s=   %s", "BARE", param.bare));
            writer.println(String.format("        %-8s=   %s", "RSP", 1.0));
            writer.println(String.format("        %-8s=   %s", "SPACING", 1.0));
            writer.println(String.format("        %-8s=   %s", "FRACT", param.fract));
            writer.println("END PLANE");
            writer.close();
        } catch (FileNotFoundException ex) {
            throw new ServiceException("Problem in generating the parameter file.", ex);
        }
    }


    public void generateStormFile(String cligen_db, double Ke) throws ServiceException {

        String cligenFileName = "/" + stateId.toLowerCase()
                + "/300yr/" + stateId + "_" + climatestationId + "_300yr.out";

        String workSpaceFileName = stateId + "_" + climatestationId + ".out";
        File workSpaceFile = new File(workSpaceFileName, workSpaceFileName);

        try {
            URLConnection conn = new URL(cligen_db + cligenFileName).openConnection();
            FileUtils.copyInputStreamToFile(conn.getInputStream(), workSpaceFile);

            FileReader fileReader = new FileReader(workSpaceFile);
            long counter = 0;
            try (BufferedReader bufferedReader = new BufferedReader(fileReader)) {
                File tempFile = new File(workSpaceDir, "tempSummaryFile.pre");
                try (PrintWriter writer = new PrintWriter(tempFile)) {
                    for (int i = 0; i <= 17; i++) {
                        bufferedReader.readLine();
                    }
                    String line;
                    while ((line = bufferedReader.readLine()) != null) {
                        String[] test = line.split("\\s+");
                        double keComparison = Double.parseDouble(test[8]) * (Double.parseDouble(test[5]) / Double.parseDouble(test[6]));
                        if (Ke < keComparison && test.length > 8) {
                            counter++;
                            writer.println(String.format("    %-5s %-5s %-5s %-5s %-5s %-6s %-6s %-6s",
                                    counter, test[2], test[3], test[4], test[5], test[6], test[7], test[8]));
                        }
                    }
                }
                bufferedReader.close();
                appendInfo(tempFile, counter);
            }
        } catch (IOException | ServiceException exception) {
            throw new ServiceException("Problem in generating the storm file.", exception);
        }
    }


    public void appendInfo(File tempFile, long counter) throws ServiceException {
        try (PrintWriter writer = new PrintWriter(new File(workSpaceDir, stormFileName))) {
            FileReader fileReader = new FileReader(tempFile);
            writer.println("# Storm file for scenario: " + scenarioName);
            writer.println("# Date built: " + today + " (Version 2.3)");
            writer.println("# State: " + stateId);
            writer.println("# Climate Station: " + climatestationId);
            writer.println(counter + " # The number of rain events");
            writer.println("0 # Breakpoint data? (0 for no, 1 for yes)");
            writer.println("#  id     day  month  year  Rain   Dur    Tp     Ip");
            writer.println("#                           (mm)   (h)");
            try (BufferedReader bufferedReader = new BufferedReader(fileReader)) {
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    writer.println(line);
                }
            }
        } catch (IOException exception) {
            throw new ServiceException("Problem in generating the storm file.", exception);
        }
    }


    public void generateRunFile() throws ServiceException {
        try (PrintWriter writer = new PrintWriter(new File(workSpaceDir, runFileName))) {
            writer.println(parameterFileName + ", " + stormFileName + ", "
                    + summaryFileName + ", \"" + scenarioName + "\", 0, 2, y, y, n, n, y");
            writer.close();
        } catch (FileNotFoundException exception) {
            throw new ServiceException("Problem in generating the run file.", exception);
        }
    }


    public void appendToSumFile(double avgYearlyPrecip) throws ServiceException {
        int counter = 0;
        File summaryFile = new File(workSpaceDir, summaryFileName);
        File tempSummaryFile = new File(workSpaceDir, "temp_" + summaryFileName);
        try (PrintWriter writer = new PrintWriter(tempSummaryFile)) {
            FileReader fileReader = new FileReader(summaryFile);
            writer.println("     -ANNUAL-AVERAGES-");
            writer.println();
            writer.println("Avg-Precipitation(mm/year)=   " + avgYearlyPrecip);
            try (BufferedReader bufferedReader = new BufferedReader(fileReader)) {
                String line;
                for (int i = 0; i < 3; i++) {
                    bufferedReader.readLine();
                    counter++;
                }
                while ((line = bufferedReader.readLine()) != null) {
                    counter++;
                    String[] test = line.split("\\s+");
                    for (int i = 1; i < test.length; i++) {
                        switch (counter) {
                            case 13:
                                if (i == 1) {
                                    writer.print(String.format("%-28s", "Precipitation(mm)"));
                                    i = 2;
                                } else {
                                    writer.print(String.format("%-28s", test[i]));
                                }
                                break;
                            default:
                                writer.print(String.format("%-28s", test[i]));
                                break;
                        }
                    }
                    writer.println();
                }
                bufferedReader.close();
            }
            summaryFile.delete();
            writer.close();
            tempSummaryFile.renameTo(summaryFile);
        } catch (IOException exception) {
            throw new ServiceException("Problem in editing the summary file.", exception);
        }
    }
}
