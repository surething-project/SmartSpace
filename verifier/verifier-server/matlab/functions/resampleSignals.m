function [Ytrusted, Yuntrusted, F] = resampleSignals(Ttrusted, Ytrusted, Tuntrusted, Yuntrusted)
    Ftrusted = round(samplingFrequency(Ttrusted), 3);
    Funtrusted = round(samplingFrequency(Tuntrusted), 3);

    % Set a fixed sampling rate.
    F = max(Ftrusted, Funtrusted);
    
    [Puntrusted, Quntrusted] = rat(F / Funtrusted);
    Yuntrusted = resample(Yuntrusted, Puntrusted, Quntrusted);
    [Ptrusted, Qtrusted] = rat(F / Ftrusted);
    Ytrusted = resample(Ytrusted, Ptrusted, Qtrusted);
end