function [corrY, corrP, warpingDistance] = processSound(trustedId, untrustedId, proofDuration)
    trustedDir = strcat('dataset\', trustedId, '\');
    untrustedDir = strcat('dataset\', untrustedId, '\');

    % Trusted signal.
    [Ttrusted, Ytrusted] = timeAndValue(strcat(trustedDir, 'trusted_witness_sound_amplitude'));

    % Untrusted signal.
    [Tuntrusted, Yuntrusted] = timeAndValue(strcat(untrustedDir, 'untrusted_witness_sound_amplitude'));
    
    % Fill missing values.
    Ytrusted = fillmissing(Ytrusted, 'nearest');
    Yuntrusted = fillmissing(Yuntrusted, 'nearest');

    % Normalize both datasets.
    Ytrusted = normalize(Ytrusted);
    Yuntrusted = normalize(Yuntrusted);
    
    % Resample signal.
    [Ytrusted, Yuntrusted] = resampleSignals(Ttrusted, Ytrusted, Tuntrusted, Yuntrusted);
        
    [warpingDistance, warpingPathTrusted, warpingPathUntrusted] = dtw(Ytrusted, Yuntrusted);
    Ytrusted = Ytrusted(warpingPathTrusted);
    Yuntrusted = Yuntrusted(warpingPathUntrusted);
    
    S = length(Ytrusted);
    X = linspace(0, proofDuration, S);
    
    % Power Spectral Density Estimate.
    [Ptrusted, Ftrusted] = periodogram(Ytrusted, hamming(S), S);
    [Puntrusted, Funtrusted] = periodogram(Yuntrusted, hamming(S), S);

    % Build a figure.
    figure = getFigure(6, 2);
    
    plot(X, Ytrusted, 'b')
    xlim([0 proofDuration])
    xlabel('Time (s)')
    ylabel({'Normalized';'amplitude'})
    saveas(figure, strcat(trustedDir, 'Audio_trusted.pdf'))
    
    plot(X, Yuntrusted, 'r')
    xlim([0 proofDuration])
    xlabel('Time (s)')
    ylabel({'Normalized';'amplitude'})
    saveas(figure, strcat(trustedDir, 'Audio_untrusted.pdf'))
    
    plot(X, Ytrusted, 'b', X, Yuntrusted, 'r')
    xlim([0 proofDuration]);
    xlabel('Time (s)')
    ylabel({'Normalized';'amplitude'})
    legend('{\it w_{audio}}', '{\it w''_{audio}}', 'Location', 'northoutside')
    saveas(figure, strcat(trustedDir, 'Audio_overlap.pdf'))

    plot(Ftrusted/pi, Ptrusted, 'b')
    xlabel('Normalized frequency (Hz)')
    ylabel('Magnitude')
    saveas(figure, strcat(trustedDir, 'Audio_spectrum_trusted.pdf'))
    
    plot(Ftrusted/pi, Puntrusted, 'r')
    xlabel('Normalized frequency (Hz)')
    ylabel('Magnitude')
    saveas(figure, strcat(trustedDir, 'Audio_spectrum_untrusted.pdf'))

    plot(Ftrusted/pi, Ptrusted, 'b')
    hold on
    plot(Funtrusted/pi, Puntrusted, 'r')
    xlabel('Normalized frequency (Hz)')
    ylabel('Magnitude')
    legend('{\it w_{audio}}', '{\it w''_{audio}}', 'Location', 'northoutside')
    saveas(figure, strcat(trustedDir, 'Audio_spectrum_overlap.pdf'))
    
    corrY = corr(Ytrusted, Yuntrusted);
    corrP = corr(Ptrusted, Puntrusted);
end