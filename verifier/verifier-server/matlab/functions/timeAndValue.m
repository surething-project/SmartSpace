function [T, Y] = timeAndValue(csvFile)
    M = readmatrix(csvFile);
    
    T = M(:,1);
    Y = M(:,2);
end