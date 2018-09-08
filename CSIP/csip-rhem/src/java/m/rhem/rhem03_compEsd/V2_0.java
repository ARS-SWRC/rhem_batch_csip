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

import csip.Config;
import gisobjects.db.GISEngine;
import gisobjects.GISObject;
import gisobjects.GISObjectException;
import gisobjects.GISObjectFactory;
import csip.ModelDataService;
import csip.ServiceException;
import csip.SessionLogger;
import csip.annotations.Polling;
import csip.annotations.Resource;
import csip.utils.JSONUtils;
import static gisobjects.db.GISEngineFactory.createGISEngine;
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
import static rhem.utils.DBResources.CRDB;
import soils.MapUnit;
import soils.db.SOILS_DATA;
import soils.db.SOILS_DB_Factory;

/**
 * RHEM-03: Get Soil Component, Ecological Site, Soil Component Surface Texture
 * Class, Soil Component Slope Length and Steepness for RHEM Evaluation Area
 *
 * @version 2.0
 * @author Rumpal Sidhu
 */
@Name("RHEM-03: Get Soil Component, Ecological Site, Soil Component Surface Texture Class, "
        + "Soil Component Slope Length and Steepness for RHEM Evaluation Area.")
@Description("Get map unit, soil component and ecological site information for "
        + "RHEM evaluation site. Get surface soil texture class relevant to "
        + "the soil component associated with a RHEM evaluation site within "
        + "an AoA. Get slope length and steepness relevant to the soil "
        + "component associated with a RHEM evaluation site within an AoA.")
@Path("m/rhem/getrhemsoilcomponent/2.0")
@Polling(first = 10000, next = 2000)
@Resource(from = DBResources.class)
@Resource(from = soils.db.DBResources.class)

public class V2_0 extends ModelDataService {

    private int aoaId, rhemSiteId;
    private JSONObject rhemSiteGeometryPoint;
    private ArrayList<Mapunit> mapunitList = new ArrayList<>();
    private AoA aoa;

    @Override
    public void preProcess() throws ServiceException {
        aoaId = getIntParam("AoAID");
        rhemSiteId = getIntParam("rhem_site_id");
        rhemSiteGeometryPoint = getParam("rhem_site_geometry");
    }

    @Override
    public void doProcess() throws ServiceException, SQLException, GISObjectException, JSONException, IOException, Exception {
        try (SOILS_DATA soilsDb = SOILS_DB_Factory.createEngine(getClass(), LOG, Config.getString("soils.gis.database.source"));
                Connection crdb = getResourceJDBC(CRDB);) {
            try (GISEngine gisEngine = createGISEngine(crdb)) {
                GISObject geometry = GISObjectFactory.createGISObject(rhemSiteGeometryPoint, gisEngine);

                aoa = new AoA(soilsDb, geometry, LOG);
                aoa.findIntersectedMapUnits();
                aoa.findAllComponentsData_Basic();
                aoa.findEcoClasses();
                aoa.findAllTextureData();
                getData(crdb);
            }
        }
    }

    private void getData(Connection crdb) throws ServiceException, SQLException {
        for (MapUnit mapUnit : aoa.getMapUnits().values()) {
            ArrayList<Component> componentList = new ArrayList<>();
            for (soils.Component component : mapUnit.components().values()) {
                Component comp = new Component(component.cokey(), component.compname(),
                        component.comppct_r(), component.slope_l(), component.slope_r(), component.slope_h());

                ArrayList<EcologicalSite> ecoList = new ArrayList<>();
                for (soils.Coecoclass coEcoClass : component.ecoClasses().values()) {
                    String ecoclassid = coEcoClass.ecoclassid();
                    if (ecoclassid != null && ecoclassid.startsWith("R")) {
                        try (Connection esdConnection = getResourceJDBC(DBResources.ESD);
                                Statement esdStmt = esdConnection.createStatement();
                                ResultSet resultSet = esdStmt.executeQuery(DBQueries.RHEM03Query04(ecoclassid));) {
                            while (resultSet.next()) {
                                ecoList.add(new EcologicalSite(ecoclassid,
                                        resultSet.getString("es_range_name")));
                            }
                        }
                    }
                }
                comp.setEcoSiteList(ecoList);

                ArrayList<SurfaceTexture> surfaceTextureList = new ArrayList<>();
                for (soils.Horizon horizon : component.horizons.values()) {
                    if (horizon.hzdept_r() == 0) {
                        for (soils.TextureGroup textureGroup : horizon.textureGroups.values()) {
                            for (soils.Texture texture : textureGroup.textures.values()) {
                                String texcl = texture.texcl();
                                try (Statement statement = crdb.createStatement();
                                        ResultSet resultSet = statement.executeQuery(DBQueries.RHEM03Query06(texcl));) {
                                    while (resultSet.next()) {
                                        int textureId = resultSet.getInt("texture_subclass_id");
                                        String textureClass = resultSet.getString("text_abreviation");
                                        String textureClassLabel = resultSet.getString("text_label");
                                        surfaceTextureList.add(new SurfaceTexture(texcl,
                                                textureId, textureClass, textureClassLabel));
                                    }
                                }
                            }
                        }
                    }
                }
                comp.setSurfaceTextureList(surfaceTextureList);
                componentList.add(comp);
            }
            componentList.sort((o1, o2) -> Double.compare(o2.getComppctR(), o1.getComppctR()));
            mapunitList.add(new Mapunit(mapUnit.musym(), mapUnit.muname(), componentList));
        }
    }

    @Override
    public void postProcess() throws ServiceException, JSONException {
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
                    ecoSiteArray.put(ecoSiteArr);
                }
                componentArr.put(JSONUtils.data("Ecological Site List", ecoSiteArray));

                JSONArray surfaceTestureArray = new JSONArray();
                for (SurfaceTexture texture : component.getSurfaceTextureList()) {
                    JSONArray surfaceTextureArr = new JSONArray();
                    surfaceTextureArr.put(JSONUtils.dataDesc("texcl", texture.getTexcl(), "Texture Class"));
                    surfaceTextureArr.put(JSONUtils.dataDesc("rhem_texture_id ", texture.getTextureId(), "RHEM Texture Class Identifier"));
                    surfaceTextureArr.put(JSONUtils.dataDesc("rhem_texture_class ", texture.getTextureClass(), "RHEM Texture Class"));
                    surfaceTextureArr.put(JSONUtils.dataDesc("rhem_texture_class_label ", texture.getTextureClassLabel(), "RHEM Texture Class Label"));
                    surfaceTestureArray.put(surfaceTextureArr);
                }
                componentArr.put(JSONUtils.data("Surface Texture Class List", surfaceTestureArray));
                componentArray.put(componentArr);
            }
            mapunitArr.put(JSONUtils.data("Components", componentArray));
            mapUnitArray.put(mapunitArr);
        }
        putResult("MapUnit List", mapUnitArray);
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

    static class AoA extends soils.AoA {

        public AoA(SOILS_DATA soilsDb, GISObject aoaShape, SessionLogger Log) throws GISObjectException, ServiceException, SQLException {
            super(soilsDb, aoaShape, Log);
        }
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
        protected double comppctR;
        //slope length and steepness
        protected double slopePctL, slopePctR, slopePctH;

        protected ArrayList<EcologicalSite> ecoSiteList;
        protected ArrayList<SurfaceTexture> surfaceTextureList;

        public Component(String cokey, String compName, double comppctR,
                double slopePctL, double slopePctR, double slopePctH) {
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

        public double getComppctR() {
            return this.comppctR;
        }

        public double getSlopePctL() {
            return this.slopePctL;
        }

        public double getSlopePctR() {
            return this.slopePctR;
        }

        public double getSlopePctH() {
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
