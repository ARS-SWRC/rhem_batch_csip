###################################
##
##  Purpose: This script will run the RHEM model in batch mode using the CSIP service
##           hosted by CSU
## 
##  Author: Gerardo Armendariz
##  Modified: 8/15/2018
##  
import sys
import requests
import json
import time
import itertools
from openpyxl import load_workbook
from openpyxl import Workbook

### MODIFY THESE VALUES TO RUN RHEM
RHEM_WORKBOOK = load_workbook("RHEM_template.xlsx",data_only=True)
SCENARIO_COUNT = 10

CSIP_RHEM_URL = 'http://csip.engr.colostate.edu:8083/csip-rhem/m/rhem/runrhem/1.0'

#####
#  Main function 
def main():
    t0 = time.time()
    openAndRunRHEMScenarios()
    #testSaveScenarioSummaryResults()
    #saveScenarioSummaryResultsToExcel(1)
    t1 = time.time()
    
    total = t1-t0
    
    print("Execution time: " + str(total))

#####
#  Crate the input request for the CSIP RHEM service
def createInputFile(AoAID, rhem_site_id, scenarioname, scenariodescription, units, stateid, climatestationid, soiltexture, moisturecontent, slopelength, slopeshape, slopesteepness, bunchgrasscanopycover, forbscanopycover, shrubscanopycover, sodgrasscanopycover, basalcover, rockcover, littercover, cryptogamscover):
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
                "value": ''' + str(moisturecontent) + '''
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
            }
        ]
    }'''
    return request_data

####
# Open a RHEM scenario from the workbook and submit run job
def openAndRunRHEMScenarios():
    ws = RHEM_WORKBOOK.active

    row_index = 1
    for row in ws.iter_rows(min_row=2, max_col=20, max_row=SCENARIO_COUNT):
        print("Running scenario: " + row[2].value)
        # crete the input file/request to run the curren scenario
        request_data = createInputFile(row[0].value, row[1].value,  row[2].value, row[3].value, row[4].value, row[5].value, row[6].value, row[7].value, row[8].value, row[9].value, row[10].value, row[11].value, row[12].value, row[13].value, row[14].value, row[15].value, row[16].value, row[17].value, row[18].value,row[19].value)
        #line = (row[0].value, row[1].value,  row[2].value, row[3].value, row[4].value, row[5].value, row[6].value, row[7].value, row[8].value, row[9].value, row[10].value, row[11].value, row[12].value, row[13].value, row[14].value, row[15].value, row[16].value, row[17].value, row[18].value,row[19].value)
        #print(' - '.join(map(str,line)))
        # run the RHEM CSIP Service
        rhem_response = runRHEMCSIPService(request_data, row_index)
        row_index = row_index + 1

####
# Run the CSIP RHEM service based on single scenario inputs
def runRHEMCSIPService(request_data, row_index):
     # request run from the RHEM CSIP service
    csip_rhem_response = requests.post(CSIP_RHEM_URL, data=request_data)
    rhem_run_response = json.loads(csip_rhem_response.content.decode('utf-8'))

    # save parameter file
    saveScenarioParameterFile(rhem_run_response)
    # save summary file
    saveScenarioSummaryResults(rhem_run_response)
    # save data back to spreadsheet
    saveScenarioSummaryResultsToExcel(rhem_run_response, row_index)

####
# Save the response parameter file
def saveScenarioParameterFile(rhem_run_response):
    # parameter file
    rhem_parameters_result_url = rhem_run_response["result"][16]["value"]
    rhem_parameters_result = requests.get(rhem_parameters_result_url)
    with open(rhem_run_response["result"][16]["name"], 'wb') as file:
        file.write(str(rhem_parameters_result.text).encode())

####
# Save the response summary file
def saveScenarioSummaryResults(rhem_run_response):
    # summary file
    rhem_summary_result_url = rhem_run_response["result"][18]["value"]
    rhem_summary_result = requests.get(rhem_summary_result_url)
    with open(rhem_run_response["result"][18]["name"], 'wb') as file:
        file.write(str(rhem_summary_result.text).encode())

####
# Save the simulation summary results to the Excel sheet
def saveScenarioSummaryResultsToExcel(rhem_run_response,row_index):
    ws = RHEM_WORKBOOK.active
    
    with open(rhem_run_response["result"][18]["name"], 'rb') as file:
        index = 3
        for line in itertools.islice(file, 2, 6):
            if index == 3:
                ws.cell(row=row_index + 1, column=22).value = str(line).split("=")[1].strip()
            elif index == 4:
                ws.cell(row=row_index + 1, column=23).value = str(line).split("=")[1].strip()
            elif index == 5:
                ws.cell(row=row_index + 1, column=24).value = str(line).split("=")[1].strip()
            elif index == 6:
                ws.cell(row=row_index + 1, column=25).value = str(line).split("=")[1].strip()
            index = index + 1
    
    RHEM_WORKBOOK.save("RHEM_template.xlsx")

####
# The main function
if __name__ == "__main__":
    sys.exit(main())
