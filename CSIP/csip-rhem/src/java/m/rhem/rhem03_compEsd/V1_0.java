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
package m.rhem.rhem03_compEsd;

import gisobjects.db.GISEngine;
import static gisobjects.db.GISEngineFactory.createGISEngine;
import gisobjects.GISObject;
import gisobjects.GISObjectException;
import gisobjects.GISObjectFactory;
import csip.ModelDataService;
import csip.ServiceException;
import csip.annotations.Polling;
import csip.annotations.Resource;
import csip.utils.JSONUtils;
import java.io.IOException;
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
import org.codehaus.jettison.json.JSONObject;
import rhem.utils.DBQueries;
import rhem.utils.DBResources;
import static rhem.utils.DBResources.SDM_REST;

/**
 * RHEM-03: Get Soil Component, Ecological Site, Soil Component Surface Texture
 * Class, Soil Component Slope Length and Steepness for RHEM Evaluation Area
 *
 * @version 1.0
 * @author Rumpal Sidhu
 */
@Name("RHEM-03: Get Soil Component, Ecological Site, Soil Component Surface Texture Class, "
        + "Soil Component Slope Length and Steepness for RHEM Evaluation Area.")
@Description("Get map unit, soil component and ecological site information for "
        + "RHEM evaluation site. Get surface soil texture class relevant to "
        + "the soil component associated with a RHEM evaluation site within "
        + "an AoA. Get slope length and steepness relevant to the soil "
        + "component associated with a RHEM evaluation site within an AoA.")
@Path("m/rhem/getrhemsoilcomponent/1.0")
@Polling(first = 10000, next = 2000)
@Resource(from = DBResources.class)
public class V1_0 extends ModelDataService {

    private int aoaId, rhemSiteId;
    private JSONObject rhemSiteGeometryPoint;
    private ArrayList<Mapunit> mapunitList = new ArrayList<>();

    @Override
    public void preProcess() throws ServiceException {
        aoaId = getIntParam("AoAID");
        rhemSiteId = getIntParam("rhem_site_id");
        rhemSiteGeometryPoint = getParam("rhem_site_geometry");
    }

    @Override
    public void doProcess() throws ServiceException {
        try (Connection sdmConnection = getResourceJDBC(SDM_REST);
                Connection crdbConnection = getResourceJDBC(DBResources.CRDB);) {
            GISEngine gisEngine = createGISEngine(sdmConnection);
            GISObject geometry = GISObjectFactory.createGISObject(rhemSiteGeometryPoint, gisEngine);
            getMapunitValues(sdmConnection, geometry);
            try (Connection esdConnection = getResourceJDBC(DBResources.ESD);) {
                getComponentValues(sdmConnection, esdConnection);
            }
            getSurfaceTextureClass(sdmConnection, crdbConnection);
            getSurfaceTextureClass(sdmConnection, crdbConnection);
        } catch (ServiceException | SQLException | GISObjectException | IOException | JSONException se) {
            throw new ServiceException(se);
        }
    }

    private void getMapunitValues(Connection sdmConnection, GISObject geometry) throws SQLException {
        try (Statement statement = sdmConnection.createStatement();) {
            String mukey = null, musym = null, muname = null;

            try (ResultSet resultSet = statement.executeQuery(DBQueries.RHEM03Query01(geometry))) {
                while (resultSet.next()) {
                    mukey = resultSet.getString("mukey");
                    musym = resultSet.getString("musym");
                    muname = resultSet.getString("muname");
                }
            }
            if (mukey != null) {
                ArrayList<Component> componentList = new ArrayList<>();
                try (ResultSet resultSet = statement.executeQuery(DBQueries.RHEM03Query02(mukey))) {
                    while (resultSet.next()) {
                        componentList.add(new Component(resultSet.getString("cokey"),
                                resultSet.getString("compname"),
                                resultSet.getInt("comppct_r"),
                                resultSet.getInt("slope_l"),
                                resultSet.getInt("slope_r"),
                                resultSet.getInt("slope_h")));
                    }
                }
                mapunitList.add(new Mapunit(musym, muname, componentList));
            }
        }
    }

    private void getComponentValues(Connection connection, Connection esdConnection) throws SQLException {
        try (Statement statement = connection.createStatement();) {
            for (Mapunit mapunit : mapunitList) {
                for (Component component : mapunit.getComponentList()) {
                    ArrayList<EcologicalSite> ecoList = new ArrayList<>();
                    String ecoclassid = null;
                    try (ResultSet resultSet = statement.executeQuery(DBQueries.RHEM03Query03(component.getCokey()))) {
                        while (resultSet.next()) {
                            ecoclassid = resultSet.getString("ecoclassid");
                        }
                    }
                    if (ecoclassid != null) {
                        try (Statement esdStmt = esdConnection.createStatement();
                                ResultSet resultSet = esdStmt.executeQuery(DBQueries.RHEM03Query04(ecoclassid));) {

                            while (resultSet.next()) {
                                ecoList.add(new EcologicalSite(ecoclassid,
                                        resultSet.getString("es_range_name")));
                            }
                        }
                    }
                    component.setEcoSiteList(ecoList);
                }
            }
        }
    }

    private void getSurfaceTextureClass(Connection connection, Connection rhemConnection) throws SQLException {
        try (Statement statement = connection.createStatement();) {
            for (Mapunit mapunit : this.mapunitList) {
                for (Component component : mapunit.getComponentList()) {
                    ArrayList<SurfaceTexture> surfaceTextureList = new ArrayList<>();
                    try (ResultSet resultSet = statement.executeQuery(DBQueries.RHEM03Query05(component.cokey));) {
                        while (resultSet.next()) {
                            String texcl = resultSet.getString("texcl");
                            try (Statement stmt = rhemConnection.createStatement();
                                    ResultSet rst = stmt.executeQuery(DBQueries.RHEM03Query06(texcl));) {
                                while (rst.next()) {
                                    int textureId = rst.getInt("texture_subclass_id");
                                    String textureClass = rst.getString("text_abreviation");
                                    String textureClassLabel = rst.getString("text_label");
                                    surfaceTextureList.add(new SurfaceTexture(texcl,
                                            textureId, textureClass, textureClassLabel));
                                }
                            }
                        }
                    }
                    component.setSurfaceTextureList(surfaceTextureList);
                }
            }
        }
    }

    @Override
    public void postProcess() throws ServiceException {
        try {
            putResult("AoAID", this.aoaId, "Area of Analysis Identifier");
            putResult("rhem_site_id", this.rhemSiteId, "RHEM Evaluation Site Identifier");

            JSONArray mapUnitArray = new JSONArray();
            for (Mapunit mapunit : this.mapunitList) {
                JSONArray mapunitArr = new JSONArray();
                mapunitArr.put(JSONUtils.dataDesc("musym", mapunit.getMusym(), "Mapunit Symbol"));
                mapunitArr.put(JSONUtils.dataDesc("muname", mapunit.getMuname(), "Mapunit Name"));

                JSONArray componentArray = new JSONArray();
                for (Component component : mapunit.getComponentList()) {
                    JSONArray componentArr = new JSONArray();
                    componentArr.put(JSONUtils.dataDesc("cokey", component.getCokey(), "Soil Component Key"));
                    componentArr.put(JSONUtils.dataDesc("compname", component.getCompName(), "Soil Component Name"));
                    componentArr.put(JSONUtils.dataDesc("comppct_r", component.getComppctR(), "Soil Component Percentage Relative Value"));
                    componentArr.put(JSONUtils.dataDesc("slopepct_l", component.getSlopePctL(), "Slope Percent – Low Value"));
                    componentArr.put(JSONUtils.dataDesc("slopepct_r", component.getSlopePctR(), "Slope Percent – Representative Value"));
                    componentArr.put(JSONUtils.dataDesc("slopepct_h", component.getSlopePctH(), "Slope Percent – High Value"));

                    JSONArray ecoSiteArray = new JSONArray();
                    for (EcologicalSite ecoSite : component.getEcoSiteList()) {
                        JSONArray ecoSiteArr = new JSONArray();
                        ecoSiteArr.put(JSONUtils.dataDesc("es_id", ecoSite.getEsId(), "Ecological Site Identifier"));
                        ecoSiteArr.put(JSONUtils.dataDesc("es_range_name", ecoSite.getEsRangeName(), "Ecological Site Name"));
                        ecoSiteArray.put(JSONUtils.dataDesc("ecological_site", ecoSiteArr, "Ecological Site"));
                    }
                    componentArr.put(JSONUtils.dataDesc("ecological_site_list", ecoSiteArray, "Ecological Site List"));

                    JSONArray surfaceTestureArray = new JSONArray();
                    for (SurfaceTexture texture : component.getSurfaceTextureList()) {
                        JSONArray surfaceTextureArr = new JSONArray();
                        surfaceTextureArr.put(JSONUtils.dataDesc("texcl", texture.getTexcl(), "SSURGO Texture Class"));
                        surfaceTextureArr.put(JSONUtils.dataDesc("rhem_texture_id ", texture.getTextureId(), "RHEM Texture Class Identifier"));
                        surfaceTextureArr.put(JSONUtils.dataDesc("rhem_texture_class ", texture.getTextureClass(), "RHEM Texture Class"));
                        surfaceTextureArr.put(JSONUtils.dataDesc("rhem_texture_class_label ", texture.getTextureClassLabel(), "RHEM Texture Class Label"));
                        surfaceTestureArray.put(JSONUtils.dataDesc("surface_texture_class", surfaceTextureArr, "Soil Component Surface Texture Class"));
                    }
                    componentArr.put(JSONUtils.dataDesc("surface_texture_class_list", surfaceTestureArray, "Soil Component Surface Texture Class List"));
                    componentArray.put(JSONUtils.dataDesc("component", componentArr, "Component"));
                }
                mapunitArr.put(JSONUtils.dataDesc("component_list", componentArray, "Component List"));
                mapUnitArray.put(JSONUtils.dataDesc("mapunit", mapunitArr, "Mapunit"));
            }
            putResult("mapunit_list", mapUnitArray, "Mapunit List");
        } catch (JSONException ex) {
            throw new ServiceException(ex);
        }
    }

    /**
     * Get the full JSON parameter record.
     *
     * @param name
     * @return the JSON record.
     */
    private JSONObject getParam(String name) throws ServiceException {
        JSONObject p = getParamMap().get(name);
        if (p == null) {
            throw new ServiceException("Parameter not found: '" + name + "'");
        }
        return p;
    }

    static class Mapunit {

        protected String musym;
        protected String muname;
        protected ArrayList<Component> componentList;

        public Mapunit(String musym, String muname,
                ArrayList<Component> componentList) {
            this.musym = musym;
            this.muname = muname;
            this.componentList = componentList;
        }

        public String getMusym() {
            return this.musym;
        }

        public String getMuname() {
            return this.muname;
        }

        public ArrayList<Component> getComponentList() {
            return this.componentList;
        }
    }

    static class Component {

        protected String cokey;
        protected String compName;
        protected int comppctR;
        //slope length and steepness
        protected int slopePctL;
        protected int slopePctR;
        protected int slopePctH;

        protected ArrayList<EcologicalSite> ecoSiteList;
        protected ArrayList<SurfaceTexture> surfaceTextureList;

        public Component(String cokey, String compName, int comppctR,
                int slopePctL, int slopePctR, int slopePctH) {
            this.cokey = cokey;
            this.compName = compName;
            this.comppctR = comppctR;
            this.slopePctL = slopePctL;
            this.slopePctR = slopePctR;
            this.slopePctH = slopePctH;
        }

        public String getCokey() {
            return this.cokey;
        }

        public String getCompName() {
            return this.compName;
        }

        public int getComppctR() {
            return this.comppctR;
        }

        public int getSlopePctL() {
            return this.slopePctL;
        }

        public int getSlopePctR() {
            return this.slopePctR;
        }

        public int getSlopePctH() {
            return this.slopePctH;
        }

        public ArrayList<EcologicalSite> getEcoSiteList() {
            return this.ecoSiteList;
        }

        public ArrayList<SurfaceTexture> getSurfaceTextureList() {
            return this.surfaceTextureList;
        }

        public void setEcoSiteList(ArrayList<EcologicalSite> list) {
            this.ecoSiteList = list;
        }

        public void setSurfaceTextureList(ArrayList<SurfaceTexture> list) {
            this.surfaceTextureList = list;
        }

    }

    static class EcologicalSite {

        protected String esId;
        protected String esRangeName;

        public EcologicalSite(String esId, String esRangeName) {
            this.esId = esId;
            this.esRangeName = esRangeName;
        }

        public String getEsId() {
            return esId;
        }

        public String getEsRangeName() {
            return esRangeName;
        }
    }

    static class SurfaceTexture {

        protected String texcl;
        protected int textureId;
        protected String textureClass;
        protected String textureClassLabel;

        public SurfaceTexture(String texcl, int textureId, String textureClass,
                String textureClassLabel) {
            this.texcl = texcl;
            this.textureId = textureId;
            this.textureClass = textureClass;
            this.textureClassLabel = textureClassLabel;
        }

        public String getTexcl() {
            return this.texcl;
        }

        public int getTextureId() {
            return this.textureId;
        }

        public String getTextureClass() {
            return this.textureClass;
        }

        public String getTextureClassLabel() {
            return this.textureClassLabel;
        }
    }
}
