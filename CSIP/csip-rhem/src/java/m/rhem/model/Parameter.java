/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package m.rhem.model;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import rhem.utils.DBQueries;

/**
 *
 * @author rumpal
 */
public class Parameter {

    protected String clen;
    protected String diams;
    protected String density;

    protected String len, chezy, rchezy;
    protected String sl, sx;
    protected String cv, sat, kss, komega, kcm, ke, adf, alf, bare;

    protected String g, dist, por;
    protected String fract;

    private AoA aoa;
    private double slopeLength;


    public Parameter(String diams, String density, String len,
            String chezy, String rchezy, String sl, String sx, String cv,
            String sat, String kss, String komega, String kcm, String ke,
            String adf, String alf, String bare, String g, String dist,
            String por, String fract) {
        this.diams = diams;
        this.density = density;
        this.len = len;
        this.chezy = chezy;
        this.rchezy = rchezy;
        this.sl = sl;
        this.sx = sx;
        this.cv = cv;
        this.sat = sat;
        this.kss = kss;
        this.komega = komega;
        this.kcm = kcm;
        this.ke = ke;
        this.adf = adf;
        this.alf = alf;
        this.bare = bare;
        this.g = g;
        this.dist = dist;
        this.por = por;
        this.fract = fract;

        this.clen = String.valueOf(Double.parseDouble(len) * 2.5);
    }


    public Parameter(AoA aoa) {
        this.cv = "1.00";
        this.sat = "0.25;";
        this.komega = "0.000007747";
        this.kcm = "0.00029936430000";
        this.adf = "0.0";
        this.alf = "0.8";
        this.bare = "0.23";
        this.aoa = aoa;
        this.slopeLength = aoa.slopeLength;
    }


    public void computeParameters(Connection connection) throws SQLException {
        // canopy cover for grass
//        double grasscanopycover = aoa.bunchGgrassCanopyCover + aoa.forbsCanopyCover + aoa.sodGrassCanopyCover;
        // TOTAL CANOPY COVER
        double totalcanopycover = aoa.bunchGgrassCanopyCover + aoa.forbsCanopyCover + aoa.shrubsCanopyCover + aoa.sodGrassCanopyCover;
        // TOTAL GROUND COVER
        double totalgroundcover = aoa.rockCover + aoa.basalCover + aoa.litterCover + aoa.cryptogamsCover;

        if (aoa.unit == 2) {
            slopeLength = aoa.slopeLength * 0.3048;
        } else {
            slopeLength = aoa.slopeLength;
        }

        len = String.valueOf(slopeLength);

        computeChezyValue();
        computeSlSxValues();
        computeClenValue();
        getValuesFmDb(connection);
        computeKeValue(totalcanopycover);
        computeKssValue(totalcanopycover, totalgroundcover);
    }


    public void computeChezyValue() {
        double ft = (-1 * 0.109) + (1.425 * aoa.litterCover)
                + (0.442 * aoa.rockCover) + (1.764 * (aoa.basalCover
                + aoa.cryptogamsCover)) + (2.068 * aoa.slopeSteepness);
        ft = Math.pow(10, ft);
        rchezy = chezy = String.valueOf(Math.pow((8 * 9.8) / ft, 0.5));
    }


    public void computeSlSxValues() {
        switch (aoa.slopeShape) {
            case "Uniform":
                sl = aoa.slopeSteepness + ", " + aoa.slopeSteepness;
                sx = 0.00 + ", " + 1.00;
                break;
            case "Convex":
                sl = 0.001 + ", " + (aoa.slopeSteepness * 2);
                sx = 0.00 + ", " + 1.00;
                break;
            case "Concave":
                sl = (aoa.slopeSteepness * 2) + ", " + 0.001;
                sx = 0.00 + ", " + 1.00;
                break;
            case "S-shaped":
                sl = 0.001 + ", " + (aoa.slopeSteepness * 2) + ", " + 0.001;
                sx = 0.00 + ", " + 0.50 + ", " + 1.00;
                break;
        }
    }


    public void computeClenValue() {
        clen = String.valueOf(slopeLength * 2.5);
    }


    public void getValuesFmDb(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            try (ResultSet resultSet = statement.executeQuery(DBQueries.RunRHEMQuery01(aoa.soilTexture))) {
                while (resultSet.next()) {
                    double diam1 = resultSet.getDouble("clay_diameter");
                    double diam2 = resultSet.getDouble("silt_diameter");
                    double diam3 = resultSet.getDouble("small_aggregates_diameter");
                    double diam4 = resultSet.getDouble("large_aggregates_diameter");
                    double diam5 = resultSet.getDouble("sand_diameter");
                    diams = diam1 + " " + diam2 + " " + diam3 + " " + diam4 + " " + diam5;
                    double density1 = resultSet.getDouble("clay_specific_gravity");
                    double density2 = resultSet.getDouble("silt_specific_gravity");
                    double density3 = resultSet.getDouble("small_aggregates_specific_gravity");
                    double density4 = resultSet.getDouble("large_aggregates_specific_gravity");
                    double density5 = resultSet.getDouble("sand_specific_gravity");
                    density = density1 + " " + density2 + " " + density3 + " " + density4 + " " + density5;
                    g = String.valueOf(resultSet.getDouble("mean_matric_potential"));
                    dist = String.valueOf(resultSet.getDouble("pore_size_distribution"));
                    por = String.valueOf(resultSet.getDouble("mean_porosity"));
                    double fract1 = resultSet.getDouble("clay_fraction");
                    double fract2 = resultSet.getDouble("silt_fraction");
                    double fract3 = resultSet.getDouble("small_aggregates_fraction");
                    double fract4 = resultSet.getDouble("large_aggregates_fraction");
                    double fract5 = resultSet.getDouble("sand_fraction");
                    fract = fract1 + " " + fract2 + " " + fract3 + " " + fract4 + " " + fract5;
                }
            }
        }
    }


    private void computeKeValue(double totalcanopycover) {
        double Keb = 0;
        switch (aoa.soilTexture) {
            case "Sand":
                Keb = 24 * Math.exp(0.3483 * (aoa.basalCover + aoa.litterCover));
                break;
            case "Loamy Sand":
                Keb = 10 * Math.exp(0.8755 * (aoa.basalCover + aoa.litterCover));
                break;
            case "Sandy Loam":
                Keb = 5 * Math.exp(1.1632 * (aoa.basalCover + aoa.litterCover));
                break;
            case "Loam":
                Keb = 2.5 * Math.exp(1.5686 * (aoa.basalCover + aoa.litterCover));
                break;
            case "Silt Loam":
                Keb = 1.2 * Math.exp(2.0149 * (aoa.basalCover + aoa.litterCover));
                break;
            case "Silt":
                Keb = 1.2 * Math.exp(2.0149 * (aoa.basalCover + aoa.litterCover));
                break;
            case "Sandy Clay Loam":
                Keb = 0.80 * Math.exp(2.1691 * (aoa.basalCover + aoa.litterCover));
                break;
            case "Clay Loam":
                Keb = 0.50 * Math.exp(2.3026 * (aoa.basalCover + aoa.litterCover));
                break;
            case "Silty Clay Loam":
                Keb = 0.40 * Math.exp(2.1691 * (aoa.basalCover + aoa.litterCover));
                break;
            case "Sandy Clay":
                Keb = 0.30 * Math.exp(2.1203 * (aoa.basalCover + aoa.litterCover));
                break;
            case "Silty Clay":
                Keb = 0.25 * Math.exp(1.7918 * (aoa.basalCover + aoa.litterCover));
                break;
            case "Clay":
                Keb = 0.2 * Math.exp(1.3218 * (aoa.basalCover + aoa.litterCover));
                break;
        }
        double weightedKe = 0;

        // calculate weighted Ke and Kss values for the vegetation types that have non-zero values
        if (totalcanopycover != 0) {
            weightedKe = weightedKe + ((aoa.shrubsCanopyCover / totalcanopycover) * (Keb * 1.2));
            weightedKe = weightedKe + ((aoa.sodGrassCanopyCover / totalcanopycover) * (Keb * 0.8));
            weightedKe = weightedKe + ((aoa.bunchGgrassCanopyCover / totalcanopycover) * (Keb * 1.0));
            weightedKe = weightedKe + ((aoa.forbsCanopyCover / totalcanopycover) * (Keb * 1.0));
        } else {
            weightedKe = Keb;
        }
        ke = String.valueOf(weightedKe);
    }


    public void computeKssValue(double totalcanopycover, double totalgroundcover) {
        // Kss variables
        double Kss_Seg_Bunch, Kss_Seg_Sod, Kss_Seg_Shrub, Kss_Seg_Shrub_0, Kss_Seg_Forbs;
        double Kss_Average, Kss_Final;

        // 1)
        //   a) CALCULATE KSS FOR EACH VEGETATION COMMUNITY USING TOTAL FOLIAR COVER
        //		A)   BUNCH GRASS
        if (totalgroundcover < 0.475) {
            Kss_Seg_Bunch = 4.154 + 2.5535 * aoa.slopeSteepness
                    - 2.547 * totalgroundcover - 0.7822 * totalcanopycover;
            Kss_Seg_Bunch = Math.pow(10, Kss_Seg_Bunch);

            Kss_Seg_Sod = 4.2169 + 2.5535 * aoa.slopeSteepness
                    - 2.547 * totalgroundcover - 0.7822 * totalcanopycover;
            Kss_Seg_Sod = Math.pow(10, Kss_Seg_Sod);

            Kss_Seg_Shrub = 4.2587 + 2.5535 * aoa.slopeSteepness - 2.547 * totalgroundcover - 0.7822 * totalcanopycover;
            Kss_Seg_Shrub = Math.pow(10, Kss_Seg_Shrub);

            Kss_Seg_Forbs = 4.1106 + 2.5535 * aoa.slopeSteepness - 2.547 * totalgroundcover - 0.7822 * totalcanopycover;
            Kss_Seg_Forbs = Math.pow(10, Kss_Seg_Forbs);

            Kss_Seg_Shrub_0 = 4.2587 + 2.5535 * aoa.slopeSteepness - 2.547 * totalgroundcover;
            Kss_Seg_Shrub_0 = Math.pow(10, Kss_Seg_Shrub_0);

        } else {
            Kss_Seg_Bunch = 3.1726975 + 2.5535 * aoa.slopeSteepness
                    - 0.4811 * totalgroundcover - 0.7822 * totalcanopycover;
            Kss_Seg_Bunch = Math.pow(10, Kss_Seg_Bunch);

            Kss_Seg_Sod = 3.2355975 + 2.5535 * aoa.slopeSteepness
                    - 0.4811 * totalgroundcover - 0.7822 * totalcanopycover;
            Kss_Seg_Sod = Math.pow(10, Kss_Seg_Sod);

            Kss_Seg_Shrub = 3.2773975 + 2.5535 * aoa.slopeSteepness - 0.4811 * totalgroundcover - 0.7822 * totalcanopycover;
            Kss_Seg_Shrub = Math.pow(10, Kss_Seg_Shrub);

            Kss_Seg_Forbs = 3.1292975 + 2.5535 * aoa.slopeSteepness - 0.4811 * totalgroundcover - 0.7822 * totalcanopycover;
            Kss_Seg_Forbs = Math.pow(10, Kss_Seg_Forbs);

            Kss_Seg_Shrub_0 = 3.2773975 + 2.5535 * aoa.slopeSteepness - 0.4811 * totalgroundcover;
            Kss_Seg_Shrub_0 = Math.pow(10, Kss_Seg_Shrub_0);
        }
        if (totalcanopycover > 0 && totalcanopycover < 0.02) {
            Kss_Average = totalcanopycover / 0.02 * ((aoa.shrubsCanopyCover / totalcanopycover) * Kss_Seg_Shrub
                    + (aoa.sodGrassCanopyCover / totalcanopycover) * Kss_Seg_Sod
                    + (aoa.bunchGgrassCanopyCover / totalcanopycover) * Kss_Seg_Bunch
                    + (aoa.forbsCanopyCover / totalcanopycover) * Kss_Seg_Forbs)
                    + (0.02 - totalcanopycover) / 0.02 * Kss_Seg_Shrub_0;
        } else {
            Kss_Average = (aoa.shrubsCanopyCover / totalcanopycover) * Kss_Seg_Shrub
                    + (aoa.sodGrassCanopyCover / totalcanopycover) * Kss_Seg_Sod
                    + (aoa.bunchGgrassCanopyCover / totalcanopycover) * Kss_Seg_Bunch
                    + (aoa.forbsCanopyCover / totalcanopycover) * Kss_Seg_Forbs;
        }

        // 3) CALCULATE KSS USED FOR RHEM
        if (totalcanopycover == 0) {
            if (totalgroundcover < 0.475) {
                Kss_Final = 4.2587 + 2.5535 * aoa.slopeSteepness - 2.547 * totalgroundcover;
                Kss_Final = Math.pow(10, Kss_Final);
            } else {
                Kss_Final = 3.2773975 + 2.5535 * aoa.slopeSteepness - 0.4811 * totalgroundcover;
                Kss_Final = Math.pow(10, Kss_Final);
            }
        } else if (totalgroundcover < 0.475) {
            Kss_Final = totalgroundcover / 0.475 * Kss_Average + (0.475 - totalgroundcover) / 0.475 * Kss_Seg_Shrub;
        } else {
            Kss_Final = Kss_Average;
        }
        Kss_Final = (Kss_Final * 1.3) * 2.0;
        kss = String.valueOf(Kss_Final);
    }


    public String getClen() {
        return clen;
    }


    public String getDiams() {
        return diams;
    }


    public String getDensity() {
        return density;
    }


    public String getLen() {
        return len;
    }


    public String getChezy() {
        return chezy;
    }


    public String getRchezy() {
        return rchezy;
    }


    public String getSl() {
        return sl;
    }


    public String getSx() {
        return sx;
    }


    public String getCv() {
        return cv;
    }


    public String getSat() {
        return sat;
    }


    public String getKss() {
        return kss;
    }


    public String getKomega() {
        return komega;
    }


    public String getKcm() {
        return kcm;
    }


    public String getKe() {
        return ke;
    }


    public String getAdf() {
        return adf;
    }


    public String getAlf() {
        return alf;
    }


    public String getBare() {
        return bare;
    }


    public String getG() {
        return g;
    }


    public String getDist() {
        return dist;
    }


    public String getPor() {
        return por;
    }


    public String getFract() {
        return fract;
    }

}
