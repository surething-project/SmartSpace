function [Y1] = rescaleValues(Y0)
%     yMin  = min(Y0);
%     yMax  = max(Y0);
%     range = (yMax - yMin) + eps(yMax - yMin);
%     Y1    = (Y0 - (yMin - eps(yMin))) / range;
    
%     Y1 = (Y0 - mean(Y0)) / std(Y0);
    Y1 = (Y0 - mean(Y0)) / (max(Y0) - min(Y0));
end

