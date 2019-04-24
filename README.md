
# RHEM Batch Script Description

The purpose of the RHEM Batch Script is to allow users to run the RHEM model in batch mode.
This script depends on the RHEM CSIP web service.  More information can be found describing
this web service at the following URL: https://alm.engr.colostate.edu/cb/item/27202

This script will also be available for downloaded from the following website: 
  https://apps.tucson.ars.ag.gov/rhem/docs


# Requirements

In order to run the RHEM Batch Script, you will need:
 * An internet enabled computer
 * Python
 * Excel  

# Installation and running script

**NOTE:** If you are planning on doing large batch runs (greater than 2,0000) please let us know.  You can email gerardo.armendariz@ars.usda.gov 

 1) Install Python 3 in your system

 2) Install the required Python packages by running the pip installer using the 
    provided requirements.txt file

    * Run:   pip install -r requirements.txt

 3) Once Python and the required packages have been installed, you can enter the scenario
    information into the Excel spreadsheet.

    * Note that in order to get the climate station identifiers, you can use the RHEM Web Tool. 
      Please refer to the Climate Station section: https://apps.tucson.ars.ag.gov/rhem/tool

 4) Make sure that the RHEM_template.xlsx workbook is in the same location as the RHEM Batch Script

 5) Run the RHEM Batch Script:  python rhem_batch_csip.py