package soc.common.game;

import soc.common.actions.ingame.InGameAction;

public interface IActionsQueue
{
    public void Enqueue(InGameAction inGameAction);
}
