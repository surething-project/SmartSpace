function [Freq, Psd] = powerSpectralDensity(S, Fs)
%     N = length(S);
%     N = pow2(nextpow2(N));
%     
%     xdft = fft(S, N);
%     xdft = xdft(1:N/2+1);
%     
%     Psd = (1/(2*pi*N)) * abs(xdft).^2;
%     Psd(2:end - 1) = 2 * Psd(2:end - 1);
%    
% %   Remove line below to plot it in dB
% %   Psd = 10 * log10(Psd);
%     
%     Freq = 0:(2 * pi)/N:pi;
%     Freq = Freq/pi;
    N = length(S);
    N = pow2(nextpow2(N));

    xdft = fft(S);
    xdft = xdft(1:N/2+1);
    Psd = (1/(Fs*N)) * abs(xdft).^2;
    Psd(2:end-1) = 2 * Psd(2:end-1);
    Freq = 0:Fs/N:Fs/2;
end