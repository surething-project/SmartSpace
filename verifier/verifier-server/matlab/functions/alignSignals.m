function [Ytrusted, Yuntrusted] = alignSignals(Ytrusted, Yuntrusted)
%     [C, lag] = xcorr(Ytrusted, Yuntrusted);
%     C = C/max(C);
%     [~, I] = max(C);
%     T = lag(I);
%     
%     if T < 0
%         Yuntrusted = Yuntrusted(-T:end);
%     elseif T > 0
%         Ytrusted = Ytrusted(T:end);
%     end
    
    [Ytrusted, Yuntrusted] = alignsignals(Ytrusted, Yuntrusted);
end

