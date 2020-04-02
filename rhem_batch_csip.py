###################################
##
##  Purpose: This script will run the RHEM model in batch mode using the RHEM CSIP web service
##           hosted by CSU
## 
##  Author: Gerardo Armendariz
##  
import os
import sys
import requests
import json
import time
import itertools
from openpyxl import load_workbook
from openpyxl import Workbook

###### MODIFY THESE VALUES TO RUN RHEM BATCH SCRIPT
###### Note: If you are planning on doing large batch runs (greater than 2,0000) please let us know. 
######       You can email gerardo.armendariz@usda.gov
SCENARIO_COUNT = 1                    # the number of scenarios (rows) to run
OUTPUT_DIR = "output"                 # the output directory where paramter and summary files will be saved
WORKBOOK_Name = "RHEM_template.xlsx"  # the workbook used for inputs and results
###################################################

try:
    RHEM_WORKBOOK = load_workbook(WORKBOOK_Name,data_only=True)
except:
    print("The Excel template file was not found.")

CSIP_RHEM_URL = 'http://csip.engr.colostate.edu:8083/csip-rhem/m/rhem/runrhem/1.0'

#####
#  Main function
#
def main():
    # create an output directory
    createOutputDirectory()
    # start timer
    t0 = time.time()
    # run RHEM for all scenarios in Excel template
    openAndRunRHEMScenarios()
    # end timer
    t1 = time.time()
    
    total = t1 - t0
    print("Execution time: " + str(total) + " seconds")

####
# Creates file system repository to save RHEM scenario outputs
#
def createOutputDirectory():
    try:
        if not os.path.exists(OUTPUT_DIR):
            os.makedirs(OUTPUT_DIR)
    except:
        print("The output directory for RHEM outputs to be stored could not be created!")

####
# Opens the RHEM scenarios from the workbook, validates inputs, runs CSIP web service, and saves results
#
def openAndRunRHEMScenarios():
    ws = RHEM_WORKBOOK.active
    error_message = ""
    row_index = 1
    for row in ws.iter_rows(min_row=2, max_col=20, max_row=SCENARIO_COUNT + 1):
        ## Validate for empty input values
        inputs = (row[0].value, row[2].value, row[3].value, row[4].value, row[5].value, row[7].value, row[8].value, row[9].value, row[10].value,row[11].value,row[12].value,row[13].value,row[14].value, row[15].value,row[16].value, row[17].value)
        if any(s is None for s in inputs):
            error_message = "Please make sure that you have entered all required inputs to run the current scenario"
            print(error_message)
            ws.cell(row=row_index + 1, column=25).value = error_message
            row_index = row_index + 1
            RHEM_WORKBOOK.save(WORKBOOK_Name)
            continue

        ## Validate cover values
        # total canopy cover = Bunch + Forbs + Shrubs + Sod
        total_canopy = round(float(row[10].value) + float(row[11].value) + float(row[12].value) + float(row[13].value), 2)
        # total ground cover = Basal + Rock + Litter + Biological Crusts    
        total_ground = round(float(row[14].value) + float(row[15].value) + float(row[16].value) + float(row[17].value), 2)
        
        if total_canopy > 100 or total_ground > 100:
            error_message = "Skipping scenario " + str(row[0].value) + ". Total canopy cover and total ground cover cannot exceed 100%"
            print(error_message)
            ws.cell(row=row_index + 1, column=25).value = error_message
            row_index = row_index + 1
            RHEM_WORKBOOK.save(WORKBOOK_Name)
            continue
        else:
            if ws.cell(row=row_index + 1, column=22).value is None:
                print("Running scenario: " + str(ws.cell(row=row_index + 1, column=1).value))
                # Validation: replace periods for underscores in scenario names
                scenario_name = str(row[0].value).replace(".","_")
                
                # default SAR to 0 in order for the service to run
                SAR = row[6].value
                if SAR is None:   
                    SAR = 0

                # crete the input file/request to run the curren scenario
                request_data = createInputFile(row_index, row_index,  scenario_name, row[1].value, row[2].value, row[3].value, row[4].value, row[5].value, SAR, 25, row[7].value, row[8].value, row[9].value, row[10].value, row[11].value, row[12].value, row[13].value, row[14].value, row[15].value,row[16].value, row[17].value)
                
                rhem_response = runRHEMCSIPService(request_data, row_index)

            row_index = row_index + 1    

    
####
# Runs the RHEM CSIP web service for a single scenario 
#
def runRHEMCSIPService(request_data, row_index):
    ws = RHEM_WORKBOOK.active
     # request run from the RHEM CSIP service
    headers = {'Content-Type': 'application/json'}
    csip_rhem_response = requests.post(CSIP_RHEM_URL, data=request_data, headers=headers)
    rhem_run_response = json.loads(csip_rhem_response.content.decode('utf-8'))

    if "error" in rhem_run_response["metainfo"]:
        error_message = rhem_run_response["metainfo"]["error"]
        print(error_message)
        ws.cell(row=row_index + 1, column=25).value = error_message
    else:
        saveScenarioParameterFile(rhem_run_response)
        saveScenarioSummaryResults(rhem_run_response)
        saveScenarioSummaryResultsToExcel(rhem_run_response, row_index)
    RHEM_WORKBOOK.save(WORKBOOK_Name)


####
# Saves the input parameter file
#
def saveScenarioParameterFile(rhem_run_response):
    rhem_parameters_result_url = rhem_run_response["result"][17]["value"]
    rhem_parameters_result = requests.get(rhem_parameters_result_url)
    with open(os.path.join(OUTPUT_DIR,rhem_run_response["result"][17]["name"]), 'wb') as file:
        file.write(str(rhem_parameters_result.text).encode())


####
# Saves the response summary files
#
def saveScenarioSummaryResults(rhem_run_response):
    # summary file
    rhem_summary_result_url = rhem_run_response["result"][19]["value"]
    rhem_summary_result = requests.get(rhem_summary_result_url)
    with open(os.path.join(OUTPUT_DIR,rhem_run_response["result"][19]["name"]), 'wb') as file:
        file.write(str(rhem_summary_result.text).encode())


####
# Saves the simulation output summary results to the Excel sheet
#
def saveScenarioSummaryResultsToExcel(rhem_run_response,row_index):
    ws = RHEM_WORKBOOK.active

    # save the TDS (total dissolved solids) value
    tds = rhem_run_response["result"][16]["value"]
    ws.cell(row=row_index + 1, column=24).value = str(tds)

    with open(os.path.join(OUTPUT_DIR, rhem_run_response["result"][19]["name"]), 'rb') as file:
        index = 3
        for line in itertools.islice(file, 2, 6):
            if index == 3: # Avg. Precipitation
                ws.cell(row=row_index + 1, column=20).value = str(line).split("=")[1].rstrip("'").strip(r'\n')
            elif index == 4: # Avg. Runoff
                ws.cell(row=row_index + 1, column=21).value = str(line).split("=")[1].rstrip("'").strip(r'\n')
            elif index == 5: # Avg. Soil Loss
                ws.cell(row=row_index + 1, column=22).value = str(line).split("=")[1].rstrip("'").strip(r'\n')
            elif index == 6: # Avg. Sediment Yield
                ws.cell(row=row_index + 1, column=23).value = str(line).split("=")[1].rstrip("'").strip(r'\n')
            index = index + 1
    
    RHEM_WORKBOOK.save(WORKBOOK_Name)

#####
#  Creates the input request for the CSIP RHEM service
#
def createInputFile(AoAID, rhem_site_id, scenarioname, scenariodescription, units, stateid, climatestationid, soiltexture, sar, soilmoisture, slopelength, slopeshape, slopesteepness, bunchgrasscanopycover, forbscanopycover, shrubscanopycover, sodgrasscanopycover, basalcover, rockcover, littercover, cryptogamscover):
    request_data = '''{
        "metainfo": {},
        "parameter": [
            {
                "name": "AoAID",
                "description": "Area of Analysis Identifier",
                "value": ''' + str(AoAID)  + '''
            },
            {
                "name": "rhem_site_id",
                "description": "RHEM Evaluation Site Identifier",
                "value": ''' + str(rhem_site_id) + '''
            },
            {
                "name": "scenarioname",
                "description": "RHEM Scenario Name",
                "value": "''' + str(scenarioname) + '''"
            },
            {
                "name": "scenariodescription",
                "description": "RHEM Scenario description",
                "value": "''' + str(scenariodescription) + '''"
            },
            {
                "name": "units",
                "description": "RHEM Scenario Unit of Measure, 1 = metric and 2 = English",
                "value": ''' + str(units) + '''
            },
            {
                "name": "stateid",
                "description": " State Abbreviation",
                "value": "''' + str(stateid) + '''"
            },
            {
                "name": "climatestationid",
                "description": "Climate Station Identification Number",
                "value": "''' + str(climatestationid) + '''"
            },
            {
                "name": "soiltexture",
                "description": "Surface Soil Texture Class Label",
                "value": "''' + str(soiltexture) + '''"
            },
            {
                "name": "moisturecontent",
                "description": "",
                "value": ''' + str(soilmoisture) + ''',
            },
            {
                "name": "slopelength",
                "description": "Slope Length",
                "value": ''' + str(slopelength) + ''',
                "unit": "ft/m",
                "min": 0.01,
                "max": "394 feet/120 meters"
            },
            {
                "name": "slopeshape",
                "description": "Slope Shape",
                "value": "''' + str(slopeshape) + '''"
            },
            {
                "name": "slopesteepness",
                "description": "Slope Steepness",
                "value": ''' + str(slopesteepness) + ''',
                "unit": "%",
                "min": 0.01,
                "max": 100
            },
            {
                "name": "bunchgrasscanopycover",
                "description": "Bunchgrass Foliar Cover Percent",
                "value": ''' + str(bunchgrasscanopycover) + ''',
                "unit": "%",
                "min": 0.01,
                "max": 100
            },
            {
                "name": "forbscanopycover",
                "description": "Forbs and Annuals Foliar Cover Percent",
                "value": ''' + str(forbscanopycover) + ''',
                "unit": "%",
                "min": 0.01,
                "max": 100
            },
            {
                "name": "shrubscanopycover",
                "description": "Shrub Foliar Cover Percent",
                "value": ''' + str(shrubscanopycover) + ''',
                "unit": "%",
                "min": 0.01,
                "max": 100
            },
            {
                "name": "sodgrasscanopycover",
                "description": "Sodgrass Foliar Cover Percent",
                "value": ''' + str(sodgrasscanopycover) + ''',
                "unit": "%",
                "min": 0.01,
                "max": 100
            },
            {
                "name": "basalcover",
                "description": "Plant Basal Cover Percent",
                "value": ''' + str(basalcover) + ''',
                "unit": "%",
                "min": 0.01,
                "max": 100
            },
            {
                "name": "rockcover",
                "description": "Rock Cover Percent",
                "value": ''' + str(rockcover) + ''',
                "unit": "%",
                "min": 0.01,
                "max": 100
            },
            {
                "name": "littercover",
                "description": "Litter Cover Percent",
                "value": ''' + str(littercover) + ''',
                "unit": "%",
                "min": 0.01,
                "max": 100
            },
            {
                "name": "cryptogamscover",
                "Description": "Cryptogam Cover Percent",
                "value": ''' + str(cryptogamscover) + ''',
                "unit": "%",
                "min": 0.01,
                "max": 100
            },
            {
            "name": "sar",
            "Description": "Sodium Adsorption Ratio",
            "value": ''' + str(sar) + ''',
            "unit": "%",
            "min": 0,
            "max": 50
            }
        ]
    }'''
    return request_data

####
# The main function
#
if __name__ == "__main__":
    sys.exit(main())
