function corrY = processLight(trustedId, untrustedId, proofDuration)
    trustedDir = strcat('dataset\', trustedId, '\');
    untrustedDir = strcat('dataset\', untrustedId, '\');

    % Trusted signal.
    [Ttrusted, Ytrusted] = timeAndValue(strcat(trustedDir, 'trusted_witness_light_intensity'));

    % Untrusted signal.
    [Tuntrusted, Yuntrusted] = timeAndValue(strcat(untrustedDir, 'untrusted_witness_light_intensity'));
    
    % Resample signal.
    [Ytrusted, Yuntrusted, ~] = resampleSignals(Ttrusted, Ytrusted, Tuntrusted, Yuntrusted);
    
    % Align signals.
    [Ytrusted, Yuntrusted] = alignSignals(Ytrusted, Yuntrusted);
    % Truncate the longest dataset.
    [Ytrusted, Yuntrusted, S] = truncateData(Ytrusted, Yuntrusted);
    X = linspace(0, proofDuration, S);
    
    % Normalize both datasets.
    Ytrusted = normalize(Ytrusted);
    Yuntrusted = normalize(Yuntrusted);

    % Build a figure.
    figure = getFigure(6, 2);

    plot(X, Ytrusted, 'b')
    xlim([0 proofDuration])
    xlabel('Time (s)')
    ylabel({'Normalized';'intensity'})
    saveas(figure, strcat(trustedDir, 'Light_trusted.pdf'))

    plot(X, Yuntrusted, 'r')
    xlim([0 proofDuration])
    xlabel('Time (s)')
    ylabel({'Normalized';'intensity'})
    saveas(figure, strcat(trustedDir, 'Light_untrusted.pdf'))

    plot(X, Ytrusted, 'b', X, Yuntrusted, 'r')
    xlim([0 proofDuration])
    xlabel('Time (s)')
    ylabel({'Normalized';'intensity'})
    legend('{\it w_{light}}', '{\it w''_{light}}', 'Location', 'northoutside')
    saveas(figure, strcat(trustedDir, 'Light_overlap.pdf'))
    
    corrY = corr(Ytrusted, Yuntrusted);
end