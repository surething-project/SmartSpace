% Change the weight here.
weightName = 'w1';

% DO NOT change lines below.
fixedName = strcat(weightName(1), '_', weightName(2));
m = readmatrix(strcat(weightName, '.csv'));
w = m(:,1);
sum = m(:,3);
localMinima = islocalmin(sum, 'MaxNumExtrema', 1);

figure = getFigure(8, 3);
plot(w, sum, 'r', 'linewidth', 1)
hold on
plot(w(localMinima), sum(localMinima), 'b*-', 'linewidth', 4)

xlabel(fixedName)
ylabel('FPR + FNR')
xline(w(localMinima), '--ob', strcat(fixedName, ' = ', string(w(localMinima))))
saveas(figure, strcat('Optimal_', weightName, '.pdf'))