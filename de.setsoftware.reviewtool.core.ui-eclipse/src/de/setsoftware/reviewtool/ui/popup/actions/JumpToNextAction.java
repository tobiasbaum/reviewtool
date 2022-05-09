package de.setsoftware.reviewtool.ui.popup.actions;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

import de.setsoftware.reviewtool.model.Mode;
import de.setsoftware.reviewtool.plugin.ReviewPlugin;

/**
 * Jumps to the next stop or open remark, depending on the current mode.
 */
public class JumpToNextAction extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        if (ReviewPlugin.getInstance().getMode() == Mode.FIXING) {
            return new JumpToNextOpenRemarkAction().execute(event);
        } else {
            return new JumpToNextUnvisitedStopAction().execute(event);
        }
    }

}
