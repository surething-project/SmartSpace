function fig = getFigure(width, height)
    fig = figure('visible', 'off');
    
    set(fig, 'PaperOrientation', 'Landscape');
    set(fig, 'PaperUnits', 'Inches');
    set(fig, 'PaperPosition', [0 0 width height]);
    set(fig, 'PaperSize', [width height]);
end