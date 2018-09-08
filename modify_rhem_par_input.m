% you have got this:
[P.Kss,P.Ke,P.G,P.POR,P.CHEZY,P.Fraction,P.Density,P.Dist] = Driver4Function_FrictionFactorAndWeightedKeAndKss_06May2014(InputDataMatrix );    

% parfile is a template parfile (my parfile is quite complicated with many elements. You can write your own without this template if yours is simple     
O = textread(parfile,'%s','delimiter', '\n');

% newpar_file is the input par file name for simulation
fid_w = fopen(newpar_file, 'w');

% copy the comment lines in parfile
fprintf(fid_w, '%s\n',O{1:13,:});  

% write the global section (you may not need this)
header = {'BEGIN GLOBAL','CLEN	= 10','UNITS	= metric ','DIAMS	= 0.002	0.01  0.03	0.3	 0.2 ','DENSITY	= 2.6   2.65  1.8   1.6  2.65','TEMP	= 33','NELE	= 628','END GLOBAL', '  '};
for i = 1 : length(header),
    fprintf(fid_w,'%s\n', char(header{i}));
end

% write parameters
 write_plane(ID, P,fid_w,O,i)

fclose all;
display('done')

%% I wrote this function to write parameters for all plane elements (each has an unique ID)
% you will only need to write for one element, so ignore the ID and PID
% here is just an idea how to write the par file by replacing
% the corresponding strings in the template by desried RHEM parameter values
% you can also write straight forward without using the template file
% your parfile shoule be in a differnt format, follow the correct format


function write_plane(ID, P,fid_w,O,i)          
      
        PID = ( P.ElementID == ID);
        fract = P.Fraction(PID,:);
        fracstr = strjoin(cellstr(num2str(fract')),', ');
        
        fprintf(fid_w, '%s\n', O{i:i+4,:});
        line = strsplit(O{i + 5}, ',');        
        newline = [char(line(2)) ',' char(line(3))];  % remove manning's N
        fprintf(fid_w, '%s\n', newline);
                
        fprintf(fid_w, '%s\n', O{i + 6});        
        fprintf(fid_w, '%s\n%s\n', sprintf('G = %.2f, DIST = %.2f, POR = %.4f, ROCK = 0.0000', P.G(PID), P.Dist(PID),P.POR(PID)));        
        fprintf(fid_w, '%s\n',   sprintf('FR = %s,  COH = %.5f, SMAX = %.5f', fracstr, P.coh(PID), P.Smax(PID)));
        fprintf(fid_w, '%s\n',   sprintf('INTER = %.2f,  CANOPY = %.2f , PAVE = 0', P.Int(PID), P.fcc(PID)/100));
        fprintf(fid_w, '%s\n',   sprintf('BARE =  %.2f', 1 - P.gc(PID)/100));
        fprintf(fid_w, '%s\n',   sprintf('CHEZY = %.4f', P.CHEZY(PID)));
        fprintf(fid_w, '%s\n',   sprintf('RCHEZY = %.4f',P.CHEZY(PID)));
        fprintf(fid_w, '%s\n',   sprintf('KE =  %.4f', P.Ke(PID)));
        fprintf(fid_w, '%s\n',   sprintf('KSS =  %.4f', P.Kss(PID)));
        fprintf(fid_w, '%s\n',   'KOMEGA = 0.000007747');
        fprintf(fid_w, '%s\n',   'KCM = 0.000299364300');
        fprintf(fid_w, '%s\n',   'ADF = 0');
        fprintf(fid_w, '%s\n',   'ALF = 0.8000');
        fprintf(fid_w, '%s\n',   'RSP = 1');
        fprintf(fid_w, '%s\n',   'SPACING = 1');
        fprintf(fid_w, '%s\n\n',   'END PLANE');  
end
