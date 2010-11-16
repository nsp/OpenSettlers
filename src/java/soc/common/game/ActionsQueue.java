package soc.common.game;

import java.util.ArrayList;

import soc.common.actions.GameAction;
import soc.common.actions.ingame.InGameAction;

/*
 * A list of queued actions. This aids the user in what to expect from them, they
 * can actually see a list of things they must do.
 */
public class ActionsQueue extends ArrayList<GameAction> implements IActionsQueue
{
    @Override
    public void Enqueue(InGameAction inGameAction)
    {
        // TODO Auto-generated method stub   
    }
}
