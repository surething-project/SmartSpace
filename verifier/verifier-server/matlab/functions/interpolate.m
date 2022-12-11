function [Y1, Y2, N] = interpolate(Y1, Y2)
    N1 = numel(Y1);
    N2 = numel(Y2);
    interp1Method = 'nearest';
    extrapolation = 'extrap';

    N = N1;
    if N1 > N2
        Y2 = interp1((0:N2 - 1), Y2, (0:N1 - 1), interp1Method, extrapolation)';
        N = N1;

    elseif N2 > N1
        Y1 = interp1((0:N1 - 1), Y1, (0:N2 - 1), interp1Method, extrapolation)';
        N = N2;
    end
end