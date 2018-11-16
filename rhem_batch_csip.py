###################################
##
##  Purpose: This script will run the RHEM model in batch mode using the CSIP service
##           hosted by CSU
##
##  NOTE: This project is now being tracked with Git. It will be updated often.
## 
##  Author: Gerardo Armendariz
##  Modified: 11/14/2018
##  
import os
import sys
import requests
import json
import time
import itertools
from openpyxl import load_workbook
from openpyxl import Workbook

###### MODIFY THESE VALUES TO RUN RHEM
SCENARIO_COUNT = 3     # this is the number of scenarios (rows) to run
OUTPUT_DIR = "output"  # this is the output directory where paramter and summary files will be saved
######


try:
    RHEM_WORKBOOK = load_workbook("RHEM_template.xlsx",data_only=True)
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
    print("Execution time: " + str(total))


####
# Create repository to save RHEM scenario outputs
#
def createOutputDirectory():
    try:
        if not os.path.exists(OUTPUT_DIR):
            os.makedirs(OUTPUT_DIR)
    except:
        print("The output directory for RHEM outputs to be stored could not be created!")

####
# Open a RHEM scenario from the workbook and submit run job in order to under
#
def openAndRunRHEMScenarios():
    ws = RHEM_WORKBOOK.active
    error_message = ""
    row_index = 1
    for row in ws.iter_rows(min_row=2, max_col=20, max_row=SCENARIO_COUNT + 1):
        # total canopy cover = Bunch + Forbs + Shrubs + Sod
        #print("%s %s %s %s" % (row[9].value, row[10].value, row[11].value, row[12].value))
        total_canopy = float(row[9].value) + float(row[10].value) + float(row[11].value) + float(row[12].value)
        # total groudn cover = Basal + Rock + Litter + Biological Crusts    
        total_ground = float(row[13].value) + float(row[14].value) + float(row[15].value) + float(row[16].value)
        if total_canopy > 100 or total_ground > 100:
            error_message = "Skipping scenario " + row[0].value + ". Total canopy cover and total ground cover cannot exceed 100%"
            print(error_message)
            ws.cell(row=row_index + 1, column=19).value = error_message
            pass
        else:
            print("Running scenario: " + row[0].value)
            # crete the input file/request to run the curren scenario
            request_data = createInputFile(row_index, row_index,  row[0].value, row[1].value, row[2].value, row[3].value, row[4].value, row[5].value, 25, row[6].value, row[7].value, row[8].value, row[9].value, row[10].value, row[11].value, row[12].value, row[13].value, row[14].value, row[15].value,row[16].value)
            # run the RHEM CSIP Service
            rhem_response = runRHEMCSIPService(request_data, row_index)

        row_index = row_index + 1

####
# Run the CSIP RHEM service based on single scenario inputs
#
def runRHEMCSIPService(request_data, row_index):
     # request run from the RHEM CSIP service
    csip_rhem_response = requests.post(CSIP_RHEM_URL, data=request_data)
    rhem_run_response = json.loads(csip_rhem_response.content.decode('utf-8'))
    #print(rhem_run_response["result"])
   
    # save parameter file
    saveScenarioParameterFile(rhem_run_response)
    # save summary file
    saveScenarioSummaryResults(rhem_run_response)
    # save data back to spreadsheet
    saveScenarioSummaryResultsToExcel(rhem_run_response, row_index)

####
# Save the response parameter file
#
def saveScenarioParameterFile(rhem_run_response):
    # parameter file
    rhem_parameters_result_url = rhem_run_response["result"][16]["value"]
    rhem_parameters_result = requests.get(rhem_parameters_result_url)
    with open(os.path.join(OUTPUT_DIR,rhem_run_response["result"][16]["name"]), 'wb') as file:
        file.write(str(rhem_parameters_result.text).encode())

####
# Save the response summary filels
#
def saveScenarioSummaryResults(rhem_run_response):
    # summary file
    rhem_summary_result_url = rhem_run_response["result"][18]["value"]
    rhem_summary_result = requests.get(rhem_summary_result_url)
    with open(os.path.join(OUTPUT_DIR,rhem_run_response["result"][18]["name"]), 'wb') as file:
        file.write(str(rhem_summary_result.text).encode())

####
# Save the simulation summary results to the Excel sheet
#
def saveScenarioSummaryResultsToExcel(rhem_run_response,row_index):
    ws = RHEM_WORKBOOK.active

    with open(os.path.join(OUTPUT_DIR, rhem_run_response["result"][18]["name"]), 'rb') as file:
        index = 3
        for line in itertools.islice(file, 2, 6):
            #print(line)
            if index == 3: # Avg. Precipitation
                ws.cell(row=row_index + 1, column=19).value = str(line).split("=")[1].rstrip("'").strip(r'\n')
            elif index == 4: # Avg. Runoff
                ws.cell(row=row_index + 1, column=20).value = str(line).split("=")[1].rstrip("'").strip(r'\n')
            elif index == 5: # Avg. Soil Loss
                ws.cell(row=row_index + 1, column=21).value = str(line).split("=")[1].rstrip("'").strip(r'\n')
            elif index == 6: # Avg. Sediment Yield
                ws.cell(row=row_index + 1, column=22).value = str(line).split("=")[1].rstrip("'").strip(r'\n')
            index = index + 1
    
    RHEM_WORKBOOK.save("RHEM_template.xlsx")

#####
#  Crate the input request for the CSIP RHEM service
#
def createInputFile(AoAID, rhem_site_id, scenarioname, scenariodescription, units, stateid, climatestationid, soiltexture, soilmoisture, slopelength, slopeshape, slopesteepness, bunchgrasscanopycover, forbscanopycover, shrubscanopycover, sodgrasscanopycover, basalcover, rockcover, littercover, cryptogamscover):
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
            }
        ]
    }'''
    return request_data

####
# The main function
#
if __name__ == "__main__":
    sys.exit(main())
