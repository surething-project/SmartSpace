function F = samplingFrequency(v)
    F = 1 / mean(diff(v / 1000));
%     F = 1/mode(diff(v)) * 1000;
%     F = length(v) / 30.0;
end