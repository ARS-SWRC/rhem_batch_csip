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
package rhem.utils;

import gisobjects.GISObject;

/**
 *
 * @author rumpal
 */
public class DBQueries {

    public static String RunRHEMQuery01(String soilTexture) {
        String query = "SELECT clay_diameter, silt_diameter, "
                + "small_aggregates_diameter, large_aggregates_diameter, "
                + "sand_diameter, clay_specific_gravity, "
                + "silt_specific_gravity, small_aggregates_specific_gravity, "
                + "large_aggregates_specific_gravity, sand_specific_gravity, "
                + "mean_matric_potential, pore_size_distribution, mean_porosity, "
                + "clay_fraction, silt_fraction, small_aggregates_fraction, "
                + "large_aggregates_fraction, sand_fraction "
                + "FROM rhem.soil_texture_table "
                + "WHERE class_name ='" + soilTexture + "';";

        return query;
    }

    public static String RHEM01Query02(String climateStationId) {
        String query = "SELECT avg_yearly_precip_mm "
                + "FROM rhem.d_rhem_climate_stations_avg_300yr_est_rain "
                + "WHERE station_id = '" + climateStationId + "';";
        return query;
    }

    public static String RHEM02Query01(String stateId) {
        String query = "SELECT state_name, latitude, longitude, zoom "
                + "FROM rhem.d_rhem_climate_station_states "
                + "WHERE state_id = '" + stateId + "';";

        return query;
    }

    public static String RHEM02Query02(String stateId) {
        String query = "SELECT s.state, s.station, s.station_id, s.latitude, "
                + "s.longitude, s.years, s.elevation, "
                + "est.avg_yearly_precip_mm, est.jan_precip_mm, "
                + "est.feb_precip_mm, est.mar_precip_mm, est.apr_precip_mm, "
                + "est.may_precip_mm, est.jun_precip_mm, est.jul_precip_mm, "
                + "est.aug_precip_mm, est.sep_precip_mm, est.oct_precip_mm, "
                + "est.nov_precip_mm, est.dec_precip_mm "
                + "FROM rhem.d_rhem_climate_stations s "
                + "JOIN rhem.d_rhem_climate_stations_avg_300yr_est_rain est "
                + "ON (s.station_id = est.station_id) "
                + "WHERE s.state = '" + stateId + "';";

        return query;
    }

    public static String RHEM03Query01(GISObject geometry) {
        String query = "SELECT mupoly.mukey, mapunit.musym, mapunit.muname "
                + "FROM dbo.mupolygon AS mupoly "
                + "WITH (index(SI_mupolygon_24876)) "
                + "JOIN dbo.mapunit AS mapunit "
                + "ON mupoly.mukey = mapunit.mukey "
                + "INNER JOIN dbo.legend ON mapunit.lkey=dbo.legend.lkey "
                + "WHERE mupoly.mupolygongeo.STIntersects( geometry::STGeomFromText('"
                + geometry.getGeometry() + "', 4326)) = 1 AND legend.areatypename like 'Non-MLRA%';";
        return query;
    }

    public static String RHEM03Query02(String mukey) {
        String query = "SELECT cokey, compname, comppct_r, slope_l, slope_r, slope_h "
                + "FROM dbo.component "
                + "WHERE mukey ='" + mukey + "' ORDER BY comppct_r DESC;";
        return query;
    }

    public static String RHEM03Query03(String cokey) {
        String query = "SELECT ecoclassid "
                + "FROM dbo.coecoclass "
                + "WHERE cokey = '" + cokey
                + "' AND ecoclassid LIKE 'R%';";

        return query;
    }

    public static String RHEM03Query04(String ecoclassid) {
        String query = "SELECT concat(range_site_primary_name, ' ', "
                + "range_site_secondary_name, ' ', range_site_tertiary_name) "
                + "AS es_range_name "
                + "FROM esd.ecological_sites "
                + "WHERE concat(es_type, es_mlra, es_mlru, es_site_number, es_state) = '"
                + ecoclassid + "';";
        return query;
    }

    public static String RHEM03Query05(String cokey) {
        String query = "SELECT texcl FROM dbo.chtexture "
                + "WHERE chtgkey IN "
                + "(SELECT chtgkey FROM dbo.chtexturegrp WHERE chkey IN "
                + "(SELECT chkey FROM dbo.chorizon WHERE cokey = '"
                + cokey + "' AND hzdept_r = 0));";
        return query;
    }

    public static String RHEM03Query06(String texcl) {
        String query = "SELECT d2.texture_subclass_id, d3.text_abreviation, d3.text_label "
                + "FROM rhem.d_rhem_text_lookup d1 "
                + "JOIN rhem.d_rhem_texture_class_subclass d2 ON (d1.text_id = d2.text_subclass_id) "
                + "JOIN rhem.d_rhem_text_lookup d3 ON (d2.text_class_id = d3.text_id) "
                + "WHERE d1.text_label = '" + texcl + "' AND d2.obsolete = 'false';";

        return query;
    }

    public static String RHEM04Query01() {
        String query = "SELECT text_id, text_abreviation, text_label "
                + "FROM rhem.d_rhem_text_lookup "
                + "WHERE text_id "
                + "IN (SELECT DISTINCT(text_class_id) "
                + "FROM rhem.d_rhem_texture_class_subclass "
                + "WHERE obsolete = 'false');";
        return query;
    }

    public static String RHEM05Query01() {
        String query = "SELECT choice_id, choice_label "
                + "FROM rhem.d_rhem_slope_shape "
                + "WHERE obsolete = 'false';";
        return query;
    }

    public static String RHEM07Query01(String climatestationId) {
        String query = "SELECT avg_yearly_precip_mm "
                + "FROM rhem.d_rhem_climate_stations_avg_300yr_est_rain "
                + "WHERE station_id = '" + climatestationId + "';";
        return query;
    }

}
