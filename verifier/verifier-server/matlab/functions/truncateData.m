function [Ytrusted, Yuntrusted, S] = truncateData(Ytrusted, Yuntrusted)
    Strusted = length(Ytrusted);
    Suntrusted = length(Yuntrusted);

    if Strusted > Suntrusted
        Ytrusted = Ytrusted(1:Suntrusted,:);
        Strusted = Suntrusted;
    elseif Suntrusted > Strusted
        Yuntrusted = Yuntrusted(1:Strusted,:);
    end
    
    S = Strusted;
end