package de.setsoftware.reviewtool.ui.popup.actions;

import java.awt.Desktop;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.ui.handlers.HandlerUtil;

import de.setsoftware.reviewtool.model.changestructure.Stop;

/**
 * Opens the directory where the file the stop belongs to is contained in.
 */
public class OpenDirectoryAction extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        final ISelection selection = HandlerUtil.getActiveMenuSelectionChecked(event);

        if (!(selection instanceof TreeSelection)) {
            return null;
        }
        final TreeSelection ts = (TreeSelection) selection;

        final List<Stop> stops = new ArrayList<>(ts.toList());
        for (final Stop stop : stops) {
            try {
                Desktop.getDesktop().open(stop.getAbsoluteFile().getParentFile());
            } catch (final IOException e) {
                throw new ExecutionException("error while opening directory", e);
            }
        }
        return null;
    }

}
