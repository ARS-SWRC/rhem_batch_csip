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
package m.rhem.rhem02_getclimatestations;

import csip.ModelDataService;
import csip.ServiceException;
import csip.annotations.Polling;
import csip.annotations.Resource;
import csip.utils.JSONUtils;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import javax.ws.rs.Path;
import csip.annotations.Description;
import csip.annotations.Name;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import rhem.utils.DBQueries;
import rhem.utils.RHEMUtils;
import rhem.utils.DBResources;
import static rhem.utils.RHEMUtils.MONTH_NAMES_LIST;

/**
 * RHEM-02:Get Climate Stations
 *
 * @version 1.0
 * @author Rumpal Sidhu
 */
@Name("RHEM-02: Get Climate Stations")
@Description("Returns a list of climate stations associated with the state "
        + "selected by the user. Information associated with each climate "
        + "station will be station name, latitude, longitude, number of "
        + "years of observed data, elevation (feet) and 300 year average "
        + "and monthly precipitation amounts (millimeters).")
@Path("m/rhem/getclimatestations/1.0")
@Polling(first = 10000, next = 2000)
@Resource(from = DBResources.class)
public class V1_0 extends ModelDataService {

    private String stateId;
    private ClimateStationStates climateStationStates;

    @Override
    public void preProcess() throws ServiceException {
        stateId = getStringParam("stateid");
    }

    @Override
    public void doProcess() throws ServiceException {
        try (Connection connection = getResourceJDBC(DBResources.CRDB);
                Statement statement = connection.createStatement()) {

            try (ResultSet resultSet = statement.executeQuery(DBQueries.RHEM02Query01(stateId))) {
                while (resultSet.next()) {
                    climateStationStates = new ClimateStationStates(stateId,
                            resultSet.getString("state_name"),
                            resultSet.getDouble("latitude"),
                            resultSet.getDouble("longitude"),
                            resultSet.getInt("zoom"));
                }
            }

            try (ResultSet resultSet = statement.executeQuery(DBQueries.RHEM02Query02(stateId))) {
                ArrayList<ClimateStation> climateStationsList = new ArrayList();
                while (resultSet.next()) {
                    double[] monthlyPrecip = new double[12];
                    for (int i = 0; i< 12; i++) {
                        monthlyPrecip[i] = resultSet.getDouble(MONTH_NAMES_LIST[i].toLowerCase().substring(0,3) + "_precip_mm");
                    }

                    climateStationsList.add(new ClimateStation(
                            resultSet.getString("station_id"),
                            resultSet.getString("station"),
                            resultSet.getDouble("latitude"),
                            resultSet.getDouble("longitude"),
                            resultSet.getInt("years"),
                            resultSet.getInt("elevation"),
                            resultSet.getDouble("avg_yearly_precip_mm"),
                            monthlyPrecip));
                }
                climateStationStates.addClimateStationsList(climateStationsList);
            }
        } catch (SQLException se) {
            throw new ServiceException(se);
        }
    }

    @Override
    public void postProcess() throws ServiceException {
        try {
            putResult("stateid", climateStationStates.getStateId(), "State abbreviation of climate station location");
            putResult("state_name", climateStationStates.getStateName(), "State name");
            putResult("latitude", climateStationStates.getStateLatitude(), "Central latitude of the state");
            putResult("longitude", climateStationStates.getStateLongitude(), "Central longitude of the state");
            putResult("zoom", climateStationStates.getZoom(), "Zoom level utilized to display state map on UI");

            JSONArray climateStationArray = new JSONArray();
            for (ClimateStation climateStation : climateStationStates.getClimateStationsList()) {
                JSONArray innerArray = new JSONArray();
                innerArray.put(JSONUtils.dataDesc("station_id", climateStation.getStationId(), "Climate station identification number"));
                innerArray.put(JSONUtils.dataDesc("station_name", climateStation.getStationName(), "Climate station name"));
                innerArray.put(JSONUtils.dataDesc("lat", climateStation.getStationLatitude(), "Climate station latitude"));
                innerArray.put(JSONUtils.dataDesc("long", climateStation.getStationLongitude(), "Climate station longitude"));
                innerArray.put(JSONUtils.dataDesc("year_recorded", climateStation.getYearRecorded(), "Number of years of recorded observations for climate station"));
                innerArray.put(JSONUtils.dataDesc("elevation_ft", climateStation.getElevation(), "Climate station elevation"));
                innerArray.put(JSONUtils.dataDesc("avg_yearly_precip_mm", RHEMUtils.roundValues(climateStation.getAvgYearlyPrecip(), 2), "300 year average annual precipitation (mm)"));
                for (int i = 0; i < 12; i++) {
                    innerArray.put(JSONUtils.dataDesc(MONTH_NAMES_LIST[i].substring(0, 3).toLowerCase() + "_precip_mm",
                            RHEMUtils.roundValues(climateStation.getMonthlyPrecip()[i], 2), MONTH_NAMES_LIST[i] + " estimated monthly average precipitation (mm)"));
                }
                climateStationArray.put(JSONUtils.dataDesc("climate_stations", innerArray, "Climate station attributes"));
            }
            putResult("climate_station_list", climateStationArray, "Climate Station List");
        } catch (JSONException ex) {
            throw new ServiceException(ex);
        }
    }

    static class ClimateStationStates {

        protected String stateId;
        protected String stateName;
        protected double latitude;
        protected double longitude;
        protected int zoom;
        protected ArrayList<ClimateStation> climateStationsList;

        public ClimateStationStates(String stateId, String stateName, double latitude,
                double longitude, int zoom) {
            this.stateId = stateId;
            this.stateName = stateName;
            this.latitude = latitude;
            this.longitude = longitude;
            this.zoom = zoom;
        }

        public String getStateId() {
            return this.stateId;
        }

        public String getStateName() {
            return this.stateName;
        }

        public double getStateLatitude() {
            return this.latitude;
        }

        public double getStateLongitude() {
            return this.longitude;
        }

        public int getZoom() {
            return this.zoom;
        }

        public ArrayList<ClimateStation> getClimateStationsList() {
            return this.climateStationsList;
        }

        public void addClimateStationsList(ArrayList<ClimateStation> list) {
            this.climateStationsList = list;
        }

    }

    static class ClimateStation {

        protected String stationId;
        protected String stationName;
        protected double latitude;
        protected double longitude;
        protected int yearRecorded;
        protected int elevation_ft;
        protected double avgYearlyPrecip_mm;
        protected double[] monthlyPrecip_mm;

        public ClimateStation(String stationId, String stationName, double latitude,
                double longitude, int yearRecorded, int elevation,
                double avg_yearly_precip_mm, double[] monthlyPrecip_mm) {

            this.stationId = stationId;
            this.stationName = stationName;
            this.latitude = latitude;
            this.longitude = longitude;
            this.yearRecorded = yearRecorded;
            this.elevation_ft = elevation;
            this.avgYearlyPrecip_mm = avg_yearly_precip_mm;
            this.monthlyPrecip_mm = monthlyPrecip_mm;
        }

        public String getStationId() {
            return this.stationId;
        }

        public String getStationName() {
            return this.stationName;
        }

        public double getStationLatitude() {
            return this.latitude;
        }

        public double getStationLongitude() {
            return this.longitude;
        }

        public int getYearRecorded() {
            return this.yearRecorded;
        }

        public int getElevation() {
            return this.elevation_ft;
        }

        public double getAvgYearlyPrecip() {
            return this.avgYearlyPrecip_mm;
        }

        public double[] getMonthlyPrecip() {
            return this.monthlyPrecip_mm;
        }

    }
}
