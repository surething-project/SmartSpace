function [Yold, Ynew] = signalEnvelope(Y)
    [H, L] = envelope(Y, 16, 'peak');
    Yold = Y;
    Ynew = (H + L) / 2;
end

