function correlation = getCorrelation(Ttrusted, Ytrusted, Tuntrusted, Yuntrusted)
    % Resample signal.    
    [Ytrusted, Yuntrusted, ~] = resampleSignals(Ttrusted, Ytrusted, Tuntrusted, Yuntrusted);
    
    % Align signals.
    [Ytrusted, Yuntrusted] = alignSignals(Ytrusted, Yuntrusted);
    % Truncate the longest dataset.
    [Ytrusted, Yuntrusted, ~] = truncateData(Ytrusted, Yuntrusted);
    
    correlation = corr(Ytrusted, Yuntrusted);
end

