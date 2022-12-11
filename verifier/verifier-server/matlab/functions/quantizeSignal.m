function intervals = quantizeSignal(Y, nLevels)
    Y = medfilt1(Y);
    meanY = mean(Y);
    
    aboveMeanY = Y(Y > meanY);
    meanMaxY = mean(aboveMeanY);
    
    belowMeanY = Y(Y < meanY);
    meanMinY = mean(belowMeanY);
    
    codebook = linspace(meanMinY, meanMaxY, nLevels);
    step = (meanMaxY - meanMinY) / nLevels;
    partition = (meanMinY:step:meanMaxY);
    partition = partition(2:end - 1);

    intervals = quantiz(Y, partition, codebook);
end