/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package m.rhem.model;

/**
 *
 * @author rumpal
 */
public class AoA {

    protected int aoaId;
    protected int rhemSiteId;
    protected String scenarioName;
    protected String scenarioDescription;
    protected int unit;
    protected String stateId;
    protected String climatestationId;
    protected String soilTexture;
    protected double slopeLength;
    protected String slopeShape;
    protected double slopeSteepness;
    protected double moisturecontent;
    protected double bunchGgrassCanopyCover;
    protected double forbsCanopyCover;
    protected double shrubsCanopyCover;
    protected double sodGrassCanopyCover;
    protected double basalCover;
    protected double rockCover;
    protected double litterCover;
    protected double cryptogamsCover;

    public AoA(int aoaId, int rhemSiteId, String scenarioName,
            String scenarioDescription, int unit, String stateId,
            String climatestationId, String soilTexture, double slopeLength,
            String slopeShape, double slopeSteepness,
            double bunchGgrassCanopyCover, double forbsCanopyCover,
            double shrubsCanopyCover, double sodGrassCanopyCover, double basalCover,
            double rockCover, double litterCover, double cryptogamsCover, double moisturecontent) {
        this.aoaId = aoaId;
        this.rhemSiteId = rhemSiteId;
        this.scenarioName = scenarioName;
        this.scenarioDescription = scenarioDescription;
        this.unit = unit;
        this.stateId = stateId;
        this.climatestationId = climatestationId;
        this.soilTexture = soilTexture;
        this.slopeLength = slopeLength;
        this.slopeShape = slopeShape;
        this.slopeSteepness = slopeSteepness / 100.0;
        this.bunchGgrassCanopyCover = bunchGgrassCanopyCover / 100.0;
        this.forbsCanopyCover = forbsCanopyCover / 100.0;
        this.shrubsCanopyCover = shrubsCanopyCover / 100.0;
        this.sodGrassCanopyCover = sodGrassCanopyCover / 100.0;
        this.basalCover = basalCover / 100.0;
        this.rockCover = rockCover / 100.0;
        this.litterCover = litterCover / 100.0;
        this.cryptogamsCover = cryptogamsCover / 100.0;
        moisturecontent = moisturecontent / 100.0;
    }

    public int getAoaId() {
        return aoaId;
    }

    public int getRhemSiteId() {
        return rhemSiteId;
    }

    public String getScenarioName() {
        return scenarioName;
    }

    public String getStateId() {
        return stateId;
    }

    public String getClimateStationId() {
        return climatestationId;
    }
}
