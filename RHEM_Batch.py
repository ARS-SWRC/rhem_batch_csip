# output file names:
# if you want to save output file for each run, use different file names for every simulation
# if not, use the same name, get output parameter values before it gets overwritten by the next run
# same for the input files:
# if you do not need to save every input file, then using the same file name is fine
# the example here is using different names for every simulation run

# if run RHEM in windows
# copy RHEM.exe to the folder contains input files and only use filename (no path needed)
# or chdir the folder where RHEM.exe exists and use filename with path
# if call RHEM in lunix: navigate to all input files or use filename with path

# to run (my) RHEM executable:
# read in 'kin.fil', which contains the input/output files and other settings
# run command line and write output file
# to run the RHEM_300y_webface executable:
# it should be a different command line. May not need 'kin.fil' and function run_rhem

# you will need to write your own script to read the output variables:
# open and scane the textfile
# navigate to the location or find variable using string searching method
# store in csv file


# excel file contains all RHEM parameters for each simulation
RHEM_par_excel_input = r''
# simulation folder
fRHEMpool = r''
# output excel for RHEM simulation results
excel_out = r''


def Run_RHEM_batch():

    # list of prefiles (filenames with or without path)
    ListClimate = write_climatefile(RHEM_par_excel_input)
    # list of parfiles
    ListPar = write_parfile(RHEM_par_excel_input)
    # list of output file names
    ListOutfile = List_of_outputfile(RHEM_par_excel_input)

    # add other input to executable in zip function
    for climfile, parfile, outfile in zip(ListClimate, ListPar, ListOutfile):

        # if navigate to the folder of simulation, do not need path in the file name
        os.chdir(fRHEMpool)

        if os.path.exists(outfile):
            os.remove(outfile)

        run_RHEM('RHEM command', parfile, climfile, outfile)

        if os.path.exists(outfile):
            try:
                rainfall, sediment, runoff, peak = get_desired_RHEM_output_variables(outfile)
            except:
                rainfall, sediment, runoff, peak = -9999, -9999, -9999, -9999
        else:
            rainfall, sediment, runoff, peak = -999, -999, -999, -999

        simRainfall.append(rainfall)
        simSediment.append(sediment)
        simRunoff.append(runoff)
        simPeak.append(peak)

    # write all values to one excel file
    write_results_2excel(excel_out, simRainfall, simSediment, simRunoff, simPeak)


def get_desired_RHEM_output_variables(outfile):

    '''open output file and find the value for desired RHEM output '''

    return rainfall, sediment, runoff, peak


def write_climatefile(RHEM_par_excel_input):

    '''read the simulation name and write climate file for each simulation
        I think it's a good idea to have all climate files for all climate station stored in one folder
        and simply select based on user's choise '''

    List_of_climatefiles = []
    all_scenarios = find_all_scenarios(RHEM_par_excel_input)
    for scenario in all_scenarios:
        station = climate_station(scenario)
        climateFile = 'get the file name based on station name'
        ListClimate.append(climateFile)
    return List_of_climatefiles


def write_parfile(RHEM_par_excel_input):

    '''read excel file with RHEM input parameters and write par file for each run
       This can be done in two ways:
       1) write the entire file line by line
       2) read in a template parfile, keep the structure and only change the RHEM parameter values need to be modified '''

    List_of_prefiles = []
    all_scenarios = find_all_scenarios(RHEM_par_excel_input)
    for scenario in all_scenarios:
        parfile = parfile_name(scenario)  # a par file name different for each run
        f = open(parfile, 'w')
        f.write('line by line, or change RHEM parameters')
        List_of_prefiles.append(parfile)
    return List_of_prefiles


def run_RHEM(k2command, parfile, prefile, outfile):
    ''' run k2'''
    kin_str = '{},{},{}," ",N,N,,N,N\n'.format(parfile, prefile, outfile)
    f = open('kin.fil', 'w')
    f.write(kin_str)
    f.close()
    os.system(k2command)
